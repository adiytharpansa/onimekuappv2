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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
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

    private class Cue(val startMs: Long, val endMs: Long, val text: String)
    private class Plan(val src: ApiSubtitle, val srcTag: String, val need: List<String>)

    private val active = AtomicInteger(0)
    /** true selama ada terjemahan berjalan (untuk keterangan "Menerjemahkan..." di menu subtitle). */
    var busy by mutableStateOf(false)
        private set
    private fun busyUpdate(delta: Int) { busy = active.addAndGet(delta) > 0 }

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
            !cm.isActiveNetworkMetered
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
        ctx: Context, id: String, ep: Int, tracks: List<ApiSubtitle>, o: Opts,
        load: suspend (ApiSubtitle) -> String?,
    ): List<ApiSubtitle> = withContext(Dispatchers.IO) {
        if (!allowed(ctx, o)) return@withContext cached(ctx, id, ep, tracks)
        val p = plan(tracks) ?: return@withContext tracks
        val out = tracks.toMutableList()
        var cues: List<Cue>? = null
        for (tgt in p.need) {
            val f = outFile(ctx, id, ep, tgt)
            if (!(f.exists() && f.length() > 0L)) {
                if (cues == null) {
                    cues = try {
                        load(p.src)?.let { parse(it) }
                    } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { null }
                }
                val c = cues
                if (c.isNullOrEmpty()) continue
                busyUpdate(1)
                val done = try {
                    translateAll(c, p.srcTag, tgt, o)
                } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { null } finally { busyUpdate(-1) }
                if (done == null) continue
                val tmp = File(f.parentFile, f.name + ".tmp")
                tmp.writeText(toSrt(done))
                tmp.renameTo(f)
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
        if (o.engineUrl.isNotBlank()) {
            tl = try { viaEngine(o.engineUrl, texts, src, tgt) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { null }
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

    /** Gabungkan baris yang kalimatnya terpotong (maks 3 baris, jeda <= 1,5 dtk) agar terjemahan punya konteks. */
    private fun group(cues: List<Cue>): List<IntRange> {
        val out = ArrayList<IntRange>()
        var i = 0
        while (i < cues.size) {
            var j = i
            var len = cues[i].text.length
            while (j + 1 < cues.size && j - i < 2 && !endsSentence(cues[j].text) &&
                cues[j + 1].startMs - cues[j].endMs <= 1500L && len + cues[j + 1].text.length < 220) {
                j++; len += cues[j].text.length
            }
            out.add(i..j)
            i = j + 1
        }
        return out
    }

    private fun endsSentence(s: String): Boolean {
        val t = s.trimEnd()
        if (t.isEmpty()) return true
        return t.last() in ".?!\u2026\u3002\uFF01\uFF1F\"\u300D\u300F)\u266A"
    }

    /** Pecah hasil terjemahan gabungan kembali ke tiap baris asli, sebanding dengan panjang teks aslinya. */
    private fun splitBack(text: String, cues: List<Cue>, r: IntRange): List<String> {
        val n = r.last - r.first + 1
        if (n == 1) return listOf(text)
        val words = text.split(" ").filter { it.isNotEmpty() }
        if (words.size < n) return List(n) { if (it == 0) text else "" }
        val w = r.map { cues[it].text.length.coerceAtLeast(1) }
        val total = w.sum().toDouble()
        val out = ArrayList<String>()
        var used = 0
        var acc = 0
        for (k in 0 until n) {
            if (k == n - 1) { out.add(words.subList(used, words.size).joinToString(" ")); break }
            acc += w[k]
            val target = Math.round(words.size * acc / total).toInt().coerceIn(used + 1, words.size - (n - 1 - k))
            out.add(words.subList(used, target).joinToString(" "))
            used = target
        }
        return out
    }

    private val stopNames = setOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
        "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December",
        "English", "Japanese", "Japan", "Christmas", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu",
    )
    private val nameRe = Regex("(?<=[a-z,;] )[A-Z][a-zA-Z-]{2,}")

    /** Nama (kata kapital di tengah kalimat yang muncul >= 2 kali) dilindungi agar tidak ikut "diterjemahkan". */
    private fun names(cues: List<Cue>): List<String> {
        val cnt = HashMap<String, Int>()
        for (c in cues) for (m in nameRe.findAll(c.text)) cnt[m.value] = (cnt[m.value] ?: 0) + 1
        return cnt.filter { it.value >= 2 && it.key !in stopNames }.keys.sortedByDescending { it.length }
    }

    private suspend fun viaMlKit(texts: List<String>, names: List<String>, src: String, tgt: String): List<String> {
        val tr = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(src).setTargetLanguage(tgt).build())
        try {
            tr.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitTask()
            val pats = names.map { it to Regex("\\b" + Regex.escape(it) + "\\b") }
            val memo = HashMap<String, String>()
            val out = ArrayList<String>(texts.size)
            for (text in texts) {
                val hit = memo[text]
                if (hit != null) { out.add(hit); continue }
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
                val fin = res.ifBlank { text }
                memo[text] = fin
                out.add(fin)
            }
            return out
        } finally {
            tr.close()
        }
    }

    /** Server sendiri: POST {"source","target","lines":[...]} -> {"lines":[...]} (jumlah baris sama). */
    private fun viaEngine(url: String, texts: List<String>, src: String, tgt: String): List<String>? {
        val client = Net.client.newBuilder().callTimeout(90, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).build()
        val out = ArrayList<String>()
        for (chunk in texts.chunked(40)) {
            val body = JSONObject().put("source", src).put("target", tgt).put("lines", JSONArray(chunk)).toString()
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url(url).post(body).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val arr = JSONObject(resp.body?.string() ?: return null).optJSONArray("lines") ?: return null
                if (arr.length() != chunk.size) return null
                for (i in 0 until arr.length()) out.add(arr.optString(i).ifBlank { chunk[i] })
            }
        }
        return out
    }

    // ------------------------------------------------------------------ model & penyimpanan
    /** Unduh model bahasa Indonesia + English lebih awal (hanya Wi-Fi) dan rapikan berkas terjemahan lama. */
    fun warm(ctx: Context) {
        try {
            val mgr = RemoteModelManager.getInstance()
            val cond = DownloadConditions.Builder().requireWifi().build()
            for (tag in listOf(TranslateLanguage.ENGLISH, TranslateLanguage.INDONESIAN)) {
                mgr.download(TranslateRemoteModel.Builder(tag).build(), cond).addOnFailureListener { }
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

    // ------------------------------------------------------------------ parsing
    private fun parse(raw: String): List<Cue> {
        val text = raw.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        return if (text.contains("[Script Info]", true) || text.contains("\nDialogue:")) parseAss(text) else parseSrtVtt(text)
    }

    private fun clean(s: String): String = s
        .replace(Regex("<[^>]+>"), "").replace(Regex("\\{[^}]*\\}"), "")
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ").trim()

    private val timing = Regex("""(\d{1,2}:)?(\d{1,2}):(\d{2})[.,](\d{1,3})\s*-->\s*(\d{1,2}:)?(\d{1,2}):(\d{2})[.,](\d{1,3})""")

    private fun ms(h: String, m: String, s: String, f: String): Long =
        (((h.removeSuffix(":").toIntOrNull() ?: 0) * 60L + m.toLong()) * 60L + s.toLong()) * 1000L + f.padEnd(3, '0').take(3).toLong()

    private fun parseSrtVtt(text: String): List<Cue> {
        val out = ArrayList<Cue>()
        for (block in text.split(Regex("\n{2,}"))) {
            val lines = block.split("\n")
            val ti = lines.indexOfFirst { it.contains("-->") }
            if (ti < 0) continue
            val g = timing.find(lines[ti])?.groupValues ?: continue
            val body = clean(lines.drop(ti + 1).joinToString(" "))
            if (body.isBlank()) continue
            out.add(Cue(ms(g[1], g[2], g[3], g[4]), ms(g[5], g[6], g[7], g[8]), body))
        }
        return out
    }

    private fun assMs(s: String): Long? = try {
        val p = s.trim().split(":")
        val sec = p[2].split(".")
        val cs = sec.getOrNull(1)?.padEnd(2, '0')?.take(2)?.toLong() ?: 0L
        ((p[0].toLong() * 60L + p[1].toLong()) * 60L + sec[0].toLong()) * 1000L + cs * 10L
    } catch (e: Exception) { null }

    private fun parseAss(text: String): List<Cue> {
        val out = ArrayList<Cue>()
        var fmt: List<String>? = null
        for (line in text.split("\n")) {
            val t = line.trim()
            if (fmt == null && t.startsWith("Format:", true) && t.contains("Start", true) && t.contains("Text", true)) {
                fmt = t.substringAfter(":").split(",").map { it.trim().lowercase(Locale.ROOT) }
            } else if (t.startsWith("Dialogue:", true)) {
                val f = fmt ?: listOf("layer", "start", "end", "style", "name", "marginl", "marginr", "marginv", "effect", "text")
                val parts = t.substringAfter(":").trimStart().split(",", limit = f.size)
                if (parts.size < f.size) continue
                val st = assMs(parts[f.indexOf("start").coerceAtLeast(0)]) ?: continue
                val en = assMs(parts[f.indexOf("end").coerceAtLeast(0)]) ?: continue
                val body = clean(parts[f.indexOf("text").let { if (it < 0) f.size - 1 else it }]
                    .replace("\\N", " ").replace("\\n", " ").replace("\\h", " "))
                if (body.isBlank()) continue
                out.add(Cue(st, en, body))
            }
        }
        return out.sortedBy { it.startMs }
    }

    private fun stamp(ms: Long): String =
        String.format(Locale.US, "%02d:%02d:%02d,%03d", ms / 3_600_000, ms / 60_000 % 60, ms / 1000 % 60, ms % 1000)

    private fun toSrt(cues: List<Cue>): String =
        cues.filter { it.text.isNotBlank() }
            .mapIndexed { i, c -> "${i + 1}\n${stamp(c.startMs)} --> ${stamp(c.endMs)}\n${c.text}\n" }.joinToString("\n")
}
