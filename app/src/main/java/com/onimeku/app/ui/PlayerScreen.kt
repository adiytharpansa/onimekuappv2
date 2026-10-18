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

private fun pipParams(auto: Boolean): PictureInPictureParams {
    val b = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9))
    if (Build.VERSION.SDK_INT >= 31) b.setAutoEnterEnabled(auto)
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
    fun subName(i: Int): String = if (i < 0) "Off" else subTracks.getOrNull(i)?.let { it.label.ifBlank { it.lang.uppercase() }.ifBlank { "Sub ${i + 1}" } } ?: "Off"

    fun applySub(i: Int) {
        // Perbaikan agen (berulang dari v5/v6): konstanta C dipakai sebagai
        // qualifier nama lengkap, tidak disimpan ke val (ilegal di Kotlin).
        val b = player.trackSelectionParameters.buildUpon()
        val groups = player.currentTracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT }
        if (i < 0 || groups.isEmpty()) {
            b.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
        } else {
            val g = groups.getOrNull(i) ?: groups.first()
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
                    fun has(i: Int, f: (String) -> Boolean) = subTracks.getOrNull(i)?.let { f((it.lang + " " + it.label).lowercase()) } == true
                    val pick = subTracks.indices.firstOrNull { has(it) { s -> s.contains("indo") || Regex("\\bid\\b").containsMatchIn(s) } }
                        ?: subTracks.indices.firstOrNull { has(it) { s -> s.contains("eng") || Regex("\\ben\\b").containsMatchIn(s) } }
                        ?: subTracks.indices.firstOrNull() ?: -1
                    applySub(pick)
                }
            }
            override fun onRenderedFirstFrame() { rendered = true }
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
    LaunchedEffect(wantPlay) { act?.setPictureInPictureParams(pipParams(wantPlay)) }
    DisposableEffect(Unit) {
        act?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            act?.setPictureInPictureParams(pipParams(false))
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

    if (vm.inPip) {
        Box(Modifier.fillMaxSize().background(Color.Black)) { VideoSurface(player, Modifier.fillMaxSize()) }
        return
    }

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
                        Text("Up next", fontSize = 11.sp, color = InkSoft)
                        Text("Episode $nextEp · ${remain}s", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Chip("Play now") { vm.replaceTop(Route.Player(r.id, nextEp)) }
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
                Row(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(start = 6.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
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
                                .clickable { val n = if (subIdx + 1 >= subTracks.size) -1 else subIdx + 1; applySub(n); uiTick++; vm.toast("Subtitles: ${subName(n)}") }
                                .padding(horizontal = 9.dp, vertical = 4.dp))
                    }
                    Box(Modifier.padding(start = 2.dp).size(42.dp).clip(CircleShape).clickable { locked = true; showUi = true; uiTick++ }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Lock, "Lock controls", Modifier.size(20.dp))
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
                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0x40000000)).pressable { seekBy(-10_000); uiTick++ }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Replay10, "Back 10 seconds", Modifier.size(26.dp))
                        }
                        Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                            if (buffering) {
                                CircularProgressIndicator(Modifier.size(52.dp), color = Red, trackColor = Color(0x33FFFFFF), strokeWidth = 3.5.dp)
                            } else {
                                Box(Modifier.fillMaxSize().clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFD01632), Red)))
                                    .pressable { player.playWhenReady = !player.playWhenReady; uiTick++ }, contentAlignment = Alignment.Center) {
                                    Icon(if (wantPlay) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (wantPlay) "Pause" else "Play", Modifier.size(36.dp))
                                }
                            }
                        }
                        Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0x40000000)).pressable { seekBy(10_000); uiTick++ }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Forward10, "Forward 10 seconds", Modifier.size(26.dp))
                        }
                    }
                }

                // ---- Bawah: bilah seek + waktu ----
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = if (full) 28.dp else 14.dp)) {
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
                    Text(listOfNotNull(a.year.takeIf { it > 0 }?.toString(), a.genres.take(2).joinToString(", ").ifBlank { null }, a.status).joinToString("  ·  "),
                        Modifier.padding(top = 4.dp), fontSize = 13.sp, color = InkSoft, maxLines = 1)

                    if (hasPrev || hasNext) Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (hasPrev) Btn("Previous", BtnStyle.Solid, Icons.Filled.SkipPrevious, Modifier.weight(1f), height = 48.dp) { step(-1) }
                        if (hasNext) Btn("Next episode", BtnStyle.Play, Icons.Filled.SkipNext, Modifier.weight(1f), height = 48.dp) { step(1) }
                    }

                    val tiles = buildList {
                        add(CtlTile(Icons.Filled.Speed, "Speed", "${spdLabel(speed)}×", speed != 1f) {
                            val all = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
                            speed = all[(all.indexOf(speed).let { if (it < 0) 2 else it } + 1) % all.size]
                            player.setPlaybackSpeed(speed)
                            vm.toast("Speed ${spdLabel(speed)}×")
                        })
                        add(CtlTile(Icons.Filled.ClosedCaption, I18n.t("subtitles", "Subtitles"), subName(subIdx), subIdx >= 0) {
                            if (subTracks.isEmpty()) vm.toast("No subtitles for this episode")
                            else { val n = if (subIdx + 1 >= subTracks.size) -1 else subIdx + 1; applySub(n); vm.toast("Subtitles: ${subName(n)}") }
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
                        add(CtlTile(Icons.Filled.Bedtime, "Sleep", if (sleepMin == 0) "Off" else "${sleepMin}m", sleepMin > 0) {
                            sleepMin = when (sleepMin) { 0 -> 15; 15 -> 30; 30 -> 60; else -> 0 }
                        })
                        add(CtlTile(Icons.Filled.PictureInPictureAlt, "PiP", "Mini") {
                            try { act?.enterPictureInPictureMode(pipParams(wantPlay)) } catch (e: Exception) { vm.toast("Picture-in-Picture unavailable") }
                        })
                        add(CtlTile(Icons.Filled.Lock, "Lock", "Screen") { locked = true; showUi = true })
                        add(CtlTile(Icons.Filled.Fullscreen, "Display", "Full") { full = true })
                    }
                    Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        tiles.chunked(4).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { t -> ControlTile(t, Modifier.weight(1f)) }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }

                    Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x0FFFFFFF)).padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.TouchApp, null, Modifier.size(16.dp), tint = NavOff)
                        Text("Swipe left: brightness · right: volume · double-tap sides: ±10s", fontSize = 11.5.sp, color = InkSoft, lineHeight = 16.sp)
                    }
                }
            }
            if (nextEp != null) item(key = "upnext") {
                val k = "${r.id}:$nextEp"
                Column(Modifier.padding(top = 24.dp)) {
                    SectionTitle("Up next")
                    Spacer(Modifier.height(12.dp))
                    EpisodeCard(a, nextEp, seen = k in vm.p.seen, progSec = vm.p.prog[k], current = false,
                        onOpen = { vm.replaceTop(Route.Player(r.id, nextEp)) }) {}
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
                            Text("Current", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = InkSoft)
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
                if (idx > 0) Btn("Previous", BtnStyle.Solid, Icons.Filled.SkipPrevious, Modifier.weight(1f), height = 48.dp) { vm.replaceTop(Route.Player(r.id, nums[idx - 1])) }
                if (idx >= 0 && idx < nums.size - 1) Btn("Next episode", BtnStyle.Play, Icons.Filled.SkipNext, Modifier.weight(1f), height = 48.dp) { vm.replaceTop(Route.Player(r.id, nums[idx + 1])) }
            }
            Text(
                "Judul ini diputar lewat pemutar resmi sumbernya. Ketuk tombol putar di dalam video.",
                Modifier.padding(top = 14.dp), fontSize = 12.sp, color = InkSoft,
            )
        }
    }
}
