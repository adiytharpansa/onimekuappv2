package com.onimeku.app

import com.onimeku.app.data.Anime
import com.onimeku.app.data.Catalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    private fun a(id: String, title: String, poster: String? = null, rating: String? = null, ep: Int = 1, genres: List<String> = emptyList()) =
        Anime(
            id = id, title = title, genres = genres, rating = rating, hd = false, eps = ep, ep = ep, label = false,
            synopsis = "", year = 2025, poster = poster,
        )

    @Test fun dedupeKeepsOneEntryPerPosterAndPrefersRated() {
        val list = listOf(
            a("c1", "#Compass2.0 Animation Project", poster = "p/compass.jpg"),
            a("c2", "#Compass2.0: Animation Project", poster = "p/compass.jpg", rating = "6.2"),
            a("z1", "#Zombie Sagashitemasu", poster = "p/zombie.jpg"),
            a("z2", "#I'm Looking For a Zombie", poster = "p/zombie.jpg"),
            a("x", "100 METERS", poster = "p/100m.jpg", rating = "8.3"),
        )
        val out = Catalog.dedupe(list).map { it.id }
        assertEquals(3, out.size)
        assertTrue("c2" in out && "c1" !in out)      // yang punya rating menang
        assertTrue("x" in out)
    }

    @Test fun dedupeMergesSameImageFromDifferentFolders() {
        val list = listOf(
            a("1", "#Zombie Sagashitemasu", poster = "https://img.example/cover/medium/bx150001-abcdef123.jpg"),
            a("2", "#I'm Looking For a Zombie", poster = "https://img.example/cover/large/bx150001-abcdef123.jpg?v=2"),
        )
        assertEquals(1, Catalog.dedupe(list).size)
    }

    @Test fun dedupeMergesSameTitleWithDifferentPosterUrls() {
        val list = listOf(
            a("1", "#Compass2.0 Animation Project", poster = "https://a.example/one.jpg"),
            a("2", "#Compass2.0: Animation Project", poster = "https://b.example/two.jpg", rating = "6.2"),
        )
        val out = Catalog.dedupe(list)
        assertEquals(1, out.size)
        assertEquals("2", out[0].id)
    }

    @Test fun dedupeKeepsRemakesWithDifferentYearInTitle() {
        val list = listOf(
            a("1", "Fruits Basket (2001)", poster = "https://a.example/fb1.jpg"),
            a("2", "Fruits Basket (2019)", poster = "https://a.example/fb2.jpg"),
        )
        assertEquals(2, Catalog.dedupe(list).size)
    }

    @Test fun dedupeFallsBackToNormalizedTitleWithoutPoster() {
        val list = listOf(a("1", "Lord of Mysteries"), a("2", "Lord  of  Mysteries!"))
        assertEquals(1, Catalog.dedupe(list).size)
    }

    @Test fun franchiseKeyIgnoresSeasonAndPart() {
        val base = Catalog.franchiseKey("Attack on Titan")
        assertEquals(base, Catalog.franchiseKey("Attack on Titan Season 3 Part 2"))
        assertEquals(base, Catalog.franchiseKey("Attack on Titan: The Final Season"))
        assertTrue(base != Catalog.franchiseKey("Angel Beats!"))
    }

    @Test fun similarExcludesSameFranchise() {
        val g = listOf("Action", "Drama")
        Catalog.all = listOf(
            a("aot", "Attack on Titan", poster = "p/1", genres = g),
            a("aot3", "Attack on Titan Season 3 Part 2", poster = "p/2", genres = g),
            a("other", "Vinland Saga", poster = "p/3", genres = g),
            a("weak", "Angel Beats!", poster = "p/4", genres = listOf("Drama")),
        )
        val ids = Catalog.similar("aot").map { it.id }
        assertEquals(listOf("other"), ids)           // season lain & overlap genre lemah disaring
    }
}
