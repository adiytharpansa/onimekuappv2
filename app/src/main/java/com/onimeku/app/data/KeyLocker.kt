package com.onimeku.app.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.onimeku.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Loker kunci katalog (cara simple-kuat, perintah user 6 Nov 2026):
 * kunci katalog 32 byte TIDAK pernah utuh di dalam aplikasi.
 *
 * - Potongan A: tersimpan di aplikasi dalam keadaan tersamar
 *   (base64 dari A XOR SHA-256(konstanta A)).
 * - Potongan B: tersimpan di Firebase proyek khusus Onimeku
 *   (Firestore, koleksi app_keys, dokumen onimeku_catalog, field part_b —
 *   juga tersamar dengan konstanta B) dan hanya bisa dibaca sesi
 *   Firebase terautentikasi (aplikasi masuk anonim). Di perangkat,
 *   potongan B yang sudah diambil disimpan terenkripsi
 *   (EncryptedSharedPreferences / Android Keystore).
 * - Kunci penuh = A XOR B, hanya hidup di ingatan proses.
 *
 * Bila Firebase belum dikonfigurasi (BuildConfig kosong) atau potongan B
 * tidak terjangkau dan tidak ada salinan terenkripsi di perangkat, fungsi
 * ini mengembalikan null dan pemanggil (ApiClient) jatuh ke cermin
 * katalog polos — aplikasi tetap jalan penuh.
 */
object KeyLocker {
    // Potongan A tersamar (diisi saat build dari berkas rahasia di luar repo).
    private const val PART_A_MASKED = "6bQOy7gueEB0csnLEN93K1eCzucE5rRjZXxXZEvOJuI="
    private const val MASK_A_CONST = "onimeku-katalog-a-v1"
    private const val MASK_B_CONST = "onimeku-katalog-b-v1"
    private const val PREFS = "keylocker"
    private const val KEY_PART_B = "part_b_masked"

    @Volatile private var appCtx: Context? = null
    @Volatile private var cachedKey: ByteArray? = null

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
    }

    private fun unmask(maskedB64: String, constWord: String): ByteArray? = try {
        val masked = Base64.decode(maskedB64.trim(), Base64.DEFAULT)
        if (masked.size != 32) null else {
            val mask = MessageDigest.getInstance("SHA-256").digest(constWord.toByteArray(Charsets.UTF_8))
            ByteArray(32) { i -> (masked[i].toInt() xor mask[i].toInt()).toByte() }
        }
    } catch (e: Exception) {
        null
    }

    private fun partA(): ByteArray? = unmask(PART_A_MASKED, MASK_A_CONST)

    // ---- salinan potongan B terenkripsi di perangkat ----
    private fun prefs(): EncryptedSharedPreferences? {
        val ctx = appCtx ?: return null
        return try {
            val master = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                ctx, PREFS, master,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            ) as EncryptedSharedPreferences
        } catch (e: Exception) {
            null
        }
    }

    private fun cachedPartB(): ByteArray? {
        // Mekanisme uji lokal (HANYA build debug): potongan B dari
        // BuildConfig uji, tidak pernah ada di build rilis yang dilaporkan.
        if (BuildConfig.DEBUG && BuildConfig.KEY_B_TEST.isNotBlank()) {
            unmask(BuildConfig.KEY_B_TEST, MASK_B_CONST)?.let { return it }
        }
        val masked = try { prefs()?.getString(KEY_PART_B, null) } catch (e: Exception) { null }
        return masked?.let { unmask(it, MASK_B_CONST) }
    }

    private fun storePartB(maskedB64: String) {
        try {
            prefs()?.edit()?.putString(KEY_PART_B, maskedB64)?.apply()
        } catch (e: Exception) { /* cache usaha terbaik */ }
    }

    /** Ambil potongan B dari Firestore (dokumen app_keys/onimeku_catalog). */
    private suspend fun fetchPartB(): ByteArray? = withContext(Dispatchers.IO) {
        if (!AuthClient.configured) return@withContext null
        val token = try { AuthClient.anonymousIdToken() } catch (e: Exception) { null }
            ?: return@withContext null
        val url = "https://firestore.googleapis.com/v1/projects/" + BuildConfig.FIREBASE_PROJECT_ID +
            "/databases/(default)/documents/app_keys/onimeku_catalog"
        try {
            val req = okhttp3.Request.Builder().url(url)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .get().build()
            Net.client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@withContext null
                val root = org.json.JSONObject(r.body?.string().orEmpty())
                val masked = root.optJSONObject("fields")?.optJSONObject("part_b")
                    ?.optString("stringValue")?.takeIf { it.isNotBlank() } ?: return@withContext null
                val b = unmask(masked, MASK_B_CONST) ?: return@withContext null
                storePartB(masked)
                b
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Kunci katalog penuh (A XOR B) atau null bila loker belum bisa dibuka. */
    suspend fun fullKey(): ByteArray? {
        cachedKey?.let { return it }
        val a = partA() ?: return null
        val b = fetchPartB() ?: cachedPartB() ?: return null
        if (a.size != 32 || b.size != 32) return null
        val k = ByteArray(32) { i -> (a[i].toInt() xor b[i].toInt()).toByte() }
        cachedKey = k
        return k
    }
}
