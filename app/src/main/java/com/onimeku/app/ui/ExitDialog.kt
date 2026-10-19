package com.onimeku.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.onimeku.app.I18n
import kotlinx.coroutines.delay

/**
 * Dialog konfirmasi keluar bertema Onimeku (menggantikan AlertDialog bawaan Material 3).
 * - "Batal" netral, "Keluar" merah solid: pilihan aman dan aksi keluar terbedakan jelas.
 * - Masuk/keluar dengan fade + skala halus (tanpa animasi bila "Hapus animasi" aktif).
 */
@Composable
fun ExitDialog(onCancel: () -> Unit, onExit: () -> Unit) {
    val reduce = LocalReduceMotion.current
    var shown by remember { mutableStateOf(reduce) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    LaunchedEffect(Unit) { shown = true }
    // Tutup dengan animasi: sembunyikan dulu, jalankan aksi setelah animasi selesai.
    LaunchedEffect(pending) {
        val act = pending ?: return@LaunchedEffect
        shown = false
        if (!reduce) delay(180)
        act()
    }
    val k by animateFloatAsState(
        if (shown) 1f else 0f, tween(if (reduce) 0 else if (shown) 240 else 170, easing = FastOutSlowInEasing), label = "exitDlg",
    )
    Dialog(
        onDismissRequest = { if (pending == null) pending = onCancel },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 340.dp).fillMaxWidth()
                    .graphicsLayer { alpha = k; val s = 0.94f + 0.06f * k; scaleX = s; scaleY = s }
                    .clip(RoundedCornerShape(20.dp)).background(Bg2)
                    .border(0.5.dp, Line, RoundedCornerShape(20.dp))
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
            ) {
                Text(I18n.t("exit_title", "Exit Onimeku?"), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color(0xFFF4F4F8), lineHeight = 24.sp)
                Text(
                    I18n.t("exit_body", "Your watch progress is saved."), Modifier.padding(top = 8.dp),
                    fontSize = 14.sp, lineHeight = 21.sp, color = Color(0xFF9A9DB0),
                )
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DlgBtn(I18n.t("cancel", "Cancel"), Modifier.weight(1f), Color(0xFF232738), Color(0xFFF4F4F8), Line) { if (pending == null) pending = onCancel }
                    DlgBtn(I18n.t("exit", "Exit"), Modifier.weight(1f), Red, Color.White, null) { if (pending == null) pending = onExit }
                }
            }
        }
    }
}

@Composable
private fun DlgBtn(text: String, modifier: Modifier, bg: Color, fg: Color, stroke: Color?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.height(44.dp).clip(shape).background(bg).let { if (stroke != null) it.border(0.5.dp, stroke, shape) else it }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = fg, textAlign = TextAlign.Center) }
}
