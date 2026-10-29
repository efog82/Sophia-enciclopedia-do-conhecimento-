package com.sophia.enciclopedia.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sophia.domain.*
import com.sophia.enciclopedia.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SophiaApp(vm:SophiaViewModel,libraryVm:LibraryViewModel){
    val state by vm.state.collectAsStateWithLifecycle()
    val libraryState by libraryVm.state.collectAsStateWithLifecycle()
    val importing by libraryVm.importing.collectAsStateWithLifecycle()
    val importMessage by libraryVm.message.collectAsStateWithLifecycle()
    val importResult by libraryVm.result.collectAsStateWithLifecycle()
    val context=LocalContext.current
    var traditionsOpen by rememberSaveable{mutableStateOf(false)}
    var collectionsOpen by rememberSaveable{mutableStateOf(false)}
    var importUri by rememberSaveable{mutableStateOf<String?>(null)}
    var importWork by rememberSaveable{mutableStateOf<String?>(null)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){libraryVm.clearMessage();importUri=uri.toString()}}
    val read:(LibraryFile)->Unit={context.startActivity(ReaderActivity.intent(context,it.id))}
    val pick:(String?)->Unit={id->importWork=id;picker.launch(arrayOf("application/epub+zip","application/pdf","text/plain","application/octet-stream"))}
    LaunchedEffect(importResult){importResult?.let{importUri=null;vm.openBook(it.workId);libraryVm.clearResult()}}
    val selected by vm.section.collectAsStateWithLifecycle()
    val work by vm.selectedWork.collectAsStateWithLifecycle()
    val editor by vm.editor.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val editorError by vm.editorError.collectAsStateWithLifecycle()
    val feedback by vm.feedback.collectAsStateWithLifecycle()
    val busy by vm.favoriteBusy.collectAsStateWithLifecycle()
    var themeOpen by rememberSaveable{mutableStateOf(false)}
    var deleteId by rememberSaveable{mutableStateOf<String?>(null)}
    var discard by rememberSaveable{mutableStateOf(false)}
    val section=Section.entries.firstOrNull{it.name==selected}?:Section.HOME
    val snackbar=remember{SnackbarHostState()}
    LaunchedEffect(feedback){feedback?.let{snackbar.showSnackbar(it);vm.clearFeedback()}}
    SophiaTheme(state.theme){
        BackHandler(enabled=section!=Section.HOME && !editor.open && work==null){vm.navigate(Section.HOME)}
        Scaffold(snackbarHost={SnackbarHost(snackbar)},topBar={TopAppBar(title={Text(section.title)},actions={IconButton(onClick={themeOpen=true},modifier=Modifier.testTag("theme-settings")){Icon(Icons.Outlined.Palette,"Escolher tema")}})},bottomBar={NavigationBar{
            Section.entries.forEach{s->NavigationBarItem(selected=s==section,onClick={vm.navigate(s)},icon={Icon(when(s){Section.HOME->Icons.Outlined.Home;Section.LIBRARY->Icons.Outlined.AutoStories;Section.RESEARCH->Icons.Outlined.TravelExplore;Section.AI->Icons.Outlined.Psychology;Section.NOTES->Icons.Outlined.EditNote},null)},label={Text(s.label,fontSize=10.sp,maxLines=1)},modifier=Modifier.testTag("nav-${s.name}"))}
        }}){padding->Box(Modifier.padding(padding).fillMaxSize()){
            when{
                state.loading->Column(Modifier.padding(24.dp)){CircularProgressIndicator();Text("A abrir a biblioteca…",Modifier.padding(top=16.dp))}
                state.error!=null->Column(Modifier.padding(24.dp)){EmptyPanel("Dados locais indisponíveis",state.error!!);Button(onClick=vm::retry){Text("Tentar novamente")}}
                else->when(section){Section.HOME->HomeScreen(state,vm,busy,libraryState.data,read,{traditionsOpen=true});Section.LIBRARY->LibraryScreen(state,vm,busy,libraryState.data,read,{pick(null)},{collectionsOpen=true},{traditionsOpen=true});Section.RESEARCH->ResearchScreen(state,vm,busy);Section.AI->AssistantScreen();Section.NOTES->NotesScreen(state,vm){deleteId=it.id}}
            }
        }}
        if(themeOpen)AlertDialog(onDismissRequest={themeOpen=false},title={Text("Aspeto da aplicação")},text={Column{ThemeMode.entries.forEach{mode->TextButton(onClick={vm.theme(mode);themeOpen=false},modifier=Modifier.fillMaxWidth().testTag("theme-${mode.name}")){Text((if(mode==state.theme)"✓  " else "")+when(mode){ThemeMode.SYSTEM->"Seguir o sistema";ThemeMode.LIGHT->"Claro";ThemeMode.DARK->"Escuro"})}}}},confirmButton={TextButton(onClick={themeOpen=false}){Text("Fechar")}})
        if(traditionsOpen)TraditionsDialog(state,{traditionsOpen=false},{id->traditionsOpen=false;vm.openBook(id)},{id->traditionsOpen=false;vm.setFilters{it.copy(researchQuery="",researchTradition=id,author=null)};vm.navigate(Section.RESEARCH)})
        if(collectionsOpen)CollectionsDialog(state,libraryState.data,libraryVm,{collectionsOpen=false},{id->collectionsOpen=false;vm.openBook(id)})
        state.books.firstOrNull{it.id==work}?.let{book->BookDetails(book,book.id in busy,{vm.closeBook()},{vm.toggleFavorite(book)},{vm.startNote(workId=book.id)},libraryState.data,libraryVm,{pick(book.id)},read)}
        importUri?.let{uri->ImportDialog(uri,importWork,state.books,importing,importMessage,{importUri=null},libraryVm::importFile)}
        if(importUri==null&&importMessage!=null)AlertDialog(onDismissRequest=libraryVm::clearMessage,title={Text("Biblioteca")},text={Text(importMessage!!)},confirmButton={TextButton(onClick=libraryVm::clearMessage){Text("Fechar")}})
        if(libraryState.error!=null)AlertDialog(onDismissRequest=libraryVm::retry,title={Text("Dados da biblioteca")},text={Text(libraryState.error!!)},confirmButton={TextButton(onClick=libraryVm::retry){Text("Tentar reabrir")}})
        if(editor.open)NoteEditor(editor,state.books,saving,editorError,vm,{if(vm.editorDirty())discard=true else vm.closeEditor()})
        if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Descartar alterações?")},text={Text("As alterações que ainda não guardaste serão perdidas.")},confirmButton={TextButton(onClick={discard=false;vm.closeEditor()},modifier=Modifier.testTag("confirm-discard")){Text("Descartar")}},dismissButton={TextButton(onClick={discard=false}){Text("Continuar a editar")}})
        deleteId?.let{id->AlertDialog(onDismissRequest={deleteId=null},title={Text("Eliminar nota?")},text={Text("A nota será eliminada deste dispositivo. Esta ação não pode ser anulada.")},confirmButton={TextButton(onClick={vm.deleteNote(id);deleteId=null},modifier=Modifier.testTag("confirm-delete-note")){Text("Eliminar")}},dismissButton={TextButton(onClick={deleteId=null}){Text("Cancelar")}})}
    }
}

