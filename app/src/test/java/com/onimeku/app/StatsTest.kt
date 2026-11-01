package com.onimeku.app

import com.onimeku.app.data.Profile
import com.onimeku.app.data.Stats
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsTest {
    private fun day(offset: Long) = LocalDate.now().minusDays(offset).toString()

    @Test fun addSecondsAccumulatesToday() {
        val a = Stats.addSeconds(emptyMap(), 30)
        val b = Stats.addSeconds(a, 20)
        assertEquals(50, b[LocalDate.now().toString()])
        assertEquals(emptyMap<String, Int>(), Stats.addSeconds(emptyMap(), 0))
    }

    @Test fun streakCountsConsecutiveDays() {
        val p = Profile("t", days = mapOf(day(0) to 600, day(1) to 300, day(2) to 120, day(4) to 900))
        assertEquals(3, Stats.compute(p).streak)
    }

    @Test fun streakSurvivesTodayNotWatchedYet() {
        val p = Profile("t", days = mapOf(day(1) to 300, day(2) to 300))
        assertEquals(2, Stats.compute(p).streak)
    }

    @Test fun shortWatchDoesNotCountForStreak() {
        val p = Profile("t", days = mapOf(day(0) to 30))
        assertEquals(0, Stats.compute(p).streak)
    }

    @Test fun bestStreakFindsLongestRunEvenIfOld() {
        val days = mapOf(day(10) to 100, day(9) to 100, day(8) to 100, day(7) to 100, day(1) to 100, day(0) to 100, "rusak" to 999)
        assertEquals(4, Stats.bestStreak(days))
    }

    @Test fun weekHasSevenEntriesOldestFirst() {
        val p = Profile("t", days = mapOf(day(0) to 50, day(6) to 70, day(7) to 999), myList = listOf("a", "b"))
        val s = Stats.compute(p)
        assertEquals(listOf(70, 0, 0, 0, 0, 0, 50), s.week)
        assertEquals(50 + 70 + 999, s.totalSec)
        assertEquals(2, s.listCount)
    }

    @Test fun durationFormat() {
        assertEquals("0 min", Stats.fmtDuration(0, "min", "h"))
        assertEquals("59 min", Stats.fmtDuration(3599, "min", "h"))
        assertEquals("1.5 h", Stats.fmtDuration(5400, "min", "h"))
    }

    @Test fun bestStreakNeedsTheMinimumSeconds() {
        assertEquals(0, Stats.bestStreak(emptyMap()))
        assertEquals(1, Stats.bestStreak(mapOf(day(0) to 100)))
        assertEquals(0, Stats.bestStreak(mapOf(day(0) to 30)))      // di bawah ambang 60 dtk
    }

    @Test fun monthTotalCountsOnlyTheCurrentMonth() {
        val thisMonth = LocalDate.now().withDayOfMonth(1).toString()
        val p = Profile("t", days = mapOf(thisMonth to 120, "2020-01-01" to 999), seen = listOf("a:1", "a:2"))
        val s = Stats.compute(p)
        assertEquals(120, s.monthSec)
        assertEquals(2, s.episodes)                                  // jumlah entri seen
    }
}
