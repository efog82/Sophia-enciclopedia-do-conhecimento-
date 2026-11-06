package com.sophia.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryMatchTest {
    private fun book(id:String,title:String,author:String)=BaseBook(id,"spiritism",title,author,1900,BaseBookRights.PROTECTED,"nota","resumo","composição")
    private fun file(id:String,workId:String,title:String)=LibraryFile(id,workId,title,"ficheiro.epub","application/epub+zip",10,"hash","PROTECTED",0)

    @Test fun authorshipMatchesFullNamesAndDistinctiveTokens(){
        val kardec=book("k1","O Livro dos Espíritos","Allan Kardec")
        val andre=book("a1","Nosso Lar","André Luiz (autor atribuído) · psicografia de Chico Xavier")
        val andreWaldo=book("a2","Evolução em Dois Mundos","André Luiz (autor atribuído) · psicografia de Chico Xavier e Waldo Vieira")
        assertTrue(kardec.authoredBy("Allan Kardec"))
        assertFalse(kardec.authoredBy("Chico Xavier"))
        assertTrue(andre.authoredBy("Chico Xavier"))
        assertFalse(andre.authoredBy("Waldo Vieira"))
        assertTrue(andreWaldo.authoredBy("Waldo Vieira"))
        assertFalse(andre.authoredBy("Léon Denis"))
        assertFalse(kardec.authoredBy("Gabriel Delanne"))
    }

    @Test fun mentionsFindSectionsAndEventsByName(){
        val kardec=HistoryFigure("Allan Kardec","1804–1869","Codificador.")
        val denis=HistoryFigure("Léon Denis","1846–1927","Filósofo.")
        val article=HistoryArticle("spiritism","Espiritismo","✧","desc",
            sections=listOf(
                HistorySection("Codificação","Kardec publica o primeiro livro e organiza a doutrina.",listOf(1)),
                HistorySection("Brasil","O movimento cresce com outros nomes.",listOf(2))),
            sources=emptyList(),related=emptyList(),
            figures=listOf(kardec,denis),
            timeline=listOf(
                HistoryEvent("1857","O Livro dos Espíritos","Publicado por Allan Kardec em Paris."),
                HistoryEvent("1930","Expansão","A divulgação continua no Brasil.")))
        assertEquals(listOf("Codificação"),article.sectionsMentioning(kardec).map{it.title})
        assertEquals(listOf("1857"),article.eventsMentioning(kardec).map{it.period})
        assertTrue(article.sectionsMentioning(denis).isEmpty())
        assertTrue(article.eventsMentioning(denis).isEmpty())
        assertTrue(kardec.isMentionedIn("as obras de kardec, no plural"))
        assertFalse(kardec.isMentionedIn("um texto sem o nome"))
    }

    @Test fun libraryMatchPrefersWorkIdAndFallsBackToTitle(){
        val spirits=book("kardec-espiritos","O Livro dos Espíritos","Allan Kardec")
        val byWork=file("f1","spirits","Cópia instalada")
        val byTitle=file("f2","outra-obra","o livro dos espiritos")
        val unrelated=file("f3","outra","A Génese")
        assertEquals("f1",spirits.matchInLibrary(listOf(byTitle,byWork))?.id)
        assertEquals("f2",spirits.matchInLibrary(listOf(unrelated,byTitle))?.id)
        assertNull(spirits.matchInLibrary(listOf(unrelated)))
        assertNull(spirits.matchInLibrary(emptyList()))
    }
}
