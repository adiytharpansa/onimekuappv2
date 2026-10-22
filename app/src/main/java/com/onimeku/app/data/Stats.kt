package com.onimeku.app.data

import java.time.LocalDate

/** Statistik tontonan untuk layar Profil, dihitung dari data profil aktif (tanpa API). */
object Stats {
    /** [week] = detik menonton 7 hari terakhir, urut lama -> hari ini (selalu 7 elemen). */
    data class Summary(
        val monthSec: Int, val episodes: Int, val topGenre: String?, val streak: Int,
        val totalSec: Int = 0, val bestStreak: Int = 0, val week: List<Int> = List(7) { 0 }, val listCount: Int = 0,
    )

    /** Ambang menonton (detik) agar sebuah hari dihitung dalam runtun. */
    const val STREAK_MIN_SEC = 60

    /** Tambahkan [sec] detik ke hari ini; simpan maksimal ±400 hari terakhir agar state tidak membengkak. */
    fun addSeconds(days: Map<String, Int>, sec: Int, today: LocalDate = LocalDate.now()): Map<String, Int> {
        if (sec <= 0) return days
        val key = today.toString()
        val next = days + (key to ((days[key] ?: 0) + sec))
        if (next.size <= 400) return next
        return next.entries.sortedByDescending { it.key }.take(400).associate { it.key to it.value }
    }

    fun compute(p: Profile, now: LocalDate = LocalDate.now()): Summary {
        val monthPrefix = "%04d-%02d".format(now.year, now.monthValue)
        val monthSec = p.days.entries.filter { it.key.startsWith(monthPrefix) }.sumOf { it.value }

        // Runtun: hari berturut-turut dengan >= 60 dtk menonton. Hari ini belum menonton tidak memutus runtun kemarin.
        var d = now
        if ((p.days[d.toString()] ?: 0) < STREAK_MIN_SEC) d = d.minusDays(1)
        var streak = 0
        while ((p.days[d.toString()] ?: 0) >= STREAK_MIN_SEC) { streak++; d = d.minusDays(1) }

        val topGenre = p.seen
            .mapNotNull { Catalog.byId[it.substringBeforeLast(':')] }
            .flatMap { it.genres }
            .groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key

        val week = (6 downTo 0).map { p.days[now.minusDays(it.toLong()).toString()] ?: 0 }
        return Summary(monthSec, p.seen.size, topGenre, streak, p.days.values.sum(), bestStreak(p.days), week, p.myList.size)
    }

    /** Runtun terpanjang sepanjang riwayat tersimpan (±400 hari). Kunci tanggal rusak diabaikan. */
    fun bestStreak(days: Map<String, Int>): Int {
        val dates = days.entries.filter { it.value >= STREAK_MIN_SEC }
            .mapNotNull { try { LocalDate.parse(it.key) } catch (_: Exception) { null } }
            .sorted()
        var best = 0; var run = 0; var prev: LocalDate? = null
        for (x in dates) {
            run = if (prev != null && prev.plusDays(1) == x) run + 1 else 1
            if (run > best) best = run
            prev = x
        }
        return best
    }

    /** "45 min" atau "1.5 h" (satu desimal, tanpa pembulatan ke atas). Tidak bergantung locale. */
    fun fmtDuration(sec: Int, minUnit: String, hourUnit: String): String =
        if (sec < 3600) "${sec / 60} $minUnit" else "${sec / 3600}.${(sec % 3600) / 360} $hourUnit"
}
