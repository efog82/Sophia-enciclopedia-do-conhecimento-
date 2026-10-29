package com.sophia.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sophia.domain.ThemeMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    private lateinit var context:Context
    private lateinit var db:SophiaDatabase
    private lateinit var name:String
    @Before fun setup(){context=ApplicationProvider.getApplicationContext();name="test-${UUID.randomUUID()}.db";db=SophiaDatabase.open(context,name)}
    @After fun cleanup(){db.close();context.deleteDatabase(name)}
    @Test fun seedIsIdempotentAndContainsNoBookFiles()=runBlocking{
        DemoCatalog.seed(db);DemoCatalog.seed(db)
        val books=db.catalog().worksOnce();assertEquals(6,books.size);assertTrue(books.all{it.editions.isEmpty()});assertEquals("1",db.catalog().metadata("demo_catalog_version"))
    }
    @Test fun favoritesSurviveDatabaseReopen()=runBlocking{
        val repo=RoomCatalogRepository(db);repo.initialize();repo.setFavorite("spirits",true)
        db.close();db=SophiaDatabase.open(context,name)
        assertTrue(db.catalog().worksOnce().first{it.work.id=="spirits"}.favorites.isNotEmpty())
        RoomCatalogRepository(db).setFavorite("spirits",false);assertTrue(db.catalog().worksOnce().all{it.favorites.isEmpty()})
    }
    @Test fun notesCreateEditReopenAndDelete()=runBlocking{
        DemoCatalog.seed(db);var repo=RoomNotesRepository(db)
        val id=repo.save(null," Estudo ","  apontamento\n","spirits");val original=repo.notes.first().single()
        assertEquals("Estudo",original.title);assertEquals("  apontamento\n",original.body)
        assertEquals(id,repo.save(id,"Revisão","Alterada",null))
        db.close();db=SophiaDatabase.open(context,name);repo=RoomNotesRepository(db)
        val changed=repo.notes.first().single();assertEquals(original.createdAt,changed.createdAt);assertEquals("Revisão",changed.title);assertNull(changed.workId)
        repo.delete(id);assertTrue(repo.notes.first().isEmpty())
    }
    @Test fun noteSurvivesWorkDeletionWithoutDanglingReference()=runBlocking{
        DemoCatalog.seed(db);val repo=RoomNotesRepository(db);val id=repo.save(null,"Reflexão","Pessoal","spirits")
        db.catalog().deleteWork("spirits");assertNull(db.notes().find(id)!!.workId)
    }
    @Test fun editionCannotUseTranslationOfAnotherWork()=runBlocking{
        DemoCatalog.seed(db);db.catalog().insertTranslation(TranslationEntity(id="t",workId="spirits"))
        var rejected=false
        try{db.catalog().insertEdition(EditionEntity(id="e",workId="nosso-lar",translationId="t"))}catch(e:android.database.sqlite.SQLiteConstraintException){rejected=true}
        assertTrue("A tradução deve pertencer à mesma obra",rejected)
    }
    @Test fun readingProgressBelongsToAFileNotToTheOriginalWork()=runBlocking{
        DemoCatalog.seed(db);db.catalog().insertEdition(EditionEntity(id="e",workId="spirits"))
        db.catalog().insertFile(BookFileEntity(id="f",editionId="e",uri="content://test/file",mimeType="application/epub+zip"))
        db.catalog().saveProgress(ReadingProgressEntity("f",null,500,1))
        assertEquals(500,db.catalog().progress("f")!!.progressPermille)
        var rejected=false;try{db.catalog().saveProgress(ReadingProgressEntity("f",null,1001,2))}catch(e:IllegalArgumentException){rejected=true};assertTrue(rejected)
    }
    @Test fun themePersistsAfterDataStoreReopen()=runBlocking{
        val prefName="test-${UUID.randomUUID()}"
        val firstScope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        val first=DataStorePreferences(context,firstScope,prefName);assertEquals(ThemeMode.SYSTEM,first.theme.first());first.setTheme(ThemeMode.DARK)
        firstScope.coroutineContext[Job]!!.cancelAndJoin()
        val secondScope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        try{assertEquals(ThemeMode.DARK,DataStorePreferences(context,secondScope,prefName).theme.first())}finally{secondScope.coroutineContext[Job]!!.cancelAndJoin();java.io.File(context.filesDir,"datastore/$prefName.preferences_pb").delete()}
    }
}
