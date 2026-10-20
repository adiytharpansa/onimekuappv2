package com.onimeku.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import java.net.URLEncoder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
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
data class ApiDoor(
    val name: String = "",
    val url: String = "",
)

@Serializable
data class ApiDownloadDoor(
    val name: String = "",
    val q1080: String = "",
    val q720: String = "",
    val q480: String = "",
    val subsEn: String? = null,
    val subsId: String? = null,
)

@Serializable
data class ApiDownload(
    val downloadable: Boolean = true,
    val doors: List<ApiDownloadDoor> = emptyList(),
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
    // Gudang (salinan sendiri): penanda internal + semua pintu putar
    // (master.m3u8 per pintu) + sumber unduhan per pintu/kualitas.
    val source: String? = null,
    val doors: List<ApiDoor> = emptyList(),
    val durationSec: Int? = null,
    val download: ApiDownload? = null,
    val mediaKind: String? = null,
    val playable: String? = null,
    // Posisi intro (detik) bila API menyediakannya; tombol Skip intro hanya muncul jika keduanya ada.
    val introStart: Int? = null,
    val introEnd: Int? = null,
    // Posisi outro (detik) bila API menyediakannya; tombol Skip outro hanya muncul jika outroStart ada.
    val outroStart: Int? = null,
    val outroEnd: Int? = null,
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
     * Ambil seluruh katalog dalam SATU respons (hulu tidak berpaginasi).
     * Memakai bentuk LITE (?lite=1, ~27% lebih kecil): sinopsis dipotong
     * 240 karakter dan episodeNumbers dibuang — cukup untuk daftar/hero.
     * Sinopsis penuh + nomor episode asli diambil per judul dari
     * fetchDetail saat Detail dibuka (lihat AppVM.loadDetail).
     * onProgress/onRaw dipanggil sekali dengan hasil final demi
     * kompatibilitas pemanggil.
     */
    suspend fun fetchCatalog(
        onProgress: (List<Anime>) -> Unit = {},
        onRaw: (List<ApiAnime>) -> Unit = {},
    ): List<Anime> {
        // Tampilan gudang-saja (perintah user 21 Okt 2026): katalog
        // yang ditampilkan HANYA judul yang sudah tersalin ke
        // gudang; metadata tetap dari API katalog kita.
        val text = getTextRetry("$BASE/api/anime?lite=1&gudang=1")
        // ±900 KB JSON: parse + pemetaan di thread latar agar UI tidak tersendat.
        val (raw, mapped) = withContext(Dispatchers.Default) {
            val r = json.decodeFromString<ListResp>(text)
            val rw = r.data.filter { it.id.isNotBlank() && it.title.isNotBlank() }.distinctBy { it.id }
            val m = rw.map { it.toAnime() }
            Catalog.warm(m) // daftar turunan dihitung di thread latar, bukan saat komposisi Home
            rw to m
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
                ?.also { Catalog.warm(it) }
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
     * Jadwal tayang ASLI 7 hari ke depan dari AniList (hulu Onimeku tidak punya
     * endpoint jadwal). Hanya episode dengan airingAt > sekarang; dicocokkan ke
     * katalog lewat anilistId. Melempar exception bila jaringan gagal agar UI
     * bisa membedakan "gagal memuat" dari "memang kosong".
     */
    suspend fun fetchSchedule(): List<ApiScheduleItem> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis() / 1000
        val out = ArrayList<ApiScheduleItem>()
        var page = 1
        while (page <= 8) {
            val query = "query(\$p:Int,\$a:Int,\$b:Int){Page(page:\$p,perPage:50){pageInfo{hasNextPage}" +
                "airingSchedules(airingAt_greater:\$a,airingAt_lesser:\$b,sort:TIME){airingAt episode mediaId}}}"
            val body = org.json.JSONObject()
                .put("query", query)
                .put("variables", org.json.JSONObject().put("p", page).put("a", now).put("b", now + 7 * 86400))
                .toString().toRequestBody("application/json".toMediaType())
            val req = okhttp3.Request.Builder().url("https://graphql.anilist.co")
                .header("Accept", "application/json").post(body).build()
            val text = Net.client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) throw java.io.IOException("AniList HTTP ${r.code}")
                r.body?.string() ?: throw java.io.IOException("respons kosong")
            }
            val pg = org.json.JSONObject(text).getJSONObject("data").getJSONObject("Page")
            val arr = pg.getJSONArray("airingSchedules")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out += ApiScheduleItem(anilistId = o.getInt("mediaId"), ep = o.getInt("episode"), airingAt = o.getLong("airingAt"))
            }
            if (!pg.getJSONObject("pageInfo").getBoolean("hasNextPage")) break
            page++
        }
        out
    }

    suspend fun fetchDetail(id: String): Anime? {
        val r = json.decodeFromString<DetailResp>(getTextRetry("$BASE/api/anime?id=${enc(id)}&gudang=1"))
        return r.data?.takeIf { it.id.isNotBlank() }?.toAnime()
    }

    /**
     * Kirim laporan penonton dari tombol lapor di pemutar ke API
     * (POST /api/gudang-report; format mengikuti rancangan berkas
     * laporan penyalin: anilistId + episode + catatan). true bila
     * terkirim; false -> pemanggil menyimpan ke antrean lokal.
     */
    suspend fun sendReport(anilistId: Int?, slug: String, ep: Int, note: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val body = org.json.JSONObject()
                    .put("anilistId", anilistId ?: org.json.JSONObject.NULL)
                    .put("slug", slug)
                    .put("episode", ep)
                    .put("catatan", note)
                    .toString().toRequestBody("application/json".toMediaType())
                val req = okhttp3.Request.Builder().url("$BASE/api/gudang-report")
                    .header("User-Agent", "OnimekuApp/1.0 (Android)")
                    .post(body).build()
                Net.client.newCall(req).execute().use { it.isSuccessful }
            } catch (e: Exception) {
                false
            }
        }

    // Data putar yang diambil lebih dulu (episode berikutnya). URL stream
    // bertoken kedaluwarsa, jadi hanya dipakai sekali dan dalam TTL pendek.
    private class Prefetched(val at: Long, val pb: Playback)
    private val prefetched = java.util.concurrent.ConcurrentHashMap<String, Prefetched>()
    // 10 menit: URL nexabloom berumur jam-jaman; jendela
    // prefetch panjang membuat ketukan Play lama sesudah Detail dibuka
    // tetap mengenai hasil pemanasan.
    private const val PREFETCH_TTL_MS = 600_000L

    /** Lihat data putar yang sudah di-prefetch TANPA mengambilnya (null bila tak ada/kedaluwarsa). */
    fun peekPrefetched(id: String, ep: Int): Playback? =
        prefetched["$id:$ep"]?.takeIf { System.currentTimeMillis() - it.at < PREFETCH_TTL_MS }?.pb

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
        // Timeout baca 12 dtk: endpoint ini normalnya menjawab
        // <5 dtk; 20 dtk x 2 putaran membuat layar memuat menggantung
        // terlalu lama di jaringan seluler saat hulu goyah.
        val r = json.decodeFromString<PlaybackResp>(getText("$BASE/api/anime?id=${enc(id)}&ep=$ep", readTimeoutSec = 12))
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
     * Resolusi poster pengganti TIDAK lagi dilakukan aplikasi
     * (debug.31, perintah user): semua data poster wajib berasal dari
     * API my-anime-app. Penyembuhan poster ber-host rusak (s4.anilist.co
     * 403) kini hidup di server (poster_map + resolusi Kitsu di
     * my-anime-app/src/lib/store.ts: healPosters), sehingga respons
     * /api/anime sudah membawa poster sembuh. Di sini tetap null agar
     * tidak ada panggilan keluar selain ke API sendiri; PosterHealer
     * menampilkan gradasi untuk poster yang memang belum sembuh.
     */
    suspend fun resolvePoster(anilistId: Int): PosterResp? = null

    /**
     * Kirim log diagnostik lokal ke server: kegagalan putar di
     * HP user tak terlihat dari server (permintaan varian/segmen tak
     * pernah tiba), jadi aplikasi melaporkan lognya sendiri ke
     * /api/app-log (KV app_log_v1) untuk dibaca agen dari server.
     * Usaha terbaik — kegagalan kirim diabaikan.
     */
    suspend fun uploadDiag(tag: String) {
        try {
            val text = CrashLog.read()
            val body = org.json.JSONObject().put("tag", tag).put("msg", text).toString()
                .toRequestBody("application/json".toMediaType())
            val req = okhttp3.Request.Builder().url("$BASE/api/app-log").post(body).build()
            withContext(Dispatchers.IO) { Net.client.newCall(req).execute().use { } }
        } catch (e: Exception) { /* abaikan */ }
    }
}

@Serializable
data class ApiScheduleItem(
    val anilistId: Int = 0,
    val ep: Int = 0,
    /** Epoch detik waktu tayang. */
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
    val odt = runCatching {
        OffsetDateTime.parse(updatedAt).atZoneSameInstant(java.time.ZoneId.systemDefault())
    }.getOrNull()
    val cleanSyn = (synopsis ?: "")
        .substringBefore("(Source:").trim()
        .ifBlank { "No synopsis available yet." }
    val scoreTxt = score?.takeIf { it > 0.0 }?.let { s ->
        val t = if (s % 1.0 == 0.0) s.toInt().toString() else s.toString()
        "★ $t"
    }
    return Anime(
        id = id,
        title = title,
        genres = genres,
        rating = scoreTxt,
        hd = false, // API tidak menyediakan info kualitas; jangan klaim HD
        eps = maxOf(total, latest, episodeNumbers.maxOrNull() ?: 0),
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
