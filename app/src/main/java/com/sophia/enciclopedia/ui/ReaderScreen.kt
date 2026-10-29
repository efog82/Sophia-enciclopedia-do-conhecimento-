package com.sophia.enciclopedia.ui

import android.graphics.Bitmap
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sophia.domain.*
import com.sophia.enciclopedia.*
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ReaderScreen(vm:ReaderViewModel,onClose:()->Unit){
    val state by vm.state.collectAsStateWithLifecycle();val theme by vm.theme.collectAsStateWithLifecycle();val font by vm.font.collectAsStateWithLifecycle();val marks by vm.bookmarks.collectAsStateWithLifecycle();val message by vm.message.collectAsStateWithLifecycle()
    val dark=theme==ThemeMode.DARK||(theme==ThemeMode.SYSTEM&&isSystemInDarkTheme());val scope=rememberCoroutineScope()
    var indexOpen by rememberSaveable{mutableStateOf(false)};var marksOpen by rememberSaveable{mutableStateOf(false)};var noteOpen by rememberSaveable{mutableStateOf(false)}
    BackHandler{vm.close(onClose)}
    SophiaTheme(theme){Scaffold(topBar={TopAppBar(title={Text(state.info?.title?:"Leitor",maxLines=1)},navigationIcon={IconButton(onClick={vm.close(onClose)},modifier=Modifier.testTag("close-reader")){Icon(Icons.Outlined.ArrowBack,"Voltar à biblioteca")}},actions={IconButton(onClick={vm.theme(!dark)},modifier=Modifier.testTag("reader-theme")){Icon(Icons.Outlined.DarkMode,"Alternar tema do leitor")}})}){padding->Column(Modifier.padding(padding).fillMaxSize().testTag("reader-screen")){
        when{
            state.loading->CircularProgressIndicator(Modifier.padding(24.dp))
            state.error!=null->Column(Modifier.padding(20.dp)){EmptyPanel("Leitura indisponível",state.error!!);Button(onClick=vm::load){Text("Tentar novamente")}}
            state.document!=null->{val doc=state.document!!;val unit=doc.units[state.unit]
                Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){TextButton(onClick={indexOpen=true},modifier=Modifier.weight(1f).testTag("reader-index")){Text("${state.unit+1}/${doc.units.size} · ${unit.title}",maxLines=1)};if(state.info!!.mimeType!="application/pdf"){TextButton(onClick={vm.font(font-2)},enabled=font>12,modifier=Modifier.testTag("font-minus")){Text("A−")};Text("$font",modifier=Modifier.testTag("reader-font"));TextButton(onClick={vm.font(font+2)},enabled=font<36,modifier=Modifier.testTag("font-plus")){Text("A+")}}}
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()){
                    when(state.info!!.mimeType){
                        "application/pdf"->if(state.bitmap==null)CircularProgressIndicator(Modifier.align(Alignment.Center))else PdfSurface(state.bitmap!!,dark)
                        "application/epub+zip"->EpubSurface(state,font,dark,vm)
                        else->key(state.unit){val scroll=rememberScrollState();val initialFraction=remember{state.fraction};var restored by remember{mutableStateOf(false)};LaunchedEffect(scroll.maxValue){if(scroll.maxValue>0&&!restored){scroll.scrollTo((initialFraction*scroll.maxValue).toInt());restored=true}};LaunchedEffect(scroll){snapshotFlow{scroll.value to scroll.maxValue}.collect{(value,max)->if(restored&&max>0)vm.scroll(value.toDouble()/max)}};SelectionContainer{Text(unit.text.orEmpty(),Modifier.fillMaxSize().verticalScroll(scroll).padding(24.dp).testTag("txt-content"),fontSize=font.sp,lineHeight=(font*1.65).sp)}}
                    }
                }
                if(state.info!!.mimeType=="application/pdf")Text("PDF: letra fixa; usa dois dedos para ampliar e deslocar.",Modifier.padding(horizontal=12.dp),style=MaterialTheme.typography.labelSmall)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){IconButton(onClick={vm.go(state.unit-1)},enabled=state.unit>0,modifier=Modifier.testTag("reader-previous")){Icon(Icons.Outlined.NavigateBefore,"Anterior")};IconButton(onClick=vm::bookmark,modifier=Modifier.testTag("add-bookmark")){Icon(Icons.Outlined.BookmarkAdd,"Adicionar marcador")};IconButton(onClick={marksOpen=true},modifier=Modifier.testTag("open-bookmarks")){Icon(Icons.Outlined.Bookmarks,"Ver marcadores")};IconButton(onClick={noteOpen=true},modifier=Modifier.testTag("reader-note")){Icon(Icons.Outlined.EditNote,"Criar nota nesta posição")};IconButton(onClick={vm.go(state.unit+1)},enabled=state.unit<doc.units.lastIndex,modifier=Modifier.testTag("reader-next")){Icon(Icons.Outlined.NavigateNext,"Seguinte")}}
            }
        }
    }}
    if(indexOpen)LibraryDialog("Índice de leitura",{indexOpen=false}){Text("EPUB: títulos dos recursos na ordem real do ficheiro. TXT: blocos técnicos, não capítulos inventados. PDF: páginas.");state.document?.units?.forEachIndexed{i,u->TextButton(onClick={vm.go(i);indexOpen=false},modifier=Modifier.fillMaxWidth()){Text("${i+1}. ${u.title}")}}}
    if(marksOpen)LibraryDialog("Marcadores",{marksOpen=false}){if(marks.isEmpty())Text("Ainda não há marcadores neste ficheiro.");marks.forEach{mark->Row(verticalAlignment=Alignment.CenterVertically){TextButton(onClick={vm.go(mark.location.unit,mark.location.fraction);marksOpen=false},modifier=Modifier.weight(1f)){Text(mark.label)};IconButton(onClick={vm.deleteBookmark(mark.id)}){Icon(Icons.Outlined.DeleteOutline,"Eliminar marcador: ${mark.label}")}}}}
    if(noteOpen){var title by rememberSaveable{mutableStateOf((state.info?.title.orEmpty()+" · "+state.document!!.units[state.unit].title).take(160))};var body by rememberSaveable{mutableStateOf("")};var excerpt by rememberSaveable{mutableStateOf("")};var error by remember{mutableStateOf<String?>(null)};var saving by remember{mutableStateOf(false)}
        LibraryDialog("Nota nesta posição",{if(!saving)noteOpen=false}){Text("A nota ficará ligada ao ficheiro e à posição atual. Para guardar uma passagem exata, copia-a do texto e cola-a no excerto. Não são geradas citações.");OutlinedTextField(title,{if(it.length<=160)title=it},label={Text("Título")},modifier=Modifier.fillMaxWidth().testTag("passage-note-title"));OutlinedTextField(body,{if(it.length<=50000)body=it},label={Text("Apontamentos")},minLines=3,modifier=Modifier.fillMaxWidth().testTag("passage-note-body"));OutlinedTextField(excerpt,{if(it.length<=10000)excerpt=it},label={Text("Excerto copiado pelo utilizador (opcional)")},modifier=Modifier.fillMaxWidth());error?.let{Text(it,color=MaterialTheme.colorScheme.error)};Button(onClick={saving=true;scope.launch{try{vm.saveNote(title,body,excerpt);noteOpen=false}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message?:"Não foi possível guardar a nota."}finally{saving=false}}},enabled=!saving,modifier=Modifier.testTag("save-passage-note")){Text("Guardar no caderno")}}
    }
    if(message!=null)AlertDialog(onDismissRequest=vm::clearMessage,title={Text("Leitura")},text={Text(message!!)},confirmButton={TextButton(onClick=vm::clearMessage){Text("Fechar")}})
    }
}
@Composable private fun PdfSurface(bitmap:Bitmap,dark:Boolean){var scale by remember(bitmap){mutableFloatStateOf(1f)};var offset by remember(bitmap){mutableStateOf(Offset.Zero)};val transform=rememberTransformableState{zoom,pan,_->scale=(scale*zoom).coerceIn(1f,4f);offset=if(scale<=1f)Offset.Zero else (offset+pan).let{Offset(it.x.coerceIn(-1800f,1800f),it.y.coerceIn(-2400f,2400f))}};Box(Modifier.fillMaxSize().transformable(transform)){Image(bitmap.asImageBitmap(),"Página PDF",Modifier.fillMaxSize().graphicsLayer{scaleX=scale;scaleY=scale;translationX=offset.x;translationY=offset.y},contentScale=ContentScale.Fit,colorFilter=if(dark)ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(-1f,0f,0f,0f,255f,0f,-1f,0f,0f,255f,0f,0f,-1f,0f,255f,0f,0f,0f,1f,0f)))else null);if(scale>1f)TextButton(onClick={scale=1f;offset=Offset.Zero},modifier=Modifier.align(Alignment.BottomCenter)){Text("Repor zoom")}}}
private class LocalBookWebView(context:android.content.Context):WebView(context){var onScroll:((Double)->Unit)?=null;var restoring=true;override fun onScrollChanged(l:Int,t:Int,oldl:Int,oldt:Int){super.onScrollChanged(l,t,oldl,oldt);if(!restoring){val range=(contentHeight*resources.displayMetrics.density-height).coerceAtLeast(0f);if(range>0)onScroll?.invoke((t/range).toDouble().coerceIn(0.0,1.0))}}}
private data class LocalHtmlPage(val key:String,val html:String?=null,val error:String?=null)
@Composable private fun EpubSurface(state:ReaderState,font:Int,dark:Boolean,vm:ReaderViewModel){
    val href=state.document!!.units[state.unit].href
    val requestKey="${state.info!!.id}:${state.unit}:$font:$dark"
    val page by produceState(LocalHtmlPage(""),requestKey){
        val loaded=try{LocalHtmlPage(requestKey,withContext(Dispatchers.IO){BookDocuments.safeHtml(state.localFile!!,href,dark,font)})}
        catch(e:CancellationException){throw e}
        catch(e:Exception){LocalHtmlPage(requestKey,error="Não foi possível ler este recurso EPUB. Volte ao índice ou tente abrir o ficheiro novamente.")}
        value=loaded
    }
    if(page.key!=requestKey){CircularProgressIndicator(Modifier.padding(24.dp));return}
    if(page.error!=null){EmptyPanel("Recurso indisponível",page.error!!);return}
    val html=page.html?:return
    key(requestKey){
        val initialFraction=remember{state.fraction}
        AndroidView(factory={context->LocalBookWebView(context).apply{
            settings.javaScriptEnabled=false
            settings.allowFileAccess=false
            settings.allowContentAccess=false
            settings.blockNetworkLoads=true
            settings.domStorageEnabled=false
            settings.cacheMode=WebSettings.LOAD_NO_CACHE
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
            onScroll=vm::scroll
            webViewClient=object:WebViewClient(){
                override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse{
                    val url=request.url
                    val path=url.path?.removePrefix("/")?:""
                    if(url.scheme!="https"||url.host!="sophia.invalid")return blocked()
                    return try{
                        val safe=BookDocuments.normalizedPath("",path)
                        if(request.isForMainFrame && safe==href){
                            WebResourceResponse("text/html","UTF-8",ByteArrayInputStream(html.toByteArray(Charsets.UTF_8))).apply{responseHeaders=mapOf("Cache-Control" to "no-store")}
                        }else{
                            val bytes=BookDocuments.resource(state.localFile!!,safe)
                            val mime=when(safe.substringAfterLast('.').lowercase()){"jpg","jpeg"->"image/jpeg";"png"->"image/png";"webp"->"image/webp";"gif"->"image/gif";"svg"->"image/svg+xml";else->"application/octet-stream"}
                            WebResourceResponse(mime,null,ByteArrayInputStream(bytes))
                        }
                    }catch(e:Exception){blocked()}
                }
                override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean{
                    val url=request.url
                    if(url.scheme=="https"&&url.host=="sophia.invalid"){
                        val index=state.document.units.indexOfFirst{it.href==url.path?.removePrefix("/")}
                        if(index>=0&&index!=state.unit)vm.go(index)
                        if(index==state.unit)return false
                    }
                    return true
                }
                override fun onPageFinished(view:WebView,url:String){
                    view.post{view.scrollTo(0,(initialFraction*(view.contentHeight*view.resources.displayMetrics.density-view.height).coerceAtLeast(0f)).toInt());(view as LocalBookWebView).restoring=false}
                }
            }
            loadUrl("https://sophia.invalid/$href")
        }},modifier=Modifier.fillMaxSize().testTag("epub-content"),onRelease={it.stopLoading();it.destroy()})
    }
}
private fun blocked()=WebResourceResponse("text/plain","UTF-8",403,"Bloqueado",emptyMap(),ByteArrayInputStream(ByteArray(0)))
