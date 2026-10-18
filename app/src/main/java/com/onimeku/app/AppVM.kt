package com.onimeku.app

import android.app.Application
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
}

class AppVM(private val app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    var s by mutableStateOf(store.load()); private set
    var stack by mutableStateOf<List<Route>>(listOf(Route.Home)); private set
    var toastMsg by mutableStateOf<String?>(null); private set
    var welcomeOpen by mutableStateOf(!s.welcomeSeen); private set
    var calDay by mutableIntStateOf(0)
    val downloading = mutableStateMapOf<String, Int>()
    var inPip by mutableStateOf(false)
    var playing by mutableStateOf(false)

    init {
        PosterHealer.init(app)
        Reminders.rescheduleAll(app, s)
        reloadCatalog()
    }

    /** Muat katalog dari API Onimeku; dipanggil saat aplikasi dibuka dan dari tombol coba lagi. */
    fun reloadCatalog() {
        viewModelScope.launch {
            Catalog.loading = true
            try {
                val items = ApiClient.fetchCatalog()
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

    fun download(id: String, ep: Int) {
        val k = "$id:$ep"
        if (hasDl(id, ep)) { toast("Already saved offline"); return }
        if (jobs[k]?.isActive == true) {
            jobs.remove(k)?.cancel(); downloading.remove(k); toast("Download cancelled"); return
        }
        downloading[k] = 0
        jobs[k] = viewModelScope.launch {
            var pr = 0.0
            while (pr < 100) {
                delay(150)
                pr += 8 + Random.nextDouble() * 10
                if (pr < 100) downloading[k] = pr.toInt()
            }
            downloading.remove(k); jobs.remove(k)
            if (!hasDl(id, ep)) upd { it.copy(downloads = it.downloads + Dl(id, ep)) }
            toast("Saved for offline watching")
        }
    }

    fun downloadAll(id: String) {
        val a = Catalog.byId[id] ?: return
        val nums = if (a.available.isNotEmpty()) a.available else (1..a.eps).toList()
        val add = nums.filter { !hasDl(id, it) }.map { Dl(id, it) }
        upd { it.copy(downloads = it.downloads + add) }
        toast(if (add.isNotEmpty()) "${add.size} episodes saved" else "All episodes already saved")
    }

    fun removeDl(id: String, ep: Int) {
        upd { st -> st.copy(downloads = st.downloads.filterNot { it.id == id && it.ep == ep }) }
        toast("Download removed")
    }

    fun removeAllDl() { upd { it.copy(downloads = emptyList()) }; toast("All downloads removed") }

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

    // ---------- welcome ----------
    fun openWelcome() { welcomeOpen = true }
    fun closeWelcome() { upd { it.copy(welcomeSeen = true) }; welcomeOpen = false; setTab(Route.Home) }
}
