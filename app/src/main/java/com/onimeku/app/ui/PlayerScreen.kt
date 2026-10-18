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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import com.onimeku.app.Route
import com.onimeku.app.data.Catalog
import kotlinx.coroutines.delay
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
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
            SubHead("Wi-Fi only is on", vm)
            Text("Streaming over mobile data is turned off in Settings. Connect to Wi-Fi or turn the option off.", color = InkSoft)
            Box(Modifier.padding(top = 16.dp)) { Btn("Open Settings", BtnStyle.Solid) { vm.openSettings() } }
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
    var pbFailed by remember(r) { mutableStateOf(false) }
    LaunchedEffect(r, attempt) {
        pb = null; pbFailed = false
        val res = try { com.onimeku.app.data.ApiClient.fetchPlayback(r.id, r.ep) } catch (e: Exception) { null }
        if (res == null || (res.streamUrl.isNullOrBlank() && res.streamProxyUrl.isNullOrBlank())) pbFailed = true else pb = res
    }
    if (pbFailed) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
            SubHead(a.title, vm)
            Text("This episode's stream is unavailable right now.", color = InkSoft)
            Box(Modifier.padding(top = 16.dp)) { Btn("Try again", BtnStyle.Solid) { attempt++ } }
        }
        return
    }
    val playback = pb
    if (playback == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
            SubHead(a.title, vm)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(28.dp), color = Red, strokeWidth = 3.dp)
                Text("Loading stream…", color = InkSoft)
            }
        }
        return
    }
    val streamUrl = playback.streamUrl ?: playback.streamProxyUrl ?: return

    PlayerReady(vm, r, a, ctx, act, audio, quality, playback, streamUrl)

}

