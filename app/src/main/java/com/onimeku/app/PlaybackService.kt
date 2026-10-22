package com.onimeku.app

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Layanan sesi media: memberi notifikasi + kontrol layar kunci dan menjaga
 * proses tetap hidup saat memutar di latar. Sesi dibuat oleh PlayerScreen
 * (pemilik ExoPlayer) dan didaftarkan lewat [current]; layanan hanya
 * menyerahkannya ke pengendali (notifikasi sistem) yang terhubung.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    override fun onCreate() {
        super.onCreate()
        // Ikon kecil + nama kanal sendiri (bukan bawaan Media3).
        val provider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelName(R.string.playback_channel_name)
            .build()
        provider.setSmallIcon(R.drawable.ic_notify_play)
        setMediaNotificationProvider(provider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        if (controllerInfo.packageName == packageName || controllerInfo.isTrusted) current else null

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        // Pengguna menggeser aplikasi dari recents: hentikan, jangan memutar tanpa UI.
        // (Pemutar toh dilepas saat PlayerReady dibongkar, jadi pemutaran tidak berlanjut tanpa UI.)
        current?.player?.pause()
        stopSelf()
    }

    companion object {
        @Volatile var current: MediaSession? = null
    }
}
