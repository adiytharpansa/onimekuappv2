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
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset

/**
 * Seek bar gaya YouTube: bilah buffer, penanda intro/outro (kuning), tooltip waktu saat digeser, dan animasi linear
 * 1 dtk yang mengisi jeda polling posisi (bilah mengalir halus tanpa recompose tambahan). Dapat dipakai TalkBack.
 */
@Composable
internal fun YtSeekBar(
    pos: Float, dur: Float, buffered: Float, modifier: Modifier = Modifier,
    marks: List<Pair<Float, Float>> = emptyList(),
    onPreview: (Float) -> Unit, onDone: (Float) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    val trackH by androidx.compose.animation.core.animateDpAsState(if (dragging) 5.dp else 3.dp, tween(120), label = "seekH")
    val thumbR by androidx.compose.animation.core.animateDpAsState(if (dragging) 9.dp else 6.dp, tween(120), label = "seekThumb")
    val d = dur.coerceAtLeast(1f)
    val prev by rememberUpdatedState(onPreview)
    val fin by rememberUpdatedState(onDone)
    var widthPx by remember { mutableFloatStateOf(0f) }
    // Loncatan >1,6 dtk (seek / kecepatan tinggi) langsung tanpa animasi; selain itu animasi linear 1 dtk.
    var lastPos by remember { mutableFloatStateOf(pos) }
    val jump = kotlin.math.abs(pos - lastPos) > 1.6f
    SideEffect { lastPos = pos }
    val shown by animateFloatAsState(
        pos, if (dragging || jump) snap<Float>() else tween<Float>(1000, easing = LinearEasing), label = "seekPos",
    )
    Box(
        modifier.fillMaxWidth().height(28.dp)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .semantics {
                contentDescription = "Seek bar"
                stateDescription = "${fmt(pos.toInt())} / ${fmt(d.toInt())}"
                progressBarRangeInfo = ProgressBarRangeInfo(pos.coerceIn(0f, d), 0f..d)
                setProgress { v -> fin(v.coerceIn(0f, d)); true }
            }
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
            val pf = (shown / d).coerceIn(0f, 1f)
            val bf = (buffered / d).coerceIn(pf, 1f)
            val cr = androidx.compose.ui.geometry.CornerRadius(r, r)
            drawRoundRect(Color(0x55FFFFFF), androidx.compose.ui.geometry.Offset(0f, cy - r), androidx.compose.ui.geometry.Size(w, h), cr)
            drawRoundRect(Color(0x88FFFFFF), androidx.compose.ui.geometry.Offset(0f, cy - r), androidx.compose.ui.geometry.Size(w * bf, h), cr)
            marks.forEach { (m0, m1) ->
                val x0 = (m0 / d).coerceIn(0f, 1f) * w; val x1 = (m1 / d).coerceIn(0f, 1f) * w
                if (x1 > x0) drawRoundRect(Color(0xFFFFC857), androidx.compose.ui.geometry.Offset(x0, cy - r), androidx.compose.ui.geometry.Size(x1 - x0, h), cr)
            }
            drawRoundRect(Red, androidx.compose.ui.geometry.Offset(0f, cy - r), androidx.compose.ui.geometry.Size(w * pf, h), cr)
            drawCircle(Red, thumbR.toPx(), androidx.compose.ui.geometry.Offset(w * pf, cy))
        }
        if (dragging) {
            Text(
                fmt(pos.toInt()),
                Modifier.offset {
                    val tw = 48.dp.roundToPx()
                    IntOffset((widthPx * (pos / d).coerceIn(0f, 1f) - tw / 2f).toInt().coerceIn(0, (widthPx - tw).toInt().coerceAtLeast(0)), -36.dp.roundToPx())
                }.clip(RoundedCornerShape(8.dp)).background(Color(0xCC000000)).padding(horizontal = Sp.S8, vertical = Sp.S4),
                fontSize = Ty.Caption, fontWeight = FontWeight.SemiBold, color = Color.White,
            )
        }
    }
}

