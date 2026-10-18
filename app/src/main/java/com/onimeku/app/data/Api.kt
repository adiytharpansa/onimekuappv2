package com.onimeku.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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

    private suspend fun getText(url: String, readTimeoutSec: Long = 60): String = withContext(Dispatchers.IO) {
        // OkHttp bersama (Net): koneksi HTTP/2 ke worker dipakai ulang antar
        // permintaan, jadi playback/detail tidak mengulang handshake TLS.
        val req = okhttp3.Request.Builder().url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "OnimekuApp/1.0 (Android)")
            .build()
        val client = Net.client.newBuilder().readTimeout(readTimeoutSec, java.util.concurrent.TimeUnit.SECONDS).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code}")
            resp.body?.string() ?: throw java.io.IOException("respons kosong")
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

    // Data putar yang diambil lebih dulu (episode berikutnya). URL stream
    // bertoken kedaluwarsa, jadi hanya dipakai sekali dan dalam TTL pendek.
    private class Prefetched(val at: Long, val pb: Playback)
    private val prefetched = java.util.concurrent.ConcurrentHashMap<String, Prefetched>()
    private const val PREFETCH_TTL_MS = 120_000L

    /** Panggil saat episode hampir habis: episode berikutnya langsung siap diputar. */
    suspend fun prefetchPlayback(id: String, ep: Int) {
        val k = "$id:$ep"
        if (prefetched[k]?.let { System.currentTimeMillis() - it.at < PREFETCH_TTL_MS } == true) return
        val pb = try { fetchPlaybackNet(id, ep) } catch (e: Exception) { null } ?: return
        prefetched[k] = Prefetched(System.currentTimeMillis(), pb)
    }

    /**
     * [fresh] = true mengabaikan hasil prefetch (mis. untuk unduhan, yang
     * butuh URL segar). Satu percobaan ulang cepat menutup gangguan sesaat
     * tanpa membuat pengguna menunggu 3 kali seperti katalog.
     */
    suspend fun fetchPlayback(id: String, ep: Int, fresh: Boolean = false): Playback? {
        val k = "$id:$ep"
        val hit = prefetched.remove(k)
        if (!fresh && hit != null && System.currentTimeMillis() - hit.at < PREFETCH_TTL_MS) return hit.pb
        return try {
            fetchPlaybackNet(id, ep)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            kotlinx.coroutines.delay(400)
            fetchPlaybackNet(id, ep)
        }
    }

    private suspend fun fetchPlaybackNet(id: String, ep: Int): Playback? {
        val r = json.decodeFromString<PlaybackResp>(getText("$BASE/api/anime?id=${enc(id)}&ep=$ep", readTimeoutSec = 20))
        val pb = r.data ?: return null
        // Normalisasi label subtitle hulu ({label, lang, url}) ke lang.
        val subs = pb.subtitles.map { s -> if (s.lang.isBlank() && s.label.isNotBlank()) s.copy(lang = s.label) else s }
        val raw = pb.streamUrl
        // Stream MegaPlay (CDN nexabloom): bungkus lewat proksi media
        // hulu, MENIRU pemutar web persis: ref = referer UTUH dari API
        // (proksi yang memilah kandidat referer di sisi server); field
        // referer sendiri tetap akar origin untuk header jalur langsung.
        // Rantai cadangan di pemutar meniru web: proksi -> langsung ->
        // gagal jujur (embed BUKAN cadangan untuk judul ber-stream).
        return if (raw != null && raw.contains("nexabloom", ignoreCase = true)) {
            val origin = originOf(pb.referer) ?: "https://megaplay.buzz/"
            val proxied = "$BASE/api/proxy?u=${enc(raw)}&ref=${enc(pb.referer?.takeIf { it.isNotBlank() } ?: origin)}"
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
     * Resolusi poster pengganti LANGSUNG ke Kitsu (perbaikan debug.26):
     * poster katalog ber-host s4.anilist.co dibalas 403 oleh CDN-nya —
     * dari jaringan mana pun, termasuk lewat /api/img (502) — sehingga
     * 372 judul tampil tanpa poster. Padanan Kitsu dicari lewat API
     * pemetaan publik Kitsu (anilistId -> item Kitsu -> posterImage/
     * coverImage original di media.kitsu.app, terverifikasi 200).
     * PosterHealer meng-cache hasil ini permanen di disk, jadi tiap
     * judul hanya dicari sekali. Kegagalan = null (coba lagi sesi depan).
     */
    /** Ambil teks dari Kitsu: API-nya JSON:API ketat — menolak
     *  "Accept: application/json" dengan 406 (bug debug.26: resolusi
     *  poster selalu gagal di dalam aplikasi walau URL-nya benar).
     *  Wajib "application/vnd.api+json", jadi tidak memakai getText. */
    private suspend fun getKitsu(url: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val req = okhttp3.Request.Builder().url(url)
            .header("Accept", "application/vnd.api+json")
            .header("User-Agent", "OnimekuApp/1.0 (Android)")
            .build()
        Net.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw java.io.IOException("kitsu http ${resp.code}")
            resp.body?.string() ?: throw java.io.IOException("kitsu kosong")
        }
    }

    suspend fun resolvePoster(anilistId: Int): PosterResp? {
        return try {
            val mapJson = getKitsu(
                "https://kitsu.io/api/edge/mappings?filter%5BexternalSite%5D=anilist%2Fanime&filter%5BexternalId%5D=$anilistId",
            )
            val mapId = Regex(""""id":"(\d+)","type":"mappings"""").find(mapJson)?.groupValues?.get(1)
                ?: return null
            val itemJson = getKitsu("https://kitsu.io/api/edge/mappings/$mapId/item")
            fun original(field: String): String? =
                Regex(""""$field":\{[^}]*?"original":"([^"]+)"""").find(itemJson)?.groupValues?.get(1)
                    ?.replace("\\/", "/")
            val poster = original("posterImage")
            val cover = original("coverImage")
            if (poster == null && cover == null) null
            else PosterResp(ok = true, poster = poster, cover = cover)
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
