package com.onimeku.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@androidx.compose.runtime.Immutable
data class Anime(
    val id: String,
    val title: String,
    val genres: List<String>,
    val rating: String?,
    val hd: Boolean,
    val eps: Int,
    val ep: Int,
    val label: Boolean,
    val synopsis: String,
    val year: Int,
    val day: Int = 0,
    val time: String = "",
    val poster: String? = null,
    val banner: String? = null,
    val available: List<Int> = emptyList(),
    val apiStatus: String? = null,
    val updatedAt: String? = null,
    val score: Double? = null,
    val anilistId: Int? = null,
    val country: String? = null,
) {
    val status: String
        get() = when (apiStatus) {
            "FINISHED" -> "Completed"
            "RELEASING" -> "Airing"
            "NOT_YET_RELEASED" -> "Upcoming"
            else -> if (ep < eps) "Airing" else "Completed"
        }
}

/**
 * Katalog aplikasi. Isinya diisi dari API Onimeku (lihat ApiClient) saat
 * aplikasi dibuka; sebelum data tiba, daftar kosong dan layar menampilkan
 * status memuat. Tidak ada data contoh ter-hardcode lagi.
 *
 * Semua daftar turunan (top, populer, per genre, dst.) dihitung SEKALI per
 * perubahan [all] lalu di-cache. Sebelumnya setiap akses (mis. byId[...] di
 * dalam loop Home) mengurutkan/menyusun ulang seluruh katalog sehingga
 * layar terasa berat saat katalog berisi ribuan judul.
 */
object Catalog {
    var all by mutableStateOf<List<Anime>>(emptyList())
    var loading by mutableStateOf(true)
    var loadFailed by mutableStateOf(false)

    /**
     * Ganti satu entri katalog dengan versi LENGKAP dari endpoint detail
     * (sinopsis utuh + nomor episode asli). Katalog daftar kini berbentuk
     * lite (?lite=1), jadi judul yang dibuka Detail-nya diperkaya lewat
     * fungsi ini. Entri yang tidak lebih lengkap tidak menimpa apa pun.
     */
    fun enrich(full: Anime) {
        val cur = all.find { it.id == full.id } ?: return
        if (full.synopsis.length <= cur.synopsis.length && full.available.isEmpty()) return
        all = all.map { if (it.id == full.id) full else it }
    }

    private val byScore = compareByDescending<Anime> { it.score ?: -1.0 }.thenBy { it.title.lowercase() }

    private fun relLabel(iso: String?): String {
        val d = runCatching { OffsetDateTime.parse(iso).toLocalDate() }.getOrNull() ?: return "Today"
        val days = ChronoUnit.DAYS.between(d, LocalDate.now())
        return when {
            days <= 0L -> "Today"
            days == 1L -> "Yesterday"
            else -> "$days days ago"
        }
    }

    /**
     * Kunci kemiripan untuk menggabungkan entri ganda (poster sama, atau judul sama
     * setelah tanda baca/spasi dibuang). Contoh: "#Compass2.0 Animation Project" dan
     * "#Compass2.0: Animation Project" memakai poster yang sama -> satu kartu saja.
     */
    internal fun dupKey(a: Anime): String =
        a.poster?.takeIf { it.isNotBlank() }?.let { posterFileKey(it) } ?: titleKey(a)

    /**
     * Identitas gambar poster: nama berkas tanpa folder/ukuran/query, sehingga gambar yang sama dari folder atau CDN
     * berbeda (mis. /medium/ vs /large/) tetap terhitung satu. Nama terlalu pendek/generik ("poster.jpg") TIDAK dipakai
     * sendirian (bisa menggabungkan judul berbeda), jadi dipakai URL utuh tanpa query.
     */
    internal fun posterFileKey(url: String): String {
        val clean = url.lowercase().substringBefore('?').substringBefore('#')
        val file = clean.substringAfterLast('/').substringBeforeLast('.')
        return if (file.length >= 10) file else clean
    }

