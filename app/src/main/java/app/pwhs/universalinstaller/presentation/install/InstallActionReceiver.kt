package app.pwhs.universalinstaller.presentation.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import timber.log.Timber

/**
 * Notification actions that need no window.
 *
 * The app had no receiver of any kind before this: every notification it posted used
 * `PendingIntent.getActivity`, so nothing could be answered without opening the app. Declining an
 * install is exactly the case where opening a window defeats the point.
 *
 * Not exported — the manifest entry has `android:exported="false"`, and only our own
 * PendingIntents (built in [InstallPromptNotifier]) can reach it.
 */
class InstallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return
        val pendingId = intent.getStringExtra(EXTRA_PENDING_ID) ?: return

        when (intent.action) {
            ACTION_CANCEL -> {
                // Dropping the entry is what actually cancels: nothing has been installed yet,
                // and the Install action resolves to nothing once the store no longer has it.
                val entry = PendingInstallStore.consume(pendingId)
                Timber.d("Install prompt declined for ${entry?.packageName ?: pendingId}")
                NotificationManagerCompat.from(context)
                    .cancel(InstallPromptNotifier(context).notificationId(pendingId))
            }

            else -> Timber.w("InstallActionReceiver: unknown action ${intent.action}")
        }
    }

    companion object {
        const val ACTION_CANCEL = "app.pwhs.universalinstaller.action.CANCEL_PENDING_INSTALL"
        const val EXTRA_PENDING_ID = "pending_id"
    }
}
