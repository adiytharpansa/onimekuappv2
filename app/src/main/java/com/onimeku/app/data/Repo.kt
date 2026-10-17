package com.onimeku.app.data

import android.content.Context
import com.onimeku.app.BuildConfig
import com.onimeku.app.media.MediaKit
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import retrofit2.http.Path

val DAYS = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")

/** Kontrak API (lihat README): GET anime  ·  GET anime/{id}/episodes/{ep}/stream -> {"url": "https://...m3u8"} */
@Serializable data class AnimeDto(val id: String, val title: String, val episodes: Int, val synopsis: String = "", val poster: String? = null,
                                  val genres: List<String> = emptyList(), val airDay: Int? = null, val airTime: String? = null)
@Serializable data class StreamDto(val url: String)
@Serializable data class CommentDto(val id: String, val userId: String, val user: String, val text: String, val timeMs: Long = 0, val mine: Boolean = false)
@Serializable data class NewComment(val text: String, val timeMs: Long)
interface Api {
    @GET("anime/{id}/episodes/{ep}/comments") suspend fun comments(@Path("id") id: String, @Path("ep") ep: Int): List<CommentDto>
    @POST("anime/{id}/episodes/{ep}/comments") suspend fun postComment(@Path("id") id: String, @Path("ep") ep: Int, @Body b: NewComment): CommentDto
    @DELETE("comments/{cid}") suspend fun deleteComment(@Path("cid") cid: String)
    @POST("comments/{cid}/report") suspend fun reportComment(@Path("cid") cid: String)
    @GET("anime") suspend fun list(): List<AnimeDto>
    @GET("anime/{id}/episodes/{ep}/stream") suspend fun stream(@Path("id") id: String, @Path("ep") ep: Int): StreamDto
}

/** Offline-first: UI selalu membaca Room; refresh() menyegarkan dari API. */
@UnstableApi
class Repo private constructor(private val c: Context) {
    /** Menambahkan token Firebase (bila login) agar server tahu siapa yang berkomentar. */
    private val client = OkHttpClient.Builder().addInterceptor { ch ->
        val tok = runCatching { if (FirebaseApp.getApps(c).isEmpty()) null else FirebaseAuth.getInstance().currentUser?.let { Tasks.await(it.getIdToken(false)).token } }.getOrNull()
        ch.proceed(if (tok != null) ch.request().newBuilder().header("Authorization", "Bearer $tok").build() else ch.request()) }.build()
    private val api: Api by lazy {
        Retrofit.Builder().baseUrl(BuildConfig.API_URL).client(client)
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType())).build().create(Api::class.java) }
    private val dao = AppDb.get(c).anime()
    val all: Flow<List<Anime>> = dao.all()
    fun one(id: String): Flow<Anime?> = dao.one(id)
    suspend fun get(id: String) = dao.get(id)
    suspend fun refresh(): Result<Unit> = runCatching {
        dao.replaceAll(api.list().map { Anime(it.id, it.title, it.episodes, it.synopsis, it.poster, it.genres.joinToString(","), it.airDay, it.airTime) }) }
    suspend fun stream(id: String, ep: Int): String = api.stream(id, ep).url
    suspend fun comments(id: String, ep: Int) = api.comments(id, ep).sortedBy { it.timeMs }
    suspend fun postComment(id: String, ep: Int, text: String, timeMs: Long) = api.postComment(id, ep, NewComment(text.trim().take(200), timeMs))
    suspend fun deleteComment(cid: String) = runCatching { api.deleteComment(cid) }.isSuccess
    suspend fun reportComment(cid: String) = runCatching { api.reportComment(cid) }.isSuccess
    /** Minta URL stream lalu antrekan unduhan. false bila gagal. */
    suspend fun download(id: String, ep: Int): Boolean = runCatching { MediaKit.enqueue(c, "$id:$ep", stream(id, ep)) }.isSuccess
    companion object { fun of(c: Context) = Repo(c.applicationContext) }
}
