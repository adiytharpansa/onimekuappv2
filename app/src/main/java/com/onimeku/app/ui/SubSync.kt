package com.onimeku.app.ui

import java.util.Locale
import kotlin.math.abs

/**
 * Sinkronisasi subtitle (logika murni, bisa diuji unit): menggeser SEMUA waktu cue sebesar [offsetMs].
 * Positif = subtitle muncul LEBIH LAMBAT (dipakai bila teks tampil sebelum tokoh bicara);
 * negatif = lebih cepat (teks tampil setelah tokoh bicara).
 *
 * Media3 tidak punya "subtitle delay" untuk trek samping, jadi waktu di berkas lokal ditulis ulang
 * (berkas asli tetap utuh; hasil geseran disimpan di sebelahnya dengan akhiran ".s<ms>").
 *
 * Juga: [decode] (UTF-8 atau cadangan Windows-1252), [cueStartsMs] + [alignOffset] untuk "sinkronkan ke
 * ucapan sekarang" (hitung geseran dari satu baris yang didengar pengguna).
 */
internal object SubSync {
    const val MAX_MS = 30_000
    const val STEP_FINE = 100
    const val STEP_COARSE = 500
    /** Jendela pencarian baris terdekat untuk [alignOffset]. */
    const val ALIGN_WINDOW_MS = 20_000L

    fun clamp(ms: Int): Int = ms.coerceIn(-MAX_MS, MAX_MS)

    /** "0.0 s", "+0.5 s", "-1.2 s" — netral bahasa supaya sama di semua lokal. */
    fun label(ms: Int): String {
        val v = String.format(Locale.US, "%.1f s", ms / 1000.0)
        return if (ms > 0) "+$v" else v
    }

    /** Menyimpan geseran per judul; 0 = hapus entri. Entri terlama dibuang bila melebihi [max]. */
    fun remember(map: Map<String, Int>, id: String, ms: Int, max: Int = 200): Map<String, Int> {
        if (id.isBlank()) return map
        val m = LinkedHashMap(map)
        m.remove(id)
        if (ms != 0) m[id] = clamp(ms)
        while (m.size > max) m.remove(m.keys.first())
        return m
    }

