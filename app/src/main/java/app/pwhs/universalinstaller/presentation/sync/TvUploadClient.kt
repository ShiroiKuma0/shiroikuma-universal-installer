package app.pwhs.universalinstaller.presentation.sync

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Uploads an APK to a TV's receiver server (the QR encodes its `url` + `token`). Streams the
 * file via a multipart/form-data POST over HttpURLConnection — no extra deps, no whole-file
 * buffering, so large APKs don't OOM.
 */
object TvUploadClient {

    sealed interface Result {
        data object Success : Result
        data class Failure(val message: String) : Result
    }

    /**
     * @param scanned the exact string from the TV QR, e.g. `http://192.168.1.5:8787/?token=ab12`.
     */
    suspend fun upload(
        context: Context,
        scanned: String,
        apk: Uri,
        fileName: String,
        onProgress: ((bytesUploaded: Long, totalBytes: Long, percent: Int) -> Unit)? = null,
    ): Result = withContext(Dispatchers.IO) {
        val parsed = runCatching { Uri.parse(scanned) }.getOrNull()
        val host = parsed?.host
        val port = parsed?.port?.takeIf { it > 0 }
        val token = parsed?.getQueryParameter("token").orEmpty()
        if (host == null || port == null) {
            return@withContext Result.Failure("Not a valid TV code")
        }

        val resolver = context.contentResolver
        val size = runCatching {
            resolver.query(apk, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (idx != -1) cursor.getLong(idx) else null
                } else null
            }
        }.getOrNull() ?: runCatching {
            resolver.openAssetFileDescriptor(apk, "r")?.use { it.length }
        }.getOrNull()?.takeIf { it >= 0 } ?: -1L

        val boundary = "----uitv${System.currentTimeMillis()}"
        val crlf = "\r\n"

        val preamble = buildString {
            append("--").append(boundary).append(crlf)
            append("Content-Disposition: form-data; name=\"apk\"; filename=\"").append(fileName).append("\"").append(crlf)
            append("Content-Type: application/octet-stream").append(crlf).append(crlf)
        }.toByteArray(Charsets.UTF_8)
        val epilogue = "$crlf--$boundary--$crlf".toByteArray(Charsets.UTF_8)

        val totalLength = if (size >= 0) preamble.size.toLong() + size + epilogue.size.toLong() else -1L
        android.util.Log.d("TvUploadClient", "Starting upload: file=$fileName, size=$size, totalLength=$totalLength, target=http://$host:$port/upload")

        val uploadUrl = if (token.isNotBlank()) "http://$host:$port/upload?token=$token" else "http://$host:$port/upload"
        return@withContext try {
            val conn = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 120_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                if (totalLength >= 0) {
                    setFixedLengthStreamingMode(totalLength)
                } else {
                    setChunkedStreamingMode(64 * 1024)
                }
            }
            DataOutputStream(conn.outputStream).use { out ->
                out.write(preamble)
                resolver.openInputStream(apk)?.use { input ->
                    val buffer = ByteArray(32 * 1024)
                    var bytesCopied = 0L
                    var lastProgressMs = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } >= 0) {
                        out.write(buffer, 0, read)
                        bytesCopied += read
                        val now = System.currentTimeMillis()
                        if (now - lastProgressMs > 100L || bytesCopied == size) {
                            lastProgressMs = now
                            val pct = if (size > 0) ((bytesCopied * 100) / size).toInt().coerceIn(0, 100) else 0
                            onProgress?.invoke(bytesCopied, size, pct)
                        }
                    }
                } ?: return@withContext Result.Failure("Could not read the selected file")
                out.write(epilogue)
                out.flush()
            }
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) Result.Success
            else Result.Failure("TV rejected upload (HTTP $code)")
        } catch (t: Throwable) {
            Result.Failure(t.message ?: "Upload failed")
        }
    }
}
