package com.sophia.domain

import kotlinx.coroutines.flow.Flow

/** Locators refer to a file edition, never to an abstract original work. */
data class ReadingLocation(val unit:Int=0,val fraction:Double=0.0) {
    init { require(unit>=0);require(fraction.isFinite() && fraction in 0.0..1.0) }
}
data class LibraryFile(val id:String,val workId:String,val title:String,val displayName:String,val mimeType:String,val sizeBytes:Long,val sha256:String,val rightsStatus:String,val addedAt:Long,val location:ReadingLocation?=null,val progressPermille:Int=0,val updatedAt:Long=0)
data class Bookmark(val id:String,val fileId:String,val label:String,val location:ReadingLocation,val createdAt:Long)
data class StudyCollection(val id:String,val name:String,val readingList:Boolean,val workIds:List<String>)
data class LibrarySnapshot(val files:List<LibraryFile> = emptyList(),val collections:List<StudyCollection> = emptyList())
data class ImportRequest(val sourceUri:String,val workId:String?=null,val title:String="",val authorised:Boolean=false)
data class ImportResult(val fileId:String,val workId:String,val duplicate:Boolean)
interface LibraryRepository {
    val snapshot:Flow<LibrarySnapshot>
    suspend fun importFile(request:ImportRequest):ImportResult
    suspend fun file(id:String):LibraryFile
    suspend fun savePosition(fileId:String,location:ReadingLocation,progressPermille:Int)
    fun bookmarks(fileId:String):Flow<List<Bookmark>>
    suspend fun addBookmark(fileId:String,label:String,location:ReadingLocation)
    suspend fun deleteBookmark(id:String)
    suspend fun createCollection(name:String,readingList:Boolean)
    suspend fun deleteCollection(id:String)
    suspend fun setInCollection(collectionId:String,workId:String,included:Boolean)
    suspend fun moveInCollection(collectionId:String,workId:String,delta:Int)
}
