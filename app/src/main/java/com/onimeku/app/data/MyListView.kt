package com.onimeku.app.data

/** Pengurutan + penyaringan My List (murni, tanpa UI) supaya bisa diuji. */
object MyListView {
    enum class Sort { RECENT, TITLE, RATING }
    enum class Filter { ALL, AIRING, COMPLETED, NOT_STARTED }

    /** Skor numerik: pakai [Anime.score], bila kosong coba baca [Anime.rating] ("★ 8.9"), kalau gagal 0. */
    fun scoreOf(a: Anime): Double =
        a.score ?: a.rating?.replace("★", "")?.trim()?.toDoubleOrNull() ?: 0.0

    /** [items] sudah dalam urutan tersimpan (terbaru di depan). [startedIds] = id judul yang pernah mulai ditonton. */
    fun apply(items: List<Anime>, sort: Sort, filter: Filter, startedIds: Set<String>): List<Anime> {
        val f = when (filter) {
            Filter.ALL -> items
            Filter.AIRING -> items.filter { it.status == "Airing" }
            Filter.COMPLETED -> items.filter { it.status == "Completed" }
            Filter.NOT_STARTED -> items.filter { it.id !in startedIds }
        }
        return when (sort) {
            Sort.RECENT -> f
            Sort.TITLE -> f.sortedBy { it.title.lowercase() }
            Sort.RATING -> f.sortedByDescending { scoreOf(it) }
        }
    }

    /** Id judul yang pernah mulai/selesai ditonton dari kunci "id:ep". */
    fun startedIds(p: Profile): Set<String> = (p.prog.keys + p.seen).map { it.substringBeforeLast(':') }.toSet()
}
