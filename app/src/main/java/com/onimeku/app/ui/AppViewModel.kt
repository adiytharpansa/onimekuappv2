package com.onimeku.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onimeku.app.data.AnimeRepository
import com.onimeku.app.data.HomeData
import com.onimeku.app.data.ListStatus
import com.onimeku.app.data.Sort
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class Progress(val ep: Int, val pct: Int)
data class Dl(val p: Int = 0, val paused: Boolean = false)
data class Settings(
    val autoplay: Boolean = true,
    val wifi: Boolean = true,
    val push: Boolean = true,
    val quality: String = "1080p",
    val subtitle: String = "Indonesia"
)
data class Notif(val text: String, val read: Boolean = false)
data class Review(val user: String, val stars: Int, val text: String)
data class UserProfile(val name: String = "Tamu", val email: String = "")

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("onimeku", Context.MODE_PRIVATE)

    private val _home = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val home = _home.asStateFlow()
    val genre = MutableStateFlow("Semua")

    private val _status = MutableStateFlow<Map<Int, ListStatus>>(emptyMap())
    val status = _status.asStateFlow()
    private val _fav = MutableStateFlow<Set<Int>>(emptySet())
    val fav = _fav.asStateFlow()
    private val _history = MutableStateFlow<Map<Int, Progress>>(emptyMap())
    val history = _history.asStateFlow()
    private val _dl = MutableStateFlow<Map<Int, Dl>>(emptyMap())
    val downloads = _dl.asStateFlow()
    private val _remind = MutableStateFlow<Set<Int>>(emptySet())
    val remind = _remind.asStateFlow()
    private val _notif = MutableStateFlow<List<Notif>>(emptyList())
    val notif = _notif.asStateFlow()
    private val _settings = MutableStateFlow(Settings())
    val settings = _settings.asStateFlow()
    private val _reviews = MutableStateFlow<Map<Int, List<Review>>>(emptyMap())
    val reviews = _reviews.asStateFlow()
    private val _profile = MutableStateFlow(UserProfile())
    val profile = _profile.asStateFlow()

    val query = MutableStateFlow("")
    val searchGenre = MutableStateFlow("Semua")
    val sort = MutableStateFlow(Sort.Rating)
    private val _recent = MutableStateFlow<List<String>>(emptyList())
    val recent = _recent.asStateFlow()
    val results = combine(query.debounce(300), searchGenre, sort) { q, g, s -> Triple(q, g, s) }
        .mapLatest { (q, g, s) -> AnimeRepository.search(q, g, s) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnimeRepository.all)

    private val _msg = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val msg = _msg.asSharedFlow()
    fun toast(s: String) { _msg.tryEmit(s) }

    init {
        restore()
        load()
    }

    // ── Katalog ─────────────────────────────────────────────────────────────
    fun load() = viewModelScope.launch {
        _home.value = UiState.Loading
        try {
            AnimeRepository.refresh()
            val data = AnimeRepository.home()
            _home.value = UiState.Success(data)
            if (!AnimeRepository.isLive) {
                toast("Mode offline · menampilkan katalog contoh")
            }
        } catch (e: Exception) {
            _home.value = UiState.Error(e.message ?: "Tidak bisa memuat katalog")
        }
    }

    fun refreshCatalog() = viewModelScope.launch {
        try {
            AnimeRepository.refresh(force = true)
            _home.value = UiState.Success(AnimeRepository.home())
            toast(if (AnimeRepository.isLive) "Katalog diperbarui" else "Gagal memperbarui katalog")
        } catch (e: Exception) {
            toast(e.message ?: "Gagal memperbarui katalog")
        }
    }

    // ── My List / favorit / riwayat ────────────────────────────────────────
    fun quickToggle(id: Int) {
        if (id in _status.value) {
            _status.update { it - id }
            toast("Dihapus dari My List")
        } else {
            _status.update { it + (id to ListStatus.Plan) }
            toast("Ditambahkan ke My List")
        }
        persist()
    }

    fun setStatus(id: Int, s: ListStatus?) {
        _status.update { if (s == null) it - id else it + (id to s) }
        toast(if (s == null) "Dihapus dari My List" else "My List diperbarui")
        persist()
    }

    fun toggleFav(id: Int) {
        _fav.update { if (id in it) it - id else it + id }
        toast(if (id in _fav.value) "Ditambahkan ke favorit" else "Dihapus dari favorit")
        persist()
    }

    fun toggleRemind(id: Int) {
        val adding = id !in _remind.value
        _remind.update { if (adding) it + id else it - id }
        if (adding) {
            val title = AnimeRepository.byId(id).title
            _notif.update { listOf(Notif("Pengingat untuk $title diaktifkan")) + it }
        }
        toast(if (adding) "Pengingat diaktifkan" else "Pengingat dimatikan")
        persist()
    }

    fun saveProgress(id: Int, ep: Int, pct: Int) {
        _history.update { it + (id to Progress(ep, pct.coerceIn(1, 100))) }
        persist()
    }

    // ── Notifikasi ─────────────────────────────────────────────────────────
    fun markNotifRead() = _notif.update { l -> l.map { it.copy(read = true) } }

    fun setSettings(f: (Settings) -> Settings) {
        _settings.update(f)
        toast("Pengaturan disimpan")
        persist()
    }

    // ── Ulasan ─────────────────────────────────────────────────────────────
    fun addReview(id: Int, stars: Int) {
        _reviews.update {
            it + (id to (listOf(Review(_profile.value.name, stars, "Terima kasih atas nilainya!")) + (it[id] ?: emptyList())))
        }
        toast("Ulasan terkirim")
    }

    // ── Pencarian ──────────────────────────────────────────────────────────
    fun addRecent(q: String) {
        if (q.isNotBlank() && q !in _recent.value) {
            _recent.update { listOf(q) + it.take(5) }
            persist()
        }
    }

    fun removeRecent(q: String) {
        _recent.update { it - q }
        persist()
    }

    fun clearRecent() {
        _recent.value = emptyList()
        persist()
    }

    // ── Akun lokal ─────────────────────────────────────────────────────────
    fun signIn(name: String, email: String) {
        _profile.value = UserProfile(name.ifBlank { "Pengguna Onimeku" }, email.trim())
        persist()
    }

    fun signOut() {
        _profile.value = UserProfile()
        persist()
    }

    // ── Unduhan (simulasi progres; lihat README) ───────────────────────────
    fun download(id: Int) {
        if (id in _dl.value) {
            toast("Sudah ada di unduhan")
            return
        }
        _dl.update { it + (id to Dl()) }
        toast("Mulai mengunduh")
        viewModelScope.launch {
            while (true) {
                delay(250)
                val d = _dl.value[id] ?: break
                if (d.paused) continue
                val n = (d.p + 5).coerceAtMost(100)
                _dl.update { it + (id to d.copy(p = n)) }
                if (n >= 100) {
                    _notif.update {
                        listOf(Notif("${AnimeRepository.byId(id).title} selesai diunduh")) + it
                    }
                    break
                }
            }
        }
    }

    fun togglePause(id: Int) = _dl.update { m ->
        m[id]?.let { m + (id to it.copy(paused = !it.paused)) } ?: m
    }

    fun removeDownload(id: Int) {
        _dl.update { it - id }
        toast("Unduhan dihapus")
    }

    fun clearDownloads() {
        _dl.value = emptyMap()
        toast("Semua unduhan dihapus")
    }

    // ── Persistensi ────────────────────────────────────────────────────────
    private fun restore() {
        _status.value = prefs.getStringSet(K_STATUS, emptySet()).orEmpty().mapNotNull { s ->
            val id = s.substringBefore(':').toIntOrNull() ?: return@mapNotNull null
            val value = ListStatus.entries.firstOrNull { it.name == s.substringAfter(':', "") }
                ?: return@mapNotNull null
            id to value
        }.toMap()
        _fav.value = prefs.getStringSet(K_FAV, emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }.toSet()
        _history.value = prefs.getStringSet(K_HISTORY, emptySet()).orEmpty().mapNotNull { s ->
            val p = s.split(':')
            val id = p.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            id to Progress(p.getOrNull(1)?.toIntOrNull() ?: 1, p.getOrNull(2)?.toIntOrNull() ?: 1)
        }.toMap()
        _remind.value = prefs.getStringSet(K_REMIND, emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }.toSet()
        _settings.value = Settings(
            autoplay = prefs.getBoolean(K_AUTOPLAY, true),
            wifi = prefs.getBoolean(K_WIFI, true),
            push = prefs.getBoolean(K_PUSH, true),
            quality = prefs.getString(K_QUALITY, "1080p") ?: "1080p",
            subtitle = prefs.getString(K_SUBTITLE, "Indonesia") ?: "Indonesia"
        )
        _profile.value = UserProfile(
            name = prefs.getString(K_NAME, "Tamu") ?: "Tamu",
            email = prefs.getString(K_EMAIL, "") ?: ""
        )
        _recent.value = prefs.getString(K_RECENT, null)
            ?.split(SEP)?.filter { it.isNotBlank() } ?: emptyList()
    }

    private fun persist() {
        prefs.edit()
            .putStringSet(K_STATUS, _status.value.map { "${it.key}:${it.value.name}" }.toSet())
            .putStringSet(K_FAV, _fav.value.map { it.toString() }.toSet())
            .putStringSet(
                K_HISTORY,
                _history.value.map { "${it.key}:${it.value.ep}:${it.value.pct}" }.toSet()
            )
            .putStringSet(K_REMIND, _remind.value.map { it.toString() }.toSet())
            .putBoolean(K_AUTOPLAY, _settings.value.autoplay)
            .putBoolean(K_WIFI, _settings.value.wifi)
            .putBoolean(K_PUSH, _settings.value.push)
            .putString(K_QUALITY, _settings.value.quality)
            .putString(K_SUBTITLE, _settings.value.subtitle)
            .putString(K_NAME, _profile.value.name)
            .putString(K_EMAIL, _profile.value.email)
            .putString(K_RECENT, _recent.value.joinToString(SEP))
            .apply()
    }

    private companion object {
        const val SEP = "\u001F"
        const val K_STATUS = "status"
        const val K_FAV = "fav"
        const val K_HISTORY = "history"
        const val K_REMIND = "remind"
        const val K_AUTOPLAY = "autoplay"
        const val K_WIFI = "wifi"
        const val K_PUSH = "push"
        const val K_QUALITY = "quality"
        const val K_SUBTITLE = "subtitle"
        const val K_NAME = "name"
        const val K_EMAIL = "email"
        const val K_RECENT = "recent"
    }
}
