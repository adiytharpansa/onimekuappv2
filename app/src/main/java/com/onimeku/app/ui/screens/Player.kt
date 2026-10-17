package com.onimeku.app.ui.screens

import android.content.Intent
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.onimeku.app.ui.components.Badge
import com.onimeku.app.ui.components.Chip
import com.onimeku.app.ui.components.GlassButton
import com.onimeku.app.ui.components.IconCircle
import com.onimeku.app.ui.components.OptionSheet
import com.onimeku.app.ui.components.QuickAction
import com.onimeku.app.ui.components.SectionCard
import com.onimeku.app.ui.theme.Crimson
import com.onimeku.app.ui.theme.Radius
import com.onimeku.app.ui.theme.Space
import com.onimeku.app.ui.theme.Surface1
import com.onimeku.app.ui.theme.TextSecondary
import com.onimeku.app.ui.theme.TextTertiary
import kotlinx.coroutines.delay

// Ganti dengan URL HLS/DASH dari API.
private const val SAMPLE =
    "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"

private fun fmt(ms: Long) = "%02d:%02d".format((ms / 60000).coerceAtLeast(0), (ms / 1000 % 60).coerceAtLeast(0))

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
    val status by vm.status.collectAsState()
    val resumePct = remember { vm.history.value[anime.id]?.takeIf { it.ep == startEp }?.pct ?: 0 }

    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(SAMPLE))
            prepare()
            playWhenReady = true
        }
    }

    val next = {
        ep++
        player.seekTo(0)
        player.play()
        vm.toast("Memutar episode $ep")
    }

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && autoplay) next()
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l) }
    }

    LaunchedEffect(Unit) {
        var resumed = resumePct == 0
        while (true) {
            delay(250)
            if (player.duration > 0) {
                dur = player.duration
                if (!resumed) {
                    player.seekTo(dur * resumePct / 100)
                    resumed = true
                }
            }
            pos = player.currentPosition
        }
    }

    LaunchedEffect(visible, playing, sheet) {
        if (visible && playing && sheet.isEmpty()) {
            delay(3500)
            visible = false
        }
    }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) player.pause() }
        owner.lifecycle.addObserver(obs)
        onDispose {
            owner.lifecycle.removeObserver(obs)
            vm.saveProgress(anime.id, ep, (pos * 100 / dur).toInt())
            player.release()
        }
    }

    when (sheet) {
        "menu" -> OptionSheet(
            "Pengaturan Pemutar",
            listOf("Kecepatan: ${speed}x", "Kualitas: ${settings.quality}", "Subtitle: ${settings.subtitle}"),
            null,
            { sheet = when { it.startsWith("Kec") -> "speed"; it.startsWith("Kual") -> "quality"; else -> "sub" } },
            { sheet = "" }
        )
        "speed" -> OptionSheet(
            "Kecepatan", listOf("0.5x", "1.0x", "1.25x", "1.5x", "2.0x"),
            "${speed}x".let {
                when (it) {
                    "1.0x" -> "1.0x"; "2.0x" -> "2.0x"; "1.25x" -> "1.25x"; "1.5x" -> "1.5x"; else -> "0.5x"
                }
            },
            {
                speed = it.dropLast(1).toFloat()
                player.setPlaybackSpeed(speed)
                sheet = ""
            },
            { sheet = "" }
        )
        "quality" -> OptionSheet(
            "Kualitas video", listOf("480p", "720p", "1080p"), settings.quality,
            { q -> vm.setSettings { it.copy(quality = q) }; sheet = "" },
            { sheet = "" }
        )
        "sub" -> OptionSheet(
            "Bahasa subtitle", listOf("Indonesia", "English", "Mati"), settings.subtitle,
            { s -> vm.setSettings { it.copy(subtitle = s) }; sheet = "" },
            { sheet = "" }
        )
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clickable(remember { MutableInteractionSource() }, null) { visible = !visible }
        ) {
            AndroidView(
                {
                    PlayerView(it).apply {
                        this.player = player
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(-1, -1)
                    }
                },
                Modifier.fillMaxSize()
            )
            PlayerOverlay(
                visible = visible,
                title = anime.title,
                ep = ep,
                playing = playing,
                pos = pos,
                dur = dur,
                speed = speed,
                quality = settings.quality,
                onBack = onBack,
                onOpenMenu = { sheet = "menu" },
                onTogglePlay = { if (playing) player.pause() else player.play() },
                onSeekBack = { player.seekTo((pos - 10000).coerceAtLeast(0)) },
                onSeekForward = { player.seekTo((pos + 10000).coerceAtMost(dur)) },
                onSeek = { player.seekTo(it); pos = it },
                onNext = next
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = Space.lg, vertical = Space.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge("Episode $ep", Crimson, Color.White)
                Spacer(Modifier.width(Space.xs))
                Badge("Sub ${settings.subtitle}", Surface1, TextSecondary)
                Spacer(Modifier.width(Space.xs))
                Badge(settings.quality, Surface1, TextSecondary)
            }
            Text(
                anime.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = Space.sm)
            )
            Text(
                "Episode $ep · 24 menit · ${anime.studio}",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Row(
                Modifier.fillMaxWidth().padding(top = Space.lg),
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                QuickAction(
                    if (anime.id in status) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    "My List", active = anime.id in status, modifier = Modifier.weight(1f)
                ) { vm.quickToggle(anime.id) }
                QuickAction(
                    Icons.Rounded.PlayArrow, "Kecepatan ${speed}x",
                    modifier = Modifier.weight(1f)
                ) { sheet = "speed" }
                QuickAction(
                    Icons.Rounded.Share, "Bagikan", modifier = Modifier.weight(1f)
                ) {
                    ctx.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Nonton ${anime.title} episode $ep di Onimeku!"
                                ),
                            "Bagikan"
                        )
                    )
                }
            }

            SectionCard("Pemutaran") {
                Row(
                    Modifier.fillMaxWidth().clickable { sheet = "quality" }
                        .padding(horizontal = Space.lg, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kualitas video", Modifier.weight(1f), fontSize = 14.sp)
                    Text(settings.quality, color = TextSecondary, fontSize = 13.sp)
                }
                Row(
                    Modifier.fillMaxWidth().clickable { sheet = "sub" }
                        .padding(horizontal = Space.lg, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Subtitle", Modifier.weight(1f), fontSize = 14.sp)
                    Text(settings.subtitle, color = TextSecondary, fontSize = 13.sp)
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Putar otomatis", fontSize = 14.sp)
                        Text(
                            "Episode berikutnya diputar setelah selesai",
                            color = TextTertiary, fontSize = 11.sp
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = settings.autoplay,
                        onCheckedChange = { v -> vm.setSettings { it.copy(autoplay = v) } },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedTrackColor = Crimson,
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = Surface1,
                            uncheckedThumbColor = TextTertiary
                        )
                    )
                }
            }

            GlassButton(
                "Episode berikutnya · Eps ${ep + 1}",
                Modifier.fillMaxWidth().padding(top = Space.lg),
                Icons.Rounded.SkipNext,
                onClick = next
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

@Composable
private fun PlayerOverlay(
    visible: Boolean,
    title: String,
    ep: Int,
    playing: Boolean,
    pos: Long,
    dur: Long,
    speed: Float,
    quality: String,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(.7f),
                        Color.Black.copy(.15f),
                        Color.Black.copy(.15f),
                        Color.Black.copy(.85f)
                    )
                )
            )
        ) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconCircle(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", onClick = onBack)
                Text(
                    "$title · Eps $ep",
                    Modifier.weight(1f).padding(horizontal = Space.sm),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconCircle(Icons.Rounded.Settings, "Pengaturan", onClick = onOpenMenu)
            }

            Row(
                Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(Space.xxl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconCircle(Icons.Rounded.Replay10, "Mundur 10 detik", size = 46.dp, onClick = onSeekBack)
                IconCircle(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    "Putar/Jeda", bg = Crimson, size = 62.dp, onClick = onTogglePlay
                )
                IconCircle(Icons.Rounded.Forward10, "Maju 10 detik", size = 46.dp, onClick = onSeekForward)
            }

            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                Slider(
                    pos.toFloat().coerceIn(0f, dur.toFloat()),
                    { onSeek(it.toLong()) },
                    valueRange = 0f..dur.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Crimson,
                        activeTrackColor = Crimson,
                        inactiveTrackColor = Color.White.copy(.25f)
                    )
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = Space.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${fmt(pos)} / ${fmt(dur)}", fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("${speed}x · $quality", fontSize = 11.sp, color = Color.White.copy(.75f))
                    Spacer(Modifier.width(Space.sm))
                    Chip("Eps ${ep + 1} ›", false) { onNext() }
                }
            }
        }
    }
}
