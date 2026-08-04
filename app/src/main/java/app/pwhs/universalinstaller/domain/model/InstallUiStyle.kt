package app.pwhs.universalinstaller.domain.model

/**
 * The shape the install UI takes when a package is opened from another app.
 *
 * Both styles render the identical stage content ([app.pwhs.universalinstaller.presentation
 * .install.dialog.PositionDialog] and the `Dialog*Content` composables) — only the container
 * differs. That split is the point: InstallerX-Revived runs its dialog page and its bottom-sheet
 * page off one session state machine, and this app can do the same because `PositionDialog` was
 * already a plain layout with no window of its own.
 */
enum class InstallUiStyle(val value: String) {
    /** A centred card floating over the calling app. */
    Dialog("dialog"),

    /** Anchored to the bottom edge, within thumb reach on a tall phone. */
    Sheet("sheet");

    companion object {
        fun from(value: String?): InstallUiStyle = entries.find { it.value == value } ?: Dialog
    }
}
