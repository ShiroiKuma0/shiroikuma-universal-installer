package app.pwhs.universalinstaller.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import app.pwhs.universalinstaller.R

object AndroidAutoCompat {

    const val GEARHEAD_PKG = "com.google.android.projection.gearhead"
    const val VENDING_PKG = "com.android.vending"
    const val SHELL_PKG = "com.android.shell"
    const val GOOGLE_INSTALLER_PKG = "com.google.android.packageinstaller"

    private val aaSupportCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    @Volatile
    private var isAaInstalledCached: Boolean? = null

    data class CompatibilityResult(
        val isCompatible: Boolean,
        @StringRes val titleRes: Int,
        @StringRes val descriptionRes: Int,
    )

    /**
     * Rules from Android Auto:
     * - The "Installed by" (installingPackageName) MUST be Google Play Store (`com.android.vending`).
     * - If "Requested by" (initiatingPackageName) is `com.android.shell` (ADB), Android Auto blocks it.
     */
    fun checkCompatibility(
        installingPackage: String?,
        initiatingPackage: String?,
    ): CompatibilityResult {
        val isPlayStoreInstalled = installingPackage == VENDING_PKG
        val isAdb = initiatingPackage == SHELL_PKG || installingPackage == SHELL_PKG

        val isCompatible = isPlayStoreInstalled && !isAdb

        return if (isCompatible) {
            CompatibilityResult(
                isCompatible = true,
                titleRes = R.string.aa_compatibility_ok,
                descriptionRes = R.string.aa_compatibility_ok_desc,
            )
        } else {
            CompatibilityResult(
                isCompatible = false,
                titleRes = R.string.aa_compatibility_error,
                descriptionRes = R.string.aa_compatibility_error_desc,
            )
        }
    }

    fun isAndroidAutoInstalled(context: Context): Boolean {
        isAaInstalledCached?.let { return it }
        val installed = try {
            context.packageManager.getPackageInfo(GEARHEAD_PKG, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        isAaInstalledCached = installed
        return installed
    }

    fun getSettingsIntent(context: Context): Intent? {
        val directIntent = Intent("com.google.android.projection.gearhead.SETTINGS")
            .setPackage(GEARHEAD_PKG)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val pm = context.packageManager
        if (directIntent.resolveActivity(pm) != null) {
            return directIntent
        }

        val launchIntent = pm.getLaunchIntentForPackage(GEARHEAD_PKG)
        if (launchIntent != null) {
            return launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return null
    }

    /**
     * Checks if the specified installed package declares Android Auto support
     * (e.g. MediaBrowserService, CarAppService, or car application metadata).
     * Cached in-memory to prevent repeated IPC binder calls on the UI thread.
     */
    fun supportsAndroidAuto(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return aaSupportCache.getOrPut(packageName) {
            val pm = context.packageManager
            runCatching {
                val mediaIntent = Intent("android.media.browse.MediaBrowserService").setPackage(packageName)
                val carAppIntent = Intent("androidx.car.app.CarAppService").setPackage(packageName)
                val hasService = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentServices(mediaIntent, PackageManager.ResolveInfoFlags.of(0)).isNotEmpty() ||
                        pm.queryIntentServices(carAppIntent, PackageManager.ResolveInfoFlags.of(0)).isNotEmpty()
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentServices(mediaIntent, 0).isNotEmpty() ||
                        pm.queryIntentServices(carAppIntent, 0).isNotEmpty()
                }
                if (hasService) return@getOrPut true

                val appInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                }
                val metaData = appInfo.metaData
                metaData != null && (
                    metaData.containsKey("com.google.android.gms.car.application") ||
                    metaData.containsKey("androidx.car.app.minCarApiLevel") ||
                    metaData.containsKey("com.google.android.gms.car.notification.SmallIcon")
                )
            }.getOrDefault(false)
        }
    }
}
