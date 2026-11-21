package com.onimeku.app.data

import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import okhttp3.Request

/**
 * Unduhan luring berbasis BERKAS polos (menggantikan DownloadManager +
 * SimpleCache Media3 yang dibuang bersama seluruh Media3).
 *
 * Bentuk unduhan satu episode: satu folder di `filesDir/downloads/<id>/<ep>/`
 * berisi segmen-segmen HLS (seg0000.ts, ...), berkas kunci/peta awal bila ada,
 * dan `episode.m3u8` — playlist varian yang ditulis ulang menunjuk ke berkas
 * lokal. Penanda `complete` ditulis paling akhir; tanpa penanda itu unduhan
 * dianggap belum selesai (putar luring menolak, unduh ulang melanjutkan dari
 * segmen yang sudah ada — resume di tingkat segmen).
 *
 * Pemutaran luring memakai mesin yang sama dengan streaming (mpv): ia cukup
 * membuka berkas `episode.m3u8` itu.
 */
object Downloads {

    /** Kunci unduhan yang disimpan di state ("dl:<id>:<episode>"). */
    fun key(id: String, ep: Int): String = "dl:$id:$ep"

    /** Balikkan [key] menjadi (id, episode); null bila bentuknya asing. */
    fun parseKey(k: String): Pair<String, Int>? {
        if (!k.startsWith("dl:")) return null
        val rest = k.removePrefix("dl:")
        val cut = rest.lastIndexOf(':')
        if (cut <= 0) return null
        val ep = rest.substring(cut + 1).toIntOrNull() ?: return null
        return rest.substring(0, cut) to ep
    }

