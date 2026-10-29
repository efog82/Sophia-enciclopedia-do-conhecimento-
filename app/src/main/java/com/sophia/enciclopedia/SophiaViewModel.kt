package com.sophia.enciclopedia

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sophia.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class Section(val label:String,val title:String){HOME("Início","Sophia"),LIBRARY("Biblioteca","Biblioteca"),RESEARCH("Investigar","Investigar"),AI("IA","Assistente IA"),NOTES("Caderno","Caderno de Estudos")}
data class Filters(val libraryQuery:String="",val tradition:String?=null,val category:String?=null,val favorites:Boolean=false,val researchQuery:String="",val researchTradition:String?=null,val author:String?=null,val notesQuery:String="")
data class SophiaState(val loading:Boolean=true,val error:String?=null,val books:List<Book> = emptyList(),val notes:List<StudyNote> = emptyList(),val traditions:List<Tradition> = emptyList(),val categories:List<Category> = emptyList(),val theme:ThemeMode=ThemeMode.SYSTEM,val filters:Filters=Filters()) {
    val library get()=CatalogSearch.filter(books,filters.libraryQuery,filters.tradition,categoryId=filters.category,favoritesOnly=filters.favorites)
    val research get()=CatalogSearch.filter(books,filters.researchQuery,filters.researchTradition,filters.author)
    val filteredNotes get()=notes.filter{CatalogSearch.normalized(it.title+" "+it.body).contains(CatalogSearch.normalized(filters.notesQuery))}
}
data class NoteEditorState(val open:Boolean=false,val id:String?=null,val title:String="",val body:String="",val workId:String?=null,val fileId:String?=null,val locatorJson:String?=null,val excerpt:String?=null)