    // ---- Encoding ----
    /**
     * Baca berkas subtitle: UTF-8 bila sahih, selain itu Windows-1252 (subtitle lama berhuruf Latin beraksen).
     * BOM dibuang. Hasil geseran selalu ditulis ulang sebagai UTF-8 (yang dibaca Media3).
     */
    fun decode(bytes: ByteArray): String {
        val s = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        } catch (e: java.nio.charset.CharacterCodingException) {
            String(bytes, java.nio.charset.Charset.forName("windows-1252"))
        }
        return s.removePrefix("\uFEFF")
    }

    // ---- Nama berkas ----
    private val SHIFTED = Regex("""\.s-?\d+(\.[A-Za-z0-9]+)$""")

    /** Jalur berkas asli dari jalur berkas hasil geseran (atau dirinya sendiri bila belum bergeser). */
    fun originalPath(path: String): String = SHIFTED.replace(path, "$1")

    /** Jalur berkas untuk geseran [offsetMs]; 0 = berkas asli. [orig] harus jalur berkas asli. */
    fun targetPath(orig: String, offsetMs: Int): String {
        if (offsetMs == 0) return orig
        val dot = orig.lastIndexOf('.')
        if (dot <= orig.lastIndexOf('/')) return "$orig.s$offsetMs"
        return orig.substring(0, dot) + ".s$offsetMs" + orig.substring(dot)
    }

    // ---- Penulisan ulang waktu ----
    private val NEWLINE = Regex("\r\n|\n|\r")

    /** Tulis ulang [text] berformat [ext] (".vtt" / ".srt" / ".ass" / ".ssa") dengan geseran [offsetMs]. */
    fun shift(text: String, ext: String, offsetMs: Int): String {
        if (offsetMs == 0) return text
        return when (ext.lowercase(Locale.US)) {
            ".ass", ".ssa" -> shiftAss(text, offsetMs)
            else -> shiftCueTimes(text, offsetMs, ext.equals(".srt", true))
        }
    }

    // [HH:]MM:SS.mmm atau [HH:]MM:SS,mmm
    private val CUE_TIME = Regex("""(?:(\d{1,3}):)?(\d{2}):(\d{2})[.,](\d{3})""")

    private fun cueMs(m: MatchResult): Long {
        val h = m.groupValues[1].ifEmpty { "0" }.toLong()
        return ((h * 60 + m.groupValues[2].toLong()) * 60 + m.groupValues[3].toLong()) * 1000 + m.groupValues[4].toLong()
    }

    private fun shiftCueTimes(text: String, offsetMs: Int, srt: Boolean): String {
        val sep = if (srt) ',' else '.'
        val out = StringBuilder(text.length + 64)
        val nl = if (text.contains("\r\n")) "\r\n" else "\n"
        for ((i, line) in text.split(NEWLINE).withIndex()) {
            if (i > 0) out.append(nl)
            if (line.contains("-->")) {
                out.append(CUE_TIME.replace(line) { m -> fmt(cueMs(m) + offsetMs, sep) })
            } else out.append(line)
        }
        return out.toString()
    }

    private fun fmt(msIn: Long, sep: Char): String {
        val ms = if (msIn < 0) 0L else msIn
        val h = ms / 3_600_000
        val m = (ms / 60_000) % 60
        val s = (ms / 1000) % 60
        val f = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d%c%03d", h, m, s, sep, f)
    }

    // ASS/SSA: "Dialogue: Layer,Start,End,Style,..." dengan waktu H:MM:SS.cc (sentidetik).
    private val ASS_TIME = Regex("""^\s*(\d+):(\d{2}):(\d{2})[.](\d{1,2})\s*$""")

    private fun assMs(m: MatchResult): Long {
        val frac = m.groupValues[4].padEnd(2, '0').toLong() * 10
        return ((m.groupValues[1].toLong() * 60 + m.groupValues[2].toLong()) * 60 + m.groupValues[3].toLong()) * 1000 + frac
    }

    private fun shiftAss(text: String, offsetMs: Int): String {
        val nl = if (text.contains("\r\n")) "\r\n" else "\n"
        return text.split(NEWLINE).joinToString(nl) { line ->
            if (!line.startsWith("Dialogue:") && !line.startsWith("Comment:")) return@joinToString line
            val f = line.split(",", limit = 10)
            if (f.size < 10) return@joinToString line
            val a = assShift(f[1], offsetMs) ?: return@joinToString line
            val b = assShift(f[2], offsetMs) ?: return@joinToString line
            (listOf(f[0], a, b) + f.drop(3)).joinToString(",")
        }
    }

    private fun assShift(t: String, offsetMs: Int): String? {
        val m = ASS_TIME.matchEntire(t) ?: return null
        val ms = assMs(m) + offsetMs
        val v = if (ms < 0) 0L else ms
        val cs = (v + 5) / 10
        return String.format(Locale.US, "%d:%02d:%02d.%02d", cs / 360_000, (cs / 6_000) % 60, (cs / 100) % 60, cs % 100)
    }

    // ---- Sinkron otomatis dari satu baris ----
    /** Waktu mulai (ms) tiap cue di berkas ASLI (belum bergeser). */
    fun cueStartsMs(text: String, ext: String): List<Long> {
        val ass = ext.equals(".ass", true) || ext.equals(".ssa", true)
        val out = ArrayList<Long>()
        for (line in text.split(NEWLINE)) {
            if (ass) {
                if (!line.startsWith("Dialogue:")) continue
                val f = line.split(",", limit = 10)
                if (f.size < 10) continue
                ASS_TIME.matchEntire(f[1])?.let { out.add(assMs(it)) }
            } else if (line.contains("-->")) {
                CUE_TIME.find(line)?.let { out.add(cueMs(it)) }
            }
        }
        return out
    }

    /**
     * "Sinkronkan ke ucapan sekarang": pengguna menekan tombol tepat saat tokoh mulai bicara di posisi [posMs].
     * Baris yang SEDANG berlaku (mulai paling dekat dengan [posMs] setelah digeser [currentOffsetMs]) dianggap baris
     * yang diucapkan; geseran baru dibuat agar awalnya tepat di [posMs]. null bila tak ada baris dalam [windowMs].
     */
    fun alignOffset(origStarts: List<Long>, currentOffsetMs: Int, posMs: Long, windowMs: Long = ALIGN_WINDOW_MS): Int? {
        var best: Long? = null
        var bestD = Long.MAX_VALUE
        for (s in origStarts) {
            val d = abs(s + currentOffsetMs - posMs)
            if (d < bestD) { bestD = d; best = s }
        }
        val b = best ?: return null
        if (bestD > windowMs) return null
        return clamp((posMs - b).toInt())
    }
}
