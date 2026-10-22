package com.onimeku.app.data

import android.content.Context
import android.os.Build
import java.io.File

/**
 * Pencatat galat lokal (tanpa pihak ketiga, tanpa data pribadi).
 * - Crash tak tertangani + galat pemutar ditulis ke filesDir/diagnostics.log (maks ±64 KB, bagian lama dibuang).
 * - Pengguna bisa membagikan log dari About > Send diagnostics.
 * Untuk pelaporan otomatis ke server (Crashlytics/Sentry) diperlukan akun layanan tersebut.
 */
object CrashLog {
    private const val MAX = 64 * 1024
    @Volatile private var file: File? = null
    private val lock = Any()

    fun install(ctx: Context) {
        val app = ctx.applicationContext
        file = File(app.filesDir, "diagnostics.log")
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { note("CRASH", "thread=${t.name}\n${android.util.Log.getStackTraceString(e)}") } catch (_: Throwable) {}
            prev?.uncaughtException(t, e)
        }
    }

    // Antrean baris + satu thread penulis. Dulu note() membaca lalu menulis ulang berkas (hingga 64 KB) SINKRON di thread
    // pemanggil; listener ExoPlayer memanggilnya di main thread tiap perubahan state/galat muat -> jeda saat pemutar dibuka.
    private val queue = java.util.concurrent.ConcurrentLinkedQueue<String>()
    private val io: java.util.concurrent.ExecutorService by lazy {
        java.util.concurrent.Executors.newSingleThreadExecutor { r -> Thread(r, "onimeku-log").apply { isDaemon = true } }
    }

    private fun flush(f: File) {
        synchronized(lock) {
            val sb = StringBuilder()
            while (true) sb.append(queue.poll() ?: break)
            if (sb.isEmpty()) return
            try {
                if (f.exists() && f.length() + sb.length > MAX) f.writeText(f.readText().takeLast(MAX / 2) + sb)
                else f.appendText(sb.toString())
            } catch (_: Exception) {}
        }
    }

    fun note(tag: String, msg: String) {
        val f = file ?: return
        queue.add("${java.time.Instant.now()} [$tag] ${LogRedact.urls(msg)}\n")
        // CRASH ditulis sinkron (proses akan mati); catatan lain di thread latar.
        if (tag == "CRASH") flush(f) else io.execute { flush(f) }
    }

    fun read(): String {
        file?.let { flush(it) }
        val head = "Onimeku ${com.onimeku.app.BuildConfig.VERSION_NAME} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}\n\n"
        val f = file
        return head + (if (f != null && f.exists()) LogRedact.urls(f.readText().takeLast(20_000)) else "(no entries)")
    }
}
