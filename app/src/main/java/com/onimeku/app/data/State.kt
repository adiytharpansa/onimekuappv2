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
@Serializable data class Account(
    val userId: String, val email: String, val accessToken: String, val refreshToken: String,
    /** Foto profil akun (mis. foto Google/Gmail) dan nama tampilan; kosong bila akun email biasa tanpa foto. */
    val avatarUrl: String = "", val name: String = "",
)

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
    // Pengingat per EPISODE dari Kalender: "id|ep|airingAtDetik". Lokal saja, tidak disinkronkan.
    val remindEp: List<String> = emptyList(),
    // Detik menonton per hari ("yyyy-MM-dd" -> detik), untuk statistik Profil. Lokal saja, tidak disinkronkan.
    val days: Map<String, Int> = emptyMap(),
    // Cap waktu (epoch ms) perubahan terakhir tiap id (ditambah ATAU dihapus), untuk penggabungan sinkron.
    val myListTs: Map<String, Long> = emptyMap(),
    val seenTs: Map<String, Long> = emptyMap(),
)

/** Beri cap [now] pada [ids]; dibatasi 1500 entri terbaru agar tidak membengkak. */
fun Map<String, Long>.stamped(ids: Collection<String>, now: Long): Map<String, Long> {
    val m = LinkedHashMap(this)
    ids.forEach { m[it] = now }
    return if (m.size <= 1500) m
    else m.entries.sortedByDescending { it.value }.take(1500).associate { it.key to it.value }
}

@Serializable
data class Settings(
    val autoplay: Boolean = true,
    val skipIntro: Boolean = true,
    /** Lompati intro otomatis (sekali per episode) bila API memberi penanda intro. */
    val autoSkipIntro: Boolean = false,
    val wifi: Boolean = false,
    val alerts: Boolean = true,
    val quality: String = "Auto",
    val bgPlay: Boolean = true,
    // Bahasa antarmuka: "en" atau "id". Default Inggris agar
    // keadaan lama tidak berubah; dipilih di Profil -> Language.
    val lang: String = "en",
    // Bahasa subtitle pemutar: "auto" (ikut bahasa aplikasi, Indonesia/English), "id", "en", atau "off".
    val subLang: String = "auto",
    // Dubbing (trek audio) bawaan: "auto" (ikut stream) atau kode bahasa ("ja", "id", "en"). Dipakai otomatis bila episode punya trek itu.
    val dubLang: String = "auto",
    // Dubbing pilihan per judul (id anime -> kode bahasa). Menang atas [dubLang]; maks. 200 judul terakhir.
    val dubByTitle: Map<String, String> = emptyMap(),
    // Gaya subtitle pemutar: ukuran "small" | "normal" | "large" | "xl", latar hitam di belakang teks.
    val subSize: String = "normal",
    val subBg: Boolean = false,
    // Warna teks subtitle: "white" | "yellow". subSystem = ikuti gaya teks-tayang (captioning) sistem Android.
    val subColor: String = "white",
    val subSystem: Boolean = false,
    // Geseran waktu subtitle per judul (id anime -> milidetik; + = teks muncul lebih lambat). Maks. 200 judul terakhir.
    val subOffsetByTitle: Map<String, Int> = emptyMap(),
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
    // Nama profil yang sengaja dihapus/diganti nama di perangkat ini. Tanpa ini, penggabungan sinkronisasi
    // (union per nama) menghidupkan kembali profil lama dari server. Dibersihkan saat nama dipakai lagi.
    val goneProfiles: List<String> = emptyList(),
)

class Store(ctx: Context) {
    private val appCtx = ctx.applicationContext
    private val sp = appCtx.getSharedPreferences("onimeku", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // Token akun disimpan di EncryptedSharedPreferences (kunci di Android Keystore),
    // bukan di JSON state biasa. Bila Keystore gagal, jatuh ke penyimpanan lama.
    private val secure: android.content.SharedPreferences? get() = secureOf(appCtx)
    private var lastAcc: Account? = null
    private var accWritten = false

    // Cadangan penyimpanan akun HANYA bila Keystore gagal. Berkas ini dikecualikan dari backup.
    private val accPlain = appCtx.getSharedPreferences("onimeku_acc_fallback", Context.MODE_PRIVATE)

    private fun plainAcc(): Account? = try {
        accPlain.getString("account", null)?.let { json.decodeFromString<Account>(it) }
    } catch (e: Exception) { null }

    // Penulisan state: JSON di-encode + ditulis di thread latar tunggal. Sebelumnya tiap upd() (termasuk
    // simpan progres tonton berkala) meng-encode SELURUH AppState di main thread. Simpan beruntun digabung:
    // hanya state terbaru yang ditulis. Eksekutor tunggal menjaga urutan tulis.
    private val pending = java.util.concurrent.atomic.AtomicReference<AppState?>(null)
    private val io: java.util.concurrent.ExecutorService by lazy {
        java.util.concurrent.Executors.newSingleThreadExecutor { r -> Thread(r, "onimeku-store") }
    }

    companion object {
        private var secureInst: android.content.SharedPreferences? = null
        private var secureTried = false

        /** Satu instans terenkripsi per proses (inisialisasi Keystore/Tink mahal). Aman dipanggil dari thread mana pun. */
        @Synchronized
        fun secureOf(ctx: Context): android.content.SharedPreferences? {
            if (!secureTried) {
                secureInst = try {
                    val mk = androidx.security.crypto.MasterKey.Builder(ctx.applicationContext)
                        .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM).build()
                    androidx.security.crypto.EncryptedSharedPreferences.create(
                        ctx.applicationContext, "onimeku_secure", mk,
                        androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                    )
                } catch (e: Exception) { null }
                secureTried = true
            }
            return secureInst
        }
    }

    fun load(): AppState {
        val raw = sp.getString("state", null)
        val base = if (raw == null) AppState() else try {
            json.decodeFromString<AppState>(raw)
        } catch (e: Exception) {
            // Jangan hilangkan data yang gagal dibaca: simpan salinan mentah untuk pemulihan/diagnostik.
            sp.edit().putString("state_corrupt_backup", raw).apply()
            AppState()
        }
        val sec = secure
        val acc = if (sec != null) {
            try { sec.getString("account", null)?.let { json.decodeFromString<Account>(it) } } catch (e: Exception) { null }
        } else null
        // Urutan: terenkripsi -> cadangan polos (Keystore gagal) -> JSON lama (migrasi).
        return base.copy(account = acc ?: plainAcc() ?: base.account)
    }

    fun save(s: AppState) {
        // AppState tidak dapat diubah, jadi aman diserahkan ke thread latar apa adanya.
        if (pending.getAndSet(s) == null) io.execute { pending.getAndSet(null)?.let { write(it) } }
    }

    private fun write(s: AppState) {
        // Tulis token hanya saat berubah (enkripsi Keystore tidak murah; save() dipanggil tiap simpan progres).
        if (!accWritten || s.account != lastAcc) {
            val sec = secure
            val e = (sec ?: accPlain).edit()
            if (s.account != null) e.putString("account", json.encodeToString(s.account)) else e.remove("account")
            e.apply()
            // Keystore pulih: buang salinan polos supaya token tidak tersisa tanpa enkripsi.
            if (sec != null) accPlain.edit().remove("account").apply()
            lastAcc = s.account; accWritten = true
        }
        sp.edit().putString("state", json.encodeToString(s.copy(account = null))).apply()
    }
}
