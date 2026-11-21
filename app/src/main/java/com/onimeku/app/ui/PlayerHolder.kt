package com.onimeku.app.ui

import android.content.Context
import android.view.Surface
import com.onimeku.app.data.CrashLog
import com.onimeku.app.data.Net
import dev.jdtech.mpv.MPVLib

/** Satu trek dari daftar trek mpv (`track-list`). */
internal data class MpvTrack(
    val id: Int,
    val type: String, // "video" | "audio" | "sub"
    val lang: String,
    val title: String,
    val external: Boolean,
    val selected: Boolean,
    val width: Int,
    val height: Int,
)

/**
 * Mesin putar tunggal aplikasi: mpv lewat libmpv. Dipakai untuk streaming
 * (playlist HLS dari gudang/sumber lain) DAN luring (playlist lokal hasil
 * unduhan berbasis berkas di data/Downloads.kt). Tidak ada mesin kedua.
 *
 * Semua panggilan dibungkus runCatching: properti mpv bisa tidak tersedia
 * sesaat (belum ada berkas dimuat, trek hilang), dan kegagalan satu panggilan
 * tidak boleh menjatuhkan aplikasi.
 */
internal class MpvEngine private constructor(val mpv: MPVLib) {

    /** URL yang terakhir diminta dimuat (untuk muat ulang ganti kualitas). */
    @Volatile var currentUrl: String? = null
        private set

    /** Posisi (ms) yang harus diterapkan begitu berkas selesai dimuat; -1 = tidak ada. */
    @Volatile var seekAfterLoadMs: Long = -1L

    fun load(url: String, referer: String?) {
        currentUrl = url
        runCatching {
            if (!referer.isNullOrBlank() && url.startsWith("http")) {
                mpv.setPropertyString("http-header-fields", "Referer: $referer")
            } else {
                mpv.setPropertyString("http-header-fields", "")
            }
        }
        runCatching { mpv.command(arrayOf("loadfile", url)) }
    }

    /** Muat ulang URL yang sama (dipakai sesudah batas bitrate kualitas diganti). */
    fun reload() {
        val u = currentUrl ?: return
        seekAfterLoadMs = positionMs()
        runCatching { mpv.command(arrayOf("loadfile", u)) }
    }

    fun stop() {
        currentUrl = null
        runCatching { mpv.command(arrayOf("stop")) }
    }

    fun setPaused(paused: Boolean) {
        runCatching { mpv.setPropertyBoolean("pause", paused) }
    }

    fun isPaused(): Boolean =
        runCatching { mpv.getPropertyBoolean("pause") }.getOrNull() ?: true

    /** Posisi putar sekarang dalam milidetik (0 bila belum ada). */
    fun positionMs(): Long =
        (runCatching { mpv.getPropertyDouble("time-pos") }.getOrNull() ?: 0.0)
            .times(1000).toLong().coerceAtLeast(0)

    fun durationMs(): Long =
        (runCatching { mpv.getPropertyDouble("duration") }.getOrNull() ?: 0.0)
            .times(1000).toLong().coerceAtLeast(0)

    /** Akhir rentang yang sudah ter-buffer (ms): posisi + sisa cache demuxer. */
    fun bufferedEndMs(): Long {
        val pos = runCatching { mpv.getPropertyDouble("time-pos") }.getOrNull() ?: 0.0
        val cache = runCatching { mpv.getPropertyDouble("demuxer-cache-time") }.getOrNull() ?: 0.0
        return ((pos + cache) * 1000).toLong().coerceAtLeast(0)
    }

    fun seekToMs(ms: Long) {
        runCatching { mpv.command(arrayOf("seek", (ms / 1000.0).toString(), "absolute")) }
    }

    fun setSpeed(v: Double) {
        runCatching { mpv.setPropertyDouble("speed", v) }
    }

