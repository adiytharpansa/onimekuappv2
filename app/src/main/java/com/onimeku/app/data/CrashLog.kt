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

    fun note(tag: String, msg: String) {
        val f = file ?: return
        synchronized(lock) {
            try {
                val line = "${java.time.Instant.now()} [$tag] $msg\n"
                var old = if (f.exists()) f.readText() else ""
                if (old.length + line.length > MAX) old = old.takeLast(MAX / 2)
                f.writeText(old + line)
            } catch (_: Exception) {}
        }
    }

    fun read(): String {
        val head = "Onimeku ${com.onimeku.app.BuildConfig.VERSION_NAME} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}\n\n"
        val f = file
        return head + (if (f != null && f.exists()) f.readText().takeLast(20_000) else "(no entries)")
    }
}