/** Roda gigi di pemutar -> lembar bawah: Kualitas, Subtitle (+gaya), Kecepatan, Audio, Timer tidur, PiP, Kunci layar. Potret: naik dari bawah; layar penuh: panel kanan bawah. */
@Composable
internal fun PlayerSettingsSheet(
    page: String?, full: Boolean,
    quality: String, autoRes: Int, qualityOpts: List<String>, onQuality: (String) -> Unit,
    subTracks: List<com.onimeku.app.data.ApiSubtitle>, subCurrent: Int, subPref: String, subLabel: String,
    onSubTrack: (Int) -> Unit, onSubOff: () -> Unit, onSubAuto: () -> Unit,
    subSize: String, subBg: Boolean, onSubSize: (String) -> Unit, onSubBg: (Boolean) -> Unit,
    speed: Float, onSpeed: (Float) -> Unit,
    audioLabels: List<String>, audioIdx: Int, onAudio: (Int) -> Unit, audioKeys: List<String> = emptyList(),
    sleepMin: Int, onSleep: (Int) -> Unit, onLock: () -> Unit, onPip: () -> Unit,
    audioOnly: Boolean, onAudioOnly: () -> Unit,
    onPage: (String?) -> Unit,
    onReport: () -> Unit = {},
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
                (if (full) Modifier.width(340.dp).padding(end = Sp.S16, bottom = Sp.S12) else Modifier.widthIn(max = 560.dp).fillMaxWidth())
                    .heightIn(max = if (full) 300.dp else 520.dp)
                    .clip(shape).background(Bg2).border(1.dp, Line, shape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .then(if (full) Modifier else Modifier.navigationBarsPadding()),
            ) {
                if (!full) Box(
                    Modifier.fillMaxWidth().height(28.dp).pointerInput(Unit) {
                        var acc = 0f
                        detectVerticalDragGestures(onDragStart = { acc = 0f }, onDragEnd = { if (acc > 40f) onPage(null) }) { _, dy -> acc += dy }
                    },
                    contentAlignment = Alignment.Center,
                ) { Box(Modifier.size(width = 36.dp, height = 4.dp).clip(CircleShape).background(Color(0x55FFFFFF))) }
                Column(Modifier.verticalScroll(rememberScrollState()).padding(top = Sp.S8, bottom = if (full) Sp.S8 else Sp.S16)) {
                    when (held) {
                        "quality" -> {
                            SheetHeader(I18n.t("quality", "Quality")) { onPage("main") }
                            qualityOpts.forEach { q -> SheetOption(qLabel(q), q == quality) { onQuality(q) } }
                        }
                        "subs" -> {
                            SheetHeader(I18n.t("subtitles", "Subtitles")) { onPage("main") }
                            val autoSub = if (subPref == "auto" && subCurrent >= 0) subTracks.getOrNull(subCurrent)?.let { " · " + SubLang.display(it, subCurrent) } else null
                            SubLang.menu(subTracks).forEach { e ->
                                when (e) {
                                    SubMenuEntry.Auto -> SheetOption("Auto" + (autoSub ?: ""), subPref == "auto") { onSubAuto() }
                                    is SubMenuEntry.Track -> SheetOption(SubLang.display(subTracks[e.index], e.index), subPref != "auto" && subCurrent == e.index) { onSubTrack(e.index) }
                                    SubMenuEntry.Off -> SheetOption(I18n.t("ctl_off", "Off"), subPref == "off" || (subCurrent < 0 && subPref != "auto")) { onSubOff() }
                                }
                            }
                            Box(Modifier.padding(vertical = Sp.S8).fillMaxWidth().height(1.dp).background(Line))
                            SheetRowItem(Icons.Filled.FormatSize, "Subtitle style", sizeNames.firstOrNull { it.first == subSize }?.second ?: "Normal") { onPage("substyle") }
                        }
                        "substyle" -> {
                            SheetHeader("Subtitle style") { onPage("subs") }
                            sizeNames.forEach { (k, label) -> SheetOption(label, subSize == k) { onSubSize(k) } }
                            Box(Modifier.padding(vertical = Sp.S8).fillMaxWidth().height(1.dp).background(Line))
                            Row(Modifier.fillMaxWidth().clickable { onSubBg(!subBg) }.padding(horizontal = Sp.S20, vertical = Sp.S8), verticalAlignment = Alignment.CenterVertically) {
                                Text("Black background", Modifier.weight(1f), fontSize = Ty.Subtitle, color = Color.White)
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
                            SheetHeader(I18n.t("dubbing", "Dubbing")) { onPage("main") }
                            // Urutan tampil: English pertama, Indonesia kedua, sisanya (mis. Japanese) mengikuti urutan trek asli.
                            audioLabels.indices.sortedBy { i -> when (audioKeys.getOrNull(i)) { "en" -> 0; "id" -> 1; else -> 2 } }
                                .forEach { i -> DubOption(audioLabels[i], dubFlag(audioKeys.getOrNull(i)), i == audioIdx) { onAudio(i) } }
                        }
                        "sleep" -> {
                            SheetHeader(I18n.t("ctl_sleep", "Sleep timer")) { onPage("main") }
                            listOf(0, 15, 30, 60, -1).forEach { m -> SheetOption(when (m) { 0 -> I18n.t("ctl_off", "Off"); -1 -> I18n.t("sleep_end", "End of episode"); else -> "$m min" }, sleepMin == m) { onSleep(m) } }
                        }
                        else -> {
                            SheetRowItem(Icons.Filled.Tune, I18n.t("quality", "Quality"), qLabel(quality)) { onPage("quality") }
                            SheetRowItem(Icons.Filled.ClosedCaption, I18n.t("subtitles", "Subtitles"), if (subTracks.isNotEmpty()) subLabel else I18n.t("subs_na", "Not available")) { if (subTracks.isNotEmpty()) onPage("subs") }
                            SheetRowItem(Icons.Filled.Speed, I18n.t("speed", "Playback speed"), if (speed == 1f) I18n.t("normal", "Normal") else "${spdLabel(speed)}×") { onPage("speed") }
                            if (audioLabels.size > 1) SheetRowItem(Icons.Filled.Audiotrack, I18n.t("dubbing", "Dubbing"), audioLabels.getOrElse(audioIdx) { "?" }) { onPage("audio") }
                            SheetRowItem(Icons.Filled.Bedtime, I18n.t("ctl_sleep", "Sleep timer"), when (sleepMin) { 0 -> I18n.t("ctl_off", "Off"); -1 -> I18n.t("sleep_end", "End of episode"); else -> "$sleepMin min" }) { onPage("sleep") }
                            SheetRowItem(Icons.Filled.PictureInPictureAlt, "Picture-in-picture", "") { onPip() }
                            SheetRowItem(Icons.Filled.Lock, I18n.t("ctl_lock", "Lock screen"), "") { onLock() }
                            SheetRowItem(Icons.Filled.Headphones, I18n.t("audio_only", "Audio only"), if (audioOnly) "On" else "Off") { onAudioOnly() }
                            SheetRowItem(Icons.Filled.Flag, I18n.t("report_problem", "Report a problem"), "") { onReport() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SheetRowItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Sp.S20, vertical = Sp.S16), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(22.dp), tint = Color.White)
        Text(label, Modifier.weight(1f).padding(start = Sp.S16), fontSize = Ty.Subtitle, color = Color.White)
        if (value.isNotEmpty()) {
            Text(value, fontSize = Ty.Body, color = NavOff, maxLines = 1)
            Icon(Icons.Filled.ChevronRight, null, Modifier.padding(start = Sp.S4).size(20.dp), tint = NavOff)
        }
    }
}

@Composable
internal fun SheetHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onBack).padding(horizontal = Sp.S16, vertical = Sp.S12), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(22.dp), tint = Color.White)
        Text(title, Modifier.padding(start = Sp.S16), fontSize = Ty.Subtitle, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}

@Composable
internal fun SheetOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Sp.S20, vertical = Sp.S12), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { if (selected) Icon(Icons.Filled.Check, null, Modifier.size(20.dp), tint = RedText) }
        Text(label, Modifier.padding(start = Sp.S16), fontSize = Ty.Subtitle, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) RedText else Color.White)
    }
}

