package app.pwhs.universalinstaller.wearos.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import app.pwhs.universalinstaller.wearos.R
import app.pwhs.universalinstaller.wearos.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.File

/**
 * Listens for incoming APK channels opened by the paired phone.
 *
 * The phone calls ChannelClient.openChannel(nodeId, "/apk-transfer/<filename>") and then
 * sends the raw APK bytes. This service reads them, writes to disk, and posts a notification.
 */
class WearReceiverService : WearableListenerService() {

    private val repository: WearApkRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        val path = channel.path
        if (!path.startsWith(CHANNEL_PATH_PREFIX)) return

        val fileName = path.removePrefix(CHANNEL_PATH_PREFIX)
        Log.d(TAG, "Receiving APK channel: $fileName")

        scope.launch {
            receiveApk(channel, fileName)
        }
    }

    private suspend fun receiveApk(channel: ChannelClient.Channel, fileName: String) =
        withContext(Dispatchers.IO) {
            val channelClient = Wearable.getChannelClient(this@WearReceiverService)
            val tempFile: File = repository.createTempApkFile(fileName)

            runCatching {
                // Tasks.await() is safe on IO dispatcher (blocking)
                val inputStream = Tasks.await(channelClient.getInputStream(channel))
                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Tasks.await(channelClient.close(channel))
            }.onFailure { e ->
                Log.e(TAG, "Failed to receive APK: ${e.message}", e)
                tempFile.delete()
                return@withContext
            }

            Log.d(TAG, "APK written to ${tempFile.absolutePath} (${tempFile.length()} bytes)")
            val apkInfo = repository.addApk(tempFile)
            if (apkInfo != null) {
                postNotification(apkInfo)
            } else {
                Log.e(TAG, "Could not parse APK info from ${tempFile.name}")
                tempFile.delete()
            }
        }

    private fun postNotification(apkInfo: WearApkInfo) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "APK Received", NotificationManager.IMPORTANCE_DEFAULT)
        )

        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.apk_received_title))
            .setContentText(apkInfo.appName)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        nm.notify(apkInfo.id.hashCode(), notification)
    }

    companion object {
        private const val TAG = "WearReceiverService"
        const val CHANNEL_PATH_PREFIX = "/apk-transfer/"
        private const val CHANNEL_ID = "wear_apk_received"
    }
}
