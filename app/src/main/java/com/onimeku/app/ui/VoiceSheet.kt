package com.onimeku.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onimeku.app.I18n
import com.onimeku.app.data.Anime
import com.onimeku.app.data.Catalog
import java.util.Locale

// ------------------------------------------------------------------ VOICE SEARCH
/** Status panel suara. */
private sealed interface VState {
    data object Listening : VState
    data class Error(val title: String, val body: String, val needsPermission: Boolean = false) : VState
    /** [heard] = ucapan terbaik, [picks] = saran judul katalog, [selected] = teks yang akan dicari. */
    data class Done(val heard: String, val picks: List<String>, val selected: String) : VState
}

private val SheetBg = Color(0xFF161824)
private val RingRed = Color(0xFFC4143A)

/** Ucapan "gin tama" harus cocok dengan "Gintama": abaikan spasi dan tanda baca. */
private fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

private fun errorFor(code: Int): VState.Error = when (code) {
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER ->
        VState.Error(I18n.t("voice_net_t", "No connection"), I18n.t("voice_net_b", "Voice search needs the internet. Check your connection and try again."))
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
        VState.Error(I18n.t("voice_perm_t", "Microphone access needed"), I18n.t("voice_perm_b", "Allow the microphone in settings to use voice search."), needsPermission = true)
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
        VState.Error(I18n.t("voice_busy_t", "Voice service is busy"), I18n.t("voice_busy_b", "Wait a moment and try again."))
    12, 13 -> // ERROR_LANGUAGE_NOT_SUPPORTED / ERROR_LANGUAGE_UNAVAILABLE (API 31+)
        VState.Error(I18n.t("voice_lang_t", "Language not available"), I18n.t("voice_lang_b", "Download the language pack in Google voice settings, or pick another language."))
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
        VState.Error(I18n.t("voice_unclear_t", "Couldn't hear that clearly"), I18n.t("voice_unclear_b", "Hold your phone closer, or switch to English for titles like Gintama or One Piece."))
    else -> VState.Error(I18n.t("voice_fail_t", "Voice search failed"), I18n.t("voice_fail_b", "Try again or type the title instead."))
}

