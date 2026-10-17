package com.onimeku.app.media

import android.app.Notification
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.onimeku.app.R

/** Pemutar hidup di service: kontrol di notifikasi/lock screen + lanjut di latar belakang. */
@UnstableApi
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(MediaKit.dataSource(this)))
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true).build()
        session = MediaSession.Builder(this, player).build()
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session
    override fun onDestroy() { session?.run { player.release(); release() }; session = null; super.onDestroy() }
}

/** Unduhan offline HLS di foreground service (patuh Wi-Fi only lewat Requirements). */
@UnstableApi
class OnimekuDownloadService : DownloadService(1, DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL, MediaKit.CHANNEL, R.string.downloads, 0) {
    override fun getDownloadManager(): DownloadManager = MediaKit.downloads(this)
    override fun getScheduler(): Scheduler? = null
    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification =
        DownloadNotificationHelper(this, MediaKit.CHANNEL).buildProgressNotification(
            this, android.R.drawable.stat_sys_download, null, null, downloads, notMetRequirements)
}
