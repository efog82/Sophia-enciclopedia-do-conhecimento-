package com.sophia.data

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.sophia.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

object LocationCodec {
    fun encode(location:ReadingLocation)=JSONObject().put("version",1).put("unit",location.unit).put("fraction",location.fraction).toString()
    fun decode(value:String):ReadingLocation{val json=JSONObject(value);require(json.getInt("version")==1){"Versão de posição não suportada."};return ReadingLocation(json.getInt("unit"),json.getDouble("fraction"))}
}
class LocalLibraryRepository(private val context:Context,private val db:SophiaDatabase):LibraryRepository {
    private val mutex=Mutex()
    private val coverMutex=Mutex()
    private val directory get()=File(context.filesDir,"books").apply{mkdirs()}
    override val snapshot=combine(db.library().files(),db.library().collections(),db.catalog().observeWorks()){files,collections,works->
        val titles=works.associate{it.work.id to it.work.title}
        LibrarySnapshot(files.map{mapFile(it,titles[it.edition.workId]?:"Obra importada")},collections.map{StudyCollection(it.collection.id,it.collection.name,it.collection.readingList,it.works.sortedWith(compareBy<CollectionWorkEntity>{it.position}.thenBy{it.workId}).map{it.workId})})
    }
    private fun mapFile(row:FileRelation,title:String):LibraryFile {val p=row.progress.firstOrNull();return LibraryFile(row.file.id,row.edition.workId,title,row.file.displayName.ifBlank{"Ficheiro importado"},row.file.mimeType,row.file.sizeBytes?:0,row.file.sha256.orEmpty(),row.file.rightsStatus,row.file.addedAt,p?.locatorJson?.let{runCatching{LocationCodec.decode(it)}.getOrNull()},p?.progressPermille?:0,p?.updatedAt?:0)}
    override suspend fun file(id:String):LibraryFile {val row=db.library().fileOnce(id)?:error("O ficheiro já não existe na biblioteca.");return mapFile(row,db.library().workTitle(row.edition.workId)?:"Obra importada")}
    suspend fun localFile(id:String):File=withContext(Dispatchers.IO){val row=db.library().fileOnce(id)?:error("O ficheiro já não existe na biblioteca.");val result=File(Uri.parse(row.file.uri).path?:error("Localização inválida."));require(result.canonicalFile.parentFile==directory.canonicalFile && result.isFile){"O ficheiro local não está disponível. Volte a importá-lo."};result}
    suspend fun document(id:String):BookDocument=withContext(Dispatchers.IO){val info=file(id);val f=localFile(id);when(info.mimeType){"application/epub+zip"->BookDocuments.epub(f);"text/plain"->BookDocuments.text(f);"application/pdf"->{ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY).use{descriptor->PdfRenderer(descriptor).use{pdf->BookDocument(FileMetadata(),(0 until pdf.pageCount).map{ReadingUnit("page:$it","Página ${it+1}")},false)}}};else->error("Formato não suportado.")}}
    suspend fun pdfPage(id:String,index:Int):android.graphics.Bitmap=withContext(Dispatchers.IO){
        val f=localFile(id)
        ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY).use{pfd->PdfRenderer(pfd).use{renderer->
            require(index in 0 until renderer.pageCount)
            renderer.openPage(index).use{page->val scale=minOf(1600.0/page.width,2400.0/page.height);val bitmap=android.graphics.Bitmap.createBitmap((page.width*scale).toInt().coerceAtLeast(1),(page.height*scale).toInt().coerceAtLeast(1),android.graphics.Bitmap.Config.ARGB_8888);bitmap.eraseColor(android.graphics.Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);bitmap}
        }}
    }
    /** Derived thumbnails only: no Room schema change and no remote image requests. */
    suspend fun cover(id:String):android.graphics.Bitmap?=withContext(Dispatchers.IO){coverMutex.withLock{
        val info=file(id)
        val cache=File(context.cacheDir,"book-covers").apply{mkdirs()}
        val image=File(cache,"${info.sha256}-v1.png")
        if(image.isFile)return@withLock android.graphics.BitmapFactory.decodeFile(image.path)
        val missing=File(cache,"${info.sha256}-v1.none")
        if(missing.exists())return@withLock null
        try{
            val bitmap=when(info.mimeType){
                "application/pdf"->pdfPage(id,0).let{page->
                    val scale=minOf(320f/page.width,480f/page.height,1f)
                    val small=android.graphics.Bitmap.createScaledBitmap(page,(page.width*scale).toInt().coerceAtLeast(1),(page.height*scale).toInt().coerceAtLeast(1),true)
                    if(small!==page)page.recycle()
                    small
                }
                "application/epub+zip"->{
                    val local=localFile(id)
                    val path=BookDocuments.epub(local).coverHref
                    val bytes=path?.let{BookDocuments.resource(local,it)}
                    bytes?.let{
                        val bounds=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true}
                        android.graphics.BitmapFactory.decodeByteArray(it,0,it.size,bounds)
                        require(bounds.outWidth>0&&bounds.outHeight>0&&bounds.outWidth.toLong()*bounds.outHeight<=40000000){"Imagem de capa incompatível."}
                        var sample=1
                        while(bounds.outWidth/sample>640||bounds.outHeight/sample>960)sample*=2
                        android.graphics.BitmapFactory.decodeByteArray(it,0,it.size,android.graphics.BitmapFactory.Options().apply{inSampleSize=sample})
                    }
                }
                else->null
            }
            if(bitmap==null)missing.writeText("")
            else runCatching{image.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
            bitmap
        }catch(e:CancellationException){throw e}catch(_:Exception){null}
    }}
    override suspend fun importFile(request:ImportRequest):ImportResult=mutex.withLock{withContext(Dispatchers.IO){
        require(request.authorised){"Confirme que tem autorização para utilizar o ficheiro."}
        val source=Uri.parse(request.sourceUri);require(source.scheme=="content"){"Escolha o ficheiro através do seletor Android."}
        val originalName=context.contentResolver.query(source,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{cursor->if(cursor.moveToFirst())cursor.getString(0)else null}?:"Ficheiro importado"
        val id=UUID.randomUUID().toString();val temp=File(directory,"$id.part");var target:File?=null;var committed=false
        try{
            val digest=MessageDigest.getInstance("SHA-256");var size=0L
            val input=context.contentResolver.openInputStream(source)?:error("Não foi possível ler o ficheiro selecionado.")
            input.use{stream->temp.outputStream().use{out->val buffer=ByteArray(65536);while(true){currentCoroutineContext().ensureActive();val n=stream.read(buffer);if(n<0)break;size+=n;require(size<=BookDocuments.MAX_IMPORT_BYTES){"O ficheiro excede 200 MiB."};digest.update(buffer,0,n);out.write(buffer,0,n)}}}
            require(size>0){"O ficheiro está vazio."}
            val header=ByteArray(5);temp.inputStream().use{it.read(header)}
            val mime=when{header.toString(Charsets.US_ASCII)=="%PDF-"->"application/pdf";header[0]==80.toByte()&&header[1]==75.toByte()->"application/epub+zip";originalName.substringAfterLast('.',"").lowercase()=="txt"||context.contentResolver.getType(source)=="text/plain"->"text/plain";else->throw IllegalArgumentException("Ficheiro incompatível. Escolha EPUB, PDF ou TXT.")}
            val metadata=when(mime){"application/epub+zip"->BookDocuments.epub(temp).metadata;"text/plain"->BookDocuments.text(temp).metadata;else->{try{ParcelFileDescriptor.open(temp,ParcelFileDescriptor.MODE_READ_ONLY).use{pfd->PdfRenderer(pfd).use{require(it.pageCount>0){"O PDF não contém páginas."}}}}catch(e:SecurityException){throw IllegalArgumentException("PDF protegido por palavra-passe não suportado. Importe uma cópia autorizada sem proteção.")};FileMetadata()}}
            val hash=digest.digest().joinToString(""){"%02x".format(it)}
            db.library().sameHash(hash).firstOrNull{request.workId==null||it.edition.workId==request.workId}?.let{return@withContext ImportResult(it.file.id,it.edition.workId,true)}
            target=File(directory,"$id.${when(mime){"application/pdf"->"pdf";"text/plain"->"txt";else->"epub"}}")
            require(temp.renameTo(target)){"Não foi possível guardar a cópia local."}
            val workId=request.workId?:UUID.randomUUID().toString();val editionId=UUID.randomUUID().toString();val now=System.currentTimeMillis()
            db.withTransaction{
                if(request.workId==null){val title=request.title.trim().ifBlank{metadata.title?:originalName.substringBeforeLast('.',originalName)}.take(300);require(title.isNotBlank());db.catalog().insertWorks(listOf(WorkEntity(workId,title,"Obra importada pelo utilizador para estudo pessoal.",sourceUrl="",sourceLabel="Ficheiro pessoal através do seletor Android",metadataNote="Metadados declarados no ficheiro ou pelo utilizador; não verificados bibliograficamente. Autorização de uso declarada pelo utilizador, sem verificação independente de licença e sem redistribuição.",addedAt=now,demo=false)))}
                else require(db.library().workTitle(workId)!=null){"A obra selecionada já não existe."}
                db.catalog().insertEdition(EditionEntity(editionId,workId,label="Importação pessoal · $originalName",publisher=metadata.publisher,publicationYear=metadata.year,language=metadata.language,rightsStatus="USER_AUTHORISED_NOT_VERIFIED"))
                metadata.authors.forEach{name->val personId=UUID.randomUUID().toString();db.catalog().insertPeople(listOf(PersonEntity(personId,name)));db.catalog().insertEditionCredit(EditionContributorEntity(editionId,personId,"AUTHOR"))}
                db.catalog().insertFile(BookFileEntity(id,editionId,Uri.fromFile(target).toString(),mime,hash,"USER_AUTHORISED_NOT_VERIFIED",sizeBytes=size,displayName=originalName,addedAt=now))
            }
            committed=true;ImportResult(id,workId,false)
        }finally{temp.delete();if(!committed)target?.delete()}
    }}
    override suspend fun savePosition(fileId:String,location:ReadingLocation,progressPermille:Int){require(progressPermille in 0..1000);db.catalog().saveProgress(ReadingProgressEntity(fileId,LocationCodec.encode(location),progressPermille,System.currentTimeMillis()))}
    override fun bookmarks(fileId:String)=db.library().bookmarks(fileId).map{rows->rows.map{Bookmark(it.id,it.fileId,it.label,LocationCodec.decode(it.locatorJson),it.createdAt)}}
    override suspend fun addBookmark(fileId:String,label:String,location:ReadingLocation){db.library().addBookmark(BookmarkEntity(UUID.randomUUID().toString(),fileId,LocationCodec.encode(location),label.take(200),System.currentTimeMillis()))}
    override suspend fun deleteBookmark(id:String)=db.library().deleteBookmark(id)
    override suspend fun createCollection(name:String,readingList:Boolean){val n=name.trim();require(n.isNotEmpty()&&n.length<=120){"Escreva um nome de 1 a 120 caracteres."};db.library().addCollection(StudyCollectionEntity(UUID.randomUUID().toString(),n,readingList,System.currentTimeMillis()))}
    override suspend fun deleteCollection(id:String)=db.library().deleteCollection(id)
    override suspend fun setInCollection(collectionId:String,workId:String,included:Boolean)=db.withTransaction{if(!included)db.library().removeEntry(collectionId,workId)else{val entries=db.library().entries(collectionId);if(entries.none{it.workId==workId})db.library().setEntry(CollectionWorkEntity(collectionId,workId,(entries.maxOfOrNull{it.position}?:-1)+1))}}
    override suspend fun moveInCollection(collectionId:String,workId:String,delta:Int)=db.withTransaction{val entries=db.library().entries(collectionId).toMutableList();val from=entries.indexOfFirst{it.workId==workId};val to=from+delta;require(delta==1||delta== -1);if(from>=0&&to in entries.indices){val moved=entries.removeAt(from);entries.add(to,moved);entries.forEachIndexed{i,e->db.library().setEntry(e.copy(position=i))}}}
}
