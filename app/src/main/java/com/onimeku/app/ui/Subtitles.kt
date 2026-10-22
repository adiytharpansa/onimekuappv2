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

/** Satu baris di menu subtitle; lihat [SubLang.menu]. */
sealed interface SubMenuEntry {
    object Auto : SubMenuEntry
    data class Track(val index: Int) : SubMenuEntry
    object Off : SubMenuEntry
}

/** Logika pemilihan subtitle otomatis: Indonesia / English, mengikuti preferensi & bahasa aplikasi. */
object SubLang {
    const val INDONESIAN_LABEL = "Indonesian"
    /** "id", "en", atau null (bahasa lain) dari kode bahasa + label trek. */
    fun code(lang: String, label: String = ""): String? {
        if (lang.trim().equals("in", true)) return "id"   // kode lama ISO 639-1 untuk Indonesia
        val s = "$lang $label".lowercase()
        return when {
            s.contains("indo") || Regex("\\b(id|ind)\\b").containsMatchIn(s) -> "id"
            s.contains("engl") || Regex("\\b(en|eng)\\b").containsMatchIn(s) -> "en"
            else -> null
        }
    }

    fun code(t: ApiSubtitle): String? = code(t.lang, t.label)

    /** Trek sebagian (hanya "Signs & Songs", forced, lirik): bukan terjemahan dialog penuh, jangan jadi pilihan otomatis. */
    private val PARTIAL = Regex("\\b(forced|signs?|songs?|karaoke|lyrics?)\\b", RegexOption.IGNORE_CASE)
    fun isPartial(t: ApiSubtitle): Boolean = PARTIAL.containsMatchIn(t.label)

    /** Kunci bahasa untuk disimpan sebagai preferensi: "id"/"en" atau kode 2 huruf bahasa lain ("ar", "ko", ...); null bila tak dikenal. */
    fun key(t: ApiSubtitle): String? =
        code(t) ?: DubLogic.normalize(t.lang).ifEmpty { DubLogic.normalize(t.label) }.ifEmpty { null }

    /** Nama ramah untuk ditampilkan. */
    private val VARIANT = Regex("\\b(sdh|cc|forced|signs?\\s*(&|and)?\\s*songs?|signs?|songs?|karaoke|commentary)\\b", RegexOption.IGNORE_CASE)
    fun display(t: ApiSubtitle, i: Int): String {
        val base = when (code(t)) {
            "id" -> INDONESIAN_LABEL
            "en" -> "English"
            else -> return t.label.ifBlank { t.lang.uppercase() }.ifBlank { "Sub ${i + 1}" }
        }
        // Dua trek se-bahasa (mis. "English" dan "English (SDH)") dibedakan lewat penanda variannya.
        val v = VARIANT.find(t.label)?.value?.trim()
        return if (v != null) "$base · $v" else base
    }

    /**
     * Susunan baku menu subtitle (satu sumber untuk semua menu): Auto, English, Indonesian, bahasa lain, Off.
     * Hanya trek yang benar-benar tersedia yang tampil; bahasa yang tidak ada tidak ditawarkan.
     * Urutan antar trek yang bahasanya sama mengikuti urutan aslinya (stabil).
     */
    fun menu(tracks: List<ApiSubtitle>): List<SubMenuEntry> {
        val idx = tracks.indices
        val en = idx.filter { code(tracks[it]) == "en" }
        val id = idx.filter { code(tracks[it]) == "id" }
        val other = idx.filter { code(tracks[it]) == null }
        return buildList {
            add(SubMenuEntry.Auto)
            en.forEach { add(SubMenuEntry.Track(it)) }
            id.forEach { add(SubMenuEntry.Track(it)) }
            other.forEach { add(SubMenuEntry.Track(it)) }
            add(SubMenuEntry.Off)
        }
    }

