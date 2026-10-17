package com.onimeku.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Brand palette ────────────────────────────────────────────────────────────
val Crimson = Color(0xFFE11D2E)      // aksen utama
val CrimsonSoft = Color(0xFFFF5C6C)
val DeepRed = Color(0xFF7A0C1B)      // merah tua
val Wine = Color(0xFF3A0A12)
val LogoRed = Color(0xFFF20A16)

val Bg = Color(0xFF0F0A0B)
val Surface1 = Color(0xFF191214)
val Surface2 = Color(0xFF241A1D)
val Surface3 = Color(0xFF2F2326)

val Glass = Color(0xE6191113)        // menu bawah melayang
val Gold = Color(0xFFFFC107)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB8A9AC)
val TextTertiary = Color(0xFF8C7B7F)
val Stroke = Color(0x1FFFFFFF)

// ── Design tokens ────────────────────────────────────────────────────────────
/** Skala jarak 4dp agar ritme layout konsisten di semua layar. */
object Space {
    val xxs = 4.dp
    val xs = 6.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val huge = 32.dp
}

/** Radius sudut standar: kartu 16–20, chip/aksi bulat penuh. */
object Radius {
    val sm = 10.dp
    val md = 14.dp
    val lg = 18.dp
    val xl = 22.dp
    val sheet = 28.dp
    val full = 999.dp
}

private val Scheme = darkColorScheme(
    primary = Crimson,
    onPrimary = Color.White,
    primaryContainer = DeepRed,
    onPrimaryContainer = Color.White,
    secondary = CrimsonSoft,
    onSecondary = Color.White,
    tertiary = Gold,
    onTertiary = Color.Black,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Surface3,
    outline = Color(0xFF4A3A3E),
    outlineVariant = Stroke,
    error = Color(0xFFFF6B6B),
    onError = Color.Black
)

private val Typo = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Aplikasi streaming → tema gelap konsisten (tidak mengikuti sistem). */
@Composable
fun OnimekuTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Scheme, typography = Typo, shapes = AppShapes, content = content)
