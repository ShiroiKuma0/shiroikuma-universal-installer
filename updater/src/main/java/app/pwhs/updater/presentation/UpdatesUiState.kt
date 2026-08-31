package app.pwhs.updater.presentation

import app.pwhs.updater.domain.model.TrackedApp

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isTracked: Boolean = false,
)

data class UpdatesUiState(
    val trackedApps: List<TrackedApp> = emptyList(),
    val isChecking: Boolean = false,
    val isAdding: Boolean = false,
    val isUpdatingAll: Boolean = false,
    val downloadingPackage: String? = null,
    val downloadProgress: Float = 0f,
    val searchQuery: String = "",
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val showAppPickerDialog: Boolean = false,
    val installedApps: List<InstalledAppItem> = emptyList(),
    val isLoadingInstalledApps: Boolean = false,
) {
    val updateCount: Int
        get() = trackedApps.count { it.hasUpdate }

    val filteredApps: List<TrackedApp>
        get() {
            if (searchQuery.isBlank()) return trackedApps
            return trackedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
}
