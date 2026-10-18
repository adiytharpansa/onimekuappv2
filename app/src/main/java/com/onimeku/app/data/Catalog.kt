package com.onimeku.app.data

import androidx.compose.ui.graphics.Color

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
) {
    val status: String get() = if (ep < eps) "Airing" else "Completed"
}

/** Sample catalog (only Demon Slayer is a real title, same as the HTML preview). */
object Catalog {
    private fun col(h: String) = Color(android.graphics.Color.parseColor(h))
    private val times = listOf("19:00", "20:30", "21:00", "18:30", "22:00", "20:00", "19:30")

    private fun mk(
        id: String, t: String, g: List<String>, r: String?, hd: Boolean, eps: Int, ep: Int, lab: Boolean,
        c: Triple<String, String, String>, x: Int, y: Int, syn: String, year: Int,
    ) = Anime(id, t, g, r, hd, eps, ep, lab, col(c.first), col(c.second), col(c.third), x / 100f, y / 100f, syn, year)

    val all: List<Anime> = listOf(
        mk("a1", "Ashen Crown", listOf("Action", "Drama", "Fantasy"), "PG-13", true, 24, 18, false,
            Triple("#8dd4e8", "#101a2c", "#343848"), 35, 42,
            "A disgraced knight carries the last ember of a fallen kingdom and must decide who deserves to wear the crown.", 2026),
        mk("a2", "Demon Slayer: Kimetsu no Yaiba", listOf("Action", "Shounen", "Martial Arts", "Adventure"), "TV-14", true, 26, 26, true,
            Triple("#c3202c", "#2a0a12", "#ff9d3d"), 50, 40,
            "Tanjiro Kamado becomes a demon slayer to turn his sister Nezuko back into a human and to avenge his family.", 2019),
        mk("a3", "Lantern District", listOf("Mystery", "Slice of Life"), null, false, 12, 9, false,
            Triple("#d4d6c4", "#555b49", "#364235"), 50, 60,
            "In a city lit by paper lanterns, a young courier finds that every message she delivers changes someone’s fate.", 2025),
        mk("a4", "Violet Hour", listOf("Music", "Drama"), "PG-13", true, 13, 6, false,
            Triple("#7a5cc7", "#1c1233", "#e07ad1"), 60, 38,
            "Two rival musicians are forced to share one rehearsal room and one deadline.", 2026),
        mk("a5", "Skybound Ronin", listOf("Action", "Adventure"), null, true, 25, 14, false,
            Triple("#f0a35e", "#4a1d12", "#ffd36e"), 40, 36,
            "A wandering swordsman crosses a sea of clouds in search of the sky island where his master vanished.", 2024),
        mk("a6", "Tidebreaker", listOf("Fantasy", "Drama"), "PG-13", false, 12, 4, false,
            Triple("#58c4a8", "#0f2c2a", "#c8f0d8"), 55, 45,
            "A lighthouse keeper in a coastal town slowly learns that the storms are answering to her.", 2025),
        mk("b1", "Paper Moons", listOf("Romance", "Fantasy"), null, false, 12, 12, false,
            Triple("#f6c9dc", "#8c5578", "#2a1a2c"), 62, 62,
            "A shy illustrator’s drawings begin appearing in the night sky above her town.", 2026),
        mk("b2", "Cobalt Academy", listOf("School", "Comedy"), null, false, 24, 9, false,
            Triple("#a9d3a0", "#c5b7a3", "#f4e4d0"), 30, 55,
            "First-years at an elite academy start a club that nobody else wants to join.", 2025),
        mk("b3", "Neon Shrine", listOf("Sci-Fi", "Supernatural"), null, true, 24, 24, false,
            Triple("#1a1c20", "#8d8574", "#4a5a35"), 78, 65,
            "A shrine maiden in a futuristic city balances old rituals with new technology.", 2026),
        mk("b4", "Moonlit Arcade", listOf("Fantasy", "Game"), "PG-13", false, 13, 3, false,
            Triple("#5b7bd6", "#1a1f45", "#9fc0ff"), 40, 40,
            "Every night at midnight, an abandoned arcade opens a door to another game world.", 2026),
        mk("b5", "Harvest Line", listOf("Slice of Life", "Supernatural"), null, false, 12, 7, false,
            Triple("#e07a5f", "#3d1f2b", "#ffd9a8"), 55, 45,
            "A quiet farming village gets new neighbors who may not be entirely human.", 2025),
    ).mapIndexed { i, a -> a.copy(day = i % 7, time = times[i % 7]) }

    /** Ganti dengan URL stream (HLS .m3u8 / MP4) milik Anda. Sekarang semua episode memakai video contoh. */
    const val SAMPLE_STREAM = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
    fun streamUrl(id: String, ep: Int): String = SAMPLE_STREAM

    val byId: Map<String, Anime> = all.associateBy { it.id }
    val top = listOf("a1", "a2", "a3", "a4", "a5", "a6")
    val new = listOf(Triple("b1", 12, "Today"), Triple("b2", 9, "Today"), Triple("b3", 24, "Yesterday"),
        Triple("b4", 3, "Yesterday"), Triple("b5", 7, "2 days ago"))
    val notifs = listOf(Triple("b1", 12, "2h ago"), Triple("b2", 9, "5h ago"), Triple("b3", 24, "Yesterday"),
        Triple("b4", 3, "Yesterday"), Triple("b5", 7, "2 days ago"), Triple("a1", 18, "3 days ago"))
    val chips = listOf("Action", "Fantasy", "Drama", "Comedy", "Sci-Fi", "Music")
    val years = listOf("2026", "2025", "2024")
    val statuses = listOf("Airing", "Completed")

    fun similar(id: String): List<Anime> {
        val g = byId[id]?.genres ?: return emptyList()
        return all.filter { it.id != id }
            .map { it to it.genres.count { x -> x in g } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(6).map { it.first }
    }
}
