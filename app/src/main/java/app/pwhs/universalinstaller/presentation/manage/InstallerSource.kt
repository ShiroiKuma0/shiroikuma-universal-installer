package app.pwhs.universalinstaller.presentation.manage

import android.content.Intent
import androidx.core.net.toUri

/**
 * Friendly description + deep-link for an app's known installer source. Returned only for
 * stores we can route the user back to; sideload / unknown installers fall through to null
 * because we have no canonical "view this app" URL for them.
 */
data class InstallerInfo(
    val displayName: String,
    val intent: Intent? = null,
)

/**
 * Map an installer package name → store name + optional listing intent.
 * Stores we can route the user back to return an intent.
 * Sideload / unknown installers return just the display name without an intent.
 */
fun resolveInstallerInfo(installerPackage: String?, packageName: String): InstallerInfo? {
    return when (installerPackage) {
        "com.android.vending" -> InstallerInfo(
            displayName = "Play Store",
            intent = Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$packageName".toUri(),
            ).setPackage("com.android.vending"),
        )
        "org.fdroid.fdroid",
        "org.fdroid.basic" -> InstallerInfo(
            displayName = "F-Droid",
            intent = Intent(
                Intent.ACTION_VIEW,
                "https://f-droid.org/packages/$packageName/".toUri(),
            ).setPackage(installerPackage),
        )
        "com.aurora.store" -> InstallerInfo(
            displayName = "Aurora Store",
            intent = Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$packageName".toUri(),
            ),
        )
        "app.pwhs.universalinstaller" -> InstallerInfo(displayName = "Universal Installer")
        "app.pwhs.universalinstaller.debug" -> InstallerInfo(displayName = "Universal Installer (Debug)")
        "app.obtainium" -> InstallerInfo(displayName = "Obtainium")
        "com.android.packageinstaller", "com.google.android.packageinstaller" -> InstallerInfo(displayName = "Package Installer")
        "com.android.shell" -> InstallerInfo(displayName = "ADB (Shell)")
        "com.termux" -> InstallerInfo(displayName = "Termux")
        "com.google.android.apps.nbu.files", "com.android.documentsui", "com.marc.files" -> InstallerInfo(displayName = "File Manager")
        null -> null
        else -> InstallerInfo(displayName = installerPackage)
    }
}
