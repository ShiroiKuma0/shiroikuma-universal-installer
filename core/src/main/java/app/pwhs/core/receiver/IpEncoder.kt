package app.pwhs.core.receiver

/**
 * Encodes the last two octets of a local IPv4 address into a 4-letter mnemonic code
 * (e.g., 192.168.1.128 -> "ERHE") and decodes it back using the current device's subnet prefix.
 */
object IpEncoder {
    private const val HEX = "0123456789ABCDEF"
    private const val SUBSTITUTION = "ERYUPDFGHKLJNWXC"

    fun encode(ip: String?): String? {
        if (ip.isNullOrBlank()) return null
        val parts = ip.split(".")
        if (parts.size != 4) return null
        val o3 = parts[2].toIntOrNull() ?: return null
        val o4 = parts[3].toIntOrNull() ?: return null
        if (o3 !in 0..255 || o4 !in 0..255) return null

        val hexStr = "%02X%02X".format(o3, o4)
        val sb = StringBuilder(4)
        for (ch in hexStr) {
            val idx = HEX.indexOf(ch)
            if (idx == -1) return null
            sb.append(SUBSTITUTION[idx])
        }
        return sb.toString()
    }

    fun decode(code: String?, localSubnetPrefix: String? = null): String? {
        if (code.isNullOrBlank() || code.length != 4) return null
        val cleanCode = code.uppercase()
        val hexSb = StringBuilder(4)
        for (ch in cleanCode) {
            val idx = SUBSTITUTION.indexOf(ch)
            if (idx == -1) return null
            hexSb.append(HEX[idx])
        }
        return runCatching {
            val o3 = hexSb.substring(0, 2).toInt(16)
            val o4 = hexSb.substring(2, 4).toInt(16)
            val prefix = if (!localSubnetPrefix.isNullOrBlank()) {
                val parts = localSubnetPrefix.split(".")
                if (parts.size >= 2) "${parts[0]}.${parts[1]}" else "192.168"
            } else {
                "192.168"
            }
            "$prefix.$o3.$o4"
        }.getOrNull()
    }
}
