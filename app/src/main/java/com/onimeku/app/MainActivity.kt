package com.onimeku.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.onimeku.app.ui.OnimekuApp
import com.onimeku.app.ui.theme.OnimekuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Tema aplikasi selalu gelap → paksa ikon status/navigation bar terang,
        // agar tidak hitam di atas latar gelap saat sistem memakai mode terang.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent { OnimekuTheme { OnimekuApp() } }
    }
}
