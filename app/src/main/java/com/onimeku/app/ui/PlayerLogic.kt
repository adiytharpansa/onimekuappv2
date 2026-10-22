package com.onimeku.app.ui

import androidx.media3.common.PlaybackException

/** Langkah berikutnya di rantai cadangan putar setelah galat fatal. */
internal sealed interface ChainStep {
    /** Jalur utama (proksi) dicoba sekali lagi. */
    object RetryMain : ChainStep
    /** Pindah ke URL langsung CDN. */
    object UseDirect : ChainStep
    /** Gudang: pindah ke pintu berikutnya. */
    data class NextDoor(val idx: Int) : ChainStep
    /** Rantai habis: tampilkan galat jujur + tombol coba lagi. */
    object Dead : ChainStep
}

/** Logika murni rantai cadangan putar (dipisah dari Compose supaya bisa diuji unit). */
internal object PlaybackChain {
    /**
     * Gudang ([doorCount] > 0): pintu berikutnya, maksimal satu putaran penuh. Selain itu: ulang jalur utama sekali ->
     * URL langsung (bila ada) -> habis.
     */
    fun onFatal(doorCount: Int, doorIdx: Int, useDirect: Boolean, mainRetry: Int, hasDirect: Boolean): ChainStep = when {
        doorCount > 0 -> if (doorIdx < doorCount - 1) ChainStep.NextDoor(doorIdx + 1) else ChainStep.Dead
        !useDirect && mainRetry == 0 -> ChainStep.RetryMain
        !useDirect && hasDirect -> ChainStep.UseDirect
        else -> ChainStep.Dead
    }

    /**
     * Galat jaringan sesaat yang layak dicoba lanjutkan di tempat: koneksi gagal/timeout, IO tak spesifik, dan segmen
     * 5xx/429. 4xx lain (403/404: token/segmen mati) langsung ke rantai cadangan.
     */
    fun isTransient(errorCode: Int, httpCode: Int?): Boolean =
        errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
            errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
            errorCode == PlaybackException.ERROR_CODE_TIMEOUT ||
            (errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS && httpCode != null && (httpCode >= 500 || httpCode == 429))
}

/** Pengawas macet: jeda antar siklus dan kapan kualitas diturunkan. */
internal object StallPolicy {
    fun delayMs(tries: Int): Long = if (tries == 0) 12_000L else 15_000L
    fun shouldLower(tries: Int, lowerHeight: Int?): Boolean = tries >= 2 && lowerHeight != null
}

/** Hitungan nomor episode (murni). */
internal object EpisodeMath {
    /**
     * Episode berikutnya. Bila daftar episode tersedia diketahui, HANYA yang ada di daftar yang dihitung (dulu jatuh ke
     * nomor+1 sehingga autoplay bisa mencoba episode yang belum tayang). Tanpa daftar: nomor+1 sampai total.
     */
    fun next(available: List<Int>, total: Int, cur: Int): Int? =
        if (available.isNotEmpty()) available.firstOrNull { it > cur }
        else if (cur < total) cur + 1 else null

    /** Geser [d] langkah (+1 / -1) dari [cur]; null bila di ujung. */
    fun step(available: List<Int>, total: Int, cur: Int, d: Int): Int? {
        val nums = if (available.isNotEmpty()) available else (1..total).toList()
        val i = nums.indexOf(cur)
        return if (i >= 0) nums.getOrNull(i + d) else if (cur + d in 1..total) cur + d else null
    }

    /** Tawarkan "Melanjutkan dari ..." bila episode ini pernah ditonton >= 30 dtk. */
    fun offerResume(savedSec: Int): Boolean = savedSec >= 30
}

/** Logika lewati intro. */
internal object SkipLogic {
    /** Lompatan tombol cadangan bila API tak memberi penanda intro (durasi lagu pembuka anime ±90 dtk). */
    const val FALLBACK_SEC = 85

    /** Tombol cadangan hanya untuk episode penuh (>= 10 mnt) pada awal tayangan, tanpa penanda dari API. */
    fun fallbackVisible(posSec: Int, hasIntroMarker: Boolean, enabled: Boolean, durSec: Int): Boolean =
        enabled && !hasIntroMarker && durSec >= 600 && posSec in 20..240

    /** Tujuan lompat otomatis (detik) bila posisi sedang di dalam intro dan opsi aktif; selain itu null. */
    fun autoSkipTarget(posSec: Int, introStart: Int?, introEnd: Int?, enabled: Boolean, auto: Boolean): Int? {
        if (!enabled || !auto || introEnd == null || introEnd <= 0) return null
        val start = introStart ?: 0
        return if (posSec >= start && posSec < introEnd - 1) introEnd else null
    }
}
