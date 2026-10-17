package com.onimeku.app.ui.screens

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.onimeku.app.data.Anime
import com.onimeku.app.ui.AppViewModel
import com.onimeku.app.ui.components.*
import com.onimeku.app.ui.theme.*
import kotlinx.coroutines.delay

private const val SAMPLE = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4" // ganti dengan URL HLS/DASH
private fun fmt(ms: Long) = "%02d:%02d".format(ms / 60000, ms / 1000 % 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(vm: AppViewModel, anime: Anime, startEp: Int, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsState()
    val autoplay by rememberUpdatedState(settings.autoplay)
    var ep by remember { mutableIntStateOf(startEp) }
    var playing by remember { mutableStateOf(true) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(1L) }
    var visible by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(1f) }
    var sheet by remember { mutableStateOf("") }
    val resumePct = remember { vm.history.value[anime.id]?.takeIf { it.ep == startEp }?.pct ?: 0 }
    val player = remember { ExoPlayer.Builder(ctx).build().apply { setMediaItem(MediaItem.fromUri(SAMPLE)); prepare(); playWhenReady = true } }
    val next = { ep++; player.seekTo(0); player.play(); vm.toast("Memutar episode $ep") }

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED && autoplay) next() }
        }
        player.addListener(l); onDispose { player.removeListener(l) }
    }
    LaunchedEffect(Unit) {
        var resumed = resumePct == 0
        while (true) {
            delay(250)
            if (player.duration > 0) { dur = player.duration; if (!resumed) { player.seekTo(dur * resumePct / 100); resumed = true } }
            pos = player.currentPosition
        }
    }
    LaunchedEffect(visible, playing, sheet) { if (visible && playing && sheet.isEmpty()) { delay(3000); visible = false } }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) player.pause() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs); vm.saveProgress(anime.id, ep, (pos * 100 / dur).toInt()); player.release() }
    }

    when (sheet) {
        "menu" -> OptionSheet("Pengaturan Pemutar", listOf("Kecepatan: ${speed}x", "Kualitas: ${settings.quality}", "Subtitle: ${settings.subtitle}"), null,
            { sheet = when { it.startsWith("Kec") -> "speed"; it.startsWith("Kual") -> "quality"; else -> "sub" } }, { sheet = "" })
        "speed" -> OptionSheet("Kecepatan", listOf("0.5x", "1.0x", "1.25x", "1.5x", "2.0x"), "${speed}x".let { if (it == "1x") "1.0x" else if (it == "2x") "2.0x" else if (it == "0.5x") "0.5x" else it },
            { speed = it.dropLast(1).toFloat(); player.setPlaybackSpeed(speed); sheet = "" }, { sheet = "" })
        "quality" -> OptionSheet("Kualitas video", listOf("480p", "720p", "1080p"), settings.quality, { q -> vm.setSettings { it.copy(quality = q) }; sheet = "" }, { sheet = "" })
        "sub" -> OptionSheet("Bahasa subtitle", listOf("Indonesia", "English", "Mati"), settings.subtitle, { s -> vm.setSettings { it.copy(subtitle = s) }; sheet = "" }, { sheet = "" })
    }

    Column(Modifier.fillMaxSize().background(Color.Black), Arrangement.Center) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clickable(remember { MutableInteractionSource() }, null) { visible = !visible }) {
            AndroidView({ PlayerView(it).apply { this.player = player; useController = false; layoutParams = ViewGroup.LayoutParams(-1, -1) } }, Modifier.fillMaxSize())
            PlayerOverlay(
                visible, anime.title, ep, playing, pos, dur, speed, settings.quality,
                onBack, { sheet = "menu" },
                { if (playing) player.pause() else player.play() },
                { player.seekTo((pos - 10000).coerceAtLeast(0)) },
                { player.seekTo((pos + 10000).coerceAtMost(dur)) },
                { player.seekTo(it); pos = it },
                next
            )
        }
        Column(Modifier.padding(16.dp)) {
            Text("Episode $ep", style = MaterialTheme.typography.titleLarge)
            Text("Subtitle: ${settings.subtitle} · ${settings.quality}", color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
            if (settings.autoplay) Text("Putar otomatis episode berikut: aktif", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun PlayerOverlay(
    visible: Boolean, title: String, ep: Int, playing: Boolean,
    pos: Long, dur: Long, speed: Float, quality: String,
    onBack: () -> Unit, onOpenMenu: () -> Unit, onTogglePlay: () -> Unit,
    onSeekBack: () -> Unit, onSeekForward: () -> Unit,
    onSeek: (Long) -> Unit, onNext: () -> Unit
) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.65f), Color.Black.copy(.15f), Color.Black.copy(.75f)))).padding(10.dp), Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconCircle(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", onClick = onBack)
                Text("$title · Eps $ep", Modifier.weight(1f).padding(horizontal = 8.dp), maxLines = 1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                IconCircle(Icons.Rounded.Settings, "Pengaturan") { onOpenMenu() }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                IconCircle(Icons.Rounded.Replay10, "Mundur 10 detik", size = 48.dp) { onSeekBack() }
                Spacer(Modifier.width(28.dp))
                IconCircle(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Putar/Jeda", bg = Crimson, size = 60.dp) { onTogglePlay() }
                Spacer(Modifier.width(28.dp))
                IconCircle(Icons.Rounded.Forward10, "Maju 10 detik", size = 48.dp) { onSeekForward() }
            }
            Column {
                Slider(pos.toFloat().coerceIn(0f, dur.toFloat()), { onSeek(it.toLong()) }, valueRange = 0f..dur.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = Crimson, activeTrackColor = Crimson, inactiveTrackColor = Color.White.copy(.3f)))
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${fmt(pos)} / ${fmt(dur)}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Chip("Eps ${ep + 1} ›", false) { onNext() }
                    Text("  ${speed}x · $quality", fontSize = 12.sp)
                }
            }
        }
    }
}
