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

    /** Jadwal tayang asli dari API: id katalog -> episode berikutnya + waktu. */
    var schedule by mutableStateOf<Map<String, ApiScheduleItem>>(emptyMap())
        private set
    var inPip by mutableStateOf(false)
    var playing by mutableStateOf(false)

    init {
        Downloads.oneTimeResetV13(app)
        PosterHealer.init(app)
        Reminders.rescheduleAll(app, s)
        initDownloads()
        watchNetwork()
        reloadCatalog()
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
                } else {
                    Catalog.loadFailed = Catalog.all.isEmpty()
                }
            } catch (e: Exception) {
                Catalog.loadFailed = Catalog.all.isEmpty()
            } finally {
                Catalog.loading = false
                refreshing = false
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
    fun go(r: Route) { stack = stack + r }
    fun back() { stack = if (stack.size > 1) stack.dropLast(1) else listOf(Route.Home) }
    fun setTab(r: Route) { stack = listOf(r) }
    fun replaceTop(r: Route) { stack = stack.dropLast(1) + r }
    fun openSettings() { stack = listOf(Route.ProfileTab, Route.Sub("settings")) }

    fun toast(m: String) {
        toastMsg = m
        toastJob?.cancel()
        toastJob = viewModelScope.launch { delay(1800); toastMsg = null }
    }

    // ---------- state helpers ----------
    private fun upd(f: (AppState) -> AppState) { s = f(s); store.save(s) }
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
        viewModelScope.launch {
            val pb = try { ApiClient.fetchPlayback(id, ep, fresh = true) } catch (e: Exception) { null }
            val url = pb?.streamUrl
            if (pb == null || url.isNullOrBlank() || !url.contains(".m3u8")) {
                toast("This title can't be downloaded")
                return@launch
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
    }

    fun downloadAll(id: String) {
        val a = Catalog.byId[id] ?: return
        val nums = if (a.available.isNotEmpty()) a.available else (1..a.eps).toList()
        val todo = nums.filter { !hasDl(id, it) }
        if (todo.isEmpty()) { toast("All episodes already saved"); return }
        toast("Starting ${todo.size} downloads")
        for (n in todo) download(id, n)
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

    // ---------- welcome ----------
    fun openWelcome() { welcomeOpen = true }
    fun closeWelcome() { upd { it.copy(welcomeSeen = true) }; welcomeOpen = false; setTab(Route.Home) }
}
