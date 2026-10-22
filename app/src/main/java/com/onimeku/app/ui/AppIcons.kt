package com.onimeku.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Ikon garis aplikasi (debug.67): kisi 24, garis bulat 1,9 — SAMA dengan
 * ikon bilah bawah ([NavGlyph]) sehingga seluruh aplikasi satu bahasa visual
 * (gaya Lucide). Menggantikan Icons.Outlined.* Material di layar non-pemutar.
 * Warna diatur lewat tint pada Icon (garis hitam di sini hanya penampung).
 * Bintang, segitiga putar, dan kontrol pemutar sengaja tetap Material Filled.
 */
object AppIcons {
    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

    private fun build(name: String, mirror: Boolean, filledFirst: Boolean, d: List<String>): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = mirror).apply {
            d.forEachIndexed { i, p ->
                val fillIt = filledFirst && i == 0
                addPath(
                    PathParser().parsePathString(p).toNodes(),
                    fill = if (fillIt) SolidColor(Color.Black) else null,
                    stroke = SolidColor(Color.Black), strokeLineWidth = 1.9f,
                    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun line(name: String, vararg d: String): ImageVector = build(name, false, false, d.toList())
    /** Ikon berarah (panah/chevron/kirim): otomatis dicerminkan pada bahasa kanan-ke-kiri. */
    private fun lineM(name: String, vararg d: String): ImageVector = build(name, true, false, d.toList())
    /** Bentuk pertama terisi + bergaris (keadaan aktif); sisanya hanya garis. */
    private fun solid(name: String, vararg d: String): ImageVector = build(name, false, true, d.toList())

    val Download: ImageVector by lazy { line("Download", "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "M7 10l5 5 5-5", "M12 15V3") }
    val Share: ImageVector by lazy { line("Share", "M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8", "M16 6l-4-4-4 4", "M12 2v13") }
    val Trash: ImageVector by lazy { line("Trash", "M3 6h18", "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6", "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2", "M10 11v6", "M14 11v6") }
    val Edit: ImageVector by lazy { line("Edit", "M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5z", "M15 5l4 4") }
    val Search: ImageVector by lazy { line("Search", circle(11f, 11f, 8f), "M21 21l-4.3-4.3") }
    val Info: ImageVector by lazy { line("Info", circle(12f, 12f, 10f), "M12 16v-4", "M12 8h.01") }
    val Help: ImageVector by lazy { line("Help", circle(12f, 12f, 10f), "M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3", "M12 17h.01") }
    val Lock: ImageVector by lazy { line("Lock", "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z", "M7 11V7a5 5 0 0 1 10 0v4") }
    val User: ImageVector by lazy { line("User", circle(12f, 7f, 4f), "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2") }
    val History: ImageVector by lazy { line("History", "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8", "M3 3v5h5", "M12 7v5l4 2") }
    val Clock: ImageVector by lazy { line("Clock", circle(12f, 12f, 10f), "M12 6v6l4 2") }
    val Globe: ImageVector by lazy { line("Globe", circle(12f, 12f, 10f), "M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20", "M2 12h20") }
    val Message: ImageVector by lazy { line("Message", "M7.9 20A9 9 0 1 0 4 16.1L2 22z") }
    val Flame: ImageVector by lazy { line("Flame", "M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z") }
    val Bell: ImageVector by lazy { line("Bell", "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0") }
    val Bookmark: ImageVector by lazy { line("Bookmark", "M19 21l-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z") }
    val Refresh: ImageVector by lazy { line("Refresh", "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8", "M21 3v5h-5", "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16", "M8 16H3v5") }
    val Eye: ImageVector by lazy { line("Eye", "M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0", circle(12f, 12f, 3f)) }
    val EyeOff: ImageVector by lazy { line("EyeOff", "M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49", "M14.084 14.158a3 3 0 0 1-4.242-4.242", "M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143", "M2 2l20 20") }
    val Settings: ImageVector by lazy { line("Settings", "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z", circle(12f, 12f, 3f)) }
    val Inbox: ImageVector by lazy { line("Inbox", "M22 12h-6l-2 3h-4l-2-3H2", "M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z") }
    val Keyboard: ImageVector by lazy { line("Keyboard", "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M6 8h.01", "M10 8h.01", "M14 8h.01", "M18 8h.01", "M8 12h.01", "M12 12h.01", "M16 12h.01", "M7 16h10") }
    val Dice: ImageVector by lazy { line("Dice", "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z", "M8 8h.01", "M16 8h.01", "M12 12h.01", "M8 16h.01", "M16 16h.01") }
    val Check: ImageVector by lazy { line("Check", "M20 6L9 17l-5-5") }
    val Plus: ImageVector by lazy { line("Plus", "M5 12h14", "M12 5v14") }
    val Close: ImageVector by lazy { line("Close", "M18 6L6 18", "M6 6l12 12") }
    val ChevronRight: ImageVector by lazy { lineM("ChevronRight", "M9 18l6-6-6-6") }
    val ChevronUp: ImageVector by lazy { line("ChevronUp", "M18 15l-6-6-6 6") }
    val ChevronDown: ImageVector by lazy { line("ChevronDown", "M6 9l6 6 6-6") }
    val ArrowLeft: ImageVector by lazy { lineM("ArrowLeft", "M19 12H5", "M12 19l-7-7 7-7") }
    val Send: ImageVector by lazy { lineM("Send", "M22 2L11 13", "M22 2l-7 20-4-9-9-4 20-7z") }
    val Swap: ImageVector by lazy { line("Swap", "M21 16l-4 4-4-4", "M17 20V4", "M3 8l4-4 4 4", "M7 4v16") }
    val Mic: ImageVector by lazy { line("Mic", "M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3z", "M19 10v2a7 7 0 0 1-14 0v-2", "M12 19v3") }
    val MicOff: ImageVector by lazy { line("MicOff", "M2 2l20 20", "M18.89 13.23A7.12 7.12 0 0 0 19 12v-2", "M5 10v2a7 7 0 0 0 12 5", "M15 9.34V5a3 3 0 0 0-5.68-1.33", "M9 9v3a3 3 0 0 0 5.12 2.12", "M12 19v3") }
    val BellFill: ImageVector by lazy { solid("BellFill", "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9z", "M10.3 21a1.94 1.94 0 0 0 3.4 0") }
    val BookmarkFill: ImageVector by lazy { solid("BookmarkFill", "M19 21l-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z") }
}
