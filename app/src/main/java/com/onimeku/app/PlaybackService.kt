package com.onimeku.app

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Layanan sesi media: memberi notifikasi + kontrol layar kunci dan menjaga
 * proses tetap hidup saat memutar di latar. Sesi dibuat oleh PlayerScreen
 * (pemilik ExoPlayer) dan didaftarkan lewat [current]; layanan hanya
 * menyerahkannya ke pengendali (notifikasi sistem) yang terhubung.
 */
class PlaybackService : MediaSessionService() {
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        if (controllerInfo.packageName == packageName || controllerInfo.isTrusted) current else null

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        // Pengguna menggeser aplikasi dari recents: hentikan, jangan memutar tanpa UI.
        current?.player?.pause()
        stopSelf()
    }

    companion object {
        @Volatile var current: MediaSession? = null
    }
}
