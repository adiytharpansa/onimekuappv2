package com.onimeku.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsivitas lintas perangkat (ponsel, lipat, tablet, jendela terbagi).
 * Semua keputusan tata letak memakai LEBAR JENDELA saat ini (bukan lebar fisik layar),
 * sehingga mode split-screen, layar lipat, dan "Display size" besar ikut benar.
 * Ambang mengikuti kelas ukuran jendela Android: <600 compact, 600-839 medium, >=840 expanded.
 */
enum class WidthClass { Compact, Medium, Expanded }

/** Logika murni (bisa diuji tanpa Compose): lebar jendela dalam dp -> kelas lebar. */
fun widthClassFor(widthDp: Int): WidthClass = when {
    widthDp >= 840 -> WidthClass.Expanded
    widthDp >= 600 -> WidthClass.Medium
    else -> WidthClass.Compact
}

@Composable
fun widthClass(): WidthClass = widthClassFor(LocalConfiguration.current.screenWidthDp)

/** Lebar maksimum konten bacaan/form (Profil, Pengaturan, Unduhan, Login, dst.). */
val ContentMaxW: Dp = 720.dp

/** Lebar maksimum grid poster; di atas ini grid dipusatkan agar poster tidak membengkak. */
val GridMaxW: Dp = 1200.dp

/** Lebar maksimum bilah tab bawah; ikon tidak ditarik sepanjang layar tablet. */
val TabBarMaxW: Dp = 560.dp

/** Margin samping halaman: 16 dp ponsel, 24 dp medium, 32 dp expanded. */
@Composable
fun pageSidePad(): Dp = when (widthClass()) {
    WidthClass.Compact -> Sp.S16
    WidthClass.Medium -> Sp.S24
    WidthClass.Expanded -> Sp.S32
}

/** Lebar minimum satu sel poster pada grid adaptif. */
@Composable
fun gridCellMin(): Dp = when (widthClass()) {
    WidthClass.Compact -> 100.dp
    WidthClass.Medium -> 128.dp
    WidthClass.Expanded -> 148.dp
}

/**
 * Menjauhkan konten dari lubang kamera (display cutout) dan bilah navigasi yang berada di sisi
 * kiri/kanan saat landscape. Hanya sisi horizontal; sisi atas/bawah diurus Page/TabBar sendiri.
 */
@Composable
fun Modifier.safeSides(): Modifier =
    windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
