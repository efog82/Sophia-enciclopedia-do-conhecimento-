package com.sophia.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sophia.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class HistoryAndCoverTest {
    @Test fun historicalNavigationAndSourcesAreComplete(){
        val articles=HistoricalEncyclopedia.articles
        assertEquals(17,articles.size)
        assertEquals(articles.size,articles.distinctBy{it.id}.size)
        assertEquals(setOf("spirituality","religions","philosophy","mysticism","esotericism","occultism"),articles.filter{it.area}.map{it.id}.toSet())
        articles.forEach{article->
            assertTrue(article.title,article.sections.size>=3)
            article.sections.forEach{section->
                assertTrue(section.title,section.sources.isNotEmpty())
                section.sources.forEach{assertTrue("${article.id}: source $it",it in 1..article.sources.size)}
            }
            article.related.forEach{id->assertTrue("${article.id}: broken link $id",articles.any{it.id==id})}
            assertTrue(article.id,article.sources.all{it.url.startsWith("https://")})
        }
        assertTrue(articles.first{it.id=="atheism"}.matches("Huxley"))
        assertTrue(articles.first{it.id=="spiritism"}.matches("Paris 1857"))
        assertTrue(articles.first{it.id=="hinduism"}.matches("vedica"))
    }
    @Test fun embeddedCoverIsLocalAndSurvivesReopenWithoutChangingPersonalData()=runBlocking{
        val context:Context=ApplicationProvider.getApplicationContext()
        val name="covers-${UUID.randomUUID()}.db"
        var db=SophiaDatabase.open(context,name)
        val repo=LocalLibraryRepository(context,db)
        val image=Bitmap.createBitmap(30,45,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val png=ByteArrayOutputStream().also{image.compress(Bitmap.CompressFormat.PNG,100,it)}.toByteArray();image.recycle()
        val output=ByteArrayOutputStream()
        ZipOutputStream(output).use{zip->
            val resources=mapOf("mimetype" to "application/epub+zip".toByteArray(),"META-INF/container.xml" to "<container><rootfiles><rootfile full-path='book.opf'/></rootfiles></container>".toByteArray(),"book.opf" to "<package><manifest><item id='c' href='one.xhtml' media-type='application/xhtml+xml'/><item id='cover' href='cover.png' media-type='image/png' properties='cover-image'/></manifest><spine><itemref idref='c'/></spine></package>".toByteArray(),"one.xhtml" to "<h1>Teste original</h1>".toByteArray(),"cover.png" to png)
            resources.forEach{(path,bytes)->zip.putNextEntry(ZipEntry(path));zip.write(bytes);zip.closeEntry()}
        }
        val fixtureDir=File(InstrumentationRegistry.getInstrumentation().context.cacheDir,"fixtures").apply{mkdirs()}
        val file=File(fixtureDir,"${UUID.randomUUID()}.epub").apply{writeBytes(output.toByteArray())}
        try{
            RoomCatalogRepository(db).initialize()
            val count=db.catalog().worksOnce().size
            val imported=repo.importFile(ImportRequest("content://com.sophia.data.test.fixtures/${file.name}",workId="rigveda",authorised=true))
            repo.savePosition(imported.fileId,ReadingLocation(0,.4),400)
            RoomCatalogRepository(db).setFavorite("rigveda",true)
            val note=RoomNotesRepository(db).save(null,"Antes da atualização","Texto pessoal","rigveda",null,null,null)
            val cover=repo.cover(imported.fileId)!!
            assertEquals(Color.RED,cover.getPixel(0,0));cover.recycle()
            file.delete();db.close();db=SophiaDatabase.open(context,name)
            RoomCatalogRepository(db).initialize()
            val reopened=LocalLibraryRepository(context,db)
            assertNotNull(reopened.cover(imported.fileId))
            assertEquals(ReadingLocation(0,.4),reopened.file(imported.fileId).location)
            assertEquals(count,db.catalog().worksOnce().size)
            assertTrue(db.catalog().worksOnce().first{it.work.id=="rigveda"}.favorites.isNotEmpty())
            assertEquals("Texto pessoal",db.notes().find(note)!!.body)
            assertNotNull(reopened.document(imported.fileId))
        }finally{db.close();context.deleteDatabase(name);file.delete()}
    }
}
