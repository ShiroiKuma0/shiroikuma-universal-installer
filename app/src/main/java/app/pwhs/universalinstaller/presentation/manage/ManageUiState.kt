package app.pwhs.universalinstaller.presentation.manage

import android.net.Uri
import app.pwhs.universalinstaller.domain.model.InstalledApp

enum class UninstallSortBy { Name, Size, InstalledAt, LastUpdated, LastUsed }
enum class SortDirection { Asc, Desc }

/**
 * Per-app category used by the filter chips. An app is exactly one category at a time —
 * Disabled wins over System / User because a disabled app has its main entry point gone
 * regardless of where its APK lives.
 *
 * User/System are a pair — with neither selected, both types show. Disabled and Blocked are
 * independent switches layered on top, each narrowing the list to that state.
 */
enum class AppFilter { User, System, Disabled, Blocked }
enum class GroupBy { None, Installer }

data class StorageBreakdown(
    val appBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long,
) {
    val totalBytes: Long get() = appBytes + dataBytes + cacheBytes
}

/** Usage time per day for the last N days, oldest → newest. Empty list if no data. */
data class UsageBucket(
    val dayStartMillis: Long,
    val foregroundMillis: Long,
)

/**
 * Surfaced to the UI when the pending uninstall touches one or more system apps. The UI
 * renders either a single-app warning (with 2 method options) or a batch breakdown
 * ("N user + K system apps — pick method for system").
 */
sealed interface SystemAppPrompt {
    data class Single(val pkg: String, val appName: String) : SystemAppPrompt
    data class Batch(
        val systemApps: List<Pair<String, String>>,   // pkg → appName
        val userApps: List<Pair<String, String>>,
    ) : SystemAppPrompt

    /** Shown when neither Root nor Shizuku is ready to handle system-app removal. */
    data class PrivilegedRequired(
        val systemApps: List<Pair<String, String>>,
        val userAppsAvailable: List<String>,         // optional normal uninstall path
    ) : SystemAppPrompt
}

/**
 * Backup → save to public Download/.../Extracted, snackbar with "Open folder" action.
 * Share  → save to cacheDir, fire ACTION_SEND chooser as soon as the copy completes.
 */
enum class ExtractMode { Backup, Share, Server, Reinstall }

sealed interface ExtractState {
    data object Idle : ExtractState
    data class Running(
        val packageName: String,
        val appName: String,
        val bytesCopied: Long,
        val totalBytes: Long,
        val mode: ExtractMode,
    ) : ExtractState
    data class Done(
        val appName: String,
        val uri: Uri,
        val mode: ExtractMode,
    ) : ExtractState
    data class Error(
        val appName: String,
        val message: String,
        val mode: ExtractMode,
    ) : ExtractState
}

/** Progress for a bulk extract over the current selection. */
sealed interface BatchExtractState {
    data object Idle : BatchExtractState
    data class Running(
        val completed: Int,
        val total: Int,
        val currentName: String,
        val bytesCopied: Long,
        val totalBytes: Long,
    ) : BatchExtractState
    data class Done(val success: Int, val failed: Int) : BatchExtractState
}

/**
 * One-shot snackbar payload for privileged actions (force-stop, disable/enable). Cleared
 * after the UI consumes it via `dismissPrivilegedActionResult`.
 */
sealed interface PrivilegedActionResult {
    val message: String
    data class Success(override val message: String) : PrivilegedActionResult
    data class Failure(override val message: String) : PrivilegedActionResult
}

data class ManageUiState(
    val apps: List<InstalledApp> = emptyList(),
    val filteredApps: List<InstalledApp> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val appFilter: Set<AppFilter> = setOf(AppFilter.User),
    val selectedPackages: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isAllSelected: Boolean = false,
    val sortBy: UninstallSortBy = UninstallSortBy.Name,
    val sortDirection: SortDirection = SortDirection.Asc,
    val groupBy: GroupBy = GroupBy.None,
    val usageAccessGranted: Boolean = false,
    val systemAppPrompt: SystemAppPrompt? = null,
    val extractState: ExtractState = ExtractState.Idle,
    val batchExtractState: BatchExtractState = BatchExtractState.Idle,
    /** True when Root or Shizuku is currently ready to run shell commands. */
    val privilegedReady: Boolean = false,
    val privilegedActionResult: PrivilegedActionResult? = null,
)
