package com.onimeku.app.ui

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.onimeku.app.data.ApiClient
import com.onimeku.app.data.ApiSubtitle
import com.onimeku.app.data.Net
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import com.onimeku.app.ui.CueText.group
import com.onimeku.app.ui.CueText.names
import com.onimeku.app.ui.CueText.parse
import com.onimeku.app.ui.CueText.splitBack
import com.onimeku.app.ui.CueText.toSrt
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Melengkapi trek subtitle Indonesia + English: bila episode hanya punya salah satu (atau bahasa lain),
 * trek yang kurang diterjemahkan dari trek yang ada lalu disimpan sebagai .srt di penyimpanan aplikasi.
 *
 * Mesin: (1) server sendiri bila diisi di Pengaturan (kualitas tinggi, mis. Claude/DeepL), (2) ML Kit di perangkat.
 * Peningkatan: kalimat terpotong digabung sebelum diterjemahkan, nama karakter dilindungi, terjemahan disiapkan
 * lebih awal (prefetch) dan model bahasa diunduh dini lewat Wi-Fi.
 */
object SubTranslate {
    /** Pengaturan terjemahan dari Settings. */
    data class Opts(val enabled: Boolean = true, val wifiOnly: Boolean = false, val engineUrl: String = "")

    private class Plan(val src: ApiSubtitle, val srcTag: String, val need: List<String>)

    private val active = AtomicInteger(0)
    /** true selama ada terjemahan berjalan (untuk keterangan "Menerjemahkan..." di menu subtitle). */
    var busy by mutableStateOf(false)
        private set
    private fun busyUpdate(delta: Int) {
        busy = active.addAndGet(delta) > 0
        if (!busy) progress = 0
    }

    /** Kemajuan terjemahan berjalan, 0..100 (untuk keterangan di menu subtitle). */
    var progress by mutableStateOf(0)
        private set

    /** Satu kunci per berkas keluaran: terjemahan otomatis dan permintaan pengguna tidak saling menimpa / mengulang kerja. */
    private val locks = ConcurrentHashMap<String, Mutex>()
    private fun lockFor(f: File): Mutex = locks.getOrPut(f.absolutePath) { Mutex() }

    /** Dipakai saat pengguna menekan Indonesian: tetap jalan walau terjemahan otomatis mati / hanya-Wi-Fi, hanya Indonesian, dan segera. */
    suspend fun fillIndonesian(ctx: Context, id: String, ep: Int, tracks: List<ApiSubtitle>, o: Opts): List<ApiSubtitle> =
        run(ctx, id, ep, tracks, o.copy(enabled = true, wifiOnly = false), only = TranslateLanguage.INDONESIAN) { s ->
            File(android.net.Uri.parse(s.url).path ?: "").readText()
        }

    /** Catat kegagalan terjemahan ke log aplikasi (tanpa mengganggu pengguna) agar bisa dilacak. */
    private fun log(msg: String) { try { com.onimeku.app.data.CrashLog.note("subtrans", msg) } catch (e: Exception) { } }

    private fun dir(ctx: Context) = File(ctx.filesDir, "subtrans").apply { mkdirs() }

    private fun outFile(ctx: Context, id: String, ep: Int, tgt: String): File =
        File(dir(ctx), "${id}_${ep}_${tgt}_tr.srt".replace(Regex("[^A-Za-z0-9._-]"), "_"))

    private fun asTrack(tgt: String, f: File) = ApiSubtitle(
        lang = tgt,
        label = if (tgt == TranslateLanguage.INDONESIAN) "Indonesia (terjemahan)" else "English (translated)",
        url = android.net.Uri.fromFile(f).toString(),
    )

