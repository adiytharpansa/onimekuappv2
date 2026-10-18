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
// Klien API katalog Onimeku — LANGSUNG ke my-anime-app (perintah user
// 18 Okt 2026: "ganti pakai api ini saja", tanpa perantara onimekuappv2-api).
// Kontrak terverifikasi live:
//   GET /api/anime                  -> { ok, provider, data: [Anime] } (ringkasan, ~900KB)
//   GET /api/anime?id=<slug>        -> { ok, provider, data: Anime (+episodes[]) }
//   GET /api/anime?id=<slug>&ep=<n> -> { ok, provider, data: Playback }
// Hulu TIDAK punya endpoint jadwal maupun poster pengganti; fungsi yang
// bersangkutan dinonaktifkan dengan anggun di bawah.
// Tugas yang dulu dikerjakan API perantara kini dikerjakan di klien:
// stream MegaPlay (CDN nexabloom) dibungkus proksi media hulu
// (/api/proxy?u=&ref=) dengan Referer akar origin — pola yang sama yang
// membuat putar bekerja (host segmen acak tak terjangkau dari jaringan
// sebagian pengguna; Referer halaman penuh ditolak CDN).
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
    val country: String? = null,
)

@Serializable
private data class ListResp(
    val ok: Boolean = false,
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
    val label: String = "",
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
    const val BASE = "https://my-anime-app.adiyth-arpansa.workers.dev"
    private val json = Json { ignoreUnknownKeys = true }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun originOf(url: String?): String? = try {
        val u = URL(url ?: "")
        "${u.protocol}://${u.host}/"
    } catch (e: Exception) {
        null
    }

    private suspend fun getText(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
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
     * Ambil seluruh katalog dalam SATU respons (hulu tidak berpaginasi;
     * ringkasan ~900KB untuk ~900 judul). onProgress/onRaw dipanggil
     * sekali dengan hasil final demi kompatibilitas pemanggil.
     */
    suspend fun fetchCatalog(
        onProgress: (List<Anime>) -> Unit = {},
        onRaw: (List<ApiAnime>) -> Unit = {},
    ): List<Anime> {
        val text = getTextRetry("$BASE/api/anime")
        // ±900 KB JSON: parse + pemetaan di thread latar agar UI tidak tersendat.
        val (raw, mapped) = withContext(Dispatchers.Default) {
            val r = json.decodeFromString<ListResp>(text)
            val rw = r.data.filter { it.id.isNotBlank() && it.title.isNotBlank() }.distinctBy { it.id }
            rw to rw.map { it.toAnime() }
        }
        onProgress(mapped)
        onRaw(raw)
        return mapped
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

    /**
     * Hulu tidak menyediakan endpoint jadwal tayang. Kalender aplikasi
     * otomatis jatuh ke turunan hari/jam dari katalog (jalur cadangan
     * yang memang sudah ada di AppVM).
     */
    suspend fun fetchSchedule(): List<ApiScheduleItem> = emptyList()

    suspend fun fetchDetail(id: String): Anime? {
        val r = json.decodeFromString<DetailResp>(getTextRetry("$BASE/api/anime?id=${enc(id)}"))
        return r.data?.takeIf { it.id.isNotBlank() }?.toAnime()
    }

    suspend fun fetchPlayback(id: String, ep: Int): Playback? {
        val r = json.decodeFromString<PlaybackResp>(getText("$BASE/api/anime?id=${enc(id)}&ep=$ep"))
        val pb = r.data ?: return null
        // Normalisasi label subtitle hulu ({label, lang, url}) ke lang.
        val subs = pb.subtitles.map { s -> if (s.lang.isBlank() && s.label.isNotBlank()) s.copy(lang = s.label) else s }
        val raw = pb.streamUrl
        // Stream MegaPlay (CDN nexabloom): bungkus lewat proksi media
        // hulu dengan Referer akar origin (CDN menolak Referer halaman
        // penuh); simpan URL asli + halaman putar untuk rantai cadangan
        // di pemutar (langsung -> embed), persis perilaku era API v2.
        return if (raw != null && raw.contains("nexabloom", ignoreCase = true)) {
            val origin = originOf(pb.referer) ?: "https://megaplay.buzz/"
            val proxied = "$BASE/api/proxy?u=${enc(raw)}&ref=${enc(origin)}"
            pb.copy(
                streamUrl = proxied,
                streamProxyUrl = proxied,
                directUrl = raw,
                pageUrl = pb.referer?.takeIf { it.contains("/stream/") },
                referer = origin,
                subtitles = subs,
            )
        } else {
            pb.copy(subtitles = subs)
        }
    }

    /**
     * Hulu tidak menyediakan endpoint poster pengganti; pengobat poster
     * aplikasi memakai poster katalog apa adanya (cache lokal hasil
     * resolusi lama tetap berlaku di PosterHealer).
     */
    suspend fun resolvePoster(anilistId: Int): PosterResp? = null
}

@Serializable
data class ApiScheduleItem(
    val id: String = "",
    val ep: Int = 0,
    val airingAt: Long = 0,
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
        country = country,
    )
}
