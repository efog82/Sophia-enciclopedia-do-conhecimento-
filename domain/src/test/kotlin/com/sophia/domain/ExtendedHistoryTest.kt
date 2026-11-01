package com.sophia.domain

import org.junit.Assert.*
import org.junit.Test

class ExtendedHistoryTest {
    @Test fun supplementCoversAllSeventeenTopics() {
        assertEquals(17, ExtendedHistory.pilot.keys.size)
    }

    @Test fun allPilotSectionsHaveTextAndUniqueHeadings() {
        ExtendedHistory.pilot.values.forEach { sections ->
            assertTrue(sections.isNotEmpty())
            assertEquals(sections.size, sections.map { it.heading }.distinct().size)
            assertTrue(sections.all { it.heading.isNotBlank() && it.narrative.length > 100 && it.sourceNumbers.isNotEmpty() && it.sourceNumbers.all { n -> n > 0 } })
        }
    }

    @Test fun v03HistoryTypesAreUntouched() {
        val article = HistoryArticle("sample", "Título", "✧", "Texto", false,
            listOf(HistorySection("Origem", "História", listOf(1))),
            listOf(HistorySource("Fonte", "https://example.org")),
            emptyList())
        assertEquals("sample", article.id)
        assertEquals("História", article.sections.single().text)
    }
}
