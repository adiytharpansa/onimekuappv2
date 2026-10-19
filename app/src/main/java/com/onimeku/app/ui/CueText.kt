package com.onimeku.app.ui

import java.util.Locale

/** Satu baris subtitle bertanda waktu (milidetik). */
internal data class Cue(val startMs: Long, val endMs: Long, val text: String)

/**
 * Logika teks subtitle yang murni (tanpa Android): baca SRT/VTT/ASS, tulis SRT, gabung baris terpotong
 * sebelum diterjemahkan, pecah balik hasilnya, dan deteksi nama karakter. Dipisah dari [SubTranslate] agar bisa diuji unit.
 */
internal object CueText {
    /** Gabungkan baris yang kalimatnya terpotong (maks 3 baris, jeda <= 1,5 dtk) agar terjemahan punya konteks. */
    fun group(cues: List<Cue>): List<IntRange> {
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

    fun endsSentence(s: String): Boolean {
        val t = s.trimEnd()
        if (t.isEmpty()) return true
        return t.last() in ".?!\u2026\u3002\uFF01\uFF1F\"\u300D\u300F)\u266A"
    }

    /** Pecah hasil terjemahan gabungan kembali ke tiap baris asli, sebanding dengan panjang teks aslinya. */
    fun splitBack(text: String, cues: List<Cue>, r: IntRange): List<String> {
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

    val stopNames = setOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
        "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December",
        "English", "Japanese", "Japan", "Christmas", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu",
    )
    val nameRe = Regex("(?<=[a-z,;] )[A-Z][a-zA-Z-]{2,}")

    /** Nama (kata kapital di tengah kalimat yang muncul >= 2 kali) dilindungi agar tidak ikut "diterjemahkan". */
    fun names(cues: List<Cue>): List<String> {
        val cnt = HashMap<String, Int>()
        for (c in cues) for (m in nameRe.findAll(c.text)) cnt[m.value] = (cnt[m.value] ?: 0) + 1
        return cnt.filter { it.value >= 2 && it.key !in stopNames }.keys.sortedByDescending { it.length }
    }

    fun parse(raw: String): List<Cue> {
        val text = raw.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        return if (text.contains("[Script Info]", true) || text.contains("\nDialogue:")) parseAss(text) else parseSrtVtt(text)
    }

    fun clean(s: String): String = s
        .replace(Regex("<[^>]+>"), "").replace(Regex("\\{[^}]*\\}"), "")
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ").trim()

    val timing = Regex("""(\d{1,2}:)?(\d{1,2}):(\d{2})[.,](\d{1,3})\s*-->\s*(\d{1,2}:)?(\d{1,2}):(\d{2})[.,](\d{1,3})""")

    fun ms(h: String, m: String, s: String, f: String): Long =
        (((h.removeSuffix(":").toIntOrNull() ?: 0) * 60L + m.toLong()) * 60L + s.toLong()) * 1000L + f.padEnd(3, '0').take(3).toLong()

    fun parseSrtVtt(text: String): List<Cue> {
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

    fun assMs(s: String): Long? = try {
        val p = s.trim().split(":")
        val sec = p[2].split(".")
        val cs = sec.getOrNull(1)?.padEnd(2, '0')?.take(2)?.toLong() ?: 0L
        ((p[0].toLong() * 60L + p[1].toLong()) * 60L + sec[0].toLong()) * 1000L + cs * 10L
    } catch (e: Exception) { null }

    fun parseAss(text: String): List<Cue> {
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

    fun stamp(ms: Long): String =
        String.format(Locale.US, "%02d:%02d:%02d,%03d", ms / 3_600_000, ms / 60_000 % 60, ms / 1000 % 60, ms % 1000)

    fun toSrt(cues: List<Cue>): String =
        cues.filter { it.text.isNotBlank() }
            .mapIndexed { i, c -> "${i + 1}\n${stamp(c.startMs)} --> ${stamp(c.endMs)}\n${c.text}\n" }.joinToString("\n")
}
