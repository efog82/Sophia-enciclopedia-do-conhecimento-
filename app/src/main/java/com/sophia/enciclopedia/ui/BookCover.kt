package com.sophia.enciclopedia.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sophia.enciclopedia.SophiaApplication

/**
 * Capa de um livro.
 *
 * Quando o ficheiro traz capa própria (ou primeira página, nos PDF), mostra-se
 * essa imagem real, com a legenda «Capa do ficheiro»/«Primeira página».
 *
 * Quando não há capa do ficheiro, gera-se uma capa de edição ilustrativa da
 * Sophia: composição tipográfica própria — autor no topo, título em destaque e
 * o símbolo da tradição como motivo central, sobre a paleta dessa tradição,
 * com moldura dupla e ombro de lombada. É uma edição ilustrativa da Sophia,
 * nunca um fac-símile de uma edição publicada.
 */
@Composable fun BookCover(title:String,traditionId:String?=null,fileId:String?=null,mimeType:String?=null,author:String?=null,modifier:Modifier=Modifier){
    val context=LocalContext.current
    var bitmap by remember(fileId){mutableStateOf<Bitmap?>(null)}
    LaunchedEffect(fileId){
        if(fileId!=null)bitmap=(context.applicationContext as SophiaApplication).container.library.cover(fileId)
    }
    Column(modifier.testTag(if(fileId==null)"reference-cover" else "cover-$fileId")){
        val actual=bitmap
        if(actual!=null){
            Image(actual.asImageBitmap(),contentDescription=if(mimeType=="application/pdf")"Primeira página de $title" else "Capa do ficheiro: $title",modifier=Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(6.dp)),contentScale=ContentScale.Fit)
            Text(if(mimeType=="application/pdf")"Primeira página" else "Capa do ficheiro",style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
        }else{
            GeneratedCover(title=title,author=author,traditionId=traditionId,modifier=Modifier.weight(1f).fillMaxWidth())
        }
    }
}

/** Paleta sóbria de edição de uma tradição: fundo, fundo profundo (base do gradiente), tinta e acento. */
private data class EditionPalette(val background:Color,val deep:Color,val ink:Color,val accent:Color)

private fun editionPalette(traditionId:String?):EditionPalette=when(traditionId){
    "hinduism"->EditionPalette(Color(0xFF8A3324),Color(0xFF5E2117),Color(0xFFF7E3B0),Color(0xFFE9A13B))
    "buddhism"->EditionPalette(Color(0xFF8A5A13),Color(0xFF5C3B0B),Color(0xFFFFEFC2),Color(0xFFF2C14E))
    "spiritism"->EditionPalette(Color(0xFF2A5A7B),Color(0xFF18394F),Color(0xFFDCEEF7),Color(0xFF8FC1DE))
    "christianity"->EditionPalette(Color(0xFF5B2333),Color(0xFF3A1521),Color(0xFFF0DCB2),Color(0xFFC9A86A))
    "judaism"->EditionPalette(Color(0xFF21395E),Color(0xFF132539),Color(0xFFD9E6F2),Color(0xFF7FA8D7))
    "islam"->EditionPalette(Color(0xFF0F5132),Color(0xFF093620),Color(0xFFDDEED6),Color(0xFFD4B86A))
    "taoism"->EditionPalette(Color(0xFF2B2B2B),Color(0xFF121212),Color(0xFFF0EDE6),Color(0xFFB8B2A7))
    "confucianism"->EditionPalette(Color(0xFF7A1F1F),Color(0xFF4E1212),Color(0xFFF3DFAE),Color(0xFFD9A441))
    "hermeticism"->EditionPalette(Color(0xFF1F4D4A),Color(0xFF12302E),Color(0xFFD6EBD9),Color(0xFFC0A062))
    "ancient-philosophy"->EditionPalette(Color(0xFF5A5346),Color(0xFF38332B),Color(0xFFF0E6CC),Color(0xFFC2B280))
    "atheism"->EditionPalette(Color(0xFF3F4756),Color(0xFF272C36),Color(0xFFE6E8EC),Color(0xFF9AA5B1))
    "religions"->EditionPalette(Color(0xFF54426A),Color(0xFF372A47),Color(0xFFE9DFF2),Color(0xFFB79FD1))
    "philosophy"->EditionPalette(Color(0xFF2E4053),Color(0xFF1B2835),Color(0xFFE3DDD0),Color(0xFF93A8BB))
    "mysticism"->EditionPalette(Color(0xFF3E2A5E),Color(0xFF271A3F),Color(0xFFE8DFF7),Color(0xFFB79FE0))
    "esotericism"->EditionPalette(Color(0xFF274156),Color(0xFF172938),Color(0xFFDCE9E4),Color(0xFF7FB3A3))
    "occultism"->EditionPalette(Color(0xFF1C1A1E),Color(0xFF0D0C0F),Color(0xFFE8C96A),Color(0xFFD4AF37))
    "spirituality"->EditionPalette(Color(0xFF2F5D50),Color(0xFF1D3E35),Color(0xFFE2EFD9),Color(0xFFA8C69F))
    else->EditionPalette(Color(0xFF334155),Color(0xFF1E293B),Color(0xFFEDE6D6),Color(0xFF94A3B8))
}