    /** Judul ternormalisasi (huruf+angka saja; tahun tetap ikut sehingga remake beda tahun tidak ikut tergabung). */
    internal fun titleKey(a: Anime): String = a.title.lowercase().filter { it.isLetterOrDigit() }

    /** Kunci franchise: judul tanpa "Season N", "Part N", sub-judul setelah ":" dan angka akhir. */
    internal fun franchiseKey(title: String): String =
        title.lowercase()
            .substringBefore(":")
            .replace(Regex("\\b(season|part|cour|s)\\s*\\d+\\b"), " ")
            .replace(Regex("\\b(the )?(final|last)( season)?\\b"), " ")
            .filter { it.isLetterOrDigit() }
            .trimEnd { it.isDigit() }

    /** Pilih entri terbaik dari kelompok ganda: punya rating, episode terbanyak, lalu paling baru diperbarui. */
    private val bestFirst = compareByDescending<Anime> { if (it.rating != null) 1 else 0 }
        .thenByDescending { it.ep }
        .thenByDescending { it.updatedAt ?: "" }

    /** Dua tahap: (1) gambar poster yang sama, (2) judul yang sama walau URL poster berbeda ("#X Project" vs "#X: Project"). */
    internal fun dedupe(list: List<Anime>): List<Anime> = pickBest(pickBest(list) { dupKey(it) }) { titleKey(it) }

    private fun pickBest(list: List<Anime>, key: (Anime) -> String): List<Anime> {
        val best = list.groupBy(key).mapValues { (_, g) -> g.sortedWith(bestFirst).first().id }
        return list.filter { best[key(it)] == it.id }
    }

    private class Derived(val full: List<Anime>) {
        /** Semua id (termasuk entri ganda) tetap bisa dibuka: riwayat tonton / My List lama memakainya. */
        val byId: Map<String, Anime> = full.associateBy { it.id }
        /** Daftar tampilan: tanpa entri ganda. */
        val src: List<Anime> = dedupe(full)
        val popular: List<String> = src.sortedWith(byScore).map { it.id }
        val top: List<String> = popular.take(10)
        val airing: List<String> = src.filter { it.apiStatus == "RELEASING" }.sortedWith(byScore).map { it.id }
        /** Paling baru diperbarui dulu (realtime dari API) — untuk hero Beranda. */
        val freshest: List<String> = src.sortedByDescending { it.updatedAt ?: "" }.map { it.id }
        /** Sedang tayang, paling baru diperbarui dulu — untuk halaman Kalender. */
        val airingFresh: List<String> = src.filter { it.apiStatus == "RELEASING" }
            .sortedByDescending { it.updatedAt ?: "" }.map { it.id }
        val latest: List<String> = src.sortedWith(
            compareByDescending<Anime> { it.year }.thenByDescending { it.updatedAt ?: "" },
        ).map { it.id }
        val donghua: List<String> = src.filter { it.country == "CN" }.sortedWith(byScore).map { it.id }
        /**
         * Belum tayang (NOT_YET_RELEASED): tahun terdekat dulu, lalu yang paling baru diperbarui.
         * Judul yang sudah punya episode (ep > 0 / available terisi) otomatis keluar walau status API belum
         * sempat berubah; ia pindah ke "New Episode Releases" (urut updatedAt) dan slotnya diisi judul berikutnya.
         */
        val comingSoon: List<String> = src.filter { it.apiStatus == "NOT_YET_RELEASED" && it.ep <= 0 && it.available.isEmpty() }
            .sortedWith(compareBy<Anime> { if (it.year > 0) it.year else Int.MAX_VALUE }.thenByDescending { it.updatedAt ?: "" })
            .map { it.id }
        val alphabetical: List<String> = src.sortedBy { it.title.lowercase() }.map { it.id }
        private val genreCount: List<Map.Entry<String, Int>> =
            src.flatMap { it.genres }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
        val topGenres: List<String> = genreCount.take(4).map { it.key }
        val chips: List<String> = genreCount.take(10).map { it.key }
        val years: List<String> = src.map { it.year }.filter { it > 0 }.distinct().sortedDescending().take(6).map { it.toString() }
        val byGenre: Map<String, List<String>> = genreCount.associate { (g, _) ->
            g to src.filter { g in it.genres }.sortedWith(byScore).map { it.id }
        }
        val fresh: List<Triple<String, Int, String>> = src.filter { it.ep > 0 }
            .sortedByDescending { it.updatedAt ?: "" }
            .take(8)
            .map { Triple(it.id, it.ep, relLabel(it.updatedAt)) }
    }

