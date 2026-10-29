package com.sophia.enciclopedia

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sophia.data.AnnotationEntity
import com.sophia.data.LocationCodec
import com.sophia.domain.ThemeMode
import com.sophia.enciclopedia.ui.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.*
import org.readium.r2.navigator.util.DirectionalNavigationAdapter
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.navigator.epub.*
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator

@OptIn(ExperimentalReadiumApi::class)
class ReadiumActivity:FragmentActivity(),EpubNavigatorFragment.Listener {
    val model:ReadiumViewModel by viewModels{viewModelFactory{initializer{ReadiumViewModel(applicationContext,(application as SophiaApplication).container,intent.getStringExtra("fileId")?:"",intent.getStringExtra("locator"))}}}
    var navigator:EpubNavigatorFragment? by mutableStateOf(null);private set
    private var pendingSelection by mutableStateOf<Locator?>(null)
    private var action by mutableStateOf<String?>(null)
    private var feedback by mutableStateOf<String?>(null)
    private var externalLink by mutableStateOf<String?>(null)
    override fun onExternalLinkActivated(url:AbsoluteUrl){if(url.toString().startsWith("https://")||url.toString().startsWith("http://"))externalLink=url.toString()}
    private var editing by mutableStateOf<AnnotationEntity?>(null)
    override fun onCreate(savedInstanceState:Bundle?){
        supportFragmentManager.fragmentFactory=EpubNavigatorFragment.createDummyFactory()
        super.onCreate(savedInstanceState)
        // Replace restored dummy fragments before onResume; actual locators come from the ViewModel/Room.
        supportFragmentManager.fragments.toList().takeIf{it.isNotEmpty()}?.let{old->supportFragmentManager.commitNow{old.forEach{remove(it)}}}
        setContent{Content()}
    }
    private fun selection(kind:String,mode:ActionMode?=null){lifecycleScope.launch{
        val selected=navigator?.currentSelection()?.locator
        if(selected?.text?.highlight.isNullOrBlank()){feedback="Seleciona uma passagem do texto com um toque prolongado primeiro.";return@launch}
        pendingSelection=selected;action=kind;mode?.finish()
        if(kind=="STUDY"){startActivity(StudyActivity.intent(this@ReadiumActivity,model.source(selected!!)));action=null;navigator?.clearSelection()}
    }}
    private val selectionMenu=object:ActionMode.Callback{
        override fun onCreateActionMode(mode:ActionMode,menu:Menu):Boolean{
            menu.add(0,101,1,"Destacar");menu.add(0,102,2,"Sublinhar");menu.add(0,103,3,"Criar nota");menu.add(0,104,4,"Estudar com IA");return true
        }
        override fun onPrepareActionMode(mode:ActionMode,menu:Menu)=false
        override fun onDestroyActionMode(mode:ActionMode){}
        override fun onActionItemClicked(mode:ActionMode,item:MenuItem):Boolean {
            val kind=when(item.itemId){101->"HIGHLIGHT";102->"UNDERLINE";103->"NOTE";104->"STUDY";else->return false};selection(kind,mode);return true
        }
    }
    private fun install(container:FragmentContainerView,ready:ReadiumViewModel.Ready,dark:Boolean,font:Int){
        val factory=ready.factory.createFragmentFactory(model.initialLocator(),initialPreferences=EpubPreferences(fontSize=font/20.0,theme=if(dark)Theme.DARK else Theme.LIGHT,scroll=false),listener=this,configuration=EpubNavigatorFragment.Configuration(selectionActionModeCallback=selectionMenu,shouldApplyInsetsPadding=false))
        supportFragmentManager.fragmentFactory=factory
        val fragment=factory.instantiate(classLoader,EpubNavigatorFragment::class.java.name) as EpubNavigatorFragment
        supportFragmentManager.commitNow{replace(container.id,fragment,"sophia-readium")}
        navigator=fragment
        fragment.addInputListener(DirectionalNavigationAdapter(fragment))
        fragment.addDecorationListener("sophia-highlights",object:DecorableNavigator.Listener{override fun onDecorationActivated(event:DecorableNavigator.OnActivatedEvent):Boolean{editing=model.annotations.value.firstOrNull{it.id==event.decoration.id};return editing!=null}})
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun Content(){
        val ready by model.ready.collectAsStateWithLifecycle();val error by model.error.collectAsStateWithLifecycle();val theme by model.theme.collectAsStateWithLifecycle();val font by model.font.collectAsStateWithLifecycle();val annotations by model.annotations.collectAsStateWithLifecycle();val bookmarks by model.bookmarks.collectAsStateWithLifecycle();val message by model.message.collectAsStateWithLifecycle()
        var current by remember{mutableStateOf<Locator?>(null)};var indexOpen by rememberSaveable{mutableStateOf(false)};var marksOpen by rememberSaveable{mutableStateOf(false)};var annotationsOpen by rememberSaveable{mutableStateOf(false)};var deleteId by rememberSaveable{mutableStateOf<String?>(null)}
        val dark=theme==ThemeMode.DARK||(theme==ThemeMode.SYSTEM&&isSystemInDarkTheme());val scope=rememberCoroutineScope()
        BackHandler{model.close{finish()}}
        SophiaTheme(theme){Scaffold(topBar={TopAppBar(title={Text(ready?.info?.title?:"Leitor EPUB",maxLines=1)},navigationIcon={IconButton(onClick={model.close{finish()}},modifier=Modifier.testTag("close-reader")){Icon(Icons.Outlined.ArrowBack,"Voltar à biblioteca")}},actions={IconButton(onClick={model.theme(!dark)},modifier=Modifier.testTag("reader-theme")){Icon(Icons.Outlined.DarkMode,"Alternar tema do leitor")}})}){padding->Column(Modifier.padding(padding).fillMaxSize().testTag("reader-screen")){
            if(error!=null){Column(Modifier.padding(20.dp)){EmptyPanel("Readium indisponível",error!!);Button(onClick=model::load){Text("Tentar novamente")};OutlinedButton(onClick={startActivity(ReaderActivity.intent(this@ReadiumActivity,model.fileId,intent.getStringExtra("locator")).putExtra("simple",true));finish()}){Text("Abrir no leitor simples")}}}
            else if(ready==null)CircularProgressIndicator(Modifier.padding(24.dp))
            else{
                val data=ready!!
                val location=current?.let{model.locationOf(it)}
                Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){TextButton(onClick={indexOpen=true},modifier=Modifier.weight(1f).testTag("reader-index")){Text("${(location?.unit?:0)+1}/${data.publication.readingOrder.size} · ${current?.let{model.label(it)}?:"A abrir…"}",maxLines=1)};TextButton(onClick={model.font(font-2)},enabled=font>12,modifier=Modifier.testTag("font-minus")){Text("A−")};Text("$font",Modifier.testTag("reader-font"));TextButton(onClick={model.font(font+2)},enabled=font<36,modifier=Modifier.testTag("font-plus")){Text("A+")}}
                AndroidView(factory={context->FragmentContainerView(context).apply{id=R.id.readium_container;post{if(!isFinishing&&navigator==null)install(this,data,dark,font)}}},modifier=Modifier.weight(1f).fillMaxWidth().testTag("epub-content"))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){IconButton(onClick={navigator?.goBackward(true)},modifier=Modifier.testTag("reader-previous")){Icon(Icons.Outlined.NavigateBefore,"Anterior")};IconButton(onClick=model::bookmark,modifier=Modifier.testTag("add-bookmark")){Icon(Icons.Outlined.BookmarkAdd,"Adicionar marcador")};IconButton(onClick={marksOpen=true},modifier=Modifier.testTag("open-bookmarks")){Icon(Icons.Outlined.Bookmarks,"Ver marcadores")};IconButton(onClick={pendingSelection=null;action="NOTE"},modifier=Modifier.testTag("reader-note")){Icon(Icons.Outlined.EditNote,"Criar nota nesta posição")};IconButton(onClick={annotationsOpen=true},modifier=Modifier.testTag("open-annotations")){Icon(Icons.Outlined.FormatColorHighlight,"Destaques e sublinhados")};IconButton(onClick={navigator?.goForward(true)},modifier=Modifier.testTag("reader-next")){Icon(Icons.Outlined.NavigateNext,"Seguinte")}}
                Text("Toque prolongado no texto: destacar, sublinhar, criar nota ou estudar com IA.",Modifier.padding(horizontal=12.dp),style=MaterialTheme.typography.labelSmall)
                val nav=navigator
                LaunchedEffect(nav){nav?.currentLocator?.collect{current=it;model.location(it)}}
                LaunchedEffect(nav,font,dark){nav?.submitPreferences(EpubPreferences(fontSize=font/20.0,theme=if(dark)Theme.DARK else Theme.LIGHT,scroll=false))}
                LaunchedEffect(nav,annotations){nav?.applyDecorations(annotations.mapNotNull{a->runCatching{Locator.fromJSON(JSONObject(a.locatorJson))?.let{l->Decoration(a.id,l,if(a.style=="UNDERLINE")Decoration.Style.Underline(a.color)else Decoration.Style.Highlight(a.color))}}.getOrNull()},"sophia-highlights")}
            }
        }}
        if(indexOpen)LibraryDialog("Índice EPUB",{indexOpen=false}){
            val publication=ready?.publication
            fun flattened(links:List<Link>,depth:Int=0):List<Pair<Link,Int>> = links.flatMap{listOf(it to depth)+flattened(it.children,depth+1)}
            val links=publication?.let{flattened(it.tableOfContents.ifEmpty{it.readingOrder})}.orEmpty()
            links.forEachIndexed{i,(link,depth)->TextButton(onClick={navigator?.go(link);indexOpen=false},modifier=Modifier.fillMaxWidth()){Text("  ".repeat(depth)+(link.title?:ready?.titles?.getOrNull(i)?:"Recurso ${i+1}"))}}
        }
        if(marksOpen)LibraryDialog("Marcadores",{marksOpen=false}){if(bookmarks.isEmpty())Text("Ainda não há marcadores neste ficheiro.");bookmarks.forEach{mark->Row{TextButton(onClick={val pub=ready?.publication;val locator=mark.location.readiumLocator?.let{Locator.fromJSON(JSONObject(it))}?:pub?.readingOrder?.getOrNull(mark.location.unit)?.let{pub.locatorFromLink(it)};locator?.let{navigator?.go(it)};marksOpen=false},modifier=Modifier.weight(1f)){Text(mark.label)};IconButton(onClick={model.deleteBookmark(mark.id)}){Icon(Icons.Outlined.DeleteOutline,"Eliminar marcador")}}}}
        if(annotationsOpen)LibraryDialog("Destaques e sublinhados",{annotationsOpen=false}){
            if(annotations.isEmpty())Text("Seleciona texto com um toque prolongado e escolhe Destacar ou Sublinhar.")
            annotations.forEach{a->OutlinedCard{Column(Modifier.padding(12.dp)){Text(a.excerpt);if(a.note.isNotBlank())Text("Comentário: ${a.note}");Row{TextButton(onClick={Locator.fromJSON(JSONObject(a.locatorJson))?.let{navigator?.go(it)};annotationsOpen=false}){Text("Ler passagem")};TextButton(onClick={editing=a}){Text("Comentário")};IconButton(onClick={deleteId=a.id}){Icon(Icons.Outlined.DeleteOutline,"Eliminar destaque")}}}}}
        }
        if(action in listOf("HIGHLIGHT","UNDERLINE"))LibraryDialog(if(action=="UNDERLINE")"Sublinhar passagem"else "Destacar passagem",{action=null}){
            Text(pendingSelection?.text?.highlight.orEmpty())
            listOf("Dourado" to 0xFFF4D35E.toInt(),"Azul" to 0xFF90CAF9.toInt(),"Violeta" to 0xFFCE93D8.toInt(),"Verde" to 0xFFA5D6A7.toInt()).forEach{(name,color)->Button(onClick={scope.launch{try{model.annotation(pendingSelection!!,action!!,color);navigator?.clearSelection();action=null}catch(e:Exception){feedback=e.message?:"Não foi possível guardar o destaque."}}}){Text(name)}}
        }
        if(action=="NOTE"){
            var title by rememberSaveable{mutableStateOf((ready?.info?.title.orEmpty()+" · "+current?.let{model.label(it)}.orEmpty()).take(160))};var body by rememberSaveable{mutableStateOf("")};var excerpt by rememberSaveable{mutableStateOf(pendingSelection?.text?.highlight.orEmpty())};var saving by remember{mutableStateOf(false)}
            LibraryDialog("Nota nesta passagem",{if(!saving)action=null}){OutlinedTextField(title,{if(it.length<=160)title=it},label={Text("Título")},modifier=Modifier.fillMaxWidth().testTag("passage-note-title"));OutlinedTextField(body,{if(it.length<=50000)body=it},label={Text("Apontamentos")},minLines=3,modifier=Modifier.fillMaxWidth().testTag("passage-note-body"));OutlinedTextField(excerpt,{if(it.length<=10000)excerpt=it},label={Text("Excerto selecionado ou copiado")},modifier=Modifier.fillMaxWidth());Button(onClick={saving=true;scope.launch{try{model.note(pendingSelection,title,body,excerpt);action=null;navigator?.clearSelection()}catch(e:Exception){feedback=e.message?:"Não foi possível guardar a nota."}finally{saving=false}}},enabled=!saving,modifier=Modifier.testTag("save-passage-note")){Text("Guardar no caderno")}}
        }
        editing?.let{a->var note by rememberSaveable(a.id){mutableStateOf(a.note)};LibraryDialog("Comentário do destaque",{editing=null}){Text(a.excerpt);OutlinedTextField(note,{if(it.length<=10000)note=it},label={Text("Comentário pessoal")},modifier=Modifier.fillMaxWidth());Button(onClick={scope.launch{model.update(a,note);editing=null}}){Text("Guardar comentário")};OutlinedButton(onClick={Locator.fromJSON(JSONObject(a.locatorJson))?.let{startActivity(StudyActivity.intent(this@ReadiumActivity,model.source(it)))}}){Text("Estudar esta passagem com IA")}}}
        deleteId?.let{id->AlertDialog(onDismissRequest={deleteId=null},title={Text("Eliminar destaque?")},text={Text("Será eliminado o destaque e o seu comentário. O livro e as notas do caderno não serão alterados.")},confirmButton={TextButton(onClick={scope.launch{model.deleteAnnotation(id);deleteId=null}}){Text("Eliminar")}},dismissButton={TextButton(onClick={deleteId=null}){Text("Cancelar")}})}
        externalLink?.let{url->AlertDialog(onDismissRequest={externalLink=null},title={Text("Abrir ligação externa?")},text={Text("O endereço será aberto no navegador, fora da Sophia:\n$url")},confirmButton={TextButton(onClick={startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));externalLink=null}){Text("Abrir navegador")}},dismissButton={TextButton(onClick={externalLink=null}){Text("Cancelar")}})}
        (feedback?:message)?.let{text->AlertDialog(onDismissRequest={feedback=null;model.clearMessage()},title={Text("Leitor")},text={Text(text)},confirmButton={TextButton(onClick={feedback=null;model.clearMessage()}){Text("Fechar")}})}
        }
    }
    companion object{fun intent(context:Context,fileId:String,locator:String?=null)=Intent(context,ReadiumActivity::class.java).putExtra("fileId",fileId).putExtra("locator",locator)}
}
