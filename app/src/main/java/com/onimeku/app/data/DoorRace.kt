package com.onimeku.app.data

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * BALAPAN PINTU (hedged requests) untuk putar gudang — debug.60.
 *
 * Pola V3 yang sudah ada memindahkan pintu hanya SETELAH muatan gagal
 * total (putar dibangun ulang dari detik yang sama). Balapan menyerang
 * kasus yang lebih sering: pintu yang tidak mati tetapi LAMBAT — playlist
 * atau segmen yang menggantung belasan detik sebelum akhirnya tiba/gagal.
 *
 * Cara kerja (per sesi putar, lihat [DoorRaceSession]):
 * - Permintaan ke berkas di bawah direktori master episode gudang
 *   dipetakan ke semua pintu (isi identik, awalan direktori berbeda).
 * - open() memulai dari pintu utama (terakhir-berhasil). Bila open belum
 *   selesai dalam [DoorRaceSession.hedgeDelayMs], permintaan yang SAMA
 *   dibuka bersamaan di pintu berikutnya (maks 3 kaki). Kaki pertama yang
 *   berhasil menjadi sumber bacaan; kaki lain ditutup/dibatalkan.
 * - Pemenang yang berbeda dari pintu utama menjadi pintu utama sesi dan
 *   dilaporkan lewat callback (pemanggil menyimpannya sebagai pintu
 *   terakhir-berhasil untuk kunjungan berikutnya).
 * - Balapan hanya dipicu pada fase awal putar (beberapa muatan pertama —
 *   playlist master/varian + segmen pertama, termasuk sesudah sik karena
 *   pemutar membangun ulang muatan dari posisi baru) dan pada "mode
 *   dorong" sesudah ada muatan lambat/gagal. Di luar itu penundaan
 *   balapannya lebih panjang, jadi segmen yang sehat tidak pernah
 *   diduplikasi — data pengguna tidak diboroskan untuk balapan rutin.
 *
 * BUKAN untuk putar luring (cache unduhan) dan bukan untuk berkas di luar
 * direktori episode gudang (subtitle samping file:// dsb. diteruskan apa
 * adanya ke hulu).
 */
class DoorRaceSession(
    doors: List<ApiDoor>,
    startDoorName: String?,
    private val onWin: (String) -> Unit,
) {
    /** (nama pintu, awalan direktori master) per pintu, urutan dari API yang sudah diurutkan pemanggil. */
    private val bases: List<Pair<String, String>> =
        doors.filter { it.url.isNotBlank() }.map { it.name to DoorMath.dirOf(it.url) }

    @Volatile
    private var primaryIdx: Int =
        bases.indexOfFirst { it.first == startDoorName }.takeIf { it >= 0 } ?: 0

    private val loadCount = AtomicInteger(0)

    @Volatile
    private var boostUntilMs = 0L

    val doorCount: Int get() = bases.size

    /**
     * Kandidat URL untuk [url]: pintu utama lebih dulu, lalu sisa pintu
     * satu putaran — atau null bila URL tidak berada di bawah direktori
     * episode gudang mana pun (jangan disentuh).
     */
    fun candidates(url: String): List<Pair<String, String>>? {
        val hit = bases.indexOfFirst { url.startsWith(it.second) }
        if (hit < 0) return null
        val rel = url.removePrefix(bases[hit].second)
        if (rel.isBlank() || rel.contains("..")) return null
        return DoorMath.rotationOrder(bases.size, primaryIdx)
            .map { i -> bases[i].first to (bases[i].second + rel) }
    }

    /** Jeda sebelum kaki balapan berikutnya dibuka: singkat di fase awal / mode dorong. */
    fun hedgeDelayMs(): Long =
        if (loadCount.get() < STARTUP_LOADS || System.currentTimeMillis() < boostUntilMs) {
            HEDGE_FAST_MS
        } else {
            HEDGE_SLOW_MS
        }

    /** Catat satu open() yang berhasil; [winnerName] = pintu yang benar-benar melayani. */
    fun onOpenSucceeded(winnerName: String?) {
        loadCount.incrementAndGet()
        if (winnerName == null) return
        val i = bases.indexOfFirst { it.first == winnerName }
        if (i >= 0 && i != primaryIdx) {
            primaryIdx = i
            try {
                onWin(winnerName)
            } catch (e: Exception) { /* callback usaha terbaik */ }
        }
    }

    /** Catat muatan lambat/gagal: balapan berikutnya dibuat lebih agresif sebentar. */
    fun onSlowOrError() {
        loadCount.incrementAndGet()
        boostUntilMs = System.currentTimeMillis() + BOOST_MS
    }

    companion object {
        /** Muatan pertama sesi dianggap fase awal (master, varian, segmen-segmen pertama). */
        const val STARTUP_LOADS = 6
        const val HEDGE_FAST_MS = 1_200L
        const val HEDGE_SLOW_MS = 2_500L
        const val BOOST_MS = 15_000L
        const val MAX_LEGS = 3
        const val RACE_CAP_MS = 20_000L
        const val SLOW_FIRST_BYTE_MS = 3_000L
    }
}

