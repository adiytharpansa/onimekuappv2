package com.onimeku.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.OffsetDateTime

// ---------------------------------------------------------------------------
// Lapisan data Onimeku TANPA API katalog (perintah user 6 Nov 2026:
// "api buang, pakai cara simple-kuat").
//
// Katalog = katalog gudang salinan sendiri (bentuk project-X yang sama
// dengan Anilover), dibaca lewat rantai:
//   1. Salinan TERENKRIPSI AES-256-GCM khusus Onimeku di Cloudflare Pages
//      (jalur tersamar khusus Onimeku; kuncinya dipecah dua — potongan A
//      di aplikasi, potongan B di loker Firebase; lihat KeyLocker).
//   2. Cermin-cermin katalog polos (jsDelivr -> gcore -> raw -> fastly)
//      bila pintu utama/kunci tidak terjangkau — aplikasi tetap jalan.
// Detail dibaca dari data katalog yang sama (tanpa panggilan terpisah).
// Putar = playlist HLS gudang per episode lewat daftar pintu CDN
// (doorVariants) dan mekanisme Gudang/DoorRace yang sudah ada;
// subtitle adalah trek bawaan manifest HLS. Jadwal tetap AniList
// langsung. TIDAK ada lagi panggilan ke API katalog lama mana pun.
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
    val source: String? = null,
    val doors: List<ApiDoor> = emptyList(),
    val durationSec: Int? = null,
    val download: ApiDownload? = null,
    val mediaKind: String? = null,
    val playable: String? = null,
    val introStart: Int? = null,
    val introEnd: Int? = null,
    val outroStart: Int? = null,
    val outroEnd: Int? = null,
) {
    val hasStream: Boolean
        get() = !streamUrl.isNullOrBlank() || !streamProxyUrl.isNullOrBlank() ||
            !embedUrl.isNullOrBlank() || doors.any { it.url.isNotBlank() }
}

/** Satu judul gudang hasil parse katalog (internal lapisan data). */
private class WhTitle(
    val api: ApiAnime,
    /** nomor episode -> URL playlist master absolut. */
    val playlists: Map<Int, String>,
)

object ApiClient {
    private val json = Json { ignoreUnknownKeys = true }

    // Pintu utama: katalog terenkripsi khusus Onimeku di Pages (jalur
    // tersamar khusus Onimeku, token diisi saat build dari berkas rahasia).
    private const val CATALOG_PAGES =
        "https://anilover-catalog.pages.dev/265f5e92892cbb3dc1b3a8fc/lib.dat"

    // Cermin katalog polos (repo katalog gudang yang sama).
    private const val MIRROR_JSDELIVR =
        "https://cdn.jsdelivr.net/gh/adiytharpansa/xjatxz2upew4@main/catalog.json"
    private const val MIRROR_GCORE =
        "https://gcore.jsdelivr.net/gh/adiytharpansa/xjatxz2upew4@main/catalog.json"
    private const val MIRROR_RAW =
        "https://raw.githubusercontent.com/adiytharpansa/xjatxz2upew4/main/catalog.json"
    private const val MIRROR_FASTLY =
        "https://fastly.jsdelivr.net/gh/adiytharpansa/xjatxz2upew4@main/catalog.json"
    private const val PROJECT_REPO = "adiytharpansa/xjatxz2upew4"

    /** Rincian kegagalan terakhir tiap sumber katalog (untuk diagnosis). */
    @Volatile
    var lastCatalogError: String? = null

    /** Peta gudang sesi ini: id judul -> data putar. Diisi fetchCatalog/ensureWarehouse. */
    @Volatile
    private var warehouse: Map<String, WhTitle> = emptyMap()

    // ---------------- pengambilan jaringan dasar ----------------

