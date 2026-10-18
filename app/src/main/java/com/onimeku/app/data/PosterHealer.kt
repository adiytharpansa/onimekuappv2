package com.onimeku.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * Poster ber-host s4.anilist.co tidak terjangkau dari jaringan pengguna
 * (CDN-nya membalas 403 dari mana pun). Penyembuhannya kini hidup di
 * SERVER (debug.31, perintah user: semua data poster dari API ini):
 * my-anime-app menyelesaikan padanan Kitsu per anilistId dan
 * menyajikannya langsung di respons katalog/detail, jadi di sini
 * poster katalog yang masih ber-host rusak cukup disembunyikan
 * (null -> latar gradasi) sampai API menyembuhkannya — aplikasi
 * TIDAK lagi menelepon Kitsu atau API lain sendiri. Cache lokal
 * hanya menyimpan URL yang disajikan API.
 */
object PosterHealer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val posters = mutableStateMapOf<Int, String>()
    private val covers = mutableStateMapOf<Int, String>()
    private val inFlight = Collections.synchronizedSet(HashSet<Int>())
    private var prefs: SharedPreferences? = null

    fun init(ctx: Context) {
        if (prefs == null) {
            prefs = ctx.getSharedPreferences("poster_cache", Context.MODE_PRIVATE)
            // Sekali saja (debug.31): kosongkan cache lama yang berisi
            // URL hasil resolusi langsung aplikasi (era debug.26-.30) —
            // mulai sekarang poster hanya berasal dari API.
            if (prefs?.getBoolean("reset_v31", false) != true) {
                prefs?.edit()?.clear()?.putBoolean("reset_v31", true)?.apply()
            }
        }
    }

    // Host/pola poster bermasalah — cerminan daftar BAD_HOST/BAD_PATH di API:
    // poster seperti ini tidak ditampilkan mentah, melainkan diganti poster
    // asli dari Kitsu lewat endpoint resolusi API.
    private fun isBad(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val u = url.lowercase()
        return u.contains("anilist.co") || u.contains("samehadaku") ||
            u.contains("placeholder") || u.contains("placehold") ||
            u.contains("dummyimage") || u.contains("picsum") || u.contains("ui-avatars") ||
            u.contains("backblazeb2") || u.contains("amazonaws.com")
    }

    // Sebagian kecil poster Kitsu berupa URL bertanda tangan S3/Backblaze
    // (X-Amz-Expires=900 — mati dalam 15 menit). URL semacam itu TIDAK
    // boleh disimpan permanen: cukup hidup di memori sesi ini, dan entri
    // cache lama yang terlanjur menyimpannya dianggap basi (debug.28).
    private fun isPresigned(u: String?): Boolean = u?.contains("X-Amz-") == true

    fun posterFor(a: Anime): String? {
        val p = a.poster
        if (p.isNullOrBlank()) return null
        if (!isBad(p)) return p
        val aid = a.anilistId ?: return null
        posters[aid]?.let { return it.ifBlank { null } }
        prefs?.getString("p$aid", null)?.let {
            if (!isPresigned(it)) { posters[aid] = it; return it }
        }
        ensure(aid)
        return null
    }

    fun coverFor(a: Anime): String? {
        val b = a.banner
        if (b.isNullOrBlank()) return null
        if (!isBad(b)) return b
        val aid = a.anilistId ?: return null
        covers[aid]?.let { return it.ifBlank { null } }
        prefs?.getString("c$aid", null)?.let {
            if (!isPresigned(it)) { covers[aid] = it; return it }
        }
        ensure(aid)
        return null
    }

    private fun ensure(aid: Int) {
        if (!inFlight.add(aid)) return
        scope.launch {
            try {
                val r = ApiClient.resolvePoster(aid)
                posters[aid] = r?.poster ?: ""
                covers[aid] = r?.cover ?: ""
                if (r != null) {
                    // Hanya URL stabil yang disimpan permanen; yang
                    // bertanda tangan kedaluwarsa (X-Amz-) memori saja.
                    val ed = prefs?.edit()
                    if (!isPresigned(r.poster)) ed?.putString("p$aid", r.poster ?: "")
                    if (!isPresigned(r.cover)) ed?.putString("c$aid", r.cover ?: "")
                    ed?.apply()
                }
            } catch (e: Exception) {
                posters[aid] = ""
                covers[aid] = ""
            } finally {
                inFlight.remove(aid)
            }
        }
    }
}