class DoorRaceDataSource(
    private val upstream: DataSource.Factory,
    private val session: DoorRaceSession,
) : DataSource {

    class Factory(
        private val upstream: DataSource.Factory,
        private val session: DoorRaceSession,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = DoorRaceDataSource(upstream, session)
    }

    private val listeners = ArrayList<TransferListener>()
    private var active: DataSource? = null
    private var openAtMs = 0L
    private var firstReadChecked = false

    override fun addTransferListener(transferListener: TransferListener) {
        listeners.add(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        closeQuietly()
        openAtMs = System.currentTimeMillis()
        firstReadChecked = false
        val url = dataSpec.uri.toString()
        val isHttp = url.startsWith("http://") || url.startsWith("https://")
        val cands = if (isHttp && dataSpec.position == 0L) session.candidates(url) else null
        if (cands == null || cands.size < 2) {
            return openSingle(dataSpec, cands?.firstOrNull()?.first)
        }
        return openHedged(dataSpec, cands)
    }

    private fun newLeg(): DataSource {
        val leg = upstream.createDataSource()
        for (l in listeners) leg.addTransferListener(l)
        return leg
    }

    private fun openSingle(spec: DataSpec, doorName: String?): Long {
        val leg = newLeg()
        try {
            val len = leg.open(spec)
            active = leg
            session.onOpenSucceeded(doorName)
            return len
        } catch (e: IOException) {
            closeQuiet(leg)
            session.onSlowOrError()
            throw e
        }
    }

    private class LegOutcome(val ds: DataSource?, val length: Long)

    private fun openHedged(spec: DataSpec, cands: List<Pair<String, String>>): Long {
        val resolved = AtomicBoolean(false)
        val legs = java.util.Collections.synchronizedList(ArrayList<DataSource>())
        val futures = ArrayList<Future<LegOutcome>>()
        val errors = java.util.Collections.synchronizedList(ArrayList<IOException>())
        var submitted = 0
        val deadline = openAtMs + DoorRaceSession.RACE_CAP_MS
        var nextHedgeAt = openAtMs + session.hedgeDelayMs()
        val maxLegs = minOf(cands.size, DoorRaceSession.MAX_LEGS)

        fun submit(idx: Int) {
            val (name, altUrl) = cands[idx]
            val leg = newLeg()
            legs.add(leg)
            futures.add(
                raceExecutor.submit(
                    Callable<LegOutcome> {
                        try {
                            val len = leg.open(
                                spec.buildUpon().setUri(Uri.parse(altUrl)).build()
                            )
                            if (!resolved.compareAndSet(false, true)) {
                                // Kaki lain sudah menang: tutup diri sendiri.
                                closeQuiet(leg)
                                LegOutcome(null, -1)
                            } else {
                                LegOutcome(leg, len)
                            }
                        } catch (e: IOException) {
                            errors.add(e)
                            LegOutcome(null, -1)
                        }
                    }
                )
            )
            submitted++
        }

        submit(0)
        while (true) {
            var winner: LegOutcome? = null
            for (f in futures) {
                if (f.isDone) {
                    val o = try {
                        f.get()
                    } catch (e: Exception) {
                        null
                    }
                    if (o?.ds != null) {
                        winner = o
                        break
                    }
                }
            }
            if (winner != null) {
                active = winner.ds
                // Nama pintu pemenang: kaki yang DataSource-nya terpilih.
                val winIdx = legs.indexOf(winner.ds)
                val winName = if (winIdx in cands.indices) cands[winIdx].first else null
                session.onOpenSucceeded(winName)
                return winner.length
            }
            val allDone = futures.all { it.isDone }
            if (allDone && submitted >= maxLegs) break
            val now = System.currentTimeMillis()
            if (submitted < maxLegs && (allDone || now >= nextHedgeAt)) {
                submit(submitted)
                nextHedgeAt = now + session.hedgeDelayMs()
                continue
            }
            if (now >= deadline) break
            try {
                Thread.sleep(15)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
        for (leg in legs) closeQuiet(leg)
        session.onSlowOrError()
        throw errors.firstOrNull() ?: IOException("balapan pintu: semua pintu gagal/gantung")
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val ds = active ?: throw IOException("DoorRaceDataSource belum dibuka")
        val n = try {
            ds.read(buffer, offset, length)
        } catch (e: IOException) {
            session.onSlowOrError()
            throw e
        }
        if (!firstReadChecked) {
            firstReadChecked = true
            if (n > 0 && System.currentTimeMillis() - openAtMs > DoorRaceSession.SLOW_FIRST_BYTE_MS) {
                // Tiba, tetapi sangat lambat: muatan berikutnya dibalap lebih agresif.
                session.onSlowOrError()
            }
        }
        return n
    }

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

    companion object {
        private val raceExecutor = Executors.newCachedThreadPool { r ->
            Thread(r, "door-race").apply { isDaemon = true }
        }

        private fun closeQuiet(ds: DataSource) {
            try {
                ds.close()
            } catch (e: Exception) { /* tutup usaha terbaik */ }
        }
    }
}