    private var cache: Derived? = null
    private val d: Derived
        get() {
            val cur = all // membaca state: Compose ikut memantau perubahan katalog
            val c = cache
            if (c != null && c.full === cur) return c
            return Derived(cur).also { cache = it }
        }

    val byId: Map<String, Anime> get() = d.byId

    /** Seluruh katalog tanpa entri ganda (untuk daftar yang dipindai langsung, mis. Kalender). */
    val unique: List<Anime> get() = d.src

    /** Judul yang belum tayang, urut dari yang paling dekat. */
    val comingSoon: List<String> get() = d.comingSoon

    /** Sepuluh judul skor tertinggi (baris "Top Hits Anime" + hero). */
    val top: List<String> get() = d.top

    /** Seluruh katalog menurut skor tertinggi. */
    val popular: List<String> get() = d.popular

    /** Sedang tayang (status RELEASING dari API), skor tertinggi dulu. */
    val airing: List<String> get() = d.airing

    /** Seluruh katalog, paling baru diperbarui dulu (updatedAt API). */
    val freshest: List<String> get() = d.freshest

    /** Sedang tayang, paling baru diperbarui dulu (updatedAt API). */
    val airingFresh: List<String> get() = d.airingFresh

    /** Rilisan terbaru: tahun paling baru, lalu paling baru diperbarui. */
    val latest: List<String> get() = d.latest

    /** Donghua (animasi Tiongkok), barisnya sendiri di Home. */
    val donghua: List<String> get() = d.donghua

    /** Seluruh katalog urut abjad (halaman "All Anime"). */
    val alphabetical: List<String> get() = d.alphabetical

    /** Genre dengan jumlah judul terbanyak, untuk baris per genre di Home. */
    val topGenres: List<String> get() = d.topGenres

    /** Semua judul dalam satu genre, skor tertinggi dulu. */
    fun byGenre(g: String): List<String> = d.byGenre[g].orEmpty()

    /** Episode terbaru (5 teratas untuk baris Home). */
    val new: List<Triple<String, Int, String>> get() = d.fresh.take(5)

    val notifs: List<Triple<String, Int, String>> get() = d.fresh.take(6)

    val chips: List<String> get() = d.chips

    val years: List<String> get() = d.years

    val statuses = listOf("Airing", "Completed", "Upcoming")

    fun similar(id: String): List<Anime> {
        val base = byId[id] ?: return emptyList()
        val g = base.genres
        // Kemiripan: tumpang-tindih genre dulu, lalu negara/jenis yang sama
        // (donghua tidak lagi mendominasi rekomendasi anime Jepang hanya
        // karena genre-nya kebetulan sama), lalu skor tertinggi.
        val fk = franchiseKey(base.title)
        val strong = if (g.size >= 2) 2 else 1
        return d.src.filter { it.id != id && franchiseKey(it.title) != fk }
            .map { it to it.genres.count { x -> x in g } }
            .filter { it.second >= strong }
            .sortedWith(
                compareByDescending<Pair<Anime, Int>> { it.second }
                    .thenByDescending { if (it.first.country == base.country) 1 else 0 }
                    .thenByDescending { it.first.score ?: -1.0 },
            )
            .take(8).map { it.first }
    }
}
