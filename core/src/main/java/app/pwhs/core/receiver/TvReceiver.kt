package app.pwhs.core.receiver

import android.content.Context
import java.util.UUID

/**
 * Owns the [ApkReceiverServer] lifecycle so callers (the TV foreground service) never touch
 * NanoHTTPD directly — keeps the server library an internal detail of :core. Computes the
 * LAN URL + upload token and publishes status via [TvReceiverState].
 */
object TvReceiver {

    private const val PORT = 8787
    private var server: ApkReceiverServer? = null

    @Synchronized
    fun start(context: Context): ReceiverStatus {
        server?.let { return TvReceiverState.status.value }
        val ip = LanAddress.siteLocalIpv4() ?: "0.0.0.0"
        val token = UUID.randomUUID().toString().substring(0, 8)
        return runCatching {
            ApkReceiverServer(context.applicationContext, PORT, token).also {
                it.start(60_000, false)
                server = it
            }
            ReceiverStatus.Running(
                ip = ip, port = PORT, token = token, url = "http://$ip:$PORT/?token=$token",
            ).also { TvReceiverState.setStatus(it) }
        }.getOrElse {
            TvReceiverState.setStatus(ReceiverStatus.Stopped)
            ReceiverStatus.Stopped
        }
    }

    @Synchronized
    fun stop() {
        runCatching { server?.stop() }
        server = null
        TvReceiverState.setStatus(ReceiverStatus.Stopped)
    }
}
