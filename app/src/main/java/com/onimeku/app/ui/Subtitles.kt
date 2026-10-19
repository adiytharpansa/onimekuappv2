package com.onimeku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.onimeku.app.I18n
import com.onimeku.app.data.ApiSubtitle

/** Logika pemilihan subtitle otomatis: Indonesia / English, mengikuti preferensi & bahasa aplikasi. */
object SubLang {
    /** "id", "en", atau null (bahasa lain) dari kode bahasa + label trek. */
    fun code(lang: String, label: String = ""): String? {
        val s = "$lang $label".lowercase()
        return when {
            s.contains("indo") || Regex("\\b(id|ind)\\b").containsMatchIn(s) -> "id"
            s.contains("engl") || Regex("\\b(en|eng)\\b").containsMatchIn(s) -> "en"
            else -> null
        }
    }

    fun code(t: ApiSubtitle): String? = code(t.lang, t.label)

    /** Nama ramah untuk ditampilkan. */
    fun display(t: ApiSubtitle, i: Int): String = when (code(t)) {
        "id" -> "Indonesia"
        "en" -> "English"
        else -> t.label.ifBlank { t.lang.uppercase() }.ifBlank { "Sub ${i + 1}" }
    }

    fun prefName(pref: String): String = when (pref) {
        "id" -> "Indonesia"; "en" -> "English"; "off" -> I18n.t("ctl_off", "Off"); else -> "Auto"
    }

    /**
     * Pilih indeks trek. pref: "auto" | "id" | "en" | "off". Auto mengikuti bahasa aplikasi
     * (English dulu, lalu Indonesia); bila bahasa pilihan tidak ada, jatuh ke
     * bahasa kedua (Indonesia <-> English), lalu trek pertama. -1 = mati.
     */
    fun pick(tracks: List<ApiSubtitle>, pref: String, appLang: String): Int {
        if (pref == "off" || tracks.isEmpty()) return -1
        val order = when (pref) {
            "id" -> listOf("id", "en")
            "en" -> listOf("en", "id")
            else -> listOf("en", "id")
        }
        for (c in order) { val i = tracks.indexOfFirst { code(it) == c }; if (i >= 0) return i }
        return 0
    }
}

@Composable
private fun SubOpt(label: String, sub: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) RedText else androidx.compose.ui.graphics.Color.White)
            if (sub != null) Text(sub, fontSize = 12.5.sp, color = NavOff, modifier = Modifier.padding(top = 2.dp))
        }
        if (selected) Icon(Icons.Filled.Check, null, Modifier.size(20.dp), tint = RedText)
    }
}

/** Menu subtitle: Auto, Off, dan semua trek tersedia (English lalu Indonesia). */
@Composable
fun SubtitleDialog(
    tracks: List<ApiSubtitle>, current: Int, pref: String, appLang: String,
    onTrack: (Int) -> Unit, onOff: () -> Unit, onAuto: () -> Unit, onDismiss: () -> Unit,
) {
    val order = tracks.indices.sortedBy { i -> when (SubLang.code(tracks[i])) { "en" -> 0; "id" -> 1; else -> 2 } }
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(22.dp)
        Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).clip(shape).background(Bg2).border(1.dp, Line, shape).padding(vertical = 12.dp)) {
            Text(I18n.t("subtitles", "Subtitles"), Modifier.padding(horizontal = 22.dp, vertical = 8.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val autoSub = if (pref == "auto" && current >= 0) tracks.getOrNull(current)?.let { SubLang.display(it, current) } else null
                SubOpt("Auto", (I18n.t("sub_auto_d", "English / Indonesia, automatically")) + (autoSub?.let { " · $it" } ?: ""), pref == "auto") { onAuto() }
                order.forEach { i ->
                    SubOpt(SubLang.display(tracks[i], i), null, pref != "auto" && current == i) { onTrack(i) }
                }
                SubOpt(I18n.t("ctl_off", "Off"), null, current < 0) { onOff() }
            }
        }
    }
}
