# Onimeku — Catatan Redesain & Animasi (debug.40 – debug.45)

> Dokumen serah-terima untuk paket `Onimeku-Kotlin-Redesign.zip`.
> Tanggal: 19 Okt 2026 · Paket: `com.onimeku.app`
> **PENTING:** semua perubahan di bawah **belum dikompilasi dan belum diuji** (lingkungan penyusun tidak punya Gradle/Android SDK). Jalankan `./gradlew assembleDebug` lebih dulu; bila ada galat kompilasi, perbaiki sebelum melanjutkan.

---

## 1. Ringkasan permintaan dan hasil

| # | Permintaan | Hasil |
|---|---|---|
| 1 | Hapus ikon lingkaran merah di atas Beranda | Itu indikator tarik-untuk-segar. Indikator + pergeseran layar dihapus; penyegaran diam-diam tetap jalan |
| 2 | Tambah animasi semenarik mungkin | Hero, tombol Play, indikator slide, titik Airing, ranking, Detail, tab bar, welcome (lihat bagian 3) |
| 3 | Hapus kategori di Beranda, All Anime langsung tampil semua | Rail genre dihapus; All Anime jadi grid 3 kolom langsung |
| 4 | PiP seperti YouTube | Tombol di jendela PiP, rasio video, transisi halus, singleTask |
| 5 | Subtitle otomatis Indonesia/English | Setting Auto/Indonesia/English/Off + menu di pemutar |
| 6 | Buang latar hitam di atas, hapus ikon notif | Bilah atas dihapus total dari Beranda |

---

## 2. Daftar perubahan per versi

### debug.40 — Poles Beranda + Animasi
- Indikator tarik-untuk-segar dihapus (`Screens.kt`).
- Hero: zoom pelan (Ken Burns), scrim atas, indikator slide terisi 5,5 dtk.
- Tombol Play: kilau menyapu + panah bergoyang. Titik "Airing": denyut.
- Judul bagian: bilah merah tumbuh.

### debug.41 — Animasi lanjutan + kerapian
- Ranking Top Hits memantul berurutan; poster terangkat+miring saat ditekan.
- Detail: header memudar/mengecil saat digulir, episode muncul berurutan, percikan partikel saat masuk My List.
- Bilah progres terisi animasi (Continue Watching, thumbnail episode).
- Welcome: logo berputar-memantul lalu melayang; transisi ke Beranda memudar+zoom.
- Tab bar: penanda merah meluncur. `IconBtn` tak terpakai dihapus. "Latest Releases" lewat I18n.

### debug.42 — Beranda disederhanakan
- Rail per genre dan pita "Explore Genres" dihapus dari Beranda (kategori kini hanya di Pencarian).
- "All Anime": seluruh katalog tampil langsung sebagai grid 3 kolom (urut abjad, lazy per baris).

### debug.43 — PiP ala YouTube
- RemoteAction: Sebelumnya / Putar-Jeda / Berikutnya di jendela PiP (BroadcastReceiver `com.onimeku.app.PIP_CONTROL`).
- Rasio PiP mengikuti video; `sourceRectHint` + seamless resize untuk transisi halus.
- Auto-masuk PiP hanya saat diputar; menutup PiP (X) menjeda video (kecuali Background playback aktif).
- Manifest: `launchMode="singleTask"`, `resizeableActivity="true"`.
- Android < 12 memakai parameter yang sama lewat `vm.pipParams`.
- 4 ikon vektor baru: `ic_pip_play/pause/prev/next`.

### debug.44 — Subtitle otomatis
- `Settings.subLang`: `"auto" | "id" | "en" | "off"` (tersimpan).
- Auto mengikuti bahasa aplikasi; bahasa pilihan tidak ada → cadangan (Indonesia ↔ English → trek pertama) + toast.
- Menu subtitle di pemutar (ketuk "CC" atau tile Subtitles) menggantikan putar-siklus.
- Pencocokan trek ExoPlayer menurut bahasa, bukan indeks.
- Berkas baru: `ui/Subtitles.kt` (`SubLang`, `SubtitleDialog`).

### debug.45 — Bagian atas Beranda polos
- Bilah atas (latar solid + lonceng) dihapus. Scrim hero dilemahkan (0xB3→0x40, 120→90dp).
- Halaman Notifikasi (`Route.Notif`) masih ada di kode tetapi tidak punya pintu masuk UI.

---

## 3. Peta berkas yang diubah

| Berkas | Perubahan |
|---|---|
| `ui/Motion.kt` | `kenBurns`, `sweepShine`, `nudgeX`, `PulseDot`, `HeroDot`, `growIn`, `popIn`, `liftPress`, `sparkBurst`, `AnimatedProgressBar`, `GenreMarquee` (kini tak dipakai di Beranda) |
| `ui/Screens.kt` | Beranda (hapus indikator, rail genre, tombol All Anime→grid), hero, Detail (header, episode, spark) |
| `ui/Components.kt` | `Poster` (liftPress, popIn, progres animasi), `SectionHead` (bilah tumbuh) |
| `ui/PlayerScreen.kt` | PiP (aksi, rasio, sourceRect, receiver), subtitle (menu, pencocokan trek) |
| `ui/Subtitles.kt` | **Baru** — logika & dialog subtitle |
| `ui/ProfileScreens.kt` | Settings → baris Subtitles |
| `ui/WelcomeScreen.kt` | Logo animasi |
| `data/State.kt` | `Settings.subLang` |
| `AppVM.kt` | `pipParams` |
| `MainActivity.kt` | Tab bar berpenanda geser, transisi Welcome→Beranda, hapus TopBar & IconBtn, PiP Android <12 |
| `AndroidManifest.xml` | `singleTask`, `resizeableActivity` |
| `res/drawable/ic_pip_*.xml` | **Baru** — 4 ikon aksi PiP |
| `CHANGELOG.md` | Entri debug.40–45 |

---

## 4. Catatan perilaku

- **Reduce motion:** semua animasi baru menghormati `LocalReduceMotion` (animasi sistem dimatikan → statis).
- **Progres tonton** memakai asumsi episode 24 menit (`progSec / 1440`), sama seperti kartu episode di Detail.
- **Subtitle** hanya muncul bila API mengirim trek; pengaturan hanya memilih trek, bukan menerjemahkan.
- **Tarik-untuk-segar** tetap memanggil `vm.refresh()` tetapi tanpa indikator visual.
- **Terjemahan baru** (`sec_latest`, `sub_auto_d`, `sub_auto_hint`, `sec_explore`) masih berbahasa Inggris di `I18n.kt`.

---

## 5. Yang belum ada / perlu dilakukan

**Dari pekerjaan ini**
1. Kompilasi dan uji di HP (terutama PiP, kelancaran animasi, subtitle).
2. Tambah terjemahan Indonesia untuk string baru di `I18n.kt`.
3. Putuskan pintu masuk halaman Notifikasi (mis. dari Profil) atau hapus rutenya.
4. `GenreMarquee` di `Motion.kt` tidak lagi dipakai — hapus atau pasang di halaman Pencarian.

**Dari daftar proyek (bagian 8 panduan agen)**
1. Isi kunci Supabase (`ONIMEKU_SUPABASE_URL/KEY`) dan atur Auth.
2. Jalur hapus akun (wajib Google Play bila ada pembuatan akun) via Edge Function.
3. Simpan token dengan aman (EncryptedSharedPreferences/Keystore) + refresh token.
4. Sinkronisasi My List/progres ke akun (Supabase + RLS).
5. Bahasa Indonesia/Jepang lengkap; pindahkan string ke `strings.xml`.
6. Tinjau teks legal + URL kebijakan publik.
7. Ukur performa (Macrobenchmark) dan uji build rilis R8.
8. Selaraskan `README.md` dengan arsitektur terkini (API langsung + Supabase).

---

## 6. Cara build

```bash
./gradlew assembleDebug      # APK debug
./gradlew test               # uji unit (CoreTest)
./gradlew assembleRelease    # rilis (R8 aktif; butuh ONIMEKU_STORE_* di gradle.properties)
```

---

## 7. Lampiran — Kode Sumber Lengkap

Semua berkas sumber proyek. Berkas bertanda **[DIUBAH]** adalah yang disentuh di debug.40–45; **[BARU]** adalah berkas baru.

### `app/src/main/java/com/onimeku/app/AppVM.kt` **[DIUBAH]**

````kotlin
package com.onimeku.app

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onimeku.app.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.random.Random

sealed interface Route {
    data object Home : Route
    data object Calendar : Route
    data object MyList : Route
    data object Downloads : Route
    data object ProfileTab : Route
    data class Sub(val key: String) : Route
    data class Detail(val id: String) : Route
    data class SeeAll(val key: String) : Route
    data object Notif : Route
    data object Search : Route
    data class Player(val id: String, val ep: Int) : Route
    data class Auth(val signUp: Boolean) : Route
}

class AppVM(private val app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    var s by mutableStateOf(store.load()); private set
    var stack by mutableStateOf<List<Route>>(listOf(Route.Home)); private set
    // Keadaan fullscreen pemutar disimpan di VM (debug.31): sebelumnya di
    // PlayerReady yang dibongkar tiap pindah episode sehingga fullscreen
    // selalu lepas. Direset otomatis saat rute teratas bukan pemutar.
    var playerFull by mutableStateOf(false)
    var toastMsg by mutableStateOf<String?>(null); private set
    var welcomeOpen by mutableStateOf(!s.welcomeSeen); private set
    var calDay by mutableIntStateOf(0)
    val downloading = mutableStateMapOf<String, Int>()

    /** Posisi scroll Home disimpan di sini agar tidak kembali ke atas setelah membuka Detail lalu Back. */
    val homeScroll = LazyListState()

    /** Jarak gulir Home dalam piksel (0 di puncak; besar bila item pertama sudah lewat). Dibaca di lambda graphicsLayer/derivedStateOf, bukan di komposisi. */
    fun homeScrollPx(): Int = if (homeScroll.firstVisibleItemIndex == 0) homeScroll.firstVisibleItemScrollOffset else Int.MAX_VALUE / 2

    /** Status koneksi internet (untuk spanduk offline dan muat-ulang otomatis). */
    var online by mutableStateOf(true); private set
    private var netCb: ConnectivityManager.NetworkCallback? = null

    private fun watchNetwork() {
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return
        fun now() = cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        online = now()
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                viewModelScope.launch {
                    val was = !online
                    online = true
                    if (was && (Catalog.all.isEmpty() || Catalog.loadFailed)) reloadCatalog()
                }
            }
            override fun onLost(network: Network) { viewModelScope.launch { online = now() } }
        }
        try { cm.registerDefaultNetworkCallback(cb); netCb = cb } catch (_: Exception) {}
    }

    override fun onCleared() {
        netCb?.let { try { app.getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(it) } catch (_: Exception) {} }
        super.onCleared()
    }

    /** True saat pull-to-refresh sedang berjalan. */
    var refreshing by mutableStateOf(false); private set
    fun refresh() { refreshing = true; reloadCatalog() }

    // Kesegaran katalog (debug.33): kapan terakhir data katalog tiba dari API
    // + penanda muat berjalan, agar penyegaran otomatis tidak menumpuk.
    @Volatile private var lastFetchAt = 0L
    @Volatile private var catalogBusy = false

    /** Segarkan katalog DIAM-DIAM (tanpa indikator tarik) bila data yang tampil
     *  sudah lebih tua dari [maxAgeMs]. Dipanggil saat aplikasi kembali ke depan,
     *  saat kembali ke tab Beranda, dan berkala selama aplikasi terbuka — supaya
     *  anime/episode baru dari API langsung tampil tanpa menunggu mulai dingin. */
    fun refreshIfStale(maxAgeMs: Long = 5 * 60_000L) {
        if (Catalog.all.isEmpty() || catalogBusy || refreshing) return
        if (System.currentTimeMillis() - lastFetchAt > maxAgeMs) reloadCatalog()
    }

    /** Jadwal tayang asli dari API: id katalog -> episode berikutnya + waktu. */
    var schedule by mutableStateOf<Map<String, ApiScheduleItem>>(emptyMap())
        private set
    var inPip by mutableStateOf(false)
    /** Parameter PiP terbaru dari pemutar (rasio, aksi, sourceRect) — dipakai MainActivity untuk Android < 12. */
    var pipParams: android.app.PictureInPictureParams? = null
    var playing by mutableStateOf(false)

    init {
        Downloads.oneTimeResetV13(app)
        Downloads.oneTimeCleanupV23(app)
        // Pembersihan sekali (debug.31, perintah user: semua data harus
        // realtime dari API): snapshot katalog disk + cache gambar lama
        // dibuang agar buka pertama build ini 100% data API live —
        // tidak ada poster era resolusi-lama yang tersaji dari cache.
        try {
            val flags = app.getSharedPreferences("onimeku_flags", android.content.Context.MODE_PRIVATE)
            if (!flags.getBoolean("purge_v31", false)) {
                java.io.File(app.filesDir, "catalog_cache_direct.json").delete()
                try { coil.Coil.imageLoader(app).diskCache?.clear() } catch (e: Exception) { /* abaikan */ }
                flags.edit().putBoolean("purge_v31", true).apply()
            }
        } catch (e: Exception) { /* abaikan */ }
        PosterHealer.init(app)
        Reminders.rescheduleAll(app, s)
        initDownloads()
        watchNetwork()
        reloadCatalog()
        pullAndMerge()
    }

    /** Sambungkan DownloadManager Media3 ke state aplikasi (progres asli). */
    private fun initDownloads() {
        val dm = Downloads.manager(app)
        Downloads.setWifiOnly(app, s.settings.wifi)
        // Pulihkan tampilan untuk unduhan dari sesi sebelumnya.
        for ((contentKey, d) in Downloads.states(app)) {
            val (id, ep) = Downloads.parseKey(contentKey) ?: continue
            val k = "$id:$ep"
            when (d.state) {
                androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING,
                androidx.media3.exoplayer.offline.Download.STATE_QUEUED,
                androidx.media3.exoplayer.offline.Download.STATE_RESTARTING ->
                    downloading[k] = pctOf(d)
                androidx.media3.exoplayer.offline.Download.STATE_COMPLETED ->
                    if (!hasDl(id, ep)) upd { it.copy(downloads = it.downloads + Dl(id, ep)) }
            }
        }
        dm.addListener(object : androidx.media3.exoplayer.offline.DownloadManager.Listener {
            override fun onDownloadChanged(
                dm: androidx.media3.exoplayer.offline.DownloadManager,
                download: androidx.media3.exoplayer.offline.Download,
                finalException: Exception?,
            ) {
                val (id, ep) = Downloads.parseKey(download.request.id) ?: return
                val k = "$id:$ep"
                when (download.state) {
                    androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING,
                    androidx.media3.exoplayer.offline.Download.STATE_QUEUED,
                    androidx.media3.exoplayer.offline.Download.STATE_RESTARTING ->
                        downloading[k] = pctOf(download)
                    androidx.media3.exoplayer.offline.Download.STATE_COMPLETED -> {
                        downloading.remove(k)
                        if (!hasDl(id, ep)) upd { it.copy(downloads = it.downloads + Dl(id, ep)) }
                        toast("Episode $ep saved for offline watching")
                    }
                    androidx.media3.exoplayer.offline.Download.STATE_FAILED -> {
                        downloading.remove(k)
                        if (hasDl(id, ep)) {
                            upd { st -> st.copy(downloads = st.downloads.filterNot { it.id == id && it.ep == ep }) }
                            toast("Download failed: Episode $ep")
                        }
                    }
                    androidx.media3.exoplayer.offline.Download.STATE_REMOVING -> downloading.remove(k)
                }
            }

            override fun onDownloadRemoved(
                dm: androidx.media3.exoplayer.offline.DownloadManager,
                download: androidx.media3.exoplayer.offline.Download,
            ) {
                val (id, ep) = Downloads.parseKey(download.request.id) ?: return
                downloading.remove("$id:$ep")
            }
        })
    }

    private fun pctOf(d: androidx.media3.exoplayer.offline.Download): Int =
        d.percentDownloaded.toInt().coerceIn(0, 99)

    /** Muat katalog dari API Onimeku; dipanggil saat aplikasi dibuka dan dari tombol coba lagi. */
    fun reloadCatalog() {
        // Tampilkan dulu katalog terakhir dari disk (seketika), jaringan
        // menyegarkannya di latar — katalog tidak boleh kosong hanya karena
        // satu permintaan jaringan goyah.
        Catalog.loading = Catalog.all.isEmpty()
        Catalog.loadFailed = false
        if (Catalog.all.isEmpty()) {
            // Baca + parse cache disk di thread latar: tidak menahan frame pertama.
            viewModelScope.launch {
                val cached = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    ApiClient.loadCachedCatalog(java.io.File(app.filesDir, "catalog_cache_direct.json"))
                }
                if (cached != null && Catalog.all.isEmpty()) { Catalog.all = cached; Catalog.loading = false }
            }
        }
        viewModelScope.launch {
            try {
                schedule = ApiClient.fetchSchedule().associateBy { it.id }
            } catch (e: Exception) {
                // Jadwal asli gagal dimuat: Calendar memakai cadangan lama.
            }
        }
        catalogBusy = true
        viewModelScope.launch {
            try {
                val items = ApiClient.fetchCatalog(
                    onProgress = { part ->
                        // Jangan menyusutkan daftar saat refresh: halaman awal lebih pendek dari katalog yang sudah tampil.
                        if (part.isNotEmpty() && (Catalog.all.isEmpty() || part.size >= Catalog.all.size)) {
                            Catalog.all = part
                            Catalog.loading = false
                        }
                    },
                    onRaw = { raw ->
                        // Tulis ke disk di thread IO (bukan main).
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            ApiClient.saveCachedCatalog(java.io.File(app.filesDir, "catalog_cache_direct.json"), raw)
                        }
                    },
                )
                if (items.isNotEmpty()) {
                    Catalog.all = items
                    Catalog.loadFailed = false
                    lastFetchAt = System.currentTimeMillis()
                } else {
                    Catalog.loadFailed = Catalog.all.isEmpty()
                }
            } catch (e: Exception) {
                Catalog.loadFailed = Catalog.all.isEmpty()
            } finally {
                Catalog.loading = false
                refreshing = false
                catalogBusy = false
                Reminders.rescheduleAll(app, s)
            }
        }
    }

    private val jobs = HashMap<String, Job>()
    private var toastJob: Job? = null

    val route: Route get() = stack.last()
    val tab: Route get() = stack.first()
    val p: Profile get() = s.profiles[s.pi]

    // ---------- navigation ----------
    /** Arah transisi terakhir: 1 = masuk (push), -1 = kembali (pop), 0 = sejajar (tab / ganti episode). Dibaca oleh AnimatedContent. */
    var navDir = 0
    fun go(r: Route) { navDir = 1; stack = stack + r; afterNav() }
    fun back(animated: Boolean = true) { navDir = if (animated) -1 else 0; stack = if (stack.size > 1) stack.dropLast(1) else listOf(Route.Home); afterNav() }
    fun setTab(r: Route) { navDir = 0; stack = listOf(r); afterNav(); if (r == Route.Home) refreshIfStale() }
    fun replaceTop(r: Route) { navDir = 0; stack = stack.dropLast(1) + r; afterNav() }
    private fun afterNav() { if (stack.lastOrNull() !is Route.Player) playerFull = false }
    fun openSettings() { stack = listOf(Route.ProfileTab, Route.Sub("settings")) }

    fun toast(m: String) {
        toastMsg = m
        toastJob?.cancel()
        toastJob = viewModelScope.launch { delay(1800); toastMsg = null }
    }

    // ---------- state helpers ----------
    private fun upd(f: (AppState) -> AppState) { s = f(s); store.save(s); scheduleSync() }

    // ---------- sinkronisasi akun ----------
    private var syncJob: Job? = null
    private fun scheduleSync() {
        if (s.account == null || !Sync.enabled) return
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            delay(15_000)
            try { withAccount { Sync.push(it, s.profiles) } } catch (_: Exception) {}
        }
    }

    /** Jalankan [block]; bila token kedaluwarsa (401) tukar refresh token sekali lalu ulangi. */
    private suspend fun <T> withAccount(block: suspend (Account) -> T): T? {
        var acc = s.account ?: return null
        return try {
            block(acc)
        } catch (e: Sync.Unauthorized) {
            acc = AuthClient.refresh(acc.refreshToken)
            s = s.copy(account = acc); store.save(s)
            block(acc)
        }
    }

    fun pullAndMerge() {
        if (s.account == null || !Sync.enabled) return
        viewModelScope.launch {
            try {
                val remote = withAccount { Sync.pull(it) } ?: return@launch
                upd { st -> st.copy(profiles = Sync.merge(st.profiles, remote)) }
            } catch (_: Exception) {}
        }
    }
    private fun updP(f: (Profile) -> Profile) =
        upd { st -> st.copy(profiles = st.profiles.mapIndexed { i, pr -> if (i == st.pi) f(pr) else pr }) }

    fun toggleML(id: String) {
        val on = id in p.myList
        updP { it.copy(myList = if (on) it.myList - id else listOf(id) + it.myList) }
        toast(if (on) "Removed from My List" else "Added to My List")
    }

    fun toggleRemind(id: String, ensure: ((Boolean) -> Unit) -> Unit) {
        val on = id in p.remind
        updP { it.copy(remind = if (on) it.remind - id else it.remind + id) }
        if (on) Reminders.cancel(app, s.pi, id) else Reminders.schedule(app, s.pi, id)
        if (on) toast("Reminder removed") else {
            toast("We will remind you")
            val c = Catalog.byId[id] ?: return
            ensure { g -> if (g) Notifier.show(app, "Reminder set", "${c.title} airs at ${c.time}") }
        }
    }

    fun hasDl(id: String, ep: Int) = s.downloads.any { it.id == id && it.ep == ep }

    /**
     * Unduhan ASLI (Media3): ketuk mulai, ketuk lagi saat berjalan = batal.
     * Hanya judul dengan stream HLS langsung yang bisa diunduh; judul
     * embed-saja ditolak dengan jujur.
     */
    fun download(id: String, ep: Int) {
        val k = "$id:$ep"
        val contentKey = Downloads.key(id, ep)
        val st = Downloads.states(app)[contentKey]?.state
        if (st == androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING ||
            st == androidx.media3.exoplayer.offline.Download.STATE_QUEUED ||
            st == androidx.media3.exoplayer.offline.Download.STATE_RESTARTING
        ) {
            Downloads.remove(app, contentKey)
            downloading.remove(k)
            upd { s2 -> s2.copy(downloads = s2.downloads.filterNot { it.id == id && it.ep == ep }) }
            toast("Download cancelled")
            return
        }
        if (hasDl(id, ep)) { toast("Already saved offline"); return }
        val free = try { android.os.StatFs(app.filesDir.path).availableBytes } catch (e: Exception) { Long.MAX_VALUE }
        if (free < 300L * 1024 * 1024) { toast("Not enough storage. Free up some space and try again."); return }
        viewModelScope.launch { startDownload(id, ep) }
    }

    /** Isi unduhan (ambil data putar + antrekan). Dipisah dari download()
     *  agar downloadAll bisa menjalankannya BERURUTAN (debug.31). */
    private suspend fun startDownload(id: String, ep: Int) {
        val k = "$id:$ep"
        val pb = try { ApiClient.fetchPlayback(id, ep, fresh = true) } catch (e: Exception) { null }
        val url = pb?.streamUrl
        if (pb == null || url.isNullOrBlank() || !url.contains(".m3u8")) {
            toast("This title can't be downloaded")
            return
        }
        if (!hasDl(id, ep)) upd { it.copy(downloads = it.downloads + Dl(id, ep)) }
        downloading[k] = 0
        Downloads.enqueue(app, id, ep, url, pb.referer) {
            downloading.remove(k)
            upd { s2 -> s2.copy(downloads = s2.downloads.filterNot { it.id == id && it.ep == ep }) }
            toast("Download failed to start: Episode $ep")
        }
        toast("Download started: Episode $ep")
    }

    fun downloadAll(id: String) {
        val a = Catalog.byId[id] ?: return
        val nums = if (a.available.isNotEmpty()) a.available else (1..a.eps).toList()
        val todo = nums.filter { !hasDl(id, it) }
        if (todo.isEmpty()) { toast("All episodes already saved"); return }
        toast("Starting ${todo.size} downloads")
        // Berurutan (debug.31): satu pengambilan data putar pada satu
        // waktu. Sebelumnya semua episode menembak fetchPlayback paralel —
        // untuk judul ratusan episode itu membanjiri jaringan dan hulu.
        viewModelScope.launch { for (n in todo) startDownload(id, n) }
    }

    fun removeDl(id: String, ep: Int) {
        Downloads.remove(app, Downloads.key(id, ep))
        downloading.remove("$id:$ep")
        upd { st -> st.copy(downloads = st.downloads.filterNot { it.id == id && it.ep == ep }) }
        toast("Download removed")
    }

    fun removeAllDl() {
        for (d in s.downloads) Downloads.remove(app, Downloads.key(d.id, d.ep))
        downloading.clear()
        upd { it.copy(downloads = emptyList()) }
        toast("All downloads removed")
    }

    // ---- Komentar per judul (debug.31; backend Supabase, data/Comments.kt) ----
    var comments by mutableStateOf<List<com.onimeku.app.data.CommentRow>>(emptyList()); private set
    var commentsBusy by mutableStateOf(false); private set
    fun loadComments(id: String) {
        viewModelScope.launch {
            commentsBusy = true
            comments = com.onimeku.app.data.Comments.list(id, s.account)
            commentsBusy = false
        }
    }
    fun postComment(id: String, body: String) {
        val acc = s.account ?: return
        val text = body.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            if (com.onimeku.app.data.Comments.post(acc, id, p.name, text)) loadComments(id)
            else toast("Comment failed to send")
        }
    }
    fun deleteComment(id: String, commentId: Long) {
        val acc = s.account ?: return
        viewModelScope.launch {
            if (com.onimeku.app.data.Comments.delete(acc, commentId)) loadComments(id)
        }
    }

    fun saveProg(id: String, ep: Int, t: Int, dur: Int) {
        val k = "$id:$ep"
        updP { pr ->
            var base = pr.base; var prog = pr.prog; var seen = pr.seen; var last = pr.last
            if (t >= 5) base = id
            if (t >= dur * 0.9) { prog = prog - k; if (k !in seen) seen = seen + k }
            else if (t >= 5) { prog = prog + (k to t); last = Last(id, ep) }
            pr.copy(base = base, prog = prog, seen = seen, last = last)
        }
    }

    fun addHist(v: String) {
        val q = v.trim(); if (q.length < 2) return
        upd { it.copy(hist = (listOf(q) + it.hist.filter { x -> x != q }).take(6)) }
    }
    fun clearHist() = upd { it.copy(hist = emptyList()) }

    fun markAllRead() = upd { it.copy(notifRead = true) }

    fun setSettings(f: (Settings) -> Settings) {
        val old = s.settings.alerts
        upd { it.copy(settings = f(it.settings)) }
        if (old != s.settings.alerts) Reminders.rescheduleAll(app, s)
        Downloads.setWifiOnly(app, s.settings.wifi)
    }

    // ---------- profiles ----------
    fun switchProfile(i: Int) { upd { it.copy(pi = i) }; toast("Switched to ${s.profiles[i].name}"); setTab(Route.ProfileTab) }
    fun addProfile(name: String) = upd { it.copy(profiles = it.profiles + Profile(name)) }
    fun delProfile(i: Int) {
        upd { st ->
            st.copy(profiles = st.profiles.filterIndexed { k, _ -> k != i }, pi = if (i < st.pi) st.pi - 1 else st.pi)
        }
        Reminders.rescheduleAll(app, s)
    }

    // ---------- akun ----------
    var authBusy by mutableStateOf(false); private set
    var authError by mutableStateOf<String?>(null)

    private fun runAuth(block: suspend () -> Unit) {
        if (authBusy) return
        authBusy = true; authError = null
        viewModelScope.launch {
            try { block() }
            catch (e: AuthException) { authError = e.message }
            catch (e: Exception) { authError = "Network problem. Check your connection and try again." }
            finally { authBusy = false }
        }
    }

    fun signIn(email: String, password: String) = runAuth {
        val acc = AuthClient.signIn(email.trim(), password)
        upd { it.copy(account = acc) }
        pullAndMerge()
        toast("Welcome back")
        setTab(Route.ProfileTab)
    }

    fun signUp(email: String, password: String) = runAuth {
        val acc = AuthClient.signUp(email.trim(), password)
        if (acc == null) {
            toast("Check your email to confirm your account")
            replaceTop(Route.Auth(false))
        } else {
            upd { it.copy(account = acc) }
            toast("Account created")
            setTab(Route.ProfileTab)
        }
    }

    fun resetPassword(email: String) = runAuth {
        AuthClient.recover(email.trim())
        toast("Reset link sent if the email exists")
    }

    fun signOut() {
        val acc = s.account ?: return
        upd { it.copy(account = null) }
        toast("Signed out")
        viewModelScope.launch { try { AuthClient.signOut(acc.accessToken) } catch (_: Exception) {} }
    }

    /** Hapus akun permanen (wajib Google Play). Token disegarkan dulu agar tidak kedaluwarsa. */
    fun deleteAccount() = runAuth {
        val acc = s.account ?: return@runAuth
        val fresh = AuthClient.refresh(acc.refreshToken)
        upd { it.copy(account = fresh) }
        AuthClient.deleteAccount(fresh.accessToken)
        syncJob?.cancel()
        upd { it.copy(account = null) }
        toast("Account deleted")
    }

    // ---------- welcome ----------
    fun openWelcome() { welcomeOpen = true }
    fun closeWelcome() { upd { it.copy(welcomeSeen = true) }; welcomeOpen = false; setTab(Route.Home) }
}
````

### `app/src/main/java/com/onimeku/app/I18n.kt`

````kotlin
package com.onimeku.app

/**
 * Lokalisasi antarmuka (debug.31). Bahasa aktif di [lang] ("en"/"id"),
 * disinkronkan dari Settings di App(). Pemanggil memakai
 * `I18n.t("kunci", "English fallback")`: teks Inggris tetap tertulis di
 * tempat pemakaian sebagai sumber & cadangan, terjemahan Indonesia
 * dipusatkan di sini. Kunci yang belum diterjemahkan jatuh ke Inggris.
 * Isi data (judul, sinopsis dari API) tidak diterjemahkan.
 */
object I18n {
    @Volatile var lang: String = "en"

    fun t(key: String, en: String): String =
        if (lang == "id") ID[key] ?: en else en

