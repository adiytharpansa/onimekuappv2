package com.onimeku.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Komentar per judul: koleksi `comments` di Firestore proyek Firebase
 * khusus Onimeku. Baca: publik (rules mengizinkan baca tanpa masuk).
 * Tulis/hapus: wajib masuk; rules memaksa user_id = uid sesi.
 * Nomor urut komentar disimpan sebagai field `n` (dan nama dokumen)
 * agar CommentRow.id tetap Long seperti yang diharapkan UI.
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
    val enabled: Boolean get() = Sync.enabled

    private fun nowIso(): String =
        java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now())

    suspend fun list(animeId: String, acc: Account?): List<CommentRow> = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext emptyList()
        val bearer = acc?.accessToken ?: try { AuthClient.anonymousIdToken() } catch (e: Exception) { null }
        val query = org.json.JSONObject().put(
            "structuredQuery",
            org.json.JSONObject()
                .put("from", org.json.JSONArray().put(org.json.JSONObject().put("collectionId", "comments")))
                .put(
                    "where",
                    org.json.JSONObject().put(
                        "fieldFilter",
                        org.json.JSONObject()
                            .put("field", org.json.JSONObject().put("fieldPath", "anime_id"))
                            .put("op", "EQUAL")
                            .put("value", Firestore.sv(animeId)),
                    ),
                )
                .put(
                    "orderBy",
                    org.json.JSONArray().put(
                        org.json.JSONObject()
                            .put("field", org.json.JSONObject().put("fieldPath", "created_at"))
                            .put("direction", "DESCENDING"),
                    ),
                )
                .put("limit", 50),
        ).toString()
        listViaArray(animeId, bearer, query)
    }

    /** runQuery menjawab JSON array telanjang, jadi teks mentahnya dibaca langsung lewat klien bersama. */
    private suspend fun listViaArray(animeId: String, bearer: String?, query: String): List<CommentRow> = withContext(Dispatchers.IO) {
        try {
            val b = okhttp3.Request.Builder().url(Firestore.base + ":runQuery")
                .header("Accept", "application/json")
            if (!bearer.isNullOrBlank()) b.header("Authorization", "Bearer $bearer")
            b.post(query.toRequestBody("application/json".toMediaType()))
            Net.client.newCall(b.build()).execute().use { r ->
                if (!r.isSuccessful) return@withContext emptyList()
                val arr = org.json.JSONArray(r.body?.string().orEmpty())
                val out = ArrayList<CommentRow>()
                for (i in 0 until arr.length()) {
                    val doc = arr.optJSONObject(i)?.optJSONObject("document") ?: continue
                    val f = doc.optJSONObject("fields") ?: continue
                    out += CommentRow(
                        id = f.optJSONObject("n")?.optString("integerValue")?.toLongOrNull() ?: 0L,
                        anime_id = f.optJSONObject("anime_id")?.optString("stringValue").orEmpty(),
                        user_id = f.optJSONObject("user_id")?.optString("stringValue").orEmpty(),
                        author = f.optJSONObject("author")?.optString("stringValue").orEmpty(),
                        body = f.optJSONObject("body")?.optString("stringValue").orEmpty(),
                        created_at = f.optJSONObject("created_at")?.optString("timestampValue").orEmpty(),
                    )
                }
                out
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun post(acc: Account, animeId: String, author: String, body: String): Boolean = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext false
        val n = System.currentTimeMillis() * 1000 + (0..999).random()
        val payload = Firestore.doc(
            mapOf(
                "n" to Firestore.iv(n),
                "anime_id" to Firestore.sv(animeId),
                "user_id" to Firestore.sv(acc.userId),
                "author" to Firestore.sv(author.take(40)),
                "body" to Firestore.sv(body.take(1000)),
                "created_at" to Firestore.tv(nowIso()),
            ),
        )
        try {
            val c = Net.client.newBuilder().retryOnConnectionFailure(false).build() // hindari komentar ganda
            val b = okhttp3.Request.Builder().url(Firestore.base + "/comments?documentId=" + n)
                .header("Authorization", "Bearer ${acc.accessToken}")
                .post(payload.toRequestBody("application/json".toMediaType())).build()
            c.newCall(b).execute().use { r -> r.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun delete(acc: Account, commentId: Long): Boolean = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext false
        try {
            val r = Firestore.request("DELETE", "/comments/$commentId", acc.accessToken)
            r.code in 200..299
        } catch (e: Exception) {
            false
        }
    }
}
