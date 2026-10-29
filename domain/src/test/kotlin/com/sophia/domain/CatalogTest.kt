package com.sophia.domain

import org.junit.Test
import kotlin.test.*

class CatalogTest {
    private val book=Book("a","O Livro dos Espíritos","Estudo de questões filosóficas",1857,"https://example.org","Fonte","Referência",0,listOf(Contributor("k","Allan Kardec",ContributorRole.AUTHOR)),listOf(Tradition("s","Espiritismo","")),listOf(Category("p","Filosofia")),emptyList(),true)
    @Test fun accentsAndCaseDoNotPreventFindingTitles(){assertEquals(listOf(book),CatalogSearch.filter(listOf(book),"ESPIRITOS"))}
    @Test fun titleAndAuthorTermsAreCombined(){assertEquals(1,CatalogSearch.filter(listOf(book),"livro kardec").size);assertTrue(CatalogSearch.filter(listOf(book),"livro buda").isEmpty())}
    @Test fun classificationsAreSearchableMetadata(){assertEquals(1,CatalogSearch.filter(listOf(book),"filosofia espiritismo").size)}
    @Test fun filtersIntersect(){assertEquals(1,CatalogSearch.filter(listOf(book),traditionId="s",authorId="k",categoryId="p",favoritesOnly=true).size);assertTrue(CatalogSearch.filter(listOf(book),traditionId="b").isEmpty())}
    @Test fun favoritesFilterExcludesUnmarkedWorks(){assertTrue(CatalogSearch.filter(listOf(book.copy(favorite=false)),favoritesOnly=true).isEmpty())}
    @Test fun emptyQueryReturnsAll(){assertEquals(listOf(book),CatalogSearch.filter(listOf(book),"  "))}
    @Test fun noteTitleIsRequired(){assertFailsWith<IllegalArgumentException>{NoteValidation.clean("  ","texto")}}
    @Test fun noteLimitsAreEnforced(){assertFailsWith<IllegalArgumentException>{NoteValidation.clean("a".repeat(161),"")};assertFailsWith<IllegalArgumentException>{NoteValidation.clean("a","b".repeat(50001))}}
    @Test fun personalTextAndIndentationArePreserved(){assertEquals("Título" to "  linha\n\n",NoteValidation.clean(" Título ","  linha\n\n"))}
}