    private fun safeName(id: String): String =
        id.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "anime" }

    private fun root(ctx: android.content.Context): File = File(ctx.filesDir, "downloads")

    fun dir(ctx: android.content.Context, id: String, ep: Int): File =
        File(File(root(ctx), safeName(id)), ep.toString())

    fun playlistFile(ctx: android.content.Context, id: String, ep: Int): File =
        File(dir(ctx, id, ep), "episode.m3u8")

    /** True bila unduhan episode ini utuh (penanda selesai + playlist ada). */
    fun isComplete(ctx: android.content.Context, id: String, ep: Int): Boolean {
        val d = dir(ctx, id, ep)
        return File(d, "complete").isFile && File(d, "episode.m3u8").isFile
    }

    /** Jalur playlist lokal untuk diputar mpv; null bila unduhan belum utuh. */
    fun localPlaylistPath(ctx: android.content.Context, id: String, ep: Int): String? =
        if (isComplete(ctx, id, ep)) playlistFile(ctx, id, ep).absolutePath else null

    fun remove(ctx: android.content.Context, id: String, ep: Int) {
        runCatching { dir(ctx, id, ep).deleteRecursively() }
    }

    fun removeAll(ctx: android.content.Context) {
        runCatching { root(ctx).deleteRecursively() }
    }

    /**
     * Pembersihan satu kali: buang cache SimpleCache Media3 lama
     * (`filesDir/media_downloads`) yang tidak bisa dibaca siapa pun lagi.
     * Ditandai di prefs agar tidak diulang tiap buka.
     */
    fun cleanupLegacyCache(ctx: android.content.Context) {
        val prefs = ctx.getSharedPreferences("dl_state", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("legacy_cache_removed", false)) return
        runCatching { File(ctx.filesDir, "media_downloads").deleteRecursively() }
        prefs.edit().putBoolean("legacy_cache_removed", true).apply()
    }

    /** Satu aset yang harus diunduh: URI asli di playlist + nama berkas lokalnya. */
    private data class Asset(val absUrl: String, val localName: String)

    /**
     * Unduh satu episode dari tangga URL varian [ladderUrls] (playlist varian
     * yang sama di beberapa pintu CDN; pintu pertama yang menjawab dipakai
     * sebagai dasar, segmen dicoba lintas pintu). [onProgress] menerima
     * 0..100. Mengembalikan true bila unduhan utuh dan penanda selesai ditulis.
     * Melempar CancellationException bila korutin dibatalkan (berkas parsial
     * dibiarkan untuk resume).
     */
    suspend fun download(
        ctx: android.content.Context,
        id: String,
        ep: Int,
        ladderUrls: List<String>,
        referer: String?,
        onProgress: (Int) -> Unit,
    ): Boolean {
        if (ladderUrls.isEmpty()) return false
        val outDir = dir(ctx, id, ep)
        outDir.mkdirs()
        // Unduhan baru/parsial tidak boleh tampak selesai.
        runCatching { File(outDir, "complete").delete() }

        // 1) Ambil teks playlist varian dari pintu pertama yang menjawab.
        var baseUrl: String? = null
        var text: String? = null
        for (u in ladderUrls) {
            val t = fetchText(u, referer) ?: continue
            if (t.contains("#EXTM3U")) { baseUrl = u; text = t; break }
        }
        val base = baseUrl ?: return false
        val lines = (text ?: return false).lines()

        // 2) Petakan setiap URI (segmen, kunci, peta awal) ke nama berkas lokal.
        val assets = LinkedHashMap<String, Asset>() // absUrl -> aset
        var segIdx = 0
        fun segAsset(abs: String): Asset = assets.getOrPut(abs) {
            val fn = DoorMath.fileNameOf(abs)
            val ext = fn.substringAfterLast('.', "").takeIf { it.isNotBlank() && it.length <= 5 }
            Asset(abs, "seg" + (segIdx++).toString().padStart(4, '0') + (ext?.let { ".$it" } ?: ".ts"))
        }
        val keyAbs = keyUriOf(lines, base)
        val mapAbs = mapUriOf(lines, base)
        val rewritten = ArrayList<String>(lines.size)
        for (line in lines) {
            val t = line.trim()
            when {
                t.startsWith("#EXT-X-KEY") && keyAbs != null && t.contains("URI=") -> {
                    val a = assets.getOrPut(keyAbs) { Asset(keyAbs, "key.bin") }
                    rewritten += replaceUriAttr(t, a.localName)
                }
                t.startsWith("#EXT-X-MAP") && mapAbs != null && t.contains("URI=") -> {
                    val a = assets.getOrPut(mapAbs) {
                        val fn = DoorMath.fileNameOf(mapAbs)
                        val ext = fn.substringAfterLast('.', "").takeIf { it.isNotBlank() && it.length <= 5 }
                        Asset(mapAbs, "init" + (ext?.let { ".$it" } ?: ".mp4"))
                    }
                    rewritten += replaceUriAttr(t, a.localName)
                }
                t.isNotEmpty() && !t.startsWith("#") -> rewritten += segAsset(DoorMath.resolve(base, t)).localName
                else -> rewritten += line
            }
        }
        val all = assets.values.toList()
        if (all.isEmpty()) return false

        // 3) Unduh semua aset, 4 pekerja sejajar, tiap aset dicoba lintas pintu.
        //    Berkas yang sudah ada dan tak kosong dilewati (resume).
        val baseDir = DoorMath.dirOf(base)
        fun candidates(asset: Asset): List<String> {
            val rel = asset.absUrl.removePrefix(baseDir)
            if (rel == asset.absUrl || rel.contains("..")) return listOf(asset.absUrl)
            return ladderUrls.map { DoorMath.dirOf(it) + rel }.distinct()
        }
        var doneCount = 0
        val total = all.size
        onProgress(0)
        val ok = coroutineScope {
            val groups = DoorMath.splitRoundRobin(all, 4)
            val jobs = groups.map { group ->
                async {
                    for (asset in group) {
                        ensureActive()
                        val target = File(outDir, asset.localName)
                        val got = if (target.isFile && target.length() > 0) true
                        else fetchFile(candidates(asset), referer, target)
                        synchronized(this@Downloads) {
                            doneCount++
                            onProgress((doneCount * 100 / total).coerceIn(0, 99))
                        }
                        if (!got) return@async false
                    }
                    true
                }
            }
            try {
                jobs.awaitAll().all { it }
            } catch (ce: CancellationException) {
                jobs.forEach { it.cancel() }
                throw ce
            }
        }
        if (!ok) return false

        // 4) Tulis playlist lokal + penanda selesai PALING akhir.
        playlistFile(ctx, id, ep).writeText(rewritten.joinToString("\n"))
        File(outDir, "complete").writeText("ok")
        onProgress(100)
        return true
    }

    private fun keyUriOf(lines: List<String>, base: String): String? {
        for (line in lines) {
            val t = line.trim()
            if (t.startsWith("#EXT-X-KEY") && t.contains("URI=\"")) {
                val uri = t.substringAfter("URI=\"").substringBefore('"')
                if (uri.isNotBlank()) return DoorMath.resolve(base, uri)
            }
        }
        return null
    }

    private fun mapUriOf(lines: List<String>, base: String): String? {
        for (line in lines) {
            val t = line.trim()
            if (t.startsWith("#EXT-X-MAP") && t.contains("URI=\"")) {
                val uri = t.substringAfter("URI=\"").substringBefore('"')
                if (uri.isNotBlank()) return DoorMath.resolve(base, uri)
            }
        }
        return null
    }

    private fun replaceUriAttr(tagLine: String, localName: String): String {
        val start = tagLine.indexOf("URI=\"")
        if (start < 0) return tagLine
        val end = tagLine.indexOf('"', start + 5)
        if (end < 0) return tagLine
        return tagLine.substring(0, start + 5) + localName + tagLine.substring(end)
    }

    private fun request(url: String, referer: String?): Request {
        val b = Request.Builder().url(url).header("User-Agent", Net.UA)
        if (!referer.isNullOrBlank()) b.header("Referer", referer)
        return b.build()
    }

    private fun fetchText(url: String, referer: String?): String? = runCatching {
        Net.client.newCall(request(url, referer)).execute().use { resp ->
            if (!resp.isSuccessful) null else resp.body?.string()
        }
    }.getOrNull()

    /** Unduh satu berkas dari daftar calon URL (pintu); true bila tersimpan utuh. */
    private fun fetchFile(candidates: List<String>, referer: String?, target: File): Boolean {
        val tmp = File(target.parentFile, target.name + ".part")
        for (url in candidates) {
            val ok = runCatching {
                Net.client.newCall(request(url, referer)).execute().use { resp ->
                    val body = resp.body
                    if (!resp.isSuccessful || body == null) false
                    else {
                        tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
                        tmp.length() > 0
                    }
                }
            }.getOrDefault(false)
            if (ok) {
                runCatching { if (target.exists()) target.delete(); tmp.renameTo(target) }
                if (target.isFile && target.length() > 0) return true
            }
        }
        runCatching { tmp.delete() }
        return false
    }
}
