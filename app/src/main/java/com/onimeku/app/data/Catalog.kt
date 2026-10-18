package com.onimeku.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@androidx.compose.runtime.Immutable
data class Anime(
    val id: String,
    val title: String,
    val genres: List<String>,
    val rating: String?,
    val hd: Boolean,
    val eps: Int,
    val ep: Int,
    val label: Boolean,
    val synopsis: String,
    val year: Int,
    val day: Int = 0,
    val time: String = "",
    val poster: String? = null,
    val banner: String? = null,
    val available: List<Int> = emptyList(),
    val apiStatus: String? = null,
    val updatedAt: String? = null,
    val score: Double? = null,
    val anilistId: Int? = null,
    val country: String? = null,
) {
    val status: String
        get() = when (apiStatus) {
            "FINISHED" -> "Completed"
            "RELEASING", "NOT_YET_RELEASED" -> "Airing"
            else -> if (ep < eps) "Airing" else "Completed"
        }
}

/**
 * Katalog aplikasi. Isinya diisi dari API Onimeku (lihat ApiClient) saat
 * aplikasi dibuka; sebelum data tiba, daftar kosong dan layar menampilkan
 * status memuat. Tidak ada data contoh ter-hardcode lagi.
 *
 * Semua daftar turunan (top, populer, per genre, dst.) dihitung SEKALI per
 * perubahan [all] lalu di-cache. Sebelumnya setiap akses (mis. byId[...] di
 * dalam loop Home) mengurutkan/menyusun ulang seluruh katalog sehingga
 * layar terasa berat saat katalog berisi ribuan judul.
 */
object Catalog {
    var all by mutableStateOf<List<Anime>>(emptyList())
    var loading by mutableStateOf(true)
    var loadFailed by mutableStateOf(false)

    private val byScore = compareByDescending<Anime> { it.score ?: -1.0 }.thenBy { it.title.lowercase() }

    private fun relLabel(iso: String?): String {
        val d = runCatching { OffsetDateTime.parse(iso).toLocalDate() }.getOrNull() ?: return "Today"
        val days = ChronoUnit.DAYS.between(d, LocalDate.now())
        return when {
            days <= 0L -> "Today"
            days == 1L -> "Yesterday"
            else -> "$days days ago"
        }
    }

    private class Derived(val src: List<Anime>) {
        val byId: Map<String, Anime> = src.associateBy { it.id }
        val popular: List<String> = src.sortedWith(byScore).map { it.id }
        val top: List<String> = popular.take(10)
        val airing: List<String> = src.filter { it.apiStatus == "RELEASING" }.sortedWith(byScore).map { it.id }
        /** Paling baru diperbarui dulu (realtime dari API) — untuk hero Beranda. */
        val freshest: List<String> = src.sortedByDescending { it.updatedAt ?: "" }.map { it.id }
        /** Sedang tayang, paling baru diperbarui dulu — untuk halaman Kalender. */
        val airingFresh: List<String> = src.filter { it.apiStatus == "RELEASING" }
            .sortedByDescending { it.updatedAt ?: "" }.map { it.id }
        val latest: List<String> = src.sortedWith(
            compareByDescending<Anime> { it.year }.thenByDescending { it.updatedAt ?: "" },
        ).map { it.id }
        val donghua: List<String> = src.filter { it.country == "CN" }.sortedWith(byScore).map { it.id }
        val alphabetical: List<String> = src.sortedBy { it.title.lowercase() }.map { it.id }
        private val genreCount: List<Map.Entry<String, Int>> =
            src.flatMap { it.genres }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
        val topGenres: List<String> = genreCount.take(4).map { it.key }
        val chips: List<String> = genreCount.take(10).map { it.key }
        val years: List<String> = src.map { it.year }.filter { it > 0 }.distinct().sortedDescending().take(6).map { it.toString() }
        val byGenre: Map<String, List<String>> = genreCount.associate { (g, _) ->
            g to src.filter { g in it.genres }.sortedWith(byScore).map { it.id }
        }
        val fresh: List<Triple<String, Int, String>> = src.filter { it.ep > 0 }
            .sortedByDescending { it.updatedAt ?: "" }
            .take(8)
            .map { Triple(it.id, it.ep, relLabel(it.updatedAt)) }
    }

    private var cache: Derived? = null
    private val d: Derived
        get() {
            val cur = all // membaca state: Compose ikut memantau perubahan katalog
            val c = cache
            if (c != null && c.src === cur) return c
            return Derived(cur).also { cache = it }
        }

    val byId: Map<String, Anime> get() = d.byId

    /** Sepuluh judul skor tertinggi (baris "Top Hits Anime" + hero). */
    val top: List<String> get() = d.top

    /** Seluruh katalog menurut skor tertinggi. */
    val popular: List<String> get() = d.popular

    /** Sedang tayang (status RELEASING dari API), skor tertinggi dulu. */
    val airing: List<String> get() = d.airing

    /** Seluruh katalog, paling baru diperbarui dulu (updatedAt API). */
    val freshest: List<String> get() = d.freshest

    /** Sedang tayang, paling baru diperbarui dulu (updatedAt API). */
    val airingFresh: List<String> get() = d.airingFresh

    /** Rilisan terbaru: tahun paling baru, lalu paling baru diperbarui. */
    val latest: List<String> get() = d.latest

    /** Donghua (animasi Tiongkok), barisnya sendiri di Home. */
    val donghua: List<String> get() = d.donghua

    /** Seluruh katalog urut abjad (halaman "All Anime"). */
    val alphabetical: List<String> get() = d.alphabetical

    /** Genre dengan jumlah judul terbanyak, untuk baris per genre di Home. */
    val topGenres: List<String> get() = d.topGenres

    /** Semua judul dalam satu genre, skor tertinggi dulu. */
    fun byGenre(g: String): List<String> = d.byGenre[g].orEmpty()

    /** Episode terbaru (5 teratas untuk baris Home). */
    val new: List<Triple<String, Int, String>> get() = d.fresh.take(5)

    val notifs: List<Triple<String, Int, String>> get() = d.fresh.take(6)

    val chips: List<String> get() = d.chips

    val years: List<String> get() = d.years

    val statuses = listOf("Airing", "Completed")

    fun similar(id: String): List<Anime> {
        val g = byId[id]?.genres ?: return emptyList()
        return all.filter { it.id != id }
            .map { it to it.genres.count { x -> x in g } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(8).map { it.first }
    }
}
