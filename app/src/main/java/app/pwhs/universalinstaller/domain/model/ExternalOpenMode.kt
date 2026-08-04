package app.pwhs.universalinstaller.domain.model

/**
 * How an install opened from another app (file manager, browser, share sheet) asks for
 * confirmation.
 *
 * Modelled on InstallerX-Revived's `InstallMode` (both projects are GPL-3.0). Their enum also
 * carries `AutoDialog` and `Ignore`; ours does not — [app.pwhs.universalinstaller.presentation
 * .setting.PreferencesKeys.AUTO_CONFIRM_EXTERNAL_INSTALL] already covers auto-dialog, and
 * "ignore" (hand the intent back to the system installer) is not something this app offers.
 */
enum class ExternalOpenMode(val value: String) {
    /** Current behaviour: the translucent install dialog appears over the calling app. */
    Dialog("dialog"),

    /**
     * No window at all. The package is parsed headlessly and a notification asks for
     * confirmation, so the calling app is never covered.
     */
    Notification("notification"),

    /**
     * Like [Notification] but installs immediately, reporting progress in the notification.
     * Falls back to [Notification]'s prompt when the package carries a risk the user has not
     * accepted yet (downgrade, signature mismatch) — silently installing those is not
     * something a mode switch should authorise.
     */
    AutoNotification("auto_notification");

    companion object {
        fun from(value: String?): ExternalOpenMode = entries.find { it.value == value } ?: Dialog
    }
}
