package com.sophia.data

import android.content.Context
import androidx.room.withTransaction
import com.sophia.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class StudyRepository(context:Context,private val db:SophiaDatabase) {
    val keys=ApiKeyVault(context.applicationContext)
    private val gateway=StudyHttpGateway(keys)
    val conversations=db.study().conversations();val providers=db.study().providers();val models=db.study().models()
    private val sending=Mutex()
    fun messages(id:String)=db.study().messages(id)
    suspend fun newConversation(source:StudySource?=null):String {
        val id=UUID.randomUUID().toString();val now=System.currentTimeMillis()
        db.study().saveConversation(ConversationEntity(id,"Novo estudo",now,now,sourceTitle=source?.title,sourceLocator=source?.locator,sourceExcerpt=source?.excerpt?.take(6500)))
        return id
    }
    suspend fun saveProvider(id:String,key:String,enabled:Boolean,freeConfirmed:Boolean){
        require(id in StudyProviders.names)
        require(!enabled||freeConfirmed){"Confirme que usa uma conta gratuita sem faturação ativa."}
        val changed=key.isNotBlank()
        if(changed)keys.save(id,key)
        require(!enabled||keys.has(id)){"Introduza uma chave gratuita do fornecedor."}
        db.withTransaction{if(changed)db.study().clearModels(id);db.study().saveProvider(ProviderEntity(id,enabled,freeConfirmed))}
    }
    suspend fun removeKey(id:String){keys.save(id,"");db.withTransaction{db.study().clearModels(id);db.study().saveProvider(ProviderEntity(id))}}
    suspend fun discover(id:String):Int {
        val provider=db.study().provider(id)?:error("Guarde primeiro a configuração.")
        require(provider.enabled&&provider.freeAccountConfirmed){"Ative a conta gratuita primeiro."}
        return try{
            val found=gateway.discover(id);val old=db.study().modelsOnce().filter{it.provider==id}.associateBy{it.modelId}
            db.withTransaction{db.study().clearModels(id);db.study().saveModels(found.map{m->old[m.modelId]?.let{m.copy(validatedAt=it.validatedAt,cooldownUntil=it.cooldownUntil,preferred=it.preferred)}?:m});db.study().saveProvider(provider.copy(error=""))}
            found.size
        }catch(e:CancellationException){throw e}catch(e:Exception){db.study().saveProvider(provider.copy(error=friendly(e)));throw e}
    }
    suspend fun test(model:ModelEntity){
        val provider=db.study().provider(model.provider)?:error("Configure o fornecedor.")
        require(provider.enabled&&provider.freeAccountConfirmed)
        try{
            val (_,quota)=gateway.complete(model.domain(),listOf(StudyTurn("user","Responde apenas com: Ligação Sophia confirmada.")))
            db.study().saveModels(listOf(model.copy(validatedAt=System.currentTimeMillis(),cooldownUntil=0)))
            db.study().saveProvider(provider.copy(error="",quotaInfo=quota))
        }catch(e:CancellationException){throw e}catch(e:Exception){db.study().saveModels(listOf(model.copy(validatedAt=0)));db.study().saveProvider(provider.copy(error=friendly(e)));throw e}
    }
    suspend fun prefer(model:ModelEntity){db.study().saveModels(db.study().modelsOnce().map{it.copy(preferred=it.provider==model.provider&&it.modelId==model.modelId)})}
    suspend fun send(conversationId:String,text:String,selected:String?,status:(String)->Unit):StudyAnswer=sending.withLock{
        require(keys.externalConsent){"Autorize o envio das mensagens e da passagem aos fornecedores configurados antes de continuar."}
        val body=text.trim();require(body.isNotBlank()&&body.length<=12000){"Escreva um pedido até 12 000 caracteres."}
        val convo=db.study().conversation(conversationId)?:error("A conversa já não existe.")
        val providerStates=providers.first().associateBy{it.id}
        val candidates=db.study().modelsOnce().filter{m->providerStates[m.provider]?.let{it.enabled&&it.freeAccountConfirmed}==true&&keys.has(m.provider)}.filter{selected==null||"${it.provider}/${it.modelId}"==selected}.map{it.domain()}
        require(candidates.any{it.validatedAt>0&&it.cooldownUntil<=System.currentTimeMillis()}){"Não há modelos testados disponíveis. Configure e teste uma ligação gratuita."}
        val now=System.currentTimeMillis()
        db.withTransaction{db.study().insertMessage(MessageEntity(UUID.randomUUID().toString(),convo.id,"user",body,now));db.study().saveConversation(convo.copy(title=if(convo.title=="Novo estudo")body.take(70) else convo.title,updatedAt=now))}
        val turns=db.study().messagesOnce(convo.id).map{StudyTurn(it.role,it.content)}
        val source=convo.sourceExcerpt?.let{StudySource(convo.sourceTitle.orEmpty(),convo.sourceLocator.orEmpty(),it)}
        val answer=StudyRouter(gateway).answer(StudyRequest(turns,source,convo.mode),candidates,onFailure={m,e->
            val old=db.study().modelsOnce().filter{it.provider==m.provider&&(e.providerWide||it.modelId==m.id)}
            db.study().saveModels(old.map{it.copy(validatedAt=if(e.kind in setOf(StudyFailureKind.AUTH,StudyFailureKind.MODEL))0 else it.validatedAt,cooldownUntil=System.currentTimeMillis()+e.retryAfterSeconds*1000)})
            db.study().provider(m.provider)?.let{db.study().saveProvider(it.copy(error=e.explanation))}
        },onStatus=status)
        db.withTransaction{db.study().insertMessage(MessageEntity(UUID.randomUUID().toString(),convo.id,"assistant",answer.text,System.currentTimeMillis(),answer.model.provider,answer.model.id,answer.contextReduced,answer.attempts.joinToString("\n")));db.study().saveConversation(convo.copy(title=if(convo.title=="Novo estudo")body.take(70) else convo.title,updatedAt=System.currentTimeMillis()))}
        db.study().provider(answer.model.provider)?.let{db.study().saveProvider(it.copy(error="",quotaInfo=answer.quotaInfo))}
        answer
    }
    suspend fun deleteConversation(id:String)=db.study().deleteConversation(id)
    suspend fun mode(id:String,value:String){require(value in listOf("Explicar","Revisão","Comparar tradições"));db.study().conversation(id)?.let{db.study().saveConversation(it.copy(mode=value))}}
    private fun ModelEntity.domain()=StudyModel(provider,modelId,label,contextTokens,validatedAt,cooldownUntil)
    companion object{fun friendly(e:Exception)=if(e is StudyFailure)e.explanation else e.message?.takeIf{it.length<=200}?:"Não foi possível concluir a operação."}
}
