package com.onimeku.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class Sort { Rating, Newest }
enum class ListStatus(val label: String) {
    Watching("Sedang menonton"),
    Plan("Rencana menonton"),
    Done("Selesai")
}

data class Anime(
    val id: Int,
    val title: String,
    val genres: List<String>,
    val rating: Double,
    val year: Int,
    val studio: String,
    val synopsis: String,
    val poster: String = "",
    val banner: String = "",
    val episodes: Int = 12,
    val ageRating: String = "PG-13",
    val hd: Boolean = true,
    val dayIndex: Int? = null,
    val airTime: String = "",
    val trailer: String = ""
)

data class ReleaseItem(val anime: Anime, val day: Int, val time: String, val episode: Int = 0)
data class HomeData(val featured: List<Anime>, val top: List<Anime>, val fresh: List<Anime>)

val Anime.ratingText: String get() = if (rating > 0) "%.1f".format(rating) else "—"
val Anime.yearText: String get() = if (year > 0) "$year" else "TBA"
val Anime.episodeText: String get() = if (episodes > 0) "$episodes Eps" else "Ongoing"

/**
 * Repository katalog anime.
 *
 * Sumber utama: Jikan API v4 (MyAnimeList) tanpa API key.
 * - `refresh()` mengambil katalog "top airing" + musim berjalan.
 * - Jika jaringan gagal, aplikasi tetap berjalan dengan [fallback] lokal.
 * - Hasil pencarian remote ikut di-cache agar halaman detail bisa dibuka.
 */
object AnimeRepository {
    private const val BASE = "https://api.jikan.moe/v4"
    private const val CACHE_TTL_MS = 10 * 60 * 1000L
    private const val USER_AGENT = "Onimeku/2.0 (Android)"

    private val baseGenres = listOf(
        "Action", "Adventure", "Award Winning", "Comedy", "Drama", "Fantasy",
        "Horror", "Mystery", "Romance", "Sci-Fi", "Slice of Life", "Sports",
        "Supernatural", "Thriller"
    )

    /** Genre dari katalog live + daftar dasar, dipakai di baris filter. */
    val genres: List<String>
        get() = (baseGenres + all.flatMap { it.genres })
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

    // ── Katalog ─────────────────────────────────────────────────────────────
    @Volatile private var cache: List<Anime> = emptyList()
    @Volatile private var scheduleCache: List<ReleaseItem> = emptyList()
    @Volatile private var fetchedAt: Long = 0L
    private val extra = java.util.concurrent.ConcurrentHashMap<Int, Anime>()
    private val mutex = Mutex()

    val all: List<Anime> get() = cache.ifEmpty { fallback }
    val schedule: List<ReleaseItem> get() = scheduleCache.ifEmpty { fallbackSchedule() }
    val isLive: Boolean get() = cache.isNotEmpty()

    fun byId(id: Int): Anime =
        all.firstOrNull { it.id == id } ?: extra[id] ?: fallback.first()

    fun byIdOrNull(id: Int): Anime? =
        all.firstOrNull { it.id == id } ?: extra[id]

    /** Ambil katalog terbaru. Aman dipanggil berkali-kali (di-cache 10 menit). */
    suspend fun refresh(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (!force && cache.isNotEmpty() && System.currentTimeMillis() - fetchedAt < CACHE_TTL_MS) {
            return@withContext true
        }
        mutex.withLock {
            if (!force && cache.isNotEmpty() && System.currentTimeMillis() - fetchedAt < CACHE_TTL_MS) {
                return@withLock true
            }
            runCatching {
                val airing = fetchJson("$BASE/top/anime?filter=airing&limit=20&sfw=true")
                delay(450)
                val season = fetchJson("$BASE/seasons/now?limit=14&sfw=true")
                val parsed = (parseAnimeList(airing) + parseAnimeList(season))
                    .distinctBy { it.id }
                require(parsed.size >= 6) { "Katalog API tidak cukup" }

                cache = parsed
                extra.putAll(parsed.associateBy { it.id })
                scheduleCache = buildSchedule(parsed)
                fetchedAt = System.currentTimeMillis()
                true
            }.getOrElse {
                // Gagal jaringan → pakai cache lama, atau fallback lokal.
                cache.isNotEmpty()
            }
        }
    }

