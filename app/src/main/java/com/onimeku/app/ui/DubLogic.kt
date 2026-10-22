package com.onimeku.app.ui

/**
 * Logika murni dubbing (trek audio), dipisah dari Compose/Media3 supaya bisa diuji unit.
 * Kunci trek = kode bahasa 2 huruf ("ja", "id", "en", ...) atau "" bila tak dikenali.
 */
internal object DubLogic {
    private val LANG3 = mapOf(
        "jpn" to "ja", "ind" to "id", "eng" to "en", "kor" to "ko", "zho" to "zh", "chi" to "zh", "spa" to "es",
        "por" to "pt", "fra" to "fr", "fre" to "fr", "deu" to "de", "ger" to "de", "ara" to "ar", "tha" to "th",
        "vie" to "vi", "msa" to "ms", "may" to "ms", "rus" to "ru", "ita" to "it",
    )

    /** Nama bahasa yang sering dipakai sebagai label trek (bila `language` kosong). */
    private val NAMES = mapOf(
        "japanese" to "ja", "jepang" to "ja", "nihongo" to "ja",
        "indonesian" to "id", "indonesia" to "id", "bahasa indonesia" to "id",
        "english" to "en", "inggris" to "en",
        "korean" to "ko", "korea" to "ko",
        "chinese" to "zh", "mandarin" to "zh", "cina" to "zh",
        "spanish" to "es", "portuguese" to "pt", "french" to "fr", "german" to "de",
        "arabic" to "ar", "thai" to "th", "vietnamese" to "vi", "malay" to "ms", "russian" to "ru", "italian" to "it",
    )

    /**
     * Mengubah label/kode bahasa mentah menjadi kunci stabil. "jpn" -> "ja", "en-US" -> "en", "Japanese - Stereo" -> "ja",
     * "und"/kosong -> "" (tak dikenali, tidak boleh disimpan sebagai pilihan).
     */
    fun normalize(raw: String?): String {
        val s = raw.orEmpty().trim().lowercase()
        if (s.isEmpty()) return ""
        val base = s.substringBefore('-').substringBefore('_').trim()
        if (base == "und" || base == "zxx" || base == "mul") return ""
        LANG3[base]?.let { return it }
        if (base.length == 2) return base
        NAMES.entries.firstOrNull { s.startsWith(it.key) }?.let { return it.value }
        return base
    }

    /**
     * Trek yang harus dipakai saat episode dimuat: pilihan khusus judul ini lebih dulu, lalu dubbing bawaan global.
     * `null` = biarkan pilihan bawaan stream. "auto"/kosong diabaikan.
     */
    fun pick(keys: List<String>, perTitle: String?, global: String): Int? {
        for (want in listOf(perTitle, global)) {
            if (want.isNullOrBlank() || want == "auto") continue
            val i = keys.indexOf(want)
            if (i >= 0) return i
        }
        return null
    }

    /**
     * Kode dubbing yang diinginkan tetapi TIDAK dipakai pada episode ini (untuk toast "tidak tersedia · memakai ..."),
     * atau null bila tidak perlu memberi tahu. Stream hanya satu trek (judul sub-saja) tidak pernah memicu toast.
     */
    fun fallbackNotice(keys: List<String>, perTitle: String?, global: String, selected: Int): String? {
        val want = listOf(perTitle, global).firstOrNull { !it.isNullOrBlank() && it != "auto" } ?: return null
        if (keys.size < 2) return null
        return if (keys.getOrNull(selected) == want) null else want
    }

    /** Menyimpan pilihan per judul; entri terlama dibuang bila melebihi [max] (urutan sisip dipertahankan). */
    fun remember(map: Map<String, String>, id: String, key: String, max: Int = 200): Map<String, String> {
        if (id.isBlank() || key.isBlank()) return map
        val m = LinkedHashMap(map)
        m.remove(id)
        m[id] = key
        while (m.size > max) m.remove(m.keys.first())
        return m
    }

    private val GENERIC = Regex("^(audio\\s*track|audio|track|stream|sound|default)?[\\s_#-]*\\d*$", RegexOption.IGNORE_CASE)

    /** Label bawaan stream yang tak bermakna bagi pengguna ("Track 1", "Audio 2", "Stream", ...). Label kosong juga dianggap generik. */
    fun isGenericLabel(label: String?): Boolean = label.orEmpty().trim().let { it.isEmpty() || GENERIC.matches(it) }

    /** Chip/ikon dubbing tampil bila ada >1 trek, atau satu trek yang bahasanya dikenal (ditampilkan tanpa pilihan). */
    fun showPicker(keys: List<String>): Boolean = keys.size > 1 || keys.singleOrNull()?.isNotEmpty() == true

    /** Label kembar diberi nomor ("English", "English 2") supaya baris pilihan tidak membingungkan. */
    fun uniqueLabels(labels: List<String>): List<String> {
        val seen = HashMap<String, Int>()
        return labels.map { l ->
            val n = (seen[l] ?: 0) + 1
            seen[l] = n
            if (n == 1) l else "$l $n"
        }
    }

    /** Urutan baris di lembar dubbing: English, Indonesia, lalu sisanya; urutan asli dipertahankan di dalam kelompok. */
    fun sheetOrder(keys: List<String>): List<Int> =
        keys.indices.sortedBy { i -> when (keys[i]) { "en" -> 0; "id" -> 1; else -> 2 } }
}
