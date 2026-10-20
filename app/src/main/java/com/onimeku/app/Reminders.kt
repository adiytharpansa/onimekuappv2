package com.onimeku.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.onimeku.app.data.AppState
import com.onimeku.app.data.Catalog
import com.onimeku.app.data.Store
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Reminder mingguan lewat WorkManager: tetap jalan walau app ditutup atau HP restart. */
object Reminders {
    private const val TAG = "reminder"
    private const val TAG_EP = "reminder_ep"
    /** Menit sebelum jam tayang notifikasi per-episode dikirim. */
    const val LEAD_MIN = 15

    private fun delayMs(day: Int, time: String): Long {
        val now = LocalDateTime.now()
        // time bisa kosong bila updatedAt katalog tak terbaca: jangan crash.
        val parts = time.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val add = ((day - now.dayOfWeek.value % 7) + 7) % 7
        var t = now.toLocalDate().plusDays(add.toLong()).atTime(h, m)
        if (!t.isAfter(now)) t = t.plusDays(7)
        return Duration.between(now, t).toMillis()
    }

    fun schedule(ctx: Context, pi: Int, id: String, anime: com.onimeku.app.data.Anime? = null) {
        val c = anime ?: Catalog.byId[id] ?: return
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs(c.day, c.time), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("pi" to pi, "id" to id))
            .addTag(TAG).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("remind:$pi:$id", ExistingWorkPolicy.REPLACE, req)
    }

    /** Pengingat TEPAT untuk satu episode (jam tayang asli dari jadwal). WorkManager bisa terlambat beberapa menit saat Doze. */
    fun scheduleEpisode(ctx: Context, pi: Int, id: String, title: String, ep: Int, airingAtSec: Long) {
        val now = System.currentTimeMillis()
        val airMs = airingAtSec * 1000
        if (airMs <= now) return
        val lead = if (airMs - LEAD_MIN * 60_000L > now) LEAD_MIN else 0
        val req = OneTimeWorkRequestBuilder<EpisodeReminderWorker>()
            .setInitialDelay(airMs - lead * 60_000L - now, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("pi" to pi, "id" to id, "ep" to ep, "title" to title, "lead" to lead))
            .addTag(TAG_EP).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("remind-ep:$pi:$id:$ep", ExistingWorkPolicy.REPLACE, req)
    }

    fun cancelEpisode(ctx: Context, pi: Int, id: String, ep: Int) {
        WorkManager.getInstance(ctx).cancelUniqueWork("remind-ep:$pi:$id:$ep")
    }

    fun cancel(ctx: Context, pi: Int, id: String) {
        WorkManager.getInstance(ctx).cancelUniqueWork("remind:$pi:$id")
    }

    fun rescheduleAll(ctx: Context, s: AppState) {
        // Katalog belum termuat (mis. dibuka luring tanpa cache):
        // jangan batalkan apa pun — pengingat yang ada harus utuh.
        if (Catalog.all.isEmpty()) return
        WorkManager.getInstance(ctx).cancelAllWorkByTag(TAG)
        if (!s.settings.alerts) return
        s.profiles.forEachIndexed { pi, p -> p.remind.forEach { schedule(ctx, pi, it) } }
    }
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val pi = inputData.getInt("pi", 0)
        val id = inputData.getString("id") ?: return Result.success()
        val st = Store(applicationContext).load()
        val still = st.settings.alerts && st.profiles.getOrNull(pi)?.remind?.contains(id) == true
        if (!still) return Result.success()
        // Katalog tidak dimuat di proses worker; ambil detail judul ini dari API.
        val c = try { com.onimeku.app.data.ApiClient.fetchDetail(id) } catch (e: Exception) { null }
            ?: return Result.success()
        // Belum tayang: jangan kirim "Episode N is out now"; cek lagi minggu depan sampai benar-benar rilis.
        if (c.apiStatus == "NOT_YET_RELEASED") {
            Reminders.schedule(applicationContext, pi, id, c)
            return Result.success()
        }
        Notifier.show(applicationContext, "New episode: ${c.title}", "Episode ${c.ep} is out now")
        // Katalog kosong di proses worker: kirim detail yang baru diambil agar
        // jadwal minggu depan benar-benar dibuat (sebelumnya diam-diam batal).
        Reminders.schedule(applicationContext, pi, id, c)
        return Result.success()
    }
}

/** Notifikasi satu episode: dikirim sesuai jadwal; dibatalkan diam-diam bila pengingat/notifikasi sudah dimatikan. */
class EpisodeReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val pi = inputData.getInt("pi", 0)
        val id = inputData.getString("id") ?: return Result.success()
        val ep = inputData.getInt("ep", 0)
        val title = inputData.getString("title") ?: return Result.success()
        val lead = inputData.getInt("lead", 0)
        val st = Store(applicationContext).load()
        val still = st.settings.alerts && st.profiles.getOrNull(pi)?.remindEp?.any { it.startsWith("$id|$ep|") } == true
        if (!still) return Result.success()
        I18n.lang = st.settings.lang
        val body = if (lead > 0) I18n.t("cal_notif_soon", "Episode %1\$d airs in %2\$d minutes").format(ep, lead)
        else I18n.t("cal_notif_now", "Episode %d is out now").format(ep)
        Notifier.show(applicationContext, title, body)
        return Result.success()
    }
}
