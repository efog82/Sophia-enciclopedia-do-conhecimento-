package com.sophia.enciclopedia.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sophia.enciclopedia.SophiaApplication

/** Original typography is a fallback, never a claimed facsimile of a published edition. */
@Composable fun BookCover(title:String,traditionId:String?=null,fileId:String?=null,mimeType:String?=null,modifier:Modifier=Modifier){
    val context=LocalContext.current
    var bitmap by remember(fileId){mutableStateOf<Bitmap?>(null)}
    LaunchedEffect(fileId){
        if(fileId!=null)bitmap=(context.applicationContext as SophiaApplication).container.library.cover(fileId)
    }
    val symbol=when(traditionId){"hinduism"->"ॐ";"buddhism"->"☸";"christianity"->"✝";"judaism"->"✡";"islam"->"☪";"taoism"->"☯";"ancient-philosophy"->"Φ";"confucianism"->"儒";else->"✧"}
    val palettes=listOf(Color(0xFF174D49),Color(0xFF3C3569),Color(0xFF6B3F32),Color(0xFF294866))
    val color=palettes[(title.hashCode().toLong() and 0x7fffffff).rem(palettes.size).toInt()]
    Column(modifier.testTag(if(fileId==null)"reference-cover" else "cover-$fileId")){
        val actual=bitmap
        if(actual!=null)Image(actual.asImageBitmap(),contentDescription=if(mimeType=="application/pdf")"Primeira página de $title" else "Capa do ficheiro: $title",modifier=Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(6.dp)),contentScale=ContentScale.Fit)
        else Column(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Brush.verticalGradient(listOf(color,color.copy(alpha=.82f)))).padding(8.dp),verticalArrangement=Arrangement.SpaceBetween){
            Text("SOPHIA",style=MaterialTheme.typography.labelSmall,color=Color(0xFFF1D8A0))
            Text(symbol,style=MaterialTheme.typography.titleLarge,color=Color(0xFFF1D8A0))
            Text(title,style=MaterialTheme.typography.labelSmall,color=Color.White,maxLines=3,overflow=TextOverflow.Ellipsis)
        }
        Text(if(actual==null)"Capa ilustrativa" else if(mimeType=="application/pdf")"Primeira página" else "Capa do ficheiro",style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
