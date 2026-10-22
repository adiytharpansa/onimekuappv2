package com.onimeku.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp

/** Ikon merek (kanvas 24x24). Warna resmi Google dan Discord, digambar vektor sehingga tajam di semua kepadatan layar. */
/** Path memakai perintah M/L/C/Z saja (tanpa arc): parser vektor Compose salah membaca flag arc yang ditulis rapat. */
private fun p(d: String): Path = PathParser().parsePathString(d).toPath()

@Composable
fun GoogleIcon(size: Dp) {
    val paths = remember {
        listOf(
            Color(0xFF4285F4) to p("M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"),
            Color(0xFF34A853) to p("M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"),
            Color(0xFFFBBC05) to p("M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"),
            Color(0xFFEA4335) to p("M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"),
        )
    }
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 24f
        scale(k, k, Offset.Zero) { paths.forEach { (c, path) -> drawPath(path, c) } }
    }
}

@Composable
fun DiscordIcon(size: Dp) {
    val logo = remember { p("M20.317 4.3698C18.761 3.6556 17.1189 3.1462 15.4319 2.8546C15.4005 2.8487 15.3688 2.8637 15.3534 2.8917C15.1424 3.267 14.9087 3.7565 14.7451 4.1412C12.9004 3.865 11.0651 3.865 9.2583 4.1412C9.0947 3.7479 8.8525 3.267 8.6406 2.8917C8.6245 2.8644 8.5934 2.8497 8.5621 2.8547C6.8749 3.1457 5.2327 3.6549 3.6769 4.3697C3.6636 4.3754 3.6524 4.3851 3.6448 4.3974C0.5334 9.0458 -0.319 13.5799 0.0992 18.0578C0.1016 18.0799 0.1128 18.1002 0.1304 18.1139C2.1832 19.6215 4.1717 20.5367 6.1233 21.1433C6.1544 21.1526 6.1879 21.1416 6.2075 21.1157C6.6691 20.4853 7.0806 19.8205 7.4335 19.1215C7.4433 19.1021 7.4444 19.0795 7.4364 19.0593C7.4285 19.0391 7.4123 19.0233 7.3919 19.0158C6.7391 18.7682 6.1176 18.4663 5.5197 18.1235C5.4976 18.1105 5.4834 18.0873 5.4818 18.0617C5.4803 18.036 5.4917 18.0113 5.5121 17.9958C5.6379 17.9015 5.7638 17.8035 5.8839 17.7044C5.9057 17.6864 5.9357 17.6824 5.9615 17.6939C9.8893 19.4872 14.1415 19.4872 18.0229 17.6939C18.0487 17.6816 18.0793 17.6853 18.1014 17.7034C18.2216 17.8024 18.3474 17.9015 18.4742 17.9958C18.4947 18.0112 18.5062 18.0358 18.5049 18.0614C18.5035 18.087 18.4896 18.1102 18.4676 18.1234C17.8695 18.4727 17.2429 18.7709 16.5946 19.0148C16.5742 19.0226 16.5582 19.0387 16.5504 19.059C16.5426 19.0794 16.5439 19.1021 16.5539 19.1215C16.9143 19.8195 17.3258 20.4843 17.7789 21.1147C17.7979 21.1414 17.8318 21.1529 17.8631 21.1433C19.8241 20.5366 21.8126 19.6214 23.8654 18.1139C23.8833 18.1009 23.8947 18.0807 23.8967 18.0587C24.3971 12.8817 23.0585 8.3848 20.3482 4.3983C20.3415 4.3853 20.3305 4.3752 20.317 4.3697ZM8.02 15.3312C6.8375 15.3312 5.8631 14.2455 5.8631 12.9122C5.8631 11.579 6.8186 10.4933 8.0201 10.4933C9.2309 10.4933 10.1958 11.5885 10.1769 12.9123C10.1769 14.2455 9.2214 15.3312 8.02 15.3312ZM15.9948 15.3312C14.8123 15.3312 13.8379 14.2455 13.8379 12.9122C13.8379 11.579 14.7933 10.4933 15.9948 10.4933C17.2056 10.4933 18.1705 11.5885 18.1516 12.9123C18.1516 14.2455 17.2056 15.3312 15.9948 15.3312Z").apply { fillType = PathFillType.EvenOdd } }
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 24f
        scale(k, k, Offset.Zero) { drawPath(logo, Color(0xFF5865F2)) }
    }
}
