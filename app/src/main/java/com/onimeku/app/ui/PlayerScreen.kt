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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
private fun VideoSurface(player: Player, modifier: Modifier, subScale: Float = 1f, subBg: Boolean = false) {
    AndroidView(
        factory = { c -> PlayerView(c).also { it.player = player; it.useController = false } },
        update = { pv ->
            pv.subtitleView?.let { sv ->
                sv.setApplyEmbeddedStyles(false)
                sv.setFractionalTextSize(androidx.media3.ui.SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subScale)
                sv.setStyle(androidx.media3.ui.CaptionStyleCompat(
                    android.graphics.Color.WHITE, if (subBg) 0xB3000000.toInt() else android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT,
                    androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE, android.graphics.Color.BLACK, null,
                ))
            }
        },
        modifier = modifier,
    )
}

private fun subScaleOf(size: String): Float = when (size) { "small" -> 0.8f; "large" -> 1.3f; "xl" -> 1.7f; else -> 1f }

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
    var speed by remember(r) { mutableFloatStateOf(vm.playerSpeed) }
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
    // Menu pengaturan ala YouTube (roda gigi): null = tertutup, "main" | "quality" | "subs" | "speed" | "audio".
    var sheet by remember { mutableStateOf<String?>(null) }
    var buffered by remember(r) { mutableFloatStateOf(0f) }
    var playingH by remember(r) { mutableIntStateOf(0) }
    var boost by remember { mutableStateOf(false) }          // tahan layar = 2x
    var dtSide by remember { mutableIntStateOf(0) }          // ketuk dua kali: -1 kiri, 1 kanan
    var dtSecs by remember { mutableIntStateOf(0) }
    var dtTick by remember { mutableIntStateOf(0) }
    val speedNow by rememberUpdatedState(speed)
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

    // Ketuk dua kali di sisi kiri/kanan: geser +-10 dtk; ketukan lanjutan menambah (10, 20, 30 ...).
    fun doubleSeek(side: Int) {
        if (locked) return
        if (dtSide != side) dtSecs = 0
        dtSide = side; dtSecs += 10; dtTick++
        player.seekTo((player.currentPosition + side * 10_000L).coerceIn(0L, if (player.duration > 0) player.duration else Long.MAX_VALUE))
    }
    LaunchedEffect(dtTick) { if (dtTick > 0) { delay(800); dtSide = 0; dtSecs = 0 } }

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
                if (size.height > 0) playingH = size.height
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
            val bufSec = (player.bufferedPosition / 1000L).toFloat()
            if (bufSec != buffered) buffered = bufSec
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
    LaunchedEffect(player) { if (speed != 1f) player.setPlaybackSpeed(speed) }
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
    BackHandler(enabled = sheet != null) { if (sheet == "main") sheet = null else sheet = "main" }
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

    val subScale = subScaleOf(vm.s.settings.subSize)
    val subBgOn = vm.s.settings.subBg
    if (vm.inPip) {
        Box(Modifier.fillMaxSize().background(Color.Black)) { VideoSurface(player, Modifier.fillMaxSize(), subScale, subBgOn) }
        return
    }

    val epList = if (a.available.isNotEmpty()) a.available else (1..a.eps).toList()
    val curIdx = epList.indexOf(r.ep)
    val epListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val thumbSrc = remember { MutableInteractionSource() }

    Box(Modifier.fillMaxSize()) {
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
                        onTap = { o ->
                            if (dtSide != 0 && !locked) doubleSeek(if (o.x < size.width / 2f) -1 else 1)
                            else { showUi = !showUi; uiTick++ }
                        },
                        onDoubleTap = { o -> doubleSeek(if (o.x < size.width / 2f) -1 else 1) },
                    )
                }
                .pointerInput(player) {
                    // Tahan layar = kecepatan 2x selama jari menempel (seperti YouTube).
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var held = true
                        withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation(); held = false }
                        if (held && !locked && player.isPlaying) {
                            boost = true; player.setPlaybackSpeed(2f)
                            waitForUpOrCancellation()
                            boost = false; player.setPlaybackSpeed(speedNow)
                        }
                    }
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
            VideoSurface(player, Modifier.fillMaxSize(), subScale, subBgOn)
            // Sampul anime menutupi layar hitam sampai bingkai video pertama tampil.
            if (!rendered && !failed) {
                RealImage(a, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color(0xA6080910)))
            }
            // Skip intro HANYA bila API memberi posisi intro episode ini (introStart/introEnd, detik).
            val introEnd = playback.introEnd
            if (vm.s.settings.skipIntro && introEnd != null && introEnd > 0 && pos >= (playback.introStart ?: 0) && pos < introEnd) {
                Text(I18n.t("skip_intro", "Skip intro"), Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 66.dp).clip(CircleShape)
                    .background(Color(0x99080910)).border(1.dp, Color(0xCCFFFFFF), CircleShape).clickable { player.seekTo(introEnd * 1000L) }
                    .padding(horizontal = 14.dp, vertical = 7.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            hud?.let {
                Text(it, Modifier.align(Alignment.Center).offset(y = (-58).dp).clip(RoundedCornerShape(14.dp)).background(Color(0xB3000000))
                    .padding(horizontal = 14.dp, vertical = 6.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            val remain = dur - pos.toInt()
            if (nextEp != null && dur > 60 && remain in 1..20 && !failed) {
                Row(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 66.dp).clip(RoundedCornerShape(14.dp))
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
            if (dtSide != 0) {
                val left = dtSide < 0
                Box(Modifier.align(if (left) Alignment.CenterStart else Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.38f)
                    .clip(if (left) RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50) else RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50))
                    .background(Color(0x4D000000)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(if (left) Icons.Filled.FastRewind else Icons.Filled.FastForward, null, Modifier.size(34.dp), tint = Color.White)
                        Text("$dtSecs ${I18n.t("seconds", "seconds")}", Modifier.padding(top = 4.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
            if (boost) {
                Row(Modifier.align(Alignment.TopCenter).padding(top = 14.dp).clip(CircleShape).background(Color(0x99000000)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("2×", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Icon(Icons.Filled.FastForward, null, Modifier.size(16.dp), tint = Color.White)
                }
            }
            val uiVisible = !locked && (showUi || !wantPlay || failed || buffering)
            val uiA by animateFloatAsState(if (uiVisible) 1f else 0f, tween(200), label = "controlsAlpha")
            if (uiA > 0.01f) Box(Modifier.matchParentSize().graphicsLayer { alpha = uiA }) {
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color(0xB3080910), 0.32f to Color(0x33080910), 0.6f to Color(0x33080910), 1f to Color(0xD9080910))))

                // ---- Bar atas: kembali · judul · CC · roda gigi ----
                Row(Modifier.align(Alignment.TopStart).graphicsLayer { translationY = -(1f - uiA) * 28.dp.toPx() }.fillMaxWidth().padding(start = 6.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).clickable { vm.back() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Column(Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(a.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text("Episode ${r.ep}", fontSize = 12.sp, color = Color(0xFFE6E8F0))
                    }
                    if (subTracks.isNotEmpty()) Box(Modifier.size(42.dp).clip(CircleShape).clickable {
                        // Tombol CC: hidupkan/matikan subtitle sekali ketuk (seperti YouTube).
                        if (subIdx >= 0) {
                            applySub(-1); vm.setSettings { it.copy(subLang = "off") }; vm.toast("Subtitles: ${I18n.t("ctl_off", "Off")}")
                        } else {
                            val pf = vm.s.settings.subLang.let { if (it == "off") "auto" else it }
                            val n = SubLang.pick(subTracks, pf, vm.s.settings.lang); applySub(n)
                            vm.setSettings { it.copy(subLang = pf) }; vm.toast("Subtitles: ${subName(n)}")
                        }
                        uiTick++
                    }, contentAlignment = Alignment.Center) {
                        Icon(if (subIdx >= 0) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionOff, "Subtitles", tint = Color.White)
                    }
                    Box(Modifier.size(42.dp).clip(CircleShape).clickable { sheet = "main"; uiTick++ }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Settings, "Settings", tint = Color.White)
                    }
                }

                // ---- Tengah: -10 · putar/jeda (ikon putih polos) · +10 ----
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
                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                        Box(Modifier.size(52.dp).clip(CircleShape).pressable { rotB -= 360f; seekBy(-10_000); uiTick++ }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Replay10, "Back 10 seconds", Modifier.size(34.dp).graphicsLayer { rotationZ = rb }, tint = Color.White)
                        }
                        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                            if (buffering) {
                                CircularProgressIndicator(Modifier.size(52.dp), color = Color.White, trackColor = Color(0x33FFFFFF), strokeWidth = 3.5.dp)
                            } else {
                                Box(Modifier.fillMaxSize().clip(CircleShape).pressable { player.playWhenReady = !player.playWhenReady; uiTick++ }, contentAlignment = Alignment.Center) {
                                    MorphIcon(wantPlay, Icons.Filled.Pause, Icons.Filled.PlayArrow, if (wantPlay) "Pause" else "Play", 56.dp, Color.White)
                                }
                            }
                        }
                        Box(Modifier.size(52.dp).clip(CircleShape).pressable { rotF += 360f; seekBy(10_000); uiTick++ }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Forward10, "Forward 10 seconds", Modifier.size(34.dp).graphicsLayer { rotationZ = rf }, tint = Color.White)
                        }
                    }
                }

                // ---- Bawah: waktu + layar penuh (kanan), bilah seek tipis dengan buffer ----
                Column(Modifier.align(Alignment.BottomCenter).graphicsLayer { translationY = (1f - uiA) * 28.dp.toPx() }.fillMaxWidth().padding(horizontal = if (full) 28.dp else 14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(fmt(pos.toInt()), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(" / ${fmt(dur)}", fontSize = 12.5.sp, color = Color(0xB3FFFFFF))
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.size(40.dp).clip(CircleShape).clickable { full = !full; uiTick++ }, contentAlignment = Alignment.Center) {
                            Icon(if (full) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, if (full) "Exit fullscreen" else "Fullscreen", tint = Color.White)
                        }
                    }
                    YtSeekBar(
                        pos = pos, dur = dur.toFloat(), buffered = buffered, modifier = Modifier.padding(bottom = 4.dp),
                        onPreview = { dragging = true; pos = it; uiTick++ },
                        onDone = { player.seekTo((it * 1000).toLong()); pos = it; dragging = false; uiTick++ },
                    )
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

                    Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x0FFFFFFF)).padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.TouchApp, null, Modifier.size(16.dp), tint = NavOff)
                        Text(I18n.t("swipe_hint", "Swipe left: brightness · right: volume · double-tap sides: ±10s · hold: 2× speed"), fontSize = 11.5.sp, color = InkSoft, lineHeight = 16.sp)
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
    PlayerSettingsSheet(
        page = sheet, full = full,
        quality = quality, autoRes = playingH,
        qualityOpts = listOf("Auto") + (if (heights.isNotEmpty()) heights.map { "${it}p" } else listOf("1080p", "720p", "480p")),
        onQuality = { q -> vm.setSettings { it.copy(quality = q) }; applyQuality(q); sheet = null; vm.toast("Quality: $q") },
        subTracks = subTracks, subCurrent = subIdx, subPref = vm.s.settings.subLang, subLabel = subName(subIdx),
        onSubTrack = { i ->
            applySub(i); sheet = null
            SubLang.code(subTracks[i])?.let { c -> vm.setSettings { it.copy(subLang = c) } }
            vm.toast("Subtitles: ${subName(i)}")
        },
        onSubOff = { applySub(-1); sheet = null; vm.setSettings { it.copy(subLang = "off") }; vm.toast("Subtitles: ${I18n.t("ctl_off", "Off")}") },
        onSubAuto = {
            sheet = null; vm.setSettings { it.copy(subLang = "auto") }
            val n = SubLang.pick(subTracks, "auto", vm.s.settings.lang); applySub(n); vm.toast("Subtitles: Auto · ${subName(n)}")
        },
        subSize = vm.s.settings.subSize, subBg = vm.s.settings.subBg,
        onSubSize = { v -> vm.setSettings { it.copy(subSize = v) } },
        onSubBg = { v -> vm.setSettings { it.copy(subBg = v) } },
        speed = speed, onSpeed = { v -> speed = v; vm.playerSpeed = v; player.setPlaybackSpeed(v); sheet = null; vm.toast("Speed ${spdLabel(v)}×") },
        audioLabels = audioLabels, audioIdx = audioIdx, onAudio = { n -> applyAudio(n); sheet = null; vm.toast("Audio: ${audioLabels.getOrElse(n) { "?" }}") },
        sleepMin = sleepMin, onSleep = { m -> sleepMin = m; sheet = null; vm.toast(if (m == 0) "Sleep timer: Off" else "Sleep timer: $m min") },
        onLock = { locked = true; showUi = true; sheet = null },
        onPip = {
            sheet = null
            try { act?.enterPictureInPictureMode(vm.pipParams ?: pipParams(ctx, false, wantPlay)) } catch (e: Exception) { vm.toast("Picture-in-Picture unavailable") }
        },
        onPage = { sheet = it },
    )
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

// ------------------------------------------------------------------ Bilah seek ala YouTube
/** Bilah seek tipis: lintasan, segmen buffer, progres merah, bulatan penunjuk yang membesar saat digeser. Ketuk = lompat. */
@Composable
private fun YtSeekBar(pos: Float, dur: Float, buffered: Float, modifier: Modifier = Modifier, onPreview: (Float) -> Unit, onDone: (Float) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    val trackH by androidx.compose.animation.core.animateDpAsState(if (dragging) 5.dp else 3.dp, tween(120), label = "seekH")
    val thumbR by androidx.compose.animation.core.animateDpAsState(if (dragging) 9.dp else 6.dp, tween(120), label = "seekThumb")
    val d = dur.coerceAtLeast(1f)
    val prev by rememberUpdatedState(onPreview)
    val fin by rememberUpdatedState(onDone)
    Box(
        modifier.fillMaxWidth().height(28.dp)
            .pointerInput(d) {
                detectTapGestures { o -> val v = (o.x / size.width).coerceIn(0f, 1f) * d; prev(v); fin(v) }
            }
            .pointerInput(d) {
                var last = 0f
                detectHorizontalDragGestures(
                    onDragStart = { o -> dragging = true; last = (o.x / size.width).coerceIn(0f, 1f) * d; prev(last) },
                    onDragEnd = { dragging = false; fin(last) },
                    onDragCancel = { dragging = false; fin(last) },
                ) { change, _ -> change.consume(); last = (change.position.x / size.width).coerceIn(0f, 1f) * d; prev(last) }
            },
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val h = trackH.toPx(); val cy = size.height / 2f; val r = h / 2f; val w = size.width
            val pf = (pos / d).coerceIn(0f, 1f)
            val bf = (buffered / d).coerceIn(pf, 1f)
            val cr = androidx.compose.ui.geometry.CornerRadius(r, r)
            drawRoundRect(Color(0x55FFFFFF), androidx.compose.ui.geometry.Offset(0f, cy - r), androidx.compose.ui.geometry.Size(w, h), cr)
            drawRoundRect(Color(0x88FFFFFF), androidx.compose.ui.geometry.Offset(0f, cy - r), androidx.compose.ui.geometry.Size(w * bf, h), cr)
            drawRoundRect(Red, androidx.compose.ui.geometry.Offset(0f, cy - r), androidx.compose.ui.geometry.Size(w * pf, h), cr)
            drawCircle(Red, thumbR.toPx(), androidx.compose.ui.geometry.Offset(w * pf, cy))
        }
    }
}

// ------------------------------------------------------------------ Menu pengaturan ala YouTube
/** Roda gigi di pemutar -> lembar bawah: Kualitas, Subtitle (+gaya), Kecepatan, Audio, Timer tidur, PiP, Kunci layar. Potret: naik dari bawah; layar penuh: panel kanan bawah. */
@Composable
private fun PlayerSettingsSheet(
    page: String?, full: Boolean,
    quality: String, autoRes: Int, qualityOpts: List<String>, onQuality: (String) -> Unit,
    subTracks: List<com.onimeku.app.data.ApiSubtitle>, subCurrent: Int, subPref: String, subLabel: String,
    onSubTrack: (Int) -> Unit, onSubOff: () -> Unit, onSubAuto: () -> Unit,
    subSize: String, subBg: Boolean, onSubSize: (String) -> Unit, onSubBg: (Boolean) -> Unit,
    speed: Float, onSpeed: (Float) -> Unit,
    audioLabels: List<String>, audioIdx: Int, onAudio: (Int) -> Unit,
    sleepMin: Int, onSleep: (Int) -> Unit, onLock: () -> Unit, onPip: () -> Unit,
    onPage: (String?) -> Unit,
) {
    var held by remember { mutableStateOf("main") }
    LaunchedEffect(page) { if (page != null) held = page }
    val open = page != null
    fun qLabel(q: String) = if (q == "Auto" && quality == "Auto" && autoRes > 0) "Auto (${autoRes}p)" else q
    val sizeNames = listOf("small" to "Small", "normal" to "Normal", "large" to "Large", "xl" to "Extra large")
    Box(Modifier.fillMaxSize()) {
        androidx.compose.animation.AnimatedVisibility(
            open, enter = androidx.compose.animation.fadeIn(tween(180)), exit = androidx.compose.animation.fadeOut(tween(160)),
        ) {
            Box(Modifier.fillMaxSize().background(Color(0x8C000000))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPage(null) })
        }
        androidx.compose.animation.AnimatedVisibility(
            open, Modifier.align(if (full) Alignment.BottomEnd else Alignment.BottomCenter),
            enter = androidx.compose.animation.slideInVertically(tween(260)) { it } + androidx.compose.animation.fadeIn(tween(160)),
            exit = androidx.compose.animation.slideOutVertically(tween(200)) { it } + androidx.compose.animation.fadeOut(tween(160)),
        ) {
            val shape = if (full) RoundedCornerShape(18.dp) else RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            Column(
                (if (full) Modifier.width(340.dp).padding(end = 16.dp, bottom = 12.dp) else Modifier.fillMaxWidth())
                    .heightIn(max = if (full) 300.dp else 520.dp)
                    .clip(shape).background(Bg2).border(1.dp, Line, shape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .then(if (full) Modifier else Modifier.navigationBarsPadding()),
            ) {
                if (!full) Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 4.dp).size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape).background(Color(0x55FFFFFF)))
                Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
                    when (held) {
                        "quality" -> {
                            SheetHeader(I18n.t("quality", "Quality")) { onPage("main") }
                            qualityOpts.forEach { q -> SheetOption(qLabel(q), q == quality) { onQuality(q) } }
                        }
                        "subs" -> {
                            SheetHeader(I18n.t("subtitles", "Subtitles")) { onPage("main") }
                            val order = subTracks.indices.sortedBy { i -> when (SubLang.code(subTracks[i])) { "id" -> 0; "en" -> 1; else -> 2 } }
                            SheetOption(I18n.t("ctl_off", "Off"), subPref == "off" || (subCurrent < 0 && subPref != "auto")) { onSubOff() }
                            val autoSub = if (subPref == "auto" && subCurrent >= 0) subTracks.getOrNull(subCurrent)?.let { " · " + SubLang.display(it, subCurrent) } else null
                            SheetOption("Auto" + (autoSub ?: ""), subPref == "auto") { onSubAuto() }
                            order.forEach { i -> SheetOption(SubLang.display(subTracks[i], i), subPref != "auto" && subCurrent == i) { onSubTrack(i) } }
                            Box(Modifier.padding(vertical = 6.dp).fillMaxWidth().height(1.dp).background(Line))
                            SheetRowItem(Icons.Filled.FormatSize, "Subtitle style", sizeNames.firstOrNull { it.first == subSize }?.second ?: "Normal") { onPage("substyle") }
                        }
                        "substyle" -> {
                            SheetHeader("Subtitle style") { onPage("subs") }
                            sizeNames.forEach { (k, label) -> SheetOption(label, subSize == k) { onSubSize(k) } }
                            Box(Modifier.padding(vertical = 6.dp).fillMaxWidth().height(1.dp).background(Line))
                            Row(Modifier.fillMaxWidth().clickable { onSubBg(!subBg) }.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Black background", Modifier.weight(1f), fontSize = 15.sp, color = Color.White)
                                Switch(subBg, { onSubBg(it) }, colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White, checkedTrackColor = Red, checkedBorderColor = Red,
                                    uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF3A3E50), uncheckedBorderColor = Color(0xFF3A3E50)))
                            }
                        }
                        "speed" -> {
                            SheetHeader(I18n.t("speed", "Playback speed")) { onPage("main") }
                            listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { v ->
                                SheetOption(if (v == 1f) I18n.t("normal", "Normal") else "${spdLabel(v)}×", v == speed) { onSpeed(v) }
                            }
                        }
                        "audio" -> {
                            SheetHeader("Audio") { onPage("main") }
                            audioLabels.forEachIndexed { i, l -> SheetOption(l, i == audioIdx) { onAudio(i) } }
                        }
                        "sleep" -> {
                            SheetHeader(I18n.t("ctl_sleep", "Sleep timer")) { onPage("main") }
                            listOf(0, 15, 30, 60).forEach { m -> SheetOption(if (m == 0) I18n.t("ctl_off", "Off") else "$m min", sleepMin == m) { onSleep(m) } }
                        }
                        else -> {
                            SheetRowItem(Icons.Filled.Tune, I18n.t("quality", "Quality"), qLabel(quality)) { onPage("quality") }
                            if (subTracks.isNotEmpty()) SheetRowItem(Icons.Filled.ClosedCaption, I18n.t("subtitles", "Subtitles"), subLabel) { onPage("subs") }
                            SheetRowItem(Icons.Filled.Speed, I18n.t("speed", "Playback speed"), if (speed == 1f) I18n.t("normal", "Normal") else "${spdLabel(speed)}×") { onPage("speed") }
                            if (audioLabels.size > 1) SheetRowItem(Icons.Filled.Audiotrack, "Audio", audioLabels.getOrElse(audioIdx) { "?" }) { onPage("audio") }
                            SheetRowItem(Icons.Filled.Bedtime, I18n.t("ctl_sleep", "Sleep timer"), if (sleepMin == 0) I18n.t("ctl_off", "Off") else "$sleepMin min") { onPage("sleep") }
                            SheetRowItem(Icons.Filled.PictureInPictureAlt, "Picture-in-picture", "") { onPip() }
                            SheetRowItem(Icons.Filled.Lock, I18n.t("ctl_lock", "Lock screen"), "") { onLock() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetRowItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(22.dp), tint = Color.White)
        Text(label, Modifier.weight(1f).padding(start = 16.dp), fontSize = 15.sp, color = Color.White)
        if (value.isNotEmpty()) {
            Text(value, fontSize = 13.5.sp, color = NavOff, maxLines = 1)
            Icon(Icons.Filled.ChevronRight, null, Modifier.padding(start = 4.dp).size(20.dp), tint = NavOff)
        }
    }
}

@Composable
private fun SheetHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onBack).padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(22.dp), tint = Color.White)
        Text(title, Modifier.padding(start = 14.dp), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}

@Composable
private fun SheetOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { if (selected) Icon(Icons.Filled.Check, null, Modifier.size(20.dp), tint = RedText) }
        Text(label, Modifier.padding(start = 16.dp), fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) RedText else Color.White)
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
