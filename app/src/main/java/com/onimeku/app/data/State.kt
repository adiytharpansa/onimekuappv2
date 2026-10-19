package com.onimeku.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class Dl(val id: String, val ep: Int)
@Serializable data class Last(val id: String, val ep: Int)

/** Satu catatan tontonan: judul + episode terakhir diputar, kapan (epoch ms), dan durasi episode itu (detik). */
@Serializable data class Watch(val id: String, val ep: Int, val at: Long = 0, val dur: Int = 0)

/** Sesi akun yang sedang masuk (null = tamu). */
@Serializable data class Account(val userId: String, val email: String, val accessToken: String, val refreshToken: String)

@Serializable
data class Profile(
    val name: String,
    val myList: List<String> = emptyList(),
    val prog: Map<String, Int> = emptyMap(),
    val seen: List<String> = emptyList(),
    val last: Last? = null,
    val watch: List<Watch> = emptyList(),
    val base: String? = null,
    val remind: List<String> = emptyList(),
    // Detik menonton per hari ("yyyy-MM-dd" -> detik), untuk statistik Profil. Lokal saja, tidak disinkronkan.
    val days: Map<String, Int> = emptyMap(),
)

@Serializable
data class Settings(
    val autoplay: Boolean = true,
    val skipIntro: Boolean = true,
    val wifi: Boolean = false,
    val alerts: Boolean = true,
    val quality: String = "Auto",
    val bgPlay: Boolean = true,
    // Bahasa antarmuka: "en" atau "id". Default Inggris agar
    // keadaan lama tidak berubah; dipilih di Profil -> Language.
    val lang: String = "en",
    // Bahasa subtitle pemutar: "auto" (ikut bahasa aplikasi, Indonesia/English), "id", "en", atau "off".
    val subLang: String = "auto",
    // Gaya subtitle pemutar: ukuran "small" | "normal" | "large" | "xl", latar hitam di belakang teks.
    val subSize: String = "normal",
    val subBg: Boolean = false,
    // Petunjuk gestur di halaman pemutar: tampil sampai ditutup/20 dtk menonton, lalu tidak muncul lagi.
    val gestureHintSeen: Boolean = false,
    // Picture-in-picture otomatis saat menekan Home/gestur home ketika video diputar (seperti YouTube).
    val autoPip: Boolean = true,
    // Unduh otomatis episode berikutnya saat Wi-Fi (jaringan tak terukur) ketika episode berjalan >60%.
    val autoDlNext: Boolean = false,
)

@Serializable
data class AppState(
    val welcomeSeen: Boolean = false,
    val downloads: List<Dl> = emptyList(),
    val notifRead: Boolean = false,
    val hist: List<String> = emptyList(),
    val fired: Map<String, String> = emptyMap(),
    val settings: Settings = Settings(),
    val profiles: List<Profile> = listOf(Profile("Me")),
    val pi: Int = 0,
    val account: Account? = null,
)

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("onimeku", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // Token akun disimpan di EncryptedSharedPreferences (kunci di Android Keystore),
    // bukan di JSON state biasa. Bila Keystore gagal, jatuh ke penyimpanan lama.
    private val secure: android.content.SharedPreferences? by lazy {
        try {
            val mk = androidx.security.crypto.MasterKey.Builder(ctx.applicationContext)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM).build()
            androidx.security.crypto.EncryptedSharedPreferences.create(
                ctx.applicationContext, "onimeku_secure", mk,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) { null }
    }
    private var lastAcc: Account? = null
    private var accWritten = false

    fun load(): AppState {
        val base = try {
            sp.getString("state", null)?.let { json.decodeFromString<AppState>(it) } ?: AppState()
        } catch (e: Exception) { AppState() }
        val sec = secure ?: return base
        val acc = try { sec.getString("account", null)?.let { json.decodeFromString<Account>(it) } } catch (e: Exception) { null }
        // Migrasi: akun lama yang masih di JSON biasa dipindah ke penyimpanan terenkripsi.
        return base.copy(account = acc ?: base.account)
    }

    fun save(s: AppState) {
        val sec = secure
        if (sec == null) { sp.edit().putString("state", json.encodeToString(s)).apply(); return }
        // Tulis token hanya saat berubah (enkripsi Keystore tidak murah; save() dipanggil tiap simpan progres).
        if (!accWritten || s.account != lastAcc) {
            val e = sec.edit()
            if (s.account != null) e.putString("account", json.encodeToString(s.account)) else e.remove("account")
            e.apply(); lastAcc = s.account; accWritten = true
        }
        sp.edit().putString("state", json.encodeToString(s.copy(account = null))).apply()
    }
}
