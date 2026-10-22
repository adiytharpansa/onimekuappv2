package com.onimeku.app

import com.onimeku.app.data.Anime
import com.onimeku.app.data.SearchLogic
import com.onimeku.app.data.SearchLogic.Sort
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchLogicTest {
    private fun a(id: String, title: String, genres: List<String> = listOf("Action"), year: Int = 2020, score: Double = 7.0, st: String = "FINISHED") =
        Anime(id, title, genres, null, false, 12, 12, false, "", year, apiStatus = st, score = score)

    private val idx = SearchLogic.index(listOf(
        a("1", "Attack on Titan Season 3 Part 2", score = 8.9, year = 2019),
        a("2", "BLEACH: Thousand-Year Blood War", score = 8.9, year = 2022, st = "RELEASING"),
        a("3", "Re:Zero", listOf("Drama"), 2016, 8.4),
        a("4", "Pokémon", listOf("Adventure"), 1997, 7.5),
        a("5", "Titan Quest Tales", score = 5.0, year = 2024),
    ))
    private fun ids(q: String, g: List<String> = emptyList(), y: String = "", s: String = "", sort: Sort = Sort.RELEVANCE) =
        SearchLogic.search(idx, q, g, y, s, sort).map { it.id }

    @Test fun normalizeStripsAccentsAndPunctuation() {
        assertEquals("re zero", SearchLogic.normalize("Re:Zero"))
        assertEquals("pokemon", SearchLogic.normalize("  Pokémon!! "))
    }
    @Test fun wordOrderDoesNotMatter() = assertEquals(listOf("1"), ids("titan attack"))
    @Test fun accentAndSquashedMatches() { assertEquals(listOf("4"), ids("pokemon")); assertEquals(listOf("3"), ids("rezero")) }
    @Test fun titleStartingWithQueryRanksFirst() = assertEquals(listOf("5", "1"), ids("titan"))
    @Test fun genreIsSearchable() = assertEquals("3", ids("drama").single())
    @Test fun filtersCombine() {
        assertEquals(listOf("2"), ids("", s = "Airing"))
        assertEquals(listOf("1"), ids("", y = "2019"))
        assertEquals(emptyList<String>(), ids("zzz"))
    }
    @Test fun emptyQueryDefaultsToRating() = assertEquals("5", ids("", g = listOf("Action")).last())
    @Test fun sorts() {
        assertEquals("5", ids("", sort = Sort.NEWEST).first())
        assertEquals("1", ids("", sort = Sort.TITLE).first())
    }
}
