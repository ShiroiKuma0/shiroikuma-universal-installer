package app.pwhs.universalinstaller.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import timber.log.Timber

/** What we can tell about Dhizuku right now, mirroring how Shizuku readiness is reported. */
enum class DhizukuState {
    /** Below API 26, where Dhizuku cannot run at all. */
    UNSUPPORTED,

    /** The Dhizuku app is not on the device. */
    NOT_INSTALLED,

    /** Installed, but not running as device owner or the service is not up. */
    NOT_RUNNING,

    /** Running, but this app has not been granted permission yet. */
    NOT_AUTHORIZED,

    /** Ready to install. */
    READY,
}

/**
 * Thin wrapper over the Dhizuku API.
 *
 * Dhizuku delegates *device owner* privileges rather than shell ones, which is why it can install
 * silently without ADB — and also why it can do less than Shizuku: the plugin exposes only
 * `requestDowngrade` for installs. Anything the UI offers beyond that has no effect here, so the
 * settings screen has to say so rather than showing dead switches.
 */
object DhizukuCompat {

    const val PACKAGE_NAME = "com.rosan.dhizuku"

    /** Dhizuku is device-owner based and needs API 26; the plugin declares the same minimum. */
    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    /**
     * [Dhizuku.init] binds to the Dhizuku service and must succeed before anything else works.
     * Safe to call repeatedly — it returns true once bound.
     */
    fun state(context: Context): DhizukuState {
        if (!isSupported) return DhizukuState.UNSUPPORTED
        if (!isInstalled(context)) return DhizukuState.NOT_INSTALLED
        val bound = try {
            Dhizuku.init(context)
        } catch (t: Throwable) {
            // Throwable, not Exception: a version mismatch between the app and the installed
            // Dhizuku surfaces as a linkage error rather than something catchable as Exception.
            Timber.w(t, "Dhizuku.init failed")
            false
        }
        if (!bound) return DhizukuState.NOT_RUNNING
        return try {
            if (Dhizuku.isPermissionGranted()) DhizukuState.READY else DhizukuState.NOT_AUTHORIZED
        } catch (t: Throwable) {
            Timber.w(t, "Dhizuku permission check failed")
            DhizukuState.NOT_RUNNING
        }
    }

    fun isReady(context: Context): Boolean = state(context) == DhizukuState.READY

    /**
     * Ask Dhizuku for permission. The result arrives on the listener, not as a return value —
     * [onResult] is called with whether it was granted.
     */
    fun requestPermission(context: Context, onResult: (Boolean) -> Unit) {
        val current = state(context)
        if (current == DhizukuState.UNSUPPORTED || current == DhizukuState.NOT_INSTALLED) {
            onResult(false)
            return
        }
        try {
            if (!Dhizuku.init(context)) {
                onResult(false)
                return
            }
            if (Dhizuku.isPermissionGranted()) {
                onResult(true)
                return
            }
            Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                override fun onRequestPermission(grantResult: Int) {
                    onResult(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            })
        } catch (t: Throwable) {
            Timber.e(t, "Dhizuku permission request failed")
            onResult(false)
        }
    }

    private fun isInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(PACKAGE_NAME, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (t: Throwable) {
        Timber.w(t, "Could not check for Dhizuku")
        false
    }
}
