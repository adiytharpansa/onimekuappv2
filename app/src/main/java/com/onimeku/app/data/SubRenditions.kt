package com.onimeku.app.data

/**
 * Trek subtitle yang diumumkan playlist master HLS gudang
 * (`#EXT-X-MEDIA:TYPE=SUBTITLES`), dibaca sebagai data murni agar bisa
 * diuji unit dan dipakai dua tempat: unduhan luring per pintu dan daftar
 * trek subtitle putar daring.
 *
 * Kenapa perlu: URI rendition gudang menunjuk BERKAS subtitle mentah
 * (`.vtt`), bukan media playlist WebVTT bersegmen. Parser HLS Media3
 * menolak URI semacam itu sebagai rendition (tidak akan pernah
 * menghasilkan cue), sehingga memilih grup teks bawaan manifest =
 * subtitle tidak tampil. Trek ini karena itu dipasang sebagai
 * SubtitleConfiguration samping (jalur yang sama dengan putar luring).
 * Bila suatu saat rendition memang berupa playlist (`.m3u8`),
 * [SubRendition.playlist] menandainya agar tidak dipasang sebagai berkas
 * samping — biarkan Media3 memakai grup teks bawaannya.
 */
data class SubRendition(
    /** Atribut LANGUAGE, huruf kecil (mis. "id", "en"); kosong bila tak ada. */
    val lang: String,
    /** Atribut NAME untuk label menu (mis. "Indonesian"); bisa kosong. */
    val label: String,
    /** URL absolut (sudah diselesaikan terhadap playlist master). */
    val url: String,
    /** true = URI berupa playlist HLS (.m3u8), bukan berkas subtitle biasa. */
    val playlist: Boolean,
    /** Jalur relatif terhadap direktori playlist master; null bila di luar direktori itu. */
    val rel: String?,
) {
    /** Bisa dipasang sebagai trek samping (berkas subtitle biasa)? */
    val sideLoadable: Boolean get() = !playlist && url.isNotBlank()
}

object SubRenditions {
    private val ATTR = Regex("""([A-Z0-9-]+)=("[^"]*"|[^,]*)""")

    /** Semua atribut `KUNCI=nilai` pada satu baris tag; tanda kutip dibuang. */
    fun attrs(line: String): Map<String, String> {
        val out = HashMap<String, String>()
        for (m in ATTR.findAll(line)) {
            val v = m.groupValues[2].trim()
            out[m.groupValues[1]] = v.removeSurrounding("\"")
        }
        return out
    }

    /**
     * Baca semua rendition subtitle dari teks playlist master.
     * Urutan kemunculan dipertahankan (menu memakai urutan ini), URI
     * ganda dengan bahasa+label sama dibuang.
     */
    fun parse(masterText: String, masterUrl: String): List<SubRendition> {
        if (masterText.isBlank() || masterUrl.isBlank()) return emptyList()
        val dir = DoorMath.dirOf(masterUrl)
        val out = ArrayList<SubRendition>()
        val seen = HashSet<String>()
        for (raw in masterText.lineSequence()) {
            val line = raw.trim()
            if (!line.startsWith("#EXT-X-MEDIA:")) continue
            val a = attrs(line)
            // TYPE bisa tanpa kutip (TYPE=SUBTITLES) — attrs tetap membacanya.
            if (!a["TYPE"].equals("SUBTITLES", ignoreCase = true)) continue
            val uri = a["URI"]?.trim().orEmpty()
            if (uri.isEmpty()) continue
            val abs = DoorMath.resolve(masterUrl, uri)
            if (abs.isBlank()) continue
            val lang = a["LANGUAGE"].orEmpty().trim().lowercase()
            val label = a["NAME"].orEmpty().trim()
            val path = abs.substringBefore('?').substringBefore('#')
            val playlist = path.endsWith(".m3u8", ignoreCase = true)
            val rel = if (abs.startsWith(dir)) abs.removePrefix(dir) else null
            if (!seen.add(abs)) continue
            out += SubRendition(lang = lang, label = label, url = abs, playlist = playlist, rel = rel)
        }
        return out
    }

    /** Rendition bahasa Inggris pertama (untuk unduhan luring "en"). */
    fun firstEnglish(list: List<SubRendition>): SubRendition? =
        list.firstOrNull { it.lang.startsWith("en") }

    /** Rendition bahasa Indonesia pertama (kode lama "in" ikut dikenali). */
    fun firstIndonesian(list: List<SubRendition>): SubRendition? =
        list.firstOrNull { it.lang.startsWith("id") || it.lang.startsWith("in") }
}
