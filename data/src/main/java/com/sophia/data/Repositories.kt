package com.sophia.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.withTransaction
import com.sophia.domain.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.UUID

class RoomCatalogRepository(private val db:SophiaDatabase):CatalogRepository {
    override val books=db.catalog().observeWorks().map{rows->rows.map{row->Book(row.work.id,row.work.title,row.work.description,row.work.originalYear,row.work.sourceUrl,row.work.sourceLabel,row.work.metadataNote,row.work.addedAt,row.credits.map{Contributor(it.person.id,it.person.displayName,ContributorRole.valueOf(it.credit.role))}.sortedBy{it.role.ordinal},row.traditions.map{Tradition(it.id,it.name,it.description,it.historicalContext,it.concepts,it.sources)},row.categories.map{Category(it.id,it.name)},row.editions.map{e->Edition(e.edition.id,e.edition.label,e.edition.publisher,e.edition.publicationYear,e.edition.language,e.edition.translationId,e.files.size,e.credits.map{Contributor(it.person.id,it.person.displayName,ContributorRole.valueOf(it.credit.role))},e.edition.rightsStatus,e.edition.sourceUrl)},row.favorites.isNotEmpty())}}
    override val traditions=db.catalog().observeTraditions().map{rows->rows.map{Tradition(it.id,it.name,it.description,it.historicalContext,it.concepts,it.sources)}}
    override val categories=db.catalog().observeCategories().map{rows->rows.map{Category(it.id,it.name)}}
    override suspend fun initialize(){DemoCatalog.seed(db);ExpandedCatalog.seed(db)}
    override suspend fun setFavorite(workId:String,favorite:Boolean){if(favorite)db.catalog().addFavorite(FavoriteEntity(workId,System.currentTimeMillis())) else db.catalog().removeFavorite(workId)}
}
class RoomNotesRepository(private val db:SophiaDatabase):NotesRepository {
    override val notes=db.notes().observe().map{rows->rows.map{StudyNote(it.id,it.title,it.body,it.workId,it.createdAt,it.updatedAt,it.fileId,it.locatorJson,it.excerpt)}}
    override suspend fun save(id:String?,title:String,body:String,workId:String?,fileId:String?,locatorJson:String?,excerpt:String?):String {
        val cleaned=NoteValidation.clean(title,body);val target=id?:UUID.randomUUID().toString()
        db.withTransaction {
            val old=if(id==null)null else db.notes().find(id)
            require(id==null||old!=null){"Esta nota já não existe. Volte ao caderno."}
            if(fileId!=null){val f=db.library().fileOnce(fileId)?:error("O ficheiro associado já não existe.");require(f.edition.workId==workId){"A passagem deve pertencer à mesma obra."};require(locatorJson!=null);LocationCodec.decode(locatorJson)}
            val now=System.currentTimeMillis()
            db.notes().save(NoteEntity(target,cleaned.first,cleaned.second,workId,old?.createdAt?:now,now,fileId,locatorJson,excerpt?.takeIf{it.isNotBlank()}))
        }
        return target
    }
    override suspend fun delete(id:String)=db.notes().delete(id)
}
class DataStorePreferences(context:Context,scope:CoroutineScope=CoroutineScope(SupervisorJob()+Dispatchers.IO),name:String="sophia") : PreferencesRepository {
    private val store=PreferenceDataStoreFactory.create(corruptionHandler=ReplaceFileCorruptionHandler{emptyPreferences()},scope=scope,produceFile={context.applicationContext.preferencesDataStoreFile(name)})
    private val key=stringPreferencesKey("theme")
    override val theme:Flow<ThemeMode> = store.data.catch{e->if(e is IOException)emit(emptyPreferences()) else throw e}.map{prefs->runCatching{ThemeMode.valueOf(prefs[key]?:"SYSTEM")}.getOrDefault(ThemeMode.SYSTEM)}
    override suspend fun setTheme(mode:ThemeMode){store.edit{it[key]=mode.name}}
}