@OptIn(ExperimentalCoroutinesApi::class)
class SophiaViewModel(private val catalog:CatalogRepository,private val notesRepo:NotesRepository,private val preferences:PreferencesRepository,private val saved:SavedStateHandle):ViewModel(){
    private val retry=MutableStateFlow(0)
    private val filters=MutableStateFlow(Filters(saved["libraryQuery"]?:"",saved["tradition"],saved["category"],saved["favorites"]?:false,saved["researchQuery"]?:"",saved["researchTradition"],saved["author"],saved["notesQuery"]?:""))
    private val content:Flow<SophiaState> = retry.flatMapLatest {flow {
        emit(SophiaState())
        catalog.initialize()
        emitAll(combine(catalog.books,notesRepo.notes,catalog.traditions,catalog.categories,preferences.theme){books,notes,traditions,categories,theme->SophiaState(false,books=books,notes=notes,traditions=traditions,categories=categories,theme=theme)})
    }.catch{e->if(e is CancellationException)throw e;emit(SophiaState(loading=false,error="Não foi possível abrir os dados locais. Pode tentar novamente. Nenhum restauro destrutivo foi efetuado."))}}
    val state=combine(content,filters){data,f->data.copy(filters=f)}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),SophiaState())
    val section=saved.getStateFlow("section",Section.HOME.name)
    val selectedWork=saved.getStateFlow<String?>("work",null)
    val editor=combine(saved.getStateFlow("editorOpen",false),saved.getStateFlow<String?>("noteId",null),saved.getStateFlow("noteTitle",""),saved.getStateFlow("noteBody",""),saved.getStateFlow<String?>("noteWork",null)){open,id,title,body,work->NoteEditorState(open,id,title,body,work,saved["noteFile"],saved["noteLocator"],saved["noteExcerpt"])}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),NoteEditorState())
    private val _saving=MutableStateFlow(false);val saving=_saving.asStateFlow()
    private val _editorError=MutableStateFlow<String?>(null);val editorError=_editorError.asStateFlow()
    private val _feedback=MutableStateFlow<String?>(null);val feedback=_feedback.asStateFlow()
    private val _favoriteBusy=MutableStateFlow<Set<String>>(emptySet());val favoriteBusy=_favoriteBusy.asStateFlow()
    fun retry(){retry.value++}
    fun navigate(s:Section){saved["section"]=s.name;saved["work"]=null}
    fun openBook(id:String){saved["work"]=id}
    fun closeBook(){saved["work"]=null}
    fun setFilters(transform:(Filters)->Filters){
        val f=transform(filters.value);filters.value=f
        saved["libraryQuery"]=f.libraryQuery;saved["tradition"]=f.tradition;saved["category"]=f.category;saved["favorites"]=f.favorites;saved["researchQuery"]=f.researchQuery;saved["researchTradition"]=f.researchTradition;saved["author"]=f.author;saved["notesQuery"]=f.notesQuery
    }
    fun chooseCategory(id:String){setFilters{it.copy(category=id,libraryQuery="",tradition=null,favorites=false)};navigate(Section.LIBRARY)}
    fun searchFromHome(query:String){setFilters{it.copy(libraryQuery=query,category=null,tradition=null,favorites=false)};navigate(Section.LIBRARY)}
    fun toggleFavorite(book:Book){
        if(book.id in _favoriteBusy.value)return
        _favoriteBusy.value+=book.id
        viewModelScope.launch{try{catalog.setFavorite(book.id,!book.favorite)}catch(e:CancellationException){throw e}catch(e:Exception){_feedback.value="Não foi possível atualizar o favorito. Tente novamente."}finally{_favoriteBusy.value-=book.id}}
    }
    fun theme(mode:ThemeMode){viewModelScope.launch{try{preferences.setTheme(mode)}catch(e:CancellationException){throw e}catch(e:Exception){_feedback.value="Não foi possível guardar o tema."}}}
    fun clearFeedback(){_feedback.value=null}
    fun startNote(note:StudyNote?=null,workId:String?=null){
        _editorError.value=null;saved["noteFile"]=note?.fileId;saved["noteLocator"]=note?.locatorJson;saved["noteExcerpt"]=note?.excerpt;saved["noteId"]=note?.id;saved["noteTitle"]=note?.title?:"";saved["noteBody"]=note?.body?:"";saved["noteWork"]=note?.workId?:workId;saved["editorOpen"]=true
    }
    fun editTitle(value:String){if(value.length<=160){saved["noteTitle"]=value;_editorError.value=null}else _editorError.value="O título admite até 160 caracteres."}
    fun editBody(value:String){if(value.length<=50000){saved["noteBody"]=value;_editorError.value=null}else _editorError.value="A nota admite até 50 000 caracteres."}
    fun editWork(id:String?){if(saved.get<String?>("noteWork")!=id){saved["noteFile"]=null;saved["noteLocator"]=null;saved["noteExcerpt"]=null};saved["noteWork"]=id}
    fun closeEditor(){if(!_saving.value){saved["editorOpen"]=false;_editorError.value=null}}
    fun editorDirty():Boolean {
        val e=editor.value;val old=state.value.notes.firstOrNull{it.id==e.id}
        return if(old==null)e.title.isNotBlank()||e.body.isNotBlank() else e.title!=old.title||e.body!=old.body||e.workId!=old.workId
    }
    fun saveNote(){
        if(_saving.value)return
        val e=editor.value;_saving.value=true;_editorError.value=null
        viewModelScope.launch{try{notesRepo.save(e.id,e.title,e.body,e.workId,e.fileId,e.locatorJson,e.excerpt);saved["editorOpen"]=false;_feedback.value="Nota guardada no dispositivo."}catch(x:CancellationException){throw x}catch(x:Exception){_editorError.value=if(x is IllegalArgumentException)x.message else "Não foi possível guardar a nota. O texto mantém-se disponível para tentar novamente."}finally{_saving.value=false}}
    }
    fun deleteNote(id:String){viewModelScope.launch{try{notesRepo.delete(id);_feedback.value="Nota eliminada."}catch(e:CancellationException){throw e}catch(e:Exception){_feedback.value="Não foi possível eliminar a nota."}}}
}
