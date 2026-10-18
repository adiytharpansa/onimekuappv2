package com.onimeku.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.OffsetDateTime

// ---------------------------------------------------------------------------
// Klien API katalog Onimeku (onimekuappv2-api).
// Kontrak terverifikasi live:
//   GET /api/anime?limit=&page=     -> { ok, total, page, limit, totalPages, data: [Anime] }
//   GET /api/anime/:id              -> { ok, data: Anime }
//   GET /api/anime/:id/episode/:ep  -> { ok, data: Playback }
// ---------------------------------------------------------------------------

@Serializable
data class ApiAnime(
    val id: String = "",
    val title: String = "",
    val poster: String? = null,
    val banner: String? = null,
    val synopsis: String? = null,
    val genres: List<String> = emptyList(),
    val score: Double? = null,
    val year: Int? = null,
    val status: String? = null,
    val totalEpisodes: Int? = null,
    val latestEpisode: Int? = null,
    val episodeNumbers: List<Int> = emptyList(),
    val updatedAt: String? = null,
    val anilistId: Int? = null,
)

@Serializable
private data class ListResp(
    val ok: Boolean = false,
    val totalPages: Int = 0,
    val data: List<ApiAnime> = emptyList(),
)

@Serializable
private data class DetailResp(
    val ok: Boolean = false,
    val data: ApiAnime? = null,
)

@Serializable
data class ApiSubtitle(
    val lang: String = "",
    val url: String = "",
    val referer: String? = null,
)

@Serializable
data class Playback(
    val streamUrl: String? = null,
    val streamProxyUrl: String? = null,
    val embedUrl: String? = null,
    val referer: String? = null,
    val downloadUrl: String? = null,
    val poster: String? = null,
    val episode: Int = 0,
    val subtitles: List<ApiSubtitle> = emptyList(),
    val mediaKind: String? = null,
    val playable: String? = null,
)

@Serializable
private data class PlaybackResp(
    val ok: Boolean = false,
    val data: Playback? = null,
)

object ApiClient {
    const val BASE = "https://onimekuappv2-api.adiyth-arpansa.workers.dev"
    private val json = Json { ignoreUnknownKeys = true }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private suspend fun getText(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "OnimekuApp/1.0 (Android)")
            if (conn.responseCode !in 200..299) throw java.io.IOException("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /** Ambil seluruh katalog (per halaman 100, sesuai batas API). */
    suspend fun fetchCatalog(): List<Anime> {
        val first = json.decodeFromString<ListResp>(getText("$BASE/api/anime?limit=100&page=1"))
        val acc = ArrayList<ApiAnime>(first.data)
        val pages = first.totalPages.coerceIn(1, 20)
        for (p in 2..pages) {
            val r = json.decodeFromString<ListResp>(getText("$BASE/api/anime?limit=100&page=$p"))
            acc += r.data
        }
        return acc.filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
            .map { it.toAnime() }
    }

    suspend fun fetchDetail(id: String): Anime? {
        val r = json.decodeFromString<DetailResp>(getText("$BASE/api/anime/${enc(id)}"))
        return r.data?.takeIf { it.id.isNotBlank() }?.toAnime()
    }

    suspend fun fetchPlayback(id: String, ep: Int): Playback? {
        val r = json.decodeFromString<PlaybackResp>(getText("$BASE/api/anime/${enc(id)}/episode/$ep"))
        return r.data
    }

    /** Poster/cover pengganti (Kitsu) untuk judul berposter AniList. */
    suspend fun resolvePoster(anilistId: Int): PosterResp? {
        return try {
            val r = json.decodeFromString<PosterResp>(getText("$BASE/api/poster/$anilistId"))
            if (r.ok) r else null
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
data class PosterResp(
    val ok: Boolean = false,
    val poster: String? = null,
    val cover: String? = null,
)

private fun ApiAnime.toAnime(): Anime {
    val latest = latestEpisode ?: episodeNumbers.maxOrNull() ?: 0
    val total = totalEpisodes ?: 0
    val odt = runCatching { OffsetDateTime.parse(updatedAt) }.getOrNull()
    val (colors, xy) = Catalog.artFor(id)
    val cleanSyn = (synopsis ?: "")
        .substringBefore("(Source:").trim()
        .ifBlank { "No synopsis available yet." }
    val scoreTxt = score?.let { s ->
        val t = if (s % 1.0 == 0.0) s.toInt().toString() else s.toString()
        "★ $t"
    }
    return Anime(
        id = id,
        title = title,
        genres = genres,
        rating = scoreTxt,
        hd = true,
        eps = if (total > 0) total else latest,
        ep = latest,
        label = false,
        ca = colors.first, cb = colors.second, cc = colors.third,
        x = xy.first, y = xy.second,
        synopsis = cleanSyn,
        year = year ?: 0,
        day = odt?.dayOfWeek?.value?.rem(7) ?: 0,
        time = odt?.toLocalTime()?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "",
        poster = poster?.ifBlank { null },
        banner = banner?.ifBlank { null },
        available = episodeNumbers.distinct().sorted(),
        apiStatus = status,
        updatedAt = updatedAt,
        score = score,
        anilistId = anilistId,
    )
}
