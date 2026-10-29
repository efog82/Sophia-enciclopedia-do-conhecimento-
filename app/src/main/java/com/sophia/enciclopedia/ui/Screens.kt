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

@Composable fun HomeScreen(state:SophiaState,onTopic:(String?)->Unit){
    var query by rememberSaveable{mutableStateOf("")}
    val matches=state.histories.filter{it.matches(query)}
    LazyColumn(Modifier.fillMaxSize().testTag("home-screen"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item{Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.primaryContainer){Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("SOPHIA · HISTÓRIA E CONHECIMENTO",style=MaterialTheme.typography.labelSmall)
            Text("Conhecer a história. Compreender as ideias.",style=MaterialTheme.typography.headlineMedium)
            Text("Pessoas, lugares, datas e tradições. Percursos históricos com fontes, disponíveis offline.")
        }}}
        item{SearchInput(query,{query=it},"Pesquisar categorias e história","home-search")}
        item{OutlinedButton(onClick={onTopic(null)},modifier=Modifier.fillMaxWidth().testTag("open-traditions")){Icon(Icons.Outlined.TravelExplore,null);Text("Todas as tradições e perspetivas",Modifier.padding(start=8.dp))}}
        if(matches.isEmpty())item{EmptyPanel("Nenhum tema encontrado","Experimenta outra palavra, pessoa ou lugar.",Icons.Outlined.SearchOff)}
        if(matches.any{it.area})item{SectionTitle("Explorar por categoria","Áreas de estudo · história, conceitos e referências")}
        items(matches.filter{it.area}.chunked(2)){row->TopicRow(row,onTopic)}
        if(matches.any{!it.area})item{SectionTitle("Tradições e perspetivas","Percursos históricos · das origens às transformações posteriores")}
        items(matches.filter{!it.area}.chunked(2)){row->TopicRow(row,onTopic)}
        item{Text("As fontes documentam a história das crenças e da não crença. Os ensinamentos religiosos são apresentados no seu contexto; datas incertas e atribuições tradicionais são identificadas.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}
@Composable private fun TopicRow(topics:List<HistoryArticle>,onTopic:(String?)->Unit){
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
        topics.forEach{topic->OutlinedCard(onClick={onTopic(topic.id)},modifier=Modifier.weight(1f).testTag("topic-${topic.id}"),shape=RoundedCornerShape(16.dp)){
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Text(topic.symbol,style=MaterialTheme.typography.headlineSmall,color=MaterialTheme.colorScheme.primary,modifier=Modifier.widthIn(min=28.dp))
                    Text(topic.title,style=MaterialTheme.typography.titleSmall)
                }
                Text(topic.description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Conhecer a história →",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
            }
        }}
        if(topics.size==1)Spacer(Modifier.weight(1f))
    }
}
@Composable fun LibraryScreen(state:SophiaState,vm:SophiaViewModel,busy:Set<String>,library:LibrarySnapshot,onRead:(LibraryFile)->Unit,onImport:()->Unit,onCollections:()->Unit,onTraditions:()->Unit){
    LazyColumn(Modifier.fillMaxSize().testTag("library-screen"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{SectionTitle("A tua biblioteca","Os teus livros importados, disponíveis para leitura offline.")}
        item{Button(onClick=onImport,modifier=Modifier.fillMaxWidth().testTag("import-file")){Icon(Icons.Outlined.FileOpen,null);Text("Importar EPUB, PDF ou TXT",Modifier.padding(start=8.dp))}}
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick=onCollections,modifier=Modifier.weight(1f).testTag("open-collections")){Text("Coleções e listas")};OutlinedButton(onClick=onTraditions,modifier=Modifier.weight(1f)){Text("Tradições")}}}
        item{SearchInput(state.filters.libraryQuery,{q->vm.setFilters{it.copy(libraryQuery=q)}},"Título ou autor","library-search")}
        item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){SelectFilter("Tradição",state.filters.tradition,state.traditions.map{it.id to it.name},{id->vm.setFilters{it.copy(tradition=id)}},Modifier.weight(1f));SelectFilter("Categoria",state.filters.category,state.categories.map{it.id to it.name},{id->vm.setFilters{it.copy(category=id)}},Modifier.weight(1f))}}
        item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){FilterChip(selected=state.filters.favorites,onClick={vm.setFilters{it.copy(favorites=!it.favorites)}},label={Text("Favoritos")},leadingIcon={Icon(Icons.Outlined.BookmarkBorder,null,Modifier.size(16.dp))},modifier=Modifier.testTag("favorites-filter"));Spacer(Modifier.weight(1f));TextButton(onClick={vm.setFilters{it.copy(libraryQuery="",tradition=null,category=null,favorites=false)}}){Text("Limpar filtros")}}}
        val filteredFiles=library.files.filter{file->state.library.any{it.id==file.workId}}
        item{Text("${filteredFiles.size} ${if(filteredFiles.size==1)"ficheiro encontrado" else "ficheiros encontrados"}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.testTag("library-count"))}
        if(library.files.isEmpty())item{EmptyPanel("A tua biblioteca está pronta para receber livros","Importa um EPUB, PDF ou TXT. As referências históricas estão nas categorias do Início e em Investigar; não são livros disponíveis para leitura.")}
        else if(filteredFiles.isEmpty())item{EmptyPanel("Nenhum livro importado neste filtro","Experimenta outro título, autor ou tradição, ou limpa os filtros.",Icons.Outlined.SearchOff)}
        items(filteredFiles,key={"file-${it.id}"}){file->Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
            FileCard(file){onRead(file)}
            TextButton(onClick={vm.openBook(file.workId)},modifier=Modifier.testTag("file-details-${file.id}")){Text("Detalhes, favoritos e notas")}
        }}
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
            item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("${state.research.size} ${if(state.research.size==1)"resultado" else "resultados"} de metadados",Modifier.weight(1f),style=MaterialTheme.typography.titleSmall);TextButton(onClick={vm.setFilters{it.copy(researchQuery="",researchTradition=null,author=null)}}){Text("Limpar")}}}
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