    private fun httpGetText(url: String, timeoutSec: Long = 30): String {
        val client = Net.client.newBuilder()
            .connectTimeout(timeoutSec, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(timeoutSec, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val req = okhttp3.Request.Builder().url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "OnimekuApp/1.0 (Android)")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code}")
            return resp.body?.string() ?: throw java.io.IOException("respons kosong")
        }
    }

    private fun httpGetBytes(url: String, timeoutSec: Long = 30): ByteArray {
        val client = Net.client.newBuilder()
            .connectTimeout(timeoutSec, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(timeoutSec, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val req = okhttp3.Request.Builder().url(url)
            .header("User-Agent", "OnimekuApp/1.0 (Android)")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code}")
            return resp.body?.bytes() ?: throw java.io.IOException("respons kosong")
        }
    }

    // ---------------- dekripsi katalog (AES-256-GCM, nonce||ct||tag) ----------------

    private fun decryptCatalog(data: ByteArray, key: ByteArray): ByteArray {
        require(data.size > 28) { "berkas terenkripsi terlalu pendek" }
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            javax.crypto.Cipher.DECRYPT_MODE,
            javax.crypto.spec.SecretKeySpec(key, "AES"),
            javax.crypto.spec.GCMParameterSpec(128, data.copyOfRange(0, 12)),
        )
        return cipher.doFinal(data.copyOfRange(12, data.size))
    }

    private fun decodeBody(bytes: ByteArray, key: ByteArray?): String {
        if (bytes.isNotEmpty() && (bytes[0] == '{'.code.toByte() || bytes[0] == '['.code.toByte())) {
            return String(bytes, Charsets.UTF_8)
        }
        if (key == null) throw java.io.IOException("kunci katalog tidak tersedia")
        return String(decryptCatalog(bytes, key), Charsets.UTF_8)
    }

    /**
     * Ambil badan katalog mentah: pintu Pages terenkripsi dulu (kunci dari
     * loker), lalu cermin polos. Dua putaran; kegagalan dicatat di
     * [lastCatalogError]. Jatuh ke cermin polos BUKAN kegagalan aplikasi.
     */
    private suspend fun fetchRawCatalog(): String = withContext(Dispatchers.IO) {
        val key = try { KeyLocker.fullKey() } catch (e: Exception) { null }
        val sources: List<Pair<String, suspend () -> String>> = listOf(
            "Pages" to { decodeBody(httpGetBytes(CATALOG_PAGES), key) },
            "jsDelivr" to { httpGetText(MIRROR_JSDELIVR) },
            "gcore" to { httpGetText(MIRROR_GCORE) },
            "raw" to { httpGetText(MIRROR_RAW) },
            "fastly" to { httpGetText(MIRROR_FASTLY) },
        )
        val errs = ArrayList<String>()
        var last: Exception? = null
        for (round in 1..2) {
            for ((name, fetch) in sources) {
                try {
                    val body = fetch()
                    if (!body.trimStart().startsWith("{")) {
                        throw java.io.IOException("isi katalog tidak dikenal dari $name")
                    }
                    lastCatalogError = null
                    return@withContext body
                } catch (e: Exception) {
                    last = e
                    if (round == 2) errs.add("$name: ${e.javaClass.simpleName}: ${e.message?.take(90) ?: "?"}")
                }
            }
        }
        lastCatalogError = errs.joinToString(" · ")
        throw last ?: java.io.IOException("semua sumber katalog gagal")
    }

    // ---------------- parse katalog gudang ----------------

    private fun absolut(base: String?, path: String): String =
        if (path.startsWith("http") || base == null) path else "$base/$path"

    private fun parseWarehouse(body: String): List<WhTitle> {
        val root = org.json.JSONObject(body)
        val rootPin = root.optString("pin")
        val arr = root.optJSONArray("titles") ?: org.json.JSONArray()
        val out = ArrayList<WhTitle>()
        for (i in 0 until arr.length()) {
            try {
                val t = arr.getJSONObject(i)
                val id = t.optString("id")
                val title = t.optString("title")
                if (id.isBlank() || title.isBlank()) continue
                var tRepo = t.optString("repo")
                var tPin = t.optString("pin")
                if (tRepo.isBlank() || tPin.isBlank()) {
                    // Judul lama tanpa repo/pin: tebak dari URL poster
                    // (pola .../gh/<owner>/<repo>@<pin>/...).
                    val m = Regex("""/gh/([^/]+/[^/@]+)@([0-9a-f]{40})/""")
                        .find(t.optString("poster"))
                    if (m != null) {
                        tRepo = m.groupValues[1]
                        tPin = m.groupValues[2]
                    }
                }
                val base = when {
                    tRepo.isNotBlank() && tPin.isNotBlank() ->
                        "https://cdn.jsdelivr.net/gh/$tRepo@$tPin"
                    rootPin.isNotBlank() ->
                        "https://cdn.jsdelivr.net/gh/$PROJECT_REPO@$rootPin"
                    else -> null
                }
                val playlists = LinkedHashMap<Int, String>()
                val ea = t.optJSONArray("episodes") ?: org.json.JSONArray()
                for (j in 0 until ea.length()) {
                    when (val item = ea.opt(j)) {
                        is org.json.JSONObject -> {
                            val n = item.optInt("number")
                            val p = item.optString("playlist")
                            if (n > 0 && p.isNotBlank()) playlists[n] = absolut(base, p)
                        }
                        is Number -> {
                            val n = item.toInt()
                            if (n > 0) playlists[n] = absolut(base, "$id/ep$n/playlist.m3u8")
                        }
                    }
                }
                if (playlists.isEmpty()) continue
                val genres = ArrayList<String>()
                val ga = t.optJSONArray("genres") ?: org.json.JSONArray()
                for (j in 0 until ga.length()) genres.add(ga.optString(j))
                val nums = playlists.keys.sorted()
                val api = ApiAnime(
                    id = id,
                    title = title,
                    poster = t.optString("poster").ifBlank { null },
                    banner = t.optString("banner").ifBlank { null },
                    synopsis = t.optString("synopsis").ifBlank { null },
                    genres = genres,
                    score = if (t.has("score") && !t.isNull("score")) t.optDouble("score") else null,
                    year = if (t.has("year") && !t.isNull("year")) t.optInt("year") else null,
                    status = t.optString("status").ifBlank { null },
                    totalEpisodes = nums.size,
                    latestEpisode = nums.maxOrNull() ?: 0,
                    episodeNumbers = nums,
                    updatedAt = null,
                    anilistId = null,
                    country = t.optString("country").ifBlank { null },
                )
                out += WhTitle(api, playlists)
            } catch (e: Exception) {
                // Satu judul rusak tidak boleh menggagalkan seluruh katalog.
                continue
            }
        }
        return out
    }

    // ---------------- katalog publik ----------------

    suspend fun fetchCatalog(
        onProgress: (List<Anime>) -> Unit = {},
        onRaw: (List<ApiAnime>) -> Unit = {},
    ): List<Anime> {
        val text = fetchRawCatalog()
        val (raw, mapped) = withContext(Dispatchers.Default) {
            val wh = parseWarehouse(text)
            warehouse = wh.associateBy { it.api.id }
            val rw = wh.map { it.api }
            val m = rw.map { it.toAnime() }
            Catalog.warm(m)
            rw to m
        }
        onProgress(mapped)
        onRaw(raw)
        return mapped
    }

    /** Pastikan peta gudang terisi (untuk detail/putar sebelum katalog selesai dimuat). */
    private suspend fun ensureWarehouse(): Map<String, WhTitle> {
        warehouse.takeIf { it.isNotEmpty() }?.let { return it }
        val text = fetchRawCatalog()
        val wh = withContext(Dispatchers.Default) { parseWarehouse(text) }
        warehouse = wh.associateBy { it.api.id }
        return warehouse
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

    // ---------------- jadwal (AniList langsung, tidak berubah) ----------------

    suspend fun fetchSchedule(): List<ApiScheduleItem> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis() / 1000
        val out = LinkedHashMap<String, ApiScheduleItem>()
        var page = 1
        while (page <= 8) {
            val query = "query(\$p:Int,\$a:Int,\$b:Int){Page(page:\$p,perPage:50){pageInfo{hasNextPage}" +
                "airingSchedules(airingAt_greater:\$a,airingAt_lesser:\$b,sort:TIME){airingAt episode mediaId}}}"
            val body = org.json.JSONObject()
                .put("query", query)
                .put("variables", org.json.JSONObject().put("p", page).put("a", now).put("b", now + 7 * 86400))
                .toString()
            val root = org.json.JSONObject(anilistPost(body))
            val data = root.optJSONObject("data")
                ?: throw java.io.IOException("AniList: " + (root.optJSONArray("errors")?.toString()?.take(120) ?: "tanpa data"))
            val pg = data.getJSONObject("Page")
            val arr = pg.getJSONArray("airingSchedules")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val item = ApiScheduleItem(anilistId = o.getInt("mediaId"), ep = o.getInt("episode"), airingAt = o.getLong("airingAt"))
                out["${item.anilistId}:${item.ep}"] = item
            }
            if (!pg.getJSONObject("pageInfo").getBoolean("hasNextPage")) break
            page++
        }
        out.values.toList()
    }

    private suspend fun anilistPost(body: String): String {
        var last: java.io.IOException? = null
        for (attempt in 0 until 3) {
            var waitSec = 1L shl attempt
            try {
                val req = okhttp3.Request.Builder().url("https://graphql.anilist.co")
                    .header("Accept", "application/json")
                    .post(body.toRequestBody("application/json".toMediaType())).build()
                val res = Net.client.newCall(req).execute().use { r ->
                    Triple(r.code, r.body?.string(), r.header("Retry-After")?.toLongOrNull())
                }
                val (code, text, retryAfter) = res
                if (code in 200..299 && text != null) return text
                if (code != 429 && code < 500) throw IllegalStateException("AniList HTTP $code")
                if (retryAfter != null) waitSec = retryAfter
                last = java.io.IOException("AniList HTTP $code")
            } catch (e: java.io.IOException) {
                last = e
            }
            if (attempt < 2) kotlinx.coroutines.delay(waitSec.coerceIn(1L, 10L) * 1000L)
        }
        throw last ?: java.io.IOException("AniList gagal")
    }

    private fun scheduleCacheFile(ctx: android.content.Context) =
        java.io.File(ctx.applicationContext.cacheDir, "schedule_cache.json")

    fun readScheduleCache(ctx: android.content.Context): ScheduleCache? = try {
        val f = scheduleCacheFile(ctx)
        if (!f.exists()) null else {
            val root = org.json.JSONObject(f.readText())
            val arr = root.getJSONArray("items")
            val now = System.currentTimeMillis() / 1000
            val items = ArrayList<ApiScheduleItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val t = o.getLong("t")
                if (t > now) items += ApiScheduleItem(anilistId = o.getInt("id"), ep = o.getInt("ep"), airingAt = t)
            }
            if (items.isEmpty()) null else ScheduleCache(items, root.getLong("at"))
        }
    } catch (e: Exception) {
        null
    }

    fun writeScheduleCache(ctx: android.content.Context, items: List<ApiScheduleItem>, savedAt: Long) {
        runCatching {
            val arr = org.json.JSONArray()
            items.forEach { arr.put(org.json.JSONObject().put("id", it.anilistId).put("ep", it.ep).put("t", it.airingAt)) }
            val f = scheduleCacheFile(ctx)
            val tmp = java.io.File(f.parentFile, f.name + ".tmp")
            tmp.writeText(org.json.JSONObject().put("at", savedAt).put("items", arr).toString())
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
        }
    }

    // ---------------- detail (dari katalog yang sama) ----------------

    suspend fun fetchDetail(id: String): Anime? {
        val hit = warehouse[id] ?: try { ensureWarehouse()[id] } catch (e: Exception) { null }
        return hit?.api?.toAnime()
    }

    /**
     * Laporan penonton dinonaktifkan dengan anggun: tidak ada lagi
     * server laporan (API lama dibuang). Mengembalikan false agar
     * pemanggil menyimpan catatan secara lokal saja.
     */
    suspend fun sendReport(anilistId: Int?, slug: String, ep: Int, note: String): Boolean = false

    // ---------------- putar (playlist gudang + pintu CDN) ----------------

    /** Pintu-pintu CDN untuk satu URL playlist gudang (pola yang sama dengan Anilover). */
    fun doorVariants(url: String): List<ApiDoor> {
        val m = Regex("""^https://[A-Za-z0-9.-]+/gh/([^/]+)/([^/@]+)@([^/]+)/(.+)$""").find(url)
            ?: return listOf(ApiDoor("jsDelivr", url))
        val (owner, repo, pin, path) = m.destructured
        val gh = "$owner/$repo@$pin/$path"
        val raw = "$owner/$repo/$pin/$path"
        return listOf(
            ApiDoor("jsDelivr", "https://cdn.jsdelivr.net/gh/$gh"),
            ApiDoor("jsDelivr Fastly", "https://fastly.jsdelivr.net/gh/$gh"),
            ApiDoor("jsDelivr Gcore", "https://gcore.jsdelivr.net/gh/$gh"),
            ApiDoor("jsDelivr Quantil", "https://quantil.jsdelivr.net/gh/$gh"),
            ApiDoor("jsDelivr Testing", "https://testingcf.jsdelivr.net/gh/$gh"),
            ApiDoor("jsDelivr B-CDN", "https://jsdelivr.b-cdn.net/gh/$gh"),
            ApiDoor("JSDMirror", "https://cdn.jsdmirror.com/gh/$gh"),
            ApiDoor("GitHack", "https://raw.githack.com/$raw"),
            ApiDoor("GitHub Raw", "https://raw.githubusercontent.com/$raw"),
            ApiDoor("GHProxy", "https://ghproxy.net/https://raw.githubusercontent.com/$raw"),
            ApiDoor("Statically", "https://cdn.statically.io/gh/$gh"),
        )
    }

    private class Prefetched(val at: Long, val pb: Playback)
    private val prefetched = java.util.concurrent.ConcurrentHashMap<String, Prefetched>()
    private const val PREFETCH_TTL_MS = 600_000L

    fun peekPrefetched(id: String, ep: Int): Playback? =
        prefetched["$id:$ep"]?.takeIf { System.currentTimeMillis() - it.at < PREFETCH_TTL_MS }?.pb

    suspend fun prefetchPlayback(id: String, ep: Int) {
        val k = "$id:$ep"
        if (prefetched[k]?.let { System.currentTimeMillis() - it.at < PREFETCH_TTL_MS } == true) return
        val pb = try { buildPlayback(id, ep) } catch (e: Exception) { null } ?: return
        if (!pb.hasStream) return
        prefetched[k] = Prefetched(System.currentTimeMillis(), pb)
    }

    suspend fun fetchPlayback(id: String, ep: Int, fresh: Boolean = false): Playback? {
        val k = "$id:$ep"
        val hit = prefetched.remove(k)
        if (!fresh && hit != null && System.currentTimeMillis() - hit.at < PREFETCH_TTL_MS && hit.pb.hasStream) return hit.pb
        val waits = longArrayOf(0L, 600L, 1_500L)
        for (w in waits) {
            if (w > 0) kotlinx.coroutines.delay(w)
            val got = try {
                buildPlayback(id, ep)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (got != null && got.hasStream) return got
        }
        return null
    }

    /**
     * Bangun data putar dari katalog: playlist master episode ini,
     * daftar pintu CDN, dan tangga unduhan per pintu dari varian
     * manifest. Subtitle tidak dimodelkan di sini — trek teks bawaan
     * manifest HLS dibaca pemutar langsung (pola gudang yang sudah ada).
     */
    private suspend fun buildPlayback(id: String, ep: Int): Playback? = withContext(Dispatchers.IO) {
        val t = warehouse[id] ?: try { ensureWarehouse()[id] } catch (e: Exception) { null }
            ?: return@withContext null
        val master = t.playlists[ep] ?: return@withContext null
        val doors = doorVariants(master)
        // Ambil manifest master dari pintu pertama yang menjawab sah.
        var masterText: String? = null
        var usedDoor: ApiDoor? = null
        for (d in doors) {
            try {
                val txt = httpGetText(d.url, timeoutSec = 8)
                if (txt.trimStart().startsWith("#EXTM3U")) {
                    masterText = txt
                    usedDoor = d
                    break
                }
            } catch (e: Exception) { /* coba pintu berikutnya */ }
        }
        val dlDoors = ArrayList<ApiDownloadDoor>()
        val mt = masterText
        val ud = usedDoor
        // Trek subtitle yang diumumkan manifest master (bahasa + label + URL
        // absolut). Dipakai untuk unduhan luring per pintu DAN untuk daftar
        // trek subtitle putar daring — lihat catatan di bawah soal rendition
        // mentah vs playlist.
        var renditions: List<SubRendition> = emptyList()
        if (mt != null && ud != null) {
            val variants = DoorMath.parseVariants(mt, ud.url)
            val masterDir = DoorMath.dirOf(ud.url)
            fun relOf(abs: String): String? =
                if (abs.startsWith(masterDir)) abs.removePrefix(masterDir) else null
            renditions = SubRenditions.parse(mt, ud.url)
            val subEn: String? = SubRenditions.firstEnglish(renditions)?.url?.let { relOf(it) }
            val subId: String? = SubRenditions.firstIndonesian(renditions)?.url?.let { relOf(it) }
            fun variantRel(height: Int): String? {
                val v = variants.filter { it.height == height }.firstOrNull()
                    ?: variants.minByOrNull { kotlin.math.abs(it.height - height) }
                    ?: return null
                return relOf(v.uri)
            }
            val r1080 = variantRel(1080)
            val r720 = variantRel(720)
            val r480 = variantRel(480)
            for (d in doors) {
                val dir = DoorMath.dirOf(d.url)
                fun at(rel: String?): String = if (rel.isNullOrBlank()) "" else dir + rel
                dlDoors += ApiDownloadDoor(
                    name = d.name,
                    q1080 = at(r1080), q720 = at(r720), q480 = at(r480),
                    subsEn = subEn?.let { dir + it },
                    subsId = subId?.let { dir + it },
                )
            }
        }
        Playback(
            streamUrl = master,
            episode = ep,
            // debug.75: SUBTITLE GUDANG DARING KOSONG = BUG "subtitle Indonesia
            // tidak tampil". Daftar kosong membuat menu menulis "Not available",
            // tombol CC disembunyikan, dan onTracksChanged memanggil applySub(-1)
            // yang MEMATIKAN seluruh tipe trek teks — termasuk grup teks bawaan
            // manifest. Trek rendition kini dibawa di sini sebagai trek samping
            // (Media3 menolak URI .vtt mentah sebagai rendition HLS); rendition
            // yang benar-benar playlist (.m3u8) dibiarkan ke Media3.
            subtitles = renditions.filter { it.sideLoadable }.map {
                ApiSubtitle(lang = it.lang, label = it.label, url = it.url)
            },
            source = "gudang",
            doors = doors,
            download = if (dlDoors.isNotEmpty()) ApiDownload(downloadable = true, doors = dlDoors) else null,
        )
    }

    /** Poster selalu dari katalog; tidak ada resolusi poster terpisah. */
    suspend fun resolvePoster(anilistId: Int): PosterResp? = null

    /** Log diagnostik ke server dinonaktifkan (tidak ada server log lagi); diabaikan dengan anggun. */
    suspend fun uploadDiag(tag: String) { /* nonaktif */ }
}

/** Jadwal tersimpan di disk + waktu simpan (epoch ms). */
class ScheduleCache(val items: List<ApiScheduleItem>, val savedAt: Long)

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
        hd = false, // katalog tidak menyediakan info kualitas; jangan klaim HD
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
