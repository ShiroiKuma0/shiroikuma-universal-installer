package app.pwhs.universalinstaller.presentation.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Discovered TV on the local network.
 * @param name   Human-readable name (e.g. "MiTV 4K")
 * @param host   IP address (e.g. "192.168.1.128")
 * @param port   HTTP server port (e.g. 8787)
 * @param token  Authentication token broadcasted by TV
 * @param ipCode 4-letter short IP mnemonic code
 */
data class DiscoveredTv(
    val name: String,
    val host: String,
    val port: Int,
    val token: String = "",
    val ipCode: String = "",
)

/**
 * Uses Android NSD (Network Service Discovery / mDNS) to find TVs running
 * Universal Installer receiver on the local network.
 */
object TvDiscovery {

    private const val TAG = "TvDiscovery"
    private const val SERVICE_TYPE = "_uinstaller._tcp."

    fun discover(context: Context): Flow<List<DiscoveredTv>> = callbackFlow {
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val found = mutableMapOf<String, DiscoveredTv>()
        val resolveQueue = Channel<NsdServiceInfo>(Channel.UNLIMITED)

        var isResolving = false

        fun processNextResolve() {
            if (isResolving) return
            val next = resolveQueue.tryReceive().getOrNull() ?: return
            isResolving = true

            nsdManager.resolveService(next, object : NsdManager.ResolveListener {
                override fun onResolveFailed(si: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "NSD resolve failed for ${si.serviceName}: $errorCode")
                    isResolving = false
                    processNextResolve()
                }

                override fun onServiceResolved(si: NsdServiceInfo) {
                    val host = si.host?.hostAddress
                    if (host != null) {
                        val token = runCatching {
                            si.attributes?.get("token")?.let { String(it, Charsets.UTF_8) }
                        }.getOrNull().orEmpty()

                        val ipCode = runCatching {
                            si.attributes?.get("ipcode")?.let { String(it, Charsets.UTF_8) }
                        }.getOrNull().orEmpty()

                        val tv = DiscoveredTv(
                            name = si.serviceName
                                .removePrefix("UIns-")
                                .replace("_", " "),
                            host = host,
                            port = si.port,
                            token = token,
                            ipCode = ipCode,
                        )
                        found[si.serviceName] = tv
                        trySend(found.values.toList())
                    }
                    isResolving = false
                    processNextResolve()
                }
            })
        }

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "NSD discovery started for $serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "NSD discovery stopped for $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "NSD found: ${serviceInfo.serviceName}")
                resolveQueue.trySend(serviceInfo)
                processNextResolve()
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "NSD lost: ${serviceInfo.serviceName}")
                found.remove(serviceInfo.serviceName)
                trySend(found.values.toList())
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "NSD start failed: $errorCode")
                close()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "NSD stop failed: $errorCode")
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

        awaitClose {
            Log.d(TAG, "Stopping NSD discovery")
            runCatching { nsdManager.stopServiceDiscovery(discoveryListener) }
            resolveQueue.close()
        }
    }
}
