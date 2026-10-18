package com.onimeku.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF14161F)
val Bg2 = Color(0xFF1B1E2A)
val InkSoft = Color(0xFFC9CCD8)
val Red = Color(0xFFE50914)
val RedText = Color(0xFFFF4452)
val Line = Color(0xFF2A2D3B)
val NavOff = Color(0xFF9EA4B8)
val Solid = Color(0xFF2B2E3B)

@Composable
fun OnimekuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Red, onPrimary = Color.White,
            background = Bg, onBackground = Color.White,
            surface = Bg2, onSurface = Color.White,
        ),
        content = content,
    )
}
