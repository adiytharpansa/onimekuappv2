package com.onimeku.app.ui

import androidx.lifecycle.ViewModel
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

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AppViewModel : ViewModel() {
    private val _home = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val home = _home.asStateFlow()
    val genre = MutableStateFlow("Semua")

    private val _status = MutableStateFlow<Map<Int, ListStatus>>(
        mapOf(3 to ListStatus.Watching, 9 to ListStatus.Plan)
    )
    val status = _status.asStateFlow()
    private val _fav = MutableStateFlow(setOf(1))
    val fav = _fav.asStateFlow()
    private val _history = MutableStateFlow(mapOf(1 to Progress(7, 42), 3 to Progress(3, 68)))
    val history = _history.asStateFlow()
    private val _dl = MutableStateFlow<Map<Int, Dl>>(emptyMap())
    val downloads = _dl.asStateFlow()
    private val _remind = MutableStateFlow<Set<Int>>(emptySet())
    val remind = _remind.asStateFlow()
    private val _notif = MutableStateFlow(
        listOf(
            Notif("Demon Slayer episode 12 sudah tayang"),
            Notif("Frieren masuk Top Hits minggu ini", true),
            Notif("Attack on Titan: episode baru tersedia", true)
        )
    )
    val notif = _notif.asStateFlow()
    private val _settings = MutableStateFlow(Settings())
    val settings = _settings.asStateFlow()

    // Ulasan contoh tersebar di semua judul agar tab Ulasan tidak terasa kosong.
    private val names = listOf("Rizky", "Sari", "Dewi", "Bagas", "Nadia", "Fajar", "Alya", "Rian")
    private val texts = listOf(
        "Animasinya luar biasa, wajib tonton!",
        "Alur seru, OST-nya keren.",
        "Pacing rapi dan karakter mudah disukai.",
        "Fighting-nya memuaskan, tunggu season berikutnya.",
        "Cerita ringan, cocok untuk maraton akhir pekan."
    )
    private val _reviews = MutableStateFlow(
        AnimeRepository.all.associate { a ->
            a.id to List(2) { i ->
                Review(
                    names[(a.id * 2 + i) % names.size],
                    5 - (a.id + i) % 2,
                    texts[(a.id + i * 3) % texts.size]
                )
            }
        } + (0 to listOf(Review("Rizky", 5, "Animasinya luar biasa, wajib tonton!"), Review("Sari", 4, "Alur seru, OST keren.")))
    )
    val reviews = _reviews.asStateFlow()

    val query = MutableStateFlow("")
    val searchGenre = MutableStateFlow("Semua")
    val sort = MutableStateFlow(Sort.Rating)
    private val _recent = MutableStateFlow(listOf("Jujutsu", "Action", "Frieren"))
    val recent = _recent.asStateFlow()
    val results = combine(query.debounce(250), searchGenre, sort) { q, g, s -> Triple(q, g, s) }
        .mapLatest { (q, g, s) -> AnimeRepository.search(q, g, s) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnimeRepository.all)

    private val _msg = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val msg = _msg.asSharedFlow()
    fun toast(s: String) { _msg.tryEmit(s) }

    init { load() }

    fun load() = viewModelScope.launch {
        _home.value = UiState.Loading
        _home.value = try {
            UiState.Success(AnimeRepository.home())
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Terjadi kesalahan")
        }
    }

    fun quickToggle(id: Int) {
        if (id in _status.value) {
            _status.update { it - id }
            toast("Dihapus dari My List")
        } else {
            _status.update { it + (id to ListStatus.Plan) }
            toast("Ditambahkan ke My List")
        }
    }

    fun setStatus(id: Int, s: ListStatus?) {
        _status.update { if (s == null) it - id else it + (id to s) }
        toast(if (s == null) "Dihapus dari My List" else "My List diperbarui")
    }

    fun toggleFav(id: Int) {
        _fav.update { if (id in it) it - id else it + id }
        toast(if (id in _fav.value) "Ditambahkan ke favorit" else "Dihapus dari favorit")
    }

    fun toggleRemind(id: Int) {
        _remind.update { if (id in it) it - id else it + id }
        toast(if (id in _remind.value) "Pengingat diaktifkan" else "Pengingat dimatikan")
    }

    fun saveProgress(id: Int, ep: Int, pct: Int) =
        _history.update { it + (id to Progress(ep, pct.coerceIn(1, 100))) }

    fun markNotifRead() = _notif.update { l -> l.map { it.copy(read = true) } }

    fun setSettings(f: (Settings) -> Settings) {
        _settings.update(f)
        toast("Pengaturan disimpan")
    }

    fun addReview(id: Int, stars: Int) {
        _reviews.update {
            it + (id to (listOf(Review("Kamu", stars, "Terima kasih atas nilainya!")) + (it[id] ?: emptyList())))
        }
        toast("Ulasan terkirim")
    }

    fun addRecent(q: String) {
        if (q.isNotBlank() && q !in _recent.value) _recent.update { listOf(q) + it.take(5) }
    }

    fun removeRecent(q: String) = _recent.update { it - q }

    fun clearRecent() { _recent.value = emptyList() }

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
}
