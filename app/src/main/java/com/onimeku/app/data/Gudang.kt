package com.onimeku.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------
// Gudang (salinan sendiri) — keadaan sisi aplikasi:
// - Pintu terakhir yang BERHASIL memutar disimpan (nama pintu),
//   menjadi pintu awal kunjungan berikutnya (pola pemutar V3).
// - Laporan penonton: dikirim ke API; bila gagal, disimpan ke
//   antrean lokal (berkas di filesDir) dan dicoba lagi nanti.
// - Subtitle luring: berkas en/id gudang yang diunduh bersama
//   episode tersimpan di filesDir/gudang-subs untuk putar offline.
// ---------------------------------------------------------------------------

object GudangStore {
    private const val PREFS = "gudang"
    private const val KEY_LAST_DOOR = "last_good_door"
    private const val QUEUE_FILE = "gudang_reports_queue.jsonl"
    private val scope = CoroutineScope(Dispatchers.IO)

    fun lastDoor(ctx: Context): String? =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_DOOR, null)?.takeIf { it.isNotBlank() }

    fun saveDoor(ctx: Context, name: String) {
        if (name.isBlank()) return
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_DOOR, name).apply()
    }

    /** Indeks awal pintu: pintu terakhir yang berhasil (bila ada di daftar), selain itu 0. */
    fun startIndex(ctx: Context, doors: List<ApiDoor>): Int {
        val last = lastDoor(ctx) ?: return 0
        val i = doors.indexOfFirst { it.name == last }
        return if (i >= 0) i else 0
    }

    /** Urutan percobaan pintu: mulai dari pintu terakhir yang berhasil, lalu sisa pintu sekali putaran. */
    fun orderedDoors(ctx: Context, doors: List<ApiDoor>): List<ApiDoor> {
        if (doors.size <= 1) return doors
        val start = startIndex(ctx, doors)
        return doors.drop(start) + doors.take(start)
    }

    // ---------------- Laporan penonton ----------------

    private fun queueFile(ctx: Context) = java.io.File(ctx.applicationContext.filesDir, QUEUE_FILE)

    private fun enqueue(ctx: Context, anilistId: Int?, slug: String, ep: Int, note: String) {
        try {
            val line = org.json.JSONObject()
                .put("anilistId", anilistId ?: org.json.JSONObject.NULL)
                .put("slug", slug)
                .put("episode", ep)
                .put("catatan", note)
                .toString()
            queueFile(ctx).appendText(line + "\n")
        } catch (e: Exception) { /* antrean lokal usaha terbaik */ }
    }

    /**
     * Laporkan masalah putar (fire-and-forget): kirim ke API
     * sekarang; gagal -> antrean lokal untuk dikirim ulang oleh
     * flushReports() pada kesempatan berikutnya.
     */
    fun report(ctx: Context, anilistId: Int?, slug: String, ep: Int, note: String) {
        val app = ctx.applicationContext
        scope.launch {
            val ok = ApiClient.sendReport(anilistId, slug, ep, note)
            if (!ok) enqueue(app, anilistId, slug, ep, note)
        }
    }

    /** Kirim ulang laporan yang tertahan di antrean lokal. */
    fun flushReports(ctx: Context) {
        val app = ctx.applicationContext
        scope.launch {
            val f = queueFile(app)
            if (!f.exists()) return@launch
            val lines = try { f.readLines().filter { it.isNotBlank() } } catch (e: Exception) { return@launch }
            if (lines.isEmpty()) { f.delete(); return@launch }
            val remaining = ArrayList<String>()
            for (line in lines) {
                val ok = try {
                    val o = org.json.JSONObject(line)
                    ApiClient.sendReport(
                        if (o.isNull("anilistId")) null else o.optInt("anilistId"),
                        o.optString("slug"), o.optInt("episode"), o.optString("catatan"),
                    )
                } catch (e: Exception) { false }
                if (!ok) remaining.add(line)
            }
            try {
                if (remaining.isEmpty()) f.delete() else f.writeText(remaining.joinToString("\n") + "\n")
            } catch (e: Exception) { /* abaikan */ }
        }
    }

    // ---------------- Subtitle luring gudang ----------------

    private fun subsDir(ctx: Context) =
        java.io.File(ctx.applicationContext.filesDir, "gudang-subs").apply { mkdirs() }

    private fun subFile(ctx: Context, id: String, ep: Int, lang: String): java.io.File {
        val safe = "${id}_${ep}_$lang".replace(Regex("[^A-Za-z0-9._-]"), "_")
        return java.io.File(subsDir(ctx), "$safe.vtt")
    }

    /**
     * Unduh subtitle gudang (en/id) untuk putar luring episode ini.
     * Mengambil dari pintu yang sama dengan unduhan videonya.
     * Usaha terbaik — kegagalan tidak menggagalkan unduhan video.
     */
    suspend fun downloadSubs(ctx: Context, id: String, ep: Int, door: ApiDownloadDoor) {
        withContext(Dispatchers.IO) {
            val jobs = listOf("en" to door.subsEn, "id" to door.subsId)
            for ((lang, url) in jobs) {
                if (url.isNullOrBlank()) continue
                try {
                    val req = okhttp3.Request.Builder().url(url)
                        .header("User-Agent", Net.UA).build()
                    Net.client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@use
                        val bytes = resp.body?.bytes() ?: return@use
                        if (bytes.isNotEmpty()) subFile(ctx, id, ep, lang).writeBytes(bytes)
                    }
                } catch (e: Exception) { /* abaikan per trek */ }
            }
        }
    }

    /** Subtitle luring yang tersimpan untuk episode ini (untuk putar offline). */
    fun offlineSubs(ctx: Context, id: String, ep: Int): List<ApiSubtitle> {
        val out = ArrayList<ApiSubtitle>()
        for ((lang, label) in listOf("en" to "English", "id" to "Indonesian")) {
            val f = subFile(ctx, id, ep, lang)
            if (f.exists() && f.length() > 0) {
                out += ApiSubtitle(
                    lang = lang, label = label,
                    url = android.net.Uri.fromFile(f).toString(),
                )
            }
        }
        return out
    }
}