/** Ubah alternatif hasil pengenalan menjadi keadaan Done (atau Error bila kosong). */
private fun buildDone(alts: List<String>, titles: List<Pair<Anime, String>>): VState {
    val list = alts.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    val heard = list.firstOrNull() ?: return errorFor(SpeechRecognizer.ERROR_NO_MATCH)
    val picks = LinkedHashSet<String>()
    for (alt in list) {
        val n = norm(alt)
        if (n.length < 2) continue
        titles.filter { it.second.contains(n) }.take(3).forEach { picks += it.first.title }
        if (picks.size >= 3) break
    }
    val rawHit = titles.any { it.first.title.lowercase().contains(heard.lowercase()) }
    // Pencarian layar Search memakai "contains" mentah: bila ucapan tak cocok mentah, pakai judul katalog.
    val selected = if (rawHit || picks.isEmpty()) heard else picks.first()
    return VState.Done(heard, picks.take(3).ifEmpty { list.take(3) }, selected)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceSearchSheet(onApply: (String) -> Unit, onType: () -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var lang by rememberSaveable { mutableStateOf(if (I18n.lang == "id") "id" else "en") }
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<VState>(VState.Listening) }
    var partial by remember { mutableStateOf("") }
    var level by remember { mutableFloatStateOf(0f) }
    var granted by remember { mutableStateOf(ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val askPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (!ok) state = errorFor(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
    }
    // Judul katalog ternormalisasi, dihitung sekali per perubahan katalog.
    val titles = remember(Catalog.all) { Catalog.unique.map { it to norm(it.title) } }
    LaunchedEffect(Unit) { if (!granted) askPerm.launch(Manifest.permission.RECORD_AUDIO) }

    // Pengenal suara: dimulai ulang tiap [attempt] atau ganti bahasa; selalu dilepas saat keluar.
    DisposableEffect(attempt, lang, granted) {
        if (!granted) return@DisposableEffect onDispose { }
        state = VState.Listening; partial = ""; level = 0f
        val rec = SpeechRecognizer.createSpeechRecognizer(ctx)
        rec.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() { level = 0f }
            override fun onEvent(t: Int, p: Bundle?) {}
            override fun onPartialResults(b: Bundle?) {
                partial = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            }
            override fun onResults(b: Bundle?) {
                state = buildDone(b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty(), titles)
            }
            override fun onError(code: Int) {
                if (code == SpeechRecognizer.ERROR_CLIENT) return // terjadi saat dibatalkan/dilepas: bukan kegagalan
                state = errorFor(code)
            }
        })
        rec.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (lang == "id") "id-ID" else "en-US")
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName),
        )
        onDispose { rec.cancel(); rec.destroy() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss, containerColor = SheetBg, contentColor = Color.White,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LangToggle(lang) { lang = it }
            when (val s = state) {
                is VState.Listening -> {
                    MicPulse(level, active = true)
                    Text(I18n.t("voice_listening", "Listening"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        partial.ifEmpty { " " }, Modifier.padding(top = 8.dp).heightIn(min = 30.dp),
                        fontSize = 22.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                    )
                    Text(I18n.t("voice_prompt", "Say an anime title or genre"), Modifier.padding(top = 6.dp), fontSize = 13.sp, color = NavOff)
                    Text(
                        I18n.t("voice_cancel", "Cancel"), Modifier.padding(top = 14.dp).clip(CircleShape).clickable(onClick = onDismiss).padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 13.sp, color = NavOff,
                    )
                }
                is VState.Error -> {
                    MicPulse(0f, active = false)
                    Text(s.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Text(s.body, Modifier.padding(top = 6.dp), fontSize = 13.sp, lineHeight = 19.sp, color = NavOff, textAlign = TextAlign.Center)
                    Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VButton(I18n.t("voice_type", "Type"), Icons.Outlined.Keyboard, primary = false, Modifier.weight(1f), onType)
                        if (s.needsPermission) VButton(I18n.t("voice_settings", "Open settings"), null, primary = true, Modifier.weight(1f)) {
                            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
                        } else VButton(I18n.t("retry", "Retry"), Icons.Outlined.Refresh, primary = true, Modifier.weight(1f)) { attempt++ }
                    }
                }
                is VState.Done -> {
                    var sel by remember(s) { mutableStateOf(s.selected) }
                    val hits = remember(sel, titles) { norm(sel).let { n -> titles.count { it.second.contains(n) } } }
                    Text(I18n.t("voice_heard", "I heard"), Modifier.padding(top = 20.dp), fontSize = 13.sp, color = NavOff)
                    Text(s.heard, Modifier.padding(top = 4.dp), fontSize = 22.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    Text(I18n.t("voice_maybe", "Did you mean"), Modifier.padding(top = 18.dp, bottom = 10.dp), fontSize = 13.sp, color = NavOff)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.picks.forEach { p -> Chip(p, on = p == sel) { sel = p } }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VButton(I18n.t("voice_repeat", "Again"), Icons.Filled.Mic, primary = false, Modifier.weight(1f)) { attempt++ }
                        VButton(
                            if (hits > 0) I18n.t("voice_see", "See %d results").format(hits) else I18n.t("voice_search_go", "Search"),
                            null, primary = true, Modifier.weight(1.3f),
                        ) { onApply(sel) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LangToggle(lang: String, onPick: (String) -> Unit) {
    Row(Modifier.clip(CircleShape).background(Color(0xFF0F1019)).border(1.dp, Line, CircleShape).padding(3.dp)) {
        listOf("id" to "Indonesia", "en" to "English").forEach { (code, label) ->
            val on = code == lang
            Text(
                label, Modifier.clip(CircleShape).background(if (on) Color(0xFF2A2D42) else Color.Transparent)
                    .clickable { if (!on) onPick(code) }.padding(horizontal = 16.dp, vertical = 7.dp),
                fontSize = 13.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) Color.White else NavOff,
            )
        }
    }
}

/** Mikrofon dengan dua cincin yang memuai; ukuran inti ikut kerasnya suara. Statis bila reduce-motion. */
@Composable
private fun MicPulse(level: Float, active: Boolean) {
    val reduce = LocalReduceMotion.current
    val t = rememberInfiniteTransition(label = "mic")
    @Composable fun ring(offset: Int) = t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2000, easing = LinearOutSlowInEasing), RepeatMode.Restart, StartOffset(offset)), label = "ring$offset",
    )
    val r1 by ring(0); val r2 by ring(1000)
    val core by animateFloatAsState(1f + level * 0.12f, tween(90), label = "core")
    Box(Modifier.padding(top = 24.dp, bottom = 16.dp).size(88.dp), contentAlignment = Alignment.Center) {
        if (active && !reduce) listOf(r1, r2).forEach { p ->
            Box(Modifier.matchParentSize().graphicsLayer { scaleX = 1f + p * 0.8f; scaleY = 1f + p * 0.8f; alpha = 0.7f * (1f - p) }
                .border(2.dp, RingRed, CircleShape))
        }
        Box(
            Modifier.matchParentSize().graphicsLayer { scaleX = core; scaleY = core }.clip(CircleShape)
                .background(if (active) RingRed else Color(0xFF2A2D42)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (active) Icons.Filled.Mic else Icons.Filled.MicOff, null, Modifier.size(34.dp), tint = if (active) Color.White else NavOff)
        }
    }
}

@Composable
private fun VButton(text: String, icon: ImageVector?, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.height(46.dp).clip(shape).background(if (primary) Red else Color.Transparent)
            .border(1.dp, if (primary) Red else Line, shape).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp), tint = Color.White); Spacer(Modifier.width(8.dp)) }
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (primary) Color.White else TimeInk)
    }
}
