package com.sophia.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CatalogDao {
    @Transaction @Query("SELECT * FROM works ORDER BY addedAt DESC, title ASC") abstract fun observeWorks():Flow<List<WorkRelations>>
    @Transaction @Query("SELECT * FROM works ORDER BY id") abstract suspend fun worksOnce():List<WorkRelations>
    @Query("SELECT * FROM traditions ORDER BY name") abstract fun observeTraditions():Flow<List<TraditionEntity>>
    @Query("SELECT * FROM categories ORDER BY name") abstract fun observeCategories():Flow<List<CategoryEntity>>
    @Query("SELECT value FROM catalog_metadata WHERE `key` = :key") abstract suspend fun metadata(key:String):String?
    @Insert(onConflict=OnConflictStrategy.IGNORE) abstract suspend fun insertWorks(rows:List<WorkEntity>)
    @Insert(onConflict=OnConflictStrategy.IGNORE) abstract suspend fun insertPeople(rows:List<PersonEntity>)
    @Insert(onConflict=OnConflictStrategy.IGNORE) abstract suspend fun insertTraditions(rows:List<TraditionEntity>)
    @Insert(onConflict=OnConflictStrategy.IGNORE) abstract suspend fun insertCategories(rows:List<CategoryEntity>)
    @Insert(onConflict=OnConflictStrategy.IGNORE) abstract suspend fun insertCredits(rows:List<WorkContributorEntity>)
    @Insert(onConflict=OnConflictStrategy.IGNORE) abstract suspend fun insertWorkTraditions(rows:List<WorkTraditionEntity>)
    @Insert(onConflict=OnConflictStrategy.IGNORE) abstract suspend fun insertWorkCategories(rows:List<WorkCategoryEntity>)
    @Upsert abstract suspend fun saveMetadata(value:CatalogMetadataEntity)
    @Insert(onConflict=OnConflictStrategy.IGNORE) abstract suspend fun addFavorite(value:FavoriteEntity)
    @Query("DELETE FROM favorites WHERE workId=:id") abstract suspend fun removeFavorite(id:String)
    @Insert abstract suspend fun insertTranslation(value:TranslationEntity)
    @Insert abstract suspend fun insertEdition(value:EditionEntity)
    @Insert abstract suspend fun insertEditionCredit(value:EditionContributorEntity)
    @Insert abstract suspend fun insertFile(value:BookFileEntity)
    @Query("SELECT * FROM book_files WHERE id=:id") abstract suspend fun file(id:String):BookFileEntity?
    @Upsert protected abstract suspend fun upsertProgress(value:ReadingProgressEntity)
    suspend fun saveProgress(value:ReadingProgressEntity){require(value.progressPermille in 0..1000);upsertProgress(value)}
    @Query("SELECT * FROM reading_progress WHERE fileId=:id") abstract suspend fun progress(id:String):ReadingProgressEntity?
    @Query("DELETE FROM works WHERE id=:id") abstract suspend fun deleteWork(id:String)
}
@Dao
interface NotesDao {
    @Query("SELECT * FROM study_notes ORDER BY updatedAt DESC, id") fun observe():Flow<List<NoteEntity>>
    @Query("SELECT * FROM study_notes WHERE id=:id") suspend fun find(id:String):NoteEntity?
    @Upsert suspend fun save(value:NoteEntity)
    @Query("DELETE FROM study_notes WHERE id=:id") suspend fun delete(id:String)
}
@Database(entities=[WorkEntity::class,PersonEntity::class,TraditionEntity::class,CategoryEntity::class,WorkContributorEntity::class,WorkTraditionEntity::class,WorkCategoryEntity::class,TranslationEntity::class,EditionEntity::class,EditionContributorEntity::class,BookFileEntity::class,FavoriteEntity::class,ReadingProgressEntity::class,NoteEntity::class,CatalogMetadataEntity::class],version=1,exportSchema=true)
abstract class SophiaDatabase:RoomDatabase() {
    abstract fun catalog():CatalogDao
    abstract fun notes():NotesDao
    companion object {
        fun open(context:Context,name:String="sophia.db"):SophiaDatabase=Room.databaseBuilder(context.applicationContext,SophiaDatabase::class.java,name).addMigrations(*DatabaseMigrations.ALL).build()
    }
}
/** First schema: no fictitious upgrade. Future migrations must be explicit, exported and tested. */
object DatabaseMigrations { val ALL:Array<Migration> = emptyArray() }
