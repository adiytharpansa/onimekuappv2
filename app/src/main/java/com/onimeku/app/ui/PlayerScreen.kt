package com.onimeku.app.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.util.Rational
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.onimeku.app.data.*
import com.onimeku.app.media.MediaKit
import com.onimeku.app.data.Repo
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.shape.RoundedCornerShape
import com.onimeku.app.media.PlaybackService
import kotlinx.coroutines.*

object PlayerState { var active by mutableStateOf(false); var inPip by mutableStateOf(false) }

@UnstableApi @Composable
fun PlayerScreen(animeId: String, title: String, ep: Int, total: Int, onEpisode: (Int) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current; val act = ctx as Activity
    val settings by SettingsStore.of(ctx).flow.collectAsState(Settings())
    val cfg by rememberUpdatedState(settings); val epNow by rememberUpdatedState(ep)
    val dao = remember { AppDb.get(ctx).progress() }
    val scope = rememberCoroutineScope()
    var player by remember { mutableStateOf<MediaController?>(null) }
    var showSkip by remember { mutableStateOf(false) }
    var blocked by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }; var liveOn by remember { mutableStateOf(true) }
    var comments by remember { mutableStateOf<List<CommentDto>>(emptyList()) }; var pos by remember { mutableLongStateOf(0L) }
    var cmsg by remember { mutableStateOf<String?>(null) }; var lastPost by remember { mutableLongStateOf(0L) }
    val blocked = settings.blocked; val store = remember { SettingsStore.of(ctx) }
    val key = "$animeId:$ep"

    fun save(p: Player?, k: String) { p ?: return; val d = p.duration; if (d <= 0) return
        val pos = p.currentPosition; scope.launch(Dispatchers.IO) { dao.upsert(Progress(k, pos, d, System.currentTimeMillis())) } }

    // Landscape + fullscreen selama pemutar tampil
    DisposableEffect(Unit) {
        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val c = WindowCompat.getInsetsController(act.window, act.window.decorView)
        c.hide(WindowInsetsCompat.Type.systemBars()); c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        PlayerState.active = true
        onDispose { c.show(WindowInsetsCompat.Type.systemBars()); act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; PlayerState.active = false }
    }
    // Sambung ke MediaSession
    DisposableEffect(Unit) {
        val f = MediaController.Builder(ctx, SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))).buildAsync()
        f.addListener({ player = f.get() }, ContextCompat.getMainExecutor(ctx))
        onDispose { player?.let { save(it, "$animeId:$epNow"); it.stop() }; MediaController.releaseFuture(f); player = null }
    }
    // Muat episode: pakai URI unduhan bila ada (offline), kalau tidak minta URL stream ke API
    LaunchedEffect(player, ep) {
        val p = player ?: return@LaunchedEffect
        failed = false
        val local = withContext(Dispatchers.IO) { MediaKit.downloadedUri(ctx, key) }
        blocked = cfg.wifiOnly && MediaKit.isMetered(ctx) && local == null
        if (blocked) return@LaunchedEffect
        val url = local?.toString() ?: runCatching { Repo.of(ctx).stream(animeId, ep) }.getOrNull()
        if (url == null) { failed = true; return@LaunchedEffect }
        val saved = withContext(Dispatchers.IO) { dao.get(key) }
        val start = saved?.takeIf { it.positionMs < it.durationMs * 0.95 }?.positionMs ?: 0L
        p.setMediaItem(MediaItem.Builder().setMediaId(key).setUri(url)
            .setMediaMetadata(MediaMetadata.Builder().setTitle("$title · Episode $ep").build()).build(), start)
        p.prepare(); p.playWhenReady = true
    }
    LaunchedEffect(ep) { comments = runCatching { Repo.of(ctx).comments(animeId, ep) }.getOrDefault(emptyList()) }
    // Kualitas video dari Settings
    LaunchedEffect(player, settings.quality) {
        val p = player ?: return@LaunchedEffect
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().apply {
            when (settings.quality) { "1080p" -> setMaxVideoSize(1920, 1080); "720p" -> setMaxVideoSize(1280, 720)
                "480p" -> setMaxVideoSize(854, 480); else -> clearVideoSizeConstraints() } }.build()
    }
    // Simpan progres tiap 5 dtk + tombol Skip Intro
    LaunchedEffect(player) { while (true) { delay(5000); save(player, "$animeId:$epNow") } }
    LaunchedEffect(player) { while (true) { val cp = player?.currentPosition ?: 0L; pos = cp; showSkip = cfg.skipIntro && cp in 3_000L..89_000L; delay(500) } }
    // Autoplay episode berikutnya
    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onPlaybackStateChanged(s: Int) {
                if (s == Player.STATE_ENDED) {
                    val d = player?.duration ?: 0L; val k = "$animeId:$epNow"
                    scope.launch(Dispatchers.IO) { dao.upsert(Progress(k, d, d, System.currentTimeMillis())) }
                    if (cfg.autoplay && epNow < total) onEpisode(epNow + 1)
                } } }
        player?.addListener(l); onDispose { player?.removeListener(l) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { c ->
            PlayerView(c).apply { layoutParams = ViewGroup.LayoutParams(-1, -1); setShowSubtitleButton(true) } // ikon gear bawaan: audio & subtitle
        }, update = { it.player = player })
        if (blocked) Text("Stream over Wi-Fi only aktif. Sambungkan Wi-Fi atau unduh episode ini.", Modifier.align(Alignment.Center).padding(24.dp), color = Color.White)
        if (failed) Text("Gagal memuat video. Periksa koneksi lalu coba lagi.", Modifier.align(Alignment.Center).padding(24.dp), color = Color.White)
        if (!PlayerState.inPip) {
            TextButton(onBack, Modifier.align(Alignment.TopStart).statusBarsPadding()) { Text("‹ Back", color = Color.White) }
            if (showSkip) Button({ player?.seekTo(90_000L) }, Modifier.align(Alignment.BottomEnd).padding(24.dp, 72.dp)) { Text("Skip intro") }
            Row(Modifier.align(Alignment.TopEnd).statusBarsPadding()) {
                TextButton({ showComments = !showComments }) { Text("Komentar (${comments.count { it.userId !in blocked }})", color = Color.White) }
                TextButton({ liveOn = !liveOn }) { Text(if (liveOn) "Live ✓" else "Live ✗", color = Color.White) }
                TextButton({ act.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()) }) { Text("PiP", color = Color.White) }
            }
            // Komentar "live": muncul sesuai menit tontonan
            if (liveOn && !showComments) Column(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 72.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                comments.filter { it.userId !in blocked && pos - it.timeMs in 0L..6000L }.takeLast(3).forEach {
                    Surface(color = Color.Black.copy(alpha = .55f), shape = RoundedCornerShape(12.dp)) { Text(it.text, Modifier.padding(10.dp, 4.dp), color = Color.White, maxLines = 1) } } }
            if (showComments) CommentsPanel(Modifier.align(Alignment.CenterEnd), comments.filter { it.userId !in blocked },
                canPost = FirebaseApp.getApps(ctx).isNotEmpty() && FirebaseAuth.getInstance().currentUser != null, message = cmsg, posMs = pos,
                onClose = { showComments = false }, onSeek = { player?.seekTo(it) },
                onPost = { t -> if (System.currentTimeMillis() - lastPost < 3000) cmsg = "Tunggu beberapa detik sebelum mengirim lagi." else { lastPost = System.currentTimeMillis()
                    scope.launch { runCatching { Repo.of(ctx).postComment(animeId, epNow, t, player?.currentPosition ?: 0L) }
                        .onSuccess { comments = (comments + it).sortedBy { c -> c.timeMs }; cmsg = null }.onFailure { cmsg = "Gagal mengirim komentar." } } } },
                onDelete = { c -> scope.launch { if (Repo.of(ctx).deleteComment(c.id)) comments = comments - c else cmsg = "Gagal menghapus." } },
                onReport = { c -> scope.launch { cmsg = if (Repo.of(ctx).reportComment(c.id)) "Laporan terkirim. Terima kasih." else "Gagal melapor." } },
                onBlock = { c -> scope.launch { store.block(c.userId); cmsg = "${c.user} diblokir." } })
        }
    }
}
