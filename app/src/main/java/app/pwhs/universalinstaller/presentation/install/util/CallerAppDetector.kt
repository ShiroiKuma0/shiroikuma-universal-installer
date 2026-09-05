package app.pwhs.universalinstaller.presentation.install.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import timber.log.Timber

/**
 * Utility to identify the caller application that initiated an installation intent.
 */
object CallerAppDetector {

    private const val TAG = "CallerAppDetector"

    fun detectCallerPackage(activity: Activity, intent: Intent?): String? {
        val candidates = mutableListOf<String>()

        // 1. Android 14+ (API 34+): direct framework method for any startActivity launch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.launchedFromPackage?.takeIf { it.isNotBlank() }?.let {
                candidates.add(it)
            }
        }

        // 2. Activity callingPackage / callingActivity (populated when startActivityForResult was used)
        activity.callingPackage?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
        activity.callingActivity?.packageName?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }

        // 3. Activity referrer (e.g. android-app://org.fdroid.fdroid)
        val referrer = runCatching { activity.referrer }.getOrNull()
        if (referrer != null && referrer.scheme == "android-app") {
            val host = referrer.host
            if (!host.isNullOrBlank()) {
                candidates.add(host)
            }
        }

        // 4. Intent extra referrer
        if (intent != null) {
            val extraReferrer = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_REFERRER, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_REFERRER)
                }
            }.getOrNull() ?: (intent.getStringExtra("android.intent.extra.REFERRER_NAME")?.let {
                runCatching { Uri.parse(it) }.getOrNull()
            })

            if (extraReferrer != null && extraReferrer.scheme == "android-app") {
                val host = extraReferrer.host
                if (!host.isNullOrBlank()) {
                    candidates.add(host)
                }
            }

            // 5. If intent has content URI, check which package hosts the ContentProvider
            val uri = intent.data ?: intent.clipData?.let { clip ->
                if (clip.itemCount > 0) clip.getItemAt(0).uri else null
            }
            if (uri != null && uri.scheme == "content" && !uri.authority.isNullOrBlank()) {
                val authority = uri.authority
                if (!authority.isNullOrBlank()) {
                    val providerInfo = runCatching {
                        activity.packageManager.resolveContentProvider(authority, 0)
                    }.getOrNull()
                    if (providerInfo != null && !providerInfo.packageName.isNullOrBlank()) {
                        candidates.add(providerInfo.packageName)
                    }
                }
            }
        }

        // Filter out self package
        val detected = candidates.firstOrNull { it != activity.packageName }
        if (detected != null) {
            Timber.d("$TAG: Detected caller package: $detected")
        } else {
            Timber.d("$TAG: Could not detect caller package")
        }
        return detected
    }
}
