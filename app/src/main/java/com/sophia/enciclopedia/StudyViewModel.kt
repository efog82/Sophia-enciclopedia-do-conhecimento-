package com.sophia.enciclopedia

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sophia.data.*
import com.sophia.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModel(private val repo:StudyRepository,private val saved:SavedStateHandle):ViewModel(){
    val conversations=repo.conversations.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val providers=repo.providers.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val models=repo.models.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val selected=saved.getStateFlow<String?>("study-conversation",null)
    val messages=selected.flatMapLatest{id->if(id==null)flowOf(emptyList())else repo.messages(id)}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    private val _busy=MutableStateFlow(false);val busy=_busy.asStateFlow()
    private val _status=MutableStateFlow("");val status=_status.asStateFlow()
    private val _feedback=MutableStateFlow<String?>(null);val feedback=_feedback.asStateFlow()
    private val _consent=MutableStateFlow(repo.keys.externalConsent);val consent=_consent.asStateFlow()
    private var work:Job?=null
    fun consent(value:Boolean){repo.keys.externalConsent=value;_consent.value=value}
    fun hasKey(id:String)=repo.keys.has(id)
    fun clearFeedback(){_feedback.value=null}
    private fun operation(block:suspend()->Unit){if(_busy.value)return;_busy.value=true;work=viewModelScope.launch{try{block()}catch(e:CancellationException){_status.value="Pedido interrompido. O histórico foi conservado.";throw e}catch(e:Exception){_feedback.value=StudyRepository.friendly(e)}finally{_busy.value=false}}}
    fun open(id:String){if(!_busy.value)saved["study-conversation"]=id}
    fun new(source:StudySource?=null)=operation{saved["study-conversation"]=repo.newConversation(source);_status.value=""}
    fun delete(id:String)=operation{repo.deleteConversation(id);if(selected.value==id)saved["study-conversation"]=null}
    fun mode(value:String){selected.value?.let{id->operation{repo.mode(id,value)}}}
    fun saveProvider(id:String,key:String,enabled:Boolean,free:Boolean)=operation{repo.saveProvider(id,key,enabled,free);_feedback.value="Configuração guardada. Descobre os modelos e testa um pedido real antes de usar o modo automático."}
    fun removeKey(id:String)=operation{repo.removeKey(id);_feedback.value="Chave eliminada e modelos retirados do modo automático."}
    fun discover(id:String)=operation{val n=repo.discover(id);_feedback.value=if(n>0)"$n modelos elegíveis encontrados. Testa um modelo para o ativar."else "O serviço não devolveu modelos de texto elegíveis para acesso gratuito nesta conta."}
    fun test(model:ModelEntity)=operation{_status.value="A enviar um pedido real de ligação…";repo.test(model);_status.value="Ligação real confirmada: ${model.label}."}
    fun prefer(model:ModelEntity)=operation{repo.prefer(model)}
    fun send(text:String,model:String?=null)=operation{
        val id=selected.value?:repo.newConversation().also{saved["study-conversation"]=it}
        val answer=repo.send(id,text,model){_status.value=it}
        _status.value=buildString{if(answer.attempts.isNotEmpty())append("Mudança automática: ${answer.attempts.joinToString(" · ")}\n");append("Resposta de ${StudyProviders.names[answer.model.provider]} · ${answer.model.id}.");if(answer.contextReduced)append(" Contexto reduzido para este modelo; o histórico completo continua guardado.")}
    }
    fun cancel(){work?.cancel()}
}
