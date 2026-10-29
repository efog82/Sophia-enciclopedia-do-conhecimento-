package com.sophia.enciclopedia.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sophia.data.*
import com.sophia.enciclopedia.StudyViewModel
import java.text.DateFormat
import java.util.Date

@Composable fun StudyScreen(vm:StudyViewModel){
    val conversations by vm.conversations.collectAsStateWithLifecycle();val current by vm.selected.collectAsStateWithLifecycle();val messages by vm.messages.collectAsStateWithLifecycle();val providers by vm.providers.collectAsStateWithLifecycle();val models by vm.models.collectAsStateWithLifecycle();val busy by vm.busy.collectAsStateWithLifecycle();val status by vm.status.collectAsStateWithLifecycle();val feedback by vm.feedback.collectAsStateWithLifecycle();val consent by vm.consent.collectAsStateWithLifecycle()
    var settings by rememberSaveable{mutableStateOf(false)};var history by rememberSaveable{mutableStateOf(false)};var deleting by rememberSaveable{mutableStateOf<String?>(null)};var draft by rememberSaveable(current){mutableStateOf("")};var choice by rememberSaveable{mutableStateOf<String?>(null)};var modelMenu by remember{mutableStateOf(false)}
    val enabled=providers.filter{it.enabled&&it.freeAccountConfirmed}.map{it.id}.toSet();val available=models.filter{it.provider in enabled&&it.validatedAt>0&&it.cooldownUntil<=System.currentTimeMillis()};val conversation=conversations.firstOrNull{it.id==current};val clipboard=LocalClipboardManager.current
    Column(Modifier.fillMaxSize().testTag("assistant-screen")){
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){Text("Estudar com IA",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f));IconButton(onClick={history=true},enabled=!busy){Icon(Icons.Outlined.History,"Conversas de estudo")};IconButton(onClick={vm.new()},enabled=!busy){Icon(Icons.Outlined.Add,"Novo estudo")};IconButton(onClick={settings=true},modifier=Modifier.testTag("study-settings")){Icon(Icons.Outlined.Settings,"Fornecedores de IA")}}
        Row(Modifier.padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f)){TextButton(onClick={modelMenu=true},enabled=!busy){Text(choice?.let{c->available.firstOrNull{"${it.provider}/${it.modelId}"==c}?.label}?:"Automático · ${available.size} modelos testados")};DropdownMenu(expanded=modelMenu,onDismissRequest={modelMenu=false}){DropdownMenuItem(text={Text("Automático: alternar entre modelos configurados")},onClick={choice=null;modelMenu=false});available.forEach{m->DropdownMenuItem(text={Text("${StudyProviders.names[m.provider]} · ${m.label}")},onClick={choice="${m.provider}/${m.modelId}";modelMenu=false})}}};if(current!=null){var modeMenu by remember{mutableStateOf(false)};Box{TextButton(onClick={modeMenu=true},enabled=!busy){Text(conversation?.mode?:"Explicar")};DropdownMenu(modeMenu,{modeMenu=false}){listOf("Explicar","Revisão","Comparar tradições").forEach{mode->DropdownMenuItem(text={Text(mode)},onClick={vm.mode(mode);modeMenu=false})}}}}
        LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            if(conversation?.sourceExcerpt!=null)item{OutlinedCard{Column(Modifier.padding(12.dp)){Text("Fonte local [F1] · ${conversation.sourceTitle}",style=MaterialTheme.typography.titleSmall);Text(conversation.sourceLocator.orEmpty(),style=MaterialTheme.typography.bodySmall);SelectionContainer{Text(conversation.sourceExcerpt.orEmpty().take(6500))};Text("Esta passagem será enviada com o pedido. A IA não recebe automaticamente o livro completo.",style=MaterialTheme.typography.labelSmall)}}}
            if(messages.isEmpty())item{EmptyPanel("Um espaço para estudar","Faz perguntas, revê conceitos e compara ensinamentos. Configura uma chave gratuita e testa um modelo. A IA pode errar: confirma sempre as fontes.");if(available.isEmpty())Button(onClick={settings=true}){Text("Configurar fornecedor gratuito")}}
            items(messages,key={it.id}){message->Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=if(message.role=="user")MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(message.role=="user")"Tu"else "Sophia · ${StudyProviders.names[message.provider]?:message.provider.orEmpty()} · ${message.model.orEmpty()}",style=MaterialTheme.typography.labelMedium);SelectionContainer{Text(message.content,style=MaterialTheme.typography.bodyLarge)};if(message.contextReduced)Text("Foi usado contexto reduzido; o histórico completo permanece nesta conversa.",style=MaterialTheme.typography.labelSmall);if(message.diagnostic.isNotBlank())Text("Mudança de modelo: ${message.diagnostic}",style=MaterialTheme.typography.labelSmall);TextButton(onClick={clipboard.setText(AnnotatedString(message.content))}){Icon(Icons.Outlined.ContentCopy,null);Text("Copiar")}}}}
            if(status.isNotBlank())item{Text(status,style=MaterialTheme.typography.bodySmall)}
            if(busy)item{LinearProgressIndicator(Modifier.fillMaxWidth());TextButton(onClick=vm::cancel){Text("Interromper pedido")}}
        }
        if(!consent)Row(Modifier.padding(horizontal=12.dp),verticalAlignment=Alignment.Top){Checkbox(consent,{vm.consent(it)});Text("Autorizo enviar mensagens e a passagem selecionada aos fornecedores que ativei, incluindo na troca automática. Os serviços gratuitos podem tratar os dados segundo as suas políticas. Não enviar dados sensíveis.",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp))}
        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.Bottom){OutlinedTextField(draft,{if(it.length<=12000)draft=it},modifier=Modifier.weight(1f).testTag("study-prompt"),placeholder={Text("O que queres estudar?")},maxLines=5,enabled=!busy);Spacer(Modifier.width(8.dp));IconButton(onClick={val text=draft;vm.send(text,choice);draft=""},enabled=!busy&&draft.isNotBlank()&&consent&&available.isNotEmpty(),modifier=Modifier.testTag("study-send")){Icon(Icons.Outlined.Send,"Enviar pergunta")}}
    }
    if(settings)ProvidersDialog(vm,providers,models,busy,consent,{settings=false})
    if(history)LibraryDialog("Conversas de estudo",{history=false}){var query by rememberSaveable{mutableStateOf("")};OutlinedTextField(query,{query=it},label={Text("Pesquisar conversas")});if(conversations.isEmpty())Text("Ainda não há conversas guardadas.");conversations.filter{it.title.contains(query,true)}.forEach{c->Row(verticalAlignment=Alignment.CenterVertically){TextButton(onClick={vm.open(c.id);history=false},modifier=Modifier.weight(1f)){Text(c.title)};IconButton(onClick={deleting=c.id}){Icon(Icons.Outlined.DeleteOutline,"Eliminar conversa: ${c.title}")}}}}
    deleting?.let{id->AlertDialog(onDismissRequest={deleting=null},title={Text("Eliminar esta conversa?")},text={Text("As mensagens e a passagem guardada nesta conversa serão removidas deste dispositivo. As notas e os livros não serão apagados.")},confirmButton={TextButton(onClick={vm.delete(id);deleting=null}){Text("Eliminar")}},dismissButton={TextButton(onClick={deleting=null}){Text("Cancelar")}})}
    feedback?.let{AlertDialog(onDismissRequest=vm::clearFeedback,title={Text("Assistente de estudo")},text={Text(it)},confirmButton={TextButton(onClick=vm::clearFeedback){Text("Fechar")}})}
}
@Composable private fun ProvidersDialog(vm:StudyViewModel,providers:List<ProviderEntity>,models:List<ModelEntity>,busy:Boolean,consent:Boolean,onClose:()->Unit){
    var selected by rememberSaveable{mutableStateOf("gemini")};val provider=providers.firstOrNull{it.id==selected}
    // Credentials must never enter saved instance state or conversation storage.
    var key by remember(selected){mutableStateOf("")};var enabled by remember(selected,provider){mutableStateOf(provider?.enabled?:false)};var free by remember(selected,provider){mutableStateOf(provider?.freeAccountConfirmed?:false)};var remove by remember{mutableStateOf(false)};val context=LocalContext.current
    LibraryDialog("Fornecedores de IA",onClose){
        Text("As chaves ficam cifradas com Android Keystore. A Sophia não ativa faturação, não compra créditos e não contorna quotas. Gemini/Groq exigem conta gratuita sem faturação; o plano não pode ser certificado pela aplicação.")
        Row{StudyProviders.names.forEach{(id,name)->FilterChip(selected==id,{selected=id},label={Text(name)})}}
        Text(StudyProviders.terms[selected].orEmpty(),style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(key,{key=it},label={Text(if(vm.hasKey(selected))"Nova chave (vazio mantém a atual)"else "Chave API gratuita")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth().testTag("provider-key"))
        TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(StudyProviders.keyPages.getValue(selected))))}){Text("Obter chave no serviço oficial")}
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(free,{free=it});Text("Confirmo conta gratuita e faturação desativada.")}
        Row(verticalAlignment=Alignment.CenterVertically){Switch(enabled,{enabled=it});Text(" Ativar fornecedor")}
        Button(onClick={vm.saveProvider(selected,key,enabled,free);key=""},enabled=!busy,modifier=Modifier.testTag("provider-save")){Text("Guardar configuração")}
        OutlinedButton(onClick={vm.discover(selected)},enabled=!busy&&provider?.enabled==true,modifier=Modifier.testTag("provider-discover")){Text("Descobrir modelos pela API")}
        provider?.error?.takeIf{it.isNotBlank()}?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        provider?.quotaInfo?.takeIf{it.isNotBlank()}?.let{Text("Limites comunicados na última resposta:\n$it",style=MaterialTheme.typography.bodySmall)}
        Text("Sem dados do fornecedor não são inventadas quotas restantes, diárias ou mensais.",style=MaterialTheme.typography.labelSmall)
        models.filter{it.provider==selected}.forEach{m->OutlinedCard{Column(Modifier.padding(12.dp)){Text(m.label,style=MaterialTheme.typography.titleSmall);Text(m.modelId,style=MaterialTheme.typography.bodySmall);Text(if(m.validatedAt>0)"Pedido real confirmado em ${DateFormat.getDateTimeInstance().format(Date(m.validatedAt))}"else "Descoberto; ainda não testado.");if(m.cooldownUntil>System.currentTimeMillis())Text("Em espera até ${DateFormat.getTimeInstance().format(Date(m.cooldownUntil))}");TextButton(onClick={vm.test(m)},enabled=!busy){Text("Testar com pedido real")};if(m.validatedAt>0)TextButton(onClick={vm.prefer(m)},enabled=!busy){Text(if(m.preferred)"✓ Preferido no modo automático"else "Preferir no modo automático")}}}}
        if(models.none{it.provider==selected})Text("Nenhum modelo descoberto. Guarda a chave e usa Descobrir modelos.")
        TextButton(onClick={remove=true},enabled=!busy&&vm.hasKey(selected)){Text("Eliminar chave deste fornecedor")}
        if(consent)TextButton(onClick={vm.consent(false)}){Text("Revogar autorização de envio de mensagens")}
    }
    if(remove)AlertDialog(onDismissRequest={remove=false},title={Text("Eliminar chave?")},text={Text("O fornecedor deixará de participar nos pedidos. O histórico mantém-se.")},confirmButton={TextButton(onClick={vm.removeKey(selected);remove=false}){Text("Eliminar")}},dismissButton={TextButton(onClick={remove=false}){Text("Cancelar")}})
}
