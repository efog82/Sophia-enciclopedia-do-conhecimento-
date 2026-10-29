package com.sophia.enciclopedia.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sophia.enciclopedia.SophiaState

@Composable fun TraditionsDialog(state:SophiaState,initialTopic:String?,onClose:()->Unit,onBook:(String)->Unit,onResearch:(String)->Unit){
    var selected by rememberSaveable(initialTopic){mutableStateOf(initialTopic)}
    val article=state.histories.firstOrNull{it.id==selected}
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)){
        BackHandler(enabled=article!=null){selected=null}
        Surface(Modifier.fillMaxWidth().widthIn(max=800.dp).fillMaxHeight(.96f).padding(10.dp),shape=MaterialTheme.shapes.large){
            key(selected){LazyColumn(Modifier.fillMaxSize().testTag("history-screen"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
                item{Row{TextButton(onClick=onClose){Text("Fechar")};if(article!=null)TextButton(onClick={selected=null}){Text("Todos os temas")}}}
                if(article==null){
                    item{SectionTitle("Tradições e perspetivas","História documentada · crença e não crença")}
                    items(state.histories.filter{!it.area},key={it.id}){topic->OutlinedCard(onClick={selected=topic.id},modifier=Modifier.fillMaxWidth().testTag("tradition-${topic.id}")){
                        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text("${topic.symbol}  ${topic.title}",style=MaterialTheme.typography.titleMedium);Text(topic.description)}
                    }}
                }else{
                    item{Text("${article.symbol}  ${article.title}",style=MaterialTheme.typography.headlineMedium);Text(article.description,Modifier.padding(top=8.dp))}
                    item{Text("História e contexto",style=MaterialTheme.typography.titleLarge);Text("Texto editorial Sophia · fontes numeradas por secção · revisão de 29/10/2026",style=MaterialTheme.typography.labelSmall)}
                    items(article.sections){section->Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                        Text(section.title,style=MaterialTheme.typography.titleMedium)
                        SelectionContainer{Text(section.text,style=MaterialTheme.typography.bodyLarge)}
                        Text("Fontes: "+section.sources.joinToString{ "[$it]" },style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
                    }}
                    if(article.concepts.isNotBlank())item{SectionTitle("Conceitos fundamentais");Text(article.concepts,Modifier.padding(top=8.dp))}
                    val references=state.books.filter{book->if(article.area)book.categories.any{it.id==article.id} else book.traditions.any{it.id==article.id}}
                    item{SectionTitle("Livros de referência e autores","Lista bibliográfica · não inclui textos integrais. As capas ilustrativas não reproduzem edições comerciais.")}
                    if(references.isEmpty())item{Text(if(article.id=="atheism")"David Hume · História Natural da Religião (1757) e Diálogos sobre a Religião Natural (1779). Referências para estudo; nenhuma edição ou tradução incluída." else "Ainda não há referências específicas nesta área. Explora também os temas relacionados.")}
                    items(references,key={"reference-${it.id}"}){book->BookCard(book,onOpen={onBook(book.id)},onFavorite={},showFavorite=false)}
                    item{SectionTitle("Continuar a explorar")}
                    items(article.related){id->state.histories.firstOrNull{it.id==id}?.let{related->OutlinedButton(onClick={selected=id},modifier=Modifier.fillMaxWidth().testTag("related-$id")){Text("${related.symbol}  ${related.title}")}}}
                    if(!article.area&&article.id!="atheism")item{OutlinedButton(onClick={onResearch(article.id)},modifier=Modifier.fillMaxWidth()){Text("Investigar referências desta tradição")}}
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
            }}
        }
    }
}
