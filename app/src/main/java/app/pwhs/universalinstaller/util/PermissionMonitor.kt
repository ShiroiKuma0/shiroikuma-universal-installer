package app.pwhs.universalinstaller.util

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Utility to monitor permission status changes while the user is in system settings.
 *
 * When a permission is granted, it automatically brings the app back to the foreground
 * using [Intent.FLAG_ACTIVITY_REORDER_TO_FRONT]. This provides a seamless "magic" return
 * experience without the user having to manually press the back button.
 */
object PermissionMonitor {
    private var job: Job? = null

    /**
     * Start polling for a permission change.
     *
     * @param context Current context
     * @param check Lambda that returns true when the desired permission is granted
     */
    fun start(context: Context, check: () -> Boolean) {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Main).launch {
            Timber.d("PermissionMonitor: Started polling")
            // Reduced initial delay for better responsiveness
            delay(500)
            
            var attempts = 0
            // Poll every 500ms for up to 5 minutes (600 attempts)
            while (attempts < 600) {
                if (check()) {
                    Timber.d("PermissionMonitor: Permission granted! Bringing app to front.")
                    bringAppToFront(context)
                    break
                }
                delay(500)
                attempts++
            }
            job = null
        }
    }

    private fun bringAppToFront(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        launchIntent?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }?.let {
            context.startActivity(it)
        }
    }

    /**
     * Stop active monitoring. Should be called when the app resumes or a grant is detected
     * via standard activity result pathways.
     */
    fun stop() {
        job?.cancel()
        job = null
    }
}