    private val ID = mapOf(
        // Navigasi & umum
        "tab_home" to "Beranda",
        "tab_calendar" to "Kalender",
        "tab_mylist" to "Daftarku",
        "tab_download" to "Unduhan",
        "tab_search" to "Cari",
        "synopsis" to "Sinopsis",
        "start_with" to "Mulai dari Episode",
        "continue_from" to "Lanjut dari",
        "up_next" to "Selanjutnya",
        "play_now" to "Putar sekarang",
        "next_episode" to "Episode berikutnya",
        "previous" to "Sebelumnya",
        "now_watching" to "Sedang ditonton",
        "watched" to "Sudah ditonton",
        "resume_at" to "Lanjut di",
        "not_watched" to "Belum ditonton",
        "current" to "Saat ini",
        "st_airing" to "Tayang",
        "st_completed" to "Tamat",
        "ctl_speed" to "Kecepatan",
        "ctl_sleep" to "Tidur",
        "ctl_lock" to "Kunci",
        "ctl_off" to "Mati",
        "ctl_screen" to "Layar",
        "swipe_hint" to "Geser kiri: kecerahan · kanan: volume · ketuk dua kali di sisi: ±10 dtk",
        "notifications" to "Notifikasi",
        "mark_all_read" to "Tandai semua dibaca",
        "dl_profile_empty" to "Tonton tanpa internet",
        "dl_saved" to "episode tersimpan",
        "tab_profile" to "Profil",
        "see_all" to "Lihat semua",
        "try_again" to "Coba lagi",
        "search_hint" to "Cari anime…",
        "home" to "Beranda",
        "catalog_unavail_t" to "Katalog tidak tersedia",
        "catalog_unavail_b" to "Katalog tidak dapat dimuat. Periksa koneksimu, lalu coba lagi.",
        "calendar_t" to "Kalender Rilis",
        "calendar_sub" to "Episode yang tayang minggu ini.",
        "nothing_airing_t" to "Tidak ada yang tayang",
        "nothing_airing_b" to "Tidak ada episode baru yang dijadwalkan untuk hari ini.",
        "mylist_empty_t" to "Daftarmu kosong",
        "mylist_empty_b" to "Ketuk Daftarku pada anime mana pun untuk menyimpannya di sini.",
        "no_results_t" to "Tidak ada hasil",
        "no_results_b" to "Tidak ada yang cocok. Coba kurangi filter atau periksa ejaannya.",

        "episodes" to "Episode",
        "episode" to "Episode",
        "comments" to "Komentar",
        "added" to "Ditambahkan",
        "reminder_on" to "Pengingat aktif",
        "search" to "Cari",
        "dl_none_t" to "Belum ada unduhan",
        "dl_none_b" to "Buka anime lalu ketuk ikon unduh pada sebuah episode untuk menontonnya luring.",
        "browse_anime" to "Jelajahi anime",
        "search_hint2" to "Cari anime atau genre",
        // Beranda
        "sec_continue" to "Lanjut Menonton",
        "sec_because" to "Karena kamu menonton",
        "sec_top" to "Anime Terpopuler",
        "sec_new_eps" to "Episode Baru Rilis",
        "sec_airing" to "Sedang Tayang",
        "sec_popular" to "Populer",
        "sec_latest" to "Terbaru",
        "sec_donghua" to "Donghua",
        "sec_all" to "Semua Anime",
        "browse_all" to "Jelajahi semua",
        "play" to "Putar",
        "my_list" to "Daftarku",
        // Detail
        "share" to "Bagikan",
        "download_all" to "Unduh semua",
        "read_more" to "Baca selengkapnya",
        "read_less" to "Tutup",
        "resume" to "Lanjut Ep",
        "sign_in_to_comment" to "Masuk untuk berkomentar",
        "comment_hint" to "Tulis komentar…",
        "comment_post" to "Kirim",
        "comment_empty" to "Belum ada komentar. Jadilah yang pertama.",
        "comment_delete" to "Hapus",
        // Pemutar
        "loading_stream" to "Memuat stream…",
        "stream_unavailable" to "Stream episode ini sedang tidak tersedia.",
        "subtitles" to "Subtitle",
        "quality" to "Kualitas",
        "skip_intro" to "Lewati intro",
        "speed" to "Kecepatan",
        "wifi_only_title" to "Mode Wi-Fi saja aktif",
        "wifi_only_body" to "Streaming lewat data seluler dimatikan di Pengaturan. Sambungkan ke Wi-Fi atau matikan opsi itu.",
        "open_settings" to "Buka Pengaturan",
        // Kalender
        "remind_me" to "Ingatkan aku",
        "reminded" to "Diingatkan",
        "no_releases" to "Tidak ada rilis pada hari ini.",
        // Unduhan
        "downloads_empty" to "Belum ada unduhan. Simpan episode saat daring agar bisa ditonton luring.",
        "remove_all" to "Hapus semua",
        // Profil & pengaturan
        "settings" to "Pengaturan",
        "language" to "Bahasa",
        "help" to "Pusat Bantuan",
        "about" to "Tentang Onimeku",
        "choose_language" to "Pilih bahasa yang dipakai aplikasi.",
        "coming_soon" to "Segera",
        "autoplay" to "Putar episode berikutnya otomatis",
        "skip_intro_s" to "Lewati intro otomatis",
        "wifi_only" to "Streaming hanya via Wi-Fi",
        "alerts" to "Notifikasi episode & pengingat",
        "bg_play" to "Putar di latar belakang",
        "quality_s" to "Kualitas video",
        "sign_in" to "Masuk",
        "sign_up" to "Daftar",
        "sign_out" to "Keluar",
        "grp_playback" to "Pemutaran",
        "autoplay_t" to "Putar episode berikutnya otomatis",
        "autoplay_d" to "Mulai episode berikutnya saat satu episode selesai.",
        "skip_intro_t" to "Lewati intro",
        "skip_intro_d" to "Tampilkan tombol untuk melewati lagu pembuka.",
        "bg_play_t" to "Putar di latar belakang",
        "bg_play_d" to "Audio tetap berjalan dengan kontrol layar kunci saat kamu keluar aplikasi.",
        "grp_data" to "Data dan notifikasi",
        "wifi_only_t" to "Streaming hanya via Wi-Fi",
        "wifi_only_d" to "Hindari pemakaian data seluler.",
        "alerts_t" to "Notifikasi episode baru",
        "alerts_d" to "Dapatkan notifikasi saat daftarmu diperbarui.",
        "terms" to "Ketentuan Layanan",
        "privacy" to "Kebijakan Privasi",
        "licenses" to "Lisensi",
        // Sambutan & akun
        "welcome_skip" to "Lewati",
        "skip_for_now" to "Lewati dulu",
        "welcome_back" to "Selamat datang kembali",
        "create_account_t" to "Buat akunmu",
        "create_account_b" to "Buat akun",
        "please_wait" to "Mohon tunggu…",
        "welcome_get_started" to "Mulai",
        "email" to "Email",
        "password" to "Kata sandi",
    )
}
````

### `app/src/main/java/com/onimeku/app/MainActivity.kt` **[DIUBAH]**

````kotlin
package com.onimeku.app

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.onimeku.app.ui.LocalReduceMotion
import com.onimeku.app.ui.LocalSharedScope
import com.onimeku.app.ui.LocalAnimScope
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onimeku.app.ui.*
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.util.Rational
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val vm: AppVM by viewModels()
    private var permCb: ((Boolean) -> Unit)? = null
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { g ->
        permCb?.invoke(g); permCb = null
    }

    private fun ensureNotif(cb: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) cb(true)
        else { permCb = cb; permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleLink(intent)
    }

    /** onimeku://anime/<id> -> halaman Detail (bila judul ada di katalog). */
    private fun handleLink(i: android.content.Intent?) {
        val d = i?.data ?: return
        if (d.scheme != "onimeku" || d.host != "anime") return
        val id = d.pathSegments.firstOrNull() ?: return
        vm.closeWelcome()
        vm.go(Route.Detail(id))
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Android 12+ memakai auto-enter (diatur di PlayerScreen); di bawahnya masuk PiP manual di sini.
        if (Build.VERSION.SDK_INT < 31 && vm.route is Route.Player && vm.playing) {
            try { enterPictureInPictureMode(vm.pipParams ?: PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()) } catch (_: Exception) {}
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        vm.inPip = isInPictureInPictureMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            OnimekuTheme {
                CompositionLocalProvider(LocalEnsureNotif provides { cb -> ensureNotif(cb) }) {
                    val appVm: AppVM = viewModel()
                    App(appVm)
                }
            }
        }
        // Kait tes otomatis (emulator CI): buka pemutar langsung ke satu
        // episode lewat intent agar putar bisa diverifikasi ujung-ke-ujung
        // tanpa mengetuk UI. Tidak berpengaruh pada pemakaian normal.
        handleLink(intent)
        val testId = intent?.getStringExtra("test_id")
        if (!testId.isNullOrBlank()) {
            val testEp = intent?.getIntExtra("test_ep", 1) ?: 1
            vm.closeWelcome()
            vm.go(Route.Player(testId, testEp))
        } else if (intent?.getBooleanExtra("test_home", false) == true) {
            vm.closeWelcome()
        }
    }
}

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun App(vm: AppVM) {
    // Bahasa antarmuka aktif mengikuti Settings (debug.31).
    androidx.compose.runtime.SideEffect { I18n.lang = vm.s.settings.lang }
    // Kesegaran katalog (debug.33): segarkan diam-diam saat aplikasi kembali
    // ke depan, dan periksa berkala tiap 10 menit selama aplikasi terbuka.
    val hostActivity = androidx.compose.ui.platform.LocalContext.current as? androidx.activity.ComponentActivity
    androidx.compose.runtime.DisposableEffect(hostActivity) {
        if (hostActivity == null) return@DisposableEffect onDispose { }
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) vm.refreshIfStale()
        }
        hostActivity.lifecycle.addObserver(obs)
        onDispose { hostActivity.lifecycle.removeObserver(obs) }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10 * 60_000L)
            vm.refreshIfStale(10 * 60_000L)
        }
    }
    // Predictive back: layar mengecil mengikuti jari saat menggeser dari tepi; lepas = kembali, batal = pulih.
    var backP by remember { mutableStateOf(0f) }
    androidx.activity.compose.PredictiveBackHandler(enabled = vm.stack.size > 1 && !vm.welcomeOpen) { progress ->
        try {
            progress.collect { ev -> backP = ev.progress }
            vm.back(animated = false)
            backP = 0f
        } catch (e: kotlinx.coroutines.CancellationException) {
            backP = 0f
        }
    }
    // Hormati "Hapus animasi" / skala animasi 0 di Android.
    val ctxMotion = androidx.compose.ui.platform.LocalContext.current
    val reduceMotion = remember {
        try { android.provider.Settings.Global.getFloat(ctxMotion.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } catch (e: Exception) { false }
    }

    val enterA by animateFloatAsState(if (vm.welcomeOpen) 0f else 1f, tween(520), label = "homeEnter")
    Surface(Modifier.fillMaxSize(), color = Bg, contentColor = Color.White) {
        Box(Modifier.fillMaxSize()) {
            if (vm.welcomeOpen) WelcomeScreen(vm) else Box(Modifier.fillMaxSize().graphicsLayer { alpha = enterA; val z = 1.05f - 0.05f * enterA; scaleX = z; scaleY = z }) {
                Box(Modifier.fillMaxSize().graphicsLayer {
                    val p = backP
                    val s = 1f - 0.07f * p
                    scaleX = s; scaleY = s
                    alpha = 1f - 0.2f * p
                    shape = androidx.compose.foundation.shape.RoundedCornerShape((28 * p).dp); clip = p > 0f
                }) {
                SharedTransitionLayout {
                AnimatedContent(
                    targetState = vm.route,
                    transitionSpec = {
                        val t = targetState; val i = initialState; val dir = vm.navDir
                        val ease = androidx.compose.animation.core.FastOutSlowInEasing
                        val ct = when {
                            reduceMotion -> EnterTransition.None togetherWith ExitTransition.None
                            dir == 0 -> fadeIn(tween(220)) togetherWith fadeOut(tween(140))
                            // MASUK: Player naik dari bawah; Detail dibawa elemen bersama (poster terbang); lainnya geser dari kanan.
                            dir > 0 && t is Route.Player -> (slideInVertically(tween(340, easing = ease)) { it / 6 } + fadeIn(tween(280))) togetherWith fadeOut(tween(200))
                            dir > 0 && t is Route.Detail -> fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                            dir > 0 -> (slideInHorizontally(tween(320, easing = ease)) { it / 4 } + fadeIn(tween(280))) togetherWith
                                (slideOutHorizontally(tween(320, easing = ease)) { -it / 6 } + fadeOut(tween(220)))
                            // KEMBALI: kebalikannya.
                            i is Route.Player -> fadeIn(tween(240)) togetherWith (slideOutVertically(tween(300, easing = ease)) { it / 6 } + fadeOut(tween(240)))
                            i is Route.Detail -> fadeIn(tween(240)) togetherWith fadeOut(tween(220))
                            else -> (slideInHorizontally(tween(320, easing = ease)) { -it / 6 } + fadeIn(tween(280))) togetherWith
                                (slideOutHorizontally(tween(320, easing = ease)) { it / 4 } + fadeOut(tween(220)))
                        }
                        ct.also { it.targetContentZIndex = if (dir < 0) -1f else 1f }
                    },
                    label = "route",
                ) { r ->
                    val animScope = this
                    CompositionLocalProvider(
                        LocalSharedScope provides this@SharedTransitionLayout,
                        LocalAnimScope provides animScope,
                        LocalReduceMotion provides reduceMotion,
                    ) {
                    when (r) {
                        Route.Home -> HomeScreen(vm)
                        Route.Calendar -> CalendarScreen(vm)
                        Route.MyList -> MyListScreen(vm)
                        Route.Downloads -> DownloadsScreen(vm)
                        Route.ProfileTab -> ProfileScreen(vm)
                        is Route.Sub -> SubScreen(vm, r.key)
                        is Route.Detail -> DetailScreen(vm, r.id)
                        is Route.SeeAll -> SeeAllScreen(vm, r.key)
                        Route.Notif -> NotifScreen(vm)
                        Route.Search -> SearchScreen(vm)
                        is Route.Player -> PlayerScreen(vm, r)
                        is Route.Auth -> AuthScreen(vm, r.signUp)
                    }
                    }
                }
                }
                }
                if (vm.route !is Route.Player && vm.route !is Route.Auth) TabBar(vm, Modifier.align(Alignment.BottomCenter))
            }
            // Toast masuk/keluar dengan fade + geser; teks terakhir ditahan selama animasi keluar.
            var lastToast by remember { mutableStateOf("") }
            LaunchedEffect(vm.toastMsg) { vm.toastMsg?.let { lastToast = it } }
            val offlineBar = !vm.online && !vm.welcomeOpen && vm.route !is Route.Player
            AnimatedVisibility(
                visible = offlineBar,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(bottom = if (vm.route is Route.Auth) 0.dp else TabBarHeight),
                enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it },
            ) {
                Text("You're offline · downloads still work", Modifier.fillMaxWidth().background(RedDeep).padding(vertical = 6.dp),
                    color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            val bottom = (if (vm.route is Route.Player || vm.route is Route.Auth || vm.welcomeOpen) 24.dp else TabBarHeight + 12.dp) + (if (offlineBar) 30.dp else 0.dp)
            AnimatedVisibility(
                visible = vm.toastMsg != null,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = bottom, start = 16.dp, end = 16.dp),
                enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it / 2 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 },
            ) {
                Text(vm.toastMsg ?: lastToast, Modifier.clip(CircleShape).background(Color.White).padding(horizontal = 18.dp, vertical = 10.dp),
                    color = Bg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TabBar(vm: AppVM, modifier: Modifier) {
    data class T(val r: Route, val label: String, val icon: ImageVector, val sel: ImageVector)
    val tabs = listOf(
        T(Route.Home, I18n.t("tab_home", "Home"), Icons.Outlined.Home, Icons.Filled.Home),
        T(Route.Calendar, I18n.t("tab_calendar", "Calendar"), Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
        T(Route.MyList, I18n.t("tab_mylist", "My List"), Icons.Outlined.BookmarkBorder, Icons.Filled.Bookmark),
        T(Route.Search, I18n.t("tab_search", "Search"), Icons.Outlined.Search, Icons.Filled.Search),
        T(Route.ProfileTab, I18n.t("tab_profile", "Profile"), Icons.Outlined.Person, Icons.Filled.Person),
    )
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val selIdx = tabs.indexOfFirst { it.r == vm.tab }.coerceAtLeast(0)
    val pos by animateFloatAsState(selIdx.toFloat(), spring(dampingRatio = 0.72f, stiffness = 380f), label = "tabBar")
    Box(
        // Solid + garis tipis di atas, supaya konten yang digulir tidak tembus di balik tab bar.
        modifier.fillMaxWidth().background(Bg)
            .drawBehind { drawLine(Line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding().height(TabBarHeight),
    ) {
    // Penanda merah yang meluncur mengikuti tab aktif.
    Box(Modifier.fillMaxWidth().height(3.dp).padding(horizontal = 8.dp).drawBehind {
        val seg = size.width / tabs.size
        drawRoundRect(RedText, Offset(pos * seg + seg * 0.3f, 0f), androidx.compose.ui.geometry.Size(seg * 0.4f, size.height),
            androidx.compose.ui.geometry.CornerRadius(size.height, size.height))
    })
    Row(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        tabs.forEach { t ->
            val sel = vm.tab == t.r
            Column(Modifier.weight(1f).fillMaxHeight().clickable { if (!sel) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); vm.setTab(t.r) }, horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                val tint by animateColorAsState(if (sel) RedText else NavOff, tween(200), label = "tabTint")
                val pill by animateColorAsState(if (sel) Red.copy(alpha = 0.30f) else Color.Transparent, tween(220), label = "tabPill")
                // debug.33: pil melebar dengan pegas + ikon sedikit membesar saat aktif.
                val pw by animateDpAsState(if (sel) 60.dp else 34.dp, spring(dampingRatio = 0.7f, stiffness = 500f), label = "tabPillW")
                val isc by animateFloatAsState(if (sel) 1.12f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "tabIconScale")
                val lblA by animateFloatAsState(if (sel) 1f else 0.72f, tween(200), label = "tabLabelA")
                Box(Modifier.size(width = pw, height = 30.dp).clip(CircleShape).background(pill), contentAlignment = Alignment.Center) {
                    Icon(if (sel) t.sel else t.icon, t.label, Modifier.size(24.dp).graphicsLayer { scaleX = isc; scaleY = isc }, tint = tint)
                }
                Text(t.label, fontSize = 11.5.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium, color = tint, maxLines = 1,
                    modifier = Modifier.graphicsLayer { alpha = lblA })
            }
        }
    }
    }
}
````

### `app/src/main/java/com/onimeku/app/Notifier.kt`

````kotlin
package com.onimeku.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

object Notifier {
    private const val CH = "onimeku_alerts"

    fun show(ctx: Context, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CH) == null) {
            nm.createNotificationChannel(NotificationChannel(CH, "Episode alerts", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val pi = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(ctx, CH)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), n)
    }
}
````

### `app/src/main/java/com/onimeku/app/OnimekuApp.kt`

````kotlin
package com.onimeku.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Pemuat gambar Coil tunggal untuk seluruh aplikasi.
 * - Cache memori 20% RAM aplikasi, cache disk 256 MB: poster tidak diunduh ulang tiap sesi.
 * - respectCacheHeaders(false): proksi gambar/CDN sering mengirim no-cache; tanpa ini poster diunduh ulang terus.
 * - RGB_565 dimatikan (kualitas poster); hardware bitmap aktif otomatis di Android 8+ (hemat memori Java).
 */
class OnimekuApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        com.onimeku.app.data.CrashLog.install(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("img")).maxSizeBytes(256L * 1024 * 1024).build() }
            .respectCacheHeaders(false)
            .crossfade(false) // crossfade diatur per permintaan (rememberImg)
            .build()
}
````

### `app/src/main/java/com/onimeku/app/PlaybackService.kt`

````kotlin
package com.onimeku.app

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Layanan sesi media: memberi notifikasi + kontrol layar kunci dan menjaga
 * proses tetap hidup saat memutar di latar. Sesi dibuat oleh PlayerScreen
 * (pemilik ExoPlayer) dan didaftarkan lewat [current]; layanan hanya
 * menyerahkannya ke pengendali (notifikasi sistem) yang terhubung.
 */
class PlaybackService : MediaSessionService() {
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = current

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        // Pengguna menggeser aplikasi dari recents: hentikan, jangan memutar tanpa UI.
        current?.player?.pause()
        stopSelf()
    }

    companion object {
        @Volatile var current: MediaSession? = null
    }
}
````

### `app/src/main/java/com/onimeku/app/Reminders.kt`

````kotlin
package com.onimeku.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.onimeku.app.data.AppState
import com.onimeku.app.data.Catalog
import com.onimeku.app.data.Store
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Reminder mingguan lewat WorkManager: tetap jalan walau app ditutup atau HP restart. */
object Reminders {
    private const val TAG = "reminder"

    private fun delayMs(day: Int, time: String): Long {
        val now = LocalDateTime.now()
        // time bisa kosong bila updatedAt katalog tak terbaca: jangan crash.
        val parts = time.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val add = ((day - now.dayOfWeek.value % 7) + 7) % 7
        var t = now.toLocalDate().plusDays(add.toLong()).atTime(h, m)
        if (!t.isAfter(now)) t = t.plusDays(7)
        return Duration.between(now, t).toMillis()
    }

    fun schedule(ctx: Context, pi: Int, id: String, anime: com.onimeku.app.data.Anime? = null) {
        val c = anime ?: Catalog.byId[id] ?: return
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs(c.day, c.time), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("pi" to pi, "id" to id))
            .addTag(TAG).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("remind:$pi:$id", ExistingWorkPolicy.REPLACE, req)
    }

    fun cancel(ctx: Context, pi: Int, id: String) {
        WorkManager.getInstance(ctx).cancelUniqueWork("remind:$pi:$id")
    }

    fun rescheduleAll(ctx: Context, s: AppState) {
        WorkManager.getInstance(ctx).cancelAllWorkByTag(TAG)
        if (!s.settings.alerts) return
        s.profiles.forEachIndexed { pi, p -> p.remind.forEach { schedule(ctx, pi, it) } }
    }
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val pi = inputData.getInt("pi", 0)
        val id = inputData.getString("id") ?: return Result.success()
        val st = Store(applicationContext).load()
        val still = st.settings.alerts && st.profiles.getOrNull(pi)?.remind?.contains(id) == true
        if (!still) return Result.success()
        // Katalog tidak dimuat di proses worker; ambil detail judul ini dari API.
        val c = try { com.onimeku.app.data.ApiClient.fetchDetail(id) } catch (e: Exception) { null }
            ?: return Result.success()
        Notifier.show(applicationContext, "New episode: ${c.title}", "Episode ${c.ep} is out now")
        // Katalog kosong di proses worker: kirim detail yang baru diambil agar
        // jadwal minggu depan benar-benar dibuat (sebelumnya diam-diam batal).
        Reminders.schedule(applicationContext, pi, id, c)
        return Result.success()
    }
}
````

### `app/src/main/java/com/onimeku/app/data/Api.kt`

````kotlin
package com.onimeku.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import java.net.URLEncoder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.OffsetDateTime

// ---------------------------------------------------------------------------
// Klien API katalog Onimeku — LANGSUNG ke my-anime-app (perintah user
// 18 Okt 2026: "ganti pakai api ini saja", tanpa perantara onimekuappv2-api).
// Kontrak terverifikasi live:
//   GET /api/anime                  -> { ok, provider, data: [Anime] } (ringkasan, ~900KB)
//   GET /api/anime?id=<slug>        -> { ok, provider, data: Anime (+episodes[]) }
//   GET /api/anime?id=<slug>&ep=<n> -> { ok, provider, data: Playback }
// Hulu TIDAK punya endpoint jadwal maupun poster pengganti; fungsi yang
// bersangkutan dinonaktifkan dengan anggun di bawah.
// Tugas yang dulu dikerjakan API perantara kini dikerjakan di klien:
// stream MegaPlay (CDN nexabloom) dibungkus proksi media hulu
// (/api/proxy?u=&ref=) dengan Referer akar origin — pola yang sama yang
// membuat putar bekerja (host segmen acak tak terjangkau dari jaringan
// sebagian pengguna; Referer halaman penuh ditolak CDN).
// ---------------------------------------------------------------------------

@Serializable
data class ApiAnime(
    val id: String = "",
    val title: String = "",
    val poster: String? = null,
    val banner: String? = null,
    val synopsis: String? = null,
    val genres: List<String> = emptyList(),
    val score: Double? = null,
    val year: Int? = null,
    val status: String? = null,
    val totalEpisodes: Int? = null,
    val latestEpisode: Int? = null,
    val episodeNumbers: List<Int> = emptyList(),
    val updatedAt: String? = null,
    val anilistId: Int? = null,
    val country: String? = null,
)

@Serializable
private data class ListResp(
    val ok: Boolean = false,
    val data: List<ApiAnime> = emptyList(),
)

@Serializable
private data class DetailResp(
    val ok: Boolean = false,
    val data: ApiAnime? = null,
)

@Serializable
data class ApiSubtitle(
    val lang: String = "",
    val label: String = "",
    val url: String = "",
    val referer: String? = null,
)

@Serializable
data class Playback(
    val streamUrl: String? = null,
    val streamProxyUrl: String? = null,
    val directUrl: String? = null,
    val pageUrl: String? = null,
    val embedUrl: String? = null,
    val referer: String? = null,
    val downloadUrl: String? = null,
    val poster: String? = null,
    val episode: Int = 0,
    val subtitles: List<ApiSubtitle> = emptyList(),
    val mediaKind: String? = null,
    val playable: String? = null,
)

@Serializable
private data class PlaybackResp(
    val ok: Boolean = false,
    val data: Playback? = null,
)

object ApiClient {
    const val BASE = "https://my-anime-app.adiyth-arpansa.workers.dev"
    private val json = Json { ignoreUnknownKeys = true }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun originOf(url: String?): String? = try {
        val u = URL(url ?: "")
        "${u.protocol}://${u.host}/"
    } catch (e: Exception) {
        null
    }

    private suspend fun getText(url: String, readTimeoutSec: Long = 60): String = withContext(Dispatchers.IO) {
        // OkHttp bersama (Net): koneksi HTTP/2 ke worker dipakai ulang antar
        // permintaan, jadi playback/detail tidak mengulang handshake TLS.
        val req = okhttp3.Request.Builder().url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "OnimekuApp/1.0 (Android)")
            .build()
        val client = Net.client.newBuilder().readTimeout(readTimeoutSec, java.util.concurrent.TimeUnit.SECONDS).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code}")
            resp.body?.string() ?: throw java.io.IOException("respons kosong")
        }
    }

    /** GET dengan percobaan ulang (jaringan seluler + hulu kadang goyah). */
    private suspend fun getTextRetry(url: String, attempts: Int = 3): String {
        var last: Exception? = null
        for (i in 1..attempts) {
            try {
                return getText(url)
            } catch (e: Exception) {
                last = e
                if (i < attempts) kotlinx.coroutines.delay(900L * i)
            }
        }
        throw last ?: java.io.IOException("gagal memuat")
    }

    /**
     * Ambil seluruh katalog dalam SATU respons (hulu tidak berpaginasi;
     * ringkasan ~900KB untuk ~900 judul). onProgress/onRaw dipanggil
     * sekali dengan hasil final demi kompatibilitas pemanggil.
     */
    suspend fun fetchCatalog(
        onProgress: (List<Anime>) -> Unit = {},
        onRaw: (List<ApiAnime>) -> Unit = {},
    ): List<Anime> {
        val text = getTextRetry("$BASE/api/anime")
        // ±900 KB JSON: parse + pemetaan di thread latar agar UI tidak tersendat.
        val (raw, mapped) = withContext(Dispatchers.Default) {
            val r = json.decodeFromString<ListResp>(text)
            val rw = r.data.filter { it.id.isNotBlank() && it.title.isNotBlank() }.distinctBy { it.id }
            rw to rw.map { it.toAnime() }
        }
        onProgress(mapped)
        onRaw(raw)
        return mapped
    }

    /** Katalog terakhir yang berhasil dimuat, disimpan di disk oleh aplikasi. */
    fun loadCachedCatalog(file: java.io.File): List<Anime>? {
        return try {
            if (!file.exists()) return null
            val raw = json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(ApiAnime.serializer()),
                file.readText(),
            )
            raw.filter { it.id.isNotBlank() && it.title.isNotBlank() }
                .distinctBy { it.id }
                .map { it.toAnime() }
                .ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    fun saveCachedCatalog(file: java.io.File, raw: List<ApiAnime>) {
        try {
            file.writeText(
                json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(ApiAnime.serializer()),
                    raw,
                )
            )
        } catch (e: Exception) {
            // cache disk bersifat cadangan; gagal menulis tidak fatal
        }
    }

    /**
     * Hulu tidak menyediakan endpoint jadwal tayang. Kalender aplikasi
     * otomatis jatuh ke turunan hari/jam dari katalog (jalur cadangan
     * yang memang sudah ada di AppVM).
     */
    suspend fun fetchSchedule(): List<ApiScheduleItem> = emptyList()

    suspend fun fetchDetail(id: String): Anime? {
        val r = json.decodeFromString<DetailResp>(getTextRetry("$BASE/api/anime?id=${enc(id)}"))
        return r.data?.takeIf { it.id.isNotBlank() }?.toAnime()
    }

    // Data putar yang diambil lebih dulu (episode berikutnya). URL stream
    // bertoken kedaluwarsa, jadi hanya dipakai sekali dan dalam TTL pendek.
    private class Prefetched(val at: Long, val pb: Playback)
    private val prefetched = java.util.concurrent.ConcurrentHashMap<String, Prefetched>()
    // 10 menit (debug.37): URL nexabloom berumur jam-jaman; jendela
    // prefetch panjang membuat ketukan Play lama sesudah Detail dibuka
    // tetap mengenai hasil pemanasan.
    private const val PREFETCH_TTL_MS = 600_000L

    /** Panggil saat episode hampir habis: episode berikutnya langsung siap diputar. */
    suspend fun prefetchPlayback(id: String, ep: Int) {
        val k = "$id:$ep"
        if (prefetched[k]?.let { System.currentTimeMillis() - it.at < PREFETCH_TTL_MS } == true) return
        val pb = try { fetchPlaybackNet(id, ep) } catch (e: Exception) { null } ?: return
        prefetched[k] = Prefetched(System.currentTimeMillis(), pb)
    }

    /**
     * [fresh] = true mengabaikan hasil prefetch (mis. untuk unduhan, yang
     * butuh URL segar). Satu percobaan ulang cepat menutup gangguan sesaat
     * tanpa membuat pengguna menunggu 3 kali seperti katalog.
     */
    suspend fun fetchPlayback(id: String, ep: Int, fresh: Boolean = false): Playback? {
        val k = "$id:$ep"
        val hit = prefetched.remove(k)
        if (!fresh && hit != null && System.currentTimeMillis() - hit.at < PREFETCH_TTL_MS) return hit.pb
        return try {
            fetchPlaybackNet(id, ep)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            kotlinx.coroutines.delay(400)
            fetchPlaybackNet(id, ep)
        }
    }

    private suspend fun fetchPlaybackNet(id: String, ep: Int): Playback? {
        // Timeout baca 12 dtk (debug.30): endpoint ini normalnya menjawab
        // <5 dtk; 20 dtk x 2 putaran membuat layar memuat menggantung
        // terlalu lama di jaringan seluler saat hulu goyah.
        val r = json.decodeFromString<PlaybackResp>(getText("$BASE/api/anime?id=${enc(id)}&ep=$ep", readTimeoutSec = 12))
        val pb = r.data ?: return null
        // Normalisasi label subtitle hulu ({label, lang, url}) ke lang.
        val subs = pb.subtitles.map { s -> if (s.lang.isBlank() && s.label.isNotBlank()) s.copy(lang = s.label) else s }
        val raw = pb.streamUrl
        // Stream MegaPlay (CDN nexabloom): bungkus lewat proksi media
        // hulu, MENIRU pemutar web persis: ref = referer UTUH dari API
        // (proksi yang memilah kandidat referer di sisi server); field
        // referer sendiri tetap akar origin untuk header jalur langsung.
        // Rantai cadangan di pemutar meniru web: proksi -> langsung ->
        // gagal jujur (embed BUKAN cadangan untuk judul ber-stream).
        return if (raw != null && raw.contains("nexabloom", ignoreCase = true)) {
            val origin = originOf(pb.referer) ?: "https://megaplay.buzz/"
            val proxied = "$BASE/api/proxy?u=${enc(raw)}&ref=${enc(pb.referer?.takeIf { it.isNotBlank() } ?: origin)}"
            pb.copy(
                streamUrl = proxied,
                streamProxyUrl = proxied,
                directUrl = raw,
                pageUrl = pb.referer?.takeIf { it.contains("/stream/") },
                referer = origin,
                subtitles = subs,
            )
        } else {
            pb.copy(subtitles = subs)
        }
    }

    /**
     * Resolusi poster pengganti TIDAK lagi dilakukan aplikasi
     * (debug.31, perintah user): semua data poster wajib berasal dari
     * API my-anime-app. Penyembuhan poster ber-host rusak (s4.anilist.co
     * 403) kini hidup di server (poster_map + resolusi Kitsu di
     * my-anime-app/src/lib/store.ts: healPosters), sehingga respons
     * /api/anime sudah membawa poster sembuh. Di sini tetap null agar
     * tidak ada panggilan keluar selain ke API sendiri; PosterHealer
     * menampilkan gradasi untuk poster yang memang belum sembuh.
     */
    suspend fun resolvePoster(anilistId: Int): PosterResp? = null

    /**
     * Kirim log diagnostik lokal ke server (debug.38): kegagalan putar di
     * HP user tak terlihat dari server (permintaan varian/segmen tak
     * pernah tiba), jadi aplikasi melaporkan lognya sendiri ke
     * /api/app-log (KV app_log_v1) untuk dibaca agen dari server.
     * Usaha terbaik — kegagalan kirim diabaikan.
     */
    suspend fun uploadDiag(tag: String) {
        try {
            val text = CrashLog.read()
            val body = org.json.JSONObject().put("tag", tag).put("msg", text).toString()
                .toRequestBody("application/json".toMediaType())
            val req = okhttp3.Request.Builder().url("$BASE/api/app-log").post(body).build()
            withContext(Dispatchers.IO) { Net.client.newCall(req).execute().use { } }
        } catch (e: Exception) { /* abaikan */ }
    }
}

