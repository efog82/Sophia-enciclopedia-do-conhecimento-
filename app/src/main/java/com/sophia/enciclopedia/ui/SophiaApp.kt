package com.sophia.enciclopedia.ui

import androidx.activity.compose.BackHandler
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
@Composable fun SophiaApp(vm:SophiaViewModel){
    val state by vm.state.collectAsStateWithLifecycle()
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
                else->when(section){Section.HOME->HomeScreen(state,vm,busy);Section.LIBRARY->LibraryScreen(state,vm,busy);Section.RESEARCH->ResearchScreen(state,vm,busy);Section.AI->AssistantScreen();Section.NOTES->NotesScreen(state,vm){deleteId=it.id}}
            }
        }}
        if(themeOpen)AlertDialog(onDismissRequest={themeOpen=false},title={Text("Aspeto da aplicação")},text={Column{ThemeMode.entries.forEach{mode->TextButton(onClick={vm.theme(mode);themeOpen=false},modifier=Modifier.fillMaxWidth().testTag("theme-${mode.name}")){Text((if(mode==state.theme)"✓  " else "")+when(mode){ThemeMode.SYSTEM->"Seguir o sistema";ThemeMode.LIGHT->"Claro";ThemeMode.DARK->"Escuro"})}}}},confirmButton={TextButton(onClick={themeOpen=false}){Text("Fechar")}})
        state.books.firstOrNull{it.id==work}?.let{book->BookDetails(book,book.id in busy,{vm.closeBook()},{vm.toggleFavorite(book)},{vm.startNote(workId=book.id)})}
        if(editor.open)NoteEditor(editor,state.books,saving,editorError,vm,{if(vm.editorDirty())discard=true else vm.closeEditor()})
        if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Descartar alterações?")},text={Text("As alterações que ainda não guardaste serão perdidas.")},confirmButton={TextButton(onClick={discard=false;vm.closeEditor()},modifier=Modifier.testTag("confirm-discard")){Text("Descartar")}},dismissButton={TextButton(onClick={discard=false}){Text("Continuar a editar")}})
        deleteId?.let{id->AlertDialog(onDismissRequest={deleteId=null},title={Text("Eliminar nota?")},text={Text("A nota será eliminada deste dispositivo. Esta ação não pode ser anulada.")},confirmButton={TextButton(onClick={vm.deleteNote(id);deleteId=null},modifier=Modifier.testTag("confirm-delete-note")){Text("Eliminar")}},dismissButton={TextButton(onClick={deleteId=null}){Text("Cancelar")}})}
    }
}

@Composable private fun BookDetails(book:Book,busy:Boolean,onClose:()->Unit,onFavorite:()->Unit,onNote:()->Unit){
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
            book.editions.forEach{Text("${it.label?:"Edição sem designação verificada"} · ${it.fileCount} ficheiros")}
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
            SelectFilter("Obra associada (opcional)",editor.workId,books.map{it.id to it.title},vm::editWork,emptyLabel="Sem obra associada")
            if(editor.workId==null)Text("Nota livre, sem obra associada.",style=MaterialTheme.typography.bodySmall)
            error?.let{Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag("note-error"))}
            Button(onClick=vm::saveNote,enabled=!saving,modifier=Modifier.fillMaxWidth().testTag("save-note")){Text(if(saving)"A guardar…" else "Guardar nota")}
            TextButton(onClick=onClose,enabled=!saving,modifier=Modifier.fillMaxWidth().testTag("cancel-note")){Text("Cancelar")}
        }}
    }
}
