package app.pwhs.universalinstaller.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

object MicroGCompat {
    const val PACKAGE_NAME_MICROG_VENDING = "com.android.vending"
    const val ACTION_INSTALL_PACKAGE = "org.microg.vending.action.INSTALL_PACKAGE"
    const val MIMETYPE_APK = "application/vnd.android.package-archive"

    /**
     * Checks if the device has microG companion (FakeStore / com.android.vending)
     * installed and ready to handle [ACTION_INSTALL_PACKAGE].
     */
    fun isAvailable(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val intent = Intent(ACTION_INSTALL_PACKAGE).apply {
                setPackage(PACKAGE_NAME_MICROG_VENDING)
                type = MIMETYPE_APK
            }
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.resolveActivity(intent, 0)
            }
            resolveInfo != null
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Builds the Intent to pass APK URIs to microG Companion.
     */
    fun buildInstallIntent(uris: List<Uri>): Intent {
        val uriList = ArrayList(uris)
        return Intent(ACTION_INSTALL_PACKAGE).apply {
            setPackage(PACKAGE_NAME_MICROG_VENDING)
            type = MIMETYPE_APK
            if (uriList.size == 1) {
                data = uriList.first()
            }
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
