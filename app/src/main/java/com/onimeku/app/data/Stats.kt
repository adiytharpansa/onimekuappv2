package com.onimeku.app.data

import java.time.LocalDate

/** Statistik tontonan untuk layar Profil, dihitung dari data profil aktif (tanpa API). */
object Stats {
    data class Summary(val monthSec: Int, val episodes: Int, val topGenre: String?, val streak: Int)

    /** Tambahkan [sec] detik ke hari ini; simpan maksimal ±400 hari terakhir agar state tidak membengkak. */
    fun addSeconds(days: Map<String, Int>, sec: Int): Map<String, Int> {
        if (sec <= 0) return days
        val today = LocalDate.now().toString()
        val next = days + (today to ((days[today] ?: 0) + sec))
        if (next.size <= 400) return next
        return next.entries.sortedByDescending { it.key }.take(400).associate { it.key to it.value }
    }

    fun compute(p: Profile): Summary {
        val now = LocalDate.now()
        val monthPrefix = "%04d-%02d".format(now.year, now.monthValue)
        val monthSec = p.days.entries.filter { it.key.startsWith(monthPrefix) }.sumOf { it.value }

        // Runtun: hari berturut-turut dengan >= 60 dtk menonton. Hari ini belum menonton tidak memutus runtun kemarin.
        var d = now
        if ((p.days[d.toString()] ?: 0) < 60) d = d.minusDays(1)
        var streak = 0
        while ((p.days[d.toString()] ?: 0) >= 60) { streak++; d = d.minusDays(1) }

        val topGenre = p.seen
            .mapNotNull { Catalog.byId[it.substringBeforeLast(':')] }
            .flatMap { it.genres }
            .groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key

        return Summary(monthSec, p.seen.size, topGenre, streak)
    }
}