    /** Pilih trek subtitle berdasarkan id mpv; null = matikan subtitle. */
    fun setSid(id: Int?) {
        runCatching { mpv.setPropertyString("sid", id?.toString() ?: "no") }
    }

    /** Pilih trek audio berdasarkan id mpv; null = matikan audio. */
    fun setAid(id: Int?) {
        runCatching { mpv.setPropertyString("aid", id?.toString() ?: "no") }
    }

    /** Tambahkan subtitle samping (berkas lokal/URL) sebagai trek eksternal. */
    fun addSideSub(url: String) {
        runCatching { mpv.command(arrayOf("sub-add", url, "auto")) }
    }

    /** Daftar trek saat ini; defensif — trek bisa hilang di tengah jalan. */
    fun tracks(): List<MpvTrack> {
        val count = runCatching { mpv.getPropertyInt("track-list/count") }.getOrNull() ?: return emptyList()
        val out = ArrayList<MpvTrack>(count)
        for (i in 0 until count) {
            val type = runCatching { mpv.getPropertyString("track-list/$i/type") }.getOrNull() ?: continue
            val id = runCatching { mpv.getPropertyInt("track-list/$i/id") }.getOrNull() ?: continue
            out += MpvTrack(
                id = id,
                type = type,
                lang = runCatching { mpv.getPropertyString("track-list/$i/lang") }.getOrNull() ?: "",
                title = runCatching { mpv.getPropertyString("track-list/$i/title") }.getOrNull() ?: "",
                external = runCatching { mpv.getPropertyBoolean("track-list/$i/external") }.getOrNull() ?: false,
                selected = runCatching { mpv.getPropertyBoolean("track-list/$i/selected") }.getOrNull() ?: false,
                width = runCatching { mpv.getPropertyInt("track-list/$i/demux-w") }.getOrNull() ?: 0,
                height = runCatching { mpv.getPropertyInt("track-list/$i/demux-h") }.getOrNull() ?: 0,
            )
        }
        return out
    }

    /**
     * Petakan pilihan kualitas ke batas bitrate HLS mpv (`hls-bitrate`):
     * mpv memilih varian tertinggi yang bitrate-nya di bawah batas ini.
     * null = "Auto" (tanpa batas praktis).
     */
    fun setQualityCap(height: Int?) {
        val bps = when (height) {
            null -> 2_147_483_647
            else -> when {
                height >= 1080 -> 12_000_000
                height >= 720 -> 6_000_000
                height >= 480 -> 3_000_000
                height >= 360 -> 1_500_000
                else -> 800_000
            }
        }
        runCatching { mpv.setPropertyInt("hls-bitrate", bps) }
    }

    fun setSubDelay(ms: Int) {
        runCatching { mpv.setPropertyDouble("sub-delay", ms / 1000.0) }
    }

    /** Mode audio-saja / putar latar: matikan trek video (hemat baterai & data). */
    fun setVideoEnabled(on: Boolean) {
        runCatching { mpv.setPropertyString("vid", if (on) "auto" else "no") }
    }

    /** Isi-layar (crop) vs pas-layar, mengikuti zoom mpv `panscan`. */
    fun setPanscan(fill: Boolean) {
        runCatching { mpv.setPropertyDouble("panscan", if (fill) 1.0 else 0.0) }
    }

    /**
     * Gaya subtitle lewat properti mpv (libass menggambarnya langsung).
     * [system] = pakai gaya bawaan mpv sepenuhnya (pengaturan "Ikuti sistem").
     */
    fun applySubStyle(scale: Float, colorHex: String, bg: Boolean, system: Boolean) {
        if (system) return
        runCatching {
            mpv.setPropertyDouble("sub-scale", scale.toDouble())
            mpv.setPropertyString("sub-color", colorHex)
            mpv.setPropertyString("sub-bold", "yes")
            if (bg) {
                mpv.setPropertyString("sub-border-style", "background-box")
                mpv.setPropertyString("sub-back-color", "#B3000000")
            } else {
                mpv.setPropertyString("sub-border-style", "outline-and-shadow")
                mpv.setPropertyString("sub-border-size", "2")
                mpv.setPropertyString("sub-shadow-offset", "1")
            }
        }
    }