@Serializable
data class ApiScheduleItem(
    val id: String = "",
    val ep: Int = 0,
    val airingAt: Long = 0,
)

@Serializable
data class PosterResp(
    val ok: Boolean = false,
    val poster: String? = null,
    val cover: String? = null,
)

private fun ApiAnime.toAnime(): Anime {
    val latest = latestEpisode ?: episodeNumbers.maxOrNull() ?: 0
    val total = totalEpisodes ?: 0
    val odt = runCatching { OffsetDateTime.parse(updatedAt) }.getOrNull()
    val cleanSyn = (synopsis ?: "")
        .substringBefore("(Source:").trim()
        .ifBlank { "No synopsis available yet." }
    val scoreTxt = score?.takeIf { it > 0.0 }?.let { s ->
        val t = if (s % 1.0 == 0.0) s.toInt().toString() else s.toString()
        "★ $t"
    }
    return Anime(
        id = id,
        title = title,
        genres = genres,
        rating = scoreTxt,
        hd = false, // API tidak menyediakan info kualitas; jangan klaim HD
        eps = maxOf(total, latest, episodeNumbers.maxOrNull() ?: 0),
        ep = latest,
        label = false,
        synopsis = cleanSyn,
        year = year ?: 0,
        day = odt?.dayOfWeek?.value?.rem(7) ?: 0,
        time = odt?.toLocalTime()?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "",
        poster = poster?.ifBlank { null },
        banner = banner?.ifBlank { null },
        available = episodeNumbers.distinct().sorted(),
        apiStatus = status,
        updatedAt = updatedAt,
        score = score,
        anilistId = anilistId,
        country = country,
    )
}
````

### `app/src/main/java/com/onimeku/app/data/Auth.kt`

````kotlin
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

    /** Tukar refresh token dengan sesi baru (token akses berumur pendek). */
    suspend fun refresh(refreshToken: String): Account =
        session(call("/auth/v1/token?grant_type=refresh_token", kotlinx.serialization.json.buildJsonObject {
            put("refresh_token", kotlinx.serialization.json.JsonPrimitive(refreshToken))
        }.toString())) ?: throw AuthException("Session expired. Please sign in again.")

    /**
     * Hapus akun permanen lewat Edge Function `delete-account`
     * (backend/supabase/functions/delete-account). Klien tidak pernah memegang service_role.
     */
    suspend fun deleteAccount(accessToken: String) = withContext(Dispatchers.IO) {
        if (!configured) throw AuthException("Account service is not available yet.")
        val rb = Request.Builder()
            .url(BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/delete-account")
            .header("apikey", BuildConfig.SUPABASE_KEY)
            .header("Authorization", "Bearer $accessToken")
            .post("{}".toRequestBody(JSON)).build()
        http.newCall(rb).execute().use { r ->
            if (!r.isSuccessful) throw AuthException("Could not delete the account (code ${r.code}). Please try again.")
        }
    }
}
````

### `app/src/main/java/com/onimeku/app/data/Catalog.kt`

````kotlin
package com.onimeku.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@androidx.compose.runtime.Immutable
data class Anime(
    val id: String,
    val title: String,
    val genres: List<String>,
    val rating: String?,
    val hd: Boolean,
    val eps: Int,
    val ep: Int,
    val label: Boolean,
    val synopsis: String,
    val year: Int,
    val day: Int = 0,
    val time: String = "",
    val poster: String? = null,
    val banner: String? = null,
    val available: List<Int> = emptyList(),
    val apiStatus: String? = null,
    val updatedAt: String? = null,
    val score: Double? = null,
    val anilistId: Int? = null,
    val country: String? = null,
) {
    val status: String
        get() = when (apiStatus) {
            "FINISHED" -> "Completed"
            "RELEASING", "NOT_YET_RELEASED" -> "Airing"
            else -> if (ep < eps) "Airing" else "Completed"
        }
}

/**
 * Katalog aplikasi. Isinya diisi dari API Onimeku (lihat ApiClient) saat
 * aplikasi dibuka; sebelum data tiba, daftar kosong dan layar menampilkan
 * status memuat. Tidak ada data contoh ter-hardcode lagi.
 *
 * Semua daftar turunan (top, populer, per genre, dst.) dihitung SEKALI per
 * perubahan [all] lalu di-cache. Sebelumnya setiap akses (mis. byId[...] di
 * dalam loop Home) mengurutkan/menyusun ulang seluruh katalog sehingga
 * layar terasa berat saat katalog berisi ribuan judul.
 */
object Catalog {
    var all by mutableStateOf<List<Anime>>(emptyList())
    var loading by mutableStateOf(true)
    var loadFailed by mutableStateOf(false)

    private val byScore = compareByDescending<Anime> { it.score ?: -1.0 }.thenBy { it.title.lowercase() }

    private fun relLabel(iso: String?): String {
        val d = runCatching { OffsetDateTime.parse(iso).toLocalDate() }.getOrNull() ?: return "Today"
        val days = ChronoUnit.DAYS.between(d, LocalDate.now())
        return when {
            days <= 0L -> "Today"
            days == 1L -> "Yesterday"
            else -> "$days days ago"
        }
    }

    private class Derived(val src: List<Anime>) {
        val byId: Map<String, Anime> = src.associateBy { it.id }
        val popular: List<String> = src.sortedWith(byScore).map { it.id }
        val top: List<String> = popular.take(10)
        val airing: List<String> = src.filter { it.apiStatus == "RELEASING" }.sortedWith(byScore).map { it.id }
        val latest: List<String> = src.sortedWith(
            compareByDescending<Anime> { it.year }.thenByDescending { it.updatedAt ?: "" },
        ).map { it.id }
        val donghua: List<String> = src.filter { it.country == "CN" }.sortedWith(byScore).map { it.id }
        val alphabetical: List<String> = src.sortedBy { it.title.lowercase() }.map { it.id }
        private val genreCount: List<Map.Entry<String, Int>> =
            src.flatMap { it.genres }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
        val topGenres: List<String> = genreCount.take(4).map { it.key }
        val chips: List<String> = genreCount.take(10).map { it.key }
        val years: List<String> = src.map { it.year }.filter { it > 0 }.distinct().sortedDescending().take(6).map { it.toString() }
        val byGenre: Map<String, List<String>> = genreCount.associate { (g, _) ->
            g to src.filter { g in it.genres }.sortedWith(byScore).map { it.id }
        }
        val fresh: List<Triple<String, Int, String>> = src.filter { it.ep > 0 }
            .sortedByDescending { it.updatedAt ?: "" }
            .take(8)
            .map { Triple(it.id, it.ep, relLabel(it.updatedAt)) }
    }

    private var cache: Derived? = null
    private val d: Derived
        get() {
            val cur = all // membaca state: Compose ikut memantau perubahan katalog
            val c = cache
            if (c != null && c.src === cur) return c
            return Derived(cur).also { cache = it }
        }

    val byId: Map<String, Anime> get() = d.byId

    /** Sepuluh judul skor tertinggi (baris "Top Hits Anime" + hero). */
    val top: List<String> get() = d.top

    /** Seluruh katalog menurut skor tertinggi. */
    val popular: List<String> get() = d.popular

    /** Sedang tayang (status RELEASING dari API), skor tertinggi dulu. */
    val airing: List<String> get() = d.airing

    /** Rilisan terbaru: tahun paling baru, lalu paling baru diperbarui. */
    val latest: List<String> get() = d.latest

    /** Donghua (animasi Tiongkok), barisnya sendiri di Home. */
    val donghua: List<String> get() = d.donghua

    /** Seluruh katalog urut abjad (halaman "All Anime"). */
    val alphabetical: List<String> get() = d.alphabetical

    /** Genre dengan jumlah judul terbanyak, untuk baris per genre di Home. */
    val topGenres: List<String> get() = d.topGenres

    /** Semua judul dalam satu genre, skor tertinggi dulu. */
    fun byGenre(g: String): List<String> = d.byGenre[g].orEmpty()

    /** Episode terbaru (5 teratas untuk baris Home). */
    val new: List<Triple<String, Int, String>> get() = d.fresh.take(5)

    val notifs: List<Triple<String, Int, String>> get() = d.fresh.take(6)

    val chips: List<String> get() = d.chips

    val years: List<String> get() = d.years

    val statuses = listOf("Airing", "Completed")

    fun similar(id: String): List<Anime> {
        val g = byId[id]?.genres ?: return emptyList()
        return all.filter { it.id != id }
            .map { it to it.genres.count { x -> x in g } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(8).map { it.first }
    }
}
````

### `app/src/main/java/com/onimeku/app/data/Comments.kt`

````kotlin
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
 * Komentar per judul (debug.31): tabel `comments` di Supabase yang sama
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
````

### `app/src/main/java/com/onimeku/app/data/CrashLog.kt`

````kotlin
package com.onimeku.app.data

import android.content.Context
import android.os.Build
import java.io.File

/**
 * Pencatat galat lokal (tanpa pihak ketiga, tanpa data pribadi).
 * - Crash tak tertangani + galat pemutar ditulis ke filesDir/diagnostics.log (maks ±64 KB, bagian lama dibuang).
 * - Pengguna bisa membagikan log dari About > Send diagnostics.
 * Untuk pelaporan otomatis ke server (Crashlytics/Sentry) diperlukan akun layanan tersebut.
 */
object CrashLog {
    private const val MAX = 64 * 1024
    @Volatile private var file: File? = null
    private val lock = Any()

    fun install(ctx: Context) {
        val app = ctx.applicationContext
        file = File(app.filesDir, "diagnostics.log")
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { note("CRASH", "thread=${t.name}\n${android.util.Log.getStackTraceString(e)}") } catch (_: Throwable) {}
            prev?.uncaughtException(t, e)
        }
    }

    fun note(tag: String, msg: String) {
        val f = file ?: return
        synchronized(lock) {
            try {
                val line = "${java.time.Instant.now()} [$tag] $msg\n"
                var old = if (f.exists()) f.readText() else ""
                if (old.length + line.length > MAX) old = old.takeLast(MAX / 2)
                f.writeText(old + line)
            } catch (_: Exception) {}
        }
    }

    fun read(): String {
        val head = "Onimeku ${com.onimeku.app.BuildConfig.VERSION_NAME} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}\n\n"
        val f = file
        return head + (if (f != null && f.exists()) f.readText().takeLast(20_000) else "(no entries)")
    }
}
````

### `app/src/main/java/com/onimeku/app/data/Downloads.kt`

````kotlin
package com.onimeku.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadHelper
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.onimeku.app.R
import java.io.File
import java.io.IOException

/**
 * Unduhan ASLI memakai Media3: DownloadManager + DownloadService mengunduh
 * segmen HLS ke SimpleCache yang sama dengan yang dibaca pemutar, jadi
 * episode yang selesai terunduh bisa diputar offline. Kunci cache dibuat
 * stabil per episode ("dl:<id>:<ep>") karena URL stream bertoken berubah
 * setiap pengambilan — pemutar memakai kunci yang sama agar cocok.
 */
object Downloads {
    const val CHANNEL_ID = "onimeku_downloads"
    private const val UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    @Volatile private var manager: DownloadManager? = null
    @Volatile private var simpleCache: SimpleCache? = null
    @Volatile private var database: StandaloneDatabaseProvider? = null

    fun key(id: String, ep: Int) = "dl:$id:$ep"

    /** "dl:<id>:<ep>" -> (id, ep); id adalah slug tanpa ':' jadi aman. */
    fun parseKey(k: String): Pair<String, Int>? {
        if (!k.startsWith("dl:")) return null
        val rest = k.substring(3)
        val i = rest.lastIndexOf(':')
        if (i <= 0) return null
        val ep = rest.substring(i + 1).toIntOrNull() ?: return null
        return rest.substring(0, i) to ep
    }

    private fun db(ctx: Context): StandaloneDatabaseProvider =
        database ?: synchronized(this) {
            database ?: StandaloneDatabaseProvider(ctx.applicationContext).also { database = it }
        }

    fun cache(ctx: Context): SimpleCache =
        simpleCache ?: synchronized(this) {
            simpleCache ?: SimpleCache(
                File(ctx.applicationContext.filesDir, "media_downloads"),
                NoOpCacheEvictor(),
                db(ctx),
            ).also { simpleCache = it }
        }

    /**
     * Reset satu kali (debug.13): cache media dari era sebelum proksi bisa
     * menyimpan entri playlist rusak/terpotong (kegagalan jaringan di tengah
     * penulisan) yang dipakai ulang pemutar sebagai kunci cache yang sama,
     * sehingga episode tetap gagal diputar walau stream-nya sudah sehat.
     * Buang isi cache + kosongkan indeks unduhan lama. Wajib dipanggil
     * SEBELUM manager/cache pertama kali disentuh dalam proses ini.
     */
    fun oneTimeResetV13(ctx: Context) {
        val app = ctx.applicationContext
        val prefs = app.getSharedPreferences("dl_state", Context.MODE_PRIVATE)
        if (prefs.getBoolean("reset_v13", false)) return
        if (simpleCache == null && manager == null) {
            runCatching { File(app.filesDir, "media_downloads").deleteRecursively() }
            runCatching {
                val m = manager(ctx)
                for ((k, _) in states(ctx)) m.removeDownload(k)
            }
        }
        prefs.edit().putBoolean("reset_v13", true).apply()
    }

    /**
     * Pembersihan satu kali (debug.23): pada era cache-tulis (<= debug.20)
     * hasil streaming ikut ditulis ke cache bersama di bawah kunci konten
     * "dl:<id>:<ep>" — termasuk halaman error yang tersimpan sebagai
     * "playlist". Pemutar membaca entri rusak itu lebih dulu dan gagal
     * walau stream dari API sudah sehat; instalasi segar (emulator) tak
     * punya entri ini sehingga selalu berhasil. Buang entri "dl:" yang
     * TIDAK terdaftar di indeks unduhan; data unduhan asli tidak disentuh.
     */
    fun oneTimeCleanupV23(ctx: Context) {
        val app = ctx.applicationContext
        val prefs = app.getSharedPreferences("dl_state", Context.MODE_PRIVATE)
        if (prefs.getBoolean("cleanup_v23", false)) return
        // Bendera hanya dipasang bila pembersihan BENAR-BENAR berjalan;
        // kegagalan diam-diam (mis. indeks unduhan belum siap) tidak boleh
        // menandai selesai untuk selamanya (perbaikan debug.25).
        val done = runCatching {
            val keep = states(app).keys
            val c = cache(app)
            for (k in c.keys.toList()) {
                if (k.startsWith("dl:") && k !in keep) c.removeResource(k)
            }
            true
        }.getOrDefault(false)
        if (done) prefs.edit().putBoolean("cleanup_v23", true).apply()
    }

    // ---- Referer per unduhan ----
    // CDN membedakan asal permintaan; Referer dari respons API disimpan per
    // awalan URL stream (host + segmen path pertama) agar unduhan segmen
    // memakai Referer yang sama seperti pemutaran.
    private fun refKey(url: String): String? {
        val u = runCatching { java.net.URL(url) }.getOrNull() ?: return null
        val first = u.path.trim('/').substringBefore('/')
        return u.host + "/" + first
    }

    private fun registerRef(ctx: Context, streamUrl: String, referer: String?) {
        if (referer.isNullOrBlank()) return
        val k = refKey(streamUrl) ?: return
        // Stream MegaPlay: CDN-nya hanya menerima Referer akar origin.
        val host = runCatching { java.net.URL(streamUrl).host }.getOrNull().orEmpty()
        val eff = if (host == "nexabloom.top" || host.endsWith(".nexabloom.top")) {
            "https://megaplay.buzz/"
        } else {
            referer
        }
        ctx.applicationContext.getSharedPreferences("dl_refs", Context.MODE_PRIVATE)
            .edit().putString(k, eff).apply()
    }

    private fun refFor(ctx: Context, url: String): String? {
        val k = refKey(url) ?: return null
        return ctx.applicationContext.getSharedPreferences("dl_refs", Context.MODE_PRIVATE)
            .getString(k, null)
    }

    private fun httpFactory(ctx: Context): OkHttpDataSource.Factory {
        val app = ctx.applicationContext
        val client = Net.client.newBuilder().addInterceptor { chain ->
            val req = chain.request()
            val b = req.newBuilder().header("User-Agent", UA)
            if (req.header("Referer") == null) {
                refFor(app, req.url.toString())?.let { b.header("Referer", it) }
            }
            chain.proceed(b.build())
        }.build()
        return OkHttpDataSource.Factory(client)
    }

    fun manager(ctx: Context): DownloadManager =
        manager ?: synchronized(this) {
            manager ?: buildManager(ctx.applicationContext).also { manager = it }
        }

    private fun buildManager(app: Context): DownloadManager {
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
        )
        val dsFactory = CacheDataSource.Factory()
            .setCache(cache(app))
            .setUpstreamDataSourceFactory(httpFactory(app))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val dm = DownloadManager(
            app, db(app), cache(app), dsFactory,
            java.util.concurrent.Executors.newFixedThreadPool(4),
        )
        dm.maxParallelDownloads = 3
        return dm
    }

    /** Hormati pengaturan "Wi-Fi only": unduhan menunggu jaringan tak terukur. */
    fun setWifiOnly(ctx: Context, wifiOnly: Boolean) {
        manager(ctx).requirements = androidx.media3.exoplayer.scheduler.Requirements(
            if (wifiOnly) androidx.media3.exoplayer.scheduler.Requirements.NETWORK_UNMETERED
            else androidx.media3.exoplayer.scheduler.Requirements.NETWORK
        )
    }

    fun enqueue(
        ctx: Context, id: String, ep: Int, streamUrl: String, referer: String?,
        onError: () -> Unit,
    ) {
        val app = ctx.applicationContext
        registerRef(app, streamUrl, referer)
        val contentKey = key(id, ep)
        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(contentKey)
            .setCustomCacheKey(contentKey)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()
        // DownloadHelper memilih satu set trek bawaan (bukan semua rendisi)
        // dan mengambil manifes lewat factory kita (UA + Referer ikut).
        val helper = DownloadHelper.forMediaItem(
            app,
            mediaItem,
            DefaultRenderersFactory(app),
            httpFactory(app),
        )
        helper.prepare(object : DownloadHelper.Callback {
            override fun onPrepared(h: DownloadHelper) {
                val req = h.getDownloadRequest(null)
                DownloadService.sendAddDownload(app, OnimekuDownloadService::class.java, req, true)
                h.release()
            }

            override fun onPrepareError(h: DownloadHelper, e: IOException) {
                h.release()
                onError()
            }
        })
    }

    fun remove(ctx: Context, contentKey: String) {
        DownloadService.sendRemoveDownload(
            ctx.applicationContext, OnimekuDownloadService::class.java, contentKey, false,
        )
    }

    /** Peta contentKey -> Download dari indeks unduhan saat ini. */
    fun states(ctx: Context): Map<String, Download> {
        val out = LinkedHashMap<String, Download>()
        manager(ctx).downloadIndex.getDownloads().use { cursor ->
            while (cursor.moveToNext()) out[cursor.download.request.id] = cursor.download
        }
        return out
    }
}

/** Layanan unduhan Media3 (proses utama, berbagi DownloadManager singleton). */
class OnimekuDownloadService : DownloadService(
    1001,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    Downloads.CHANNEL_ID,
    R.string.dl_channel_name,
    R.string.dl_channel_desc,
) {
    override fun getDownloadManager(): DownloadManager = Downloads.manager(this)
    override fun getScheduler(): Scheduler? = null
    override fun getForegroundNotification(
        downloads: MutableList<Download>, notMetRequirements: Int,
    ): android.app.Notification =
        androidx.media3.exoplayer.offline.DownloadNotificationHelper(this, Downloads.CHANNEL_ID)
            .buildProgressNotification(
                this, android.R.drawable.stat_sys_download, null, null, downloads, notMetRequirements,
            )
}
````

### `app/src/main/java/com/onimeku/app/data/Net.kt`

````kotlin
package com.onimeku.app.data

import okhttp3.ConnectionPool
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Satu OkHttpClient untuk seluruh jalur jaringan media/API.
 *
 * Sebelumnya setiap pemutar membuat OkHttpClient baru (pool koneksi, TLS, dan
 * cookie dimulai dari nol tiap ganti episode) dan API memakai HttpURLConnection
 * (handshake TLS baru tiap permintaan). Dengan klien bersama, koneksi HTTP/2 ke
 * worker/CDN dipakai ulang: playlist + segmen pertama tiba lebih cepat dan
 * pindah episode tidak mengulang handshake.
 */
object Net {
    const val UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    /** Cookie per host (CDN sumber menuntut cookie dari respons sebelumnya). */
    private val cookieJar = object : CookieJar {
        private val store = ConcurrentHashMap<String, MutableList<Cookie>>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = store.getOrPut(url.host) { mutableListOf() }
            synchronized(list) {
                for (c in cookies) {
                    list.removeAll { it.name == c.name }
                    list.add(c)
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val list = store[url.host] ?: return emptyList()
            return synchronized(list) { list.filter { it.matches(url) } }
        }
    }

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
````

### `app/src/main/java/com/onimeku/app/data/PosterHealer.kt`

````kotlin
package com.onimeku.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * Poster ber-host s4.anilist.co tidak terjangkau dari jaringan pengguna
 * (CDN-nya membalas 403 dari mana pun). Penyembuhannya kini hidup di
 * SERVER (debug.31, perintah user: semua data poster dari API ini):
 * my-anime-app menyelesaikan padanan Kitsu per anilistId dan
 * menyajikannya langsung di respons katalog/detail, jadi di sini
 * poster katalog yang masih ber-host rusak cukup disembunyikan
 * (null -> latar gradasi) sampai API menyembuhkannya — aplikasi
 * TIDAK lagi menelepon Kitsu atau API lain sendiri. Cache lokal
 * hanya menyimpan URL yang disajikan API.
 */
object PosterHealer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val posters = mutableStateMapOf<Int, String>()
    private val covers = mutableStateMapOf<Int, String>()
    private val inFlight = Collections.synchronizedSet(HashSet<Int>())
    private var prefs: SharedPreferences? = null

    fun init(ctx: Context) {
        if (prefs == null) {
            prefs = ctx.getSharedPreferences("poster_cache", Context.MODE_PRIVATE)
            // Sekali saja (debug.31): kosongkan cache lama yang berisi
            // URL hasil resolusi langsung aplikasi (era debug.26-.30) —
            // mulai sekarang poster hanya berasal dari API.
            if (prefs?.getBoolean("reset_v31", false) != true) {
                prefs?.edit()?.clear()?.putBoolean("reset_v31", true)?.apply()
            }
        }
    }

    // Host/pola poster bermasalah — cerminan daftar BAD_HOST/BAD_PATH di API:
    // poster seperti ini tidak ditampilkan mentah, melainkan diganti poster
    // asli dari Kitsu lewat endpoint resolusi API.
    private fun isBad(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val u = url.lowercase()
        return u.contains("anilist.co") || u.contains("samehadaku") ||
            u.contains("placeholder") || u.contains("placehold") ||
            u.contains("dummyimage") || u.contains("picsum") || u.contains("ui-avatars") ||
            u.contains("backblazeb2") || u.contains("amazonaws.com")
    }

    // Sebagian kecil poster Kitsu berupa URL bertanda tangan S3/Backblaze
    // (X-Amz-Expires=900 — mati dalam 15 menit). URL semacam itu TIDAK
    // boleh disimpan permanen: cukup hidup di memori sesi ini, dan entri
    // cache lama yang terlanjur menyimpannya dianggap basi (debug.28).
    private fun isPresigned(u: String?): Boolean = u?.contains("X-Amz-") == true

    fun posterFor(a: Anime): String? {
        val p = a.poster
        if (p.isNullOrBlank()) return null
        if (!isBad(p)) return p
        val aid = a.anilistId ?: return null
        posters[aid]?.let { return it.ifBlank { null } }
        prefs?.getString("p$aid", null)?.let {
            if (!isPresigned(it)) { posters[aid] = it; return it }
        }
        ensure(aid)
        return null
    }

    fun coverFor(a: Anime): String? {
        val b = a.banner
        if (b.isNullOrBlank()) return null
        if (!isBad(b)) return b
        val aid = a.anilistId ?: return null
        covers[aid]?.let { return it.ifBlank { null } }
        prefs?.getString("c$aid", null)?.let {
            if (!isPresigned(it)) { covers[aid] = it; return it }
        }
        ensure(aid)
        return null
    }

    private fun ensure(aid: Int) {
        if (!inFlight.add(aid)) return
        scope.launch {
            try {
                val r = ApiClient.resolvePoster(aid)
                posters[aid] = r?.poster ?: ""
                covers[aid] = r?.cover ?: ""
                if (r != null) {
                    // Hanya URL stabil yang disimpan permanen; yang
                    // bertanda tangan kedaluwarsa (X-Amz-) memori saja.
                    val ed = prefs?.edit()
                    if (!isPresigned(r.poster)) ed?.putString("p$aid", r.poster ?: "")
                    if (!isPresigned(r.cover)) ed?.putString("c$aid", r.cover ?: "")
                    ed?.apply()
                }
            } catch (e: Exception) {
                posters[aid] = ""
                covers[aid] = ""
            } finally {
                inFlight.remove(aid)
            }
        }
    }
}
````

### `app/src/main/java/com/onimeku/app/data/State.kt` **[DIUBAH]**

````kotlin
package com.onimeku.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class Dl(val id: String, val ep: Int)
@Serializable data class Last(val id: String, val ep: Int)

/** Sesi akun yang sedang masuk (null = tamu). */
@Serializable data class Account(val userId: String, val email: String, val accessToken: String, val refreshToken: String)

@Serializable
data class Profile(
    val name: String,
    val myList: List<String> = emptyList(),
    val prog: Map<String, Int> = emptyMap(),
    val seen: List<String> = emptyList(),
    val last: Last? = null,
    val base: String? = null,
    val remind: List<String> = emptyList(),
)

@Serializable
data class Settings(
    val autoplay: Boolean = true,
    val skipIntro: Boolean = true,
    val wifi: Boolean = false,
    val alerts: Boolean = true,
    val quality: String = "Auto",
    val bgPlay: Boolean = true,
    // Bahasa antarmuka: "en" atau "id" (debug.31). Default Inggris agar
    // keadaan lama tidak berubah; dipilih di Profil -> Language.
    val lang: String = "en",
    // Bahasa subtitle pemutar: "auto" (ikut bahasa aplikasi, Indonesia/English), "id", "en", atau "off".
    val subLang: String = "auto",
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
````

### `app/src/main/java/com/onimeku/app/data/Sync.kt`

````kotlin
package com.onimeku.app.data

import com.onimeku.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Sinkronisasi My List + progres tonton ke Supabase (tabel `user_state`,
 * RLS per auth.uid(); skema di backend/supabase/schema.sql). Satu baris per
 * pengguna berisi daftar profil. Penggabungan bersifat gabung-tambah
 * (union): item yang dihapus di satu perangkat bisa muncul lagi dari perangkat lain.
 */
object Sync {
    class Unauthorized : Exception("unauthorized")

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val JSON = "application/json".toMediaType()
    private val base get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    val enabled: Boolean get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()

    private fun req(path: String, acc: Account) = Request.Builder().url(base + path)
        .header("apikey", BuildConfig.SUPABASE_KEY)
        .header("Authorization", "Bearer ${acc.accessToken}")

    suspend fun pull(acc: Account): List<Profile>? = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext null
        Net.client.newCall(req("/rest/v1/user_state?user_id=eq.${acc.userId}&select=data", acc).get().build()).execute().use { r ->
            if (r.code == 401) throw Unauthorized()
            if (!r.isSuccessful) return@withContext null
            val arr = json.parseToJsonElement(r.body?.string().orEmpty()).jsonArray
            val data = arr.firstOrNull()?.jsonObject?.get("data")?.jsonObject?.get("profiles") ?: return@withContext null
            json.decodeFromJsonElement(ListSerializer(Profile.serializer()), data)
        }
    }

    suspend fun push(acc: Account, profiles: List<Profile>) = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext
        val body = buildJsonObject {
            put("user_id", acc.userId)
            put("data", buildJsonObject {
                put("profiles", json.encodeToJsonElement(ListSerializer(Profile.serializer()), profiles))
            })
        }.toString()
        val rq = req("/rest/v1/user_state?on_conflict=user_id", acc)
            .header("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(body.toRequestBody(JSON)).build()
        Net.client.newCall(rq).execute().use { r ->
            if (r.code == 401) throw Unauthorized()
        }
    }

    /** Gabungkan profil lokal dengan profil jarak jauh (cocok per nama, maks 4 profil). */
    fun merge(local: List<Profile>, remote: List<Profile>): List<Profile> {
        val byName = remote.associateBy { it.name }
        val merged = local.map { l ->
            val r = byName[l.name] ?: return@map l
            val seen = (l.seen + r.seen).distinct()
            val prog = (l.prog.keys + r.prog.keys)
                .filter { it !in seen }
                .associateWith { k -> maxOf(l.prog[k] ?: 0, r.prog[k] ?: 0) }
            l.copy(
                myList = (l.myList + r.myList).distinct(),
                prog = prog, seen = seen,
                remind = (l.remind + r.remind).distinct(),
                last = l.last ?: r.last, base = l.base ?: r.base,
            )
        }
        val extra = remote.filter { r -> local.none { it.name == r.name } }
        return (merged + extra).take(4)
    }
}
````

### `app/src/main/java/com/onimeku/app/ui/AuthScreen.kt`

````kotlin
package com.onimeku.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.AppVM
import com.onimeku.app.I18n
import com.onimeku.app.Route
import com.onimeku.app.data.AuthClient

private val EmailRx = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

@Composable
private fun AuthField(
    label: String, value: String, onChange: (String) -> Unit, error: String?,
    keyboard: KeyboardType, ime: ImeAction, password: Boolean = false, onDone: () -> Unit = {},
) {
    var show by rememberSaveable { mutableStateOf(false) }
    val bd by animateColorAsState(if (error != null) RedText else Line, label = "fieldBd")
    Column(Modifier.padding(bottom = 14.dp)) {
        Text(label, fontSize = 13.sp, color = InkSoft, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Row(
            Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp)).background(Bg2)
                .border(1.dp, bd, RoundedCornerShape(16.dp)).padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value, onChange, Modifier.weight(1f), singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp), cursorBrush = SolidColor(Color.White),
                visualTransformation = if (password && !show) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard, imeAction = ime),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }, onNext = { }),
            )
            if (password) Box(Modifier.size(40.dp).clip(CircleShape).clickable { show = !show }, contentAlignment = Alignment.Center) {
                Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (show) "Hide password" else "Show password",
                    Modifier.size(20.dp), tint = NavOff)
            }
        }
        if (error != null) Text(error, fontSize = 12.sp, color = RedText, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
    }
}

