package com.sophia.domain

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import org.junit.Test
import org.junit.Assert.*

class StudyTest {
    private fun model(provider:String,id:String="text")=StudyModel(provider,id,id,4096,validatedAt=1)
    @Test fun quotaFallbackKeepsTheSameHistoryAndSource()=runBlocking{
        val seen=mutableListOf<List<StudyTurn>>();val calls=mutableListOf<String>()
        val gateway=object:StudyGateway{override suspend fun complete(model:StudyModel,context:List<StudyTurn>):Pair<String,String>{calls+=model.provider;seen+=context;if(model.provider=="first")throw StudyFailure(StudyFailureKind.QUOTA,"Quota diária esgotada",86400,true);return "Resposta de ensaio do teste" to ""}}
        val history=listOf(StudyTurn("user","Pergunta anterior"),StudyTurn("assistant","Resposta anterior"),StudyTurn("user","Compara esta passagem"))
        val answer=StudyRouter(gateway).answer(StudyRequest(history,StudySource("Obra","capítulo declarado","Excerto original de teste")),listOf(model("first"),model("first","other"),model("second")))
        assertEquals(listOf("first","second"),calls);assertEquals(seen[0],seen[1]);assertEquals(history,seen[1].drop(1));assertTrue(seen[1][0].content.contains("Excerto original de teste"));assertEquals("second",answer.model.provider);assertEquals(1,answer.attempts.size)
    }
    @Test fun untestedAndCoolingModelsAreNeverCalled()=runBlocking{
        val calls=mutableListOf<String>();val gateway=object:StudyGateway{override suspend fun complete(model:StudyModel,context:List<StudyTurn>):Pair<String,String>{calls+=model.id;return "Teste" to ""}}
        StudyRouter(gateway){100}.answer(StudyRequest(listOf(StudyTurn("user","Teste"))),listOf(model("a","untested").copy(validatedAt=0),model("a","cooldown").copy(cooldownUntil=101),model("a","ready")))
        assertEquals(listOf("ready"),calls)
    }
    @Test fun authFailureStopsOnlyThatProvider()=runBlocking{
        val calls=mutableListOf<String>();val gateway=object:StudyGateway{override suspend fun complete(model:StudyModel,context:List<StudyTurn>):Pair<String,String>{calls+=model.provider;if(model.provider=="a")throw StudyFailure(StudyFailureKind.AUTH,"Chave inválida");return "Teste" to ""}}
        StudyRouter(gateway).answer(StudyRequest(listOf(StudyTurn("user","Teste"))),listOf(model("a"),model("a","second"),model("b")))
        assertEquals(listOf("a","b"),calls)
    }
    @Test fun cancellationDoesNotSilentlySendToAnotherProvider()=runBlocking{
        var attempts=0;val gateway=object:StudyGateway{override suspend fun complete(model:StudyModel,context:List<StudyTurn>):Pair<String,String>{attempts++;throw CancellationException()}}
        try{StudyRouter(gateway).answer(StudyRequest(listOf(StudyTurn("user","Teste"))),listOf(model("a"),model("b")));fail()}catch(_:CancellationException){}
        assertEquals(1,attempts)
    }
    @Test fun contextReductionDoesNotMutateTheStoredConversation(){
        val history=(1..80).map{StudyTurn(if(it%2==1)"user"else "assistant","Mensagem $it "+"x".repeat(600))}+StudyTurn("user","Última pergunta")
        val before=history.toList();val prepared=StudyContext.prepare(StudyRequest(history),model("a"))
        assertTrue(prepared.reduced);assertEquals(before,history);assertEquals("Última pergunta",prepared.turns.last().content);assertEquals("user",prepared.turns[1].role)
    }
    @Test fun refusalOrInvalidRequestDoesNotTriggerQuotaChasing()=runBlocking{
        var attempts=0;val gateway=object:StudyGateway{override suspend fun complete(model:StudyModel,context:List<StudyTurn>):Pair<String,String>{attempts++;throw StudyFailure(StudyFailureKind.SAFETY,"Pedido não respondido")}}
        try{StudyRouter(gateway).answer(StudyRequest(listOf(StudyTurn("user","Teste"))),listOf(model("a"),model("b")));fail()}catch(_:StudyFailure){}
        assertEquals(1,attempts)
    }
}