@Composable private fun BookDetails(book:Book,busy:Boolean,onClose:()->Unit,onFavorite:()->Unit,onNote:()->Unit,library:LibrarySnapshot,libraryVm:LibraryViewModel,onImport:()->Unit,onRead:(LibraryFile)->Unit){
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)){
        Surface(Modifier.fillMaxWidth().widthIn(max=760.dp).fillMaxHeight(.95f).padding(12.dp),shape=MaterialTheme.shapes.large){Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()).testTag("book-details"),verticalArrangement=Arrangement.spacedBy(16.dp)){
            TextButton(onClick=onClose){Icon(Icons.Outlined.Close,null);Text("Fechar detalhes")}
            Text(book.title,style=MaterialTheme.typography.headlineMedium)
            Text(book.description,style=MaterialTheme.typography.bodyLarge)
            Text("Autores e atribuições",style=MaterialTheme.typography.titleMedium)
            if(book.contributors.isEmpty())Text("Autoria coletiva ou não individualizada nesta referência.")
            book.contributors.forEach{Text("${it.name} · ${it.role.label}")}
            Text("Tradições: "+book.traditions.joinToString{it.name})
            Text("Categorias: "+book.categories.joinToString{it.name})
            Text("Ano da obra original: ${book.originalYear?.toString()?:"Não verificado"}")
            Text("Edições e ficheiros",style=MaterialTheme.typography.titleMedium)
            if(book.editions.isEmpty())Text("Nenhuma edição, tradução ou ficheiro selecionado. Editora, data da edição e direitos de utilização não verificados. Não existe um livro disponível para leitura nesta referência.")
            book.editions.forEach{edition->OutlinedCard{Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(edition.label?:"Edição sem designação verificada");Text("Idioma: ${edition.language?:"Não verificado"} · Ano da edição: ${edition.year?:"Não verificado"}");Text("Editora: ${edition.publisher?:"Não verificada"}");edition.contributors.forEach{Text("${it.name} · ${it.role.label}")};Text(if(edition.rightsStatus=="USER_AUTHORISED_NOT_VERIFIED")"Uso pessoal autorizado por declaração do utilizador; licença não verificada pela Sophia." else "Direitos da edição concreta: não verificados.");edition.sourceUrl?.let{url->Text(url,style=MaterialTheme.typography.bodySmall)}}}}
            library.files.filter{it.workId==book.id}.forEach{file->FileCard(file){onRead(file)}}
            Button(onClick=onImport,modifier=Modifier.fillMaxWidth().testTag("book-import")){Text("Importar ficheiro para esta obra")}
            Text("Coleções e listas",style=MaterialTheme.typography.titleMedium)
            if(library.collections.isEmpty())Text("Cria uma coleção na Biblioteca para organizar esta obra.")
            library.collections.forEach{c->Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Checkbox(book.id in c.workIds,{included->libraryVm.setInCollection(c.id,book.id,included)});Text(c.name)}}
            Text("Proveniência dos metadados",style=MaterialTheme.typography.titleMedium)
            Text(book.sourceLabel)
            androidx.compose.foundation.text.selection.SelectionContainer{Text(book.sourceUrl,style=MaterialTheme.typography.bodySmall)}
            Text(book.metadataNote,style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick=onFavorite,enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("detail-favorite")){Text(if(book.favorite)"Remover dos favoritos" else "Adicionar aos favoritos")}
            Button(onClick=onNote,modifier=Modifier.fillMaxWidth().testTag("book-new-note")){Text("Criar nota sobre esta obra")}
        }}
    }
}