/** Halaman Sign in / Sign up (email + kata sandi). */
@Composable
fun AuthScreen(vm: AppVM, startSignUp: Boolean) {
    var signUp by rememberSaveable { mutableStateOf(startSignUp) }
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var tried by rememberSaveable { mutableStateOf(false) }

    val emailErr = if (tried && !EmailRx.matches(email.trim())) "Enter a valid email address." else null
    val passErr = if (tried && pass.length < 8) "Use at least 8 characters." else null
    val confErr = if (tried && signUp && confirm != pass) "Passwords do not match." else null

    fun submit() {
        tried = true
        if (!EmailRx.matches(email.trim()) || pass.length < 8 || (signUp && confirm != pass)) return
        if (signUp) vm.signUp(email, pass) else vm.signIn(email, pass)
    }

    Page(extraTop = 8.dp) {
        SubHead("", vm)
        Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp, bottom = 14.dp)) { Logo(64.dp) }
        Text(if (signUp) I18n.t("create_account_t", "Create your account") else I18n.t("welcome_back", "Welcome back"), Modifier.fillMaxWidth(), fontSize = 28.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            if (signUp) "Save your list and keep watching on any device." else "Sign in to sync your list and progress.",
            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 26.dp), color = InkSoft, fontSize = 14.sp, textAlign = TextAlign.Center,
        )

        if (!AuthClient.configured) {
            CardBox(Modifier.padding(bottom = 18.dp)) {
                Text("Accounts are not available in this build yet. You can keep using Onimeku as a guest.",
                    Modifier.padding(16.dp), color = InkSoft, fontSize = 13.5.sp)
            }
        }

        AuthField(I18n.t("email", "Email"), email, { email = it }, emailErr, KeyboardType.Email, ImeAction.Next)
        AuthField(I18n.t("password", "Password"), pass, { pass = it }, passErr, KeyboardType.Password, if (signUp) ImeAction.Next else ImeAction.Done,
            password = true, onDone = { submit() })
        if (signUp) AuthField("Confirm password", confirm, { confirm = it }, confErr, KeyboardType.Password, ImeAction.Done,
            password = true, onDone = { submit() })

        vm.authError?.let {
            Text(it, color = RedText, fontSize = 13.5.sp, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
        }

        Box(Modifier.padding(top = 6.dp)) {
            Btn(if (vm.authBusy) I18n.t("please_wait", "Please wait…") else if (signUp) I18n.t("create_account_b", "Create account") else I18n.t("sign_in", "Sign in"), BtnStyle.Play,
                modifier = Modifier.fillMaxWidth(), height = 54.dp) { if (!vm.authBusy) submit() }
            if (vm.authBusy) CircularProgressIndicator(Modifier.align(Alignment.CenterStart).padding(start = 22.dp).size(20.dp),
                color = Color.White, strokeWidth = 2.dp)
        }

        if (!signUp) {
            Text("Forgot password?", Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp).clip(RoundedCornerShape(8.dp))
                .clickable {
                    if (!EmailRx.matches(email.trim())) { tried = true; vm.toast("Enter your email first") } else vm.resetPassword(email)
                }.padding(horizontal = 10.dp, vertical = 6.dp), color = RedText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Row(Modifier.align(Alignment.CenterHorizontally).padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (signUp) "Already have an account? " else "New to Onimeku? ", color = InkSoft, fontSize = 14.sp)
            Text(if (signUp) I18n.t("sign_in", "Sign in") else I18n.t("sign_up", "Sign up"), color = RedText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { signUp = !signUp; tried = false; vm.authError = null }.padding(6.dp))
        }

        Text("By continuing you agree to our Terms of Service and Privacy Policy.", Modifier.fillMaxWidth().padding(top = 18.dp),
            color = NavOff, fontSize = 12.sp, textAlign = TextAlign.Center)
        Row(Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Terms", color = RedText, fontSize = 12.5.sp, modifier = Modifier.clickable { vm.go(Route.Sub("terms")) }.padding(6.dp))
            Text("Privacy", color = RedText, fontSize = 12.5.sp, modifier = Modifier.clickable { vm.go(Route.Sub("privacy")) }.padding(6.dp))
        }
    }
}
````

### `app/src/main/java/com/onimeku/app/ui/Components.kt` **[DIUBAH]**

````kotlin
package com.onimeku.app.ui

import androidx.compose.foundation.lazy.grid.itemsIndexed
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.AppVM
import com.onimeku.app.I18n
import com.onimeku.app.Route
import com.onimeku.app.data.Anime

val LocalEnsureNotif = staticCompositionLocalOf<((Boolean) -> Unit) -> Unit> { { cb -> cb(false) } }

fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

fun fmt(t: Int): String = "${t / 60}:${(t % 60).toString().padStart(2, '0')}"

@Composable
fun Logo(size: Dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val k = this.size.width / 60f
        rotate(45f) {
            drawRoundRect(LogoRed, Offset(6 * k, 6 * k), Size(48 * k, 48 * k), CornerRadius(16 * k))
        }
        drawCircle(Color.White, radius = 10 * k, style = Stroke(6 * k))
    }
}

@Composable
fun Tag(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier, small: Boolean = false) {
    Text(text, color = fg, fontSize = if (small) 11.sp else 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
        modifier = modifier.clip(RoundedCornerShape(if (small) 6.dp else 7.dp)).background(bg)
            .padding(horizontal = if (small) 7.dp else 9.dp, vertical = if (small) 2.dp else 3.dp))
}

@Composable
fun Chip(text: String, on: Boolean = false, minW: Dp = 0.dp, icon: ImageVector? = null, onClick: () -> Unit) {
    val fg = if (on) RedText else Color.White
    Row(
        Modifier.height(34.dp).widthIn(min = minW).clip(CircleShape).background(Bg2)
            .border(1.dp, if (on) Red else Line, CircleShape).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(16.dp), tint = fg)
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun IconChip(icon: ImageVector, desc: String, on: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.size(width = 40.dp, height = 36.dp).clip(CircleShape).background(Bg2)
            .border(1.dp, if (on) Red else Line, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(18.dp), tint = if (on) RedText else Color.White) }
}

enum class BtnStyle { Play, Solid, Outline, OutlineOn }

@Composable
fun Btn(text: String, style: BtnStyle, icon: ImageVector? = null, modifier: Modifier = Modifier, height: Dp = 48.dp, onClick: () -> Unit) {
    val (bg, fg, bd) = when (style) {
        BtnStyle.Play -> Triple(Red, Color.White, Red)
        BtnStyle.Solid -> Triple(Solid, Color.White, Solid)
        // Outline: kaca gelap tipis + garis halus (dulu cincin putih tebal 3dp yang terasa kasar).
        BtnStyle.Outline -> Triple(Color(0x40000000), Color.White, Color(0xB3FFFFFF))
        BtnStyle.OutlineOn -> Triple(Color.White, Bg, Color.White)
    }
    Row(
        modifier.pressable(onClick).height(height).clip(CircleShape).background(bg).border(1.5.dp, bd, CircleShape)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = fg)
        Text(text, color = fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/**
 * Latar polos gelap untuk area gambar. Dulu ini gradasi buatan ala poster
 * contoh — dibuang: aplikasi hanya menampilkan gambar ASLI anime. Selama
 * poster asli belum termuat, area ini kosong polos, bukan gambar palsu.
 */
fun Modifier.artBackground(a: Anime): Modifier = this.background(Bg2)

/**
 * Gambar ASLI anime untuk thumbnail/header lebar (cover, jatuh ke poster).
 * Tidak ada gambar buatan: sebelum gambar termuat, area tetap polos gelap.
 */
@Composable
fun RealImage(a: Anime, modifier: Modifier = Modifier) {
    val img = com.onimeku.app.data.PosterHealer.coverFor(a) ?: com.onimeku.app.data.PosterHealer.posterFor(a)
    val req = rememberImg(img)
    val done = remember(a.id, img) { mutableStateOf(false) }
    Box(modifier) {
        // Kilau selama gambar dimuat; crossfade saat tiba.
        if (!done.value && (req != null || !a.poster.isNullOrBlank() || !a.banner.isNullOrBlank())) Box(Modifier.fillMaxSize().shimmer())
        if (req != null) {
            coil.compose.AsyncImage(
                model = req, contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                onSuccess = { done.value = true },
                onError = { done.value = true },
            )
        }
    }
}

/** Teks status yang mengikuti bahasa aktif (nilai mentah "Airing"/"Completed" tetap dipakai untuk filter). */
fun statusText(status: String): String = I18n.t("st_" + status.lowercase(), status)

val railW: Dp @Composable get() = (LocalConfiguration.current.screenWidthDp * 0.4f).coerceIn(142f, 230f).dp

@Composable
fun Poster(
    a: Anime, w: Dp?, modifier: Modifier = Modifier, ep: Boolean = false, badges: Boolean = false,
    label: Boolean = false, rank: Int? = null, meta: Pair<String, String>? = null,
    progress: Float? = null, compact: Boolean = false, sharedKey: String? = null, onClick: () -> Unit,
) {
    val sized = if (w != null) modifier.width(w) else modifier.fillMaxWidth()
    Box(
        sized.liftPress { if (sharedKey != null) SharedArt.key = sharedKey; onClick() }.aspectRatio(if (ep) 232f / 150f else 232f / 311f)
            .sharedArt(sharedKey)
            .clip(RoundedCornerShape(if (compact) 12.dp else 18.dp)).artBackground(a),
    ) {
        val img = if (ep) {
            com.onimeku.app.data.PosterHealer.coverFor(a) ?: com.onimeku.app.data.PosterHealer.posterFor(a)
        } else {
            com.onimeku.app.data.PosterHealer.posterFor(a)
        }
        // Kilau selama poster dimuat; hilang saat gambar tiba atau gagal. Crossfade 350 ms saat tampil.
        val imgDone = remember(a.id, img) { mutableStateOf(false) }
        val req = rememberImg(img)
        if (!imgDone.value && (req != null || !a.poster.isNullOrBlank())) {
            Box(Modifier.fillMaxSize().shimmer())
        }
        if (req != null) {
            coil.compose.AsyncImage(
                model = req, contentDescription = a.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                onSuccess = { imgDone.value = true },
                onError = { imgDone.value = true },
            )
        }
        if (badges) {
            a.rating?.let { Tag(it, Color.White, Bg, Modifier.align(Alignment.TopStart).padding(if (compact) 6.dp else 12.dp), small = compact) }
            if (a.hd) Tag("HD", Red, Color.White, Modifier.align(Alignment.TopEnd).padding(if (compact) 6.dp else 10.dp), small = compact)
        }
        if (rank != null) {
            val big = 60.sp
            Box(Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 2.dp).popIn(120 + rank * 70)) {
                Text("$rank", fontSize = big, fontWeight = FontWeight.Bold, color = Bg,
                    style = TextStyle(drawStyle = Stroke(width = 14f)), lineHeight = big)
                Text("$rank", fontSize = big, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = big)
            }
        }
        if (meta != null) {
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD90A0B10))))
                    .padding(start = 12.dp, end = 12.dp, top = 26.dp, bottom = 10.dp),
            ) {
                Text(meta.first, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(meta.second, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (progress != null) {
            AnimatedProgressBar(progress, Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp))
        }
    }
}

/** Poster + judul di bawahnya (dua baris tetap, agar baris rail sejajar rapi). */
@Composable
fun PosterCard(a: Anime, w: Dp, rank: Int? = null, sharedKey: String? = null, index: Int = 0, onClick: () -> Unit) {
    Column(Modifier.width(w).let { m -> if (sharedKey != null) m.staggerIn(sharedKey, index) else m }) {
        Poster(a, w, badges = true, rank = rank, sharedKey = sharedKey, onClick = onClick)
        Text(a.title, Modifier.padding(top = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp,
            minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Kartu untuk grid (3 kolom di ponsel): poster ringkas + judul + genre. */
@Composable
fun Tile(a: Anime, vm: AppVM, index: Int = 0) {
    Column(Modifier.staggerIn("grid:${a.id}", index)) {
        Poster(a, null, badges = true, compact = true, sharedKey = "grid:${a.id}") { vm.go(Route.Detail(a.id)) }
        Text(a.title, Modifier.padding(top = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 15.sp,
            minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(a.genres.take(2).joinToString(", "), fontSize = 11.sp, color = NavOff, modifier = Modifier.padding(top = 2.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Jarak konten ke bawah supaya tidak tertutup tab bar + navigation bar sistem. */
val TabBarHeight: Dp = 68.dp

@Composable
fun bottomContentPadding(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + TabBarHeight + 16.dp

/**
 * Grid anime berbasis LazyVerticalGrid: hanya sel yang terlihat yang digambar,
 * jadi "See all", Search, dan My List tetap ringan walau katalog ribuan judul.
 * [header] tampil satu baris penuh di atas grid.
 */
@Composable
fun AnimeGrid(items: List<Anime>, vm: AppVM, header: @Composable () -> Unit) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(100.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 16.dp, bottom = bottomContentPadding()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { Column { header() } }
        itemsIndexed(items, key = { _, it -> it.id }) { i, a -> Box(Modifier.animateItem()) { Tile(a, vm, i % 9) } }
    }
}

@Composable
fun Rail(content: LazyListScope.() -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp), content = content)
}

@Composable
fun SectionHead(title: String, onAll: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(end = 10.dp).width(4.dp).height(20.dp).growIn().clip(CircleShape).background(Red))
        Text(title, Modifier.weight(1f).padding(end = 12.dp), fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (onAll != null) Row(
            Modifier.clip(CircleShape).clickable(onClick = onAll).padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(I18n.t("see_all", "See all"), color = RedText, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp), tint = RedText)
        }
    }
}

@Composable
fun Page(extraTop: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = top + extraTop, bottom = bottomContentPadding()),
        content = content,
    )
}

@Composable
fun SubHead(title: String, vm: AppVM) {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).clickable { vm.back() }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
        }
        Spacer(Modifier.width(6.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RootTitle(title: String, lead: String? = null) {
    Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
    if (lead != null) Text(lead, color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
}

@Composable
fun EmptyBox(title: String, text: String, action: (@Composable () -> Unit)? = null) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.padding(top = 28.dp).fillMaxWidth().clip(shape).background(Bg2).border(1.dp, Line, shape).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.padding(bottom = 14.dp).size(56.dp).clip(CircleShape).background(Color(0x26A6122D)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Inbox, null, Modifier.size(28.dp), tint = RedText)
        }
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(text, color = InkSoft, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
        action?.invoke()
    }
}

@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(modifier.fillMaxWidth().clip(shape).background(Bg2).border(1.dp, Line, shape), content = content)
}

@Composable
fun HLine() = Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
````

### `app/src/main/java/com/onimeku/app/ui/LegalScreens.kt`

````kotlin
package com.onimeku.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Sec(val h: String, val body: String)

// CATATAN: teks ini TEMPLATE yang disesuaikan dengan perilaku aplikasi.
// Tinjau oleh penasihat hukum dan isi kontak resmi sebelum rilis publik.
private val terms = listOf(
    Sec("Using Onimeku", "Onimeku lets you browse and stream anime and keep a personal list. You must follow these terms and the laws that apply to you. Do not misuse the app, try to disrupt it, or copy its content in bulk."),
    Sec("Accounts", "An account is optional. If you create one, keep your password private. You are responsible for activity under your account. You can stop using the app at any time."),
    Sec("Content", "Titles, images and videos shown in Onimeku belong to their respective owners. Availability can change without notice, and some titles may not be available in every region."),
    Sec("Availability", "We work to keep Onimeku running but cannot promise it will always be available or error-free. Features may change or be removed."),
    Sec("Changes", "We may update these terms. Continuing to use the app after an update means you accept the new terms."),
)

private val privacy = listOf(
    Sec("What stays on your device", "Your profiles, My List, watch progress, search history, reminders, settings and offline downloads are stored on your device."),
    Sec("Account data", "If you create an account, your email address and an encrypted password are handled by our authentication provider (Supabase) so you can sign in. We do not see your plain password."),
    Sec("Network requests", "To show the catalog and play episodes, the app contacts our API and streaming hosts. These servers may receive technical data such as your IP address and device type, as with any internet service."),
    Sec("Notifications", "If you allow notifications, the app uses them for episode reminders and download progress. You can turn them off in Settings or in your device settings."),
    Sec("No ads, no selling data", "Onimeku does not show ads and does not sell your personal data."),
    Sec("Your choices", "You can clear your history and remove downloads inside the app, sign out at any time, and uninstall the app to remove local data. To ask about your account data, contact us through the Help Center."),
)

private val licenses = listOf(
    Sec("Jetpack Compose, Material 3, AndroidX", "Copyright The Android Open Source Project. Licensed under the Apache License 2.0."),
    Sec("Media3 (ExoPlayer)", "Copyright The Android Open Source Project. Licensed under the Apache License 2.0."),
    Sec("OkHttp", "Copyright Square, Inc. Licensed under the Apache License 2.0."),
    Sec("Coil", "Copyright Coil Contributors. Licensed under the Apache License 2.0."),
    Sec("kotlinx.serialization and Kotlin", "Copyright JetBrains s.r.o. Licensed under the Apache License 2.0."),
    Sec("WorkManager", "Copyright The Android Open Source Project. Licensed under the Apache License 2.0."),
)

@Composable
fun LegalContent(key: String) {
    val secs = when (key) { "terms" -> terms; "privacy" -> privacy; else -> licenses }
    Text(if (key == "licenses") "Onimeku is built with these open-source projects." else "Last updated: October 2026",
        Modifier.padding(start = 4.dp, bottom = 12.dp), color = InkSoft, fontSize = 13.sp)
    CardBox {
        secs.forEachIndexed { i, s ->
            if (i > 0) HLine()
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
                Text(s.h, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp)
                Text(s.body, Modifier.padding(top = 6.dp), color = InkSoft, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
    }
}
````

### `app/src/main/java/com/onimeku/app/ui/Motion.kt` **[DIUBAH]**

````kotlin
package com.onimeku.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Motion & loading primitives dipakai bersama oleh semua layar.
// ---------------------------------------------------------------------------

private val ShimmerBase = Color(0xFF1E2130)
private val ShimmerHi = Color(0xFF2C3044)

/** Kilau bergerak (skeleton loading). Hanya fase gambar yang dihitung ulang tiap frame. */
@Composable
fun Modifier.shimmer(): Modifier {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "shimmerX",
    )
    return this.drawBehind {
        val w = size.width
        drawRect(
            Brush.linearGradient(
                colors = listOf(ShimmerBase, ShimmerHi, ShimmerBase),
                start = Offset(x * w, 0f),
                end = Offset(x * w + w * 0.7f, size.height),
            ),
        )
    }
}

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, radius: Dp = 12.dp) {
    Box(modifier.clip(RoundedCornerShape(radius)).shimmer())
}

/** Klik dengan efek tekan (skala pegas) — terasa "hidup" tanpa ripple. */
@Composable
fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val reduceP = LocalReduceMotion.current
    val sc by animateFloatAsState(
        targetValue = if (pressed && !reduceP) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    return this
        .graphicsLayer { scaleX = sc; scaleY = sc }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}

/** Muncul pertama kali: memudar + naik halus, bisa diberi jeda untuk efek berurutan. */
@Composable
fun Modifier.fadeSlideIn(delayMs: Int = 0): Modifier {
    val reduce = LocalReduceMotion.current
    var shown by remember { mutableStateOf(reduce) }
    LaunchedEffect(Unit) { shown = true }
    val a by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = if (reduce) 0 else 450, delayMillis = if (reduce) 0 else delayMs),
        label = "enter",
    )
    return this.graphicsLayer { alpha = a; translationY = (1f - a) * 36f }
}

/** Permintaan gambar Coil dengan crossfade, supaya poster tidak "pop" saat selesai dimuat. */
@Composable
fun rememberImg(url: String?): ImageRequest? {
    val ctx = LocalContext.current
    return remember(url) {
        if (url.isNullOrBlank()) null
        else ImageRequest.Builder(ctx).data(url).crossfade(350).build()
    }
}

/** Kerangka Home saat katalog pertama kali dimuat. */
@Composable
fun HomeSkeleton(heroHeight: Dp, posterWidth: Dp, bottomPad: Dp) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = bottomPad)) {
        Box(Modifier.fillMaxWidth().height(heroHeight).shimmer())
        repeat(3) {
            Spacer(Modifier.height(26.dp))
            SkeletonBox(Modifier.padding(horizontal = 16.dp).width(170.dp).height(22.dp), 8.dp)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    Column(Modifier.width(posterWidth)) {
                        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(232f / 311f), 18.dp)
                        Spacer(Modifier.height(8.dp))
                        SkeletonBox(Modifier.fillMaxWidth(0.8f).height(12.dp), 6.dp)
                    }
                }
            }
        }
    }
}

/** Baris kerangka (thumbnail + dua teks) untuk daftar yang masih memuat. */
@Composable
fun SkeletonRows(count: Int = 4) {
    repeat(count) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBox(Modifier.width(96.dp).aspectRatio(16f / 10f), 12.dp)
            Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(Modifier.fillMaxWidth(0.85f).height(14.dp), 6.dp)
                SkeletonBox(Modifier.fillMaxWidth(0.5f).height(11.dp), 6.dp)
            }
        }
    }
}


// ====================================================================== GERAK LANJUTAN
/** True bila pengguna mematikan animasi di Pengaturan Android (Opsi pengembang / Aksesibilitas). */
val LocalReduceMotion = androidx.compose.runtime.staticCompositionLocalOf { false }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = androidx.compose.runtime.staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimScope = androidx.compose.runtime.staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Kunci elemen bersama terakhir yang diketuk (poster -> hero Detail). */
object SharedArt { var key: String? = null }

/** Elemen bersama: poster "terbang" ke hero Detail dan kembali. Tanpa efek bila kunci/scope tidak ada. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedArt(key: String?): Modifier {
    val ss = LocalSharedScope.current
    val av = LocalAnimScope.current
    if (key == null || ss == null || av == null) return this
    return with(ss) { this@sharedArt.sharedElement(rememberSharedContentState(key = key), animatedVisibilityScope = av) }
}

object Entered { val keys = HashSet<String>() }

/** Muncul berurutan (stagger) hanya sekali per item selama aplikasi hidup; tidak mengulang saat digulir kembali. */
@Composable
fun Modifier.staggerIn(key: String, index: Int): Modifier {
    val reduce = LocalReduceMotion.current
    val already = remember(key) { reduce || key in Entered.keys }
    var shown by remember(key) { mutableStateOf(already) }
    LaunchedEffect(key) { Entered.keys.add(key); shown = true }
    val a by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = if (already) 0 else 380, delayMillis = if (already) 0 else minOf(index, 8) * 45),
        label = "stagger",
    )
    return this.graphicsLayer { alpha = a; translationY = (1f - a) * 28f; val s = 0.94f + 0.06f * a; scaleX = s; scaleY = s }
}

/** Ikon yang berubah bentuk dengan pantulan pegas (mis. + menjadi centang). */
@Composable
fun MorphIcon(on: Boolean, onIcon: ImageVector, offIcon: ImageVector, desc: String, size: Dp, tint: Color) {
    val reduce = LocalReduceMotion.current
    AnimatedContent(
        targetState = on,
        transitionSpec = {
            if (reduce) EnterTransition.None togetherWith ExitTransition.None
            else (scaleIn(spring(0.45f, 450f), initialScale = 0.3f) + fadeIn(tween(120))) togetherWith
                (scaleOut(tween(100), targetScale = 0.6f) + fadeOut(tween(80)))
        },
        label = "morph",
    ) { st -> Icon(if (st) onIcon else offIcon, desc, Modifier.size(size), tint = tint) }
}


// ====================================================================== GERAK BERANDA (v2)
/** Zoom sangat pelan bolak-balik pada gambar hero (efek sinematik "Ken Burns"). */
@Composable
fun Modifier.kenBurns(): Modifier {
    if (LocalReduceMotion.current) return this
    val t = rememberInfiniteTransition(label = "kenBurns")
    val s by t.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(9000, easing = androidx.compose.animation.core.FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "kenBurnsScale",
    )
    return this.graphicsLayer { scaleX = s; scaleY = s }
}

/** Kilau cahaya miring yang menyapu tombol utama tiap beberapa detik. */
@Composable
fun Modifier.sweepShine(): Modifier {
    if (LocalReduceMotion.current) return this
    val t = rememberInfiniteTransition(label = "shine")
    val x by t.animateFloat(
        initialValue = -0.6f, targetValue = 1.8f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "shineX",
    )
    return this.drawWithContent {
        drawContent()
        val w = size.width
        val band = w * 0.28f
        val x0 = x * w
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color.Transparent, Color(0x40FFFFFF), Color.Transparent),
                start = Offset(x0, 0f), end = Offset(x0 + band, size.height * 0.6f),
            ),
        )
    }
}

/** Panah kecil yang bergoyang ke kanan, mengajak mengetuk. */
@Composable
fun Modifier.nudgeX(): Modifier {
    if (LocalReduceMotion.current) return this
    val t = rememberInfiniteTransition(label = "nudge")
    val v by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750, easing = androidx.compose.animation.core.FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "nudgeV",
    )
    return this.graphicsLayer { translationX = v * 5.dp.toPx() }
}

/** Titik status "Airing": inti hijau + gelombang yang memudar keluar. */
@Composable
fun PulseDot(color: Color) {
    val reduce = LocalReduceMotion.current
    val t = rememberInfiniteTransition(label = "pulse")
    val p by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "pulseP",
    )
    Box(Modifier.size(6.dp)) {
        if (!reduce) Box(
            Modifier.fillMaxSize().graphicsLayer { val s = 1f + 1.8f * p; scaleX = s; scaleY = s; alpha = 0.55f * (1f - p) }
                .clip(androidx.compose.foundation.shape.CircleShape).background(color),
        )
        Box(Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape).background(color))
    }
}

/** Titik indikator hero: yang aktif memanjang dan terisi pelan sesuai waktu sebelum slide berganti (5,5 dtk). */
@Composable
fun HeroDot(selected: Boolean, page: Int) {
    val reduce = LocalReduceMotion.current
    val w by animateDpAsState(if (selected) 28.dp else 6.dp, spring(dampingRatio = 0.7f, stiffness = 500f), label = "dotW")
    val prog = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(selected, page) {
        if (selected && !reduce) { prog.snapTo(0f); prog.animateTo(1f, tween(5500, easing = LinearEasing)) }
        else prog.snapTo(if (selected) 1f else 0f)
    }
    Box(
        Modifier.height(6.dp).width(w).clip(RoundedCornerShape(50)).background(Color(0x59FFFFFF))
            .drawBehind { drawRect(RedText, size = androidx.compose.ui.geometry.Size(size.width * prog.value, size.height)) },
    )
}

/** Sekali per tampilan: bilah merah kecil di samping judul bagian tumbuh dari nol. */
@Composable
fun Modifier.growIn(delayMs: Int = 0): Modifier {
    val reduce = LocalReduceMotion.current
    var shown by remember { mutableStateOf(reduce) }
    LaunchedEffect(Unit) { shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(if (reduce) 0 else 520, delayMillis = if (reduce) 0 else delayMs, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "grow")
    return this.graphicsLayer { scaleX = a; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }
}

/**
 * Pita genre bergulir otomatis (marquee). Dua baris berlawanan arah; ketuk pita untuk membuka genre.
 * Berhenti otomatis bila pengguna mematikan animasi sistem.
 */
@Composable
fun GenreMarquee(genres: List<String>, onClick: (String) -> Unit) {
    if (genres.isEmpty()) return
    val half = genres.size / 2
    val rows = listOf(genres.take(maxOf(half, 1)) to 1, genres.drop(maxOf(half, 1)).ifEmpty { genres.reversed() } to -1)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEachIndexed { r, (list, dir) -> MarqueeRow(list, dir, 26f + r * 8f, onClick) }
    }
}

@Composable
private fun MarqueeRow(list: List<String>, dir: Int, dpPerSec: Float, onClick: (String) -> Unit) {
    val reduce = LocalReduceMotion.current
    val n = list.size
    val start = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % n
    val state = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = start)
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { dpPerSec.dp.toPx() }
    LaunchedEffect(n, reduce) {
        if (reduce) return@LaunchedEffect
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) state.dispatchRawDelta(dir * px * (now - last) / 1_000_000_000f)
                last = now
            }
        }
    }
    androidx.compose.foundation.lazy.LazyRow(
        state = state, userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(Int.MAX_VALUE) { i ->
            val g = list[i % n]
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Bg2).border(1.dp, Line, RoundedCornerShape(50))
                    .pressable { onClick(g) }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(RedText))
                androidx.compose.material3.Text(g, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = Color.White, maxLines = 1)
            }
        }
    }
}


// ====================================================================== GERAK LANJUTAN (v3)
/** Muncul dengan pantulan pegas (skala 0,3 -> 1) setelah jeda; dipakai angka ranking Top Hits. */
@Composable
fun Modifier.popIn(delayMs: Int = 0): Modifier {
    val reduce = LocalReduceMotion.current
    var shown by remember { mutableStateOf(reduce) }
    LaunchedEffect(Unit) { shown = true }
    val sc by animateFloatAsState(
        targetValue = if (shown) 1f else 0.3f,
        animationSpec = if (reduce) tween(0) else spring(dampingRatio = 0.45f, stiffness = 260f),
        label = "popScale",
    )
    val al by animateFloatAsState(if (shown) 1f else 0f, tween(if (reduce) 0 else 220, delayMillis = if (reduce) 0 else delayMs), label = "popAlpha")
    return this.graphicsLayer {
        scaleX = sc; scaleY = sc; alpha = al
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)
    }
}

/** Klik dengan efek "terangkat": mengecil + sedikit miring ke belakang saat ditekan, lalu memantul kembali. */
@Composable
fun Modifier.liftPress(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val reduce = LocalReduceMotion.current
    val sc by animateFloatAsState(if (pressed && !reduce) 0.94f else 1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "liftS")
    val tilt by animateFloatAsState(if (pressed && !reduce) 7f else 0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "liftT")
    return this
        .graphicsLayer { scaleX = sc; scaleY = sc; rotationX = tilt; cameraDistance = 14f * density }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}

