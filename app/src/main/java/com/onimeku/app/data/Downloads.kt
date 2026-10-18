package com.onimeku.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadHelper
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.onimeku.app.R
import java.io.File
import java.io.IOException

/**
 * Unduhan ASLI memakai Media3: DownloadManager + DownloadService mengunduh
 * segmen HLS ke SimpleCache yang sama dengan yang dibaca pemutar, jadi
 * episode yang selesai terunduh bisa diputar offline. Kunci cache dibuat
 * stabil per episode ("dl:<id>:<ep>") karena URL stream bertoken berubah
 * setiap pengambilan — pemutar memakai kunci yang sama agar cocok.
 */
object Downloads {
    const val CHANNEL_ID = "onimeku_downloads"
    private const val UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    @Volatile private var manager: DownloadManager? = null
    @Volatile private var simpleCache: SimpleCache? = null
    @Volatile private var database: StandaloneDatabaseProvider? = null

    fun key(id: String, ep: Int) = "dl:$id:$ep"

    /** "dl:<id>:<ep>" -> (id, ep); id adalah slug tanpa ':' jadi aman. */
    fun parseKey(k: String): Pair<String, Int>? {
        if (!k.startsWith("dl:")) return null
        val rest = k.substring(3)
        val i = rest.lastIndexOf(':')
        if (i <= 0) return null
        val ep = rest.substring(i + 1).toIntOrNull() ?: return null
        return rest.substring(0, i) to ep
    }

    private fun db(ctx: Context): StandaloneDatabaseProvider =
        database ?: synchronized(this) {
            database ?: StandaloneDatabaseProvider(ctx.applicationContext).also { database = it }
        }

    fun cache(ctx: Context): SimpleCache =
        simpleCache ?: synchronized(this) {
            simpleCache ?: SimpleCache(
                File(ctx.applicationContext.filesDir, "media_downloads"),
                NoOpCacheEvictor(),
                db(ctx),
            ).also { simpleCache = it }
        }

    // ---- Referer per unduhan ----
    // CDN membedakan asal permintaan; Referer dari respons API disimpan per
    // awalan URL stream (host + segmen path pertama) agar unduhan segmen
    // memakai Referer yang sama seperti pemutaran.
    private fun refKey(url: String): String? {
        val u = runCatching { java.net.URL(url) }.getOrNull() ?: return null
        val first = u.path.trim('/').substringBefore('/')
        return u.host + "/" + first
    }

    private fun registerRef(ctx: Context, streamUrl: String, referer: String?) {
        if (referer.isNullOrBlank()) return
        val k = refKey(streamUrl) ?: return
        ctx.applicationContext.getSharedPreferences("dl_refs", Context.MODE_PRIVATE)
            .edit().putString(k, referer).apply()
    }

    private fun refFor(ctx: Context, url: String): String? {
        val k = refKey(url) ?: return null
        return ctx.applicationContext.getSharedPreferences("dl_refs", Context.MODE_PRIVATE)
            .getString(k, null)
    }

    private fun httpFactory(ctx: Context): OkHttpDataSource.Factory {
        val app = ctx.applicationContext
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            val req = chain.request()
            val b = req.newBuilder().header("User-Agent", UA)
            if (req.header("Referer") == null) {
                refFor(app, req.url.toString())?.let { b.header("Referer", it) }
            }
            chain.proceed(b.build())
        }.build()
        return OkHttpDataSource.Factory(client)
    }

    fun manager(ctx: Context): DownloadManager =
        manager ?: synchronized(this) {
            manager ?: buildManager(ctx.applicationContext).also { manager = it }
        }

    private fun buildManager(app: Context): DownloadManager {
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
        )
        val dsFactory = CacheDataSource.Factory()
            .setCache(cache(app))
            .setUpstreamDataSourceFactory(httpFactory(app))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val dm = DownloadManager(
            app, db(app), cache(app), dsFactory,
            java.util.concurrent.Executors.newFixedThreadPool(4),
        )
        dm.maxParallelDownloads = 3
        return dm
    }

    /** Hormati pengaturan "Wi-Fi only": unduhan menunggu jaringan tak terukur. */
    fun setWifiOnly(ctx: Context, wifiOnly: Boolean) {
        manager(ctx).requirements = androidx.media3.exoplayer.scheduler.Requirements(
            if (wifiOnly) androidx.media3.exoplayer.scheduler.Requirements.NETWORK_UNMETERED
            else androidx.media3.exoplayer.scheduler.Requirements.NETWORK
        )
    }

    fun enqueue(
        ctx: Context, id: String, ep: Int, streamUrl: String, referer: String?,
        onError: () -> Unit,
    ) {
        val app = ctx.applicationContext
        registerRef(app, streamUrl, referer)
        val contentKey = key(id, ep)
        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(contentKey)
            .setCustomCacheKey(contentKey)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()
        // DownloadHelper memilih satu set trek bawaan (bukan semua rendisi)
        // dan mengambil manifes lewat factory kita (UA + Referer ikut).
        val helper = DownloadHelper.forMediaItem(
            app,
            mediaItem,
            DefaultRenderersFactory(app),
            httpFactory(app),
        )
        helper.prepare(object : DownloadHelper.Callback {
            override fun onPrepared(h: DownloadHelper) {
                val req = h.getDownloadRequest(null)
                DownloadService.sendAddDownload(app, OnimekuDownloadService::class.java, req, true)
                h.release()
            }

            override fun onPrepareError(h: DownloadHelper, e: IOException) {
                h.release()
                onError()
            }
        })
    }

    fun remove(ctx: Context, contentKey: String) {
        DownloadService.sendRemoveDownload(
            ctx.applicationContext, OnimekuDownloadService::class.java, contentKey, false,
        )
    }

    /** Peta contentKey -> Download dari indeks unduhan saat ini. */
    fun states(ctx: Context): Map<String, Download> {
        val out = LinkedHashMap<String, Download>()
        manager(ctx).downloadIndex.getDownloads().use { cursor ->
            while (cursor.moveToNext()) out[cursor.download.request.id] = cursor.download
        }
        return out
    }
}

/** Layanan unduhan Media3 (proses utama, berbagi DownloadManager singleton). */
class OnimekuDownloadService : DownloadService(
    1001,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    Downloads.CHANNEL_ID,
    R.string.dl_channel_name,
    R.string.dl_channel_desc,
) {
    override fun getDownloadManager(): DownloadManager = Downloads.manager(this)
    override fun getScheduler(): Scheduler? = null
    override fun getForegroundNotification(
        downloads: MutableList<Download>, notMetRequirements: Int,
    ): android.app.Notification =
        androidx.media3.exoplayer.offline.DownloadNotificationHelper(this, Downloads.CHANNEL_ID)
            .buildProgressNotification(
                this, android.R.drawable.stat_sys_download, null, null, downloads, notMetRequirements,
            )
}
