package com.onimeku.app.data

import android.content.Context
import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheWriter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * UNDUHAN PARALEL MULTI-PINTU untuk episode gudang — debug.60.
 *
 * Masalah lama: unduhan Media3 memakai SATU pintu (URL tangga dari satu
 * pintu) — semua segmen datang dari host yang sama; host itu lambat/mati
 * di tengah, seluruh unduhan ikut lambat/gagal.
 *
 * Susunan baru (kunci cache Media3 TIDAK berubah):
 * - [DoorPoolDataSource] duduk sebagai hulu DI BAWAH CacheDataSource
 *   (di tumpukan DownloadManager maupun di pemanasan segmen di bawah).
 *   CacheDataSource menghitung kunci cache dari DataSpec ASLI (pintu
 *   utama), jadi susunan segmen di cache dan putar luring sesudahnya
 *   persis seperti sebelumnya — yang berubah hanya pintu tempat byte
 *   benar-benar diambil.
 * - Tiap open() untuk berkas di direktori episode yang terdaftar diputar
 *   ke pintu berikutnya (mulai dari pintu terakhir-berhasil); open yang
 *   gagal dicoba ke pintu berikutnya, maks satu putaran penuh, sebelum
 *   galat diteruskan. Permintaan posisi != 0 (pengisian celah cache
 *   sebagian oleh Media3) diteruskan apa adanya ke pintu asal agar
 *   rentang byte tetap konsisten.
 * - [GudangDl.prefetchSegments] memanaskan segmen SEBELUM Media3
 *   mengambil alih: daftar segmen tangga terpilih dipecah ke 4 pekerja
 *   paralel (pola konkurensi yang sudah dipakai DownloadManager proyek
 *   ini), masing-masing menulis ke cache unduhan yang sama lewat
 *   [CacheWriter] — alat yang persis dipakai SegmentDownloader Media3
 *   1.5.0 untuk mengunduh segmen, dengan kunci cache yang sama
 *   (CacheKeyFactory bawaan: URI segmen pada pintu utama). Sesudah itu
 *   DownloadManager menemukan hampir semua segmen sudah tercache,
 *   mengambil playlist + sisa yang gagal saja, dan tetap menjadi pemilik
 *   status/progres unduhan seperti sebelumnya.
 */
object DoorPoolRegistry {
    private const val PREFS = "gudang_dl_pool"
    private val mem = ConcurrentHashMap<String, List<String>>()

    /**
     * Daftarkan direktori tangga untuk semua pintu satu episode+kualitas.
     * [ladderUrls] = URL playlist tangga terpilih dari tiap pintu.
     */
    fun register(ctx: Context, ladderUrls: List<String>) {
        val dirs = ladderUrls.filter { it.isNotBlank() }.map { DoorMath.dirOf(it) }.distinct()
        if (dirs.size < 2) return
        val canon = ladderUrls.firstNotNullOfOrNull { DoorMath.canonicalDir(it) } ?: return
        mem[canon] = dirs
        try {
            val arr = org.json.JSONArray()
            for (d in dirs) arr.put(d)
            ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(canon, arr.toString()).apply()
        } catch (e: Exception) { /* persistensi usaha terbaik */ }
    }

