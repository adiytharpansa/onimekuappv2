package com.onimeku.app.ui

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.onimeku.app.data.ApiSubtitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Melengkapi trek subtitle Indonesia + English: bila episode hanya punya salah satu (atau bahasa lain),
 * trek yang kurang diterjemahkan di perangkat (ML Kit) dari trek yang ada, disimpan sebagai .srt di cache.
 * Bahasa trek hasil terjemahan diberi kode "id"/"en" sehingga ikut dipilih oleh Auto dan muncul di menu subtitle.
 */
object SubTranslate {
    private class Cue(val startMs: Long, val endMs: Long, val text: String)

    private class Plan(val src: ApiSubtitle, val srcTag: String, val need: List<String>)

    private fun outFile(ctx: Context, id: String, ep: Int, tgt: String): File {
        val dir = File(ctx.cacheDir, "subs").apply { mkdirs() }
        return File(dir, "${id}_${ep}_${tgt}_tr.srt".replace(Regex("[^A-Za-z0-9._-]"), "_"))
    }

    private fun asTrack(tgt: String, f: File) = ApiSubtitle(
        lang = tgt,
        label = if (tgt == TranslateLanguage.INDONESIAN) "Indonesia (terjemahan)" else "English (translated)",
        url = android.net.Uri.fromFile(f).toString(),
    )

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

    /** Terjemahkan trek yang kurang (bisa memakan waktu; unduh model bahasa sekali di awal). Gagal -> daftar asli. */
    suspend fun fill(ctx: Context, id: String, ep: Int, tracks: List<ApiSubtitle>): List<ApiSubtitle> = withContext(Dispatchers.IO) {
        val p = plan(tracks) ?: return@withContext tracks
        val out = tracks.toMutableList()
        var cues: List<Cue>? = null
        for (tgt in p.need) {
            val f = outFile(ctx, id, ep, tgt)
            if (!(f.exists() && f.length() > 0L)) {
                if (cues == null) {
                    cues = try {
                        parse(File(android.net.Uri.parse(p.src.url).path ?: "").readText())
                    } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { null }
                }
                val c = cues
                if (c.isNullOrEmpty()) continue
                val done = try {
                    translate(c, p.srcTag, tgt)
                } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { null }
                if (done == null) continue
                val tmp = File(f.parentFile, f.name + ".tmp")
                tmp.writeText(toSrt(done))
                tmp.renameTo(f)
            }
            if (f.exists() && f.length() > 0L) out.add(asTrack(tgt, f))
        }
        out
    }

    private suspend fun translate(cues: List<Cue>, src: String, tgt: String): List<Cue> {
        val tr = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(src).setTargetLanguage(tgt).build())
        try {
            tr.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitTask()
            val memo = HashMap<String, String>()
            return cues.map { c ->
                val t = memo[c.text] ?: tr.translate(c.text).awaitTask().ifBlank { c.text }.also { memo[c.text] = it }
                Cue(c.startMs, c.endMs, t)
            }
        } finally {
            tr.close()
        }
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
        cues.mapIndexed { i, c -> "${i + 1}\n${stamp(c.startMs)} --> ${stamp(c.endMs)}\n${c.text}\n" }.joinToString("\n")
}
