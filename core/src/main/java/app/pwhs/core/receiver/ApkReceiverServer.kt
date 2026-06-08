package app.pwhs.core.receiver

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.File

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

    override fun serve(session: IHTTPSession): Response {
        return when {
            session.method == Method.GET && session.uri == "/" -> uploadPage()
            session.method == Method.POST && session.uri == "/upload" -> handleUpload(session)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }
    }

    private fun uploadPage(): Response {
        val html = """
            <!doctype html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>Send app to TV</title>
            <style>
              body{font-family:system-ui,sans-serif;margin:0;background:#111;color:#eee;
                   display:flex;min-height:100vh;align-items:center;justify-content:center}
              .card{background:#1d1d1d;padding:28px;border-radius:16px;width:88%;max-width:420px}
              h1{font-size:20px;margin:0 0 16px}
              input[type=file]{width:100%;margin:12px 0;color:#ccc}
              button{width:100%;padding:14px;border:0;border-radius:10px;background:#4c8bf5;
                     color:#fff;font-size:16px;font-weight:600}
              .hint{color:#888;font-size:13px;margin-top:12px}
            </style></head><body>
            <form class="card" method="post" enctype="multipart/form-data" action="/upload?token=$token">
              <h1>Send an app to your TV</h1>
              <input type="file" name="apk" accept=".apk,.apks,.xapk,.apkm" required>
              <button type="submit">Send to TV</button>
              <div class="hint">.apk and bundles (.apks/.xapk/.apkm) supported. The TV will ask to confirm the install.</div>
            </form></body></html>
        """.trimIndent()
        return newFixedLengthResponse(Response.Status.OK, MIME_HTML, html)
    }

    private fun handleUpload(session: IHTTPSession): Response {
        if (session.parms["token"] != token && session.parameters["token"]?.firstOrNull() != token) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Bad token")
        }
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
            TvReceiverState.emitReceived(
                ReceivedApk(path = dest.absolutePath, fileName = dest.name, sizeBytes = dest.length())
            )
            newFixedLengthResponse(Response.Status.OK, MIME_HTML, "<h2>Sent ✓ — confirm the install on your TV.</h2>")
        } catch (t: Throwable) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Upload failed: ${t.message}")
        }
    }

    private fun sanitize(name: String): String =
        name.map { if (it.isLetterOrDigit() || it in "-_.") it else '_' }.joinToString("").take(120)
}
