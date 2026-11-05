package com.onimeku.app.data

import com.onimeku.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Pembantu REST Firestore bersama (Comments, Sync) — tanpa SDK Firebase,
 * mengikuti gaya REST yang sudah dipakai proyek ini.
 * Dokumen: projects/<id>/databases/(default)/documents/<path>.
 */
object Firestore {
    private val JSON = "application/json".toMediaType()

    val base: String
        get() = "https://firestore.googleapis.com/v1/projects/" + BuildConfig.FIREBASE_PROJECT_ID +
            "/databases/(default)/documents"

    class Resp(val code: Int, val body: org.json.JSONObject?)

    suspend fun request(
        method: String,
        path: String,
        bearer: String?,
        jsonBody: String? = null,
    ): Resp = withContext(Dispatchers.IO) {
        val b = Request.Builder().url(base + path)
            .header("Accept", "application/json")
        if (!bearer.isNullOrBlank()) b.header("Authorization", "Bearer $bearer")
        when (method) {
            "GET" -> b.get()
            "DELETE" -> b.delete()
            "POST" -> b.post((jsonBody ?: "{}").toRequestBody(JSON))
            "PATCH" -> b.method("PATCH", (jsonBody ?: "{}").toRequestBody(JSON))
        }
        Net.client.newCall(b.build()).execute().use { r ->
            val text = r.body?.string().orEmpty()
            val obj = try { if (text.isBlank()) null else org.json.JSONObject(text) } catch (e: Exception) { null }
            Resp(r.code, obj)
        }
    }

    /** Bungkus fields Firestore dari pasangan nama -> nilai JSON mentah (sudah berbentuk {xxxValue: ...}). */
    fun doc(fields: Map<String, org.json.JSONObject>): String {
        val f = org.json.JSONObject()
        for ((k, v) in fields) f.put(k, v)
        return org.json.JSONObject().put("fields", f).toString()
    }

    fun sv(v: String) = org.json.JSONObject().put("stringValue", v)
    fun iv(v: Long) = org.json.JSONObject().put("integerValue", v.toString())
    fun tv(isoUtc: String) = org.json.JSONObject().put("timestampValue", isoUtc)

    fun fieldStr(doc: org.json.JSONObject?, name: String): String? =
        doc?.optJSONObject("fields")?.optJSONObject(name)?.optString("stringValue")?.takeIf { it.isNotBlank() }
}