@Composable private fun NoteEditor(editor:NoteEditorState,books:List<Book>,saving:Boolean,error:String?,vm:SophiaViewModel,onClose:()->Unit){
    Dialog(onDismissRequest={if(!saving)onClose()},properties=DialogProperties(usePlatformDefaultWidth=false)){
        Surface(Modifier.fillMaxWidth().widthIn(max=760.dp).fillMaxHeight(.95f).padding(12.dp).imePadding(),shape=MaterialTheme.shapes.large){Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()).testTag("note-editor"),verticalArrangement=Arrangement.spacedBy(14.dp)){
            Text(if(editor.id==null)"Nova nota de estudo" else "Editar nota",style=MaterialTheme.typography.headlineSmall)
            OutlinedTextField(editor.title,vm::editTitle,label={Text("Título")},singleLine=true,enabled=!saving,supportingText={Text("${editor.title.length}/160")},modifier=Modifier.fillMaxWidth().testTag("note-title"))
            OutlinedTextField(editor.body,vm::editBody,label={Text("Reflexões e apontamentos")},minLines=6,enabled=!saving,modifier=Modifier.fillMaxWidth().testTag("note-body"))
            if(editor.fileId!=null){
                Text("Nota ligada a uma posição do ficheiro. Alterar a obra remove esta ligação.")
                val context=LocalContext.current
                OutlinedButton(onClick={context.startActivity(ReaderActivity.intent(context,editor.fileId,editor.locatorJson))}){Text("Ler passagem associada")}
                editor.excerpt?.let{Text("Excerto guardado pelo utilizador: $it")}
            }
            SelectFilter("Obra associada (opcional)",editor.workId,books.map{it.id to it.title},vm::editWork,emptyLabel="Sem obra associada")
            if(editor.workId==null)Text("Nota livre, sem obra associada.",style=MaterialTheme.typography.bodySmall)
            error?.let{Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag("note-error"))}
            Button(onClick=vm::saveNote,enabled=!saving,modifier=Modifier.fillMaxWidth().testTag("save-note")){Text(if(saving)"A guardar…" else "Guardar nota")}
            TextButton(onClick=onClose,enabled=!saving,modifier=Modifier.fillMaxWidth().testTag("cancel-note")){Text("Cancelar")}
        }}
    }
}
