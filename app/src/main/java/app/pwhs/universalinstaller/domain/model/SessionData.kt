package app.pwhs.universalinstaller.domain.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import ru.solrudev.ackpine.resources.ResolvableString
import java.util.UUID

@Parcelize
data class SessionData(
    val id: UUID,
    val name: String,
    val appName: String = "",
    val packageName: String = "",
    val iconPath: String? = null,
    val error: ResolvableString = ResolvableString.empty(),
    val isCancellable: Boolean = true,
    /**
     * Everything Retry needs, kept here rather than in the controller's per-session maps.
     *
     * Those maps belong to one controller instance, and there is a controller per
     * InstallViewModel — so a card shown in InstallActivity for an install started by the
     * dialog Activity had no entry, and Retry silently did nothing. They are also not
     * repopulated by restoreSessionsFromSavedState, so Retry died again after a process
     * restart. SessionData lives in the shared repository, so any controller can act on it.
     */
    val uris: List<@RawValue Uri> = emptyList(),
    val originalUri: @RawValue Uri? = null,
    val deleteAfterInstall: Boolean = false,
    val allowDowngrade: Boolean = false,
    val targetUserId: Int? = null,
): Parcelable