@Composable
private fun PlayerReady(vm: AppVM, r: Route.Player, a: com.onimeku.app.data.Anime, ctx: Context, act: android.app.Activity?, audio: AudioManager, quality: String, playback: com.onimeku.app.data.Playback, streamUrl: String) {
    val player = remember(r, streamUrl) {
        val httpFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        if (!playback.referer.isNullOrBlank()) {
            httpFactory.setDefaultRequestProperties(mapOf("Referer" to playback.referer))
        }
        val dsFactory = androidx.media3.datasource.DefaultDataSource.Factory(ctx, httpFactory)
        ExoPlayer.Builder(ctx)
            .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dsFactory))
            .build().apply {
            when (quality) {
                "1080p" -> trackSelectionParameters = trackSelectionParameters.buildUpon().setMaxVideoSize(1920, 1080).build()
                "720p" -> trackSelectionParameters = trackSelectionParameters.buildUpon().setMaxVideoSize(1280, 720).build()
                "480p" -> trackSelectionParameters = trackSelectionParameters.buildUpon().setMaxVideoSize(854, 480).build()
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
            setMediaItem(
                MediaItem.Builder().setUri(android.net.Uri.parse(streamUrl))
                    .setSubtitleConfigurations(subs).build()
            )
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
    var dragging by remember(r) { mutableStateOf(false) }
    var speed by remember(r) { mutableFloatStateOf(1f) }
    var br by remember { mutableFloatStateOf(1f) }
    var hud by remember { mutableStateOf<String?>(null) }
    var hudTick by remember { mutableIntStateOf(0) }
    val durNow by rememberUpdatedState(dur)

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) failed = false
                if (state == Player.STATE_ENDED) {
                    vm.saveProg(r.id, r.ep, durNow, durNow)
                    val nxt = a.available.firstOrNull { it > r.ep } ?: if (r.ep < a.eps) r.ep + 1 else null
                    if (vm.s.settings.autoplay && nxt != null) vm.replaceTop(Route.Player(r.id, nxt))
                }
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { wantPlay = playWhenReady; vm.playing = playWhenReady }
            override fun onPlayerError(error: PlaybackException) { failed = true; buffering = false }
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

    LaunchedEffect(player) {
        var n = 0
        while (true) {
            if (!dragging) pos = player.currentPosition / 1000f
            val d = player.duration
            if (d > 0) dur = (d / 1000).toInt()
            if (++n % 10 == 0 && wantPlay) vm.saveProg(r.id, r.ep, pos.toInt(), durNow)
            delay(500)
        }
    }

    // pause saat app ke background (kecuali sedang PiP)
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, player) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP && !vm.inPip) player.pause() }
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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(16.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)).background(Color.Black)
                .pointerInput(Unit) {
                    var left = true
                    detectVerticalDragGestures(onDragStart = { left = it.x < size.width / 2 }) { change, dy ->
                        change.consume()
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
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color(0x8C0A0B10), 0.3f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color(0xCC0A0B10))))

            Row(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).clickable { vm.back() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text(a.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("Episode ${r.ep}", fontSize = 12.sp, color = Color(0xFFE6E8F0))
                }
                Text(quality, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x33FFFFFF)).padding(horizontal = 8.dp, vertical = 2.dp))
            }

            if (failed) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Can’t play this video", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Chip("Retry") { failed = false; player.prepare(); player.playWhenReady = true }
                }
            } else if (buffering) {
                CircularProgressIndicator(Modifier.align(Alignment.Center).size(44.dp), color = Red, strokeWidth = 3.dp)
            } else {
                Box(Modifier.align(Alignment.Center).size(64.dp).clip(CircleShape).background(Color(0xEBE50914))
                    .clickable { player.playWhenReady = !player.playWhenReady }, contentAlignment = Alignment.Center) {
                    Icon(if (wantPlay) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (wantPlay) "Pause" else "Play", Modifier.size(32.dp))
                }
            }

            if (vm.s.settings.skipIntro && pos >= 3 && pos < 90) {
                Text("Skip intro", Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 52.dp).clip(CircleShape)
                    .background(Color(0x800A0B10)).border(1.dp, Color.White, CircleShape).clickable { player.seekTo(90_000L) }
                    .padding(horizontal = 14.dp, vertical = 7.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            hud?.let {
                Text(it, Modifier.align(Alignment.Center).offset(y = (-44).dp).clip(RoundedCornerShape(14.dp)).background(Color(0xB3000000))
                    .padding(horizontal = 14.dp, vertical = 6.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp)) {
                Slider(pos.coerceIn(0f, dur.toFloat()), { dragging = true; pos = it },
                    valueRange = 0f..dur.toFloat(), modifier = Modifier.height(24.dp),
                    onValueChangeFinished = { player.seekTo((pos * 1000).toLong()); dragging = false },
                    colors = SliderDefaults.colors(thumbColor = Red, activeTrackColor = Red, inactiveTrackColor = Color(0x55FFFFFF)))
                Text("${fmt(pos.toInt())} / ${fmt(dur)}", fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("−10s") { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) }
            Chip("${if (speed == 1.5f) "1.5" else speed.toInt()}×") {
                speed = when (speed) { 1f -> 1.5f; 1.5f -> 2f; else -> 1f }
                player.setPlaybackSpeed(speed)
                vm.toast("Speed ${if (speed == 1.5f) "1.5" else speed.toInt()}×")
            }
            Chip("+10s") { player.seekTo(player.currentPosition + 10_000) }
            if (hasPrev) Chip("Previous") { step(-1) }
            if (hasNext) Chip("Next") { step(1) }
        }
        Text("Swipe left side: brightness · right side: volume", Modifier.padding(top = 12.dp), fontSize = 11.sp, color = InkSoft)
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("PiP") {
                try { act?.enterPictureInPictureMode(pipParams(wantPlay)) } catch (e: Exception) { vm.toast("Picture-in-Picture unavailable") }
            }
            Chip("Cast") { vm.toast("Cast belum tersedia di versi ini") }
        }
    }
}
