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
    // Keadaan fullscreen pemutar disimpan di VM: sebelumnya di
    // PlayerReady yang dibongkar tiap pindah episode sehingga fullscreen
    // selalu lepas. Direset otomatis saat rute teratas bukan pemutar.
    var playerFull by mutableStateOf(false)
    /** Id anime yang menu aksi cepatnya (tekan-lama kartu) sedang terbuka; null = tertutup. */
    var quick by mutableStateOf<String?>(null)
    // Posisi (ms) saat jalur putar gagal & pindah ke jalur cadangan: pemutar baru melanjutkan dari sini, bukan dari progres 10 dtk terakhir.
    var resumeMs: Pair<String, Long>? = null
    // Kecepatan putar dipertahankan antar episode (diatur dari roda gigi pemutar).
    var playerSpeed by mutableStateOf(1f)
    var toastMsg by mutableStateOf<String?>(null); private set
    var welcomeOpen by mutableStateOf(!s.welcomeSeen); private set
    var calDay by mutableIntStateOf(0)
    val downloading = mutableStateMapOf<String, Int>()

    /** Posisi scroll Home disimpan di sini agar tidak kembali ke atas setelah membuka Detail lalu Back. */
    val homeScroll = LazyListState()
    /** Genre terpilih di grid "All Anime" Beranda (null = Semua). Disimpan di VM agar bertahan saat pindah layar. */
    var homeGenre by mutableStateOf<String?>(null)

    /** Jarak gulir Home dalam piksel (0 di puncak; besar bila item pertama sudah lewat). Dibaca di lambda graphicsLayer/derivedStateOf, bukan di komposisi. */
    fun homeScrollPx(): Int = if (homeScroll.firstVisibleItemIndex == 0) homeScroll.firstVisibleItemScrollOffset else Int.MAX_VALUE / 2

    /** True saat dialog modal (mis. konfirmasi keluar) tampil; auto-slide hero dijeda. */
    var overlayOpen by mutableStateOf(false)

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

    // Kesegaran katalog: kapan terakhir data katalog tiba dari API
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

    // Judul yang sudah/sedang diperkaya dari endpoint detail sesi ini.
    private val detailEnriched = mutableSetOf<String>()

    /**
     * Perbaiki entri katalog-lite sebuah judul dari endpoint detail
     * (sinopsis penuh + daftar episode asli) saat layar Detail dibuka.
     * Sekali per judul per sesi; gagal jaringan boleh dicoba lagi.
     */
    fun loadDetail(id: String) {
        if (!detailEnriched.add(id)) return
        viewModelScope.launch {
            detailBusy = id
            val full = try { com.onimeku.app.data.ApiClient.fetchDetail(id) } catch (e: Exception) { null }
            if (full != null) com.onimeku.app.data.Catalog.enrich(full) else detailEnriched.remove(id)
            if (detailBusy == id) detailBusy = null
        }
    }
    /** Id judul yang detail lengkapnya (sinopsis, episode) sedang diambil; null bila tidak ada. Memicu skeleton di Detail. */
    var detailBusy by mutableStateOf<String?>(null); private set
    /** True selama katalog disinkronkan dari jaringan (muat awal, tarik-segar, segar-otomatis). Memicu bilah loading global. */
    var syncing by mutableStateOf(false); private set

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
        catalogBusy = true; syncing = true
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
                catalogBusy = false; syncing = false
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
    /** Arah perpindahan tab: -1 ke kiri, 1 ke kanan, 0 bukan perpindahan tab (dibaca transisi rute di MainActivity). */
    var tabDir = 0
    /** Urutan tab di bilah bawah (Downloads tidak ada di bilah; disisipkan agar tetap berurutan). -1 = bukan halaman akar tab. */
    fun tabIndex(r: Route): Int = when (r) {
        Route.Home -> 0; Route.Calendar -> 1; Route.MyList -> 2; Route.Downloads -> 3; Route.Search -> 4; Route.ProfileTab -> 5; else -> -1
    }
    private var lastGoAt = 0L
    fun go(r: Route) {
        val now = android.os.SystemClock.uptimeMillis()
        val top = stack.lastOrNull()
        // Anti ketuk ganda: rute yang sama persis, atau layar sejenis dalam 450 ms, diabaikan. Tanpa ini dua ketukan cepat
        // menumpuk dua Detail/Player (transisi terpotong, tombol kembali harus ditekan dua kali, dua pemutar hidup).
        if (r == top || (top != null && top::class == r::class && now - lastGoAt < 450L)) return
        lastGoAt = now
        com.onimeku.app.ui.SharedArt.expireStale()
        navDir = 1; tabDir = 0; stack = stack + r; afterNav()
    }
    fun back(animated: Boolean = true) { navDir = if (animated) -1 else 0; tabDir = 0; stack = if (stack.size > 1) stack.dropLast(1) else listOf(Route.Home); afterNav() }
    fun setTab(r: Route) { val from = tabIndex(tab); val to = tabIndex(r); tabDir = if (from >= 0 && to >= 0) Integer.signum(to - from) else 0; navDir = 0; stack = listOf(r); afterNav(); if (r == Route.Home) refreshIfStale() }
    fun replaceTop(r: Route) { navDir = 0; tabDir = 0; stack = stack.dropLast(1) + r; afterNav() }
    private fun afterNav() { quick = null; if (stack.lastOrNull() !is Route.Player) playerFull = false }
    init { if (s.settings.subTranslate) com.onimeku.app.ui.SubTranslate.warm(app) }

    fun openSettings() { tabDir = 0; stack = listOf(Route.ProfileTab, Route.Sub("settings")) }

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
            val c = Catalog.byId[id] ?: return
            val soon = c.apiStatus == "NOT_YET_RELEASED"
            toast(if (soon) "We will notify you when it is out" else "We will remind you")
            ensure { g -> if (g) Notifier.show(app, "Reminder set", if (soon) "We’ll notify you when ${c.title} is released" else "${c.title} airs at ${c.time}") }
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
     *  agar downloadAll bisa menjalankannya BERURUTAN. */
    private suspend fun startDownload(id: String, ep: Int, silent: Boolean = false) {
        val k = "$id:$ep"
        val pb = try { ApiClient.fetchPlayback(id, ep, fresh = true) } catch (e: Exception) { null }
        val url = pb?.streamUrl
        if (pb == null || url.isNullOrBlank() || !url.contains(".m3u8")) {
            autoQueued.remove(k)
            if (!silent) toast("This title can't be downloaded")
            return
        }
        if (!hasDl(id, ep)) upd { it.copy(downloads = it.downloads + Dl(id, ep)) }
        downloading[k] = 0
        Downloads.enqueue(app, id, ep, url, pb.referer) {
            autoQueued.remove(k)
            downloading.remove(k)
            upd { s2 -> s2.copy(downloads = s2.downloads.filterNot { it.id == id && it.ep == ep }) }
            if (!silent) toast("Download failed to start: Episode $ep")
        }
        toast(if (silent) "Saving Episode $ep for offline" else "Download started: Episode $ep")
    }

    fun downloadAll(id: String) {
        val a = Catalog.byId[id] ?: return
        val nums = if (a.available.isNotEmpty()) a.available else (1..a.eps).toList()
        val todo = nums.filter { !hasDl(id, it) }
        if (todo.isEmpty()) { toast("All episodes already saved"); return }
        toast("Starting ${todo.size} downloads")
        // Berurutan: satu pengambilan data putar pada satu
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

    // ---- Komentar per judul ----
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

    // Penghitung detik menonton: selisih posisi antar panggilan saveProg pada episode yang sama (maks 30 dtk per panggilan, seek jauh tidak dihitung).
    private var statKey = ""
    private var statT = 0

    fun saveProg(id: String, ep: Int, t: Int, dur: Int) {
        val k = "$id:$ep"
        val delta = if (k == statKey && t > statT) (t - statT).coerceAtMost(30) else 0
        statKey = k; statT = t
        updP { pr ->
            var base = pr.base; var prog = pr.prog; var seen = pr.seen; var last = pr.last; var watch = pr.watch
            if (t >= 5) base = id
            if (dur > 0 && t >= dur * 0.9) { prog = prog - k; if (k !in seen) seen = seen + k }
            else if (t >= 5) { prog = prog + (k to t); last = Last(id, ep) }
            // Catatan tontonan (untuk Continue Watching): hanya putar yang
            // benar-benar berjalan (>=5 dtk), terbaru di depan, maks 24 judul.
            if (t >= 5) watch = (listOf(Watch(id, ep, System.currentTimeMillis(), dur)) + watch.filter { it.id != id }).take(24)
            pr.copy(base = base, prog = prog, seen = seen, last = last, watch = watch, days = Stats.addSeconds(pr.days, delta))
        }
        if (s.settings.autoDlNext && dur > 0 && t >= dur * 0.6) maybeAutoDownloadNext(id, ep)
        refreshWidget(force = dur > 0 && t >= dur * 0.9)
    }

    private var widgetAt = 0L
    /** Segarkan widget Continue Watching; dibatasi sekali per 60 dtk kecuali [force] (episode baru tuntas). */
    private fun refreshWidget(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - widgetAt < 60_000L) return
        widgetAt = now
        com.onimeku.app.widget.ContinueWidget.requestUpdate(app)
    }

    // ---------- unduh otomatis episode berikutnya (Wi-Fi) ----------
    private val autoQueued = HashSet<String>()

    private fun maybeAutoDownloadNext(id: String, ep: Int) {
        val a = Catalog.byId[id] ?: return
        val aired = if (a.ep > 0) a.ep else a.eps
        val nums = if (a.available.isNotEmpty()) a.available else (1..aired).toList()
        val next = nums.firstOrNull { it > ep } ?: return
        val k = "$id:$next"
        if (hasDl(id, next) || downloading.containsKey(k) || !autoQueued.add(k)) return
        val cm = app.getSystemService(ConnectivityManager::class.java)
        val unmetered = try {
            cm != null && !cm.isActiveNetworkMetered &&
                cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) { false }
        if (!unmetered) { autoQueued.remove(k); return } // coba lagi saat tersambung Wi-Fi
        val free = try { android.os.StatFs(app.filesDir.path).availableBytes } catch (e: Exception) { Long.MAX_VALUE }
        if (free < 1024L * 1024 * 1024) { autoQueued.remove(k); return }
        viewModelScope.launch { startDownload(id, next, silent = true) }
    }

    // ---------- Surprise me ----------
    /** Pilih acak judul berskor bagus yang belum pernah ditonton lalu buka Detail-nya. */
    fun surprise() {
        val watched = HashSet<String>().apply {
            p.watch.forEach { add(it.id) }
            (p.prog.keys + p.seen).forEach { add(it.substringBeforeLast(':')) }
        }
        fun pool(min: Double) = Catalog.unique.filter {
            (it.score ?: 0.0) >= min && it.id !in watched && (it.ep > 0 || it.available.isNotEmpty())
        }
        val pick = pool(7.5).ifEmpty { pool(7.0) }.randomOrNull()
        if (pick == null) { toast(I18n.t("surprise_none", "Nothing new to suggest right now")); return }
        go(Route.Detail(pick.id))
    }

    // ---------- tandai ditonton ----------
    /** Tandai episode 1..[upTo] judul ini sudah ditonton (untuk pengguna yang pindah dari aplikasi lain). */
    fun markSeenUpTo(id: String, upTo: Int) {
        val a = Catalog.byId[id] ?: return
        val all = if (a.available.isNotEmpty()) a.available else (1..a.eps).toList()
        val nums = all.filter { it <= upTo }
        if (nums.isEmpty()) return
        val keys = nums.map { "$id:$it" }
        val next = all.firstOrNull { it > upTo } ?: upTo
        updP { pr ->
            pr.copy(
                seen = pr.seen + keys.filter { it !in pr.seen },
                prog = pr.prog - keys.toSet(),
                base = pr.base ?: id,
                last = Last(id, next),
                watch = (listOf(Watch(id, upTo, System.currentTimeMillis(), 0)) + pr.watch.filter { it.id != id }).take(24),
            )
        }
        refreshWidget(force = true)
        toast("Episodes ${nums.first()}–${nums.last()} marked as watched")
    }

    /** Batalkan tanda ditonton satu episode. */
    fun unmarkSeen(id: String, ep: Int) {
        val k = "$id:$ep"
        updP { pr -> pr.copy(seen = pr.seen - k, prog = pr.prog - k) }
        toast("Episode $ep marked as unwatched")
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
    fun switchProfile(i: Int) { upd { it.copy(pi = i) }; refreshWidget(force = true); toast("Switched to ${s.profiles[i].name}"); setTab(Route.ProfileTab) }
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