/** Percikan partikel kecil sekali saat [trigger] berubah dari false ke true (mis. judul masuk My List). */
@Composable
fun Modifier.sparkBurst(trigger: Boolean): Modifier {
    val reduce = LocalReduceMotion.current
    val anim = remember { androidx.compose.animation.core.Animatable(1f) }
    var prev by remember { mutableStateOf(trigger) }
    LaunchedEffect(trigger) {
        if (trigger && !prev && !reduce) { anim.snapTo(0f); anim.animateTo(1f, tween(650, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
        prev = trigger
    }
    return this.drawWithContent {
        drawContent()
        val p = anim.value
        if (p < 1f) {
            val c = Offset(size.width / 2f, size.height * 0.38f)
            val maxD = 34.dp.toPx()
            for (i in 0 until 10) {
                val ang = Math.toRadians(i * 36.0 + 12.0)
                val d = 10.dp.toPx() + (maxD - 10.dp.toPx()) * p * (if (i % 2 == 0) 1f else 0.7f)
                drawCircle(
                    color = (if (i % 3 == 0) Gold else RedText).copy(alpha = (1f - p).coerceIn(0f, 1f)),
                    radius = (if (i % 2 == 0) 3.2.dp else 2.dp).toPx() * (1f - 0.6f * p),
                    center = Offset(c.x + (kotlin.math.cos(ang) * d).toFloat(), c.y + (kotlin.math.sin(ang) * d).toFloat()),
                )
            }
        }
    }
}

/** Bilah progres yang terisi dengan animasi dari 0 ke [value] saat pertama tampil. */
@Composable
fun AnimatedProgressBar(value: Float, modifier: Modifier = Modifier, color: Color = Red, track: Color = Color(0x40FFFFFF)) {
    val reduce = LocalReduceMotion.current
    val a = remember { androidx.compose.animation.core.Animatable(if (reduce) value else 0f) }
    LaunchedEffect(value) { a.animateTo(value.coerceIn(0f, 1f), tween(if (reduce) 0 else 900, delayMillis = if (reduce) 0 else 250, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
    Box(modifier.background(track).drawBehind { drawRect(color, size = androidx.compose.ui.geometry.Size(size.width * a.value, size.height)) })
}
````

### `app/src/main/java/com/onimeku/app/ui/PlayerScreen.kt` **[DIUBAH]**

````kotlin
package com.onimeku.app.ui

import android.app.PictureInPictureParams
import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.content.pm.ActivityInfo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.onimeku.app.AppVM
import com.onimeku.app.I18n
import com.onimeku.app.PlaybackService
import com.onimeku.app.Route
import com.onimeku.app.data.Catalog
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val PIP_ACTION = "com.onimeku.app.PIP_CONTROL"

private fun pipAction(ctx: Context, op: String, icon: Int, title: String, code: Int): android.app.RemoteAction {
    val pi = android.app.PendingIntent.getBroadcast(
        ctx, code, android.content.Intent(PIP_ACTION).setPackage(ctx.packageName).putExtra("op", op),
        android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
    )
    return android.app.RemoteAction(android.graphics.drawable.Icon.createWithResource(ctx, icon), title, title, pi)
}

/** Parameter PiP ala YouTube: rasio mengikuti video, tombol sebelumnya/putar-jeda/berikutnya, transisi halus dari area video. */
private fun pipParams(
    ctx: Context, auto: Boolean, playing: Boolean, ratio: Float = 16f / 9f,
    rect: android.graphics.Rect? = null, hasPrev: Boolean = false, hasNext: Boolean = false,
): PictureInPictureParams {
    val r = ratio.coerceIn(0.5f, 2.3f)
    val b = PictureInPictureParams.Builder().setAspectRatio(Rational((r * 1000).toInt(), 1000))
    val acts = ArrayList<android.app.RemoteAction>()
    if (hasPrev) acts.add(pipAction(ctx, "prev", com.onimeku.app.R.drawable.ic_pip_prev, "Previous", 1))
    acts.add(pipAction(ctx, "toggle", if (playing) com.onimeku.app.R.drawable.ic_pip_pause else com.onimeku.app.R.drawable.ic_pip_play, if (playing) "Pause" else "Play", 2))
    if (hasNext) acts.add(pipAction(ctx, "next", com.onimeku.app.R.drawable.ic_pip_next, "Next", 3))
    b.setActions(acts)
    if (rect != null && rect.width() > 0 && rect.height() > 0) b.setSourceRectHint(rect)
    if (Build.VERSION.SDK_INT >= 31) { b.setAutoEnterEnabled(auto); b.setSeamlessResizeEnabled(true) }
    return b.build()
}

private fun onWifi(ctx: Context): Boolean {
    val cm = ctx.getSystemService(ConnectivityManager::class.java)
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}

@Composable
private fun VideoSurface(player: Player, modifier: Modifier) {
    AndroidView(factory = { c -> PlayerView(c).also { it.player = player; it.useController = false } }, modifier = modifier)
}

@Composable
fun PlayerScreen(vm: AppVM, r: Route.Player) {
    val ctx = LocalContext.current
    if (vm.s.settings.wifi && !onWifi(ctx)) {
        Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().padding(16.dp)) {
            SubHead(I18n.t("wifi_only_title", "Wi-Fi only is on"), vm)
            Text(I18n.t("wifi_only_body", "Streaming over mobile data is turned off in Settings. Connect to Wi-Fi or turn the option off."), color = InkSoft)
            Box(Modifier.padding(top = 16.dp)) { Btn(I18n.t("open_settings", "Open Settings"), BtnStyle.Solid) { vm.openSettings() } }
        }
        return
    }
    PlayerContent(vm, r)
}

@Composable
private fun PlayerContent(vm: AppVM, r: Route.Player) {
    val a = Catalog.byId[r.id] ?: return
    val ctx = LocalContext.current
    val act = ctx.findActivity()
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val quality = vm.s.settings.quality

    // Data putar diambil dari API per episode (stream + referer + subtitle).
    var attempt by remember(r) { mutableIntStateOf(0) }
    var pb by remember(r) { mutableStateOf<com.onimeku.app.data.Playback?>(null) }
    var pbOffline by remember(r) { mutableStateOf(false) }
    var pbFailed by remember(r) { mutableStateOf(false) }
    var chainDead by remember(r) { mutableStateOf(false) }
    var useDirect by remember(r) { mutableStateOf(false) }
    var mainRetry by remember(r) { mutableIntStateOf(0) }
    LaunchedEffect(r, attempt) {
        pb = null; pbFailed = false; pbOffline = false
        // Batas waktu TOTAL memuat data putar (debug.30, dilonggarkan
        // debug.37): endpoint hulu kadang lambat/flaky (terukur 4,6 dtk
        // dan sesekali 503 untuk Bleach ep1) dan timeout baca per
        // percobaan 12 dtk x 2 putaran bisa membuat "Loading stream..."
        // menggantung di jaringan seluler. debug.37: data putar kini
        // dipanaskan sejak halaman Detail dibuka + endpoint di-cache di
        // tepi, jadi batas ini hanya jaring pengaman terakhir (35 dtk)
        // sebelum galat jujur + tombol coba lagi tampil.
        val res = kotlinx.coroutines.withTimeoutOrNull(35_000) {
            try { com.onimeku.app.data.ApiClient.fetchPlayback(r.id, r.ep) } catch (e: Exception) { null }
        }
        if (res == null) {
            // API tak terjangkau (offline): bila episode ini sudah terunduh
            // penuh, putar dari cache unduhan — unduhan harus benar-benar
            // bisa diputar tanpa jaringan.
            val dl = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.onimeku.app.data.Downloads.states(ctx)[com.onimeku.app.data.Downloads.key(r.id, r.ep)]
            }
            if (dl != null && dl.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED) {
                pbOffline = true
                pb = com.onimeku.app.data.Playback(streamUrl = dl.request.uri.toString(), episode = r.ep)
            } else {
                pbFailed = true
            }
        } else if (res.streamUrl.isNullOrBlank() && res.streamProxyUrl.isNullOrBlank() && res.embedUrl.isNullOrBlank()) {
            pbFailed = true
        } else {
            // Subtitle diunduh ke penyimpanan lokal lebih dulu (dengan Referer
            // per-trek dari API), lalu diputar dari file. Trek subtitle yang
            // ditautkan mentah ke pemutar bisa menggagalkan seluruh putar
            // bila server teksnya menolak permintaan pemutar.
            // Perbaikan agen (berulang dari v6): tipe eksplisit non-null —
            // var tertangkap closure + diubah isinya tidak bisa smart-cast.
            var ready: com.onimeku.app.data.Playback = res
            if (ready.subtitles.isNotEmpty()) {
                // Unduh semua trek PARALEL dengan batas waktu total 4 dtk dan
                // pakai ulang berkas yang sudah ada. Sebelumnya berurutan
                // (timeout 10+15 dtk per trek) sehingga start video bisa
                // tertahan puluhan detik hanya karena server subtitle lambat.
                val dir = java.io.File(ctx.cacheDir, "subs").apply { mkdirs() }
                val loaded = kotlinx.coroutines.withTimeoutOrNull(4_000) {
                    kotlinx.coroutines.coroutineScope {
                        ready.subtitles.mapIndexed { i, s ->
                            async(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val ext = listOf(".vtt", ".srt", ".ass", ".ssa").firstOrNull { s.url.contains(it) } ?: ".vtt"
                                    val fname = "${r.id}_${r.ep}_${s.lang}_$i$ext".replace(Regex("[^A-Za-z0-9._-]"), "_")
                                    val f = java.io.File(dir, fname)
                                    if (!f.exists() || f.length() == 0L) {
                                        val req = okhttp3.Request.Builder().url(s.url)
                                            .header("Referer", s.referer ?: ready.referer ?: "")
                                            .header("User-Agent", com.onimeku.app.data.Net.UA).build()
                                        com.onimeku.app.data.Net.client.newCall(req).execute().use { resp ->
                                            if (!resp.isSuccessful) return@async null
                                            val bytes = resp.body?.bytes() ?: return@async null
                                            f.writeBytes(bytes)
                                        }
                                    }
                                    s.copy(url = android.net.Uri.fromFile(f).toString())
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }
                } ?: emptyList()
                ready = ready.copy(subtitles = loaded)
            }
            com.onimeku.app.data.CrashLog.note("playback", "${r.id} ep${r.ep} stream=${if (ready.streamUrl.isNullOrBlank()) "none" else "ada"} embed=${if (ready.embedUrl.isNullOrBlank()) "no" else "yes"} subs=${ready.subtitles.size}")
            pb = ready
            // Uji mandiri jalur stream (debug.38): GET polos meniru langkah
            // pemutar (master -> varian -> segmen 1) lalu unggah log.
            launch { diagStreamProbe(r, ready) }
        }
    }
    if (pbFailed) {
        PlayerStatus(vm, a, r, loading = false, message = I18n.t("stream_unavailable", "This episode's stream is unavailable right now."), onRetry = { attempt++ })
        return
    }
    val playback = pb
    if (playback == null) {
        // Latar PEKAT sejak bingkai pertama (debug.30): tanpa ini halaman
        // Detail di bawahnya tembus selama transisi rute — "halaman
        // ganda" pada rekaman user. Semua keadaan layar-penuh pemutar
        // (memuat/galat/rantai habis/Wi-Fi) kini mengecat Bg sendiri.
        PlayerStatus(vm, a, r, loading = true)
        return
    }
    // Sebagian judul hanya menyediakan pemutar embed (streamUrl-nya halaman
    // web, bukan berkas media) — untuk itu tampilkan pemutar embed resmi.
    val mediaUrl = playback.streamUrl?.takeIf { isMediaUrl(it) }
        ?: playback.streamProxyUrl?.takeIf { isMediaUrl(it) }
    val embedUrl = playback.embedUrl ?: playback.pageUrl
        ?: playback.streamUrl?.takeIf { !isMediaUrl(it) }
    // Meniru pemutar web: embed HANYA untuk judul yang memang tanpa
    // stream media (embed-only). Judul ber-stream tidak pernah jatuh ke
    // embed — halaman embed MegaPlay bisa sudah mati (Error 410) dan
    // menampilkannya menyembunyikan kegagalan jalur utama yang sebenarnya.
    if (mediaUrl == null && !embedUrl.isNullOrBlank()) {
        EmbedPlayer(vm, r, a, embedUrl)
        return
    }
    // Rantai cadangan putar (meniru web): URL utama (untuk MegaPlay =
    // proksi API) -> coba ulang utama sekali -> URL CDN langsung ->
    // gagal jujur. Embed bukan bagian rantai untuk judul ber-stream.
    val streamUrl = if (useDirect) {
        playback.directUrl?.takeIf { isMediaUrl(it) } ?: mediaUrl
    } else {
        mediaUrl
    }
    if (streamUrl == null) {
        PlayerStatus(vm, a, r, loading = false, message = I18n.t("stream_unavailable", "This episode's stream is unavailable right now."), onRetry = { attempt++ })
        return
    }

    if (chainDead) {
        PlayerStatus(vm, a, r, loading = false, message = I18n.t("stream_unavailable", "This episode's stream is unavailable right now."),
            onRetry = { chainDead = false; useDirect = false; mainRetry = 0; attempt++ })
        return
    }

    key(useDirect, mainRetry) {
        PlayerReady(vm, r, a, ctx, act, audio, quality, playback, streamUrl, offline = pbOffline, onFatalError = {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { com.onimeku.app.data.ApiClient.uploadDiag("fatal") }
            // Jalur utama (proksi) dicoba sekali lagi dulu: kegagalan sesaat
            // di jaringan seluler tidak boleh langsung menjatuhkan ke jalur
            // langsung/embed yang memang lebih rapuh di HP.
            if (!useDirect && mainRetry == 0) {
                mainRetry = 1
                com.onimeku.app.data.CrashLog.note("player", "${r.id} ep${r.ep} jalur utama gagal, mencoba ulang sekali")
            } else if (!useDirect && !playback.directUrl.isNullOrBlank()) {
                useDirect = true
            } else {
                // Rantai habis (sama seperti fail() di web): tampilkan
                // kegagalan apa adanya + tombol coba lagi yang mengambil
                // ulang data putar, BUKAN halaman embed yang mungkin mati.
                chainDead = true
                com.onimeku.app.data.CrashLog.note("player", "${r.id} ep${r.ep} rantai putar habis (proksi+gagal langsung)")
            }
        })
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerReady(vm: AppVM, r: Route.Player, a: com.onimeku.app.data.Anime, ctx: Context, act: android.app.Activity?, audio: AudioManager, quality: String, playback: com.onimeku.app.data.Playback, streamUrl: String, offline: Boolean = false, onFatalError: () -> Unit = {}) {
    val player = remember(r, streamUrl) {
        // Transport OkHttp BERSAMA (Net): pool koneksi + cookie dipakai ulang
        // antar-episode; UA peramban + Referer dari API (CDN menolak klien
        // tanpa pola ini).
        val dsFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(com.onimeku.app.data.Net.client)
        // Perbaikan debug.25 (audit vs web): identitas permintaan dibedakan
        // per jalur. Ke PROKSI (workers.dev) JANGAN pakai samaran UA
        // peramban dan JANGAN kirim header Referer situs asing — panggilan
        // API aplikasi sendiri ("OnimekuApp/1.0", tanpa Referer) terbukti
        // lolos di jaringan HP, sedangkan pemutar ber-UA Chrome + Referer
        // megaplay gagal; proksi hanya membaca parameter ref. Samaran +
        // Referer akar origin HANYA untuk jalur langsung ke CDN (di sana
        // memang syaratnya: CDN menolak Referer halaman penuh/asing).
        val viaProxy = streamUrl.contains("/api/proxy")
        if (!viaProxy) {
            dsFactory.setUserAgent(com.onimeku.app.data.Net.UA)
            val streamHost = runCatching { java.net.URL(streamUrl).host }.getOrNull().orEmpty()
            val effReferer = if (streamHost == "nexabloom.top" || streamHost.endsWith(".nexabloom.top")) {
                "https://megaplay.buzz/"
            } else {
                playback.referer
            }
            if (!effReferer.isNullOrBlank()) {
                dsFactory.setDefaultRequestProperties(mapOf("Referer" to effReferer))
            }
        }
        // Pemutar MEMBACA cache unduhan (episode terunduh diputar dari disk)
        // tetapi TIDAK menulis ke dalamnya: sebelumnya tiap segmen yang
        // di-stream ikut ditulis ke SimpleCache ber-NoOpCacheEvictor, yaitu
        // beban I/O disk saat memutar dan penyimpanan yang tumbuh tanpa batas.
        val cacheFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(com.onimeku.app.data.Downloads.cache(ctx))
            .setCacheWriteDataSinkFactory(null)
            .setUpstreamDataSourceFactory(
                androidx.media3.datasource.DefaultDataSource.Factory(ctx, dsFactory)
            )
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        // Start cepat + tahan jaringan seluler goyah: mulai putar setelah 1,5 dtk
        // terbuffer (bawaan 2,5), pulih dari rebuffer di 4 dtk (bawaan 5), buffer
        // maju hingga 50 dtk, sisakan 20 dtk ke belakang agar seek mundur/ketuk
        // dua kali -10 dtk tidak mengunduh ulang segmen.
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(20_000, 50_000, 1_500, 4_000)
            .setBackBuffer(20_000, true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        // Perbaikan debug.25: STREAMING tidak lagi membaca cache bersama.
        // Entri cache era lama (playlist/segmen rusak yang tersimpan di
        // bawah kunci URL/konten yang stabil) dibaca pemutar lebih dulu
        // dan menggagalkan putar walau server+jaringan sehat — instalasi
        // segar tidak pernah mengalaminya. Cache bersama hanya dipakai
        // untuk memutar UNDUHAN (offline); streaming murni dari jaringan.
        val playFactory = if (offline) cacheFactory else androidx.media3.datasource.DefaultDataSource.Factory(ctx, dsFactory)
        val sourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(playFactory)
            // 5 percobaan ulang per segmen (bawaan 3) sebelum dianggap galat fatal.
            .setLoadErrorHandlingPolicy(androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy(5))
        val renderers = androidx.media3.exoplayer.DefaultRenderersFactory(ctx)
            .setEnableDecoderFallback(true) // dekoder utama gagal -> coba dekoder lain, bukan error
        ExoPlayer.Builder(ctx, renderers)
            .setMediaSourceFactory(sourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
            // Telemetri pemutar (debug.38): catat transisi keadaan, galat
            // pemutar, dan galat muat per-URL ke CrashLog (diunggah otomatis).
            addAnalyticsListener(object : androidx.media3.exoplayer.analytics.AnalyticsListener {
                override fun onPlaybackStateChanged(eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime, state: Int) {
                    com.onimeku.app.data.CrashLog.note("exo-state", "state=$state")
                }
                override fun onPlayerError(eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime, error: androidx.media3.common.PlaybackException) {
                    com.onimeku.app.data.CrashLog.note("exo-error", "${error.errorCodeName}: ${error.message} | cause=${error.cause}")
                }
                override fun onLoadError(eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime, loadEventInfo: androidx.media3.exoplayer.source.LoadEventInfo, mediaLoadData: androidx.media3.exoplayer.source.MediaLoadData, error: java.io.IOException, wasCanceled: Boolean) {
                    com.onimeku.app.data.CrashLog.note("exo-load", "${loadEventInfo.dataSpec.uri} -> ${error.javaClass.simpleName}: ${error.message}")
                }
            })
            com.onimeku.app.data.CrashLog.note("player", "PlayerReady dibuat ep${r.ep} url=${streamUrl.take(60)}")
            // Seek ke keyframe terdekat: ketuk dua kali/geser bilah jauh lebih cepat.
            setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
            quality.removeSuffix("p").toIntOrNull()?.let { h ->
                trackSelectionParameters = trackSelectionParameters.buildUpon().setMaxVideoSize(Int.MAX_VALUE, h).build()
            }
            val subs = playback.subtitles.filter { it.url.isNotBlank() }.map { s ->
                val mime = when {
                    s.url.contains(".srt") -> androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
                    s.url.contains(".ass") || s.url.contains(".ssa") -> androidx.media3.common.MimeTypes.TEXT_SSA
                    else -> androidx.media3.common.MimeTypes.TEXT_VTT
                }
                MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(s.url))
                    .setMimeType(mime).setLanguage(s.lang).setLabel(s.lang.uppercase()).build()
            }
            val itemBuilder = MediaItem.Builder().setUri(android.net.Uri.parse(streamUrl))
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(a.title).setSubtitle("Episode ${r.ep}")
                        .setArtworkUri(a.poster?.let { android.net.Uri.parse(it) }).build()
                )
                .setSubtitleConfigurations(subs)
            // AKAR KEGAGALAN PUTAR DI HP (debug.39; terbukti telemetri
            // .38 di emulator): URL proksi ber-path "/api/proxy" —
            // ekstensi .m3u8 hanya ada di dalam parameter terenkode —
            // sehingga ExoPlayer gagal mengenali item sebagai HLS dan
            // memperlakukannya sebagai media progresif:
            // ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED untuk SEMUA
            // judul berproksi. Di emulator kaki langsung (CDN mentah)
            // diam-diam menyelamatkan putar; di HP user kaki itu
            // terblokir operator sehingga tak ada yang tersisa.
            // Nyatakan tipe HLS secara eksplisit untuk stream playlist.
            val looksHls = streamUrl.contains(".m3u8") ||
                (playback.directUrl?.contains(".m3u8") ?: false) ||
                (playback.streamUrl?.contains(".m3u8") ?: false)
            if (looksHls) itemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
            // Kunci cache unduhan HANYA untuk putar offline (lihat
            // playFactory): item streaming tidak menyentuh cache bersama.
            if (offline) itemBuilder.setCustomCacheKey(com.onimeku.app.data.Downloads.key(r.id, r.ep))
            setMediaItem(itemBuilder.build())
            seekTo((vm.p.prog["${r.id}:${r.ep}"] ?: 0) * 1000L)
            playWhenReady = true
            prepare()
        }
    }

    var pos by remember(r) { mutableFloatStateOf((vm.p.prog["${r.id}:${r.ep}"] ?: 0).toFloat()) }
    var dur by remember(r) { mutableIntStateOf(1440) }
    var wantPlay by remember(r) { mutableStateOf(true) }
    var buffering by remember(r) { mutableStateOf(true) }
    var failed by remember(r) { mutableStateOf(false) }
    var errMsg by remember(r) { mutableStateOf<String?>(null) }
    var dragging by remember(r) { mutableStateOf(false) }
    var speed by remember(r) { mutableFloatStateOf(1f) }
    var br by remember { mutableFloatStateOf(1f) }
    var hud by remember { mutableStateOf<String?>(null) }
    var hudTick by remember { mutableIntStateOf(0) }
    val durNow by rememberUpdatedState(dur)
    var audioLabels by remember(r) { mutableStateOf<List<String>>(emptyList()) }
    var audioIdx by remember(r) { mutableIntStateOf(0) }
    var heights by remember(r) { mutableStateOf<List<Int>>(emptyList()) }
    var sleepMin by remember(r) { mutableIntStateOf(0) }
    var locked by remember(r) { mutableStateOf(false) }
    var rendered by remember(r) { mutableStateOf(false) }
    var vidRatio by remember(r) { mutableFloatStateOf(16f / 9f) }
    var pipRect by remember { mutableStateOf<android.graphics.Rect?>(null) }
    val bg by rememberUpdatedState(vm.s.settings.bgPlay)

    // ---- Fullscreen, kontrol otomatis sembunyi, subtitle, kualitas ----
    // Fullscreen diwarisi dari AppVM (debug.31): pindah episode membongkar
    // PlayerReady, tetapi keadaan penuhnya tidak ikut hilang.
    var full by remember { mutableStateOf(vm.playerFull) }
    var showUi by remember(r) { mutableStateOf(true) }
    var uiTick by remember { mutableIntStateOf(0) }
    val subTracks = remember(playback) { playback.subtitles.filter { it.url.isNotBlank() } }
    var subIdx by remember(r) { mutableIntStateOf(-1) }       // -1 = mati, selain itu indeks di subTracks
    var subReady by remember(r) { mutableStateOf(false) }
    var subDialog by remember { mutableStateOf(false) }
    fun subName(i: Int): String = if (i < 0) I18n.t("ctl_off", "Off") else subTracks.getOrNull(i)?.let { SubLang.display(it, i) } ?: I18n.t("ctl_off", "Off")

    fun applySub(i: Int) {
        // Perbaikan agen (berulang dari v5/v6): konstanta C dipakai sebagai
        // qualifier nama lengkap, tidak disimpan ke val (ilegal di Kotlin).
        val b = player.trackSelectionParameters.buildUpon()
        val groups = player.currentTracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT }
        if (i < 0 || groups.isEmpty()) {
            b.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
        } else {
            // Cocokkan trek menurut bahasa (urutan ke-n untuk bahasa yang sama); cadangan: indeks.
            val want = subTracks.getOrNull(i)
            val nth = if (want == null) 0 else subTracks.take(i).count { it.lang.equals(want.lang, true) }
            val byLang = if (want == null) emptyList() else groups.filter { it.mediaTrackGroup.getFormat(0).language.equals(want.lang, true) }
            val g = byLang.getOrNull(nth) ?: groups.getOrNull(i) ?: groups.first()
            b.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(androidx.media3.common.TrackSelectionOverride(g.mediaTrackGroup, 0))
        }
        player.trackSelectionParameters = b.build()
        subIdx = i
    }
    fun applyQuality(q: String) {
        val b = player.trackSelectionParameters.buildUpon()
        val h = q.removeSuffix("p").toIntOrNull()
        if (h != null) b.setMaxVideoSize(Int.MAX_VALUE, h) else b.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
        player.trackSelectionParameters = b.build()
    }
    fun applyAudio(i: Int) {
        val g = player.currentTracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO }.getOrNull(i) ?: return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(androidx.media3.common.TrackSelectionOverride(g.mediaTrackGroup, 0)).build()
        audioIdx = i
    }
    fun seekBy(ms: Long) {
        val t = (player.currentPosition + ms).coerceIn(0L, if (player.duration > 0) player.duration else Long.MAX_VALUE)
        player.seekTo(t)
        hud = if (ms > 0) "+${ms / 1000}s" else "−${-ms / 1000}s"; hudTick++
    }

    DisposableEffect(player) {
        val stalls = ArrayDeque<Long>()
        var wasReady = false
        var hinted = false
        var lastSeekAt = 0L
        val l = object : Player.Listener {
            override fun onPositionDiscontinuity(o: Player.PositionInfo, n: Player.PositionInfo, reason: Int) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) lastSeekAt = System.currentTimeMillis()
            }
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                // Saran kualitas: 3x macet dalam 90 dtk (di luar seek) -> tawarkan turunkan kualitas, sekali per sesi.
                val now = System.currentTimeMillis()
                if (state == Player.STATE_READY) wasReady = true
                if (state == Player.STATE_BUFFERING && wasReady && now - lastSeekAt > 3_000) {
                    stalls.addLast(now)
                    while (stalls.isNotEmpty() && now - stalls.first() > 90_000) stalls.removeFirst()
                    if (stalls.size >= 3 && !hinted) { hinted = true; vm.toast("Slow connection. Try a lower quality.") }
                }
                if (state == Player.STATE_READY) failed = false
                if (state == Player.STATE_ENDED) {
                    vm.saveProg(r.id, r.ep, durNow, durNow)
                    val nxt = a.available.firstOrNull { it > r.ep } ?: if (r.ep < a.eps) r.ep + 1 else null
                    if (vm.s.settings.autoplay && nxt != null) vm.replaceTop(Route.Player(r.id, nxt))
                }
            }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                // Perbaikan agen: alias C0 sama ilegalnya dengan val C.
                val ag = tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO }
                audioLabels = ag.mapIndexed { i, g ->
                    val f = g.mediaTrackGroup.getFormat(0)
                    (f.label ?: f.language?.uppercase() ?: "Track ${i + 1}")
                }
                audioIdx = ag.indexOfFirst { it.isSelected }.coerceAtLeast(0)
                heights = tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_VIDEO }
                    .flatMap { g -> (0 until g.length).map { g.getTrackFormat(it).height } }
                    .filter { it > 0 }.distinct().sortedDescending()
                if (!subReady && tracks.groups.any { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT }) {
                    subReady = true
                    val pref = vm.s.settings.subLang
                    val pick = SubLang.pick(subTracks, pref, vm.s.settings.lang)
                    // Bahasa pilihan tidak tersedia di episode ini -> beri tahu, lalu pakai bahasa cadangan.
                    if ((pref == "id" || pref == "en") && pick >= 0 && SubLang.code(subTracks[pick]) != pref)
                        vm.toast("${SubLang.prefName(pref)} subtitle unavailable · using ${subName(pick)}")
                    applySub(pick)
                }
            }
            override fun onRenderedFirstFrame() { rendered = true }
            override fun onVideoSizeChanged(size: androidx.media3.common.VideoSize) {
                if (size.width > 0 && size.height > 0) vidRatio = size.width * size.pixelWidthHeightRatio / size.height
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { wantPlay = playWhenReady; vm.playing = playWhenReady }
            override fun onPlayerError(error: PlaybackException) {
                failed = true; buffering = false
                errMsg = error.errorCodeName
                com.onimeku.app.data.CrashLog.note("player", "${r.id} ep${r.ep} leg=${if (streamUrl.contains("/api/proxy")) "proxy" else "direct"} ${error.errorCodeName}: ${error.cause ?: error.message}")
                // Serahkan ke rantai cadangan di PlayerContent (URL
                // langsung / embed) — ia yang tahu lapisan berikutnya.
                onFatalError()
            }
        }
        player.addListener(l)
        vm.playing = true
        onDispose {
            player.removeListener(l)
            vm.saveProg(r.id, r.ep, (player.currentPosition / 1000).toInt(), durNow)
            player.release()
            vm.playing = false
        }
    }

    // MediaSession + layanan latar: notifikasi, kontrol layar kunci, tombol headset.
    DisposableEffect(player) {
        val session = androidx.media3.session.MediaSession.Builder(ctx, player)
            .setId("onimeku-${System.nanoTime()}").build()
        PlaybackService.current = session
        val token = androidx.media3.session.SessionToken(ctx, android.content.ComponentName(ctx, PlaybackService::class.java))
        val ctrl = androidx.media3.session.MediaController.Builder(ctx, token).buildAsync()
        onDispose {
            androidx.media3.session.MediaController.releaseFuture(ctrl)
            if (PlaybackService.current === session) PlaybackService.current = null
            session.release()
        }
    }

    // Timer tidur.
    LaunchedEffect(sleepMin) {
        if (sleepMin > 0) {
            delay(sleepMin * 60_000L)
            player.pause(); vm.toast("Sleep timer: paused"); sleepMin = 0
        }
    }

    // Polling posisi 1 dtk dan HANYA menulis state saat detiknya berubah
    // (sebelumnya tiap 500 ms -> seluruh PlayerReady recompose 2x/dtk).
    // Simpan progres tiap 10 dtk (tiap simpan = serialisasi seluruh AppState).
    val nextEp = remember(r, a) { a.available.firstOrNull { it > r.ep } ?: if (r.ep < a.eps) r.ep + 1 else null }
    LaunchedEffect(player) {
        var n = 0
        var prefetched = false
        while (true) {
            val sec = (player.currentPosition / 1000).toInt()
            if (!dragging && sec != pos.toInt()) pos = sec.toFloat()
            val d = player.duration
            if (d > 0) {
                val ds = (d / 1000).toInt()
                if (ds != dur) dur = ds
                // 60 dtk terakhir: ambil data putar episode berikutnya di latar,
                // supaya autoplay langsung mulai tanpa menunggu API.
                if (!prefetched && nextEp != null && vm.s.settings.autoplay && ds - sec in 1..60) {
                    prefetched = true
                    launch { com.onimeku.app.data.ApiClient.prefetchPlayback(r.id, nextEp) }
                }
            }
            if (++n % 10 == 0 && wantPlay) vm.saveProg(r.id, r.ep, sec, durNow)
            delay(1000)
        }
    }

    // Ke background: pause, kecuali PiP atau "Background playback" aktif.
    // Saat berlanjut di latar, trek video dimatikan (hemat data + baterai).
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, player) {
        val o = LifecycleEventObserver { _, e ->
            fun videoOff(off: Boolean) {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, off).build()
            }
            if (e == Lifecycle.Event.ON_STOP && !vm.inPip) { if (bg) videoOff(true) else player.pause() }
            if (e == Lifecycle.Event.ON_START) videoOff(false)
        }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }

    // PiP: auto-enter saat video diputar (Android 12+), layar tetap menyala, kecerahan dipulihkan
    DisposableEffect(Unit) {
        act?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            vm.pipParams = null
            act?.setPictureInPictureParams(pipParams(ctx, false, false))
            act?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            act?.window?.let { w -> w.attributes = w.attributes.also { it.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } }
        }
    }
    LaunchedEffect(hudTick) { if (hudTick > 0) { delay(800); hud = null } }

    // Fullscreen: kunci landscape + sembunyikan bilah sistem; dipulihkan saat keluar/pergi dari pemutar.
    LaunchedEffect(full) {
        vm.playerFull = full
        act?.requestedOrientation = if (full) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        act?.window?.let { w ->
            val c = WindowCompat.getInsetsController(w, w.decorView)
            if (full) {
                c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                c.hide(WindowInsetsCompat.Type.systemBars())
            } else c.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            act?.window?.let { w -> WindowCompat.getInsetsController(w, w.decorView).show(WindowInsetsCompat.Type.systemBars()) }
        }
    }
    BackHandler(enabled = full) { full = false }
    // Kontrol hilang sendiri 3,5 dtk setelah ketukan terakhir saat video berjalan.
    LaunchedEffect(showUi, uiTick, wantPlay, buffering) { if (showUi && wantPlay && !buffering) { delay(3500); showUi = false } }

    fun step(d: Int) {
        val nums = if (a.available.isNotEmpty()) a.available else (1..a.eps).toList()
        val i = nums.indexOf(r.ep)
        val n = if (i >= 0) nums.getOrNull(i + d) else if (r.ep + d in 1..a.eps) r.ep + d else null
        if (n != null) vm.replaceTop(Route.Player(r.id, n))
    }
    val hasPrev = (if (a.available.isNotEmpty()) a.available.firstOrNull() else 1)?.let { r.ep > it } ?: (r.ep > 1)
    val hasNext = (if (a.available.isNotEmpty()) a.available.lastOrNull() else a.eps)?.let { r.ep < it } ?: (r.ep < a.eps)

    // PiP ala YouTube: parameter diperbarui tiap rasio/status putar/posisi video berubah; auto-masuk PiP hanya saat video diputar.
    LaunchedEffect(wantPlay, vidRatio, pipRect, hasPrev, hasNext) {
        val p = pipParams(ctx, wantPlay, wantPlay, vidRatio, pipRect, hasPrev, hasNext)
        vm.pipParams = p
        act?.setPictureInPictureParams(p)
    }
    // Tombol di jendela PiP (putar/jeda, sebelumnya, berikutnya).
    val stepNow by rememberUpdatedState<(Int) -> Unit>({ d -> step(d) })
    DisposableEffect(player) {
        val rcv = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context, i: android.content.Intent) {
                when (i.getStringExtra("op")) {
                    "toggle" -> if (player.playWhenReady) player.pause() else player.play()
                    "prev" -> stepNow(-1)
                    "next" -> stepNow(1)
                }
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(ctx, rcv, android.content.IntentFilter(PIP_ACTION), androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { try { ctx.unregisterReceiver(rcv) } catch (_: Exception) {} }
    }
    // Masuk PiP: sembunyikan kontrol. PiP ditutup (X) saat aplikasi sudah berhenti: jeda, jangan terus berbunyi di latar.
    var wasPip by remember { mutableStateOf(false) }
    LaunchedEffect(vm.inPip) {
        if (vm.inPip) showUi = false
        if (wasPip && !vm.inPip && !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && !bg) player.pause()
        wasPip = vm.inPip
    }

    if (vm.inPip) {
        Box(Modifier.fillMaxSize().background(Color.Black)) { VideoSurface(player, Modifier.fillMaxSize()) }
        return
    }

    if (subDialog) SubtitleDialog(
        tracks = subTracks, current = subIdx, pref = vm.s.settings.subLang, appLang = vm.s.settings.lang,
        onTrack = { i ->
            applySub(i); subDialog = false
            SubLang.code(subTracks[i])?.let { c -> vm.setSettings { it.copy(subLang = c) } }
            vm.toast("Subtitles: ${subName(i)}")
        },
        onOff = { applySub(-1); subDialog = false; vm.setSettings { it.copy(subLang = "off") }; vm.toast("Subtitles: ${I18n.t("ctl_off", "Off")}") },
        onAuto = {
            subDialog = false; vm.setSettings { it.copy(subLang = "auto") }
            val n = SubLang.pick(subTracks, "auto", vm.s.settings.lang); applySub(n); vm.toast("Subtitles: Auto · ${subName(n)}")
        },
        onDismiss = { subDialog = false },
    )

    val epList = if (a.available.isNotEmpty()) a.available else (1..a.eps).toList()
    val curIdx = epList.indexOf(r.ep)
    val epListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val thumbSrc = remember { MutableInteractionSource() }

    Column(Modifier.fillMaxSize().background(if (full) Color.Black else Bg)) {
      // Area video: penuh lebar, menempel di bawah status bar (tanpa bingkai), hitam pekat.
      Box(if (full) Modifier.fillMaxSize() else Modifier.fillMaxWidth().background(Color.Black).statusBarsPadding()) {
        Box(
            (if (full) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f)).background(Color.Black)
                .onGloballyPositioned { c ->
                    val bnd = c.boundsInWindow()
                    val nr = android.graphics.Rect(bnd.left.toInt(), bnd.top.toInt(), bnd.right.toInt(), bnd.bottom.toInt())
                    if (nr != pipRect) pipRect = nr
                }
                .pointerInput(player) {
                    detectTapGestures(
                        onTap = { showUi = !showUi; uiTick++ },
                        onDoubleTap = { o -> if (!locked) { if (o.x < size.width / 2f) seekBy(-10_000) else seekBy(10_000) } },
                    )
                }
                .pointerInput(Unit) {
                    var left = true
                    detectVerticalDragGestures(onDragStart = { left = it.x < size.width / 2 }) { change, dy ->
                        change.consume()
                        if (locked) return@detectVerticalDragGestures
                        val d = -dy / size.height * 1.3f
                        if (left) {
                            br = (br + d).coerceIn(0.2f, 1f)
                            act?.window?.let { w -> w.attributes = w.attributes.also { it.screenBrightness = br } }
                            hud = "Brightness ${(br * 100).roundToInt()}%"
                        } else {
                            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
                            val v = (cur + d).coerceIn(0f, 1f)
                            audio.setStreamVolume(AudioManager.STREAM_MUSIC, (v * max).roundToInt(), 0)
                            hud = "Volume ${(v * 100).roundToInt()}%"
                        }
                        hudTick++
                    }
                },
        ) {
            VideoSurface(player, Modifier.fillMaxSize())
            // Sampul anime menutupi layar hitam sampai bingkai video pertama tampil.
            if (!rendered && !failed) {
                RealImage(a, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color(0xA6080910)))
            }
            if (vm.s.settings.skipIntro && pos >= 3 && pos < 90) {
                Text(I18n.t("skip_intro", "Skip intro"), Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 58.dp).clip(CircleShape)
                    .background(Color(0x99080910)).border(1.dp, Color(0xCCFFFFFF), CircleShape).clickable { player.seekTo(90_000L) }
                    .padding(horizontal = 14.dp, vertical = 7.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            hud?.let {
                Text(it, Modifier.align(Alignment.Center).offset(y = (-58).dp).clip(RoundedCornerShape(14.dp)).background(Color(0xB3000000))
                    .padding(horizontal = 14.dp, vertical = 6.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            val remain = dur - pos.toInt()
            if (nextEp != null && dur > 60 && remain in 1..20 && !failed) {
                Row(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 58.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC0A0B10)).border(1.dp, Line, RoundedCornerShape(14.dp)).padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text(I18n.t("up_next", "Up next"), fontSize = 11.sp, color = InkSoft)
                        Text("Episode $nextEp · ${remain}s", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Chip(I18n.t("play_now", "Play now")) { vm.replaceTop(Route.Player(r.id, nextEp)) }
                }
            }
            if (locked && showUi) {
                Box(Modifier.align(Alignment.CenterStart).padding(start = 16.dp).size(44.dp).clip(CircleShape).background(Color(0x99000000))
                    .clickable { locked = false; uiTick++ }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.LockOpen, "Unlock controls")
                }
            }
            val uiVisible = !locked && (showUi || !wantPlay || failed || buffering)
            val uiA by animateFloatAsState(if (uiVisible) 1f else 0f, tween(200), label = "controlsAlpha")
            if (uiA > 0.01f) Box(Modifier.matchParentSize().graphicsLayer { alpha = uiA }) {
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color(0xB3080910), 0.32f to Color(0x33080910), 0.6f to Color(0x33080910), 1f to Color(0xD9080910))))

                // ---- Bar atas: kembali · judul · (kualitas/CC saat penuh) · kunci · layar penuh ----
                Row(Modifier.align(Alignment.TopStart).graphicsLayer { translationY = -(1f - uiA) * 28.dp.toPx() }.fillMaxWidth().padding(start = 6.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).clickable { vm.back() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    Column(Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(a.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text("Episode ${r.ep}", fontSize = 12.sp, color = Color(0xFFE6E8F0))
                    }
                    if (full) {
                        Text(quality, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x33FFFFFF)).padding(horizontal = 9.dp, vertical = 4.dp))
                        if (subTracks.isNotEmpty()) Text("CC · ${subName(subIdx)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x33FFFFFF))
                                .clickable { subDialog = true; uiTick++ }
                                .padding(horizontal = 9.dp, vertical = 4.dp))
                    }
                    Box(Modifier.size(42.dp).clip(CircleShape).clickable { full = !full; uiTick++ }, contentAlignment = Alignment.Center) {
                        Icon(if (full) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, if (full) "Exit fullscreen" else "Fullscreen")
                    }
                }

                // ---- Tengah: -10 · putar/jeda · +10 ----
                if (failed) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.ErrorOutline, null, Modifier.size(30.dp), tint = RedText)
                        Text("Can’t play this video", Modifier.padding(top = 6.dp), fontWeight = FontWeight.SemiBold)
                        if (errMsg != null) {
                            Text(errMsg!!, Modifier.padding(top = 2.dp, start = 32.dp, end = 32.dp), fontSize = 10.sp, color = InkSoft, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        Spacer(Modifier.height(10.dp))
                        Chip("Retry", icon = Icons.Filled.Refresh) { failed = false; player.prepare(); player.playWhenReady = true }
                    }
                } else {
                    var rotB by remember { mutableStateOf(0f) }
                    var rotF by remember { mutableStateOf(0f) }
                    val rb by animateFloatAsState(rotB, tween(380), label = "rotBack")
                    val rf by animateFloatAsState(rotF, tween(380), label = "rotFwd")
                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0x40000000)).pressable { rotB -= 360f; seekBy(-10_000); uiTick++ }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Replay10, "Back 10 seconds", Modifier.size(26.dp).graphicsLayer { rotationZ = rb })
                        }
                        Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                            if (buffering) {
                                CircularProgressIndicator(Modifier.size(52.dp), color = Red, trackColor = Color(0x33FFFFFF), strokeWidth = 3.5.dp)
                            } else {
                                Box(Modifier.fillMaxSize().clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFD01632), Red)))
                                    .pressable { player.playWhenReady = !player.playWhenReady; uiTick++ }, contentAlignment = Alignment.Center) {
                                    MorphIcon(wantPlay, Icons.Filled.Pause, Icons.Filled.PlayArrow, if (wantPlay) "Pause" else "Play", 36.dp, Color.White)
                                }
                            }
                        }
                        Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0x40000000)).pressable { rotF += 360f; seekBy(10_000); uiTick++ }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Forward10, "Forward 10 seconds", Modifier.size(26.dp).graphicsLayer { rotationZ = rf })
                        }
                    }
                }

                // ---- Bawah: bilah seek + waktu ----
                Column(Modifier.align(Alignment.BottomCenter).graphicsLayer { translationY = (1f - uiA) * 28.dp.toPx() }.fillMaxWidth().padding(horizontal = if (full) 28.dp else 14.dp)) {
                    Slider(pos.coerceIn(0f, dur.toFloat()), { dragging = true; pos = it },
                        valueRange = 0f..dur.toFloat(), modifier = Modifier.height(24.dp),
                        onValueChangeFinished = { player.seekTo((pos * 1000).toLong()); dragging = false },
                        thumb = { SliderDefaults.Thumb(interactionSource = thumbSrc, thumbSize = androidx.compose.ui.unit.DpSize(14.dp, 14.dp), colors = SliderDefaults.colors(thumbColor = Color.White)) },
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Red, inactiveTrackColor = Color(0x55FFFFFF)))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp).padding(bottom = 8.dp)) {
                        Text(fmt(pos.toInt()), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        Text(fmt(dur), fontSize = 12.sp, color = Color(0xCCFFFFFF))
                    }
                }
            }
        }
      }

      if (!full) {
        // ===== Panel di bawah video: info, navigasi episode, kontrol, daftar episode =====
        val epStart = 1 + (if (nextEp != null) 1 else 0) + 1
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(), state = epListState,
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
        ) {
            item(key = "info") {
                Column(Modifier.padding(horizontal = Gutter).padding(top = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tag("EPISODE ${r.ep}", Red, Color.White, small = true)
                        if (offline) Tag("OFFLINE", Color(0x33FFFFFF), Color.White, small = true)
                    }
                    Text(a.title, Modifier.padding(top = 8.dp), fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp,
                        letterSpacing = (-0.3).sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(listOfNotNull(a.year.takeIf { it > 0 }?.toString(), a.genres.take(2).joinToString(", ").ifBlank { null }, statusText(a.status)).joinToString("  ·  "),
                        Modifier.padding(top = 4.dp), fontSize = 13.sp, color = InkSoft, maxLines = 1)

                    if (hasPrev || hasNext) Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (hasPrev) Btn(I18n.t("previous", "Previous"), BtnStyle.Solid, Icons.Filled.SkipPrevious, Modifier.weight(1f), height = 48.dp) { step(-1) }
                        if (hasNext) Btn(I18n.t("next_episode", "Next episode"), BtnStyle.Play, Icons.Filled.SkipNext, Modifier.weight(1f), height = 48.dp) { step(1) }
                    }

                    val tiles = buildList {
                        add(CtlTile(Icons.Filled.Speed, I18n.t("ctl_speed", "Speed"), "${spdLabel(speed)}×", speed != 1f) {
                            val all = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
                            speed = all[(all.indexOf(speed).let { if (it < 0) 2 else it } + 1) % all.size]
                            player.setPlaybackSpeed(speed)
                            vm.toast("Speed ${spdLabel(speed)}×")
                        })
                        add(CtlTile(Icons.Filled.ClosedCaption, I18n.t("subtitles", "Subtitles"), subName(subIdx), subIdx >= 0) {
                            if (subTracks.isEmpty()) vm.toast("No subtitles for this episode")
                            else subDialog = true
                        })
                        add(CtlTile(Icons.Filled.HighQuality, I18n.t("quality", "Quality"), quality) {
                            val opts = listOf("Auto") + (if (heights.isNotEmpty()) heights.map { "${it}p" } else listOf("1080p", "720p", "480p"))
                            val nx = opts[(opts.indexOf(quality).let { if (it < 0) 0 else it } + 1) % opts.size]
                            vm.setSettings { it.copy(quality = nx) }; applyQuality(nx); vm.toast("Quality: $nx")
                        })
                        if (audioLabels.size > 1) add(CtlTile(Icons.Filled.Audiotrack, "Audio", audioLabels.getOrElse(audioIdx) { "?" }) {
                            val n = (audioIdx + 1) % audioLabels.size
                            applyAudio(n); vm.toast("Audio: ${audioLabels[n]}")
                        })
                        add(CtlTile(Icons.Filled.Bedtime, I18n.t("ctl_sleep", "Sleep"), if (sleepMin == 0) I18n.t("ctl_off", "Off") else "${sleepMin}m", sleepMin > 0) {
                            sleepMin = when (sleepMin) { 0 -> 15; 15 -> 30; 30 -> 60; else -> 0 }
                        })
                        add(CtlTile(Icons.Filled.PictureInPictureAlt, "PiP", "Mini") {
                            try { act?.enterPictureInPictureMode(vm.pipParams ?: pipParams(ctx, false, wantPlay)) } catch (e: Exception) { vm.toast("Picture-in-Picture unavailable") }
                        })
                        add(CtlTile(Icons.Filled.Lock, I18n.t("ctl_lock", "Lock"), I18n.t("ctl_screen", "Screen")) { locked = true; showUi = true })
                    }
                    Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        tiles.chunked(4).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { t -> ControlTile(t, Modifier.weight(1f)) }
                            }
                        }
                    }

                    Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x0FFFFFFF)).padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.TouchApp, null, Modifier.size(16.dp), tint = NavOff)
                        Text(I18n.t("swipe_hint", "Swipe left: brightness · right: volume · double-tap sides: ±10s"), fontSize = 11.5.sp, color = InkSoft, lineHeight = 16.sp)
                    }
                }
            }
            if (nextEp != null) item(key = "upnext") {
                val k = "${r.id}:$nextEp"
                Column(Modifier.padding(top = 24.dp)) {
                    SectionTitle(I18n.t("up_next", "Up next"))
                    Spacer(Modifier.height(12.dp))
                    EpisodeCard(a, nextEp, seen = k in vm.p.seen, progSec = vm.p.prog[k], current = false,
                        onOpen = { vm.replaceTop(Route.Player(r.id, nextEp)) }, nextUp = true) {}
                }
            }
            item(key = "ephead") {
                Column(Modifier.padding(top = 12.dp, bottom = 12.dp)) {
                    SectionTitle(I18n.t("episodes", "Episodes")) {
                        Text("${epList.size}", fontSize = 13.sp, color = NavOff, modifier = Modifier.padding(end = 10.dp))
                        if (curIdx > 4) Row(
                            Modifier.clip(CircleShape).background(Bg2).border(1.dp, Line, CircleShape)
                                .clickable { scope.launch { epListState.animateScrollToItem(epStart + curIdx) } }.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Filled.MyLocation, null, Modifier.size(14.dp), tint = RedText)
                            Text(I18n.t("current", "Current"), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = InkSoft)
                        }
                    }
                }
            }
            items(epList, key = { "pe$it" }) { n ->
                val k = "${r.id}:$n"
                EpisodeCard(a, n, seen = k in vm.p.seen, progSec = vm.p.prog[k], current = n == r.ep,
                    onOpen = { if (n != r.ep) vm.replaceTop(Route.Player(r.id, n)) }) {
                    if (n == r.ep) Icon(Icons.Filled.GraphicEq, null, Modifier.padding(end = 6.dp), tint = RedText)
                }
            }
        }
      }
    }
}

