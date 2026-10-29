package com.sophia.enciclopedia.ui

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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sophia.domain.*
import com.sophia.enciclopedia.*

@Composable fun LibraryDialog(title:String,onClose:()->Unit,content:@Composable ColumnScope.()->Unit){Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)){Surface(Modifier.fillMaxWidth().fillMaxHeight(.95f).padding(12.dp),shape=MaterialTheme.shapes.large){Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)){TextButton(onClick=onClose){Text("Fechar")};Text(title,style=MaterialTheme.typography.headlineSmall);content()}}}}
@Composable fun FileCard(file:LibraryFile,onRead:()->Unit){OutlinedCard(onClick=onRead,modifier=Modifier.fillMaxWidth().testTag("file-${file.id}")){
    Row(Modifier.padding(14.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically){
        BookCover(file.title,fileId=file.id,mimeType=file.mimeType,modifier=Modifier.width(82.dp).height(118.dp))
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text(file.title,style=MaterialTheme.typography.titleMedium)
            Text(file.displayName,style=MaterialTheme.typography.bodySmall)
            Text("${file.mimeType.substringAfterLast('/')} · ${(file.sizeBytes/1024).coerceAtLeast(1)} KiB · ${file.progressPermille/10}%",style=MaterialTheme.typography.labelSmall)
            Text("Ler offline",color=MaterialTheme.colorScheme.primary)
        }
    }
}}
@Composable fun CollectionsDialog(state:SophiaState,library:LibrarySnapshot,vm:LibraryViewModel,onClose:()->Unit,onBook:(String)->Unit){
    var name by rememberSaveable{mutableStateOf("")}
    var readingList by rememberSaveable{mutableStateOf(false)}
    var delete by rememberSaveable{mutableStateOf<String?>(null)}
    LibraryDialog("Coleções e listas de leitura",onClose){
        Text("Adiciona obras a partir dos respetivos detalhes. As listas permitem definir uma ordem de leitura.")
        OutlinedTextField(name,{if(it.length<=120)name=it},label={Text("Nome da coleção")},modifier=Modifier.fillMaxWidth().testTag("collection-name"))
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(readingList,{readingList=it});Text("Lista de leitura ordenada")}
        Button(onClick={vm.createCollection(name,readingList);name=""},enabled=name.isNotBlank(),modifier=Modifier.testTag("create-collection")){Text("Criar")}
        if(library.collections.isEmpty())EmptyPanel("Ainda não há coleções","Cria uma coleção ou lista para organizar as tuas obras.")
        library.collections.forEach{collection->
            OutlinedCard(Modifier.fillMaxWidth()){
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text(collection.name,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                        IconButton(onClick={delete=collection.id}){Icon(Icons.Outlined.DeleteOutline,"Eliminar coleção: ${collection.name}")}
                    }
                    Text(if(collection.readingList)"Lista de leitura" else "Coleção",style=MaterialTheme.typography.labelSmall)
                    if(collection.workIds.isEmpty())Text("Ainda sem obras.")
                    collection.workIds.forEachIndexed{index,id->
                        state.books.firstOrNull{it.id==id}?.let{book->
                            TextButton(onClick={onBook(id)}){Text("${index+1}. ${book.title}")}
                            Row{
                                if(collection.readingList){
                                    IconButton(onClick={vm.move(collection.id,id,-1)},enabled=index>0){Icon(Icons.Outlined.KeyboardArrowUp,"Subir: ${book.title}")}
                                    IconButton(onClick={vm.move(collection.id,id,1)},enabled=index<collection.workIds.lastIndex){Icon(Icons.Outlined.KeyboardArrowDown,"Descer: ${book.title}")}
                                }
                                TextButton(onClick={vm.setInCollection(collection.id,id,false)}){Text("Remover da lista")}
                            }
                        }
                    }
                }
            }
        }
    }
    delete?.let{id->
        AlertDialog(onDismissRequest={delete=null},title={Text("Eliminar coleção?")},text={Text("Apenas a organização será eliminada. As obras, ficheiros, favoritos e notas serão conservados.")},confirmButton={TextButton(onClick={vm.deleteCollection(id);delete=null}){Text("Eliminar")}},dismissButton={TextButton(onClick={delete=null}){Text("Cancelar")}})
    }
}
@Composable fun ImportDialog(uri:String,initialWork:String?,books:List<Book>,busy:Boolean,message:String?,onClose:()->Unit,onImport:(ImportRequest)->Unit){
    var work by rememberSaveable(uri){mutableStateOf(initialWork)};var title by rememberSaveable(uri){mutableStateOf("")};var authorised by rememberSaveable(uri){mutableStateOf(false)}
    LibraryDialog("Importar para leitura offline",{if(!busy)onClose()}){
        Text("Será criada uma cópia privada do ficheiro escolhido. O original não é alterado. A Sophia não verifica nem transfere os direitos de autor.")
        SelectFilter("Associar a uma obra",work,books.map{it.id to it.title},{if(!busy)work=it},emptyLabel="Criar obra a partir do ficheiro")
        if(work==null)OutlinedTextField(title,{if(it.length<=300)title=it},label={Text("Título (opcional)")},supportingText={Text("Se vazio, usa o título declarado no EPUB ou o nome do ficheiro.")},modifier=Modifier.fillMaxWidth())
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(authorised,{if(!busy)authorised=it},enabled=!busy,modifier=Modifier.testTag("import-rights"));Text("Confirmo que tenho autorização para utilizar este ficheiro para estudo pessoal.",Modifier.weight(1f))}
        message?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Button(onClick={onImport(ImportRequest(uri,work,title,authorised))},enabled=authorised&&!busy,modifier=Modifier.fillMaxWidth().testTag("confirm-import")){Text(if(busy)"A copiar e validar…" else "Importar")}
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}
