package com.onimeku.app.data

import java.text.Normalizer

/** Pencarian katalog: normalisasi teks, pencocokan per kata (urutan bebas), dan peringkat relevansi. Murni, bisa diuji. */
object SearchLogic {
    enum class Sort { RELEVANCE, RATING, NEWEST, TITLE }

    class Entry(val anime: Anime, val title: String, val squashed: String, val genres: String)

    private val DIACRITICS = Regex("\\p{Mn}+")
    private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")

    /** huruf kecil, tanpa aksen/tanda baca ("Re:Zero" -> "re zero", "Pokémon" -> "pokemon"), spasi dirapatkan. */
    fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(DIACRITICS, "").lowercase().replace(NON_WORD, " ").trim()

    fun index(all: List<Anime>): List<Entry> = all.map {
        val t = normalize(it.title)
        Entry(it, t, t.replace(" ", ""), normalize(it.genres.joinToString(" ")))
    }

    /** 0 = tidak cocok. Semua kata kueri harus cocok (di judul atau genre), urutan bebas. */
    fun score(e: Entry, query: String): Int {
        val q = normalize(query)
        if (q.isEmpty()) return 1
        var total = 0
        for (tok in q.split(' ')) {
            total += when {
                e.title.split(' ').any { it.startsWith(tok) } -> 3
                e.title.contains(tok) -> 2
                e.genres.contains(tok) -> 1
                e.squashed.contains(tok) -> 1      // "rezero" cocok dengan "Re:Zero"
                else -> return 0
            }
        }
        if (e.title == q) total += 20 else if (e.title.startsWith(q)) total += 10
        else if (e.squashed.contains(q.replace(" ", ""))) total += 4
        return total
    }

    fun search(
        index: List<Entry>, query: String, genres: List<String>, year: String, status: String, sort: Sort,
    ): List<Anime> {
        val hasQuery = normalize(query).isNotEmpty()
        val hits = index.mapNotNull { e ->
            val c = e.anime
            if (!genres.all { it in c.genres } || (year.isNotEmpty() && c.year.toString() != year) || (status.isNotEmpty() && c.status != status)) return@mapNotNull null
            val sc = score(e, query)
            if (sc == 0) null else c to sc
        }
        fun rate(a: Anime) = MyListView.scoreOf(a)
        return when (if (sort == Sort.RELEVANCE && !hasQuery) Sort.RATING else sort) {
            Sort.RELEVANCE -> hits.sortedWith(compareByDescending<Pair<Anime, Int>> { it.second }.thenByDescending { rate(it.first) })
            Sort.RATING -> hits.sortedByDescending { rate(it.first) }
            Sort.NEWEST -> hits.sortedByDescending { it.first.year }
            Sort.TITLE -> hits.sortedBy { normalize(it.first.title) }
        }.map { it.first }
    }
}