    suspend fun home(): HomeData {
        refresh()
        val list = all
        val byRating = list.sortedByDescending { it.rating }
        return HomeData(
            featured = byRating.take(3),
            top = byRating.take(10),
            fresh = list.sortedByDescending { it.year }.take(10)
        )
    }

    suspend fun search(q: String, genre: String, sort: Sort): List<Anime> {
        val base = all
        var list = filter(base, q, genre)

        // Kalau kata kunci tidak ditemukan di katalog lokal, cari ke API.
        if (q.isNotBlank() && list.size < 4) {
            val remote = runCatching { remoteSearch(q) }.getOrDefault(emptyList())
            if (remote.isNotEmpty()) {
                extra.putAll(remote.associateBy { it.id })
                list = filter((base + remote).distinctBy { it.id }, q, genre)
            }
        }
        return when (sort) {
            Sort.Rating -> list.sortedByDescending { it.rating }
            Sort.Newest -> list.sortedByDescending { it.year }
        }
    }

    private fun filter(list: List<Anime>, q: String, genre: String): List<Anime> =
        list.filter { a ->
            val matchQuery = q.isBlank() ||
                a.title.contains(q, true) ||
                a.genres.any { it.contains(q, true) } ||
                a.studio.contains(q, true)
            val matchGenre = genre == "Semua" || a.genres.any { it.equals(genre, true) }
            matchQuery && matchGenre
        }

