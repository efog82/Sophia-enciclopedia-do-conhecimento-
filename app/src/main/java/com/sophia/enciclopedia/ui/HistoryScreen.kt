package com.sophia.enciclopedia.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sophia.domain.LibraryFile
import com.sophia.domain.LibrarySnapshot
import com.sophia.domain.Passage
import com.sophia.domain.authoredBy
import com.sophia.domain.eventsMentioning
import com.sophia.domain.matchInLibrary
import com.sophia.domain.sectionsMentioning
import com.sophia.enciclopedia.SophiaState

@Composable fun TraditionsDialog(state:SophiaState,initialTopic:String?,onClose:()->Unit,onBook:(String)->Unit,onResearch:(String)->Unit,onBaseBook:(String)->Unit,loadPassages:(suspend (String)->List<Passage>)?=null,onPassage:(Passage)->Unit={},library:LibrarySnapshot=LibrarySnapshot(),onReadFile:(LibraryFile)->Unit={}){
    var selected by rememberSaveable(initialTopic){mutableStateOf(initialTopic)}
    val article=state.histories.firstOrNull{it.id==selected}
    var tab by rememberSaveable(selected){mutableStateOf("intro")}
    var person by rememberSaveable(selected){mutableStateOf<String?>(null)}
    var passages by remember(selected){mutableStateOf<List<Passage>?>(null)}
    LaunchedEffect(tab,selected){if(tab=="passagens"&&passages==null&&article!=null){passages=runCatching{loadPassages?.invoke(article.id)}.getOrNull()?:emptyList()}}
    // Nota: "passagens" só existe como separador quando há livros com texto integral; o LaunchedEffect usa o tab pedido.
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)){
        BackHandler(enabled=article!=null){if(person!=null)person=null else selected=null}
        Surface(Modifier.fillMaxWidth().widthIn(max=800.dp).fillMaxHeight(.96f).padding(10.dp),shape=MaterialTheme.shapes.large){
            key(selected){LazyColumn(Modifier.fillMaxSize().testTag("history-screen"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
                item{Row{TextButton(onClick=onClose){Text("Fechar")};if(article!=null)TextButton(onClick={selected=null}){Text("Todos os temas")}}}
                if(article==null){
                    item{SectionTitle("Tradições e perspetivas","História documentada · crença e não crença")}
                    items(state.histories.filter{!it.area},key={it.id}){topic->Card(onClick={selected=topic.id},modifier=Modifier.fillMaxWidth().testTag("tradition-${topic.id}"),shape=RoundedCornerShape(18.dp)){
                        Row(verticalAlignment=Alignment.CenterVertically){TopicArtwork(topic.id,Modifier.width(92.dp).height(72.dp),topic.title);Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(topic.title,style=MaterialTheme.typography.titleMedium);Text(topic.description,style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=TextOverflow.Ellipsis)}}
                    }}
                }else{
                    val personFigure=article.figures.firstOrNull{it.name==person}
                    if(personFigure!=null){
                        // Página da pessoa: resumo, livros da sua autoria e o percurso dentro desta tradição.
                        item{TextButton(onClick={person=null},modifier=Modifier.testTag("person-back")){Text("← Nomes de renome")}}
                        item{Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
                            Text(personFigure.name,style=MaterialTheme.typography.headlineSmall,color=MaterialTheme.colorScheme.primary,modifier=Modifier.testTag("person-name"))
                            Text(personFigure.dates,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.secondary)
                            Text(personFigure.text,Modifier.padding(top=8.dp),style=MaterialTheme.typography.bodyLarge)
                        }}
                        val personBooks=state.baseBooks.filter{it.authoredBy(personFigure.name)}
                        if(personBooks.isNotEmpty()){
                            item{SectionTitle("Livros da sua autoria")}
                            items(personBooks,key={"person-book-${it.id}"}){book->Card(onClick={onBaseBook(book.id)},modifier=Modifier.fillMaxWidth().testTag("person-book-${book.id}"),shape=RoundedCornerShape(18.dp)){
                                Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){BookCover(book.title,book.traditionId,author=book.author,modifier=Modifier.width(46.dp).height(64.dp));Column(Modifier.padding(start=12.dp),verticalArrangement=Arrangement.spacedBy(2.dp)){Text(book.title,style=MaterialTheme.typography.titleSmall);Text(book.author,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(if(book.fullTextAsset!=null)"Texto integral na app" else "Resumo e estrutura",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)}}
                            }}
                        }
                        val personEvents=article.eventsMentioning(personFigure)
                        val personSections=article.sectionsMentioning(personFigure)
                        if(personEvents.isNotEmpty()||personSections.isNotEmpty()){
                            item{SectionTitle("O seu percurso nesta tradição","Os marcos e as secções da história que mencionam ${personFigure.name}.")}
                            items(personEvents,key={"person-event-${it.period}-${it.title}"}){event->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                                Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primaryContainer){Text(event.period,Modifier.padding(horizontal=10.dp,vertical=6.dp),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onPrimaryContainer)}
                                Column(verticalArrangement=Arrangement.spacedBy(2.dp)){Text(event.title,style=MaterialTheme.typography.titleSmall);Text(event.text,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                            }}
                            items(personSections,key={"person-section-${it.title}"}){section->Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                                Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.width(4.dp).height(26.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary));Text(section.title,Modifier.padding(start=10.dp),style=MaterialTheme.typography.headlineSmall.copy(fontWeight=FontWeight.Bold),color=MaterialTheme.colorScheme.primary)}
                                LinkedText(section.text,MaterialTheme.typography.bodyLarge,state.baseBooks.map{it.id}.toSet(),state.histories.map{it.id}.toSet(),onBaseBook,{selected=it})
                            }}
                        }
                    }else{
                    val baseBooks=state.baseBooks.filter{it.traditionId==article.id}
                    val baseFileIds=baseBooks.mapNotNull{it.matchInLibrary(library.files)?.id}.toSet()
                    val importedFiles=library.files.filter{it.rightsStatus=="USER_AUTHORISED_NOT_VERIFIED"&&article.id in it.traditionIds&&it.id !in baseFileIds}
                    val tabs=buildList{
                        add("intro" to "Introdução")
                        if(article.sections.isNotEmpty())add("historia" to "História")
                        if(article.figures.isNotEmpty())add("nomes" to "Nomes de renome")
                        if(article.timeline.isNotEmpty())add("tempo" to "Linha do tempo")
                        if(article.practices.isNotEmpty())add("praticas" to "Práticas")
                        if(baseBooks.isNotEmpty()||importedFiles.isNotEmpty())add("livros" to "Livros-base")
                        if(article.conceptItems.isNotEmpty()||article.concepts.isNotBlank())add("conceitos" to "Conceitos")
                        if(loadPassages!=null&&baseBooks.any{it.fullTextAsset!=null})add("passagens" to "Passagens")
                    }
                    val effectiveTab=if(tabs.any{it.first==tab})tab else "intro"
                    item{Box(Modifier.clip(RoundedCornerShape(24.dp))){ArtworkBanner(article.id,article.title,height=200.dp)}}
                    item{Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("article-tabs"),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        tabs.forEach{(id,label)->Surface(onClick={tab=id},shape=RoundedCornerShape(50),color=if(effectiveTab==id)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,modifier=Modifier.testTag("article-tab-$id")){Text(label,Modifier.padding(horizontal=14.dp,vertical=8.dp),style=MaterialTheme.typography.labelLarge,color=if(effectiveTab==id)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)}}
                    }}
                    when(effectiveTab){
                        "historia"->items(article.sections){section->Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.width(4.dp).height(26.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary));Text(section.title,Modifier.padding(start=10.dp),style=MaterialTheme.typography.headlineSmall.copy(fontWeight=FontWeight.Bold),color=MaterialTheme.colorScheme.primary)}
                            LinkedText(section.text,MaterialTheme.typography.bodyLarge,state.baseBooks.map{it.id}.toSet(),state.histories.map{it.id}.toSet(),onBaseBook,{selected=it})
                            if(section.sources.isNotEmpty())Text("Fontes: "+section.sources.joinToString{ "[$it]" },style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
                        }}
                        "nomes"->items(article.figures){figure->Card(onClick={person=figure.name},modifier=Modifier.fillMaxWidth().testTag("figure-${figure.name}"),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                            Text(figure.name,style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
                            Text(figure.dates,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)
                            Text(figure.text,style=MaterialTheme.typography.bodyMedium)
                            Text("Ver percurso e livros →",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                        }}}
                        "tempo"->items(article.timeline){event->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                            Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primaryContainer){Text(event.period,Modifier.padding(horizontal=10.dp,vertical=6.dp),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onPrimaryContainer)}
                            Column(verticalArrangement=Arrangement.spacedBy(2.dp)){Text(event.title,style=MaterialTheme.typography.titleSmall);Text(event.text,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                        }}
                        "praticas"->items(article.practices){section->Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.width(4.dp).height(26.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary));Text(section.title,Modifier.padding(start=10.dp),style=MaterialTheme.typography.headlineSmall.copy(fontWeight=FontWeight.Bold),color=MaterialTheme.colorScheme.primary)}
                            Text(section.text,style=MaterialTheme.typography.bodyLarge)
                        }}
                        "livros"->{
                            items(baseBooks){book->Card(onClick={onBaseBook(book.id)},modifier=Modifier.fillMaxWidth().testTag("article-book-${book.id}"),shape=RoundedCornerShape(18.dp)){
                                Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){BookCover(book.title,book.traditionId,author=book.author,modifier=Modifier.width(46.dp).height(64.dp));Column(Modifier.padding(start=12.dp),verticalArrangement=Arrangement.spacedBy(2.dp)){Text(book.title,style=MaterialTheme.typography.titleSmall);Text(book.author,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(if(book.fullTextAsset!=null)"Texto integral na app" else "Resumo e estrutura",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)}}
                            }}
                            if(importedFiles.isNotEmpty()){
                                item{SectionTitle("Os teus livros importados","Livros que importaste e atribuíste a esta tradição.")}
                                items(importedFiles,key={"imported-${it.id}"}){file->FileCard(file){onReadFile(file)}}
                            }
                        }
                        "conceitos"->{
                            item{SectionTitle("Conceitos fundamentais","Os termos-chave, explicados um a um.")}
                            if(article.conceptItems.isNotEmpty())items(article.conceptItems){concept->Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                                Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.width(4.dp).height(26.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary));Text(concept.term,Modifier.padding(start=10.dp),style=MaterialTheme.typography.headlineSmall.copy(fontWeight=FontWeight.Bold),color=MaterialTheme.colorScheme.primary)}
                                Text(concept.text,style=MaterialTheme.typography.bodyLarge)
                            }}
                            else item{Text(article.concepts,style=MaterialTheme.typography.bodyLarge)}
                        }
                        "passagens"->{
                            val loaded=passages
                            if(loaded==null)item{Text("A preparar passagens…",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                            else if(loaded.isEmpty())item{Text("Sem passagens disponíveis para esta tradição.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                            else items(loaded){p->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer,contentColor=MaterialTheme.colorScheme.onPrimaryContainer)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                                Text(p.text,style=MaterialTheme.typography.bodyLarge)
                                Text("— ${p.bookTitle} · ${p.unitTitle}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                                TextButton(onClick={onPassage(p)}){Text("Ler no livro")}
                            }}}
                        }
                        else->{
                            item{Text(article.description,style=MaterialTheme.typography.bodyLarge)}
                            val highlights=buildList{
                                if(article.timeline.isNotEmpty())add("tempo" to "Linha do tempo · ${article.timeline.first().period} → ${article.timeline.last().period} · ${article.timeline.size} marcos")
                                if(article.figures.isNotEmpty())add("nomes" to "Nomes de renome · "+article.figures.take(3).joinToString(", "){it.name}+if(article.figures.size>3)"…" else "")
                                if(article.practices.isNotEmpty())add("praticas" to "Práticas · "+article.practices.take(2).joinToString(", "){it.title}+if(article.practices.size>2)"…" else "")
                                if(baseBooks.isNotEmpty()||importedFiles.isNotEmpty())add("livros" to "Livros-base · ${baseBooks.size+importedFiles.size} obras · ${baseBooks.count{it.fullTextAsset!=null}} com texto integral"+(if(importedFiles.isNotEmpty())" · ${importedFiles.size} ${if(importedFiles.size==1)"importado por ti" else "importados por ti"}" else ""))
                                if(article.sections.isNotEmpty())add("historia" to "História · ${article.sections.size} secções documentadas")
                            }
                            if(highlights.isNotEmpty())item{
                                Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer,contentColor=MaterialTheme.colorScheme.onPrimaryContainer)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                                    Text("Em resumo",style=MaterialTheme.typography.titleMedium)
                                    highlights.forEach{(tabId,line)->Text(line,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.fillMaxWidth().clickable{tab=tabId}.padding(vertical=2.dp))}
                                    Text("Toca numa linha para abrir o separador correspondente.",style=MaterialTheme.typography.labelSmall)
                                }}
                            }
                            article.sections.firstOrNull()?.let{first->item{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                                SectionTitle("A história em abertura")
                                Text(first.title,style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
                                val clean=first.text.replace(Regex("\\[\\[[^|\\]]+\\|([^\\]]+)\\]\\]"),"$1")
                                Text(if(clean.length>320)clean.take(320).substringBeforeLast(" ")+"…" else clean,style=MaterialTheme.typography.bodyLarge)
                                TextButton(onClick={tab="historia"}){Text("Continuar a ler na História")}
                            }}}
                            item{Column{Text("História e contexto",style=MaterialTheme.typography.titleLarge);Text("Texto editorial Sophia · fontes numeradas por secção · revisão de 29/10/2026",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
                            item{SectionTitle("Continuar a explorar")}
                            items(article.related){id->state.histories.firstOrNull{it.id==id}?.let{related->OutlinedButton(onClick={selected=id},modifier=Modifier.fillMaxWidth().testTag("related-$id")){Text("${related.symbol}  ${related.title}")}}}
                            item{SectionTitle("Fontes e proveniência","Links externos abrem no navegador e podem exigir Internet. O artigo pode ser lido offline.")}
                            items(article.sources.withIndex().toList()){(index,source)->
                                val uriHandler=LocalUriHandler.current
                                var failure by remember{mutableStateOf(false)}
                                Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
                                    Text("[${index+1}] ${source.title}",style=MaterialTheme.typography.titleSmall)
                                    Text(source.kind,style=MaterialTheme.typography.labelSmall)
                                    SelectionContainer{Text(source.url,style=MaterialTheme.typography.bodySmall)}
                                    TextButton(onClick={failure=runCatching{uriHandler.openUri(source.url)}.isFailure}){Text("Abrir fonte [${index+1}]")}
                                    if(failure)Text("Não foi possível abrir o navegador. Podes copiar a ligação acima.",color=MaterialTheme.colorScheme.error)
                                }
                            }
                            item{Text("Esta história acompanha os principais percursos documentados; não esgota todas as comunidades, épocas ou debates. As lacunas e incertezas não são preenchidas com relatos inventados.",style=MaterialTheme.typography.bodySmall)}
                        }
                    }
                    }
                }
            }}
        }
    }
}
