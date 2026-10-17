package com.onimeku.app.data

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.work.*
import com.onimeku.app.MainActivity
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Pengingat rilis mingguan: jalan sekali, lalu menjadwalkan dirinya lagi +7 hari. (WorkManager: waktu bisa meleset beberapa menit.) */
@UnstableApi
class ReminderWorker(private val c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result {
        val a = Repo.of(c).get(inputData.getString("id") ?: return Result.failure()) ?: return Result.failure()
        if (a.airDay == null || a.airTime == null) return Result.failure()
        val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (SettingsStore.of(c).flow.first().alerts && granted) {
            val nm = NotificationManagerCompat.from(c)
            nm.createNotificationChannel(NotificationChannelCompat.Builder("reminders", NotificationManagerCompat.IMPORTANCE_DEFAULT).setName("Release reminders").build())
            val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            nm.notify(a.id.hashCode(), NotificationCompat.Builder(c, "reminders").setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Episode baru: ${a.title}").setContentText("Tayang sekarang pukul ${a.airTime}").setContentIntent(open).setAutoCancel(true).build())
        }
        schedule(c, a); return Result.success()
    }
    companion object {
        fun schedule(c: Context, a: Anime) {
            val t = Calendar.getInstance().apply { val p = a.airTime!!.split(":")
                set(Calendar.DAY_OF_WEEK, a.airDay!! + 1); set(Calendar.HOUR_OF_DAY, p[0].toInt()); set(Calendar.MINUTE, p[1].toInt()); set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis() + 60_000) add(Calendar.DAY_OF_YEAR, 7) }
            val req = OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(t.timeInMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("id" to a.id)).build()
            WorkManager.getInstance(c).enqueueUniqueWork("remind:${a.id}", ExistingWorkPolicy.REPLACE, req)
        }
        fun cancel(c: Context, id: String) { WorkManager.getInstance(c).cancelUniqueWork("remind:$id") }
    }
}
