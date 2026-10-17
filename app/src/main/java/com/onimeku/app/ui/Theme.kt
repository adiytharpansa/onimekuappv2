package com.onimeku.app.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Font aplikasi. Untuk memakai Inter / Plus Jakarta Sans: taruh file .ttf di app/src/main/res/font/
 * (nama huruf kecil, mis. inter_regular.ttf, inter_semibold.ttf, inter_bold.ttf), lalu ganti baris ini:
 * val AppFont = FontFamily(Font(R.font.inter_regular), Font(R.font.inter_semibold, FontWeight.SemiBold), Font(R.font.inter_bold, FontWeight.Bold))
 */
val AppFont: FontFamily = FontFamily.Default

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFE50914), onPrimary = Color.White,
    primaryContainer = Color(0xFF5C0A10), onPrimaryContainer = Color(0xFFFFDAD6),
    secondaryContainer = Color(0xFF2E3045), onSecondaryContainer = Color(0xFFE6E7F5),
    background = Color(0xFF0F1019), onBackground = Color(0xFFE6E7F0),
    surface = Color(0xFF1A1C28), onSurface = Color(0xFFE6E7F0),
    surfaceVariant = Color(0xFF242738), onSurfaceVariant = Color(0xFFA9ACC0),
    outline = Color(0xFF454963), outlineVariant = Color(0xFF2A2D3E),
    surfaceContainerLowest = Color(0xFF0B0C13), surfaceContainerLow = Color(0xFF151722), surfaceContainer = Color(0xFF1A1C28),
    surfaceContainerHigh = Color(0xFF222536), surfaceContainerHighest = Color(0xFF2B2E42),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFFE50914), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6), onPrimaryContainer = Color(0xFF410004),
    secondaryContainer = Color(0xFFE7E8F3), onSecondaryContainer = Color(0xFF1B1D2B),
    background = Color(0xFFF7F7FB), onBackground = Color(0xFF14151C),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF14151C),
    surfaceVariant = Color(0xFFECEDF5), onSurfaceVariant = Color(0xFF5A5D70),
    outline = Color(0xFF8B8EA3), outlineVariant = Color(0xFFDADCE8),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF4F5FA), surfaceContainer = Color(0xFFF1F2F8),
    surfaceContainerHigh = Color(0xFFECEDF5), surfaceContainerHighest = Color(0xFFE6E7F0),
)

private val base = Typography()
private val AppTypography = Typography(
    headlineLarge = base.headlineLarge.copy(fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp),
    headlineSmall = base.headlineSmall.copy(fontFamily = AppFont, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleLarge = base.titleLarge.copy(fontFamily = AppFont, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = AppFont, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFamily = AppFont),
    bodyMedium = base.bodyMedium.copy(fontFamily = AppFont),
    bodySmall = base.bodySmall.copy(fontFamily = AppFont),
    labelLarge = base.labelLarge.copy(fontFamily = AppFont, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = AppFont, fontWeight = FontWeight.Medium),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp),
)

/** mode: "Dark" | "Light" | "System" | "Dynamic" (Material You, Android 12+). */
@Composable
fun OnimekuTheme(mode: String, content: @Composable () -> Unit) {
    val sysDark = isSystemInDarkTheme(); val ctx = LocalContext.current
    val dark = when (mode) { "Light" -> false; "System", "Dynamic" -> sysDark; else -> true }
    val scheme = when {
        mode == "Dynamic" && Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> DarkScheme
        else -> LightScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        (view.context as? Activity)?.window?.let { w ->
            WindowCompat.getInsetsController(w, view).apply { isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark } }
    }
    MaterialTheme(colorScheme = scheme, shapes = AppShapes, typography = AppTypography, content = content)
}
