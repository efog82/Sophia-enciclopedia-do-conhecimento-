package com.sophia.enciclopedia

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sophia.data.*
import com.sophia.domain.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ReaderState(val loading:Boolean=true,val info:LibraryFile?=null,val document:BookDocument?=null,val localFile:File?=null,val unit:Int=0,val fraction:Double=0.0,val bitmap:Bitmap?=null,val error:String?=null)
@OptIn(FlowPreview::class)
class ReaderViewModel(private val library:LocalLibraryRepository,private val notes:NotesRepository,private val preferences:PreferencesRepository,private val id:String,private val initial:String?):ViewModel(){
    private val _state=MutableStateFlow(ReaderState());val state=_state.asStateFlow()
    val theme=preferences.theme.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),ThemeMode.SYSTEM)
    val font=preferences.readerFontSize.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),20)
    val bookmarks=library.bookmarks(id).catch{_message.value="Não foi possível abrir os marcadores.";emit(emptyList())}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    private val _message=MutableStateFlow<String?>(null);val message=_message.asStateFlow()
    private val positions=MutableSharedFlow<ReadingLocation>(extraBufferCapacity=1,onBufferOverflow=BufferOverflow.DROP_OLDEST)
    private val writing=Mutex();private var rendering:Job?=null
    init{load();viewModelScope.launch{positions.debounce(400).collect{save(it)}}}
    fun load(){viewModelScope.launch{_state.value=ReaderState();try{
        val info=library.file(id);val document=library.document(id);val local=library.localFile(id)
        require(document.units.isNotEmpty()){"O ficheiro não contém páginas ou recursos de leitura."}
        val location=initial?.let{LocationCodec.decode(it)}?:info.location?:ReadingLocation()
        _state.value=ReaderState(false,info,document,local,location.unit.coerceAtMost(document.units.lastIndex),location.fraction)
        render();positions.tryEmit(current())
    }catch(e:CancellationException){throw e}catch(e:Exception){_state.value=ReaderState(false,error=e.message?:"Não foi possível abrir o livro.")}}}
    private fun current()=ReadingLocation(_state.value.unit,_state.value.fraction)
    private suspend fun save(location:ReadingLocation){try{writing.withLock{val count=_state.value.document?.units?.size?:return;library.savePosition(id,location,(((location.unit+location.fraction)/count)*1000).toInt().coerceIn(0,1000))}}catch(e:CancellationException){throw e}catch(e:Exception){_message.value="Não foi possível guardar a posição de leitura."}}
    fun scroll(fraction:Double){if(!_state.value.loading){_state.value=_state.value.copy(fraction=fraction.coerceIn(0.0,1.0));positions.tryEmit(current())}}
    fun go(unit:Int,fraction:Double=0.0){val count=_state.value.document?.units?.size?:return;if(unit !in 0 until count)return;_state.value=_state.value.copy(unit=unit,fraction=fraction.coerceIn(0.0,1.0),bitmap=null);render();positions.tryEmit(current())}
    private fun render(){rendering?.cancel();if(_state.value.info?.mimeType!="application/pdf")return;val unit=_state.value.unit;rendering=viewModelScope.launch{try{val bitmap=library.pdfPage(id,unit);if(_state.value.unit==unit)_state.value=_state.value.copy(bitmap=bitmap)}catch(e:CancellationException){throw e}catch(e:Exception){_message.value="Não foi possível renderizar esta página PDF."}}}
    fun bookmark(){viewModelScope.launch{try{val s=_state.value;library.addBookmark(id,s.document!!.units[s.unit].title,current());_message.value="Marcador guardado."}catch(e:CancellationException){throw e}catch(e:Exception){_message.value="Não foi possível guardar o marcador."}}}
    fun deleteBookmark(id:String){viewModelScope.launch{try{library.deleteBookmark(id)}catch(e:CancellationException){throw e}catch(e:Exception){_message.value="Não foi possível eliminar o marcador."}}}
    fun font(size:Int){viewModelScope.launch{try{preferences.setReaderFontSize(size.coerceIn(12,36))}catch(e:CancellationException){throw e}catch(e:Exception){_message.value="Não foi possível guardar o tamanho da letra."}}}
    fun theme(dark:Boolean){viewModelScope.launch{try{preferences.setTheme(if(dark)ThemeMode.DARK else ThemeMode.LIGHT)}catch(e:CancellationException){throw e}catch(e:Exception){_message.value="Não foi possível guardar o tema."}}}
    fun clearMessage(){_message.value=null}
    suspend fun saveNote(title:String,body:String,excerpt:String){val s=state.value;notes.save(null,title,body,s.info!!.workId,id,LocationCodec.encode(current()),excerpt)}
    fun close(onClosed:()->Unit){viewModelScope.launch{if(_state.value.document!=null)save(current());onClosed()}}
}
