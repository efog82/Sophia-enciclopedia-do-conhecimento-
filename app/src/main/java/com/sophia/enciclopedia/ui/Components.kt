package com.sophia.enciclopedia.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sophia.domain.BaseBook
import com.sophia.domain.BaseBookRights
import com.sophia.domain.Book

@Composable fun EmptyPanel(title:String,text:String,icon:ImageVector=Icons.Outlined.AutoStories){
    OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Icon(icon,null,tint=MaterialTheme.colorScheme.secondary,modifier=Modifier.size(28.dp))
            Text(title,style=MaterialTheme.typography.titleMedium)
            Text(text,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable fun SectionTitle(title:String,description:String?=null){Column(verticalArrangement=Arrangement.spacedBy(4.dp)){Text(title,style=MaterialTheme.typography.titleLarge);description?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
@Composable fun BookCard(book:Book,busy:Boolean=false,onOpen:()->Unit,onFavorite:()->Unit,showFavorite:Boolean=true){
    Card(Modifier.fillMaxWidth().testTag("book-${book.id}").clickable(onClick=onOpen),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
            BookCover(book.title,book.traditions.firstOrNull()?.id,modifier=Modifier.width(72.dp).height(104.dp))
            Column(Modifier.weight(1f).padding(start=12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text(book.title,style=MaterialTheme.typography.titleSmall,maxLines=2,overflow=TextOverflow.Ellipsis)
                Text(book.contributorLabel,style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(book.traditions.joinToString(" · "){it.name},style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
                Text(if(book.editions.sumOf{it.fileCount}>0)"${book.editions.sumOf{it.fileCount}} ficheiros offline" else "Referência · sem ficheiro",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(showFavorite)IconButton(enabled=!busy,onClick=onFavorite,modifier=Modifier.testTag("favorite-${book.id}").semantics{stateDescription=if(book.favorite)"Nos favoritos" else "Não favorito"}){Icon(if(book.favorite)Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,if(book.favorite)"Remover dos favoritos: ${book.title}" else "Adicionar aos favoritos: ${book.title}",tint=if(book.favorite)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)}
        }
    }
}
private val WIKI_LINK=Regex("\\[\\[([^|\\]]+)\\|([^\\]]+)\\]\\]")
/** Texto com ligações internas [[id|rótulo]]: ids de livros-base abrem o livro; ids de artigos navegam para o tema. Ids desconhecidos aparecem como texto simples. */
@Composable fun LinkedText(text:String,style:TextStyle,bookIds:Set<String>,articleIds:Set<String>,onBook:(String)->Unit,onArticle:(String)->Unit){
    val linkColor=MaterialTheme.colorScheme.primary
    val annotated=remember(text,bookIds,articleIds){buildAnnotatedString{
        var last=0
        for(m in WIKI_LINK.findAll(text)){
            append(text.substring(last,m.range.first))
            val id=m.groupValues[1].trim();val label=m.groupValues[2]
            val kind=when{id in bookIds->"book";id in articleIds->"article";else->null}
            if(kind==null)append(label) else {pushStringAnnotation(kind,id);withStyle(SpanStyle(color=linkColor,textDecoration=TextDecoration.Underline)){append(label)};pop()}
            last=m.range.last+1
        }
        append(text.substring(last))
    }}
    ClickableText(text=annotated,style=style,onClick={offset->
        annotated.getStringAnnotations("book",offset,offset).firstOrNull()?.let{onBook(it.item)}
        annotated.getStringAnnotations("article",offset,offset).firstOrNull()?.let{onArticle(it.item)}
    })
}
fun BaseBook.statusLine():String = when {
    fullTextAsset!=null->"Texto integral incluído · lê e pesquisa na app"
    rights==BaseBookRights.PUBLIC_DOMAIN->"Domínio público · texto integral a integrar"
    else->"Resumo Sophia · texto protegido por direitos"
}
@Composable fun BaseBookCard(book:BaseBook,traditionName:String,onOpen:()->Unit){
    Card(Modifier.fillMaxWidth().testTag("basebook-${book.id}").clickable(onClick=onOpen),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
            BookCover(book.title,book.traditionId,modifier=Modifier.width(72.dp).height(104.dp))
            Column(Modifier.weight(1f).padding(start=12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text(book.title,style=MaterialTheme.typography.titleSmall,maxLines=2,overflow=TextOverflow.Ellipsis)
                Text(book.author+(book.year?.let{" · $it"}?:""),style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(traditionName,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
                Text(book.statusLine(),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
@Composable fun SelectFilter(label:String,value:String?,options:List<Pair<String,String>>,onChange:(String?)->Unit,modifier:Modifier=Modifier,emptyLabel:String="Todos"){
    var expanded by remember{mutableStateOf(false)}
    Column(modifier){Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Box{OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)){
            Text(options.firstOrNull{it.first==value}?.second?:emptyLabel,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis);Icon(Icons.Outlined.ExpandMore,null,Modifier.size(18.dp))
        }
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false},modifier=Modifier.heightIn(max=340.dp)){
                DropdownMenuItem(text={Text(emptyLabel)},onClick={onChange(null);expanded=false})
                options.forEach{(id,title)->DropdownMenuItem(text={Text(title)},onClick={onChange(id);expanded=false})}
            }
        }
    }
}
