package app.pwhs.core.receiver

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * LAN receiver: a phone (scanning the TV's QR) opens the upload page or POSTs an APK here;
 * the file is staged in cache and emitted via [TvReceiverState] for the TV UI to install.
 *
 * The reverse of the mobile sync [ApkHttpServer] (which *serves* files); same NanoHTTPD
 * stack. Uploads must carry the [token] from the QR — a lightweight guard so only a device
 * that scanned this TV's code can push.
 */
class ApkReceiverServer(
    private val context: Context,
    port: Int,
    private val token: String,
) : NanoHTTPD(port) {

    private val stageDir: File = File(context.cacheDir, "received").apply { mkdirs() }

    init {
        tempFileManagerFactory = TempFileManagerFactory {
            ProgressTempFileManager(stageDir)
        }
    }

    override fun serve(session: IHTTPSession): Response {
        return when {
            session.method == Method.GET && session.uri == "/" -> uploadPage(session)
            session.method == Method.GET && session.uri == "/ping" -> handlePing(session)
            session.method == Method.GET && session.uri == "/disconnect" -> handleDisconnect(session)
            session.method == Method.GET && session.uri == "/logo.png" -> serveLogo()
            session.method == Method.POST && session.uri == "/upload" -> handleUpload(session)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }
    }

    private fun serveLogo(): Response {
        return try {
            val drawable = context.packageManager.getApplicationIcon(context.applicationInfo)
            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 256
            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 256
            val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            val bytes = stream.toByteArray()
            newFixedLengthResponse(Response.Status.OK, "image/png", java.io.ByteArrayInputStream(bytes), bytes.size.toLong())
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Logo not found")
        }
    }

    private fun uploadPage(session: IHTTPSession): Response {
        recordClient(session)
        val htmlTemplate = try {
            context.assets.open("upload_page.html").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "<html><body><h2>Error loading UI</h2><p>${e.message}</p></body></html>"
        }
        val html = htmlTemplate.replace("{{TOKEN}}", token)
        return newFixedLengthResponse(Response.Status.OK, MIME_HTML, html)
    }

    private fun handlePing(session: IHTTPSession): Response {
        val reqToken = session.parms["token"] ?: session.parameters["token"]?.firstOrNull()
        if (reqToken == token) {
            recordClient(session)
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "pong")
        }
        return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Bad token")
    }

    private fun handleDisconnect(session: IHTTPSession): Response {
        val reqToken = session.parms["token"] ?: session.parameters["token"]?.firstOrNull()
        if (reqToken == token) {
            TvReceiverState.updateConnectedClient(null)
        }
        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "ok")
    }

    private fun recordClient(session: IHTTPSession) {
        val clientIp = session.remoteIpAddress ?: "Phone"
        val userAgent = session.headers["user-agent"]
        val deviceName = parseDeviceFromUserAgent(userAgent) ?: clientIp
        TvReceiverState.updateConnectedClient(ConnectedClient(ip = clientIp, deviceName = deviceName))
    }

    private fun parseDeviceFromUserAgent(ua: String?): String? {
        if (ua == null) return null
        return when {
            ua.contains("Android", ignoreCase = true) -> {
                val match = Regex(";\\s*([^;]+)\\s*Build").find(ua)?.groupValues?.get(1)
                match?.trim() ?: "Android Device"
            }
            ua.contains("iPhone", ignoreCase = true) -> "iPhone"
            ua.contains("iPad", ignoreCase = true) -> "iPad"
            ua.contains("Macintosh", ignoreCase = true) -> "Mac"
            ua.contains("Windows", ignoreCase = true) -> "Windows PC"
            ua.contains("Linux", ignoreCase = true) -> "Linux PC"
            else -> null
        }
    }

    private fun handleUpload(session: IHTTPSession): Response {
        if (session.parms["token"] != token && session.parameters["token"]?.firstOrNull() != token) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Bad token")
        }
        recordClient(session)
        val contentLength = session.headers["content-length"]?.toLongOrNull() ?: 0L
        TvReceiverState.currentExpectedBytes = contentLength.takeIf { it > 0 }
        TvReceiverState.emitReceivingProgress(
            ReceivingProgress(
                bytesReceived = 0L,
                totalBytes = contentLength.takeIf { it > 0 } ?: 1L,
                progress = 0f,
                percent = 0
            )
        )
        // NanoHTTPD writes multipart file parts to temp files; the map gives their paths.
        val files = HashMap<String, String>()
        return try {
            session.parseBody(files)
            val tempPath = files.values.firstOrNull()
                ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No file")
            val original = session.parameters["apk"]?.firstOrNull()?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() } ?: "received-${System.nanoTime()}.apk"
            val dest = File(stageDir, sanitize(original))
            File(tempPath).copyTo(dest, overwrite = true)
            
            TvReceiverState.emitReceivingProgress(
                ReceivingProgress(
                    bytesReceived = dest.length(),
                    totalBytes = dest.length(),
                    progress = 1f,
                    percent = 100
                )
            )
            Thread.sleep(350)
            TvReceiverState.emitReceivingProgress(null)
            TvReceiverState.emitReceived(
                ReceivedApk(path = dest.absolutePath, fileName = dest.name, sizeBytes = dest.length())
            )
            newFixedLengthResponse(Response.Status.OK, MIME_HTML, "<h2>Sent ✓ — confirm the install on your TV.</h2>")
        } catch (t: Throwable) {
            TvReceiverState.emitReceivingProgress(null)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Upload failed: ${t.message}")
        }
    }

    private fun sanitize(name: String): String =
        name.map { if (it.isLetterOrDigit() || it in "-_.") it else '_' }.joinToString("").take(120)

    private class ProgressTempFileManager(private val tempDir: File) : TempFileManager {
        private val tempFiles = mutableListOf<TempFile>()

        override fun clear() {
            for (file in tempFiles) {
                try {
                    file.delete()
                } catch (_: Exception) {}
            }
            tempFiles.clear()
            TvReceiverState.emitReceivingProgress(null)
        }

        override fun createTempFile(filename_hint: String?): TempFile {
            val tempFile = ProgressTempFile(tempDir)
            tempFiles.add(tempFile)
            return tempFile
        }
    }

    private class ProgressTempFile(tempDir: File) : TempFile {
        private val file = File.createTempFile("NanoHTTPD-", "", tempDir)
        private var outputStream: OutputStream? = null

        override fun delete() {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
            file.delete()
        }

        override fun getName(): String = file.absolutePath

        override fun open(): OutputStream {
            val fos = FileOutputStream(file)
            return object : OutputStream() {
                private var totalWritten = 0L
                private var lastProgressMs = 0L

                override fun write(b: Int) {
                    fos.write(b)
                    totalWritten++
                    notifyProgress(totalWritten)
                }

                override fun write(b: ByteArray, off: Int, len: Int) {
                    fos.write(b, off, len)
                    totalWritten += len
                    notifyProgress(totalWritten)
                }

                override fun flush() = fos.flush()

                override fun close() {
                    fos.close()
                }

                private fun notifyProgress(written: Long) {
                    val now = System.currentTimeMillis()
                    if (now - lastProgressMs > 100L) {
                        lastProgressMs = now
                        val total = TvReceiverState.currentExpectedBytes ?: written
                        TvReceiverState.emitReceivingProgress(
                            ReceivingProgress(
                                bytesReceived = written,
                                totalBytes = total,
                            )
                        )
                    }
                }
            }.also { outputStream = it }
        }
    }
}
