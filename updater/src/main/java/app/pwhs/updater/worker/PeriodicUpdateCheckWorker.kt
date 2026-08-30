package app.pwhs.updater.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.pwhs.updater.data.repo.AppUpdateRepository
import app.pwhs.updater.presentation.UpdatesActivity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Background worker that periodically checks for new releases across all tracked apps.
 */
class PeriodicUpdateCheckWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: AppUpdateRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            val apps = repository.checkAllUpdates()
            val appsWithUpdates = apps.filter { it.hasUpdate }

            if (appsWithUpdates.isNotEmpty()) {
                sendUpdateNotification(appsWithUpdates.map { it.appName })
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "PeriodicUpdateCheckWorker failed")
            Result.retry()
        }
    }

    private fun sendUpdateNotification(appNames: List<String>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channelId = "app_updates_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Notifications for tracked app updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, UpdatesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (appNames.size == 1) {
            "Update available for ${appNames.first()}"
        } else {
            "${appNames.size} app updates available"
        }

        val content = if (appNames.size == 1) {
            "Tap to download and install the latest release."
        } else {
            appNames.take(3).joinToString(", ") + if (appNames.size > 3) " and more..." else ""
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1001, notification)
    }

    companion object {
        private const val WORK_NAME = "PeriodicUpdateCheckWork"

        fun schedule(
            context: Context,
            intervalHours: Long = 12,
            requiresWifi: Boolean = false,
            requiresCharging: Boolean = false,
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (requiresWifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresCharging(requiresCharging)
                .build()

            val request = PeriodicWorkRequestBuilder<PeriodicUpdateCheckWorker>(
                repeatInterval = intervalHours.coerceAtLeast(6),
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
