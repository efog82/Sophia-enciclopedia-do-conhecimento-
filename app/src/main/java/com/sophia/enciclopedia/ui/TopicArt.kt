package com.sophia.enciclopedia.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sophia.enciclopedia.R

/**
 * Ilustrações locais (WebP em drawable-nodpi) de cada tradição/área da enciclopédia.
 * São imagens decorativas geradas para a Sophia: a app continua 100% offline e o
 * texto de leitura mantém-se texto real, fora das imagens.
 */
fun topicArtResource(topicId:String):Int=when(topicId){
    "hinduism"->R.drawable.art_hinduism
    "buddhism"->R.drawable.art_buddhism
    "spiritism"->R.drawable.art_spiritism
    "christianity"->R.drawable.art_christianity
    "judaism"->R.drawable.art_judaism
    "islam"->R.drawable.art_islam
    "taoism"->R.drawable.art_taoism
    "confucianism"->R.drawable.art_confucianism
    "hermeticism"->R.drawable.art_hermeticism
    "ancient-philosophy"->R.drawable.art_ancient_philosophy
    "atheism"->R.drawable.art_atheism
    "religions"->R.drawable.art_religions
    "philosophy"->R.drawable.art_philosophy
    "mysticism"->R.drawable.art_mysticism
    "esotericism"->R.drawable.art_esotericism
    "occultism"->R.drawable.art_occultism
    "spirituality"->R.drawable.art_spirituality
    else->R.drawable.art_religions
}

@Composable fun TopicArtwork(topicId:String,modifier:Modifier=Modifier,contentDescription:String?=null){
    Image(painter=painterResource(topicArtResource(topicId)),contentDescription=contentDescription,contentScale=ContentScale.Crop,modifier=modifier)
}

/** Faixa ilustrada com título sobre gradiente, para heróis e cabeçalhos de artigo. */
@Composable fun ArtworkBanner(topicId:String,title:String,description:String?=null,modifier:Modifier=Modifier,height:androidx.compose.ui.unit.Dp=216.dp,label:String?=null){
    Box(modifier.fillMaxWidth().height(height)){
        TopicArtwork(topicId,Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Black.copy(alpha=0.38f),Color.Black.copy(alpha=0.78f)),startY=0.25f)))
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            label?.let{Text(it,style=MaterialTheme.typography.labelSmall,color=Color.White.copy(alpha=0.85f))}
            Text(title,style=MaterialTheme.typography.headlineMedium,color=Color.White)
            description?.let{Text(it,style=MaterialTheme.typography.bodyMedium,color=Color.White.copy(alpha=0.92f))}
        }
    }
}
