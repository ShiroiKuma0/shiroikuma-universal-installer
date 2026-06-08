package app.pwhs.core.receiver

import java.net.Inet4Address
import java.net.NetworkInterface

/** Best-effort discovery of the device's LAN IPv4 address, for building the receiver URL. */
object LanAddress {
    fun siteLocalIpv4(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull()
}