    fun prefName(pref: String): String = when (pref) {
        "id" -> INDONESIAN_LABEL; "en" -> "English"; "off" -> I18n.t("ctl_off", "Off"); "auto" -> "Auto"
        else -> dubLangName(pref) ?: pref
    }

    /**
     * Pilih indeks trek. pref: "auto" | "off" | "id" | "en" | kode bahasa lain ("ar", "ko", ...). Auto mengikuti bahasa
     * aplikasi (aplikasi Indonesia -> Indonesia dulu, selain itu English dulu); bahasa pilihan yang tidak ada jatuh ke
     * English/Indonesia, lalu trek pertama. Di tiap bahasa, trek dialog penuh didahulukan dari trek sebagian
     * ("Signs & Songs", forced). -1 = mati.
     */
    fun pick(tracks: List<ApiSubtitle>, pref: String, appLang: String): Int {
        if (pref == "off" || tracks.isEmpty()) return -1
        val primary = when (pref) { "auto" -> if (appLang == "id") "id" else "en"; else -> pref }
        val order = buildList {
            add(primary)
            if (primary != "en") add("en")
            if (primary != "id") add("id")
        }
        for (c in order) {
            val same = tracks.indices.filter { key(tracks[it]) == c }
            val i = same.firstOrNull { !isPartial(tracks[it]) } ?: same.firstOrNull()
            if (i != null) return i
        }
        return tracks.indexOfFirst { !isPartial(it) }.takeIf { it >= 0 } ?: 0
    }

    /**
     * Pilihan setelah trek dipasang ulang (mis. karena geseran waktu). [keep] = daftar trek tidak berubah, jadi
     * pilihan [current] (-1 = mati) dipertahankan apa adanya; selain itu dipilih ulang lewat [pick].
     * Tanpa ini, pilihan "English · SDH" / "Signs & Songs" selalu kembali ke trek dialog pertama.
     */
    fun restore(keep: Boolean, current: Int, tracks: List<ApiSubtitle>, pref: String, appLang: String): Int =
        if (keep && current >= -1 && current < tracks.size) current else pick(tracks, pref, appLang)
}

@Composable
private fun SubOpt(label: String, sub: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Sp.S24, vertical = Sp.S16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = Ty.Subtitle, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) RedText else androidx.compose.ui.graphics.Color.White)
            if (sub != null) Text(sub, fontSize = Ty.Small, color = NavOff, modifier = Modifier.padding(top = Sp.S2))
        }
        if (selected) Icon(AppIcons.Check, null, Modifier.size(20.dp), tint = RedText)
    }
}

/** Menu subtitle: Auto, English, Indonesian, bahasa lain, Off (urutan dari [SubLang.menu]). */
@Composable
fun SubtitleDialog(
    tracks: List<ApiSubtitle>, current: Int, pref: String, appLang: String,
    onTrack: (Int) -> Unit, onOff: () -> Unit, onAuto: () -> Unit, onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(22.dp)
        Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).clip(shape).background(Bg2).border(1.dp, Line, shape).padding(vertical = Sp.S12)) {
            Text(I18n.t("subtitles", "Subtitles"), Modifier.padding(horizontal = Sp.S24, vertical = Sp.S8), fontSize = Ty.Title, fontWeight = FontWeight.Bold)
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val autoSub = if (pref == "auto" && current >= 0) tracks.getOrNull(current)?.let { SubLang.display(it, current) } else null
                SubLang.menu(tracks).forEach { e ->
                    when (e) {
                        SubMenuEntry.Auto -> SubOpt("Auto", I18n.t("sub_auto_d", "English / Indonesian, automatically") + (autoSub?.let { " · $it" } ?: ""), pref == "auto") { onAuto() }
                        is SubMenuEntry.Track -> SubOpt(SubLang.display(tracks[e.index], e.index), null, pref != "auto" && current == e.index) { onTrack(e.index) }
                        SubMenuEntry.Off -> SubOpt(I18n.t("ctl_off", "Off"), null, current < 0) { onOff() }
                    }
                }
            }
        }
    }
}
