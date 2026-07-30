package app.pwhs.universalinstaller.presentation.install

import android.content.Context
import app.pwhs.core.util.DeviceCompat
import app.pwhs.universalinstaller.R
import ru.solrudev.ackpine.installer.InstallFailure

object InstallErrorHelper {

    data class ErrorInfo(
        val title: String,
        val guidance: String,
    )

    /**
     * Appends the MIUI/HyperOS workaround to [info] when we're on Xiaomi hardware and the failure
     * is one this ROM family is known to cause (issue #104).
     *
     * Kept separate from [getErrorInfo] so it can be applied to the live error only — install
     * history stores the plain diagnosis, not a paragraph of advice that would be replayed on
     * every history card forever.
     */
    fun withDeviceHint(context: Context, info: ErrorInfo, failure: InstallFailure): ErrorInfo {
        if (!DeviceCompat.isXiaomi || !isMiuiSuspect(failure)) return info
        return info.copy(
            guidance = "${info.guidance}\n\n${context.getString(R.string.install_error_miui_hint)}",
        )
    }

    /**
     * Failure kinds MIUI/HyperOS "optimization" is known to produce when it silently vetoes a
     * third-party install. Which one surfaces depends on the ROM version, so we cover the whole
     * ambiguous set rather than guessing; the well-diagnosed failures (storage, invalid APK,
     * ABI mismatch, package conflict) keep their own guidance untouched.
     *
     * [InstallFailure.Aborted] is in the "suspect" set on purpose: ackpine's `await()` resolves an
     * in-app cancel by throwing [kotlinx.coroutines.CancellationException], so an Aborted result
     * here is always a *system* abort — precisely what MIUI optimization produces.
     */
    private fun isMiuiSuspect(failure: InstallFailure): Boolean = when (failure) {
        is InstallFailure.Aborted,
        is InstallFailure.Blocked,
        is InstallFailure.Generic,
        -> true
        is InstallFailure.Conflict,
        is InstallFailure.Incompatible,
        is InstallFailure.Invalid,
        is InstallFailure.Storage,
        is InstallFailure.Timeout,
        is InstallFailure.Exceptional,
        -> false
        else -> true
    }

    fun getErrorInfo(context: Context, failure: InstallFailure): ErrorInfo = when (failure) {
        is InstallFailure.Aborted -> ErrorInfo(
            title = context.getString(R.string.install_error_cancelled_title),
            guidance = context.getString(R.string.install_error_cancelled_guidance),
        )
        is InstallFailure.Blocked -> ErrorInfo(
            title = context.getString(R.string.install_error_blocked_title),
            guidance = context.getString(R.string.install_error_blocked_guidance),
        )
        is InstallFailure.Conflict -> ErrorInfo(
            title = context.getString(R.string.install_error_conflict_title),
            guidance = context.getString(R.string.install_error_conflict_guidance),
        )
        is InstallFailure.Incompatible -> ErrorInfo(
            title = context.getString(R.string.install_error_incompatible_title),
            guidance = context.getString(R.string.install_error_incompatible_guidance),
        )
        is InstallFailure.Invalid -> ErrorInfo(
            title = context.getString(R.string.install_error_invalid_title),
            guidance = context.getString(R.string.install_error_invalid_guidance),
        )
        is InstallFailure.Storage -> ErrorInfo(
            title = context.getString(R.string.install_error_storage_title),
            guidance = context.getString(R.string.install_error_storage_guidance),
        )
        is InstallFailure.Timeout -> ErrorInfo(
            title = context.getString(R.string.install_error_timeout_title),
            guidance = context.getString(R.string.install_error_timeout_guidance),
        )
        is InstallFailure.Exceptional -> ErrorInfo(
            title = context.getString(R.string.install_error_unexpected_title),
            guidance = context.getString(R.string.install_error_unexpected_guidance, failure.message ?: ""),
        )
        is InstallFailure.Generic -> ErrorInfo(
            title = context.getString(R.string.install_error_failed_title),
            guidance = failure.message ?: context.getString(R.string.install_error_unknown_guidance),
        )
        else -> ErrorInfo(
            title = context.getString(R.string.install_error_failed_title),
            guidance = failure.message ?: context.getString(R.string.install_error_unknown_guidance_short),
        )
    }

    fun getUserFriendlyMessage(context: Context, failure: InstallFailure): String {
        val info = getErrorInfo(context, failure)
        return "${info.title}: ${info.guidance}"
    }
}
