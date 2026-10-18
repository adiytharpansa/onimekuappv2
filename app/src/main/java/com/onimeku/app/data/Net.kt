package com.onimeku.app.data

import okhttp3.ConnectionPool
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Satu OkHttpClient untuk seluruh jalur jaringan media/API.
 *
 * Sebelumnya setiap pemutar membuat OkHttpClient baru (pool koneksi, TLS, dan
 * cookie dimulai dari nol tiap ganti episode) dan API memakai HttpURLConnection
 * (handshake TLS baru tiap permintaan). Dengan klien bersama, koneksi HTTP/2 ke
 * worker/CDN dipakai ulang: playlist + segmen pertama tiba lebih cepat dan
 * pindah episode tidak mengulang handshake.
 */
object Net {
    const val UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    /** Cookie per host (CDN sumber menuntut cookie dari respons sebelumnya). */
    private val cookieJar = object : CookieJar {
        private val store = ConcurrentHashMap<String, MutableList<Cookie>>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = store.getOrPut(url.host) { mutableListOf() }
            synchronized(list) {
                for (c in cookies) {
                    list.removeAll { it.name == c.name }
                    list.add(c)
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val list = store[url.host] ?: return emptyList()
            return synchronized(list) { list.filter { it.matches(url) } }
        }
    }

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