    private fun allowed(ctx: Context, o: Opts): Boolean {
        if (!o.enabled) return false
        if (!o.wifiOnly) return true
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val metered = cm.isActiveNetworkMetered
            !metered
        } catch (e: Exception) { true }
    }

    private fun plan(tracks: List<ApiSubtitle>): Plan? {
        val valid = tracks.filter { it.url.isNotBlank() }
        val hasId = valid.any { SubLang.code(it) == "id" }
        val hasEn = valid.any { SubLang.code(it) == "en" }
        if (hasId && hasEn) return null
        val src = valid.firstOrNull { SubLang.code(it) == "en" }
            ?: valid.firstOrNull { SubLang.code(it) == "id" }
            ?: valid.firstOrNull() ?: return null
        val srcTag = when (SubLang.code(src)) {
            "en" -> TranslateLanguage.ENGLISH
            "id" -> TranslateLanguage.INDONESIAN
            else -> TranslateLanguage.fromLanguageTag(src.lang.lowercase(Locale.ROOT).take(2)) ?: return null
        }
        val need = ArrayList<String>()
        if (!hasId && srcTag != TranslateLanguage.INDONESIAN) need.add(TranslateLanguage.INDONESIAN)
        if (!hasEn && srcTag != TranslateLanguage.ENGLISH) need.add(TranslateLanguage.ENGLISH)
        if (need.isEmpty()) return null
        return Plan(src, srcTag, need)
    }

    /** Cepat, tanpa jaringan: tambahkan hanya terjemahan yang sudah pernah dibuat. */
    fun cached(ctx: Context, id: String, ep: Int, tracks: List<ApiSubtitle>): List<ApiSubtitle> {
        val p = plan(tracks) ?: return tracks
        val out = tracks.toMutableList()
        for (tgt in p.need) {
            val f = outFile(ctx, id, ep, tgt)
            if (f.exists() && f.length() > 0L) out.add(asTrack(tgt, f))
        }
        return out
    }

    /** Terjemahkan trek yang kurang dari berkas subtitle lokal (sudah diunduh pemutar). Gagal -> daftar asli. */
    suspend fun fill(ctx: Context, id: String, ep: Int, tracks: List<ApiSubtitle>, o: Opts): List<ApiSubtitle> =
        run(ctx, id, ep, tracks, o) { s ->
            File(android.net.Uri.parse(s.url).path ?: "").readText()
        }

    /** Siapkan terjemahan episode [ep] lebih awal (mis. episode berikutnya) dari data putar yang sudah di-prefetch. */
    suspend fun prefetch(ctx: Context, id: String, ep: Int, o: Opts) {
        val pb = ApiClient.peekPrefetched(id, ep) ?: return
        val subs = pb.subtitles.filter { it.url.isNotBlank() }
        if (subs.isEmpty()) return
        run(ctx, id, ep, subs, o) { s ->
            val req = Request.Builder().url(s.url)
                .header("Referer", s.referer ?: pb.referer ?: "")
                .header("User-Agent", Net.UA).build()
            Net.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null else resp.body?.string()
            }
        }
    }

    private suspend fun run(
        ctx: Context, id: String, ep: Int, tracks: List<ApiSubtitle>, o: Opts, only: String? = null,
        load: suspend (ApiSubtitle) -> String?,
    ): List<ApiSubtitle> = withContext(Dispatchers.IO) {
        if (!allowed(ctx, o)) return@withContext cached(ctx, id, ep, tracks)
        val p = plan(tracks) ?: return@withContext tracks
        val targets = if (only == null) p.need else p.need.filter { it == only }
        val out = tracks.toMutableList()
        var cues: List<Cue>? = null
        for (tgt in targets) {
            val f = outFile(ctx, id, ep, tgt)
            lockFor(f).withLock {
                if (!(f.exists() && f.length() > 0L)) {
                    if (cues == null) {
                        cues = try {
                            load(p.src)?.let { parse(it) }
                        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                            log("gagal membaca sumber $id ep$ep: ${e.javaClass.simpleName}")
                            null
                        }
                    }
                    val c = cues
                    if (!c.isNullOrEmpty()) {
                        busyUpdate(1)
                        val done = try {
                            translateAll(c, p.srcTag, tgt, o)
                        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                            log("gagal menerjemahkan $id ep$ep -> $tgt: ${e.javaClass.simpleName}")
                            null
                        } finally { busyUpdate(-1) }
                        if (done == null) log("terjemahan $id ep$ep -> $tgt tidak menghasilkan berkas")
                        if (done != null) {
                            val tmp = File(f.parentFile, f.name + ".tmp")
                            tmp.writeText(toSrt(done))
                            tmp.renameTo(f)
                        }
                    }
                }
            }
            if (f.exists() && f.length() > 0L) out.add(asTrack(tgt, f))
        }
        out
    }

    // ------------------------------------------------------------------ penerjemahan
    private suspend fun translateAll(cues: List<Cue>, src: String, tgt: String, o: Opts): List<Cue>? {
        val ranges = group(cues)
        val texts = ranges.map { r -> r.joinToString(" ") { cues[it].text } }
        var tl: List<String>? = null
        // Urutan: server pilihan pengguna (Pengaturan) -> server bersama bawaan (cache + AI gratis) -> ML Kit di HP.
        val engine = o.engineUrl.ifBlank { com.onimeku.app.BuildConfig.TR_URL }
        if (engine.isNotBlank()) {
            tl = try { viaEngine(engine, texts, names(cues), src, tgt) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { null }
        }
        if (tl == null) tl = viaMlKit(texts, names(cues), src, tgt)
        if (tl.size != ranges.size) return null
        val out = ArrayList<Cue>()
        for ((k, r) in ranges.withIndex()) {
            val pieces = splitBack(tl[k], cues, r)
            for ((j, idx) in r.withIndex()) out.add(Cue(cues[idx].startMs, cues[idx].endMs, pieces[j]))
        }
        return out
    }

    /** Jumlah terjemahan ML Kit yang berjalan bersamaan; cukup untuk mengisi antrean tanpa membebani HP. */
    private const val PARALLEL = 4

    private suspend fun viaMlKit(texts: List<String>, names: List<String>, src: String, tgt: String): List<String> {
        val tr = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(src).setTargetLanguage(tgt).build())
        try {
            tr.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitTask()
            val pats = names.map { it to Regex("\\b" + Regex.escape(it) + "\\b") }
            val memo = ConcurrentHashMap<String, String>()
            val gate = Semaphore(PARALLEL)
            val finished = AtomicInteger(0)
            val total = texts.size.coerceAtLeast(1)
            suspend fun one(text: String): String {
                memo[text]?.let { return it }
                val used = pats.withIndex().filter { it.value.second.containsMatchIn(text) }
                val res: String = if (used.isEmpty()) {
                    tr.translate(text).awaitTask()
                } else {
                    var t = text
                    for (u in used) t = t.replace(u.value.second, "QQ${u.index}QQ")
                    var o = tr.translate(t).awaitTask()
                    if (used.all { o.contains("QQ${it.index}QQ") }) {
                        for (u in used) o = o.replace("QQ${u.index}QQ", u.value.first)
                        o
                    } else {
                        tr.translate(text).awaitTask()
                    }
                }
                return res.ifBlank { text }.also { memo[text] = it }
            }
            // Hasil tetap urut sesuai teks asli (awaitAll menjaga urutan), tetapi dikerjakan paralel.
            return coroutineScope {
                texts.map { text ->
                    async {
                        val r = gate.withPermit { one(text) }
                        progress = finished.incrementAndGet() * 100 / total
                        r
                    }
                }.awaitAll()
            }
        } finally {
            tr.close()
        }
    }

    /**
     * Server terjemahan: POST {"source","target","lines":[...],"names":[...]} -> {"lines":[...]} (jumlah baris sama).
     * Server bersama menyimpan hasil per isi teks, jadi episode yang sudah pernah diterjemahkan pengguna lain tiba seketika.
     * Gagal di tengah (mis. jatah AI harian habis -> 503) -> null, pemanggil jatuh ke ML Kit.
     */
    private fun viaEngine(url: String, texts: List<String>, names: List<String>, src: String, tgt: String): List<String>? {
        val client = Net.client.newBuilder().callTimeout(90, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).build()
        val key = com.onimeku.app.BuildConfig.TR_KEY
        val shared = url == com.onimeku.app.BuildConfig.TR_URL  // kunci aplikasi hanya dikirim ke server bawaan
        val out = ArrayList<String>()
        for (chunk in texts.chunked(30)) {
            val body = JSONObject().put("source", src).put("target", tgt).put("lines", JSONArray(chunk))
                .put("names", JSONArray(names.take(40))).toString()
                .toRequestBody("application/json".toMediaType())
            val rb = Request.Builder().url(url).post(body)
            if (shared && key.isNotBlank()) rb.header("x-onimeku-key", key)
            client.newCall(rb.build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val arr = JSONObject(resp.body?.string() ?: return null).optJSONArray("lines") ?: return null
                if (arr.length() != chunk.size) return null
                for (i in 0 until arr.length()) out.add(arr.optString(i).ifBlank { chunk[i] })
            }
        }
        return out
    }

    // ------------------------------------------------------------------ model & penyimpanan
    /**
     * Siapkan model bahasa Indonesia + English (keduanya dibutuhkan untuk English -> Indonesia) begitu aplikasi dibuka.
     * Pembukaan pertama (pengguna baru): unduh LANGSUNG lewat jaringan apa pun supaya tombol Indonesian siap pakai.
     * Setelah model tercatat terunduh, pembukaan berikutnya hanya memeriksa ulang lewat Wi-Fi (mis. model terhapus sistem).
     * Bila unduhan pertama gagal (tanpa internet), dicoba lagi di pembukaan berikutnya.
     */
    fun warm(ctx: Context) {
        try {
            val mgr = RemoteModelManager.getInstance()
            val prefs = ctx.getSharedPreferences("subtrans", Context.MODE_PRIVATE)
            for (tag in listOf(TranslateLanguage.ENGLISH, TranslateLanguage.INDONESIAN)) {
                val key = "model_ready_$tag"
                val model = TranslateRemoteModel.Builder(tag).build()
                val firstRun = !prefs.getBoolean(key, false)
                mgr.isModelDownloaded(model).addOnSuccessListener { have ->
                    if (have) {
                        prefs.edit().putBoolean(key, true).apply()
                    } else {
                        val cond = DownloadConditions.Builder().apply { if (!firstRun) requireWifi() }.build()
                        mgr.download(model, cond)
                            .addOnSuccessListener { prefs.edit().putBoolean(key, true).apply() }
                            .addOnFailureListener { }
                    }
                }.addOnFailureListener { }
            }
        } catch (e: Exception) { }
        trim(ctx)
    }

    private fun trim(ctx: Context) {
        try {
            val files = (dir(ctx).listFiles() ?: return).sortedByDescending { it.lastModified() }
            val old = System.currentTimeMillis() - 45L * 24 * 3600 * 1000
            files.forEachIndexed { i, f -> if (i >= 400 || f.lastModified() < old) f.delete() }
        } catch (e: Exception) { }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { c ->
        addOnSuccessListener { c.resume(it) }
        addOnFailureListener { c.resumeWithException(it) }
    }

}
