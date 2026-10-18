package com.onimeku.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF14161F)
val Bg2 = Color(0xFF1B1E2A)
val InkSoft = Color(0xFFC9CCD8)
// Palet merah tua modern (crimson): Red untuk tombol/indikator, RedText lebih terang
// supaya teks & ikon merah tetap terbaca di latar gelap (kontras >= 4.5:1).
val Red = Color(0xFFA6122D)
val RedDeep = Color(0xFF6E0F1F)
val RedText = Color(0xFFE8566F)
// Merah karya pemilik untuk LOGO di semua tempat (aturan 16 Okt 2026);
// tema antarmuka tetap crimson Red di atas (debug.31: logo dikembalikan
// dari #A6122D ke #F20A17 sesuai karya aslinya).
val LogoRed = Color(0xFFF20A17)
val Line = Color(0xFF2A2D3B)
val NavOff = Color(0xFF9EA4B8)
val Solid = Color(0xFF2B2E3B)

@Composable
fun OnimekuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Red, onPrimary = Color.White, secondary = RedText, error = RedText,
            background = Bg, onBackground = Color.White,
            surface = Bg2, onSurface = Color.White,
        ),
        content = content,
    )
}
