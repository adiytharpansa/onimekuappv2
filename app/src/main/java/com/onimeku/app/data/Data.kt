package com.onimeku.app.data

import kotlinx.coroutines.delay

enum class Sort { Rating, Newest }
enum class ListStatus(val label: String) { Watching("Sedang menonton"), Plan("Rencana menonton"), Done("Selesai") }

data class Anime(
    val id: Int, val title: String, val genres: List<String>, val rating: Double, val year: Int,
    val studio: String, val synopsis: String, val poster: String = "", val banner: String = "",
    val episodes: Int = 24, val ageRating: String = "PG-13", val hd: Boolean = true
)
data class ReleaseItem(val anime: Anime, val day: Int, val time: String, val episode: Int)
data class HomeData(val featured: List<Anime>, val top: List<Anime>, val fresh: List<Anime>)

/** Ganti dengan Retrofit (AniList/Jikan/API sendiri) — UI tidak perlu diubah. */
object AnimeRepository {
    val genres = listOf("Action", "Shounen", "Fantasy", "Comedy", "Romance", "Sports", "Drama", "Adventure")
    private const val SYN = "Seorang pemuda berjuang melawan takdir demi melindungi orang yang ia sayangi dalam petualangan epik penuh aksi dan emosi yang tak terlupakan."
    private val titles = listOf("Demon Slayer: Kimetsu no Yaiba", "Attack on Titan", "Spy x Family", "Jujutsu Kaisen",
        "Bleach: Thousand-Year Blood War", "Kaguya-sama: Love is War", "Kuroko's Basketball", "My Dress-Up Darling",
        "JoJo's Bizarre Adventure", "Frieren", "Haikyu!!", "Gintama")
    private val studios = listOf("Ufotable", "MAPPA", "Wit Studio", "MAPPA", "Pierrot", "A-1 Pictures", "Production I.G",
        "CloverWorks", "David Production", "Madhouse", "Production I.G", "Sunrise")
    val all = titles.mapIndexed { i, t ->
        Anime(i, t, listOf(genres[i % 8], genres[(i + 3) % 8]), 8.4 + ((i * 7) % 9) / 10.0, 2020 + i % 5, studios[i], SYN)
    }
    val schedule = all.take(10).map { ReleaseItem(it, it.id % 7, "%02d:00".format(10 + it.id), 5 + it.id) }

    suspend fun home(): HomeData { delay(900); return HomeData(all.take(3), all.sortedByDescending { it.rating }.take(8), all.drop(4) + all.take(3)) }
    suspend fun search(q: String, genre: String, sort: Sort): List<Anime> =
        all.filter { (q.isBlank() || it.title.contains(q, true) || it.genres.any { g -> g.contains(q, true) }) && (genre == "Semua" || genre in it.genres) }
            .let { if (sort == Sort.Rating) it.sortedByDescending { a -> a.rating } else it.sortedByDescending { a -> a.year } }
    fun byId(id: Int) = all.first { it.id == id }
}
