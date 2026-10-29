package com.sophia.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.sophia.domain.BookDocuments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.security.MessageDigest

/** Copies assets, never downloads. Fixed IDs and IGNORE inserts preserve the user's records. */
object BundledBooks {
    suspend fun install(context:Context,db:SophiaDatabase)=withContext(Dispatchers.IO){
        val catalog=JSONArray(context.assets.open("books/catalog.json").bufferedReader().use{it.readText()})
        val directory=File(context.filesDir,"books").apply{mkdirs()}
        db.catalog().insertTraditions(listOf(TraditionEntity("portuguese-philosophy","Filosofia portuguesa","Textos de reflexão filosófica produzidos em Portugal.","Conjunto plural de autores e correntes, sem doutrina única. Inclui debates sobre razão, religião, natureza e ética em contextos históricos diferentes.","Metafísica; razão; ética; crítica; relação entre ciência e filosofia","https://www.gutenberg.org/ebooks/26776")))
        for(i in 0 until catalog.length()){
            val item=catalog.getJSONObject(i);val id=item.getString("id");val fileId="bundled-$id";val name=item.getString("file")
            require(name.matches(Regex("pg-[0-9]+\\.epub")))
            val target=File(directory,"$fileId.epub");val expected=item.getString("sha256")
            fun digest(file:File)=MessageDigest.getInstance("SHA-256").let{md->file.inputStream().use{input->val buffer=ByteArray(65536);while(true){val n=input.read(buffer);if(n<0)break;md.update(buffer,0,n)}};md.digest().joinToString(""){"%02x".format(it)}}
            if(!target.isFile||digest(target)!=expected){
                val temp=File(directory,"$fileId.part")
                try{context.assets.open("books/$name").use{input->temp.outputStream().use{input.copyTo(it)}};require(digest(temp)==expected){"A cópia do livro incluído não passou na verificação de integridade."};BookDocuments.epub(temp);require(temp.renameTo(target)){"Não foi possível instalar a cópia de leitura."}}finally{temp.delete()}
            }
            val title=item.getString("title");val source=item.getString("source");val edition="bundled-edition-$id";val person="bundled-author-$id";val now=System.currentTimeMillis()
            db.withTransaction{
                db.catalog().insertWorks(listOf(WorkEntity(id,title,item.getString("description"),originalLanguage="pt-PT",sourceUrl=source,sourceLabel="Project Gutenberg · edição histórica portuguesa",metadataNote=item.getString("spelling")+" "+item.getString("rightsBasis")+" Não é uma tradução nem uma atualização ortográfica. Data/editor da edição concreta apenas quando verificados.",addedAt=now,demo=false)))
                db.catalog().insertPeople(listOf(PersonEntity(person,item.getString("author"))))
                db.catalog().insertCredits(listOf(WorkContributorEntity(id,person,"AUTHOR")))
                db.catalog().insertWorkTraditions(listOf(WorkTraditionEntity(id,item.getString("tradition"))))
                db.catalog().insertWorkCategories(listOf(WorkCategoryEntity(id,"philosophy")))
                if(db.catalog().file(fileId)==null){
                    db.catalog().insertEdition(EditionEntity(edition,id,label="Edição digital histórica · texto integral · ortografia original",language="pt-PT",sourceUrl=source,rightsStatus="PUBLIC_DOMAIN_PT_US"))
                    db.catalog().insertFile(BookFileEntity(fileId,edition,Uri.fromFile(target).toString(),"application/epub+zip",expected,"PUBLIC_DOMAIN_PT_US",source,target.length(),title+".epub",now))
                }
            }
        }
        db.catalog().saveMetadata(CatalogMetadataEntity("bundled_books_version","1"))
    }
}
