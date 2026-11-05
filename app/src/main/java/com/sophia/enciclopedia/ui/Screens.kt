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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sophia.enciclopedia.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.sophia.enciclopedia.PassageNotifications
import com.sophia.domain.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable fun HomeScreen(state:SophiaState,passage:Passage?,onShufflePassage:()->Unit,passageSettings:PassageSettings,onPassageSettings:(PassageSettings)->Unit,onTopic:(String?)->Unit){
    var query by rememberSaveable{mutableStateOf("")}
    val matches=state.histories.filter{it.matches(query)}
    LazyColumn(Modifier.fillMaxSize().testTag("home-screen"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item{Box(Modifier.clip(RoundedCornerShape(28.dp))){ArtworkBanner("religions","Conhecer a história. Compreender as ideias.","Pessoas, lugares, datas e tradições. Percursos históricos com fontes, disponíveis offline.",height=228.dp,label="SOPHIA · HISTÓRIA E CONHECIMENTO")}}
        item{PassageCard(passage,onShufflePassage,passageSettings,onPassageSettings)}
        item{SearchInput(query,{query=it},"Pesquisar categorias e história","home-search")}
        item{OutlinedButton(onClick={onTopic(null)},modifier=Modifier.fillMaxWidth().testTag("open-traditions")){Icon(Icons.Outlined.TravelExplore,null);Text("Todas as tradições e perspetivas",Modifier.padding(start=8.dp))}}
        if(matches.isEmpty())item{EmptyPanel("Nenhum tema encontrado","Experimenta outra palavra, pessoa ou lugar.",Icons.Outlined.SearchOff)}
        if(matches.any{it.area})item{SectionTitle("Explorar por categoria","Áreas de estudo · história e conceitos")}
        items(matches.filter{it.area}.chunked(2)){row->TopicRow(row,onTopic)}
        if(matches.any{!it.area})item{SectionTitle("Tradições e perspetivas","Percursos históricos · das origens às transformações posteriores")}
        items(matches.filter{!it.area}.chunked(2)){row->TopicRow(row,onTopic)}
        item{Text("As fontes documentam a história das crenças e da não crença. Os ensinamentos religiosos são apresentados no seu contexto; datas incertas e atribuições tradicionais são identificadas.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}
@Composable private fun TopicRow(topics:List<HistoryArticle>,onTopic:(String?)->Unit){
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
        topics.forEach{topic->Card(onClick={onTopic(topic.id)},modifier=Modifier.weight(1f).testTag("topic-${topic.id}"),shape=RoundedCornerShape(20.dp)){
            Column{
                Box(Modifier.fillMaxWidth().height(104.dp)){TopicArtwork(topic.id,Modifier.fillMaxSize(),topic.title);Surface(shape=RoundedCornerShape(50),color=MaterialTheme.colorScheme.primary,modifier=Modifier.align(Alignment.BottomStart).padding(8.dp)){Text(topic.symbol,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onPrimary,modifier=Modifier.padding(horizontal=9.dp,vertical=3.dp))}}
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Text(topic.title,style=MaterialTheme.typography.titleSmall,maxLines=2,overflow=TextOverflow.Ellipsis)
                    Text(topic.description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=3,overflow=TextOverflow.Ellipsis)
                    Text("Conhecer a história →",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                }
            }
        }}
        if(topics.size==1)Spacer(Modifier.weight(1f))
    }
}
@Composable fun PassageCard(passage:Passage?,onShuffle:()->Unit,settings:PassageSettings,onSettings:(PassageSettings)->Unit){
    var settingsOpen by rememberSaveable{mutableStateOf(false)}
    Card(Modifier.fillMaxWidth().testTag("passage-card"),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer,contentColor=MaterialTheme.colorScheme.onPrimaryContainer)){
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Surface(shape=RoundedCornerShape(50),color=MaterialTheme.colorScheme.primary){Icon(Icons.Outlined.AutoStories,null,tint=MaterialTheme.colorScheme.onPrimary,modifier=Modifier.padding(7.dp).size(18.dp))};Text("Passagem para refletir",Modifier.padding(start=10.dp),style=MaterialTheme.typography.titleMedium)}
            if(passage==null)Text("A preparar a passagem de hoje…",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=.75f))
            else{
                Text(passage.text,style=MaterialTheme.typography.bodyLarge)
                Text("— ${passage.bookTitle} · ${passage.unitTitle}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
            }
            Row{
                TextButton(onClick=onShuffle,modifier=Modifier.testTag("passage-shuffle")){Icon(Icons.Outlined.Refresh,null,Modifier.size(18.dp));Text("Outra passagem",Modifier.padding(start=6.dp))}
                TextButton(onClick={settingsOpen=true},modifier=Modifier.testTag("passage-settings")){Icon(Icons.Outlined.Notifications,null,Modifier.size(18.dp));Text((if(settings.enabled)"Notificação diária ativa" else "Notificação diária"),Modifier.padding(start=6.dp))}
            }
        }
    }
    if(settingsOpen)PassageSettingsDialog(settings,onSettings){settingsOpen=false}
}
@Composable fun PassageSettingsDialog(settings:PassageSettings,onSettings:(PassageSettings)->Unit,onClose:()->Unit){
    val context=LocalContext.current
    var denied by remember{mutableStateOf(false)}
    val permissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted)onSettings(settings.copy(enabled=true)) else denied=true}
    LibraryDialog("Passagem diária",onClose){
        Text("Uma passagem curta e completa dos livros-base de Kardec — um parágrafo para refletir, uma vez por dia. Nunca mais do que um parágrafo.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){Text("Receber notificação diária",style=MaterialTheme.typography.titleSmall);Text(if(settings.enabled)"Ativa" else "Desativada",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            Switch(checked=settings.enabled,onCheckedChange={on->
                if(on&&android.os.Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)}
                else onSettings(settings.copy(enabled=on))
            },modifier=Modifier.testTag("passage-enabled"))
        }
        if(denied)Text("Sem permissão de notificações, a passagem não pode ser entregue. Podes ativá-la nas definições do Android.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
        if(settings.enabled){
            SectionTitle("Quando?")
            Row(Modifier.fillMaxWidth().clickable{onSettings(settings.copy(automatic=true))},verticalAlignment=Alignment.CenterVertically){RadioButton(selected=settings.automatic,onClick={onSettings(settings.copy(automatic=true))});Column{Text("Automático");Text("Todos os dias às 9:00, sem teres de pensar nisso.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            Row(Modifier.fillMaxWidth().clickable{onSettings(settings.copy(automatic=false))},verticalAlignment=Alignment.CenterVertically){RadioButton(selected=!settings.automatic,onClick={onSettings(settings.copy(automatic=false))});Column{Text("Escolher o horário");Text("À hora que definires, todos os dias.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            if(!settings.automatic){
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    SelectFilter("Hora","%02d".format(settings.hour),(5..23).map{"%02d".format(it) to "%02d".format(it)},{it?.let{h->onSettings(settings.copy(hour=h.toInt()))}},Modifier.weight(1f),"")
                    SelectFilter("Minutos","%02d".format(settings.minute),listOf("00","15","30","45").map{it to it},{it?.let{m->onSettings(settings.copy(minute=m.toInt()))}},Modifier.weight(1f),"")
                }
                if(!PassageNotifications.canUseExactAlarms(context)){
                    Text("Para tocar exatamente à hora escolhida, o Android pede uma permissão de «alarmes exatos». Sem ela, a notificação pode chegar com alguns minutos de atraso.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick={runCatching{context.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:${context.packageName}")))}}){Text("Permitir alarmes exatos")}
                }
            }
        }
        Button(onClick=onClose,modifier=Modifier.fillMaxWidth()){Text("Fechar")}
    }
}
@Composable fun ReferencesScreen(state:SophiaState,library:LibrarySnapshot,installing:Boolean,onInstall:(BaseBook)->Unit,onReadFile:(LibraryFile)->Unit,focusBookId:String?=null,onFocusHandled:()->Unit={}){
    var query by rememberSaveable{mutableStateOf("")}
    var tradition by rememberSaveable{mutableStateOf<String?>(null)}
    var selected by remember{mutableStateOf<BaseBook?>(null)}
    LaunchedEffect(focusBookId){if(focusBookId!=null){selected=state.baseBooks.firstOrNull{it.id==focusBookId};onFocusHandled()}}
    val presentTraditions=state.traditions.filter{t->state.baseBooks.any{it.traditionId==t.id}}
    val filtered=state.baseBooks.filter{book->(tradition==null||book.traditionId==tradition)&&(query.isBlank()||CatalogSearch.normalized(book.title+" "+book.author+" "+book.summary+" "+book.composition).contains(CatalogSearch.normalized(query)))}
    LazyColumn(Modifier.fillMaxSize().testTag("references-screen"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{SectionTitle("Livros-base das tradições","Os livros fundamentais de cada espiritualidade: integrais quando os direitos o permitem; com resumo elaborado quando estão protegidos.")}
        item{SearchInput(query,{query=it},"Título, autor ou tema","references-search")}
        item{SelectFilter("Tradição",tradition,presentTraditions.map{it.id to it.name},{tradition=it})}
        item{Text("${filtered.size} ${if(filtered.size==1)"livro-base" else "livros-base"}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(filtered.isEmpty())item{EmptyPanel("Nenhum livro-base neste filtro","Experimenta outro termo ou limpa o filtro de tradição. Novas tradições entram por fases, à medida que os seus livros-base são preparados.",Icons.Outlined.SearchOff)}
        presentTraditions.filter{t->tradition==null||t.id==tradition}.forEach{trad->
            val books=filtered.filter{it.traditionId==trad.id}
            if(books.isNotEmpty()){
                item{SectionTitle(trad.name)}
                items(books,key={it.id}){book->BaseBookCard(book,trad.name){selected=book}}
            }
        }
        item{Text("Piloto: Espiritismo. As restantes tradições ganham os seus livros-base por fases, com a mesma regra — integral só quando for legal.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
    selected?.let{book->BaseBookDetail(book,state.traditions.firstOrNull{it.id==book.traditionId}?.name?:"",library.files.firstOrNull{it.workId==book.workId},installing,{onInstall(book)},onReadFile){selected=null}}
}
@Composable fun BaseBookDetail(book:BaseBook,traditionName:String,file:LibraryFile?,installing:Boolean,onInstall:()->Unit,onReadFile:(LibraryFile)->Unit,onClose:()->Unit){
    LibraryDialog(book.title,onClose){
        Text(book.author+(book.year?.let{" · $it"}?:""),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(traditionName,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)
        Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.secondaryContainer){Text(book.statusLine(),Modifier.padding(12.dp),style=MaterialTheme.typography.bodyMedium)}
        SectionTitle("Resumo")
        Text(book.summary,style=MaterialTheme.typography.bodyLarge)
        SectionTitle("De que é composto")
        Text(book.composition,style=MaterialTheme.typography.bodyLarge)
        SectionTitle("Direitos e texto")
        Text(book.rightsNote,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(book.fullTextAsset==null&&book.rights==BaseBookRights.PUBLIC_DOMAIN)Text("O texto integral desta obra está a ser preparado para leitura e pesquisa dentro da app.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(book.fullTextAsset!=null){
            if(file!=null)Button(onClick={onReadFile(file)},modifier=Modifier.fillMaxWidth().testTag("read-basebook-${book.id}")){Text("Ler agora")}
            else Button(onClick=onInstall,enabled=!installing,modifier=Modifier.fillMaxWidth().testTag("install-basebook-${book.id}")){Text(if(installing)"A adicionar…" else "Adicionar à biblioteca e ler")}
        }
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
        if(library.files.isEmpty())item{EmptyPanel("A tua biblioteca está pronta para receber livros","Importa um EPUB, PDF ou TXT. Os livros-base das tradições estão em Referências; os artigos de história estão no Início e em Investigar.")}
        else if(filteredFiles.isEmpty())item{EmptyPanel("Nenhum livro importado neste filtro","Experimenta outro título, autor ou tradição, ou limpa os filtros.",Icons.Outlined.SearchOff)}
        items(filteredFiles,key={"file-${it.id}"}){file->Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
            FileCard(file){onRead(file)}
            TextButton(onClick={vm.openBook(file.workId)},modifier=Modifier.testTag("file-details-${file.id}")){Text("Detalhes, favoritos e notas")}
        }}
        item{Spacer(Modifier.height(16.dp))}
    }
}
@Composable fun ResearchScreen(state:SophiaState,library:LibrarySnapshot,libraryVm:LibraryViewModel,installing:Boolean,onTopic:(String)->Unit,onOpenHit:(BundledSearchHit)->Unit,onReadFile:(LibraryFile)->Unit){
    var query by rememberSaveable{mutableStateOf("")}
    var tradition by rememberSaveable(state.filters.researchTradition){mutableStateOf(state.filters.researchTradition)}
    var selected by remember{mutableStateOf<BaseBook?>(null)}
    var hits by remember{mutableStateOf<List<BundledSearchHit>>(emptyList())}
    LaunchedEffect(query){if(query.trim().length>=3){kotlinx.coroutines.delay(600);libraryVm.searchBundled(query){hits=it}}else hits=emptyList()}
    val active=query.isNotBlank()||tradition!=null
    val bookResults=state.baseBooks.filter{book->(tradition==null||book.traditionId==tradition)&&(query.isBlank()||CatalogSearch.normalized(book.title+" "+book.author+" "+book.summary+" "+book.composition).contains(CatalogSearch.normalized(query)))}
    val articleResults=if(query.isBlank())emptyList() else state.histories.filter{it.matches(query)&&(tradition==null||it.id==tradition)}
    LazyColumn(Modifier.fillMaxSize().testTag("research-screen"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item{SectionTitle("Investigar com contexto","Pesquisa nos livros-base e nos artigos de história da Sophia.")}
        item{SearchInput(query,{query=it},"Tema, título ou autor","research-search")}
        item{SelectFilter("Tradição",tradition,state.traditions.map{it.id to it.name},{tradition=it})}
        item{Text("A pesquisa consulta resumos, estruturas, artigos e o texto integral dos livros já adicionados à biblioteca.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(!active)item{EmptyPanel("Por onde queres começar?","Escreve um termo ou escolhe uma tradição para veres os seus livros-base.",Icons.Outlined.TravelExplore)}
        else {
            item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("${bookResults.size+articleResults.size+hits.size} ${if(bookResults.size+articleResults.size+hits.size==1)"resultado" else "resultados"}",Modifier.weight(1f),style=MaterialTheme.typography.titleSmall);TextButton(onClick={query="";tradition=null}){Text("Limpar")}}}
            if(bookResults.isEmpty()&&articleResults.isEmpty()&&hits.isEmpty())item{EmptyPanel("Não encontrámos correspondências","Tenta um termo mais geral ou remove o filtro de tradição.",Icons.Outlined.SearchOff)}
            if(bookResults.isNotEmpty())item{SectionTitle("Livros-base")}
            items(bookResults,key={"res-book-${it.id}"}){book->BaseBookCard(book,state.traditions.firstOrNull{it.id==book.traditionId}?.name?:""){selected=book}}
            if(articleResults.isNotEmpty())item{SectionTitle("História e temas")}
            items(articleResults,key={"res-article-${it.id}"}){article->OutlinedButton(onClick={onTopic(article.id)},modifier=Modifier.fillMaxWidth()){Text("${article.symbol}  ${article.title}")}}
            if(hits.isNotEmpty())item{SectionTitle("Nos textos integrais")}
            items(hits,key={"hit-${it.fileId}-${it.unit}"}){hit->Card(Modifier.fillMaxWidth().clickable{onOpenHit(hit)},shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(hit.bookTitle,style=MaterialTheme.typography.titleSmall);Text(hit.unitTitle,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary);Text(hit.snippet,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
        }
    }
    selected?.let{book->BaseBookDetail(book,state.traditions.firstOrNull{it.id==book.traditionId}?.name?:"",library.files.firstOrNull{it.workId==book.workId},installing,{libraryVm.installBundled(book)},onReadFile){selected=null}}
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