    /** Direktori dasar semua pintu untuk direktori kanonis ini (memori dulu, lalu disk). */
    fun basesFor(ctx: Context, canonicalDir: String): List<String>? {
        mem[canonicalDir]?.let { return it }
        val raw = try {
            ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(canonicalDir, null)
        } catch (e: Exception) {
            null
        } ?: return null
        return try {
            val arr = org.json.JSONArray(raw)
            val out = ArrayList<String>(arr.length())
            for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let { out.add(it) }
            if (out.size >= 2) {
                mem[canonicalDir] = out
                out
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

class DoorPoolDataSource(
    private val ctx: Context,
    private val upstream: DataSource.Factory,
    private val counters: ConcurrentHashMap<String, AtomicInteger>,
) : DataSource {

    class Factory(
        private val ctx: Context,
        private val upstream: DataSource.Factory,
    ) : DataSource.Factory {
        private val counters = ConcurrentHashMap<String, AtomicInteger>()
        override fun createDataSource(): DataSource =
            DoorPoolDataSource(ctx.applicationContext, upstream, counters)
    }

    private val listeners = ArrayList<TransferListener>()
    private var active: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        listeners.add(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        closeQuietly()
        val url = dataSpec.uri.toString()
        val canon = if (dataSpec.position == 0L) DoorMath.canonicalDir(url) else null
        val bases = canon?.let { DoorPoolRegistry.basesFor(ctx, it) }
        if (canon == null || bases == null || bases.size < 2) {
            return openPlain(dataSpec)
        }
        val file = DoorMath.fileNameOf(url)
        val counter = counters.getOrPut(canon) { AtomicInteger(0) }
        val start = counter.getAndIncrement()
        var lastErr: IOException? = null
        for (idx in DoorMath.rotationOrder(bases.size, start)) {
            val leg = upstream.createDataSource()
            for (l in listeners) leg.addTransferListener(l)
            try {
                val len = leg.open(
                    dataSpec.buildUpon().setUri(Uri.parse(bases[idx] + file)).build()
                )
                active = leg
                return len
            } catch (e: IOException) {
                lastErr = e
                closeQuiet(leg)
            }
        }
        throw lastErr ?: IOException("semua pintu unduhan gagal untuk $file")
    }

    private fun openPlain(spec: DataSpec): Long {
        val leg = upstream.createDataSource()
        for (l in listeners) leg.addTransferListener(l)
        val len = leg.open(spec)
        active = leg
        return len
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        (active ?: throw IOException("DoorPoolDataSource belum dibuka")).read(buffer, offset, length)

    override fun getUri(): Uri? = active?.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        active?.responseHeaders ?: emptyMap()

    override fun close() {
        closeQuietly()
    }

    private fun closeQuietly() {
        val ds = active
        active = null
        if (ds != null) closeQuiet(ds)
    }

    private fun closeQuiet(ds: DataSource) {
        try {
            ds.close()
        } catch (e: Exception) { /* tutup usaha terbaik */ }
    }
}

object GudangDl {
    /** Pekerja paralel pemanasan segmen — sama dengan pola konkurensi DownloadManager proyek ini. */
    const val SEGMENT_WORKERS = 4

    /**
     * Panaskan segmen tangga terpilih ke cache unduhan dari banyak pintu.
     *
     * Mengembalikan jumlah segmen yang berhasil tercache (termasuk yang
     * memang sudah ada). Kegagalan per segmen sesudah satu putaran pintu
     * DIBIARKAN — DownloadManager akan mengambilnya sendiri lewat
     * tumpukan multi-pintunya. [onProgress] menerima persen 0..99.
     *
     * Menghormati "Wi-Fi only": bila [wifiOnly] dan jaringan aktif
     * terukur, pemanasan dilewati seluruhnya (Media3 juga menahan
     * unduhannya sampai jaringan tak terukur).
     */
    suspend fun prefetchSegments(
        ctx: Context,
        primaryLadderUrl: String,
        wifiOnly: Boolean,
        onProgress: (Int) -> Unit = {},
    ): Int = withContext(Dispatchers.IO) {
        val app = ctx.applicationContext
        if (wifiOnly && isMetered(app)) return@withContext 0
        val playlist = fetchText(primaryLadderUrl) ?: return@withContext 0
        val segments = DoorMath.parseSegments(playlist, primaryLadderUrl)
        if (segments.isEmpty()) return@withContext 0
        val done = AtomicInteger(0)
        val ok = AtomicInteger(0)
        coroutineScope {
            DoorMath.splitRoundRobin(segments, SEGMENT_WORKERS).map { group ->
                async {
                    // Satu CacheDataSource per pekerja (tidak aman dipakai bersamaan).
                    val cds = Downloads.dlCacheDataSource(app)
                    val buffer = ByteArray(64 * 1024)
                    for (segUrl in group) {
                        try {
                            val spec = DataSpec.Builder().setUri(Uri.parse(segUrl)).build()
                            CacheWriter(cds, spec, buffer, null).cache()
                            ok.incrementAndGet()
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            // Segmen ini diserahkan ke DownloadManager (jalur multi-pintu juga).
                        }
                        val d = done.incrementAndGet()
                        onProgress((d * 100 / segments.size).coerceIn(0, 99))
                    }
                }
            }.awaitAll()
        }
        ok.get()
    }

    private fun isMetered(ctx: Context): Boolean = try {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        cm.isActiveNetworkMetered
    } catch (e: Exception) {
        false
    }

    private fun fetchText(url: String): String? = try {
        val req = okhttp3.Request.Builder().url(url)
            .header("User-Agent", Net.UA).build()
        val client = Net.client.newBuilder()
            .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        client.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    } catch (e: Exception) {
        null
    }
}