private class CtlTile(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val value: String, val on: Boolean = false, val action: () -> Unit)

/** Ubin kontrol: ikon di atas, nama kecil, nilai saat ini tebal. */
@Composable
private fun ControlTile(t: CtlTile, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.height(78.dp).pressable(t.action).clip(shape).background(Bg2)
            .border(1.dp, if (t.on) Red.copy(alpha = 0.7f) else Line, shape).padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(t.icon, null, Modifier.size(22.dp), tint = if (t.on) RedText else Color.White)
        Text(t.value, Modifier.padding(top = 6.dp), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
            color = if (t.on) RedText else Color.White, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Text(t.label, fontSize = 10.5.sp, color = NavOff, maxLines = 1)
    }
}

/** Keadaan memuat / galat: area video hitam dengan sampul redup, info + tombol coba lagi di bawah. */
@Composable
private fun PlayerStatus(vm: AppVM, a: com.onimeku.app.data.Anime, r: Route.Player, loading: Boolean, message: String? = null, onRetry: (() -> Unit)? = null) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        Box(Modifier.fillMaxWidth().background(Color.Black).statusBarsPadding()) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                RealImage(a, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color(0xCC080910)))
                Box(Modifier.align(Alignment.TopStart).padding(8.dp)) { GlassButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") { vm.back() } }
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(40.dp), color = Red, strokeWidth = 3.dp)
                        Text(I18n.t("loading_stream", "Loading stream…"), Modifier.padding(top = 12.dp), fontSize = 13.sp, color = InkSoft)
                    } else {
                        Icon(Icons.Filled.ErrorOutline, null, Modifier.size(34.dp), tint = RedText)
                        Text("Stream unavailable", Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Column(Modifier.padding(horizontal = Gutter).padding(top = 18.dp)) {
            Tag("EPISODE ${r.ep}", Red, Color.White, small = true)
            Text(a.title, Modifier.padding(top = 8.dp), fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp, maxLines = 2)
            if (message != null) Text(message, Modifier.padding(top = 10.dp), color = InkSoft, fontSize = 14.sp, lineHeight = 20.sp)
            if (onRetry != null) Box(Modifier.padding(top = 18.dp)) { Btn(I18n.t("try_again", "Try again"), BtnStyle.Play, Icons.Filled.Refresh, height = 48.dp, onClick = onRetry) }
        }
    }
}

private fun spdLabel(f: Float): String = if (f == f.toInt().toFloat()) f.toInt().toString() else f.toString()

/** Uji mandiri jalur stream (debug.38): GET polos OkHttp (klien bersama
 * yang sama dengan pemutar) meniru langkah ExoPlayer: master -> varian ->
 * 128KB segmen pertama, mencatat status/waktu tiap langkah ke CrashLog.
 * Membedakan "lapisan OkHttp/jaringan perangkat gagal" dari "pemutar
 * yang tak melanjutkan" — server hanya melihat master yang tiba. */
private suspend fun diagStreamProbe(r: Route.Player, pb: com.onimeku.app.data.Playback) {
    val master = pb.streamUrl ?: return
    if (!master.contains("/api/proxy")) return
    suspend fun fetch(u: String, maxBytes: Int): Triple<Int, Int, String> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val req = okhttp3.Request.Builder().url(u).build()
            com.onimeku.app.data.Net.client.newCall(req).execute().use { resp ->
                val buf = ByteArray(maxBytes)
                var n = 0
                resp.body?.byteStream()?.use { ins ->
                    while (n < maxBytes) {
                        val rd = ins.read(buf, n, maxBytes - n)
                        if (rd < 0) break
                        n += rd
                    }
                }
                Triple(resp.code, n, String(buf, 0, n, Charsets.UTF_8))
            }
        }
    try {
        val done = kotlinx.coroutines.withTimeoutOrNull(60_000) {
            val t1 = System.currentTimeMillis()
            val (c1, n1, masterText) = fetch(master, 8192)
            com.onimeku.app.data.CrashLog.note("diag", "master HTTP $c1 ${n1}B ${System.currentTimeMillis() - t1}ms")
            val variantRel = masterText.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.startsWith("#") } ?: return@withTimeoutOrNull false
            val variantUrl = java.net.URI(master).resolve(variantRel).toString()
            val t2 = System.currentTimeMillis()
            val (c2, n2, varText) = fetch(variantUrl, 32768)
            com.onimeku.app.data.CrashLog.note("diag", "varian HTTP $c2 ${n2}B ${System.currentTimeMillis() - t2}ms")
            val segRel = varText.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.startsWith("#") } ?: return@withTimeoutOrNull false
            val segUrl = java.net.URI(variantUrl).resolve(segRel).toString()
            val t3 = System.currentTimeMillis()
            val (c3, n3, _) = fetch(segUrl, 131072)
            com.onimeku.app.data.CrashLog.note("diag", "segmen1 HTTP $c3 ${n3}B ${System.currentTimeMillis() - t3}ms")
            true
        }
        if (done != true) com.onimeku.app.data.CrashLog.note("diag", "TIMEOUT/gagal di salah satu langkah (lihat urutan di atas)")
    } catch (e: Exception) {
        com.onimeku.app.data.CrashLog.note("diag", "GALAT ${e.javaClass.simpleName}: ${e.message}")
    }
    com.onimeku.app.data.ApiClient.uploadDiag("selftest")
}

private fun isMediaUrl(u: String): Boolean =
    u.contains(".m3u8") || u.contains(".mp4") || u.contains(".mpd")

/** Pemutar embed untuk judul yang sumbernya hanya menyediakan embed resmi. */
@Composable
private fun EmbedPlayer(vm: AppVM, r: Route.Player, a: com.onimeku.app.data.Anime, url: String) {
    val ctx = LocalContext.current
    val act = ctx.findActivity()
    DisposableEffect(Unit) {
        act?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { act?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    val nums = if (a.available.isNotEmpty()) a.available else (1..a.eps).toList()
    val idx = nums.indexOf(r.ep)
    Column(Modifier.fillMaxSize().background(Bg)) {
        Box(Modifier.fillMaxWidth().background(Color.Black).statusBarsPadding()) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
                AndroidView(
                    factory = { c ->
                        val cm = android.webkit.CookieManager.getInstance()
                        cm.setAcceptCookie(true)
                        android.webkit.WebView(c).apply {
                            cm.setAcceptThirdPartyCookies(this, true)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                            webViewClient = android.webkit.WebViewClient()
                            webChromeClient = android.webkit.WebChromeClient()
                            loadUrl(url)
                        }
                    },
                    update = { wv -> if (wv.url != null && wv.url != url) wv.loadUrl(url) },
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.align(Alignment.TopStart).padding(8.dp)) { GlassButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") { vm.back() } }
            }
        }
        Column(Modifier.padding(horizontal = Gutter).padding(top = 18.dp)) {
            Tag("EPISODE ${r.ep}", Red, Color.White, small = true)
            Text(a.title, Modifier.padding(top = 8.dp), fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp, maxLines = 2)
            if (idx > 0 || (idx >= 0 && idx < nums.size - 1)) Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (idx > 0) Btn(I18n.t("previous", "Previous"), BtnStyle.Solid, Icons.Filled.SkipPrevious, Modifier.weight(1f), height = 48.dp) { vm.replaceTop(Route.Player(r.id, nums[idx - 1])) }
                if (idx >= 0 && idx < nums.size - 1) Btn("Next episode", BtnStyle.Play, Icons.Filled.SkipNext, Modifier.weight(1f), height = 48.dp) { vm.replaceTop(Route.Player(r.id, nums[idx + 1])) }
            }
            Text(
                "Judul ini diputar lewat pemutar resmi sumbernya. Ketuk tombol putar di dalam video.",
                Modifier.padding(top = 14.dp), fontSize = 12.sp, color = InkSoft,
            )
        }
    }
}
````

### `app/src/main/java/com/onimeku/app/ui/ProfileScreens.kt` **[DIUBAH]**

````kotlin
package com.onimeku.app.ui

import kotlinx.coroutines.launch
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.AppVM
import com.onimeku.app.I18n
import com.onimeku.app.Notifier
import com.onimeku.app.Route

private val avatarColors = listOf(Color(0xFFA6122D), Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B))

@Composable
fun ProfileScreen(vm: AppVM) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    Page(extraTop = 24.dp) {
        val acc = vm.s.account
        val hShape = RoundedCornerShape(24.dp)
        Row(
            Modifier.fillMaxWidth().padding(bottom = 26.dp).clip(hShape).background(Brush.linearGradient(listOf(Color(0xFF3A1220), Bg2)))
                .border(1.dp, Line, hShape).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(68.dp).clip(CircleShape).background(if (acc != null) Red else Color(0x26FFFFFF)).border(2.dp, if (acc != null) RedText else Line, CircleShape), contentAlignment = Alignment.Center) {
                if (acc != null) Text(acc.email.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                else Icon(Icons.Outlined.Person, null, Modifier.size(34.dp), tint = InkSoft)
            }
            Column(Modifier.weight(1f)) {
                Text(if (acc != null) "Signed in" else "Guest", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
                Text(acc?.email ?: "Not signed in", Modifier.padding(top = 2.dp), fontSize = 14.sp, color = InkSoft, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
        Text("Profiles", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        vm.s.profiles.forEachIndexed { i, pr ->
            val sel = i == vm.s.pi
            val bd by androidx.compose.animation.animateColorAsState(if (sel) Red else Line, androidx.compose.animation.core.tween(250), label = "profBd")
            Row(Modifier.padding(bottom = 10.dp).fadeSlideIn(minOf(i, 3) * 60), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(18.dp)).background(Bg2)
                        .border(1.dp, bd, RoundedCornerShape(18.dp))
                        .clickable { vm.switchProfile(i) }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(avatarColors[i % 4]), contentAlignment = Alignment.Center) {
                        Text(pr.name.take(1).uppercase(), fontWeight = FontWeight.Bold)
                    }
                    Text(pr.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (sel) Icon(Icons.Filled.Check, "Active", Modifier.size(20.dp), tint = RedText)
                }
                if (vm.s.profiles.size > 1 && !sel) IconChip(Icons.Outlined.Delete, "Delete profile") { vm.delProfile(i) }
            }
        }
        if (vm.s.profiles.size < 4) {
            Row(Modifier.padding(bottom = 26.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(44.dp).clip(CircleShape).background(Bg2).border(1.dp, Line, CircleShape).padding(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart) {
                    BasicTextField(name, { if (it.length <= 12) name = it }, singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp), cursorBrush = SolidColor(Color.White),
                        decorationBox = { inner -> if (name.isEmpty()) Text("New profile", color = NavOff); inner() })
                }
                Chip("Add") {
                    if (name.isBlank()) vm.toast("Enter a name") else { vm.addProfile(name.trim()); name = "" }
                }
            }
        }


        if (acc == null) CardBox(Modifier.padding(top = 20.dp).fadeSlideIn(120)) {
            Column(Modifier.padding(20.dp)) {
                Text("Sign in to Onimeku", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Save your watchlist and pick up where you left off on any device.", Modifier.padding(top = 6.dp), color = InkSoft, fontSize = 14.sp)
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Btn("Sign in", BtnStyle.Solid, modifier = Modifier.weight(1f)) { vm.go(Route.Auth(false)) }
                    Btn("Sign up", BtnStyle.Play, modifier = Modifier.weight(1f)) { vm.go(Route.Auth(true)) }
                }
            }
        } else Box(Modifier.padding(top = 20.dp)) {
            var confirmDel by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn("Sign out", BtnStyle.Outline, modifier = Modifier.fillMaxWidth()) { vm.signOut() }
                Btn("Delete account", BtnStyle.Outline, modifier = Modifier.fillMaxWidth()) { confirmDel = true }
            }
            if (confirmDel) androidx.compose.material3.AlertDialog(
                onDismissRequest = { confirmDel = false },
                title = { Text("Delete account?") },
                text = { Text("This permanently deletes your account and the data synced to it. Data stored only on this device stays here.") },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmDel = false; vm.deleteAccount() }) { Text("Delete", color = RedText) } },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDel = false }) { Text("Cancel") } },
            )
        }

        CardBox(Modifier.padding(top = 20.dp).fadeSlideIn(160)) {
            Row(Modifier.fillMaxWidth().clickable { vm.go(Route.Downloads) }.padding(horizontal = 18.dp, vertical = 17.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Red.copy(alpha = 0.28f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Download, null, Modifier.size(18.dp), tint = RedText)
                }
                Column(Modifier.weight(1f).padding(start = 2.dp)) {
                    Text(I18n.t("tab_download", "Download"), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(
                        if (vm.s.downloads.isEmpty()) I18n.t("dl_profile_empty", "Watch offline") else "${vm.s.downloads.size} ${I18n.t("dl_saved", "episodes saved")}",
                        fontSize = 12.5.sp, color = NavOff,
                    )
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(20.dp), tint = NavOff)
            }
        }

        CardBox(Modifier.padding(top = 16.dp).fadeSlideIn(200)) {
            listOf("settings" to I18n.t("settings", "Settings"), "language" to I18n.t("language", "Language"), "help" to I18n.t("help", "Help Center"), "about" to I18n.t("about", "About Onimeku")).forEachIndexed { i, (k, label) ->
                if (i > 0) HLine()
                Row(Modifier.fillMaxWidth().clickable { vm.go(Route.Sub(k)) }.padding(horizontal = 18.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0x14FFFFFF)), contentAlignment = Alignment.Center) {
                        Icon(when (k) { "settings" -> Icons.Outlined.Settings; "language" -> Icons.Outlined.Language; "help" -> Icons.Outlined.HelpOutline; else -> Icons.Outlined.Info }, null, Modifier.size(18.dp), tint = InkSoft)
                    }
                    Text(label, Modifier.weight(1f).padding(start = 2.dp), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    if (k == "language") Text(if (vm.s.settings.lang == "id") "Bahasa Indonesia" else "English", color = NavOff, fontSize = 14.sp)
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(20.dp), tint = NavOff)
                }
            }
        }
    }
}

@Composable
private fun Grp(text: String) =
    Text(text.uppercase(), Modifier.padding(start = 2.dp, top = 22.dp, bottom = 8.dp), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = NavOff, letterSpacing = 1.sp)

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(desc, fontSize = 12.5.sp, color = NavOff, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(checked, onChange, colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = Red, checkedBorderColor = Red,
            uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF3A3E50), uncheckedBorderColor = Color(0xFF3A3E50)))
    }
}

