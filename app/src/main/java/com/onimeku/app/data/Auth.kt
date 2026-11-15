package com.onimeku.app.data

import com.onimeku.app.BuildConfig
import com.onimeku.app.I18n
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AuthException(message: String) : Exception(message)

/**
 * Klien autentikasi Firebase Auth (REST identitytoolkit) — email + kata sandi.
 * Konfigurasi proyek Firebase khusus Onimeku diisi lewat gradle.properties:
 *   ONIMEKU_FIREBASE_PROJECT_ID, ONIMEKU_FIREBASE_API_KEY
 * Kunci API klien Firebase memang kunci publik aplikasi; aturan keamanan
 * hidup di Firebase (Auth + Firestore rules), bukan di kerahasiaan kunci ini.
 * Konfigurasi kosong = fitur akun nonaktif dengan anggun.
 */
object AuthClient {
    private val http = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }
    private val JSON = "application/json".toMediaType()

    val configured: Boolean
        get() = BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() && BuildConfig.FIREBASE_API_KEY.isNotBlank()

    private fun unavailable(): AuthException =
        AuthException(I18n.t("auth_unavailable", "Sign in is not available yet. Please try again later."))

    /** POST JSON ke identitytoolkit; mengembalikan objek respons atau melempar AuthException ramah. */
    private suspend fun call(action: String, body: String): JsonObject = withContext(Dispatchers.IO) {
        if (!configured) throw unavailable()
        val rb = Request.Builder()
            .url("https://identitytoolkit.googleapis.com/v1/accounts:$action?key=" + BuildConfig.FIREBASE_API_KEY)
            .post(body.toRequestBody(JSON))
            .build()
        http.newCall(rb).execute().use { r ->
            val text = r.body?.string().orEmpty()
            val obj = try { if (text.isBlank()) JsonObject(emptyMap()) else json.parseToJsonElement(text).jsonObject } catch (e: Exception) { JsonObject(emptyMap()) }
            if (!r.isSuccessful) throw AuthException(friendly(obj))
            obj
        }
    }

    private fun friendly(o: JsonObject): String {
        val raw = ((o["error"] as? JsonObject)?.get("message") as? kotlinx.serialization.json.JsonPrimitive)
            ?.contentOrNull.orEmpty().uppercase()
        return when {
            "INVALID_LOGIN_CREDENTIALS" in raw || "INVALID_PASSWORD" in raw || "EMAIL_NOT_FOUND" in raw ->
                I18n.t("auth_err_wrong", "Wrong email or password.")
            "USER_DISABLED" in raw ->
                I18n.t("auth_err_disabled", "This account has been disabled. Please contact support.")
            "EMAIL_EXISTS" in raw ->
                I18n.t("auth_err_exists", "This email is already registered. Try signing in.")
            "WEAK_PASSWORD" in raw ->
                I18n.t("auth_err_weak", "Password is too weak. Use at least 8 characters.")
            "TOO_MANY_ATTEMPTS" in raw || "QUOTA_EXCEEDED" in raw ->
                I18n.t("auth_err_toomany", "Too many attempts. Please wait a moment.")
            "INVALID_EMAIL" in raw ->
                I18n.t("auth_err_email", "Enter a valid email address.")
            "OPERATION_NOT_ALLOWED" in raw ->
                I18n.t("auth_unavailable", "Sign in is not available yet. Please try again later.")
            else -> I18n.t("auth_err_generic", "Something went wrong. Please try again.")
        }
    }

    private fun str(o: JsonObject?, key: String): String =
        (o?.get(key) as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull.orEmpty()

    /** Lengkapi profil akun (nama/foto/email) lewat accounts:lookup. */
    private suspend fun lookup(idToken: String): JsonObject {
        val o = call("lookup", buildJsonObject { put("idToken", idToken) }.toString())
        val users = o["users"] as? kotlinx.serialization.json.JsonArray
        return (users?.firstOrNull() as? JsonObject) ?: JsonObject(emptyMap())
    }

    private suspend fun session(
        idToken: String, refreshToken: String, fallbackUid: String, fallbackEmail: String = "",
    ): Account {
        val u = try { lookup(idToken) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { JsonObject(emptyMap()) }
        val uid = str(u, "localId").ifBlank { fallbackUid }
        if (uid.isBlank()) throw AuthException(I18n.t("auth_failed", "Sign in failed. Please try again."))
        return Account(
            userId = uid,
            email = str(u, "email").ifBlank { fallbackEmail },
            accessToken = idToken,
            refreshToken = refreshToken,
            avatarUrl = str(u, "photoUrl"),
            name = str(u, "displayName"),
        )
    }

    private fun creds(email: String, password: String) = buildJsonObject {
        put("email", email)
        put("password", password)
        put("returnSecureToken", true)
    }.toString()

    suspend fun signIn(email: String, password: String): Account {
        val o = call("signInWithPassword", creds(email, password))
        val at = str(o, "idToken")
        if (at.isBlank()) throw AuthException(I18n.t("auth_failed", "Sign in failed. Please try again."))
        return session(at, str(o, "refreshToken"), str(o, "localId"), str(o, "email").ifBlank { email })
    }

    /**
     * Buat akun. Firebase langsung memberi sesi (tidak ada gerbang
     * konfirmasi yang menghalangi masuk); email verifikasi tetap dikirim
     * sebagai usaha terbaik agar alamat terverifikasi di latar.
     */
    suspend fun signUp(email: String, password: String): Account? {
        val o = call("signUp", creds(email, password))
        val at = str(o, "idToken")
        if (at.isBlank()) return null
        try {
            call("sendOobCode", buildJsonObject {
                put("requestType", "VERIFY_EMAIL")
                put("idToken", at)
            }.toString())
        } catch (e: Exception) { /* verifikasi usaha terbaik */ }
        return session(at, str(o, "refreshToken"), str(o, "localId"), str(o, "email").ifBlank { email })
    }

    suspend fun recover(email: String) {
        call("sendOobCode", buildJsonObject {
            put("requestType", "PASSWORD_RESET")
            put("email", email)
        }.toString())
    }

    /**
     * Login sosial (Google/Discord) belum tersedia pada jalur REST murni
     * Firebase; pemanggil menampilkan pesan anggun. Mengembalikan string
     * kosong sebagai penanda tidak tersedia.
     */
    fun socialUrl(provider: String): String = ""

    /** Lengkapi akun dari token yang sudah ada (lookup profil). */
    suspend fun fromTokens(accessToken: String, refreshToken: String): Account {
        if (!configured) throw unavailable()
        val u = lookup(accessToken)
        if (str(u, "localId").isBlank()) throw AuthException(I18n.t("auth_failed", "Sign in failed. Please try again."))
        return Account(
            userId = str(u, "localId"),
            email = str(u, "email"),
            accessToken = accessToken,
            refreshToken = refreshToken,
            avatarUrl = str(u, "photoUrl"),
            name = str(u, "displayName"),
        )
    }

    /** Firebase Auth tanpa sesi server: keluar = buang token lokal (dilakukan pemanggil). */
    suspend fun signOut(accessToken: String) { /* tidak ada panggilan server */ }

    /** Tukar refresh token dengan sesi baru lewat securetoken. */
    suspend fun refresh(refreshToken: String): Account = withContext(Dispatchers.IO) {
        if (!configured) throw unavailable()
        val body = "grant_type=refresh_token&refresh_token=" +
            java.net.URLEncoder.encode(refreshToken, "UTF-8")
        val rb = Request.Builder()
            .url("https://securetoken.googleapis.com/v1/token?key=" + BuildConfig.FIREBASE_API_KEY)
            .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
        val o = http.newCall(rb).execute().use { r ->
            val text = r.body?.string().orEmpty()
            val obj = try { if (text.isBlank()) JsonObject(emptyMap()) else json.parseToJsonElement(text).jsonObject } catch (e: Exception) { JsonObject(emptyMap()) }
            if (!r.isSuccessful) throw AuthException(I18n.t("auth_expired", "Session expired. Please sign in again."))
            obj
        }
        val at = str(o, "id_token")
        if (at.isBlank()) throw AuthException(I18n.t("auth_expired", "Session expired. Please sign in again."))
        session(at, str(o, "refresh_token").ifBlank { refreshToken }, str(o, "user_id"))
    }

    /**
     * Hapus akun permanen: Firebase mengizinkan pemilik menghapus akunnya
     * sendiri dengan token sesi (accounts:delete) — tanpa kunci istimewa.
     */
    suspend fun deleteAccount(accessToken: String) {
        call("delete", buildJsonObject { put("idToken", accessToken) }.toString())
    }

    // Sesi anonim untuk membaca dokumen loker (app_keys) — bukan akun pengguna.
    private val anonLock = Mutex()
    private var anonToken: String? = null
    private var anonExpiresAt = 0L

    suspend fun anonymousIdToken(): String? = anonLock.withLock {
        val now = android.os.SystemClock.elapsedRealtime()
        anonToken?.takeIf { now < anonExpiresAt }?.let { return@withLock it }
        if (!configured) return@withLock null
        // Serialize renewal: parallel catalog requests must not create multiple anonymous users.
        try {
            val o = call("signUp", buildJsonObject { put("returnSecureToken", true) }.toString())
            val token = str(o, "idToken").takeIf { it.isNotBlank() } ?: return@withLock null
            val lifetime = str(o, "expiresIn").toLongOrNull()?.coerceIn(0L, 86400L) ?: 3600L
            anonToken = token
            // Renew one minute early; never reuse an expired cached token.
            anonExpiresAt = now + (lifetime - 60L).coerceAtLeast(0L) * 1000L
            token
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}
