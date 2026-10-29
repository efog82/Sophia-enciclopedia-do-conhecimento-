package com.sophia.domain

import kotlinx.coroutines.flow.Flow
import java.text.Normalizer
import java.util.Locale

enum class ContributorRole(val label:String) {
    AUTHOR("Autor"), ATTRIBUTED_AUTHOR("Autoria atribuída"), TRADITIONAL_ATTRIBUTION("Atribuição tradicional"), PSYCHOGRAPHER("Psicógrafo"), TRANSLATOR("Tradutor"), EDITOR("Editor científico"), COMPILER("Compilador")
}
data class Contributor(val id:String,val name:String,val role:ContributorRole)
data class Tradition(val id:String,val name:String,val description:String,val historicalContext:String="",val concepts:String="",val sources:String="")
data class Category(val id:String,val name:String)
data class Edition(val id:String,val label:String?,val publisher:String?,val year:Int?,val language:String?,val translationId:String?,val fileCount:Int,val contributors:List<Contributor> = emptyList(),val rightsStatus:String="UNVERIFIED",val sourceUrl:String?=null)
data class Book(
    val id:String,val title:String,val description:String,val originalYear:Int?,val sourceUrl:String,val sourceLabel:String,
    val metadataNote:String,val addedAt:Long,val contributors:List<Contributor>,val traditions:List<Tradition>,val categories:List<Category>,val editions:List<Edition>,val favorite:Boolean
) {
    val allContributors:List<Contributor> get()=(contributors+editions.flatMap{it.contributors}).distinctBy{it.id to it.role}
    val contributorLabel:String get()=allContributors.joinToString(" · "){if(it.role==ContributorRole.AUTHOR)it.name else "${it.name} (${it.role.label.lowercase(Locale.forLanguageTag("pt-PT"))})"}.ifBlank{"Autoria coletiva ou não individualizada"}
}
data class StudyNote(val id:String,val title:String,val body:String,val workId:String?,val createdAt:Long,val updatedAt:Long,val fileId:String?=null,val locatorJson:String?=null,val excerpt:String?=null)
enum class ThemeMode { SYSTEM, LIGHT, DARK }
interface CatalogRepository {
    val books:Flow<List<Book>>
    val traditions:Flow<List<Tradition>>
    val categories:Flow<List<Category>>
    suspend fun initialize()
    suspend fun setFavorite(workId:String,favorite:Boolean)
}
interface NotesRepository {
    val notes:Flow<List<StudyNote>>
    suspend fun save(id:String?,title:String,body:String,workId:String?,fileId:String?=null,locatorJson:String?=null,excerpt:String?=null):String
    suspend fun delete(id:String)
}
interface PreferencesRepository { val theme:Flow<ThemeMode>; suspend fun setTheme(mode:ThemeMode);val readerFontSize:Flow<Int>;suspend fun setReaderFontSize(size:Int) }
object CatalogSearch {
    fun normalized(text:String):String=Normalizer.normalize(text,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT).trim()
    fun filter(books:List<Book>,query:String="",traditionId:String?=null,authorId:String?=null,categoryId:String?=null,favoritesOnly:Boolean=false):List<Book>{
        val terms=normalized(query).split(Regex("\\s+")).filter{it.isNotEmpty()}
        return books.filter{book->
            (!favoritesOnly||book.favorite)&&(traditionId==null||book.traditions.any{it.id==traditionId})&&
                (authorId==null||book.allContributors.any{it.id==authorId})&&(categoryId==null||book.categories.any{it.id==categoryId})&&
                terms.all{term->normalized(listOf(book.title,book.description,book.contributorLabel,book.traditions.joinToString(" "){it.name},book.categories.joinToString(" "){it.name}).joinToString(" ")).contains(term)}
        }
    }
}
object NoteValidation {
    fun clean(title:String,body:String):Pair<String,String>{
        val cleanTitle=title.trim();val cleanBody=body
        require(cleanTitle.isNotEmpty()){ "Escreva um título para a nota." }
        require(cleanTitle.length<=160){ "O título pode ter até 160 caracteres." }
        require(cleanBody.length<=50000){ "A nota pode ter até 50 000 caracteres." }
        return cleanTitle to cleanBody
    }
}
