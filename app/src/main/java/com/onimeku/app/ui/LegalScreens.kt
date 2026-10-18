package com.onimeku.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Sec(val h: String, val body: String)

// CATATAN: teks ini TEMPLATE yang disesuaikan dengan perilaku aplikasi.
// Tinjau oleh penasihat hukum dan isi kontak resmi sebelum rilis publik.
private val terms = listOf(
    Sec("Using Onimeku", "Onimeku lets you browse and stream anime and keep a personal list. You must follow these terms and the laws that apply to you. Do not misuse the app, try to disrupt it, or copy its content in bulk."),
    Sec("Accounts", "An account is optional. If you create one, keep your password private. You are responsible for activity under your account. You can stop using the app at any time."),
    Sec("Content", "Titles, images and videos shown in Onimeku belong to their respective owners. Availability can change without notice, and some titles may not be available in every region."),
    Sec("Availability", "We work to keep Onimeku running but cannot promise it will always be available or error-free. Features may change or be removed."),
    Sec("Changes", "We may update these terms. Continuing to use the app after an update means you accept the new terms."),
)

private val privacy = listOf(
    Sec("What stays on your device", "Your profiles, My List, watch progress, search history, reminders, settings and offline downloads are stored on your device."),
    Sec("Account data", "If you create an account, your email address and an encrypted password are handled by our authentication provider (Supabase) so you can sign in. We do not see your plain password."),
    Sec("Network requests", "To show the catalog and play episodes, the app contacts our API and streaming hosts. These servers may receive technical data such as your IP address and device type, as with any internet service."),
    Sec("Notifications", "If you allow notifications, the app uses them for episode reminders and download progress. You can turn them off in Settings or in your device settings."),
    Sec("No ads, no selling data", "Onimeku does not show ads and does not sell your personal data."),
    Sec("Your choices", "You can clear your history and remove downloads inside the app, sign out at any time, and uninstall the app to remove local data. To ask about your account data, contact us through the Help Center."),
)

private val licenses = listOf(
    Sec("Jetpack Compose, Material 3, AndroidX", "Copyright The Android Open Source Project. Licensed under the Apache License 2.0."),
    Sec("Media3 (ExoPlayer)", "Copyright The Android Open Source Project. Licensed under the Apache License 2.0."),
    Sec("OkHttp", "Copyright Square, Inc. Licensed under the Apache License 2.0."),
    Sec("Coil", "Copyright Coil Contributors. Licensed under the Apache License 2.0."),
    Sec("kotlinx.serialization and Kotlin", "Copyright JetBrains s.r.o. Licensed under the Apache License 2.0."),
    Sec("WorkManager", "Copyright The Android Open Source Project. Licensed under the Apache License 2.0."),
)

@Composable
fun LegalContent(key: String) {
    val secs = when (key) { "terms" -> terms; "privacy" -> privacy; else -> licenses }
    Text(if (key == "licenses") "Onimeku is built with these open-source projects." else "Last updated: October 2026",
        Modifier.padding(start = 4.dp, bottom = 12.dp), color = InkSoft, fontSize = 13.sp)
    CardBox {
        secs.forEachIndexed { i, s ->
            if (i > 0) HLine()
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
                Text(s.h, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp)
                Text(s.body, Modifier.padding(top = 6.dp), color = InkSoft, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
    }
}