// ------------------------------------------------------------------ Dubbing (trek audio)
private val LANG3 = mapOf(
    "jpn" to "ja", "ind" to "id", "eng" to "en", "kor" to "ko", "zho" to "zh", "chi" to "zh", "spa" to "es",
    "por" to "pt", "fra" to "fr", "fre" to "fr", "deu" to "de", "ger" to "de", "ara" to "ar", "tha" to "th",
    "vie" to "vi", "msa" to "ms", "may" to "ms", "rus" to "ru", "ita" to "it",
)

/** Nama dubbing yang ramah dibaca: label trek bila bermakna, kalau tidak nama bahasa dari kodenya ("ja" -> Japanese / Jepang). */
internal fun dubName(f: androidx.media3.common.Format, i: Int): String {
    val label = f.label?.trim().orEmpty()
    if (label.length > 3) return label
    val raw = (f.language ?: label).trim().lowercase()
    if (raw.isNotEmpty() && raw != "und") {
        val code = LANG3[raw] ?: raw
        val n = java.util.Locale.forLanguageTag(code).getDisplayLanguage(uiLocale())
        if (n.isNotBlank() && !n.equals(code, true)) return n.replaceFirstChar { it.uppercase() }
    }
    return "Track ${i + 1}"
}

/** Kunci stabil sebuah trek audio, dipakai mengingat pilihan dubbing antar episode. */
internal fun audioKey(f: androidx.media3.common.Format): String {
    val raw = (f.language ?: f.label ?: "").trim().lowercase().substringBefore('-')
    return LANG3[raw] ?: raw
}

