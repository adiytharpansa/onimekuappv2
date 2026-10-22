package com.onimeku.app

import com.onimeku.app.data.Anime
import com.onimeku.app.data.MyListView
import com.onimeku.app.data.MyListView.Filter
import com.onimeku.app.data.MyListView.Sort
import com.onimeku.app.data.Profile
import org.junit.Assert.assertEquals
import org.junit.Test

class MyListViewTest {
    private fun a(id: String, title: String, st: String, score: Double? = null, rating: String? = null) =
        Anime(id, title, emptyList(), rating, false, 12, 12, false, "", 2020, apiStatus = st, score = score)

    private val items = listOf(
        a("1", "Zeta", "RELEASING", 7.0), a("2", "alpha", "FINISHED", 9.1), a("3", "Mid", "FINISHED", null, "★ 8.2"),
    )

    @Test fun recentKeepsOrder() = assertEquals(listOf("1", "2", "3"), MyListView.apply(items, Sort.RECENT, Filter.ALL, emptySet()).map { it.id })
    @Test fun titleIgnoresCase() = assertEquals(listOf("2", "3", "1"), MyListView.apply(items, Sort.TITLE, Filter.ALL, emptySet()).map { it.id })
    @Test fun ratingUsesScoreThenRatingText() = assertEquals(listOf("2", "3", "1"), MyListView.apply(items, Sort.RATING, Filter.ALL, emptySet()).map { it.id })
    @Test fun statusFilters() {
        assertEquals(listOf("1"), MyListView.apply(items, Sort.RECENT, Filter.AIRING, emptySet()).map { it.id })
        assertEquals(listOf("2", "3"), MyListView.apply(items, Sort.RECENT, Filter.COMPLETED, emptySet()).map { it.id })
    }
    @Test fun notStartedUsesProgressAndSeen() {
        val started = MyListView.startedIds(Profile("t", prog = mapOf("1:3" to 10), seen = listOf("3:1")))
        assertEquals(listOf("2"), MyListView.apply(items, Sort.RECENT, Filter.NOT_STARTED, started).map { it.id })
    }
}
