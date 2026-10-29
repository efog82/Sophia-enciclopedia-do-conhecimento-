package com.sophia.data

import androidx.room.*

@Entity(tableName="works")
data class WorkEntity(@PrimaryKey val id:String,val title:String,val description:String,val originalLanguage:String?=null,val originalYear:Int?=null,val sourceUrl:String,val sourceLabel:String,val metadataNote:String,val addedAt:Long,val demo:Boolean=true)
@Entity(tableName="people")
data class PersonEntity(@PrimaryKey val id:String,val displayName:String,val kind:String="PERSON")
@Entity(tableName="traditions")
data class TraditionEntity(@PrimaryKey val id:String,val name:String,val description:String)
@Entity(tableName="categories")
data class CategoryEntity(@PrimaryKey val id:String,val name:String)
@Entity(tableName="work_contributors",primaryKeys=["workId","personId","role"],foreignKeys=[ForeignKey(entity=WorkEntity::class,parentColumns=["id"],childColumns=["workId"],onDelete=ForeignKey.CASCADE),ForeignKey(entity=PersonEntity::class,parentColumns=["id"],childColumns=["personId"],onDelete=ForeignKey.RESTRICT)],indices=[Index("personId")])
data class WorkContributorEntity(val workId:String,val personId:String,val role:String)
@Entity(tableName="work_traditions",primaryKeys=["workId","traditionId"],foreignKeys=[ForeignKey(entity=WorkEntity::class,parentColumns=["id"],childColumns=["workId"],onDelete=ForeignKey.CASCADE),ForeignKey(entity=TraditionEntity::class,parentColumns=["id"],childColumns=["traditionId"],onDelete=ForeignKey.RESTRICT)],indices=[Index("traditionId")])
data class WorkTraditionEntity(val workId:String,val traditionId:String)
@Entity(tableName="work_categories",primaryKeys=["workId","categoryId"],foreignKeys=[ForeignKey(entity=WorkEntity::class,parentColumns=["id"],childColumns=["workId"],onDelete=ForeignKey.CASCADE),ForeignKey(entity=CategoryEntity::class,parentColumns=["id"],childColumns=["categoryId"],onDelete=ForeignKey.RESTRICT)],indices=[Index("categoryId")])
data class WorkCategoryEntity(val workId:String,val categoryId:String)
@Entity(tableName="translations",foreignKeys=[ForeignKey(entity=WorkEntity::class,parentColumns=["id"],childColumns=["workId"],onDelete=ForeignKey.CASCADE)],indices=[Index("workId"),Index(value=["id","workId"],unique=true)])
data class TranslationEntity(@PrimaryKey val id:String,val workId:String,val sourceLanguage:String?,val targetLanguage:String?,val note:String?,val sourceUrl:String?)
@Entity(tableName="editions",foreignKeys=[ForeignKey(entity=WorkEntity::class,parentColumns=["id"],childColumns=["workId"],onDelete=ForeignKey.CASCADE),ForeignKey(entity=TranslationEntity::class,parentColumns=["id","workId"],childColumns=["translationId","workId"],onDelete=ForeignKey.RESTRICT)],indices=[Index("workId"),Index(value=["translationId","workId"])])
data class EditionEntity(@PrimaryKey val id:String,val workId:String,val translationId:String?=null,val label:String?=null,val publisher:String?=null,val publicationYear:Int?=null,val isbn:String?=null,val language:String?=null,val sourceUrl:String?=null)
@Entity(tableName="edition_contributors",primaryKeys=["editionId","personId","role"],foreignKeys=[ForeignKey(entity=EditionEntity::class,parentColumns=["id"],childColumns=["editionId"],onDelete=ForeignKey.CASCADE),ForeignKey(entity=PersonEntity::class,parentColumns=["id"],childColumns=["personId"],onDelete=ForeignKey.RESTRICT)],indices=[Index("personId")])
data class EditionContributorEntity(val editionId:String,val personId:String,val role:String)
@Entity(tableName="book_files",foreignKeys=[ForeignKey(entity=EditionEntity::class,parentColumns=["id"],childColumns=["editionId"],onDelete=ForeignKey.CASCADE)],indices=[Index("editionId")])
data class BookFileEntity(@PrimaryKey val id:String,val editionId:String,val uri:String,val mimeType:String,val sha256:String?=null,val rightsStatus:String="UNVERIFIED",val rightsSourceUrl:String?=null,val sizeBytes:Long?=null)
@Entity(tableName="favorites",foreignKeys=[ForeignKey(entity=WorkEntity::class,parentColumns=["id"],childColumns=["workId"],onDelete=ForeignKey.CASCADE)])
data class FavoriteEntity(@PrimaryKey val workId:String,val addedAt:Long)
@Entity(tableName="reading_progress",foreignKeys=[ForeignKey(entity=BookFileEntity::class,parentColumns=["id"],childColumns=["fileId"],onDelete=ForeignKey.CASCADE)])
data class ReadingProgressEntity(@PrimaryKey val fileId:String,val locatorJson:String?,val progressPermille:Int,val updatedAt:Long)
@Entity(tableName="study_notes",foreignKeys=[ForeignKey(entity=WorkEntity::class,parentColumns=["id"],childColumns=["workId"],onDelete=ForeignKey.SET_NULL)],indices=[Index("workId")])
data class NoteEntity(@PrimaryKey val id:String,val title:String,val body:String,val workId:String?,val createdAt:Long,val updatedAt:Long)
@Entity(tableName="catalog_metadata")
data class CatalogMetadataEntity(@PrimaryKey val key:String,val value:String)

data class CreditRelation(@Embedded val credit:WorkContributorEntity,@Relation(parentColumn="personId",entityColumn="id") val person:PersonEntity)
data class EditionRelation(@Embedded val edition:EditionEntity,@Relation(parentColumn="id",entityColumn="editionId") val files:List<BookFileEntity>)
data class WorkRelations(
    @Embedded val work:WorkEntity,
    @Relation(entity=WorkContributorEntity::class,parentColumn="id",entityColumn="workId") val credits:List<CreditRelation>,
    @Relation(parentColumn="id",entityColumn="id",associateBy=Junction(value=WorkTraditionEntity::class,parentColumn="workId",entityColumn="traditionId")) val traditions:List<TraditionEntity>,
    @Relation(parentColumn="id",entityColumn="id",associateBy=Junction(value=WorkCategoryEntity::class,parentColumn="workId",entityColumn="categoryId")) val categories:List<CategoryEntity>,
    @Relation(entity=EditionEntity::class,parentColumn="id",entityColumn="workId") val editions:List<EditionRelation>,
    @Relation(parentColumn="id",entityColumn="workId") val favorites:List<FavoriteEntity>
)
