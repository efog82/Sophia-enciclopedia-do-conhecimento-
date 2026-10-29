package com.sophia.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao abstract class LibraryDao {
    @Transaction @Query("SELECT * FROM book_files ORDER BY addedAt DESC") abstract fun files():Flow<List<FileRelation>>
    @Transaction @Query("SELECT * FROM book_files WHERE id=:id") abstract suspend fun fileOnce(id:String):FileRelation?
    @Transaction @Query("SELECT * FROM book_files WHERE sha256=:hash") abstract suspend fun sameHash(hash:String):List<FileRelation>
    @Query("SELECT title FROM works WHERE id=:id") abstract suspend fun workTitle(id:String):String?
    @Query("UPDATE traditions SET historicalContext=:history,concepts=:concepts,sources=:sources WHERE id=:id") abstract suspend fun updateTradition(id:String,history:String,concepts:String,sources:String)
    @Query("SELECT * FROM bookmarks WHERE fileId=:id ORDER BY createdAt DESC") abstract fun bookmarks(id:String):Flow<List<BookmarkEntity>>
    @Insert(onConflict=OnConflictStrategy.IGNORE) abstract suspend fun addBookmark(value:BookmarkEntity)
    @Query("DELETE FROM bookmarks WHERE id=:id") abstract suspend fun deleteBookmark(id:String)
    @Transaction @Query("SELECT * FROM study_collections ORDER BY createdAt DESC") abstract fun collections():Flow<List<CollectionRelation>>
    @Insert abstract suspend fun addCollection(value:StudyCollectionEntity)
    @Query("DELETE FROM study_collections WHERE id=:id") abstract suspend fun deleteCollection(id:String)
    @Query("SELECT * FROM collection_works WHERE collectionId=:id ORDER BY position,workId") abstract suspend fun entries(id:String):List<CollectionWorkEntity>
    @Upsert abstract suspend fun setEntry(value:CollectionWorkEntity)
    @Query("DELETE FROM collection_works WHERE collectionId=:id AND workId=:workId") abstract suspend fun removeEntry(id:String,workId:String)
}
