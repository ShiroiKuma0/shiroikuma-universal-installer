package app.pwhs.core.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Vendor quirks that change what advice we can give the user.
 * Shared between Mobile and TV.
 */
object DeviceCompat {

    private val XIAOMI_BRANDS = listOf("xiaomi", "redmi", "poco")

    /**
     * True on Xiaomi/Redmi/POCO hardware, i.e. anything likely running MIUI or HyperOS.
     *
     * Deliberately a [Build.MANUFACTURER]/[Build.BRAND] match rather than a read of
     * `ro.miui.ui.version.name` — the latter needs reflection into `SystemProperties`
     * (hidden API) and this only gates a piece of advisory text, where a false positive
     * costs nothing more than an irrelevant tip.
     */
    val isXiaomi: Boolean by lazy {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        XIAOMI_BRANDS.any { it in manufacturer || it in brand }
    }

    /**
     * Developer options, where MIUI/HyperOS hides the "Turn on MIUI optimization" toggle.
     * Returns null when no activity handles it (developer options not unlocked yet), so
     * callers can hide the shortcut instead of throwing on launch.
     */
    fun developerOptionsIntent(context: Context): Intent? {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent.takeIf { it.resolveActivity(context.packageManager) != null }
    }
}