@Composable
fun SubScreen(vm: AppVM, key: String) {
    val ensure = LocalEnsureNotif.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val set = vm.s.settings
    val titles = mapOf("settings" to I18n.t("settings", "Settings"), "language" to I18n.t("language", "Language"), "help" to I18n.t("help", "Help Center"), "about" to I18n.t("about", "About Onimeku"),
        "terms" to I18n.t("terms", "Terms of Service"), "privacy" to I18n.t("privacy", "Privacy Policy"), "licenses" to I18n.t("licenses", "Licenses"))
    Page {
        SubHead(titles[key] ?: "", vm)
        when (key) {
            "settings" -> {
                Grp(I18n.t("grp_playback", "Playback"))
                CardBox {
                    SwitchRow(I18n.t("autoplay_t", "Autoplay next episode"), I18n.t("autoplay_d", "Start the next episode when one ends."), set.autoplay) { v -> vm.setSettings { it.copy(autoplay = v) } }
                    HLine()
                    SwitchRow(I18n.t("skip_intro_t", "Skip intro"), I18n.t("skip_intro_d", "Show a button to skip opening songs."), set.skipIntro) { v -> vm.setSettings { it.copy(skipIntro = v) } }
                    HLine()
                    SwitchRow(I18n.t("bg_play_t", "Background playback"), I18n.t("bg_play_d", "Keep audio playing with lock-screen controls when you leave the app."), set.bgPlay) { v -> vm.setSettings { it.copy(bgPlay = v) } }
                }
                Grp(I18n.t("quality_s", "Video quality"))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Bg2).border(1.dp, Line, RoundedCornerShape(18.dp)).padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Auto", "1080p", "720p", "480p").forEach { qv ->
                        val on = set.quality == qv
                        Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Red else Color.Transparent)
                            .clickable { vm.setSettings { it.copy(quality = qv) } }, contentAlignment = Alignment.Center) {
                            Text(qv, fontSize = 14.sp, color = if (on) Color.White else InkSoft)
                        }
                    }
                }
                Grp(I18n.t("subtitles", "Subtitles"))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Bg2).border(1.dp, Line, RoundedCornerShape(18.dp)).padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("auto" to "Auto", "id" to "Indonesia", "en" to "English", "off" to I18n.t("ctl_off", "Off")).forEach { (code, label) ->
                        val on = set.subLang == code
                        Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Red else Color.Transparent)
                            .clickable { vm.setSettings { it.copy(subLang = code) } }, contentAlignment = Alignment.Center) {
                            Text(label, fontSize = 13.sp, color = if (on) Color.White else InkSoft, maxLines = 1)
                        }
                    }
                }
                Text(I18n.t("sub_auto_hint", "Auto picks Indonesia or English to match the app language, and falls back to the other when one is missing."),
                    Modifier.padding(start = 2.dp, top = 8.dp), fontSize = 12.5.sp, color = NavOff)
                Grp(I18n.t("grp_data", "Data and alerts"))
                CardBox {
                    SwitchRow(I18n.t("wifi_only_t", "Stream over Wi-Fi only"), I18n.t("wifi_only_d", "Avoid using mobile data."), set.wifi) { v -> vm.setSettings { it.copy(wifi = v) } }
                    HLine()
                    SwitchRow(I18n.t("alerts_t", "New episode alerts"), I18n.t("alerts_d", "Get notified when your list updates."), set.alerts) { v ->
                        vm.setSettings { it.copy(alerts = v) }
                        if (v) ensure { g -> if (g) Notifier.show(ctx, "Alerts on", "You will get new episode and reminder alerts") }
                    }
                }
            }
            "language" -> {
                Text(I18n.t("choose_language", "Choose the language used in the app."), Modifier.padding(start = 4.dp, bottom = 18.dp), color = InkSoft)
                CardBox {
                    // Bahasa Indonesia kini BENAR-BENAR berfungsi (debug.31);
                    // pilihan tersimpan di Settings.lang dan seluruh
                    // antarmuka utama mengikuti lewat I18n.
                    listOf("en" to "English", "id" to "Bahasa Indonesia").forEachIndexed { i, (code, label) ->
                        if (i > 0) HLine()
                        Row(Modifier.fillMaxWidth().clickable { vm.setSettings { st -> st.copy(lang = code) } }.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, Modifier.weight(1f), fontSize = 16.sp)
                            if (vm.s.settings.lang == code) Icon(Icons.Filled.Check, null, Modifier.size(20.dp), tint = RedText)
                        }
                    }
                    HLine()
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("日本語", Modifier.weight(1f), fontSize = 16.sp, color = Color(0xA6FFFFFF))
                        Text(I18n.t("coming_soon", "Coming soon"), fontSize = 13.sp, color = NavOff)
                    }
                }
            }
            "help" -> {
                Text("Quick answers to common questions.", Modifier.padding(start = 4.dp, bottom = 18.dp), color = InkSoft)
                CardBox {
                    val faq = listOf(
                        "How do I watch an episode offline?" to "Open the Download tab and save an episode while you are online. Saved episodes stay there until you remove them.",
                        "How do I add an anime to My List?" to "Tap the My List button on the anime you like. It will be saved in the My List tab.",
                        "An episode will not play. What can I do?" to "Check your connection, then lower the video quality in Settings. If it still fails, close the app and open it again.",
                        "Why is some quality missing?" to "Not every episode is available in every quality. Auto picks the best one for your connection.",
                        "How do I get new episode alerts?" to "Turn on New episode alerts in Settings. You will be told when something in your list updates.",
                    )
                    faq.forEachIndexed { i, (qq, aa) ->
                        if (i > 0) HLine()
                        var open by rememberSaveable { mutableStateOf(false) }
                        Column(Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 18.dp, vertical = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(qq, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                Text(if (open) "−" else "+", fontSize = 22.sp, color = NavOff)
                            }
                            if (open) Text(aa, Modifier.padding(top = 10.dp), color = InkSoft, fontSize = 14.sp)
                        }
                    }
                }
            }
            "about" -> {
                Row(Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Logo(56.dp)
                    Column {
                        Text("Onimeku", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text("Version ${com.onimeku.app.BuildConfig.VERSION_NAME}", color = NavOff)
                    }
                }
                Text("Onimeku is a place to watch anime online. Browse top hits, follow new episodes, and keep a list of what you want to watch next.",
                    Modifier.padding(start = 4.dp, bottom = 18.dp), color = InkSoft)
                CardBox {
                    val ctxA = androidx.compose.ui.platform.LocalContext.current
                    Row(Modifier.fillMaxWidth().clickable {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { com.onimeku.app.data.ApiClient.uploadDiag("manual") }
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Onimeku diagnostics")
                            putExtra(android.content.Intent.EXTRA_TEXT, com.onimeku.app.data.CrashLog.read())
                        }
                        ctxA.startActivity(android.content.Intent.createChooser(send, "Send diagnostics").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    }.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Send diagnostics", Modifier.weight(1f), fontSize = 16.sp)
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(20.dp), tint = NavOff)
                    }
                    HLine()
                    listOf("terms" to "Terms of Service", "privacy" to "Privacy Policy", "licenses" to "Licenses").forEachIndexed { i, (k2, l) ->
                        if (i > 0) HLine()
                        Row(Modifier.fillMaxWidth().clickable { vm.go(Route.Sub(k2)) }.padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(l, Modifier.weight(1f), fontSize = 16.sp)
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(20.dp), tint = NavOff)
                        }
                    }
                }
            }
            "terms", "privacy", "licenses" -> LegalContent(key)
        }
    }
}
````

### `app/src/main/java/com/onimeku/app/ui/Screens.kt` **[DIUBAH]**

````kotlin
package com.onimeku.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.AppVM
import com.onimeku.app.I18n
import com.onimeku.app.Route
import com.onimeku.app.data.Anime
import com.onimeku.app.data.Catalog
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

// ------------------------------------------------------------------ HOME
private val SectionGap = 26.dp

/** Satu baris poster bergulir horizontal dengan judul bagian + "See all". */
@Composable
private fun PosterRow(title: String, ids: List<String>, vm: AppVM, seeAll: String?, ranked: Boolean = false, delayMs: Int = 0) {
    val items = ids.mapNotNull { Catalog.byId[it] }
    if (items.isEmpty()) return
    Column(if (delayMs >= 0) Modifier.fadeSlideIn(delayMs) else Modifier) {
        Spacer(Modifier.height(SectionGap))
        SectionHead(title, seeAll?.let { key -> { vm.go(Route.SeeAll(key)) } })
        Rail {
            itemsIndexed(items, key = { _, a -> a.id }) { i, a ->
                PosterCard(a, railW, rank = if (ranked) i + 1 else null, sharedKey = "row:$title:${a.id}", index = i) { vm.go(Route.Detail(a.id)) }
            }
        }
    }
}

@Composable
fun HomeScreen(vm: AppVM) {
    // Skeleton memudar halus menjadi konten saat katalog tiba (bukan muncul tiba-tiba).
    val loading = Catalog.all.isEmpty() && Catalog.loading
    androidx.compose.animation.Crossfade(targetState = loading, animationSpec = androidx.compose.animation.core.tween(380), label = "homeLoad") { l ->
        if (l) HomeSkeleton(heroHeight(), railW, bottomContentPadding()) else HomeContent(vm)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(vm: AppVM) {
    val p = vm.p
    val heroH = heroHeight()
    if (Catalog.all.isEmpty()) {
        if (Catalog.loading) {
            HomeSkeleton(heroH, railW, bottomContentPadding())
        } else {
            Page(extraTop = 64.dp) {
                RootTitle(I18n.t("home", "Home"))
                EmptyBox(I18n.t("catalog_unavail_t", "Catalog unavailable"), I18n.t("catalog_unavail_b", "The catalog could not be loaded. Check your connection, then try again.")) {
                    Btn("Try again", BtnStyle.Play) { vm.reloadCatalog() }
                }
            }
        }
        return
    }
    // Tarik-untuk-segar BUATAN SENDIRI (debug.31): riwayatnya,
    // PullToRefreshBox material3 1.3 membuat Home crash di emulator
    // (build .17) sehingga fitur ini dimatikan. Implementasi sekarang
    // tidak memakai komponen perpustakaan: koneksi gulir bersarang
    // mengakumulasi delta tarikan saat daftar di paling atas; lewat
    // ambang 90dp -> vm.refresh(). Risiko crash jalur lama tidak ada.
    // LazyColumn: hanya baris yang terlihat (+ prefetch) yang dikomposisi. Dulu seluruh baris
    // (belasan rail genre) digambar sekaligus saat Home dibuka.
    val base = p.base?.let { Catalog.byId[it] }
    val similar = remember(base?.id, Catalog.all) { base?.let { b -> Catalog.similar(b.id).map { it.id } }.orEmpty() }
    val last = p.last
    val lastT = last?.let { p.prog["${it.id}:${it.ep}"] }
    val lastAnime = last?.let { Catalog.byId[it.id] }
    val fresh = Catalog.new
    val allRows = remember(Catalog.all) { Catalog.alphabetical.mapNotNull { Catalog.byId[it] }.chunked(3) }
    var pullPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val ptrThresholdPx = with(density) { 90.dp.toPx() }
    val ptrConn = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                val atTop = vm.homeScroll.firstVisibleItemIndex == 0 && vm.homeScroll.firstVisibleItemScrollOffset == 0
                if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput && atTop && available.y > 0f && !vm.refreshing) {
                    pullPx = (pullPx + available.y * 0.5f).coerceAtMost(340f)
                    return Offset(0f, available.y)
                }
                if (pullPx > 0f && available.y < 0f) {
                    val used = minOf(pullPx, -available.y)
                    pullPx -= used
                    return Offset(0f, -used)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (pullPx > ptrThresholdPx) vm.refresh()
                pullPx = 0f
                return super.onPreFling(available)
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize().nestedScroll(ptrConn), state = vm.homeScroll, contentPadding = PaddingValues(bottom = bottomContentPadding())) {
        item(key = "hero", contentType = "hero") { Hero(vm, heroH) }

        if (last != null && lastT != null && lastAnime != null) {
            item(key = "continue", contentType = "rail") {
                Column(Modifier.fadeSlideIn(0)) {
                    Spacer(Modifier.height(SectionGap))
                    SectionHead(I18n.t("sec_continue", "Continue Watching"))
                    Rail {
                        item {
                            Poster(lastAnime, railW, ep = true, meta = lastAnime.title to "Episode ${last.ep} · ${fmt(lastT)}", progress = (lastT / 1440f).coerceIn(0.03f, 0.97f)) {
                                vm.go(Route.Player(last.id, last.ep))
                            }
                        }
                    }
                }
            }
        }
        if (base != null && similar.isNotEmpty()) {
            item(key = "because", contentType = "rail") { PosterRow("${I18n.t("sec_because", "Because you watched")} ${base.title}", similar, vm, seeAll = null, delayMs = 0) }
        }
        item(key = "top", contentType = "rail") { PosterRow(I18n.t("sec_top", "Top Hits Anime"), Catalog.top, vm, seeAll = "top", ranked = true, delayMs = 0) }
        if (fresh.isNotEmpty()) {
            item(key = "new", contentType = "rail") {
                Column(Modifier.fadeSlideIn(70)) {
                    Spacer(Modifier.height(SectionGap))
                    SectionHead(I18n.t("sec_new_eps", "New Episode Releases")) { vm.go(Route.SeeAll("new")) }
                    Rail {
                        items(fresh, key = { it.first }) { (id, ep, whenTxt) ->
                            Catalog.byId[id]?.let { a ->
                                // Judul anime di bawah kartu (dulu kartu hanya bertuliskan "Episode N" tanpa nama anime).
                                Column(Modifier.width(railW)) {
                                    // debug.33: ukuran poster potret SAMA dengan rail lain (dulu kartu lanskap 232:150).
                                    Poster(a, railW, badges = true, meta = "Episode $ep" to whenTxt) { vm.go(Route.Detail(id)) }
                                    Text(a.title, Modifier.padding(top = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp,
                                        minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
        // delayMs = -1: tanpa animasi masuk (baris di bawah layar tidak mengulang animasi saat digulir kembali).
        item(key = "airing", contentType = "rail") { PosterRow(I18n.t("sec_airing", "Airing Now"), Catalog.airing.take(12), vm, seeAll = "airing", delayMs = 140) }
        item(key = "popular", contentType = "rail") { PosterRow(I18n.t("sec_popular", "Popular"), Catalog.popular.take(12), vm, seeAll = "popular", delayMs = -1) }
        item(key = "latest", contentType = "rail") { PosterRow(I18n.t("sec_latest", "Latest Releases"), Catalog.latest.take(12), vm, seeAll = "latest", delayMs = -1) }
        item(key = "donghua", contentType = "rail") { PosterRow(I18n.t("sec_donghua", "Donghua"), Catalog.donghua.take(12), vm, seeAll = "donghua", delayMs = -1) }
        // All Anime: seluruh katalog langsung tampil sebagai grid 3 kolom (dipecah per baris agar tetap lazy).
        item(key = "all-head", contentType = "all-head") {
            Column {
                Spacer(Modifier.height(SectionGap))
                SectionHead(I18n.t("sec_all", "All Anime"))
            }
        }
        items(allRows.size, key = { "all-row:$it" }, contentType = { "all-row" }) { r ->
            val row = allRows[r]
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { c, a -> Box(Modifier.weight(1f)) { Tile(a, vm, (r * 3 + c) % 9) } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    }
}

/** Tinggi hero mengikuti layar (±64%), bukan angka tetap. */
@Composable
private fun heroHeight() = (LocalConfiguration.current.screenHeightDp * 0.64f).coerceIn(420f, 640f).dp

/** Carousel hero: 5 judul teratas, geser manual atau otomatis tiap 5,5 dtk, indikator animasi, parallax saat digulir. */
@Composable
private fun Hero(vm: AppVM, heroH: androidx.compose.ui.unit.Dp) {
    val items = Catalog.top.take(5).mapNotNull { Catalog.byId[it] }.ifEmpty { Catalog.all.take(1) }
    if (items.isEmpty()) return
    val pager = rememberPagerState { items.size }
    LaunchedEffect(pager, items.size) {
        if (items.size > 1) while (true) {
            delay(5500)
            if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % items.size)
        }
    }
    // clipToBounds: gambar parallax digeser ke bawah saat digulir; tanpa klip ia bocor ke bagian di bawah hero.
    Box(Modifier.fillMaxWidth().height(heroH).clipToBounds()) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { i -> HeroPage(vm, items[i.coerceIn(0, items.lastIndex)]) { (pager.currentPage - i) + pager.currentPageOffsetFraction } }
        // Scrim tipis di atas supaya status bar & lonceng tetap terbaca di poster terang.
        Box(Modifier.fillMaxWidth().height(90.dp).drawBehind { drawRect(Brush.verticalGradient(listOf(Color(0x40000000), Color.Transparent))) })
        if (items.size > 1) {
            Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                items.indices.forEach { i ->
                    HeroDot(selected = pager.currentPage == i, page = pager.currentPage)
                }
            }
        }
    }
}

@Composable
private fun HeroPage(vm: AppVM, a: Anime, pageOffset: () -> Float) {
    val on = a.id in vm.p.myList
    val firstEp = a.available.firstOrNull() ?: 1
    Box(Modifier.fillMaxSize().artBackground(a).clickable { SharedArt.key = "hero:${a.id}"; vm.go(Route.Detail(a.id)) }) {
        val req = rememberImg(com.onimeku.app.data.PosterHealer.posterFor(a))
        if (req == null) Box(Modifier.fillMaxSize().shimmer())
        else coil.compose.AsyncImage(
            model = req, contentDescription = a.title,
            // Parallax: gambar bergeser 35% dari jarak scroll.
            modifier = Modifier.fillMaxSize().sharedArt("hero:${a.id}").graphicsLayer {
                translationY = vm.homeScrollPx().coerceAtMost(4000) * 0.35f
                translationX = -pageOffset() * size.width * 0.3f // parallax: gambar lebih lambat dari halaman
            }.kenBurns(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
        Box(Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.26f to Color.Transparent, 0.52f to Color(0x3314161F), 0.8f to Color(0xE614161F), 1f to Bg))
        })
        Column(Modifier.align(Alignment.BottomStart).graphicsLayer {
            val o = pageOffset()
            alpha = (1f - kotlin.math.abs(o) * 1.6f).coerceIn(0f, 1f) // teks memudar saat geser
            translationX = o * size.width * 0.2f
        }.padding(start = 20.dp, end = 20.dp, bottom = 44.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                a.rating?.let { ScorePill(it) }
                if (a.hd) Tag("HD", Red, Color.White)
                StatusPill(a.status)
            }
            Text(a.title, Modifier.padding(top = 12.dp), fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp, letterSpacing = (-0.6).sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(
                a.year.takeIf { it > 0 }?.toString(),
                a.eps.takeIf { it > 0 }?.let { "$it ${if (it == 1) "episode" else "episodes"}" },
                a.genres.take(2).joinToString(", ").ifBlank { null },
            ).joinToString("  ·  ")
            Text(meta, Modifier.padding(top = 6.dp), fontSize = 13.sp, color = Color(0xFFD5D8E3), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (a.synopsis.isNotBlank()) Text(a.synopsis, Modifier.padding(top = 8.dp), fontSize = 13.sp, lineHeight = 19.sp,
                color = Color(0xFFB9BDCB), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PlayButton(I18n.t("play", "Play"), null, Modifier.weight(1f)) { vm.go(Route.Player(a.id, firstEp)) }
                Box(
                    Modifier.size(56.dp).pressable { vm.toggleML(a.id) }.clip(RoundedCornerShape(18.dp))
                        .background(if (on) Color.White else Color(0x59080910))
                        .border(1.dp, if (on) Color.White else Color(0x66FFFFFF), RoundedCornerShape(18.dp)).sparkBurst(on),
                    contentAlignment = Alignment.Center,
                ) { MorphIcon(on, Icons.Filled.Check, Icons.Filled.Add, if (on) "Added" else "My List", 26.dp, if (on) Bg else Color.White) }
            }
        }
    }
}

// ------------------------------------------------------------------ CALENDAR
@Composable
fun CalendarScreen(vm: AppVM) {
    val ensure = LocalEnsureNotif.current
    val days = remember { (0..6).map { LocalDate.now().plusDays(it.toLong()) } }
    val dow = days[vm.calDay].dayOfWeek.value % 7
    val list = remember(dow, Catalog.all) { Catalog.all.filter { it.day == dow }.sortedBy { it.time } }
    // Jadwal ASLI dari API bila sudah termuat: hanya judul dengan episode
    // berikutnya yang benar-benar terjadwal pada tanggal terpilih.
    val sched = vm.schedule
    val realList = if (sched.isNotEmpty()) {
        val sel = days[vm.calDay]
        Catalog.all.mapNotNull { a ->
            val s = sched[a.id] ?: return@mapNotNull null
            val dt = java.time.Instant.ofEpochSecond(s.airingAt).atZone(java.time.ZoneId.systemDefault())
            if (dt.toLocalDate() == sel) Triple(a, s, dt) else null
        }.sortedBy { it.second.airingAt }
    } else null
    Page {
        RootTitle(I18n.t("calendar_t", "Release Calendar"), I18n.t("calendar_sub", "Episodes airing this week."))
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEachIndexed { k, d ->
                val sel = k == vm.calDay
                val chipBg by androidx.compose.animation.animateColorAsState(if (sel) Red else Bg2, tween(220), label = "dayBg")
                val chipBd by androidx.compose.animation.animateColorAsState(if (sel) Red else Line, tween(220), label = "dayBd")
                Column(
                    Modifier.weight(1f).pressable { vm.calDay = k }.clip(RoundedCornerShape(16.dp)).background(chipBg)
                        .border(1.dp, chipBd, RoundedCornerShape(16.dp)).padding(vertical = 11.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(d.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.ENGLISH).uppercase(),
                        fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, color = if (sel) Color(0xCCFFFFFF) else NavOff)
                    Text("${d.dayOfMonth}", Modifier.padding(top = 3.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.padding(top = 5.dp).size(5.dp).clip(CircleShape).background(if (k == 0) (if (sel) Color.White else RedText) else Color.Transparent))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (Catalog.all.isEmpty()) {
            SkeletonRows(5)
        } else key(vm.calDay) { Column(Modifier.fadeSlideIn()) {
        if (realList != null) {
            if (realList.isEmpty()) EmptyBox(I18n.t("nothing_airing_t", "Nothing airing"), I18n.t("nothing_airing_b", "No new episodes are scheduled for this day."))
            else realList.forEach { (a, s, dt) ->
                val on = a.id in vm.p.remind
                ItemRow(
                    a, thumbLabel = dt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), title = a.title,
                    sub = "Episode ${s.ep} · ${a.genres.firstOrNull() ?: "Anime"}",
                    onOpen = { vm.go(Route.Detail(a.id)) },
                ) { IconChip(if (on) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone, if (on) "Reminder on" else "Remind me", on) { vm.toggleRemind(a.id, ensure) } }
            }
        } else if (list.isEmpty()) EmptyBox(I18n.t("nothing_airing_t", "Nothing airing"), I18n.t("nothing_airing_b", "No new episodes are scheduled for this day."))
        else list.forEach { a ->
            val on = a.id in vm.p.remind
            ItemRow(
                a, thumbLabel = a.time, title = a.title, sub = "Episode ${a.ep} · ${a.genres.firstOrNull() ?: "Anime"}",
                onOpen = { vm.go(Route.Detail(a.id)) },
            ) { IconChip(if (on) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone, if (on) "Reminder on" else "Remind me", on) { vm.toggleRemind(a.id, ensure) } }
        }
        } }
    }
}

@Composable
fun ItemRow(
    a: Anime, thumbLabel: String, title: String, sub: String, subOk: Boolean = false,
    onThumb: (() -> Unit)? = null, onOpen: () -> Unit, nextUp: Boolean = false, trailing: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(shape).background(Bg2).border(1.dp, Line, shape).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(108.dp).aspectRatio(16f / 10f).clip(RoundedCornerShape(11.dp)).artBackground(a)
            .clickable(onClick = onThumb ?: onOpen)) {
            RealImage(a, Modifier.fillMaxSize())
            Text(thumbLabel, Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xB3000000))
                .padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
            Text(sub, fontSize = 12.sp, color = if (subOk) RedText else NavOff, modifier = Modifier.padding(top = 3.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

// ------------------------------------------------------------------ MY LIST / DOWNLOADS
@Composable
fun MyListScreen(vm: AppVM) {
    val l = vm.p.myList.mapNotNull { Catalog.byId[it] }
    AnimeGrid(l, vm) {
        RootTitle(I18n.t("tab_mylist", "My List"))
        if (l.isEmpty()) EmptyBox(I18n.t("mylist_empty_t", "Your list is empty"), I18n.t("mylist_empty_b", "Tap My List on any anime to save it here.")) { Btn(I18n.t("browse_anime", "Browse anime"), BtnStyle.Play) { vm.setTab(Route.Home) } }
        else Text("${l.size} saved", color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
    }
}

@Composable
fun DownloadsScreen(vm: AppVM) = Page {
    SubHead(I18n.t("tab_download", "Download"), vm)
    val d = vm.s.downloads
    if (d.isEmpty()) EmptyBox(I18n.t("dl_none_t", "No downloads yet"), I18n.t("dl_none_b", "Open an anime and tap the download icon on an episode to watch it offline.")) { Btn(I18n.t("browse_anime", "Browse anime"), BtnStyle.Play) { vm.setTab(Route.Home) } }
    else {
        Text("${d.size} episode${if (d.size > 1) "s" else ""}", color = InkSoft, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        Box(Modifier.padding(bottom = 12.dp)) { Chip("Delete all", icon = Icons.Outlined.Delete) { vm.removeAllDl() } }
        d.forEachIndexed { idx, x ->
            val a = Catalog.byId[x.id] ?: return@forEachIndexed
            val pct = vm.downloading["${x.id}:${x.ep}"]
            Column(Modifier.fadeSlideIn(minOf(idx, 5) * 50)) {
                ItemRow(a, "Ep ${x.ep}", a.title,
                    if (pct != null) "Downloading… $pct%" else "Episode ${x.ep} · Saved offline",
                    onOpen = { vm.go(Route.Player(x.id, x.ep)) }) {
                    IconChip(Icons.Outlined.Delete, "Remove download") { vm.removeDl(x.id, x.ep) }
                }
                if (pct != null) {
                    val animPct by animateFloatAsState(pct / 100f, tween(300), label = "dlBar")
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { animPct },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp),
                        color = Red, trackColor = Line,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ DETAIL
/** Cincin progres unduhan; ketuk untuk membatalkan. */
@Composable
private fun DownloadRing(pct: Int, onClick: () -> Unit) {
    val prog by animateFloatAsState(pct / 100f, tween(300), label = "dlProg")
    Box(Modifier.size(width = 40.dp, height = 36.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(
            progress = { prog }, modifier = Modifier.size(22.dp), color = Red, trackColor = Line, strokeWidth = 2.5.dp,
        )
        Icon(Icons.Filled.Close, "Cancel download", Modifier.size(10.dp), tint = Color.White)
    }
}

// ---- Komponen khusus halaman Detail (redesain debug.33) ----
val Gutter = 20.dp
private val GoodGreen = Color(0xFF3DDC97)

/** Tombol bulat kaca (blur-look) yang melayang di atas gambar hero. */
@Composable
fun GlassButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(Color(0x4D000000))
            .border(1.dp, Color(0x40FFFFFF), CircleShape).pressable(onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(22.dp), tint = Color.White) }
}

/** Skor ber-bintang emas di atas kaca gelap. */
@Composable
private fun ScorePill(score: String) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x33FFC857)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Filled.Star, null, Modifier.size(13.dp), tint = Gold)
        Text(score.replace("★", "").trim(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Gold)
    }
}

@Composable
private fun StatusPill(status: String) {
    val live = status.equals("Airing", ignoreCase = true)
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x26FFFFFF)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (live) PulseDot(GoodGreen) else Box(Modifier.size(6.dp).clip(CircleShape).background(NavOff))
        Text(statusText(status), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, softWrap = false)
    }
}

/** Tombol utama: gradasi crimson, tinggi 56dp, bayangan lembut. */
@Composable
private fun PlayButton(label: String, sub: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().height(56.dp).pressable(onClick).clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFC8142F), Red)))
            .sweepShine()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, null, Modifier.size(22.dp), tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            if (sub != null) Text(sub, fontSize = 12.sp, color = Color(0xCCFFFFFF), maxLines = 1)
        }
        Icon(Icons.Filled.ChevronRight, null, Modifier.nudgeX(), tint = Color(0xB3FFFFFF))
    }
}

/** Aksi sekunder: ikon di atas, label di bawah, tiga sama lebar. */
@Composable
private fun ActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier,
    on: Boolean = false, iconOn: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.height(68.dp).pressable(onClick).clip(shape).background(if (on) Color(0x26A6122D) else Bg2)
            .border(1.dp, if (on) Red.copy(alpha = 0.7f) else Line, shape).sparkBurst(on),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        if (iconOn != null) MorphIcon(on, iconOn, icon, label, 22.dp, if (on) RedText else Color.White)
        else Icon(icon, null, Modifier.size(22.dp), tint = if (on) RedText else Color.White)
        Text(label, Modifier.padding(top = 5.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = if (on) RedText else InkSoft, maxLines = 1)
    }
}

@Composable
fun SectionTitle(text: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Gutter), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(18.dp).clip(CircleShape).background(Red))
        Text(text, Modifier.weight(1f).padding(start = 10.dp), fontSize = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp)
        trailing?.invoke()
    }
}

/** Satu baris episode: thumbnail + progres tonton + status + aksi unduh. */
@Composable
fun EpisodeCard(
    a: Anime, n: Int, seen: Boolean, progSec: Int?, current: Boolean,
    onOpen: () -> Unit, nextUp: Boolean = false, trailing: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val frac = when { seen -> 1f; progSec != null -> (progSec / 1440f).coerceIn(0.03f, 0.97f); else -> 0f }
    Row(
        Modifier.padding(horizontal = Gutter).padding(bottom = 10.dp).fillMaxWidth().pressable(onOpen).clip(shape)
            .background(if (current) Color(0x1FA6122D) else Bg2)
            .border(1.dp, if (current) Red.copy(alpha = 0.6f) else Line, shape).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(118.dp).aspectRatio(16f / 10f).clip(RoundedCornerShape(11.dp)).artBackground(a)) {
            RealImage(a, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color(0x40000000)))
            Box(Modifier.align(Alignment.Center).size(30.dp).clip(CircleShape).background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp), tint = Color.White)
            }
            Text("EP $n", Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xB3000000))
                .padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (frac > 0f) AnimatedProgressBar(frac, Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp), track = Color(0x59FFFFFF))
        }
        Column(Modifier.weight(1f)) {
            Text("Episode $n", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                color = if (seen) Color(0xB3FFFFFF) else Color.White)
            val (txt, col) = when {
                current -> I18n.t("now_watching", "Now watching") to RedText
                seen -> I18n.t("watched", "Watched") to GoodGreen
                progSec != null -> "${I18n.t("resume_at", "Resume at")} ${fmt(progSec)}" to RedText
                nextUp -> I18n.t("up_next", "Up next") to RedText
                else -> I18n.t("not_watched", "Not watched yet") to NavOff
            }
            Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (seen && !current) Icon(Icons.Filled.CheckCircle, null, Modifier.size(13.dp), tint = col)
                Text(txt, fontSize = 12.sp, color = col, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (nextUp && !seen && progSec == null) Text(a.title, Modifier.padding(top = 2.dp), fontSize = 12.sp, color = NavOff, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

@Composable
fun DetailScreen(vm: AppVM, id: String) {
    val a = Catalog.byId[id] ?: return
    val on = id in vm.p.myList
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var expanded by rememberSaveable(id) { mutableStateOf(false) }
    var newestFirst by rememberSaveable(id) { mutableStateOf(false) }
    val baseEps = if (a.available.isNotEmpty()) a.available else (1..a.eps).toList()
    val epNums = if (newestFirst) baseEps.reversed() else baseEps
    val sim = Catalog.similar(id)
    val resume = vm.p.last?.takeIf { it.id == id }
    var commentText by remember(id) { mutableStateOf("") }
    LaunchedEffect(id) { if (com.onimeku.app.data.Comments.enabled) vm.loadComments(id) }
    // Pemanasan data putar (debug.37): ambil info stream episode sasaran
    // tombol Play (dan episode lanjutan terakhir) SEKARANG, selagi user
    // membaca Detail. Di HP user, resolusi dingin endpoint putar ~5 dtk+
    // di 4G sempat menghabiskan batas tunggu pemutar -> "Stream
    // unavailable" padahal stream-nya hidup. Dengan prefetch, ketukan
    // Play mengenai cache dan video mulai nyaris seketika.
    LaunchedEffect(id) {
        com.onimeku.app.data.ApiClient.prefetchPlayback(id, a.available.firstOrNull() ?: 1)
        resume?.let { if (it.ep != (a.available.firstOrNull() ?: 1)) com.onimeku.app.data.ApiClient.prefetchPlayback(id, it.ep) }
    }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val heroH = (LocalConfiguration.current.screenHeightDp * 0.5f).coerceIn(320f, 470f).dp
    val heroPx = with(density) { heroH.toPx() }
    // 0 = hero terlihat penuh, 1 = hero sudah tergulung -> bilah atas jadi solid + judul muncul.
    val barA by remember(heroPx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else ((listState.firstVisibleItemScrollOffset - heroPx * 0.45f) / (heroPx * 0.3f)).coerceIn(0f, 1f)
        }
    }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    fun share() {
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, "Watch ${a.title} on Onimeku: onimeku://anime/$id")
        }
        ctx.startActivity(android.content.Intent.createChooser(send, I18n.t("share", "Share")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    Box(Modifier.fillMaxSize()) {
        // LazyColumn: judul panjang (ratusan episode) tidak digambar sekaligus.
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = bottomContentPadding())) {
            item(key = "hero") {
                // ===== HERO: gambar penuh lebar, parallax, judul menempel di bawahnya =====
                Box(Modifier.fillMaxWidth().height(heroH).clipToBounds().artBackground(a)) {
                    RealImage(a, Modifier.fillMaxSize().sharedArt(SharedArt.key?.takeIf { it.endsWith(":${a.id}") }).graphicsLayer {
                        val off = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else Int.MAX_VALUE
                        translationY = minOf(off * 0.45f, size.height * 0.12f)
                        scaleX = 1.15f; scaleY = 1.15f
                    })
                    // scrim atas (ikon terbaca) + peleburan ke latar di bawah
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                        0f to Color(0x99080910), 0.28f to Color.Transparent, 0.5f to Color(0x2214161F), 0.82f to Color(0xE614161F), 1f to Bg)))
                    Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().graphicsLayer {
                        val off = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else heroPx
                        val f = (off / (heroPx * 0.6f)).coerceIn(0f, 1f)
                        alpha = 1f - f; translationY = off * 0.3f
                        val sc = 1f - 0.06f * f; scaleX = sc; scaleY = sc
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)
                    }.padding(horizontal = Gutter)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            a.rating?.let { ScorePill(it) }
                            if (a.hd) Tag("HD", Red, Color.White)
                            StatusPill(a.status)
                        }
                        Text(a.title, Modifier.padding(top = 10.dp), fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            lineHeight = 33.sp, letterSpacing = (-0.5).sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(a.year.takeIf { it > 0 }?.toString(), "${a.eps} eps", a.genres.take(2).joinToString(", ").ifBlank { null }).joinToString("  ·  "),
                            Modifier.padding(top = 6.dp), fontSize = 13.sp, color = InkSoft, maxLines = 1)
                    }
                }
            }
            item(key = "actions") {
                Column(Modifier.padding(horizontal = Gutter).fadeSlideIn(60)) {
                    Spacer(Modifier.height(16.dp))
                    PlayButton(
                        label = if (resume != null) "${I18n.t("resume", "Resume Ep")} ${resume.ep}" else I18n.t("play", "Play"),
                        sub = resume?.let { r -> vm.p.prog["$id:${r.ep}"]?.let { "${I18n.t("continue_from", "Continue from")} ${fmt(it)}" } }
                            ?: if (resume == null) "${I18n.t("start_with", "Start with Episode")} ${a.available.firstOrNull() ?: 1}" else null,
                    ) { vm.go(Route.Player(id, resume?.ep ?: a.available.firstOrNull() ?: 1)) }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionTile(Icons.Filled.Add, if (on) I18n.t("added", "Added") else I18n.t("my_list", "My List"),
                            Modifier.weight(1f), on = on, iconOn = Icons.Filled.Check) { vm.toggleML(id) }
                        ActionTile(Icons.Outlined.Download, I18n.t("download_all", "Download all"), Modifier.weight(1f)) { vm.downloadAll(id) }
                        ActionTile(Icons.Outlined.Share, I18n.t("share", "Share"), Modifier.weight(1f)) { share() }
                    }
                }
            }
            if (a.genres.isNotEmpty()) item(key = "genres") {
                Row(Modifier.padding(top = 16.dp).horizontalScroll(rememberScrollState()).padding(horizontal = Gutter).fadeSlideIn(110),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    a.genres.forEach { g ->
                        Text(g, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = InkSoft, maxLines = 1,
                            modifier = Modifier.clip(CircleShape).background(Color(0x14FFFFFF)).padding(horizontal = 13.dp, vertical = 6.dp))
                    }
                }
            }
            item(key = "synopsis") {
                Column(Modifier.padding(horizontal = Gutter).padding(top = 20.dp).fadeSlideIn(160)) {
                    Text(I18n.t("synopsis", "Synopsis"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.padding(top = 8.dp).animateContentSize()) {
                        Text(a.synopsis, color = InkSoft, fontSize = 14.5.sp, lineHeight = 22.sp,
                            maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis)
                    }
                    if (a.synopsis.length > 180) {
                        Row(Modifier.padding(top = 6.dp).clip(CircleShape).clickable { expanded = !expanded }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(if (expanded) I18n.t("read_less", "Show less") else I18n.t("read_more", "Read more"),
                                color = RedText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, Modifier.size(18.dp), tint = RedText)
                        }
                    }
                }
            }
            // ===== Episode lebih dulu: ini alasan orang membuka halaman ini =====
            item(key = "ep_head") {
                Spacer(Modifier.height(26.dp))
                SectionTitle(I18n.t("episodes", "Episodes")) {
                    Text("${epNums.size}", fontSize = 13.sp, color = NavOff, modifier = Modifier.padding(end = 10.dp))
                    if (epNums.size > 1) Row(
                        Modifier.clip(CircleShape).background(Bg2).border(1.dp, Line, CircleShape)
                            .clickable { newestFirst = !newestFirst }.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.SwapVert, null, Modifier.size(15.dp), tint = InkSoft)
                        Text(if (newestFirst) "Newest" else "Oldest", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = InkSoft)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (epNums.isEmpty()) Text("No episodes available yet.", Modifier.padding(horizontal = Gutter), color = NavOff, fontSize = 14.sp)
            }
            itemsIndexed(epNums, key = { _, n -> "ep$n" }) { idx, n ->
                val k = "$id:$n"
                val pct = vm.downloading[k]
                val dl = vm.hasDl(id, n)
                Box(Modifier.staggerIn("ep:$id:$n", idx)) {
                EpisodeCard(
                    a, n, seen = k in vm.p.seen, progSec = vm.p.prog[k], current = resume?.ep == n,
                    onOpen = { vm.go(Route.Player(id, n)) },
                ) {
                    when {
                        pct != null -> DownloadRing(pct) { vm.download(id, n) }
                        dl -> IconChip(Icons.Filled.Check, "Saved offline", on = true) { vm.download(id, n) }
                        else -> IconChip(Icons.Outlined.Download, "Download episode $n") { vm.download(id, n) }
                    }
                }
                }
            }
            if (sim.isNotEmpty()) {
                item(key = "similar") {
                    Spacer(Modifier.height(22.dp))
                    SectionTitle("More like this")
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = Gutter)) {
                        items(sim, key = { it.id }) { x -> PosterCard(x, railW) { vm.go(Route.Detail(x.id)) } }
                    }
                }
            }
            // ==== Komentar (debug.31): backend Supabase (tabel comments).
            // Tamu bisa membaca; menulis wajib masuk. Hanya dirender bila backend akun aktif. ====
            if (com.onimeku.app.data.Comments.enabled) {
                item(key = "comments_head") {
                    Column(Modifier.padding(top = 28.dp)) {
                        SectionTitle("${I18n.t("comments", "Comments")} (${vm.comments.size})")
                        if (vm.s.account != null) {
                            Row(Modifier.padding(horizontal = Gutter).padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Bg2).border(1.dp, Line, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 13.dp)) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        commentText, { if (it.length <= 1000) commentText = it },
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Red),
                                        decorationBox = { inner -> if (commentText.isEmpty()) Text(I18n.t("comment_hint", "Write a comment…"), color = NavOff, fontSize = 14.sp); inner() },
                                    )
                                }
                                Btn(I18n.t("comment_post", "Post"), BtnStyle.Play, height = 46.dp) {
                                    vm.postComment(id, commentText); commentText = ""
                                }
                            }
                        } else {
                            Box(Modifier.padding(horizontal = Gutter).padding(top = 14.dp)) {
                                Chip(I18n.t("sign_in_to_comment", "Sign in to comment")) { vm.go(Route.Auth(false)) }
                            }
                        }
                    }
                }
                if (vm.commentsBusy && vm.comments.isEmpty()) {
                    item(key = "comments_busy") { Text("…", color = NavOff, modifier = Modifier.padding(horizontal = Gutter, vertical = 16.dp)) }
                } else if (vm.comments.isEmpty()) {
                    item(key = "comments_empty") {
                        Text(I18n.t("comment_empty", "No comments yet. Be the first."), color = NavOff, fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = Gutter, vertical = 16.dp))
                    }
                }
                items(vm.comments, key = { "comment:${it.id}" }) { c ->
                    val mine = c.user_id == vm.s.account?.userId
                    Row(Modifier.padding(horizontal = Gutter).padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(if (mine) Red else Solid), contentAlignment = Alignment.Center) {
                            Text(c.author.ifBlank { "U" }.take(1).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(c.author.ifBlank { "User" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (mine) RedText else Color.White)
                                Spacer(Modifier.weight(1f))
                                Text(if (c.created_at.length >= 16) c.created_at.substring(0, 10) + " " + c.created_at.substring(11, 16) else "", fontSize = 11.sp, color = NavOff)
                            }
                            Text(c.body, fontSize = 14.sp, color = InkSoft, lineHeight = 20.sp, modifier = Modifier.padding(top = 3.dp))
                            if (mine) Text(I18n.t("comment_delete", "Delete"), fontSize = 12.sp, color = NavOff,
                                modifier = Modifier.padding(top = 6.dp).clickable { vm.deleteComment(id, c.id) })
                        }
                    }
                }
            }
        }
        // ===== Bilah atas melayang: kaca di atas hero, jadi solid + judul saat digulir =====
        Box(Modifier.fillMaxWidth().height(statusTop + 58.dp).background(Bg.copy(alpha = barA * 0.97f)).padding(top = statusTop)) {
            Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") { vm.back() }
                Text(a.title, Modifier.weight(1f).graphicsLayer { alpha = barA }, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                GlassButton(if (on) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, if (on) "Remove from My List" else "Add to My List") { vm.toggleML(id) }
            }
        }
    }
}

