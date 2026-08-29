package app.pwhs.universalinstaller.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Creates an install Intent for single APKs with [Intent.EXTRA_INSTALLER_PACKAGE_NAME]
 * and [Intent.EXTRA_NOT_UNKNOWN_SOURCE] attached, allowing no-root installations
 * via the system package installer to specify the source package (e.g. for Android Auto compatibility).
 */
object SystemIntentInstaller {

    private const val MIME = "application/vnd.android.package-archive"
    const val VENDING_PKG = "com.android.vending"

    @Suppress("DEPRECATION")
    fun createInstallIntent(
        context: Context,
        apkUri: Uri,
        installerPackageName: String = VENDING_PKG,
    ): Intent {
        val baseIntent = SystemInstallerFallback.resolve(context, apkUri)
            ?: Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(apkUri, MIME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

        return baseIntent.apply {
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, installerPackageName)
        }
    }
}
