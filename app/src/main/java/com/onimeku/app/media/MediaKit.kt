package com.onimeku.app.media

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.*
import androidx.media3.exoplayer.scheduler.Requirements
import java.io.File
import java.util.concurrent.Executors

/** Satu cache untuk streaming DAN unduhan: episode yang sudah diunduh otomatis diputar offline. */
@UnstableApi
object MediaKit {
    const val CHANNEL = "downloads"
    private var cache: SimpleCache? = null
    private var db: StandaloneDatabaseProvider? = null
    private var dm: DownloadManager? = null
    private val http = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)

    @Synchronized private fun db(c: Context) = db ?: StandaloneDatabaseProvider(c.applicationContext).also { db = it }
    @Synchronized fun cache(c: Context) = cache ?: SimpleCache(File(c.applicationContext.filesDir, "media"), NoOpCacheEvictor(), db(c)).also { cache = it }

    fun dataSource(c: Context): DataSource.Factory = CacheDataSource.Factory().setCache(cache(c))
        .setUpstreamDataSourceFactory(DefaultDataSource.Factory(c, http)).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    @Synchronized fun downloads(c: Context): DownloadManager = dm ?: DownloadManager(
        c.applicationContext, db(c), cache(c), http, Executors.newFixedThreadPool(2)).also { dm = it }

    fun setWifiOnly(c: Context, on: Boolean) {
        downloads(c).requirements = Requirements(if (on) Requirements.NETWORK_UNMETERED else Requirements.NETWORK) }

    fun isMetered(c: Context) = c.getSystemService(ConnectivityManager::class.java).isActiveNetworkMetered

    fun enqueue(c: Context, id: String, url: String) {
        val req = DownloadRequest.Builder(id, Uri.parse(url)).setMimeType(MimeTypes.APPLICATION_M3U8).build()
        DownloadService.sendAddDownload(c, OnimekuDownloadService::class.java, req, false) }

    fun remove(c: Context, id: String) = DownloadService.sendRemoveDownload(c, OnimekuDownloadService::class.java, id, false)

    /** Semua unduhan (selesai & berjalan). Panggil dari thread background. */
    fun list(c: Context): List<Download> {
        val cur = downloads(c).downloadIndex.getDownloads()
        return buildList { while (cur.moveToNext()) add(cur.download) }.also { cur.close() } }

    /** URI asli unduhan yang selesai (dipakai saat offline agar cache key cocok). Thread background. */
    fun downloadedUri(c: Context, id: String): Uri? = downloads(c).downloadIndex.getDownload(id)?.takeIf { it.state == Download.STATE_COMPLETED }?.request?.uri

    /** Panggil dari thread background (query database). */
    fun isDownloaded(c: Context, id: String) = downloads(c).downloadIndex.getDownload(id)?.state == Download.STATE_COMPLETED
}
