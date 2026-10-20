package com.onimeku.app.data

/**
 * Logika murni seputar "pintu" gudang (URL cermin berkas yang sama di
 * banyak host) — TANPA dependensi Android agar bisa diuji unit:
 * - menurunkan direktori dasar & nama berkas dari URL,
 * - menyelesaikan URI relatif playlist HLS,
 * - kunci kanonis direktori episode gudang dari bentuk URL pintu mana pun,
 * - urutan rotasi pintu dan pembagian kerja selang-seling,
 * - penguraian playlist master/varian HLS seperlunya.
 *
 * Semua pintu gudang menyajikan ISI YANG SAMA persis di bawah direktori
 * master episode (master.m3u8, q1080/, q720/, q480/, subs/) — hanya awalan
 * host/repo yang berbeda — jadi URL berkas di satu pintu bisa dipetakan ke
 * pintu lain dengan menukar awalan direktorinya.
 */
object DoorMath {

    /** Direktori dasar sebuah URL: URL tanpa segmen nama berkas terakhir. */
    fun dirOf(url: String): String {
        val clean = url.substringBefore('?').substringBefore('#')
        val i = clean.lastIndexOf('/')
        return if (i >= 0) clean.substring(0, i + 1) else clean
    }

    /** Nama berkas dari sebuah URL (segmen terakhir, tanpa query). */
    fun fileNameOf(url: String): String {
        val clean = url.substringBefore('?').substringBefore('#')
        return clean.substringAfterLast('/')
    }

    /** Selesaikan URI relatif HLS terhadap URL playlist dasarnya. */
    fun resolve(baseUrl: String, rel: String): String {
        val r = rel.trim()
        if (r.isEmpty()) return baseUrl
        if (r.startsWith("http://") || r.startsWith("https://")) return r
        if (r.startsWith("//")) return "https:$r"
        var dir = dirOf(baseUrl)
        var rest = r
        while (rest.startsWith("../")) {
            val trimmed = dir.removeSuffix("/")
            val idx = trimmed.lastIndexOf('/')
            val schemeEnd = trimmed.indexOf("://")
            if (idx <= schemeEnd + 2) break
            dir = trimmed.substring(0, idx + 1)
            rest = rest.substring(3)
        }
        if (rest.startsWith("./")) rest = rest.substring(2)
        return dir + rest
    }

    /**
     * Kunci kanonis direktori gudang dari URL berkas pintu mana pun:
     * potongan path mulai dari segmen `<anilistId>/<episode>/` (dua segmen
     * angka pendek berurutan) sampai direktori berkasnya, mis.
     * "127230/1/q720/". Bentuk awalan tiap pintu berbeda (jsDelivr /gh/,
     * onimeku-cdn /<repo>@<commit>/, Pages /<repo>/, GitHack /<repo>/<commit>/)
     * tetapi ekor ini selalu sama untuk berkas yang sama, jadi cocok sebagai
     * kunci pengelompokan antar-pintu. Null bila pola tidak ditemukan
     * (mis. poster tingkat judul ".../127230/poster.jpg").
     */
    fun canonicalDir(url: String): String? {
        val path = try {
            java.net.URL(url).path
        } catch (e: Exception) {
            url.substringBefore('?').substringBefore('#')
        }
        val segs = path.split('/').filter { it.isNotEmpty() }
        if (segs.size < 3) return null
        for (i in 0 until segs.size - 1) {
            val a = segs[i]
            val b = segs[i + 1]
            if (a.length in 4..6 && a.all { it.isDigit() } &&
                b.length in 1..3 && b.all { it.isDigit() }
            ) {
                val tail = segs.subList(i, segs.size - 1)
                if (tail.isEmpty()) return null
                return tail.joinToString("/") + "/"
            }
        }
        return null
    }

    /** Urutan indeks percobaan: mulai dari [start], memutar tepat satu putaran penuh. */
    fun rotationOrder(size: Int, start: Int): List<Int> {
        if (size <= 0) return emptyList()
        val s = ((start % size) + size) % size
        return (0 until size).map { (s + it) % size }
    }

    /** Bagi daftar ke [workers] kelompok secara selang-seling (round-robin). */
    fun <T> splitRoundRobin(items: List<T>, workers: Int): List<List<T>> {
        if (workers <= 1) return listOf(items)
        val buckets = List(workers) { ArrayList<T>() }
        items.forEachIndexed { i, item -> buckets[i % workers].add(item) }
        return buckets.filter { it.isNotEmpty() }
    }

    // ---------------- Playlist HLS ----------------

    data class Variant(val height: Int, val uri: String)

    /** Urai varian dari teks master playlist: tinggi (dari RESOLUTION) + URI terselesaikan. */
    fun parseVariants(masterText: String, masterUrl: String): List<Variant> {
        val out = ArrayList<Variant>()
        val lines = masterText.lines().map { it.trim() }
        var pendingRes: String? = null
        for (line in lines) {
            if (line.startsWith("#EXT-X-I-FRAME-STREAM-INF")) {
                // Varian I-frame: URI-nya atribut sebaris, bukan baris
                // sesudahnya — jangan sampai mencuri URI varian asli.
                pendingRes = null
            } else if (line.startsWith("#EXT-X-STREAM-INF")) {
                pendingRes = line
            } else if (line.isNotEmpty() && !line.startsWith("#") && pendingRes != null) {
                val h = Regex("RESOLUTION=(\\d+)x(\\d+)").find(pendingRes!!)
                    ?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
                out += Variant(h, resolve(masterUrl, line))
                pendingRes = null
            }
        }
        return out
    }

    /** Urai URI segmen (baris non-komentar) dari teks varian playlist, terselesaikan penuh. */
    fun parseSegments(variantText: String, variantUrl: String): List<String> =
        variantText.lines().map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { resolve(variantUrl, it) }

    /**
     * Pilih varian untuk label kualitas pengaturan ("1080p"/"720p"/"480p";
     * "Auto"/lainnya -> 720 sebagai tangga awal yang wajar). Bila tinggi
     * persis tidak ada: tertinggi yang tidak melebihi sasaran, selain itu
     * yang terendah.
     */
    fun chooseVariant(variants: List<Variant>, qualityLabel: String): Variant? {
        if (variants.isEmpty()) return null
        val target = when (qualityLabel.trim().lowercase()) {
            "1080p" -> 1080
            "480p" -> 480
            else -> 720
        }
        val known = variants.filter { it.height > 0 }
        if (known.isEmpty()) return variants.first()
        known.firstOrNull { it.height == target }?.let { return it }
        known.filter { it.height < target }.maxByOrNull { it.height }?.let { return it }
        return known.minByOrNull { it.height }
    }
}
