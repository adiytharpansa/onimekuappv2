package com.onimeku.app.data

import com.onimeku.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AuthException(message: String) : Exception(message)

/**
 * Klien autentikasi Supabase (GoTrue REST) — email + kata sandi.
 * URL dan kunci publik (publishable/anon) diisi lewat gradle.properties:
 *   ONIMEKU_SUPABASE_URL, ONIMEKU_SUPABASE_KEY
 * Kunci publik memang aman ada di aplikasi; JANGAN pernah memasukkan service_role key.
 */
object AuthClient {
    private val http = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }
    private val JSON = "application/json".toMediaType()

    val configured: Boolean get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()

    private suspend fun call(path: String, body: String, bearer: String? = null): JsonObject = withContext(Dispatchers.IO) {
        if (!configured) throw AuthException("Sign in is not available yet. Please try again later.")
        val rb = Request.Builder()
            .url(BuildConfig.SUPABASE_URL.trimEnd('/') + path)
            .header("apikey", BuildConfig.SUPABASE_KEY)
            .header("Authorization", "Bearer " + (bearer ?: BuildConfig.SUPABASE_KEY))
            .post(body.toRequestBody(JSON))
            .build()
        http.newCall(rb).execute().use { r ->
            val text = r.body?.string().orEmpty()
            val obj = try { if (text.isBlank()) JsonObject(emptyMap()) else json.parseToJsonElement(text).jsonObject } catch (e: Exception) { JsonObject(emptyMap()) }
            if (!r.isSuccessful) throw AuthException(friendly(r.code, obj))
            obj
        }
    }

    private fun friendly(code: Int, o: JsonObject): String {
        val raw = listOf("error_code", "msg", "message", "error_description").firstNotNullOfOrNull { (o[it] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }.orEmpty().lowercase()
        return when {
            "invalid_credentials" in raw || "invalid login" in raw -> "Wrong email or password."
            "email_not_confirmed" in raw || "not confirmed" in raw -> "Please confirm your email first, then sign in."
            "user_already_exists" in raw || "already registered" in raw -> "This email is already registered. Try signing in."
            "weak_password" in raw || "password should" in raw -> "Password is too weak. Use at least 8 characters."
            "over_email_send_rate_limit" in raw || code == 429 -> "Too many attempts. Please wait a moment."
            "email_address_invalid" in raw || "invalid" in raw && "email" in raw -> "Enter a valid email address."
            else -> "Something went wrong (code $code). Please try again."
        }
    }

    private fun session(o: JsonObject): Account? {
        val at = o["access_token"]?.jsonPrimitive?.contentOrNull ?: return null
        val rt = o["refresh_token"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val u = o["user"]?.jsonObject
        val id = u?.get("id")?.jsonPrimitive?.contentOrNull.orEmpty()
        val em = u?.get("email")?.jsonPrimitive?.contentOrNull.orEmpty()
        return Account(id, em, at, rt)
    }

    private fun creds(email: String, password: String) =
        kotlinx.serialization.json.buildJsonObject {
            put("email", kotlinx.serialization.json.JsonPrimitive(email))
            put("password", kotlinx.serialization.json.JsonPrimitive(password))
        }.toString()

    suspend fun signIn(email: String, password: String): Account =
        session(call("/auth/v1/token?grant_type=password", creds(email, password)))
            ?: throw AuthException("Sign in failed. Please try again.")

    /** null = akun dibuat tetapi perlu konfirmasi email dulu. */
    suspend fun signUp(email: String, password: String): Account? =
        session(call("/auth/v1/signup", creds(email, password)))

    suspend fun recover(email: String) {
        call("/auth/v1/recover", kotlinx.serialization.json.buildJsonObject {
            put("email", kotlinx.serialization.json.JsonPrimitive(email))
        }.toString())
    }

    suspend fun signOut(accessToken: String) { call("/auth/v1/logout", "{}", accessToken) }
}