@Composable
fun SeeAllScreen(vm: AppVM, key: String) {
    val (title, ids) = when {
        key == "top" -> I18n.t("sec_top", "Top Hits Anime") to Catalog.popular // semua judul urut skor, bukan hanya 10 teratas
        key == "all" -> I18n.t("sec_all", "All Anime") to Catalog.alphabetical
        key == "airing" -> I18n.t("sec_airing", "Airing Now") to Catalog.airing
        key == "popular" -> I18n.t("sec_popular", "Popular") to Catalog.popular
        key == "latest" -> "Latest Releases" to Catalog.latest
        key == "donghua" -> I18n.t("sec_donghua", "Donghua") to Catalog.donghua
        key.startsWith("genre:") -> key.removePrefix("genre:") to Catalog.byGenre(key.removePrefix("genre:"))
        else -> I18n.t("sec_new_eps", "New Episode Releases") to Catalog.new.map { it.first }
    }
    val items = ids.mapNotNull { Catalog.byId[it] }
    AnimeGrid(items, vm) {
        SubHead(title, vm)
        Text("${items.size} anime", color = NavOff, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
    }
}

// ------------------------------------------------------------------ NOTIFICATIONS
@Composable
fun NotifScreen(vm: AppVM) = Page {
    SubHead(I18n.t("notifications", "Notifications"), vm)
    if (!vm.s.settings.alerts) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(14.dp)).background(Bg2)
            .border(1.dp, Line, RoundedCornerShape(14.dp)).padding(12.dp)) {
            Text("New episode alerts are off. ", fontSize = 13.sp, color = InkSoft)
            Text("Turn on in Settings", fontSize = 13.sp, color = RedText, modifier = Modifier.clickable { vm.openSettings() })
        }
    }
    if (Catalog.all.isEmpty()) SkeletonRows(4)
    Catalog.notifs.forEach { (id, ep, whenTxt) ->
        val a = Catalog.byId[id] ?: return@forEach
        Column {
            Row(Modifier.fillMaxWidth().clickable { vm.go(Route.Detail(id)) }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(if (vm.s.notifRead) Color.Transparent else Red))
                Text(androidx.compose.ui.text.buildAnnotatedString {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)); append(a.title); pop()
                    append(" episode $ep is out")
                }, Modifier.weight(1f), fontSize = 14.sp)
                Text(whenTxt, fontSize = 12.sp, color = NavOff)
            }
            HLine()
        }
    }
    if (!vm.s.notifRead) Box(Modifier.padding(top = 16.dp)) { Btn(I18n.t("mark_all_read", "Mark all as read"), BtnStyle.Solid) { vm.markAllRead() } }
}

// ------------------------------------------------------------------ SEARCH
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(vm: AppVM) {
    var q by rememberSaveable { mutableStateOf("") }
    // Debounce 500 ms: hasil pencarian mengikuti teks yang sudah berhenti
    // berubah, bukan setiap ketukan.
    var appliedQ by remember { mutableStateOf("") }
    LaunchedEffect(q) { kotlinx.coroutines.delay(500); appliedQ = q }
    var gs by remember { mutableStateOf(listOf<String>()) }
    var yr by remember { mutableStateOf("") }
    var st by remember { mutableStateOf("") }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { fr.requestFocus() }
    val any = gs.isNotEmpty() || yr.isNotEmpty() || st.isNotEmpty()

    @Composable fun group(label: String, content: @Composable RowScope.() -> Unit) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, Modifier.width(52.dp), fontSize = 12.sp, color = NavOff)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }

    val query = appliedQ.trim().lowercase()
    val results = remember(query, gs, yr, st, Catalog.all) {
        if (query.isEmpty() && !any) emptyList()
        else Catalog.all.filter { c ->
            (query.isEmpty() || c.title.lowercase().contains(query) || c.genres.joinToString(" ").lowercase().contains(query)) &&
                gs.all { it in c.genres } && (yr.isEmpty() || c.year.toString() == yr) && (st.isEmpty() || c.status == st)
        }
    }

    // Hasil memakai grid lazy: mengetik di katalog besar tidak lagi menggambar ratusan kartu sekaligus.
    AnimeGrid(results, vm) {
        SubHead(I18n.t("search", "Search"), vm)
        Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp)).background(Bg2).border(1.dp, if (q.isNotEmpty()) Red.copy(alpha = 0.7f) else Line, RoundedCornerShape(18.dp)).padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Search, null, Modifier.size(22.dp), tint = if (q.isNotEmpty()) RedText else NavOff)
            BasicTextField(q, { q = it }, Modifier.weight(1f).focusRequester(fr), singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp), cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.addHist(q) }),
                decorationBox = { inner -> if (q.isEmpty()) Text(I18n.t("search_hint2", "Search anime or genre"), color = NavOff); inner() })
            if (q.isNotEmpty()) Box(Modifier.size(36.dp).clip(CircleShape).clickable { q = "" }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, "Clear", Modifier.size(18.dp), tint = InkSoft)
            }
        }
        group("Genre") { Catalog.chips.forEach { g -> Chip(g, g in gs) { gs = if (g in gs) gs - g else gs + g } } }
        group("Status") { Catalog.statuses.forEach { v -> Chip(v, st == v) { st = if (st == v) "" else v } } }
        group("Year") {
            Catalog.years.forEach { v -> Chip(v, yr == v) { yr = if (yr == v) "" else v } }
            if (any) Chip("Clear") { gs = emptyList(); yr = ""; st = "" }
        }
        Spacer(Modifier.height(8.dp))
        if (q.trim().lowercase() != query) {
            androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = Red, trackColor = Color.Transparent)
        }

        if (query.isEmpty() && !any) {
            if (vm.s.hist.isEmpty()) EmptyBox("Find your next anime", "Search a title or genre, or pick filters above.")
            else {
                Text("Recent searches", Modifier.padding(top = 10.dp), color = NavOff, fontSize = 13.sp)
                FlowRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.s.hist.forEach { h -> Chip(h) { q = h } }
                    Chip("Clear history") { vm.clearHist() }
                }
            }
        } else if (results.isEmpty()) {
            EmptyBox(I18n.t("no_results_t", "No results"), I18n.t("no_results_b", "Nothing matches. Try fewer filters or check the spelling."))
        } else {
            Text("${results.size} result${if (results.size > 1) "s" else ""}", Modifier.padding(bottom = 2.dp), color = NavOff, fontSize = 13.sp)
        }
    }
}
````

### `app/src/main/java/com/onimeku/app/ui/Subtitles.kt` **[BARU]**

````kotlin
package com.onimeku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.onimeku.app.I18n
import com.onimeku.app.data.ApiSubtitle

/** Logika pemilihan subtitle otomatis: Indonesia / English, mengikuti preferensi & bahasa aplikasi. */
object SubLang {
    /** "id", "en", atau null (bahasa lain) dari kode bahasa + label trek. */
    fun code(lang: String, label: String = ""): String? {
        val s = "$lang $label".lowercase()
        return when {
            s.contains("indo") || Regex("\\b(id|ind)\\b").containsMatchIn(s) -> "id"
            s.contains("engl") || Regex("\\b(en|eng)\\b").containsMatchIn(s) -> "en"
            else -> null
        }
    }

    fun code(t: ApiSubtitle): String? = code(t.lang, t.label)

    /** Nama ramah untuk ditampilkan. */
    fun display(t: ApiSubtitle, i: Int): String = when (code(t)) {
        "id" -> "Indonesia"
        "en" -> "English"
        else -> t.label.ifBlank { t.lang.uppercase() }.ifBlank { "Sub ${i + 1}" }
    }

    fun prefName(pref: String): String = when (pref) {
        "id" -> "Indonesia"; "en" -> "English"; "off" -> I18n.t("ctl_off", "Off"); else -> "Auto"
    }

    /**
     * Pilih indeks trek. pref: "auto" | "id" | "en" | "off". Auto mengikuti bahasa aplikasi
     * (id -> Indonesia dulu, selain itu English dulu); bila bahasa pilihan tidak ada, jatuh ke
     * bahasa kedua (Indonesia <-> English), lalu trek pertama. -1 = mati.
     */
    fun pick(tracks: List<ApiSubtitle>, pref: String, appLang: String): Int {
        if (pref == "off" || tracks.isEmpty()) return -1
        val order = when (pref) {
            "id" -> listOf("id", "en")
            "en" -> listOf("en", "id")
            else -> if (appLang == "id") listOf("id", "en") else listOf("en", "id")
        }
        for (c in order) { val i = tracks.indexOfFirst { code(it) == c }; if (i >= 0) return i }
        return 0
    }
}

@Composable
private fun SubOpt(label: String, sub: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) RedText else androidx.compose.ui.graphics.Color.White)
            if (sub != null) Text(sub, fontSize = 12.5.sp, color = NavOff, modifier = Modifier.padding(top = 2.dp))
        }
        if (selected) Icon(Icons.Filled.Check, null, Modifier.size(20.dp), tint = RedText)
    }
}

/** Menu subtitle: Auto, Off, dan semua trek tersedia (Indonesia / English didahulukan). */
@Composable
fun SubtitleDialog(
    tracks: List<ApiSubtitle>, current: Int, pref: String, appLang: String,
    onTrack: (Int) -> Unit, onOff: () -> Unit, onAuto: () -> Unit, onDismiss: () -> Unit,
) {
    val order = tracks.indices.sortedBy { i -> when (SubLang.code(tracks[i])) { "id" -> 0; "en" -> 1; else -> 2 } }
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(22.dp)
        Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).clip(shape).background(Bg2).border(1.dp, Line, shape).padding(vertical = 12.dp)) {
            Text(I18n.t("subtitles", "Subtitles"), Modifier.padding(horizontal = 22.dp, vertical = 8.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val autoSub = if (pref == "auto" && current >= 0) tracks.getOrNull(current)?.let { SubLang.display(it, current) } else null
                SubOpt("Auto", (I18n.t("sub_auto_d", "Indonesia / English, automatically")) + (autoSub?.let { " · $it" } ?: ""), pref == "auto") { onAuto() }
                order.forEach { i ->
                    SubOpt(SubLang.display(tracks[i], i), null, pref != "auto" && current == i) { onTrack(i) }
                }
                SubOpt(I18n.t("ctl_off", "Off"), null, current < 0) { onOff() }
            }
        }
    }
}
````

### `app/src/main/java/com/onimeku/app/ui/Theme.kt`

````kotlin
package com.onimeku.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF14161F)
val Bg2 = Color(0xFF1B1E2A)
val InkSoft = Color(0xFFC9CCD8)
// Palet merah tua modern (crimson): Red untuk tombol/indikator, RedText lebih terang
// supaya teks & ikon merah tetap terbaca di latar gelap (kontras >= 4.5:1).
val Red = Color(0xFFA6122D)
val RedDeep = Color(0xFF6E0F1F)
val RedText = Color(0xFFE8566F)
// Merah karya pemilik untuk LOGO di semua tempat (aturan 16 Okt 2026);
// tema antarmuka tetap crimson Red di atas (debug.31: logo dikembalikan
// dari #A6122D ke #F20A17 sesuai karya aslinya).
val LogoRed = Color(0xFFF20A17)
val Line = Color(0xFF2A2D3B)
val NavOff = Color(0xFF9EA4B8)
// Emas untuk skor/rating (satu-satunya aksen hangat; jangan dipakai untuk tombol).
val Gold = Color(0xFFFFC857)
val Solid = Color(0xFF2B2E3B)

@Composable
fun OnimekuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Red, onPrimary = Color.White, secondary = RedText, error = RedText,
            background = Bg, onBackground = Color.White,
            surface = Bg2, onSurface = Color.White,
        ),
        content = content,
    )
}
````

### `app/src/main/java/com/onimeku/app/ui/WelcomeScreen.kt` **[DIUBAH]**

````kotlin
package com.onimeku.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.AppVM
import com.onimeku.app.I18n
import kotlinx.coroutines.delay

private val slides = listOf(
    Triple("Welcome To\n", "Onimeku.", "Watch Anime Online, Streaming Online"),
    Triple("Thousands\nof ", "Episodes.", "Pick from a big library in HD, with new episodes every day."),
    Triple("Watch\n", "Anywhere.", "Download your favorite episodes and keep watching offline."),
)

@Composable
fun WelcomeScreen(vm: AppVM) {
    var idx by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(tick) { while (true) { delay(4500); idx = (idx + 1) % slides.size } }

    Box(Modifier.fillMaxSize().background(Color(0xFF161821))) {
        Canvas(Modifier.fillMaxWidth().fillMaxHeight(0.64f)) {
            val w = size.width; val h = size.height
            fun strip(top: Float, hh: Float, brush: Brush) =
                rotate(-12f, Offset(w / 2, top + hh / 2)) { drawRect(brush, Offset(-0.15f * w, top), Size(1.3f * w, hh)) }
            strip(h * 0.06f, h * 0.36f, Brush.linearGradient(listOf(Color(0xFF6F8A4A), Color(0xFFD6B46A), Color(0xFF2D6A9F)), Offset.Zero, Offset(w, h * 0.3f)))
            strip(h * 0.34f, h * 0.30f, Brush.linearGradient(listOf(Color(0xFF6E0F1F), Color(0xFFA6122D), Color(0xFF2E0710)), Offset.Zero, Offset(w, h * 0.3f)))
            strip(h * 0.58f, h * 0.46f, Brush.linearGradient(listOf(Color(0xFF4A2C26), Color(0xFFB7653C), Color(0xFF7B3B2A)), Offset.Zero, Offset(w, h * 0.3f)))
            drawCircle(Color(0xFFF4C28A), w * 0.07f, Offset(w * 0.38f, h * 0.20f))
            drawCircle(Color(0xFFE9A58B), w * 0.08f, Offset(w * 0.62f, h * 0.72f))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0.30f to Color.Transparent, 0.56f to Color(0xEB161821), 0.68f to Color(0xFF161821))))

        // Logo masuk: memutar + memantul dengan pegas, lalu melayang pelan.
        val intro = remember { androidx.compose.animation.core.Animatable(0f) }
        LaunchedEffect(Unit) { intro.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 110f)) }
        val float = androidx.compose.animation.core.rememberInfiniteTransition(label = "logoFloat")
        val fy by float.animateFloat(
            initialValue = -6f, targetValue = 6f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(tween(2200, easing = androidx.compose.animation.core.FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
            label = "logoFloatY",
        )
        Box(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 96.dp).graphicsLayer {
                val v = intro.value
                scaleX = v; scaleY = v; rotationZ = (1f - v) * -140f; alpha = v.coerceIn(0f, 1f)
                translationY = fy.dp.toPx() * v
            },
        ) { Logo(88.dp) }

        Text(I18n.t("skip_for_now", "Skip for now"), Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp).clip(CircleShape)
            .background(Color(0x8C14161F)).clickable { vm.closeWelcome() }.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 13.sp, fontWeight = FontWeight.Medium)

        Column(Modifier.align(Alignment.BottomCenter).widthIn(max = 560.dp).navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            // Teks slide berganti dengan fade + geser halus.
            AnimatedContent(
                targetState = idx,
                transitionSpec = { (fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 6 }) togetherWith fadeOut(tween(200)) },
                label = "welcomeText",
            ) { i ->
                val (pl, e, sb) = slides[i]
                Column {
                    Text(buildAnnotatedString {
                        append(pl); withStyle(SpanStyle(color = Red)) { append(e) }
                    }, fontSize = 46.sp, fontWeight = FontWeight.Bold, lineHeight = 50.sp, letterSpacing = (-1.5).sp)
                    Text(sb, Modifier.padding(top = 16.dp), fontSize = 20.sp, lineHeight = 29.sp, color = Color(0xFFF2F2F6))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                slides.indices.forEach { i ->
                    val dotW by animateDpAsState(if (i == idx) 42.dp else 10.dp, tween(300), label = "dotW")
                    val dotC by animateColorAsState(if (i == idx) Red else Color(0xFF5A5F6E), tween(300), label = "dotC")
                    Box(Modifier.padding(start = 9.dp).height(10.dp).width(dotW).clip(CircleShape)
                        .background(dotC).clickable { idx = i; tick++ })
                }
            }
            Row(Modifier.padding(top = 22.dp).fadeSlideIn(150), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Btn("Sign in", BtnStyle.Solid, modifier = Modifier.weight(1f), height = 58.dp) { vm.closeWelcome(); vm.go(com.onimeku.app.Route.Auth(false)) }
                Btn("Sign up", BtnStyle.Play, modifier = Modifier.weight(1f), height = 58.dp) { vm.closeWelcome(); vm.go(com.onimeku.app.Route.Auth(true)) }
            }
        }
    }
}
````

### `app/src/test/java/com/onimeku/app/CoreTest.kt`

````kotlin
package com.onimeku.app

import com.onimeku.app.data.Downloads
import com.onimeku.app.data.Last
import com.onimeku.app.data.Profile
import com.onimeku.app.data.Sync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreTest {
    @Test fun downloadKeyRoundTrip() {
        val k = Downloads.key("one-piece", 12)
        assertEquals("one-piece" to 12, Downloads.parseKey(k))
        assertNull(Downloads.parseKey("bukan-kunci"))
        assertNull(Downloads.parseKey("dl:x"))
    }

    @Test fun mergeUnionsListAndKeepsFurthestProgress() {
        val local = listOf(Profile("Me", myList = listOf("a", "b"), prog = mapOf("a:1" to 100), seen = listOf("b:1"), last = Last("a", 1)))
        val remote = listOf(
            Profile("Me", myList = listOf("b", "c"), prog = mapOf("a:1" to 300, "b:1" to 50), seen = listOf("c:1")),
            Profile("Kid", myList = listOf("z")),
        )
        val m = Sync.merge(local, remote)
        assertEquals(listOf("a", "b", "c"), m[0].myList)
        assertEquals(300, m[0].prog["a:1"])
        assertTrue("b:1" !in m[0].prog)          // sudah ditonton -> bukan progres
        assertEquals(setOf("b:1", "c:1"), m[0].seen.toSet())
        assertEquals("Kid", m[1].name)
    }

    @Test fun mergeCapsAtFourProfiles() {
        val local = listOf(Profile("A"), Profile("B"), Profile("C"))
        val remote = listOf(Profile("D"), Profile("E"))
        assertEquals(4, Sync.merge(local, remote).size)
    }
}
````

### `app/build.gradle.kts`

````kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.onimeku.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.onimeku.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        // Kunci publik Supabase untuk Sign in/Sign up (isi di gradle.properties; kosong = fitur akun nonaktif anggun)
        fun prop(n: String) = (project.findProperty(n) as String?)?.replace("\"", "") ?: ""
        buildConfigField("String", "SUPABASE_URL", "\"${prop("ONIMEKU_SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${prop("ONIMEKU_SUPABASE_KEY")}\"")
    }
    // Tanda tangan rilis: aktif hanya bila ONIMEKU_STORE_FILE diberikan (gradle.properties / -P / CI).
    signingConfigs {
        create("release") {
            val f = project.findProperty("ONIMEKU_STORE_FILE") as String?
            if (!f.isNullOrBlank()) {
                storeFile = file(f)
                storePassword = project.findProperty("ONIMEKU_STORE_PASS") as String?
                keyAlias = project.findProperty("ONIMEKU_KEY_ALIAS") as String?
                keyPassword = (project.findProperty("ONIMEKU_KEY_PASS") as String?) ?: storePassword
            }
        }
    }
    buildTypes {
        release {
            if (!(project.findProperty("ONIMEKU_STORE_FILE") as String?).isNullOrBlank()) signingConfig = signingConfigs.getByName("release")
            // R8: kode lebih kecil + start lebih cepat. Aturan keep ada di proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.0")
    implementation("androidx.media3:media3-database:1.5.0")
    implementation("androidx.media3:media3-ui:1.5.0")
    implementation("androidx.media3:media3-session:1.5.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.media3:media3-datasource-okhttp:1.5.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    // Memasang Baseline Profile (src/main/baseline-prof.txt) saat instalasi: start dan scroll pertama lebih mulus.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    testImplementation("junit:junit:4.13.2")
}
````

### `app/src/main/AndroidManifest.xml` **[DIUBAH]**

````xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

    <application
        android:name=".OnimekuApp"
        android:enableOnBackInvokedCallback="true"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.Onimeku">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:supportsPictureInPicture="true"
            android:resizeableActivity="true"
            android:launchMode="singleTask"
            android:windowSoftInputMode="adjustResize"
            android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <!-- Tautan dalam: onimeku://anime/<id> (tombol Share di Detail) -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="onimeku" android:host="anime" />
            </intent-filter>
        </activity>
        <!-- Pemutaran latar + kontrol layar kunci/notifikasi (MediaSession) -->
        <service
            android:name=".PlaybackService"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>
        <service
            android:name=".data.OnimekuDownloadService"
            android:exported="false"
            android:foregroundServiceType="dataSync" />
    </application>
</manifest>
````

### `app/src/main/res/drawable/ic_launcher_foreground.xml`

````xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <group android:pivotX="54" android:pivotY="54" android:rotation="45">
        <path android:fillColor="#F20A17"
            android:pathData="M44,34 h20 a10,10 0 0 1 10,10 v20 a10,10 0 0 1 -10,10 h-20 a10,10 0 0 1 -10,-10 v-20 a10,10 0 0 1 10,-10 z" />
    </group>
    <path android:strokeColor="#FFFFFF" android:strokeWidth="5" android:fillColor="#00000000"
        android:pathData="M54,47 a7,7 0 1 1 0,14 a7,7 0 1 1 0,-14 z" />
</vector>
````

### `app/src/main/res/drawable/ic_notify.xml`

````xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF"
        android:pathData="M12,2a7,7 0 0 0 -7,7v4l-2,3v1h18v-1l-2,-3V9a7,7 0 0 0 -7,-7zM10,20a2,2 0 0 0 4,0z" />
</vector>
````

### `app/src/main/res/drawable/ic_pip_next.xml` **[BARU]**

````xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M6,18l8.5,-6L6,6v12zM16,6v12h2V6h-2z"/>
</vector>
````

### `app/src/main/res/drawable/ic_pip_pause.xml` **[BARU]**

````xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M6,19h4V5H6v14zM14,5v14h4V5h-4z"/>
</vector>
````

### `app/src/main/res/drawable/ic_pip_play.xml` **[BARU]**

````xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M8,5v14l11,-7z"/>
</vector>
````

### `app/src/main/res/drawable/ic_pip_prev.xml` **[BARU]**

````xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M6,6h2v12H6zM9.5,12l8.5,6V6z"/>
</vector>
````

### `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_bg" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
````

### `app/src/main/res/values/colors.xml`

````xml
<resources>
    <color name="ic_bg">#14161F</color>
</resources>
````

### `app/src/main/res/values/strings.xml`

````xml
<resources>
    <string name="app_name">Onimeku</string>
    <string name="dl_channel_name">Downloads</string>
    <string name="dl_channel_desc">Episode download progress</string>
</resources>
````

### `app/src/main/res/values/themes.xml`

````xml
<resources>
    <style name="Theme.Onimeku" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">#14161F</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
    </style>
</resources>
````

### `app/src/main/res/xml/backup_rules.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Aturan cadangan Android lama (API < 31): kecualikan token terenkripsi. -->
<full-backup-content>
    <exclude domain="sharedpref" path="onimeku_secure.xml" />
</full-backup-content>
````

### `app/src/main/res/xml/data_extraction_rules.xml`

````xml
<?xml version="1.0" encoding="utf-8"?>
<!--
  Token akun disimpan terenkripsi (EncryptedSharedPreferences
  "onimeku_secure"); kunci Keystore-nya TIDAK ikut cadangan/pindah HP,
  jadi berkas itu dikecualikan dari backup & transfer — bila ikut,
  hasil restore adalah token yang tidak bisa dibuka (debug.31).
  Preferensi biasa tetap dicadangkan seperti biasa.
-->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="sharedpref" path="onimeku_secure.xml" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="sharedpref" path="onimeku_secure.xml" />
    </device-transfer>
</data-extraction-rules>
````

### `build.gradle.kts`

````kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}
````

### `gradle.properties`

````properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official

# --- Akun (Sign in/Sign up lewat Supabase Auth) ---
# Isi dengan URL proyek dan kunci PUBLIK (sb_publishable_... / anon). Kosong = fitur akun nonaktif anggun.
# JANGAN memasukkan service_role key. Untuk CI, kirim sebagai -P atau env ORG_GRADLE_PROJECT_*.
ONIMEKU_SUPABASE_URL=
ONIMEKU_SUPABASE_KEY=<disembunyikan>
````

### `settings.gradle.kts`

````kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Onimeku"
include(":app")
````
