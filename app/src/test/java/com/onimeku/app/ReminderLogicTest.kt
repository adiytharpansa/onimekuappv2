package com.onimeku.app

import com.onimeku.app.data.Anime
import com.onimeku.app.data.Profile
import com.onimeku.app.data.ReminderLogic
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderLogicTest {
    private fun anime(
        id: String, day: Int = 3, eps: Int = 12, ep: Int = 5, apiStatus: String? = "RELEASING",
    ) = Anime(
        id = id, title = id, genres = emptyList(), rating = null, hd = false,
        eps = eps, ep = ep, label = false, synopsis = "", year = 2026,
        day = day, apiStatus = apiStatus,
    )

    @Test fun unionOfRemindAndMyList() {
        val p = Profile("Me", remind = listOf("a"), myList = listOf("b", "a"))
        val cat = mapOf("a" to anime("a"), "b" to anime("b"))
        assertEquals(setOf("a", "b"), ReminderLogic.effectiveIds(p, cat))
    }

    @Test fun titleMissingFromCatalogExcluded() {
        val p = Profile("Me", remind = listOf("ghost"), myList = listOf("a"))
        assertEquals(setOf("a"), ReminderLogic.effectiveIds(p, mapOf("a" to anime("a"))))
    }

    @Test fun titleWithoutScheduleDayExcluded() {
        val p = Profile("Me", myList = listOf("a"))
        assertEquals(emptySet<String>(), ReminderLogic.effectiveIds(p, mapOf("a" to anime("a", day = 0))))
    }

    @Test fun finishedTitleExcluded() {
        val p = Profile("Me", remind = listOf("a"), myList = listOf("b"))
        val cat = mapOf(
            "a" to anime("a", apiStatus = "FINISHED"),
            "b" to anime("b", eps = 12, ep = 12, apiStatus = null),
        )
        assertEquals(emptySet<String>(), ReminderLogic.effectiveIds(p, cat))
    }

    @Test fun upcomingAndAiringTitlesKept() {
        val p = Profile("Me", remind = listOf("soon"), myList = listOf("air"))
        val cat = mapOf(
            "soon" to anime("soon", ep = 0, apiStatus = "NOT_YET_RELEASED"),
            "air" to anime("air"),
        )
        assertEquals(setOf("soon", "air"), ReminderLogic.effectiveIds(p, cat))
    }
}