    /** Angkat subtitle saat kontrol tampil agar tidak tertutup panel bawah. */
    fun setSubLifted(lifted: Boolean) {
        runCatching { mpv.setPropertyInt("sub-pos", if (lifted) 88 else 100) }
    }

    fun attachSurface(surface: Surface) {
        runCatching { mpv.attachSurface(surface) }
    }

    fun setSurfaceSize(w: Int, h: Int) {
        runCatching { mpv.setPropertyString("android-surface-size", "${w}x$h") }
    }

    fun detachSurface() {
        runCatching { mpv.detachSurface() }
    }

    fun destroy() {
        runCatching { mpv.destroy() }
    }

    companion object {
        fun create(ctx: Context): MpvEngine? {
            val m = MPVLib.create(ctx.applicationContext) ?: return null
            return runCatching {
                m.setOptionString("vo", "gpu")
                m.setOptionString("gpu-context", "android")
                m.setOptionString("hwdec", "mediacodec")
                // mpv tidak membaca trust store Android; tanpa ini sebagian
                // pintu CDN gagal jabat tangan TLS (pola Kitsunime/Anime.id).
                m.setOptionString("tls-verify", "no")
                m.setOptionString("cache", "yes")
                // Segmen gudang besar (±14 MB); cache demuxer diperbesar agar
                // putar tidak tersendat di sambungan lambat (pelajaran Anime.id).
                m.setOptionString("demuxer-max-bytes", "128MiB")
                m.setOptionString("demuxer-max-back-bytes", "32MiB")
                m.setOptionString("user-agent", Net.UA)
                // Utamakan subtitle Indonesia lalu Inggris dari trek bawaan
                // playlist induk; pilihan persisnya tetap diatur sid sesudah muat.
                m.setOptionString("slang", "id,ind,en,eng")
                m.init()
                MpvEngine(m)
            }.getOrElse {
                CrashLog.note("mpv-create", "${it.javaClass.simpleName}: ${it.message}")
                runCatching { m.destroy() }
                null
            }
        }
    }
}

/**
 * Pemilik tunggal [MpvEngine] di luar composition: layar putar bisa disusun
 * ulang (putar layar, ganti episode) tanpa membunuh mesin. Kepemilikan diatur
 * token — pemilik baru mengambil alih, pelepasan dari pemilik lama yang sudah
 * tergeser diabaikan (semantik yang sama seperti pemegang ExoPlayer dulu).
 *
 * MediaSession/PlaybackService lama sudah dibuang: tidak ada lagi kontrol
 * notifikasi/layar kunci; pemutaran memang tidak pernah berlanjut tanpa UI
 * (komentar di PlaybackService lama menyatakannya sendiri).
 */
internal object PlayerHolder {
    private var engine: MpvEngine? = null
    private var owner: Any? = null

    @Synchronized
    fun acquire(ctx: Context, token: Any): MpvEngine? {
        val cur = engine
        if (cur != null && owner === token) return cur
        val e = cur ?: MpvEngine.create(ctx) ?: return null
        engine = e
        owner = token
        // Keadaan bersih untuk episode baru: berhenti, kecepatan normal,
        // tanpa geseran subtitle, tanpa batas kualitas, video menyala.
        e.stop()
        e.setSpeed(1.0)
        e.setSubDelay(0)
        e.setQualityCap(null)
        e.setVideoEnabled(true)
        return e
    }

    @Synchronized
    fun isOwner(token: Any): Boolean = owner === token

    @Synchronized
    fun release(token: Any) {
        if (owner !== token) return
        owner = null
        engine?.stop()
    }
}
