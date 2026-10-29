package com.sophia.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="study_conversations")
data class ConversationEntity(@PrimaryKey val id:String,val title:String,val createdAt:Long,val updatedAt:Long,val mode:String="Explicar",val sourceTitle:String?=null,val sourceLocator:String?=null,val sourceExcerpt:String?=null)
@Entity(tableName="study_messages",foreignKeys=[ForeignKey(entity=ConversationEntity::class,parentColumns=["id"],childColumns=["conversationId"],onDelete=ForeignKey.CASCADE)],indices=[Index("conversationId")])
data class MessageEntity(@PrimaryKey val id:String,val conversationId:String,val role:String,val content:String,val createdAt:Long,val provider:String?=null,val model:String?=null,val contextReduced:Boolean=false,val diagnostic:String="")
@Entity(tableName="study_providers")
data class ProviderEntity(@PrimaryKey val id:String,val enabled:Boolean=false,val freeAccountConfirmed:Boolean=false,val error:String="",val quotaInfo:String="")
@Entity(tableName="study_models",primaryKeys=["provider","modelId"])
data class ModelEntity(val provider:String,val modelId:String,val label:String,val contextTokens:Int=4096,val validatedAt:Long=0,val cooldownUntil:Long=0,val preferred:Boolean=false)
@Entity(tableName="text_annotations",foreignKeys=[ForeignKey(entity=BookFileEntity::class,parentColumns=["id"],childColumns=["fileId"],onDelete=ForeignKey.CASCADE)],indices=[Index("fileId")])
data class AnnotationEntity(@PrimaryKey val id:String,val fileId:String,val locatorJson:String,val excerpt:String,val style:String,val color:Int,val note:String,val createdAt:Long)
@Dao abstract class StudyDao {
    @Query("SELECT * FROM study_conversations ORDER BY updatedAt DESC") abstract fun conversations():Flow<List<ConversationEntity>>
    @Query("SELECT * FROM study_conversations WHERE id=:id") abstract suspend fun conversation(id:String):ConversationEntity?
    @Query("SELECT * FROM study_messages WHERE conversationId=:id ORDER BY createdAt,id") abstract fun messages(id:String):Flow<List<MessageEntity>>
    @Query("SELECT * FROM study_messages WHERE conversationId=:id ORDER BY createdAt,id") abstract suspend fun messagesOnce(id:String):List<MessageEntity>
    @Upsert abstract suspend fun saveConversation(value:ConversationEntity)
    @Insert abstract suspend fun insertMessage(value:MessageEntity)
    @Query("DELETE FROM study_conversations WHERE id=:id") abstract suspend fun deleteConversation(id:String)
    @Query("SELECT * FROM study_providers ORDER BY id") abstract fun providers():Flow<List<ProviderEntity>>
    @Query("SELECT * FROM study_providers WHERE id=:id") abstract suspend fun provider(id:String):ProviderEntity?
    @Upsert abstract suspend fun saveProvider(value:ProviderEntity)
    @Query("SELECT * FROM study_models ORDER BY preferred DESC,provider,label") abstract fun models():Flow<List<ModelEntity>>
    @Query("SELECT * FROM study_models ORDER BY preferred DESC,provider,label") abstract suspend fun modelsOnce():List<ModelEntity>
    @Upsert abstract suspend fun saveModels(rows:List<ModelEntity>)
    @Query("DELETE FROM study_models WHERE provider=:id") abstract suspend fun clearModels(id:String)
    @Query("SELECT * FROM text_annotations WHERE fileId=:id ORDER BY createdAt DESC") abstract fun annotations(id:String):Flow<List<AnnotationEntity>>
    @Upsert abstract suspend fun saveAnnotation(value:AnnotationEntity)
    @Query("DELETE FROM text_annotations WHERE id=:id") abstract suspend fun deleteAnnotation(id:String)
}
