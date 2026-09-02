package app.pwhs.universalinstaller.presentation.install

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sends an APK file to the nearest connected Wear OS watch via Wearable ChannelClient.
 *
 * The watch-side [app.pwhs.universalinstaller.wearos.data.WearReceiverService] listens on
 * channels with path prefix "/apk-transfer/<filename>".
 */
object WearSenderService {

    private const val TAG = "WearSenderService"
    private const val CHANNEL_PATH_PREFIX = "/apk-transfer/"

    /**
     * Result of a send attempt.
     */
    sealed interface SendResult {
        data object Success : SendResult
        data object NoWatchFound : SendResult
        data class Error(val message: String) : SendResult
    }

    /**
     * Returns true if at least one Wear OS node is reachable.
     * Cheap to call — runs on IO dispatcher.
     */
    suspend fun isWatchAvailable(context: Context): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
            nodes.isNotEmpty()
        }.getOrDefault(false)
    }

    /**
     * Streams the APK at [apkUri] to the nearest connected watch.
     *
     * @param context Application context
     * @param apkUri  URI of the APK file (must be readable by this process)
     * @param fileName Display name used to reconstruct the file on the watch side
     * @param onProgress Optional progress callback (0.0 – 1.0), called on IO thread
     */
    suspend fun send(
        context: Context,
        apkUri: Uri,
        fileName: String,
        onProgress: ((Float) -> Unit)? = null,
    ): SendResult = withContext(Dispatchers.IO) {
        // 1. Find nearest connected node
        val nodes = runCatching {
            Tasks.await(Wearable.getNodeClient(context).connectedNodes)
        }.getOrElse { e ->
            Log.e(TAG, "Could not get nodes: ${e.message}", e)
            return@withContext SendResult.Error("Cannot connect to Wearable API: ${e.message}")
        }

        val targetNode = nodes.firstOrNull()
            ?: return@withContext SendResult.NoWatchFound

        Log.d(TAG, "Sending to node: ${targetNode.displayName} (${targetNode.id})")

        // 2. Sanitize filename for the channel path
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val channelPath = "$CHANNEL_PATH_PREFIX$safeName"

        // 3. Open channel to watch
        val channelClient = Wearable.getChannelClient(context)
        val channel = runCatching {
            Tasks.await(channelClient.openChannel(targetNode.id, channelPath))
        }.getOrElse { e ->
            Log.e(TAG, "Failed to open channel: ${e.message}", e)
            return@withContext SendResult.Error("Failed to open channel: ${e.message}")
        }

        // 4. Stream APK bytes
        runCatching {
            val outputStream = Tasks.await(channelClient.getOutputStream(channel))
            val inputStream = context.contentResolver.openInputStream(apkUri)
                ?: return@withContext SendResult.Error("Cannot read APK file")

            val totalBytes = context.contentResolver.openFileDescriptor(apkUri, "r")
                ?.statSize?.takeIf { it > 0 }
            var bytesSent = 0L

            outputStream.use { out ->
                inputStream.use { input ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        bytesSent += read
                        if (totalBytes != null && totalBytes > 0) {
                            onProgress?.invoke(bytesSent.toFloat() / totalBytes)
                        }
                    }
                    out.flush()
                }
            }

            Tasks.await(channelClient.close(channel))
            Log.d(TAG, "APK sent successfully ($bytesSent bytes)")
        }.fold(
            onSuccess = { SendResult.Success },
            onFailure = { e ->
                Log.e(TAG, "Send failed: ${e.message}", e)
                runCatching { Tasks.await(channelClient.close(channel)) }
                SendResult.Error(e.message ?: "Transfer failed")
            }
        )
    }
}
