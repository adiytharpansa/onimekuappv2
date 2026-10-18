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
    val directUrl: String? = null,
    val pageUrl: String? = null,
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
            conn.connectTimeout = 20_000
            conn.readTimeout = 45_000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "OnimekuApp/1.0 (Android)")
            if (conn.responseCode !in 200..299) throw java.io.IOException("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /** GET dengan percobaan ulang (jaringan seluler + hulu kadang goyah). */
    private suspend fun getTextRetry(url: String, attempts: Int = 3): String {
        var last: Exception? = null
        for (i in 1..attempts) {
            try {
                return getText(url)
            } catch (e: Exception) {
                last = e
                if (i < attempts) kotlinx.coroutines.delay(900L * i)
            }
        }
        throw last ?: java.io.IOException("gagal memuat")
    }

    /**
     * Ambil seluruh katalog (per halaman 100, sesuai batas API). Hasil
     * dilaporkan PROGRESIF lewat onProgress supaya layar terisi dari
     * halaman pertama dan tidak kosong total hanya karena satu halaman
     * di tengah gagal; onRaw menerima data mentah final untuk di-cache.
     */
    suspend fun fetchCatalog(
        onProgress: (List<Anime>) -> Unit = {},
        onRaw: (List<ApiAnime>) -> Unit = {},
    ): List<Anime> {
        fun mapped(raw: List<ApiAnime>): List<Anime> =
            raw.filter { it.id.isNotBlank() && it.title.isNotBlank() }
                .distinctBy { it.id }
                .map { it.toAnime() }
        val first = json.decodeFromString<ListResp>(getTextRetry("$BASE/api/anime?limit=100&page=1"))
        val acc = ArrayList<ApiAnime>(first.data)
        onProgress(mapped(acc))
        val pages = first.totalPages.coerceIn(1, 20)
        for (p in 2..pages) {
            val r = json.decodeFromString<ListResp>(getTextRetry("$BASE/api/anime?limit=100&page=$p"))
            acc += r.data
            onProgress(mapped(acc))
        }
        val raw = acc.filter { it.id.isNotBlank() && it.title.isNotBlank() }.distinctBy { it.id }
        onRaw(raw)
        return mapped(raw)
    }

    /** Katalog terakhir yang berhasil dimuat, disimpan di disk oleh aplikasi. */
    fun loadCachedCatalog(file: java.io.File): List<Anime>? {
        return try {
            if (!file.exists()) return null
            val raw = json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(ApiAnime.serializer()),
                file.readText(),
            )
            raw.filter { it.id.isNotBlank() && it.title.isNotBlank() }
                .distinctBy { it.id }
                .map { it.toAnime() }
                .ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    fun saveCachedCatalog(file: java.io.File, raw: List<ApiAnime>) {
        try {
            file.writeText(
                json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(ApiAnime.serializer()),
                    raw,
                )
            )
        } catch (e: Exception) {
            // cache disk bersifat cadangan; gagal menulis tidak fatal
        }
    }

    /** Jadwal tayang asli dari API (episode berikutnya per judul). */
    suspend fun fetchSchedule(): List<ApiScheduleItem> {
        val r = json.decodeFromString<ScheduleResp>(getTextRetry("$BASE/api/schedule"))
        return if (r.ok) r.data.filter { it.id.isNotBlank() && it.airingAt > 0 } else emptyList()
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
data class ApiScheduleItem(
    val id: String = "",
    val ep: Int = 0,
    val airingAt: Long = 0,
)

@Serializable
private data class ScheduleResp(
    val ok: Boolean = false,
    val data: List<ApiScheduleItem> = emptyList(),
)

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
        hd = false, // API tidak menyediakan info kualitas; jangan klaim HD
        eps = if (total > 0) total else latest,
        ep = latest,
        label = false,
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
