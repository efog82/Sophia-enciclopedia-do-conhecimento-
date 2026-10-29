package com.sophia.enciclopedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sophia.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class LibraryUiState(val loading:Boolean=true,val data:LibrarySnapshot=LibrarySnapshot(),val error:String?=null)
class LibraryViewModel(private val repository:LibraryRepository):ViewModel(){
    private val retries=MutableStateFlow(0)
    @OptIn(ExperimentalCoroutinesApi::class)
    val state=retries.flatMapLatest{repository.snapshot.map{LibraryUiState(false,it)}.catch{e->if(e is CancellationException)throw e;emit(LibraryUiState(false,error="Não foi possível abrir os ficheiros e coleções locais."))}}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),LibraryUiState())
    private val _importing=MutableStateFlow(false);val importing=_importing.asStateFlow()
    private val _message=MutableStateFlow<String?>(null);val message=_message.asStateFlow()
    private val _result=MutableStateFlow<ImportResult?>(null);val result=_result.asStateFlow()
    fun retry(){retries.value++}
    fun clearMessage(){_message.value=null}
    fun clearResult(){_result.value=null}
    fun importFile(request:ImportRequest){if(_importing.value)return;_importing.value=true;viewModelScope.launch{try{_result.value=repository.importFile(request);_message.value=if(_result.value!!.duplicate)"Este ficheiro já se encontra na biblioteca." else "Ficheiro importado. Já pode ser lido offline."}catch(e:CancellationException){throw e}catch(e:Exception){_message.value=when(e){is SecurityException->"O Android já não permite aceder ao ficheiro. Volte a selecioná-lo.";is java.util.zip.ZipException->"O EPUB está danificado ou não contém um arquivo ZIP válido.";is java.io.IOException->"Falha de leitura ou armazenamento. Verifique o ficheiro e o espaço disponível e volte a tentar.";else->e.message?:"Não foi possível importar o ficheiro. Volte a selecioná-lo."}}finally{_importing.value=false}}}
    private fun operation(action:suspend ()->Unit){viewModelScope.launch{try{action()}catch(e:CancellationException){throw e}catch(e:Exception){_message.value=e.message?:"Não foi possível atualizar a coleção."}}}
    fun createCollection(name:String,readingList:Boolean)=operation{repository.createCollection(name,readingList)}
    fun deleteCollection(id:String)=operation{repository.deleteCollection(id)}
    fun setInCollection(id:String,work:String,included:Boolean)=operation{repository.setInCollection(id,work,included)}
    fun move(id:String,work:String,delta:Int)=operation{repository.moveInCollection(id,work,delta)}
}
