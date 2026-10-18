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
 * Poster ber-host s4.anilist.co tidak terjangkau dari jaringan pengguna,
 * jadi untuk judul-judul itu poster/cover diganti padanan Kitsu-nya lewat
 * endpoint /api/poster/:anilistId milik API sendiri. Resolusi berjalan
 * malas (hanya untuk judul yang sedang tampil), hasilnya di-cache di
 * memori + SharedPreferences supaya tampil seketika di buka berikutnya.
 * Selama belum teresolusi (atau tak ada padanan), pemanggil menerima null
 * dan UI menampilkan latar gradasi bawaan.
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
        }
    }

    private fun isAnilist(url: String?): Boolean =
        url != null && url.contains("s4.anilist.co")

    fun posterFor(a: Anime): String? {
        val p = a.poster
        if (p.isNullOrBlank()) return null
        if (!isAnilist(p)) return p
        val aid = a.anilistId ?: return null
        posters[aid]?.let { return it.ifBlank { null } }
        prefs?.getString("p$aid", null)?.let { posters[aid] = it; return it }
        ensure(aid)
        return null
    }

    fun coverFor(a: Anime): String? {
        val b = a.banner
        if (b.isNullOrBlank()) return null
        if (!isAnilist(b)) return b
        val aid = a.anilistId ?: return null
        covers[aid]?.let { return it.ifBlank { null } }
        prefs?.getString("c$aid", null)?.let { covers[aid] = it; return it }
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
                    prefs?.edit()
                        ?.putString("p$aid", r.poster ?: "")
                        ?.putString("c$aid", r.cover ?: "")
                        ?.apply()
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
