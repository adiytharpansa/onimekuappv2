package com.onimeku.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Crimson = Color(0xFFE11D2E)      // aksen utama
val DeepRed = Color(0xFF7A0C1B)      // merah tua
val Wine = Color(0xFF3A0A12)
val Bg = Color(0xFF0F0A0B)
val Surface1 = Color(0xFF191214)
val Surface2 = Color(0xFF241A1D)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB8A9AC)

private val Scheme = darkColorScheme(
    primary = Crimson, onPrimary = Color.White,
    primaryContainer = DeepRed, onPrimaryContainer = Color.White,
    background = Bg, onBackground = TextPrimary,
    surface = Surface1, onSurface = TextPrimary,
    surfaceVariant = Surface2, onSurfaceVariant = TextSecondary,
    outline = Color(0xFF4A3A3E)
)

private val Typo = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
)

/** Aplikasi streaming → tema gelap konsisten (tidak mengikuti sistem). */
@Composable
fun OnimekuTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Scheme, typography = Typo, content = content)

val Surface3 = Color(0xFF2F2326)
val Glass = Color(0xE6191113)
val Gold = Color(0xFFFFC107)

val LogoRed = Color(0xFFF20A16)
