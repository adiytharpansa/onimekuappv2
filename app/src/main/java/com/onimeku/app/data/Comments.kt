package com.onimeku.app.data

import com.onimeku.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Komentar per judul: tabel `comments` di Supabase yang sama
 * dengan sinkronisasi akun (skema + RLS di backend/supabase/schema.sql).
 * Baca: publik (tamu memakai kunci anon). Tulis/hapus: wajib masuk,
 * RLS memaksa user_id = auth.uid(). Semua data komentar mengalir lewat
 * backend akun milik user sendiri — bukan API katalog.
 */
@Serializable
data class CommentRow(
    val id: Long = 0,
    val anime_id: String = "",
    val user_id: String = "",
    val author: String = "",
    val body: String = "",
    val created_at: String = "",
)

object Comments {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val JSON = "application/json".toMediaType()
    private val base get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    val enabled: Boolean get() = Sync.enabled

    private fun req(path: String, bearer: String) = Request.Builder().url(base + path)
        .header("apikey", BuildConfig.SUPABASE_KEY)
        .header("Authorization", "Bearer $bearer")

    suspend fun list(animeId: String, acc: Account?): List<CommentRow> = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext emptyList()
        val bearer = acc?.accessToken ?: BuildConfig.SUPABASE_KEY
        val path = "/rest/v1/comments?anime_id=eq.${java.net.URLEncoder.encode(animeId, "UTF-8")}" +
            "&select=id,anime_id,user_id,author,body,created_at&order=created_at.desc&limit=50"
        try {
            Net.client.newCall(req(path, bearer).get().build()).execute().use { r ->
                if (!r.isSuccessful) return@withContext emptyList()
                json.decodeFromString(ListSerializer(CommentRow.serializer()), r.body?.string().orEmpty())
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun post(acc: Account, animeId: String, author: String, body: String): Boolean = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext false
        val payload = buildJsonObject {
            put("anime_id", animeId)
            put("user_id", acc.userId)
            put("author", author)
            put("body", body)
        }.toString()
        try {
            val rq = req("/rest/v1/comments", acc.accessToken)
                .header("Prefer", "return=minimal")
                .post(payload.toRequestBody(JSON)).build()
            Net.client.newCall(rq).execute().use { r -> r.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun delete(acc: Account, commentId: Long): Boolean = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext false
        try {
            val rq = req("/rest/v1/comments?id=eq.$commentId", acc.accessToken).delete().build()
            Net.client.newCall(rq).execute().use { r -> r.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }
}
