package app.pwhs.universalinstaller.presentation.manage.util

import app.pwhs.universalinstaller.domain.model.InstalledApp
import app.pwhs.universalinstaller.presentation.manage.SortDirection
import app.pwhs.universalinstaller.presentation.manage.UninstallSortBy

object ManageFilterHelper {

    fun applySort(
        list: List<InstalledApp>,
        sortBy: UninstallSortBy,
        direction: SortDirection,
    ): List<InstalledApp> {
        val nameKey: (InstalledApp) -> String = { it.appName.lowercase() }
        val comparator: Comparator<InstalledApp> = when (sortBy) {
            UninstallSortBy.Name -> compareBy(nameKey)
            UninstallSortBy.Size -> compareBy<InstalledApp> { it.sizeBytes }.thenBy(nameKey)
            UninstallSortBy.InstalledAt -> compareBy<InstalledApp> { it.installedAt }.thenBy(nameKey)
            UninstallSortBy.LastUpdated -> compareBy<InstalledApp> { it.lastUpdatedAt }.thenBy(nameKey)
            UninstallSortBy.LastUsed -> compareBy<InstalledApp> { it.lastUsedAt }.thenBy(nameKey)
        }
        val sorted = list.sortedWith(comparator)
        return if (direction == SortDirection.Asc) sorted else sorted.reversed()
    }
}