    private suspend fun remoteSearch(q: String): List<Anime> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(q, "UTF-8")
        val root = fetchJson("$BASE/anime?q=$encoded&limit=18&order_by=members&sort=desc&sfw=true")
        parseAnimeList(root)
    }

    // ── Jaringan ────────────────────────────────────────────────────────────
    private fun fetchJson(url: String): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            val code = conn.responseCode
            if (code !in 200..299) {
                val body = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                error("HTTP $code: ${body.take(120)}")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    // ── Parsing Jikan ───────────────────────────────────────────────────────
    private fun parseAnimeList(root: JSONObject): List<Anime> {
        val arr = root.optJSONArray("data") ?: return emptyList()
        val out = ArrayList<Anime>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            parseAnime(o)?.let(out::add)
        }
        return out
    }

    private fun parseAnime(o: JSONObject): Anime? {
        val id = o.optInt("mal_id")
        if (id <= 0) return null
        val title = o.str("title_english")
            .ifBlank { o.str("title") }
            .ifBlank { return null }

        val jpg = o.optJSONObject("images")?.optJSONObject("jpg")
        val poster = jpg?.str("large_image_url").orEmpty()
            .ifBlank { jpg?.str("image_url").orEmpty() }

        val trailerNode = o.optJSONObject("trailer")
        val trailerUrl = trailerNode?.str("url").orEmpty()
        val banner = trailerNode?.optJSONObject("images")
            ?.str("maximum_image_url").orEmpty()

        val genres = o.optJSONArray("genres").names().ifEmpty { listOf("Anime") }

        val studios = o.optJSONArray("studios").names()
        val yearFromAired = o.optJSONObject("aired")
            ?.optJSONObject("prop")
            ?.optJSONObject("from")
            ?.optInt("year", 0) ?: 0
        val year = o.optInt("year", 0).takeIf { it > 0 } ?: yearFromAired

        val score = o.optDouble("score", 0.0).takeIf { it.isFinite() } ?: 0.0
        val episodes = o.optInt("episodes", 0).coerceAtLeast(0)
        val age = o.str("rating").substringBefore(" -").ifBlank { "PG-13" }
        val synopsis = o.str("synopsis")
            .replace("[Written by MAL Rewrite]", "")
            .trim()

        val broadcast = o.optJSONObject("broadcast")
        val day = dayIndex(broadcast?.str("day").orEmpty())
        val time = toWib(broadcast?.str("time").orEmpty())

        return Anime(
            id = id,
            title = title,
            genres = genres.take(3),
            rating = score,
            year = year,
            studio = studios.firstOrNull() ?: "—",
            synopsis = synopsis,
            poster = poster,
            banner = banner,
            episodes = episodes,
            ageRating = age,
            hd = true,
            dayIndex = day,
            airTime = time,
            trailer = trailerUrl
        )
    }

    private fun JSONArray?.names(): List<String> {
        if (this == null) return emptyList()
        val out = ArrayList<String>(length())
        for (i in 0 until length()) {
            val name = optJSONObject(i)?.str("name").orEmpty()
            if (name.isNotBlank()) out.add(name)
        }
        return out
    }

    /** org.json di Android mengembalikan "null" untuk nilai JSON null; normalkan di sini. */
    private fun JSONObject.str(key: String): String {
        if (isNull(key)) return ""
        val v = optString(key)
        return if (v == "null") "" else v
    }

    private fun dayIndex(day: String): Int? = when (day.lowercase()) {
        "sunday", "sundays" -> 0
        "monday", "mondays" -> 1
        "tuesday", "tuesdays" -> 2
        "wednesday", "wednesdays" -> 3
        "thursday", "thursdays" -> 4
        "friday", "fridays" -> 5
        "saturday", "saturdays" -> 6
        else -> null
    }

    /** JST (UTC+9) → WIB (UTC+7). */
    private fun toWib(time: String): String {
        val h = time.substringBefore(":").toIntOrNull() ?: return ""
        val m = time.substringAfter(":", "00").toIntOrNull() ?: 0
        return "%02d:%02d".format((h - 2 + 24) % 24, m)
    }

    private fun buildSchedule(list: List<Anime>): List<ReleaseItem> {
        val items = list.mapNotNull { a ->
            val day = a.dayIndex ?: return@mapNotNull null
            ReleaseItem(a, day, a.airTime.ifBlank { "19:00" })
        }
        return items.distinctBy { it.anime.id }.sortedBy { it.day }
    }

    // ── Fallback offline ────────────────────────────────────────────────────
    private const val SYN =
        "Seorang pemuda berjuang melawan takdir demi melindungi orang yang ia sayangi " +
            "dalam petualangan epik penuh aksi dan emosi yang tak terlupakan."

    private val fallback: List<Anime> = run {
        val titles = listOf(
            "Demon Slayer: Kimetsu no Yaiba", "Attack on Titan", "Spy x Family",
            "Jujutsu Kaisen", "Bleach: Thousand-Year Blood War", "Kaguya-sama: Love is War",
            "Kuroko's Basketball", "My Dress-Up Darling", "JoJo's Bizarre Adventure",
            "Frieren: Beyond Journey's End", "Haikyu!!", "Gintama"
        )
        val studios = listOf(
            "Ufotable", "MAPPA", "Wit Studio", "MAPPA", "Pierrot", "A-1 Pictures",
            "Production I.G", "CloverWorks", "David Production", "Madhouse",
            "Production I.G", "Sunrise"
        )
        titles.mapIndexed { i, t ->
            Anime(
                id = 9000 + i,
                title = t,
                genres = listOf(baseGenres[i % 8], baseGenres[(i + 3) % 8]),
                rating = 8.4 + ((i * 7) % 9) / 10.0,
                year = 2020 + i % 5,
                studio = studios[i],
                synopsis = SYN,
                episodes = 24,
                dayIndex = i % 7,
                airTime = "%02d:00".format(10 + i % 10)
            )
        }
    }

    private fun fallbackSchedule(): List<ReleaseItem> =
        fallback.take(10).map { a ->
            ReleaseItem(a, a.dayIndex ?: 0, a.airTime.ifBlank { "19:00" }, 1 + a.id % 12)
        }
}
