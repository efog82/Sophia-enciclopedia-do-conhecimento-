package com.sophia.enciclopedia

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sophia.data.*
import com.sophia.domain.*
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.*
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.epub.EpubParser

@OptIn(ExperimentalReadiumApi::class,FlowPreview::class)
class ReadiumViewModel(private val context:Context,private val c:AppContainer,val fileId:String,private val initial:String?):ViewModel(){
    data class Ready(val info:LibraryFile,val publication:Publication,val factory:EpubNavigatorFactory,val initial:Locator?,val titles:List<String>)
    private val _ready=MutableStateFlow<Ready?>(null);val ready=_ready.asStateFlow()
    private val _error=MutableStateFlow<String?>(null);val error=_error.asStateFlow()
    private val _message=MutableStateFlow<String?>(null);val message=_message.asStateFlow()
    val theme=c.preferences.theme.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),ThemeMode.SYSTEM)
    val font=c.preferences.readerFontSize.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),20)
    val annotations=c.database.study().annotations(fileId).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val bookmarks=c.library.bookmarks(fileId).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    private var current:Locator?=null
    private val positions=MutableStateFlow<Locator?>(null)
    init{load();viewModelScope.launch{positions.filterNotNull().debounce(400).collect{save(it)}}}
    fun load(){viewModelScope.launch{
        _error.value=null
        try{
            val info=c.library.file(fileId);val original=c.library.localFile(fileId)
            val secure=withContext(Dispatchers.IO){val dir=File(context.cacheDir,"readium-safe").apply{mkdirs()};File(dir,"${info.sha256}-safe1.epub").also{if(!it.isFile)ReadiumPackage.prepare(original,it)}}
            val offlineHttp=object:HttpClient{override suspend fun stream(request:HttpRequest):HttpTry<HttpStreamResponse> = Try.failure(HttpError.IO(IOException("O leitor só usa recursos locais.")))}
            val retriever=AssetRetriever(context.contentResolver,offlineHttp)
            val asset=retriever.retrieve(secure.toUrl()).getOrElse{error("Não foi possível abrir a edição no Readium.")}
            val publication=PublicationOpener(EpubParser()).open(asset,allowUserInteraction=false).getOrElse{error("O Readium não conseguiu interpretar esta edição EPUB.")}
            val old=initial?.let{runCatching{LocationCodec.decode(it)}.getOrNull()}?:info.location
            val locator=old?.readiumLocator?.let{runCatching{Locator.fromJSON(JSONObject(it))}.getOrNull()}?:old?.let{location->publication.readingOrder.getOrNull(location.unit)?.let{publication.locatorFromLink(it)}?.let{it.copy(locations=it.locations.copy(progression=location.fraction))}}
            current=locator
            _ready.value=Ready(info,publication,EpubNavigatorFactory(publication),locator,c.library.document(fileId).units.map{it.title})
        }catch(e:CancellationException){throw e}catch(e:Exception){_error.value=e.message?:"Não foi possível abrir o leitor EPUB."}
    }}
    fun initialLocator()=current?:_ready.value?.initial
    fun label(locator:Locator):String=locator.title?:_ready.value?.titles?.getOrNull(locationOf(locator).unit)?:"Passagem EPUB"
    fun location(locator:Locator){val named=if(locator.title==null)locator.copy(title=label(locator))else locator;current=named;positions.value=named}
    fun locationOf(locator:Locator):ReadingLocation {val pub=_ready.value!!.publication;val index=pub.readingOrder.indexOfFirst{it.href.toString().substringBefore('#')==locator.href.toString().substringBefore('#')}.coerceAtLeast(0);return ReadingLocation(index,(locator.locations.progression?:0.0).coerceIn(0.0,1.0),locator.toJSON().toString())}
    private suspend fun save(locator:Locator){try{c.library.savePosition(fileId,locationOf(locator),((locator.locations.totalProgression?:0.0)*1000).toInt().coerceIn(0,1000))}catch(e:CancellationException){throw e}catch(e:Exception){_message.value="Não foi possível guardar a posição de leitura."}}
    fun close(done:()->Unit){viewModelScope.launch{current?.let{save(it)};done()}}
    fun bookmark(){viewModelScope.launch{try{val locator=current?:return@launch;c.library.addBookmark(fileId,label(locator),locationOf(locator));_message.value="Marcador guardado."}catch(e:Exception){_message.value="Não foi possível guardar o marcador."}}}
    fun deleteBookmark(id:String){viewModelScope.launch{c.library.deleteBookmark(id)}}
    fun font(value:Int){viewModelScope.launch{c.preferences.setReaderFontSize(value.coerceIn(12,36))}}
    fun theme(dark:Boolean){viewModelScope.launch{c.preferences.setTheme(if(dark)ThemeMode.DARK else ThemeMode.LIGHT)}}
    fun clearMessage(){_message.value=null}
    suspend fun annotation(locator:Locator,style:String,color:Int,note:String=""){require(style in listOf("HIGHLIGHT","UNDERLINE"));val excerpt=locator.text.highlight.orEmpty();require(excerpt.isNotBlank()){"Seleciona uma passagem do livro primeiro."};c.database.study().saveAnnotation(AnnotationEntity(UUID.randomUUID().toString(),fileId,locator.toJSON().toString(),excerpt.take(10000),style,color,note.take(10000),System.currentTimeMillis()))}
    suspend fun update(annotation:AnnotationEntity,note:String){c.database.study().saveAnnotation(annotation.copy(note=note.take(10000)))}
    suspend fun deleteAnnotation(id:String){c.database.study().deleteAnnotation(id)}
    suspend fun note(locator:Locator?,title:String,body:String,excerpt:String){val l=locator?:current?:error("Aguarda a abertura da passagem.");c.notes.save(null,title,body,_ready.value!!.info.workId,fileId,LocationCodec.encode(locationOf(l)),excerpt)}
    fun source(locator:Locator):StudySource {val info=_ready.value!!.info;return StudySource(info.title,"${locator.title?:locator.href.toString()} · posição ${locator.locations.position?.toString()?:"não indicada"}",locator.text.highlight.orEmpty())}
    override fun onCleared(){_ready.value?.publication?.close();super.onCleared()}
}
