package com.onimeku.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

data class Anime(
    val id: String,
    val title: String,
    val genres: List<String>,
    val rating: String?,
    val hd: Boolean,
    val eps: Int,
    val ep: Int,
    val label: Boolean,
    val ca: Color, val cb: Color, val cc: Color,
    val x: Float, val y: Float,
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
 */
object Catalog {
    var all by mutableStateOf<List<Anime>>(emptyList())
    var loading by mutableStateOf(true)
    var loadFailed by mutableStateOf(false)

    val byId: Map<String, Anime> get() = all.associateBy { it.id }

    /** Judul dengan skor tertinggi lebih dulu. */
    val top: List<String>
        get() = all.sortedWith(
            compareByDescending<Anime> { it.score ?: -1.0 }.thenBy { it.title },
        ).take(6).map { it.id }

    private fun relLabel(iso: String?): String {
        val d = runCatching { OffsetDateTime.parse(iso).toLocalDate() }.getOrNull() ?: return "Today"
        val days = ChronoUnit.DAYS.between(d, LocalDate.now())
        return when {
            days <= 0L -> "Today"
            days == 1L -> "Yesterday"
            else -> "$days days ago"
        }
    }

    val new: List<Triple<String, Int, String>>
        get() = all.filter { it.ep > 0 }
            .sortedByDescending { it.updatedAt ?: "" }
            .take(5)
            .map { Triple(it.id, it.ep, relLabel(it.updatedAt)) }

    val notifs: List<Triple<String, Int, String>>
        get() = all.filter { it.ep > 0 }
            .sortedByDescending { it.updatedAt ?: "" }
            .take(6)
            .map { Triple(it.id, it.ep, relLabel(it.updatedAt)) }

    val chips: List<String>
        get() = all.flatMap { it.genres }
            .groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }
            .map { it.key }.take(10)

    val years: List<String>
        get() = all.map { it.year }.filter { it > 0 }
            .distinct().sortedDescending().take(6).map { it.toString() }

    val statuses = listOf("Airing", "Completed")

    fun similar(id: String): List<Anime> {
        val g = byId[id]?.genres ?: return emptyList()
        return all.filter { it.id != id }
            .map { it to it.genres.count { x -> x in g } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(6).map { it.first }
    }

    // ---------- palet warna cadangan (dipakai bila poster tak tersedia) ----------
    private val palettes = listOf(
        Triple("#8dd4e8", "#101a2c", "#343848"),
        Triple("#c3202c", "#2a0a12", "#ff9d3d"),
        Triple("#7a5cc7", "#1c1233", "#e07ad1"),
        Triple("#58c4a8", "#0f2c2a", "#c8f0d8"),
        Triple("#f0a35e", "#4a1d12", "#ffd36e"),
        Triple("#5b7bd6", "#1a1f45", "#9fc0ff"),
        Triple("#f6c9dc", "#8c5578", "#2a1a2c"),
        Triple("#a9d3a0", "#3d4a2f", "#f4e4d0"),
    )

    private fun col(h: String) = Color(android.graphics.Color.parseColor(h))

    fun artFor(id: String): Pair<Triple<Color, Color, Color>, Pair<Float, Float>> {
        val h = (id.hashCode() and Int.MAX_VALUE)
        val p = palettes[h % palettes.size]
        val colors = Triple(col(p.first), col(p.second), col(p.third))
        val xy = (30 + h % 45) / 100f to (32 + (h / 7) % 38) / 100f
        return colors to xy
    }
}
