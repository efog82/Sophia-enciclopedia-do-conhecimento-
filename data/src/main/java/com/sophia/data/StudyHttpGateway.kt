package com.sophia.data

import com.sophia.domain.*
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object StudyProviders {
    val names=linkedMapOf("gemini" to "Google Gemini","groq" to "Groq","openrouter" to "OpenRouter")
    val keyPages=mapOf("gemini" to "https://aistudio.google.com/apikey","groq" to "https://console.groq.com/keys","openrouter" to "https://openrouter.ai/keys")
    val terms=mapOf("gemini" to "Usa um projeto elegível para o escalão gratuito, sem faturação ativa. Os modelos gratuitos dependem da região/conta. A API não permite à Sophia certificar o escalão de faturação da chave.","groq" to "Usa uma conta Free, sem ativar faturação. Limites por modelo/organização; a API não permite à Sophia certificar o plano da tua conta.","openrouter" to "Apenas variantes :free com preço de entrada e saída igual a zero. Quotas partilhadas da conta: consulta o painel; não é necessário comprar créditos.")
    val geminiFree=setOf("gemini-2.5-flash","gemini-2.5-flash-lite","gemini-2.5-pro")
}
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StudyHttpGateway(private val keys:ApiKeyVault,private val client:OkHttpClient=OkHttpClient.Builder().callTimeout(100,TimeUnit.SECONDS).connectTimeout(20,TimeUnit.SECONDS).readTimeout(90,TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()):StudyGateway {
    private suspend fun request(provider:String,path:String,body:JSONObject?=null):Pair<String,String> {
        val key=keys.read(provider)?:throw StudyFailure(StudyFailureKind.AUTH,"Configure a chave API de ${StudyProviders.names[provider]}.",providerWide=true)
        val root=when(provider){"gemini"->"https://generativelanguage.googleapis.com/v1beta/";"groq"->"https://api.groq.com/openai/v1/";"openrouter"->"https://openrouter.ai/api/v1/";else->throw IllegalArgumentException("Fornecedor não suportado.")}
        val builder=Request.Builder().url(root+path).header("Accept","application/json")
        if(provider=="gemini")builder.header("x-goog-api-key",key)else builder.header("Authorization","Bearer $key")
        if(provider=="openrouter")builder.header("X-Title","Sophia - Estudo")
        if(body!=null)builder.post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        val response=suspendCancellableCoroutine<Response>{cont->
            val call=client.newCall(builder.build());cont.invokeOnCancellation{call.cancel()}
            call.enqueue(object:Callback {
                override fun onFailure(call:Call,e:IOException){if(cont.isActive)cont.resumeWithException(StudyFailure(StudyFailureKind.NETWORK,"Não foi possível contactar o serviço. Verifique a ligação à Internet."))}
                override fun onResponse(call:Call,response:Response){if(cont.isActive)cont.resume(response){response.close()}else response.close()}
            })
        }
        response.use{r->
            val raw=r.body?.source()?.let{source->val buffer=okio.Buffer();var size=0L;while(true){val n=source.read(buffer,8192);if(n<0)break;size+=n;if(size>4*1024*1024)throw StudyFailure(StudyFailureKind.UNAVAILABLE,"A resposta excedeu o limite de segurança.")};buffer.readUtf8()}.orEmpty()
            if(!r.isSuccessful){
                val wait=r.header("Retry-After")?.toLongOrNull()?.coerceIn(1,86400)?:60
                throw when(r.code){
                    401,403->StudyFailure(StudyFailureKind.AUTH,"Chave inválida, sem permissão ou serviço indisponível nesta conta/região.",providerWide=true)
                    402->StudyFailure(StudyFailureKind.QUOTA,"A conta não tem acesso gratuito disponível. Não serão comprados créditos.",86400,true)
                    429->StudyFailure(StudyFailureKind.QUOTA,if(raw.contains("per day",true)||raw.contains("per_day",true))"Quota diária esgotada." else "Limite de utilização atingido. Respeite o tempo de espera do serviço.",wait,raw.contains("per project",true)||raw.contains("per_project",true))
                    404,410->StudyFailure(StudyFailureKind.MODEL,"O modelo já não está disponível para esta conta.")
                    in 500..599->StudyFailure(StudyFailureKind.UNAVAILABLE,"O serviço está temporariamente indisponível.")
                    else->StudyFailure(StudyFailureKind.REQUEST,"O serviço recusou o pedido (HTTP ${r.code}). Reduza o contexto ou verifique o modelo.")
                }
            }
            val limits=listOf("x-ratelimit-remaining-requests","x-ratelimit-remaining-tokens","x-ratelimit-reset-requests","x-ratelimit-reset-tokens").mapNotNull{name->r.header(name)?.let{"$name: ${it.take(120)}"}}.joinToString("\n")
            return raw to limits
        }
    }
    suspend fun discover(provider:String):List<ModelEntity> {
        val (raw,_)=request(provider,if(provider=="gemini")"models?pageSize=1000" else "models")
        val root=JSONObject(raw);val rows=root.optJSONArray(if(provider=="gemini")"models" else "data")?:throw StudyFailure(StudyFailureKind.UNAVAILABLE,"O serviço não devolveu um catálogo de modelos válido.")
        return (0 until rows.length()).mapNotNull{i->
            val row=rows.getJSONObject(i)
            val id=if(provider=="gemini")row.getString("name").removePrefix("models/")else row.getString("id")
            val eligible=when(provider){
                "gemini"->id in StudyProviders.geminiFree && row.optJSONArray("supportedGenerationMethods")?.let{a->(0 until a.length()).any{a.optString(it)=="generateContent"}}==true
                "openrouter"->id.endsWith(":free")&&row.optJSONObject("pricing")?.let{p->p.optString("prompt").toBigDecimalOrNull()?.signum()==0&&p.optString("completion").toBigDecimalOrNull()?.signum()==0}==true&&row.optJSONObject("architecture")?.optString("modality")?.contains("text")==true
                "groq"->row.optBoolean("active",true)&&!listOf("whisper","tts","guard","speech","playai","audio").any{id.contains(it,true)}
                else->false
            }
            if(!eligible)null else ModelEntity(provider,id,row.optString("displayName",row.optString("name",id)),row.optInt(if(provider=="gemini")"inputTokenLimit" else if(provider=="groq")"context_window" else "context_length",4096).coerceIn(2048,2_000_000))
        }
    }
    override suspend fun complete(model:StudyModel,context:List<StudyTurn>):Pair<String,String> {
        if(model.provider=="openrouter"&&!model.id.endsWith(":free"))throw StudyFailure(StudyFailureKind.REQUEST,"A Sophia só permite modelos OpenRouter :free.")
        val body=if(model.provider=="gemini")JSONObject().apply{
            put("systemInstruction",JSONObject().put("parts",JSONArray().put(JSONObject().put("text",context.filter{it.role=="system"}.joinToString("\n"){it.content}))))
            put("contents",JSONArray().apply{context.filter{it.role!="system"}.forEach{put(JSONObject().put("role",if(it.role=="assistant")"model" else "user").put("parts",JSONArray().put(JSONObject().put("text",it.content))))}})
            put("generationConfig",JSONObject().put("maxOutputTokens",1024))
        }else JSONObject().put("model",model.id).put("messages",JSONArray().apply{context.forEach{put(JSONObject().put("role",it.role).put("content",it.content))}}).put("max_tokens",1024).put("stream",false)
        val path=if(model.provider=="gemini")"models/${model.id}:generateContent"else "chat/completions"
        val (raw,quota)=request(model.provider,path,body)
        val json=JSONObject(raw)
        val text=if(model.provider=="gemini"){
            val candidate=json.optJSONArray("candidates")?.optJSONObject(0)
            val parts=candidate?.optJSONObject("content")?.optJSONArray("parts")
            parts?.let{a->(0 until a.length()).mapNotNull{a.optJSONObject(it)?.optString("text")}.joinToString("\n")}.orEmpty()
        }else json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
        if(text.isBlank())throw StudyFailure(StudyFailureKind.SAFETY,"O serviço não produziu texto: possível filtro de segurança, limite de saída ou formato incompatível.")
        return text to quota
    }
}
