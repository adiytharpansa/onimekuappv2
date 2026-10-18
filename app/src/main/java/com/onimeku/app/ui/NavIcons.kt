package com.onimeku.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale

/**
 * Ikon bilah bawah gaya "line + duotone" (garis bulat 1,9 pada kisi 24, seperti
 * Lucide / Phosphor). Tidak memakai Material Icons bawaan. Saat aktif, badan ikon
 * terisi lembut ([fill] 0..1) dengan warna yang sama sehingga terasa premium.
 * Path diuraikan oleh PathParser androidx.core (parser VectorDrawable) — kokoh.
 */
enum class NavGlyph(val body: List<String>, val detail: List<String>) {
    Home(
        body = listOf("M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"),
        detail = listOf("M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"),
    ),
    Calendar(
        body = listOf("M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"),
        detail = listOf("M8 2v4", "M16 2v4", "M3 10h18"),
    ),
    Bookmark(
        body = listOf("M19 21l-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"),
        detail = emptyList(),
    ),
    Search(
        body = listOf("M3 11a8 8 0 1 0 16 0a8 8 0 1 0 -16 0z"),
        detail = listOf("M21 21l-4.3-4.3"),
    ),
    Profile(
        body = listOf("M8 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0z", "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"),
        detail = emptyList(),
    ),
}

private fun parse(d: String): Path =
    androidx.core.graphics.PathParser.createPathFromPathData(d).asComposePath()

@Composable
fun NavGlyphIcon(glyph: NavGlyph, tint: Color, fill: Float, modifier: Modifier = Modifier) {
    val body = remember(glyph) { glyph.body.map { parse(it) } }
    val detail = remember(glyph) { glyph.detail.map { parse(it) } }
    Canvas(modifier) {
        scale(size.width / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            if (fill > 0.01f) body.forEach { drawPath(it, tint.copy(alpha = tint.alpha * 0.38f * fill)) }
            body.forEach { drawPath(it, tint, style = stroke) }
            detail.forEach { drawPath(it, tint, style = stroke) }
        }
    }
}
