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

    fun cancel(ctx: Context, pi: Int, id: String) {
        WorkManager.getInstance(ctx).cancelUniqueWork("remind:$pi:$id")
    }

    fun rescheduleAll(ctx: Context, s: AppState) {
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
        Notifier.show(applicationContext, "New episode: ${c.title}", "Episode ${c.ep} is out now")
        // Katalog kosong di proses worker: kirim detail yang baru diambil agar
        // jadwal minggu depan benar-benar dibuat (sebelumnya diam-diam batal).
        Reminders.schedule(applicationContext, pi, id, c)
        return Result.success()
    }
}
