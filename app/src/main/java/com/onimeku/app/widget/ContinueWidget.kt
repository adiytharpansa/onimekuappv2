package com.onimeku.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.onimeku.app.I18n
import com.onimeku.app.R
import com.onimeku.app.data.Anime
import com.onimeku.app.data.ApiClient
import com.onimeku.app.data.Profile
import com.onimeku.app.data.Store
import com.onimeku.app.ui.fmt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Widget layar utama "Continue Watching": poster, judul, episode, sisa waktu, bar progres.
 * Data dibaca dari state tersimpan (SharedPreferences) + cache katalog disk, jadi widget
 * tidak bergantung pada proses aplikasi yang hidup. Ketuk = langsung memutar episode itu.
 * Diperbarui tiap 30 mnt (sistem) dan dari aplikasi lewat [requestUpdate] saat progres tersimpan.
 */
class ContinueWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = build(context)
                for (id in ids) manager.updateAppWidget(id, views)
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }

    private data class Pick(val a: Anime, val ep: Int, val pos: Int, val dur: Int)

    /** Aturan sama dengan baris Continue Watching di Home: progres aktif, atau episode tepat setelah yang tuntas. */
    private fun pick(p: Profile, byId: Map<String, Anime>): Pick? {
        fun consider(id: String, ep: Int, dur: Int): Pick? {
            val a = byId[id] ?: return null
            val pos = p.prog["$id:$ep"]
            return when {
                pos != null && pos >= 5 -> Pick(a, ep, pos, dur)
                "$id:$ep" in p.seen && ep + 1 <= a.ep -> Pick(a, ep + 1, 0, 0)
                else -> null
            }
        }
        for (w in p.watch) consider(w.id, w.ep, w.dur)?.let { return it }
        for (k in p.prog.keys) {
            val ep = k.substringAfterLast(':').toIntOrNull() ?: continue
            consider(k.substringBeforeLast(':'), ep, 0)?.let { return it }
        }
        return null
    }

    private suspend fun build(ctx: Context): RemoteViews {
        val state = Store(ctx).load()
        I18n.lang = state.settings.lang
        val catalog = ApiClient.loadCachedCatalog(File(ctx.filesDir, "catalog_cache_direct.json")).orEmpty()
        val pick = pick(state.profiles.getOrNull(state.pi) ?: state.profiles.first(), catalog.associateBy { it.id })

        val views = RemoteViews(ctx.packageName, R.layout.widget_continue)
        if (pick == null) {
            views.setViewVisibility(R.id.w_content, View.GONE)
            views.setViewVisibility(R.id.w_empty, View.VISIBLE)
            views.setTextViewText(R.id.w_empty_title, I18n.t("nothing_to_continue", "Nothing to continue yet"))
            views.setTextViewText(R.id.w_empty_sub, I18n.t("widget_open", "Tap to open Onimeku"))
            views.setOnClickPendingIntent(R.id.w_root, openIntent(ctx, Uri.parse("onimeku://search")))
            return views
        }
        views.setViewVisibility(R.id.w_content, View.VISIBLE)
        views.setViewVisibility(R.id.w_empty, View.GONE)
        views.setTextViewText(R.id.w_title, pick.a.title)
        val left = if (pick.pos > 0 && pick.dur > pick.pos) " · ${fmt(pick.dur - pick.pos)} ${I18n.t("time_left", "left")}" else ""
        views.setTextViewText(R.id.w_sub, "${I18n.t("widget_episode", "Episode")} ${pick.ep}$left")
        val pct = if (pick.pos > 0 && pick.dur > 0) (pick.pos * 100 / pick.dur).coerceIn(3, 97) else 0
        views.setProgressBar(R.id.w_progress, 100, pct, false)
        views.setViewVisibility(R.id.w_progress, if (pct > 0) View.VISIBLE else View.GONE)

        val url = pick.a.poster
        if (!url.isNullOrBlank()) {
            try {
                val req = ImageRequest.Builder(ctx).data(url).size(240, 360).allowHardware(false).build()
                (ctx.imageLoader.execute(req) as? SuccessResult)?.drawable?.toBitmap(240, 360)?.let { views.setImageViewBitmap(R.id.w_poster, it) }
            } catch (_: Exception) { /* poster opsional */ }
        }
        val uri = Uri.parse("onimeku://continue?id=${Uri.encode(pick.a.id)}&ep=${pick.ep}")
        views.setOnClickPendingIntent(R.id.w_root, openIntent(ctx, uri))
        return views
    }

    private fun openIntent(ctx: Context, uri: Uri): PendingIntent =
        PendingIntent.getActivity(
            ctx, uri.hashCode(), Intent(Intent.ACTION_VIEW, uri).setPackage(ctx.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        /** Minta semua widget terpasang menyegarkan diri (aman dipanggil kapan saja; tanpa widget = tidak ada kerja). */
        fun requestUpdate(ctx: Context) {
            try {
                val mgr = AppWidgetManager.getInstance(ctx)
                val ids = mgr.getAppWidgetIds(ComponentName(ctx, ContinueWidget::class.java))
                if (ids.isEmpty()) return
                ctx.sendBroadcast(
                    Intent(ctx, ContinueWidget::class.java)
                        .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
                )
            } catch (_: Exception) {}
        }
    }
}
