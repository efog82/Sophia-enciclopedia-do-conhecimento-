package com.sophia.enciclopedia.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sophia.enciclopedia.*
import com.sophia.domain.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable fun HomeScreen(state:SophiaState,vm:SophiaViewModel,busy:Set<String>,library:LibrarySnapshot,onRead:(LibraryFile)->Unit,onTraditions:()->Unit){
    var query by rememberSaveable{mutableStateOf("")}
    LazyColumn(Modifier.fillMaxSize().testTag("home-screen"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item{Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.primaryContainer){Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("ENCICLOPÉDIA DO CONHECIMENTO ESPIRITUAL",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Um lugar para estudar e compreender.",style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Explora obras, tradições e ideias. Constrói o teu próprio caderno de estudo.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onPrimaryContainer)
        }}}
        item{OutlinedTextField(query,{query=it},label={Text("Pesquisar título ou autor")},leadingIcon={Icon(Icons.Outlined.Search,null)},trailingIcon={IconButton(enabled=query.isNotBlank(),onClick={vm.searchFromHome(query)}){Icon(Icons.Outlined.ArrowForward,"Pesquisar na biblioteca")}},singleLine=true,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={if(query.isNotBlank())vm.searchFromHome(query)}),modifier=Modifier.fillMaxWidth().testTag("home-search"),shape=RoundedCornerShape(16.dp))}
        item{OutlinedButton(onClick=onTraditions,modifier=Modifier.fillMaxWidth().testTag("open-traditions")){Text("Conhecer as tradições")}}
        item{SectionTitle("Explorar por categoria","Perspetivas diferentes, com contexto e fontes.")}
        items(state.categories.chunked(2)){row->Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
            row.forEach{category->OutlinedCard(onClick={vm.chooseCategory(category.id)},modifier=Modifier.weight(1f),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(13.dp)){Text(category.name,style=MaterialTheme.typography.titleSmall,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${state.books.count{b->b.categories.any{it.id==category.id}}} referências",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
            if(row.size==1)Spacer(Modifier.weight(1f))
        }}
        item{SectionTitle("Continuar a ler")}
        if(library.files.none{it.location!=null})item{EmptyPanel("Ainda não há leituras em curso","Importa EPUB, PDF ou TXT na Biblioteca e começa a ler offline.")}
        items(library.files.filter{it.location!=null}.sortedByDescending{it.updatedAt}.take(3),key={"reading-${it.id}"}){file->FileCard(file){onRead(file)}}
        item{SectionTitle("Adicionados recentemente","${state.books.size} referências bibliográficas · metadados disponíveis offline")}
        items(state.books.take(6),key={it.id}){book->BookCard(book,book.id in busy,{vm.openBook(book.id)},{vm.toggleFavorite(book)})}
        item{Spacer(Modifier.height(12.dp))}
    }
}
@Composable fun LibraryScreen(state:SophiaState,vm:SophiaViewModel,busy:Set<String>,library:LibrarySnapshot,onRead:(LibraryFile)->Unit,onImport:()->Unit,onCollections:()->Unit,onTraditions:()->Unit){
    LazyColumn(Modifier.fillMaxSize().testTag("library-screen"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{SectionTitle("A tua biblioteca","Referências, ficheiros offline e listas de leitura.")}
        item{Button(onClick=onImport,modifier=Modifier.fillMaxWidth().testTag("import-file")){Icon(Icons.Outlined.FileOpen,null);Text("Importar EPUB, PDF ou TXT",Modifier.padding(start=8.dp))}}
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick=onCollections,modifier=Modifier.weight(1f).testTag("open-collections")){Text("Coleções e listas")};OutlinedButton(onClick=onTraditions,modifier=Modifier.weight(1f)){Text("Tradições")}}}
        if(library.files.isNotEmpty()){
            item{SectionTitle("Ficheiros offline")}
            items(library.files,key={"file-${it.id}"}){file->FileCard(file){onRead(file)}}
        }
        item{SearchInput(state.filters.libraryQuery,{q->vm.setFilters{it.copy(libraryQuery=q)}},"Título ou autor","library-search")}
        item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){SelectFilter("Tradição",state.filters.tradition,state.traditions.map{it.id to it.name},{id->vm.setFilters{it.copy(tradition=id)}},Modifier.weight(1f));SelectFilter("Categoria",state.filters.category,state.categories.map{it.id to it.name},{id->vm.setFilters{it.copy(category=id)}},Modifier.weight(1f))}}
        item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){FilterChip(selected=state.filters.favorites,onClick={vm.setFilters{it.copy(favorites=!it.favorites)}},label={Text("Favoritos")},leadingIcon={Icon(Icons.Outlined.BookmarkBorder,null,Modifier.size(16.dp))},modifier=Modifier.testTag("favorites-filter"));Spacer(Modifier.weight(1f));TextButton(onClick={vm.setFilters{it.copy(libraryQuery="",tradition=null,category=null,favorites=false)}}){Text("Limpar filtros")}}}
        item{Text("${state.library.size} ${if(state.library.size==1)"obra encontrada" else "obras encontradas"}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.testTag("library-count"))}
        if(state.library.isEmpty())item{EmptyPanel(if(state.filters.favorites)"Ainda não há favoritos neste filtro" else "Nenhuma obra encontrada","Experimenta outro título, autor ou tradição, ou limpa os filtros.",Icons.Outlined.SearchOff)}
        items(state.library,key={it.id}){book->BookCard(book,book.id in busy,{vm.openBook(book.id)},{vm.toggleFavorite(book)})}
        item{Spacer(Modifier.height(16.dp))}
    }
}
@Composable fun ResearchScreen(state:SophiaState,vm:SophiaViewModel,busy:Set<String>){
    val active=state.filters.researchQuery.isNotBlank()||state.filters.researchTradition!=null||state.filters.author!=null
    val authors=state.books.flatMap{it.allContributors}.distinctBy{it.id}.sortedBy{it.name}.map{it.id to it.name}
    LazyColumn(Modifier.fillMaxSize().testTag("research-screen"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item{SectionTitle("Investigar com contexto","Pesquisa de metadados · biblioteca universal")}
        item{SearchInput(state.filters.researchQuery,{q->vm.setFilters{it.copy(researchQuery=q)}},"Tema, título ou autor","research-search")}
        item{SelectFilter("Autor ou colaborador",state.filters.author,authors,{id->vm.setFilters{it.copy(author=id)}})}
        item{SelectFilter("Tradição",state.filters.researchTradition,state.traditions.map{it.id to it.name},{id->vm.setFilters{it.copy(researchTradition=id)}})}
        item{Text("A pesquisa consulta títulos, autores, descrições e classificação. Não pesquisa o texto dos livros e não usa pesquisa semântica.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(!active)item{EmptyPanel("Por onde queres começar?","Escreve um termo ou escolhe uma tradição. Os resultados remetem para referências bibliográficas, não para citações de livros.",Icons.Outlined.TravelExplore)}
        else {
            item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("${state.research.size} resultados de metadados",Modifier.weight(1f),style=MaterialTheme.typography.titleSmall);TextButton(onClick={vm.setFilters{it.copy(researchQuery="",researchTradition=null,author=null)}}){Text("Limpar")}}}
            if(state.research.isEmpty())item{EmptyPanel("Não encontrámos correspondências","Tenta um termo mais geral ou remove um dos filtros.",Icons.Outlined.SearchOff)}
            items(state.research,key={it.id}){book->BookCard(book,book.id in busy,{vm.openBook(book.id)},{vm.toggleFavorite(book)})}
        }
    }
}
@Composable fun SearchInput(value:String,onChange:(String)->Unit,label:String,tag:String){val keyboard=LocalSoftwareKeyboardController.current;OutlinedTextField(value,onChange,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={keyboard?.hide()}),label={Text(label)},singleLine=true,leadingIcon={Icon(Icons.Outlined.Search,null)},trailingIcon={if(value.isNotEmpty())IconButton(onClick={onChange("")}){Icon(Icons.Outlined.Close,"Limpar pesquisa")}},modifier=Modifier.fillMaxWidth().testTag(tag),shape=RoundedCornerShape(16.dp))}
@Composable fun AssistantScreen(){
    LazyColumn(Modifier.fillMaxSize().testTag("assistant-screen"),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(22.dp)){
        item{Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.primaryContainer){Icon(Icons.Outlined.Psychology,null,Modifier.padding(20.dp).size(40.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer)}}
        item{Text("Um assistente com fontes, no momento certo.",style=MaterialTheme.typography.headlineMedium)}
        item{Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.secondaryContainer){Text("Em desenvolvimento · fase futura",Modifier.padding(12.dp))}}
        item{Text("O assistente IA ainda não está disponível. Nesta fase não existem modelos ligados, chamadas de API ou respostas geradas.",style=MaterialTheme.typography.bodyLarge)}
        item{OutlinedCard{Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("O que está previsto",style=MaterialTheme.typography.titleMedium);Text("Investigação fundamentada em fontes, citações verificáveis e comparação de perspetivas. Estas funcionalidades não fazem parte desta versão.",style=MaterialTheme.typography.bodyMedium)}}}
        item{Text("Podes já explorar o catálogo em Investigar e organizar as tuas notas no Caderno.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}
@Composable fun NotesScreen(state:SophiaState,vm:SophiaViewModel,onDelete:(StudyNote)->Unit){
    LazyColumn(Modifier.fillMaxSize().testTag("notes-screen"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{SectionTitle("O teu caderno de estudos","Notas pessoais guardadas apenas neste dispositivo.")}
        item{Button(onClick={vm.startNote()},modifier=Modifier.fillMaxWidth().testTag("new-note"),shape=RoundedCornerShape(14.dp)){Icon(Icons.Outlined.Add,null);Text("Nova nota",Modifier.padding(start=8.dp))}}
        item{SearchInput(state.filters.notesQuery,{q->vm.setFilters{it.copy(notesQuery=q)}},"Pesquisar nas minhas notas","notes-search")}
        if(state.filteredNotes.isEmpty())item{EmptyPanel(if(state.notes.isEmpty())"O teu caderno começa com uma ideia" else "Nenhuma nota encontrada",if(state.notes.isEmpty())"Regista perguntas, reflexões e ligações entre as tuas leituras. Podes associar cada nota a uma obra." else "Experimenta outro termo de pesquisa.",Icons.Outlined.EditNote)}
        items(state.filteredNotes,key={it.id}){note->Card(Modifier.fillMaxWidth().testTag("note-${note.id}").clickable{vm.startNote(note)},shape=RoundedCornerShape(18.dp)){
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){Text(note.title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis);IconButton(onClick={onDelete(note)},modifier=Modifier.testTag("delete-note-${note.id}")){Icon(Icons.Outlined.DeleteOutline,"Eliminar nota: ${note.title}")}}
                if(note.fileId!=null)Text("Nota associada a uma posição de leitura",style=MaterialTheme.typography.labelSmall)
                if(note.body.isNotEmpty())Text(note.body,style=MaterialTheme.typography.bodyMedium,maxLines=3,overflow=TextOverflow.Ellipsis)
                state.books.firstOrNull{it.id==note.workId}?.let{Text(it.title,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)}
                Text("Atualizada em "+DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",Locale.forLanguageTag("pt-PT")).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(note.updatedAt)),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }}
    }
}