/** Bendera kecil untuk kode bahasa trek audio; null bila tak dikenal (chip/baris memakai ikon not musik). */
internal fun dubFlag(key: String?): String? = when (key) {
    "ja" -> "\uD83C\uDDEF\uD83C\uDDF5"; "id" -> "\uD83C\uDDEE\uD83C\uDDE9"; "en" -> "\uD83C\uDDFA\uD83C\uDDF8"
    "ko" -> "\uD83C\uDDF0\uD83C\uDDF7"; "zh" -> "\uD83C\uDDE8\uD83C\uDDF3"; "es" -> "\uD83C\uDDEA\uD83C\uDDF8"
    "pt" -> "\uD83C\uDDE7\uD83C\uDDF7"; "fr" -> "\uD83C\uDDEB\uD83C\uDDF7"; "de" -> "\uD83C\uDDE9\uD83C\uDDEA"
    "ar" -> "\uD83C\uDDF8\uD83C\uDDE6"; "th" -> "\uD83C\uDDF9\uD83C\uDDED"; "vi" -> "\uD83C\uDDFB\uD83C\uDDF3"
    "ms" -> "\uD83C\uDDF2\uD83C\uDDFE"; "ru" -> "\uD83C\uDDF7\uD83C\uDDFA"; "it" -> "\uD83C\uDDEE\uD83C\uDDF9"
    else -> null
}

/**
 * Chip dubbing ringkas: bendera + nama dubbing aktif (+ panah bila ada pilihan lain). Tampilan hanya 24dp dengan tepi
 * bergradien halus; area ketuknya tetap 48dp (pedoman Material), ada umpan balik saat ditekan dan deskripsi untuk pembaca layar.
 */
@Composable
internal fun DubChip(label: String, flag: String?, canPick: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val desc = "${I18n.t("dubbing", "Dubbing")}: $label"
    Box(
        Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .then(if (canPick) Modifier.clickable(interactionSource = src, indication = null, role = Role.Button,
                onClickLabel = I18n.t("dub_pick", "Choose dubbing"), onClick = onClick) else Modifier),
        contentAlignment = Alignment.CenterEnd,
    ) {
        val shape = RoundedCornerShape(12.dp)
        Row(
            Modifier.height(24.dp).clip(shape)
                .background(if (pressed) Color(0xFF2A2E3F) else Color(0xCC1B1E2A))
                .border(1.dp, Brush.horizontalGradient(listOf(Color(0x99E8566F), Color(0x402A2D3B))), shape)
                .padding(start = if (flag != null) 6.dp else Sp.S8, end = if (canPick) 2.dp else Sp.S8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (flag != null) Text(flag, fontSize = 12.sp) else Icon(Icons.Filled.Audiotrack, null, Modifier.size(12.dp), tint = InkSoft)
            Text(label, Modifier.padding(start = 5.dp).widthIn(max = 120.dp), fontSize = Ty.Micro, fontWeight = FontWeight.SemiBold,
                color = Color.White, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (canPick) Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(14.dp), tint = NavOff)
        }
    }
}

/** Baris pilihan dubbing yang ringkas: bendera dalam lingkaran kecil, nama, dan centang di kanan; baris aktif diberi latar merah tipis. */
@Composable
internal fun DubOption(label: String, flag: String?, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Sp.S12, vertical = 2.dp).clip(shape)
            .background(if (selected) Color(0x26E8566F) else Color.Transparent)
            .clickable(role = Role.RadioButton, onClick = onClick).padding(horizontal = Sp.S8, vertical = Sp.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0x1FFFFFFF)), contentAlignment = Alignment.Center) {
            if (flag != null) Text(flag, fontSize = 15.sp) else Icon(Icons.Filled.Audiotrack, null, Modifier.size(15.dp), tint = InkSoft)
        }
        Text(label, Modifier.weight(1f).padding(start = Sp.S12), fontSize = Ty.Body, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) RedText else Color.White, maxLines = 1)
        if (selected) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = RedText)
    }
}

internal fun spdLabel(f: Float): String = if (f == f.toInt().toFloat()) f.toInt().toString() else f.toString()
