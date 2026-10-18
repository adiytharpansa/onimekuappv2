package com.onimeku.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Pemuat gambar Coil tunggal untuk seluruh aplikasi.
 * - Cache memori 20% RAM aplikasi, cache disk 256 MB: poster tidak diunduh ulang tiap sesi.
 * - respectCacheHeaders(false): proksi gambar/CDN sering mengirim no-cache; tanpa ini poster diunduh ulang terus.
 * - RGB_565 dimatikan (kualitas poster); hardware bitmap aktif otomatis di Android 8+ (hemat memori Java).
 */
class OnimekuApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        com.onimeku.app.data.CrashLog.install(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("img")).maxSizeBytes(256L * 1024 * 1024).build() }
            .respectCacheHeaders(false)
            .crossfade(false) // crossfade diatur per permintaan (rememberImg)
            .build()
}
