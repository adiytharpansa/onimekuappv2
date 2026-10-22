package com.onimeku.app.ui

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.onimeku.app.PlaybackService
import com.onimeku.app.data.CrashLog

/** Aksi navigasi episode yang dipajang ke notifikasi / layar kunci (tombol Sebelumnya / Berikutnya). */
internal class EpisodeNav(
    val hasPrev: Boolean,
    val hasNext: Boolean,
    val onPrev: () -> Unit,
    val onNext: () -> Unit,
) {
    companion object {
        val NONE = EpisodeNav(false, false, {}, {})
    }
}

/**
 * Pembungkus untuk MediaSession: satu item media saja tidak menampilkan tombol Next/Prev di notifikasi,
 * jadi perintahnya diiklankan di sini dan dialihkan ke navigasi episode aplikasi.
 */
private class EpisodeNavPlayer(player: Player, private val nav: EpisodeNav) : ForwardingPlayer(player) {
    override fun getAvailableCommands(): Player.Commands {
        val b = super.getAvailableCommands().buildUpon()
        if (nav.hasPrev) b.addAll(Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        else b.removeAll(Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        if (nav.hasNext) b.addAll(Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        else b.removeAll(Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        return b.build()
    }

    override fun isCommandAvailable(command: Int): Boolean = getAvailableCommands().contains(command)
    override fun hasPreviousMediaItem(): Boolean = nav.hasPrev
    override fun hasNextMediaItem(): Boolean = nav.hasNext
    override fun seekToPrevious() { if (nav.hasPrev) nav.onPrev() }
    override fun seekToNext() { if (nav.hasNext) nav.onNext() }
    override fun seekToPreviousMediaItem() { if (nav.hasPrev) nav.onPrev() }
    override fun seekToNextMediaItem() { if (nav.hasNext) nav.onNext() }
}

/**
 * Pemilik tunggal ExoPlayer + MediaSession, hidup DI LUAR composition.
 *
 * Sebelumnya ExoPlayer dibuat di `remember(r, streamUrl)` dan dibuang tiap ganti episode / pintu gudang /
 * retry: layar hitam + buffering ulang tiap Next, dan sesi notifikasi dibongkar-pasang. Kini satu instance
 * dipakai ulang; PlayerReady hanya "meminjam" lewat token ([acquire]) dan menyetel MediaSource per episode.
 *
 * Pengambilalihan: composable episode baru memanggil [acquire] SEBELUM composable lama dibuang. Karena itu
 * [release] dari pemilik lama diabaikan bila sudah ada pemilik baru ([isOwner] false); instance baru dilepas
 * hanya saat pemilik terakhir pergi. Semua dipanggil dari main thread.
 */
internal object PlayerHolder {
    private var exo: ExoPlayer? = null
    private var session: MediaSession? = null
    private var owner: Any? = null
    private var releaseController: (() -> Unit)? = null

    fun acquire(ctx: Context, who: Any): ExoPlayer {
        owner = who
        val p = exo ?: create(ctx.applicationContext).also { exo = it }
        reset(p)
        return p
    }

    fun isOwner(who: Any): Boolean = owner === who

    /** Pasang aksi Next/Prev untuk notifikasi & layar kunci; diabaikan bila [who] bukan pemilik lagi. */
    fun setNav(who: Any, nav: EpisodeNav) {
        if (owner !== who) return
        val p = exo ?: return
        session?.setPlayer(EpisodeNavPlayer(p, nav))
    }

    fun release(who: Any) {
        if (owner !== who) return
        owner = null
        destroy()
    }

    private fun create(app: Context): ExoPlayer {
        // Mulai putar setelah 1,5 dtk terbuffer (bawaan 2,5), pulih dari rebuffer di 5 dtk, buffer maju 30-60 dtk,
        // sisakan 20 dtk ke belakang agar seek mundur / ketuk dua kali -10 dtk tidak mengunduh ulang segmen.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(30_000, 60_000, 1_500, 5_000)
            .setBackBuffer(20_000, true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        val renderers = DefaultRenderersFactory(app)
            .setEnableDecoderFallback(true) // dekoder utama gagal -> coba dekoder lain, bukan error
        val p = ExoPlayer.Builder(app, renderers)
            .setLoadControl(loadControl)
            // Fokus audio: jeda saat ada panggilan/aplikasi lain, lanjut otomatis setelahnya; jeda saat headset dicabut.
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        // Telemetri pemutar: transisi keadaan, galat pemutar, dan galat muat per-URL ke CrashLog (query URL disamarkan).
        p.addAnalyticsListener(object : androidx.media3.exoplayer.analytics.AnalyticsListener {
            override fun onPlaybackStateChanged(eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime, state: Int) {
                CrashLog.note("exo-state", "state=$state")
            }
            override fun onPlayerError(eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime, error: androidx.media3.common.PlaybackException) {
                CrashLog.note("exo-error", "${error.errorCodeName}: ${error.message} | cause=${error.cause}")
            }
            override fun onLoadError(eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime, loadEventInfo: androidx.media3.exoplayer.source.LoadEventInfo, mediaLoadData: androidx.media3.exoplayer.source.MediaLoadData, error: java.io.IOException, wasCanceled: Boolean) {
                CrashLog.note("exo-load", "${loadEventInfo.dataSpec.uri} -> ${error.javaClass.simpleName}: ${error.message}")
            }
        })
        // Sesi media + layanan latar: notifikasi, kontrol layar kunci, tombol headset.
        val s = MediaSession.Builder(app, EpisodeNavPlayer(p, EpisodeNav.NONE))
            .setId("onimeku-${System.nanoTime()}").build()
        session = s
        PlaybackService.current = s
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val fut = MediaController.Builder(app, token).buildAsync()
        releaseController = { MediaController.releaseFuture(fut) }
        return p
    }

    /** Bersihkan sisa episode sebelumnya supaya pemilik baru mulai dari keadaan bawaan. */
    private fun reset(p: ExoPlayer) {
        p.stop()
        p.clearMediaItems()
        p.setPlaybackSpeed(1f)
        // Resume dibaca persis (EXACT); PlayerReady melonggarkannya ke CLOSEST_SYNC saat pertama READY.
        p.setSeekParameters(SeekParameters.EXACT)
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .clearOverrides()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
            .setPreferredTextLanguage(null)
            .clearVideoSizeConstraints()
            .build()
        session?.setPlayer(EpisodeNavPlayer(p, EpisodeNav.NONE))
    }

    private fun destroy() {
        releaseController?.invoke()
        releaseController = null
        val s = session
        if (s != null && PlaybackService.current === s) PlaybackService.current = null
        s?.release()
        session = null
        exo?.release()
        exo = null
    }
}
