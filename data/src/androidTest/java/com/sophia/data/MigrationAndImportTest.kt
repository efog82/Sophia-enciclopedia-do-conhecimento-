package com.sophia.data

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sophia.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.zip.*

@RunWith(AndroidJUnit4::class)
class MigrationAndImportTest {
    @get:Rule val helper=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),SophiaDatabase::class.java)
    private val context:Context get()=ApplicationProvider.getApplicationContext()
    private fun fixture(name:String,content:ByteArray):Uri{val dir=File(InstrumentationRegistry.getInstrumentation().context.cacheDir,"fixtures").apply{mkdirs()};File(dir,name).writeBytes(content);return Uri.parse("content://com.sophia.data.test.fixtures/$name")}
    private fun epub():ByteArray{val out=java.io.ByteArrayOutputStream();ZipOutputStream(out).use{zip->mapOf("mimetype" to "application/epub+zip","META-INF/container.xml" to "<container><rootfiles><rootfile full-path='book.opf'/></rootfiles></container>","book.opf" to "<package><metadata><title>Técnica Sophia</title><creator>Autor declarado de teste</creator><language>pt</language></metadata><manifest><item id='one' href='one.xhtml' media-type='application/xhtml+xml'/></manifest><spine><itemref idref='one'/></spine></package>","one.xhtml" to "<html><body><h1>Recurso técnico</h1><p>Texto original criado apenas para teste.</p></body></html>").forEach{(n,v)->zip.putNextEntry(ZipEntry(n));zip.write(v.toByteArray());zip.closeEntry()}};return out.toByteArray()}
    private suspend fun isolated(block:suspend (SophiaDatabase,LocalLibraryRepository)->Unit){val name="import-${UUID.randomUUID()}.db";val db=SophiaDatabase.open(context,name);try{RoomCatalogRepository(db).initialize();block(db,LocalLibraryRepository(context,db))}finally{db.close();context.deleteDatabase(name)}}
    @Test fun migratesV1AndAddsCatalogWithoutLosingPersonalData()=runBlocking{
        val name="migration-${UUID.randomUUID()}.db"
        helper.createDatabase(name,1).apply{
            execSQL("INSERT INTO works (id,title,description,sourceUrl,sourceLabel,metadataNote,addedAt,demo) VALUES ('spirits','O Livro dos Espíritos','Referência','https://example.org','Fonte','Nota',1,1)")
            listOf("kardec","xavier","divaldo","joanna","hermes","buddha","andre-luiz").forEach{id->execSQL("INSERT INTO people(id,displayName,kind) VALUES (?,?,'PERSON')",arrayOf(id,id))}
            listOf("spirituality","religions","philosophy","mysticism","esotericism","occultism").forEach{id->execSQL("INSERT INTO categories(id,name) VALUES (?,?)",arrayOf(id,id))}
            execSQL("INSERT INTO favorites (workId,addedAt) VALUES ('spirits',10)")
            execSQL("INSERT INTO study_notes (id,title,body,workId,createdAt,updatedAt) VALUES ('personal','Minha reflexão','  texto pessoal  ','spirits',12,13)")
            execSQL("INSERT INTO catalog_metadata (`key`,value) VALUES ('demo_catalog_version','1')")
            close()
        }
        helper.runMigrationsAndValidate(name,2,true,DatabaseMigrations.V1_TO_V2).close()
        val db=SophiaDatabase.open(context,name)
        try{RoomCatalogRepository(db).initialize();val count=db.catalog().worksOnce().size;ExpandedCatalog.seed(db);assertEquals(count,db.catalog().worksOnce().size);assertTrue(count>30);assertTrue(db.catalog().worksOnce().first{it.work.id=="spirits"}.favorites.isNotEmpty());assertEquals("  texto pessoal  ",db.notes().find("personal")!!.body);assertEquals(12L,db.notes().find("personal")!!.createdAt);assertEquals("1",db.catalog().metadata("demo_catalog_version"));assertEquals("2",db.catalog().metadata("universal_catalog_version"))}finally{db.close();context.deleteDatabase(name)}
    }
    @Test fun txtIsCopiedDeduplicatedAndReopenedOffline()=runBlocking{isolated{db,repo->
        val bytes="Texto pessoal original para ensaio offline.\n".repeat(400).toByteArray();val uri=fixture("${UUID.randomUUID()}.txt",bytes)
        val result=repo.importFile(ImportRequest(uri.toString(),workId="spirits",authorised=true));assertFalse(result.duplicate);assertEquals("spirits",result.workId);assertArrayEquals(bytes,repo.localFile(result.fileId).readBytes());assertTrue(repo.document(result.fileId).units.size>1)
        val duplicate=repo.importFile(ImportRequest(uri.toString(),workId="spirits",authorised=true));assertTrue(duplicate.duplicate);assertEquals(result.fileId,duplicate.fileId)
        repo.savePosition(result.fileId,ReadingLocation(1,.3),500);repo.addBookmark(result.fileId,"Posição guardada",ReadingLocation(1,.3));repo.addBookmark(result.fileId,"Mesma posição",ReadingLocation(1,.3));assertEquals(1,repo.bookmarks(result.fileId).first().size)
        assertEquals(ReadingLocation(1,.3),repo.file(result.fileId).location)
        val note=RoomNotesRepository(db).save(null,"Nota ligada","Apontamento","spirits",result.fileId,LocationCodec.encode(ReadingLocation(1,.3)),"Texto copiado")
        assertEquals(result.fileId,db.notes().find(note)!!.fileId)
    }}
    @Test fun epubMetadataDoesNotBecomeAnInventedOriginalEdition()=runBlocking{isolated{db,repo->
        val uri=fixture("${UUID.randomUUID()}.epub",epub());val result=repo.importFile(ImportRequest(uri.toString(),authorised=true));val book=RoomCatalogRepository(db).books.first().first{it.id==result.workId}
        assertEquals("Técnica Sophia",book.title);assertNull(book.originalYear);assertEquals("pt",book.editions.single().language);assertEquals("USER_AUTHORISED_NOT_VERIFIED",book.editions.single().rightsStatus);assertEquals("Autor declarado de teste",book.editions.single().contributors.single().name);assertEquals(1,repo.document(result.fileId).units.size)
    }}
    @Test fun pdfHasRealPagesAndCanRender()=runBlocking{isolated{_,repo->
        val output=java.io.ByteArrayOutputStream();val doc=PdfDocument();try{repeat(2){i->val page=doc.startPage(PdfDocument.PageInfo.Builder(500,700,i+1).create());page.canvas.drawText("Pagina tecnica ${i+1}",30f,80f,android.graphics.Paint().apply{textSize=18f});doc.finishPage(page)};doc.writeTo(output)}finally{doc.close()}
        val result=repo.importFile(ImportRequest(fixture("${UUID.randomUUID()}.pdf",output.toByteArray()).toString(),authorised=true));assertEquals(2,repo.document(result.fileId).units.size);val bitmap=repo.pdfPage(result.fileId,1);assertTrue(bitmap.width>0);assertTrue(bitmap.height>0);bitmap.recycle()
    }}
    @Test fun incompatibleAndUnauthorisedImportsDoNotCreateRecords()=runBlocking{isolated{_,repo->
        val uri=fixture("${UUID.randomUUID()}.bin",byteArrayOf(0,1,2));var denied=false;try{repo.importFile(ImportRequest(uri.toString()))}catch(e:IllegalArgumentException){denied=true};assertTrue(denied)
        var incompatible=false;try{repo.importFile(ImportRequest(uri.toString(),authorised=true))}catch(e:IllegalArgumentException){incompatible=true};assertTrue(incompatible);assertTrue(repo.snapshot.first().files.isEmpty())
    }}
    @Test fun readingListsReorderAndDeleteWithoutDeletingWorks()=runBlocking{isolated{db,repo->
        repo.createCollection("Estudo comparativo",true);val id=repo.snapshot.first().collections.single().id;repo.setInCollection(id,"spirits",true);repo.setInCollection(id,"rigveda",true);repo.setInCollection(id,"rigveda",true);repo.moveInCollection(id,"rigveda",-1);assertEquals(listOf("rigveda","spirits"),repo.snapshot.first().collections.single().workIds);repo.deleteCollection(id);assertTrue(repo.snapshot.first().collections.isEmpty());assertNotNull(db.library().workTitle("rigveda"))
    }}
}