private fun traditionSymbol(traditionId:String?):String=when(traditionId){
    "hinduism"->"ॐ"
    "buddhism"->"☸"
    "spiritism"->"✧"
    "christianity"->"✝"
    "judaism"->"✡"
    "islam"->"☪"
    "taoism"->"☯"
    "confucianism"->"儒"
    "hermeticism"->"⚚"
    "ancient-philosophy"->"Φ"
    "atheism"->"∴"
    "religions"->"⌂"
    "philosophy"->"Φ"
    "mysticism"->"◉"
    "esotericism"->"◇"
    "occultism"->"✶"
    "spirituality"->"❋"
    else->"✧"
}

/**
 * A capa gerada. Tem de degradar bem de 46×64 dp (listas) a 130×184 dp
 * (detalhes): os tamanhos de letra, as margens e a moldura derivam da largura
 * disponível, o título corta com reticências e o símbolo é proporcional.
 */
@Composable private fun GeneratedCover(title:String,author:String?,traditionId:String?,modifier:Modifier=Modifier){
    val palette=editionPalette(traditionId)
    val symbol=traditionSymbol(traditionId)
    BoxWithConstraints(modifier.clip(RoundedCornerShape(6.dp)).background(Brush.verticalGradient(listOf(palette.background,palette.deep)))){
        val tiny=maxWidth<60.dp
        val small=maxWidth<100.dp
        val spineWidth=if(tiny)4.dp else 7.dp
        // Ombro/lombada subtil: faixa vertical mais escura à esquerda, com o filete de luz da dobra.
        Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().width(spineWidth).background(Color.Black.copy(alpha=0.22f)))
        Box(Modifier.align(Alignment.CenterStart).padding(start=spineWidth).fillMaxHeight().width(1.dp).background(Color.White.copy(alpha=0.14f)))
        val frameInset=if(tiny)3.dp else if(small)5.dp else 7.dp
        val contentPadding=if(tiny)3.dp else if(small)6.dp else 9.dp
        // Moldura fina interior: dupla cercadura em tinta esbatida.
        Box(Modifier.fillMaxSize().padding(frameInset).border(0.8.dp,palette.ink.copy(alpha=0.55f),RoundedCornerShape(2.dp)).padding(2.dp).border(0.5.dp,palette.ink.copy(alpha=0.35f),RoundedCornerShape(1.dp)).padding(contentPadding)){
            Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally){
                if(!author.isNullOrBlank()){
                    Text(author.uppercase(),style=TextStyle(fontSize=if(tiny)6.sp else if(small)7.5.sp else 9.sp,letterSpacing=if(tiny)0.6.sp else if(small)1.sp else 1.4.sp,fontWeight=FontWeight.Medium),color=palette.ink.copy(alpha=0.88f),textAlign=TextAlign.Center,maxLines=2,overflow=TextOverflow.Ellipsis)
                    Spacer(Modifier.height(if(tiny)3.dp else 6.dp))
                }
                Text(title,style=TextStyle(fontFamily=FontFamily.Serif,fontSize=if(tiny)9.sp else if(small)13.sp else 17.sp,lineHeight=if(tiny)11.sp else if(small)16.sp else 21.sp,fontWeight=FontWeight.Bold),color=palette.ink,textAlign=TextAlign.Center,maxLines=if(tiny)3 else 4,overflow=TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(if(tiny)14.dp else if(small)20.dp else 28.dp).height(1.dp).background(palette.accent.copy(alpha=0.7f)))
                Spacer(Modifier.height(if(tiny)3.dp else 6.dp))
                Text(symbol,style=TextStyle(fontSize=if(tiny)15.sp else if(small)24.sp else 36.sp),color=palette.accent,textAlign=TextAlign.Center,maxLines=1)
                Spacer(Modifier.height(if(tiny)3.dp else 6.dp))
                Box(Modifier.width(if(tiny)14.dp else if(small)20.dp else 28.dp).height(1.dp).background(palette.accent.copy(alpha=0.7f)))
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
