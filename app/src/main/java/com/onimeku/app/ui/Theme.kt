package com.onimeku.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
// Emas untuk skor/rating (satu-satunya aksen hangat; jangan dipakai untuk tombol).
val Gold = Color(0xFFFFC857)
val Solid = Color(0xFF2B2E3B)

// Token hero Beranda (sebelumnya angka tersebar di Screens.kt).
val HeroMeta = Color(0xFFD5D8E3)
val HeroSynopsis = Color(0xFFD0D3DF)   // lebih terang: kontras sinopsis di atas gambar ramai
val ChipGlass = Color(0x99080910)       // latar chip rating/status di hero (kaca gelap, terbaca di gambar terang)
val GlassFill = Color(0x59080910)
val GlassStroke = Color(0x66FFFFFF)
val HeroPad = 20.dp
val HeroBottomPad = 44.dp   // jarak tombol Play ke tepi bawah hero
val HeroActionW = 56.dp     // lebar/tinggi tombol "+" (Daftarku) di hero
val HeroActionGap = 12.dp   // jarak antara Play dan "+"
val HeroDotsGap = 11.dp     // titik indikator: jarak dari sisi atas tombol "+" ke dasar titik
val HeroActionsTop = 28.dp  // ruang antara sinopsis dan baris tombol (menampung titik di atas "+")
const val HeroOverscan = 1.2f   // gambar diperbesar 20% sebagai ruang parallax
const val HeroParallax = 0.1f   // geser gambar = 10% lebar halaman (<= (HeroOverscan-1)/2)

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
