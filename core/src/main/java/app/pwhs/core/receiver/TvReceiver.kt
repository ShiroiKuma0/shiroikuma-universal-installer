package app.pwhs.core.receiver

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.util.UUID

/**
 * Owns the [ApkReceiverServer] lifecycle so callers (the TV foreground service) never touch
 * NanoHTTPD directly — keeps the server library an internal detail of :core. Computes the
 * LAN URL + upload token and publishes status via [TvReceiverState].
 *
 * Also registers a NSD (Network Service Discovery) service so phone apps on the
 * same Wi-Fi can auto-discover this TV without manual IP entry.
 */
object TvReceiver {

    private const val DEFAULT_PORT = 8787
    /** NSD service type — phone discovers services of this type. */
    const val NSD_SERVICE_TYPE = "_uinstaller._tcp."

    private var currentPort = DEFAULT_PORT
    private var server: ApkReceiverServer? = null
    private var nsdManager: NsdManager? = null
    private var nsdRegistered = false


    @Synchronized
    fun start(context: Context): ReceiverStatus {
        val currentServer = server
        if (currentServer != null && currentServer.isAlive) {
            val currentStatus = TvReceiverState.status.value
            if (currentStatus is ReceiverStatus.Running) return currentStatus
        }
        val ip = LanAddress.siteLocalIpv4() ?: "0.0.0.0"

        val candidatePorts = listOf(DEFAULT_PORT, 8788, 8789, 8790, 8791, 8792)
        for (port in candidatePorts) {
            try {
                server?.stop()
                val newServer = ApkReceiverServer(context.applicationContext, port)
                newServer.start(60_000, false)
                server = newServer
                currentPort = port
                val running = ReceiverStatus.Running(
                    ip = ip, port = port, url = "http://$ip:$port/",
                )
                TvReceiverState.setStatus(running)
                registerNsd(context, port, android.os.Build.MODEL, ip)
                return running
            } catch (e: Throwable) {
                android.util.Log.w("TvReceiver", "Port $port unavailable, trying next...", e)
            }
        }

        android.util.Log.e("TvReceiver", "All candidate ports failed to bind")
        TvReceiverState.setStatus(ReceiverStatus.Stopped)
        return ReceiverStatus.Stopped
    }

    @Synchronized
    fun stop() {
        unregisterNsd()
        runCatching { server?.stop() }
        server = null
        TvReceiverState.setStatus(ReceiverStatus.Stopped)
    }

    @Synchronized
    fun restart(context: Context): ReceiverStatus {
        val currentServer = server
        val ip = LanAddress.siteLocalIpv4() ?: "0.0.0.0"
        TvReceiverState.updateConnectedClient(null)
        TvReceiverState.emitReceivingProgress(null)

        if (currentServer != null && currentServer.isAlive) {
            val newRunning = ReceiverStatus.Running(
                ip = ip, port = currentPort, url = "http://$ip:$currentPort/",
            )
            TvReceiverState.setStatus(newRunning)
            registerNsd(context, currentPort, android.os.Build.MODEL, ip)
            return newRunning
        }
        return start(context)
    }

    // ── NSD Registration ────────────────────────────────────────────────

    private var activeRegistrationListener: NsdManager.RegistrationListener? = null

    private fun registerNsd(context: Context, port: Int, deviceName: String, ip: String) {
        unregisterNsd()
        try {
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "UIns-${deviceName.replace(" ", "_")}"
                serviceType = NSD_SERVICE_TYPE
                setPort(port)
                IpEncoder.encode(ip)?.let { setAttribute("ipcode", it) }
            }
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    android.util.Log.i("TvReceiver", "NSD registered: ${serviceInfo.serviceName}")
                    nsdRegistered = true
                }
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    android.util.Log.w("TvReceiver", "NSD registration failed: $errorCode")
                    nsdRegistered = false
                }
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                    android.util.Log.i("TvReceiver", "NSD unregistered: ${serviceInfo.serviceName}")
                    nsdRegistered = false
                }
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    android.util.Log.w("TvReceiver", "NSD unregistration failed: $errorCode")
                }
            }
            activeRegistrationListener = listener
            val mgr = (context.getSystemService(Context.NSD_SERVICE) as? NsdManager)
            nsdManager = mgr
            mgr?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            android.util.Log.w("TvReceiver", "Failed to register NSD", e)
        }
    }

    private fun unregisterNsd() {
        val listener = activeRegistrationListener ?: return
        activeRegistrationListener = null
        try {
            nsdManager?.unregisterService(listener)
        } catch (e: Exception) {
            android.util.Log.w("TvReceiver", "Failed to unregister NSD", e)
        }
        nsdRegistered = false
    }
}

