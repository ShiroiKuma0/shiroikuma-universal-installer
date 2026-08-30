package app.pwhs.universalinstaller.presentation.manage.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.domain.model.InstalledApp
import app.pwhs.universalinstaller.presentation.manage.AppFilter
import app.pwhs.universalinstaller.presentation.manage.BatchExtractState
import app.pwhs.universalinstaller.presentation.manage.ExtractState
import app.pwhs.universalinstaller.presentation.manage.GroupBy
import app.pwhs.universalinstaller.presentation.manage.ManageUiState
import app.pwhs.universalinstaller.presentation.manage.PrivilegedActionResult
import app.pwhs.universalinstaller.presentation.manage.SortDirection
import app.pwhs.universalinstaller.presentation.manage.SystemAppPrompt
import app.pwhs.universalinstaller.presentation.manage.UninstallSortBy
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import kotlinx.coroutines.flow.first

data class ManageFilterPreferences(
    val sortBy: UninstallSortBy = UninstallSortBy.Name,
    val sortDirection: SortDirection = SortDirection.Asc,
    val groupBy: GroupBy = GroupBy.None,
    val appFilter: Set<AppFilter> = setOf(AppFilter.User),
)

object ManageFilterHelper {

    suspend fun loadFilterPreferences(context: Context): ManageFilterPreferences {
        return runCatching {
            val prefs = context.dataStore.data.first()
            val sortBy = prefs[PreferencesKeys.MANAGE_SORT_BY]
                ?.let { name -> UninstallSortBy.entries.firstOrNull { it.name == name } }
                ?: UninstallSortBy.Name
            val sortDirection = prefs[PreferencesKeys.MANAGE_SORT_DIRECTION]
                ?.let { name -> SortDirection.entries.firstOrNull { it.name == name } }
                ?: SortDirection.Asc
            val groupBy = prefs[PreferencesKeys.MANAGE_GROUP_BY]
                ?.let { name -> GroupBy.entries.firstOrNull { it.name == name } }
                ?: GroupBy.None
            val appFilter = prefs[PreferencesKeys.MANAGE_APP_FILTER]
                ?.mapNotNull { name -> AppFilter.entries.firstOrNull { it.name == name } }
                ?.toSet()
                ?.takeIf { it.isNotEmpty() }
                ?: setOf(AppFilter.User)
            ManageFilterPreferences(sortBy, sortDirection, groupBy, appFilter)
        }.getOrDefault(ManageFilterPreferences())
    }

    suspend fun saveFilterPreferences(
        context: Context,
        sortBy: UninstallSortBy,
        sortDirection: SortDirection,
        groupBy: GroupBy,
        appFilter: Set<AppFilter>,
    ) {
        runCatching {
            context.dataStore.edit { prefs ->
                prefs[PreferencesKeys.MANAGE_SORT_BY] = sortBy.name
                prefs[PreferencesKeys.MANAGE_SORT_DIRECTION] = sortDirection.name
                prefs[PreferencesKeys.MANAGE_GROUP_BY] = groupBy.name
                prefs[PreferencesKeys.MANAGE_APP_FILTER] = appFilter.map { it.name }.toSet()
            }
        }
    }

    fun buildManageUiState(flows: Array<Any?>): ManageUiState {
        @Suppress("UNCHECKED_CAST")
        val apps = flows[0] as List<InstalledApp>
        val query = flows[1] as String
        val loading = flows[2] as Boolean
        @Suppress("UNCHECKED_CAST")
        val appFilter = flows[3] as Set<AppFilter>
        @Suppress("UNCHECKED_CAST")
        val selected = flows[4] as Set<String>
        val sortBy = flows[5] as UninstallSortBy
        val direction = flows[6] as SortDirection
        val usage = flows[7] as Boolean
        val prompt = flows[8] as SystemAppPrompt?
        val extract = flows[9] as ExtractState
        val privReady = flows[10] as Boolean
        val privResult = flows[11] as PrivilegedActionResult?
        val groupBy = flows[12] as GroupBy
        val batchExtract = flows[13] as BatchExtractState
        val refreshing = flows[14] as Boolean
        @Suppress("UNCHECKED_CAST")
        val blocked = flows[15] as Set<String>

        val filtered = filterApps(
            apps = apps,
            query = query,
            appFilter = appFilter,
            blocked = blocked,
            sortBy = sortBy,
            direction = direction,
        )
        return ManageUiState(
            apps = apps,
            filteredApps = filtered,
            searchQuery = query,
            isLoading = loading,
            isRefreshing = refreshing,
            appFilter = appFilter,
            selectedPackages = selected,
            isSelectionMode = selected.isNotEmpty(),
            isAllSelected = filtered.isNotEmpty() && selected.containsAll(filtered.map { it.packageName }.toSet()),
            sortBy = sortBy,
            sortDirection = direction,
            groupBy = groupBy,
            usageAccessGranted = usage,
            systemAppPrompt = prompt,
            extractState = extract,
            batchExtractState = batchExtract,
            privilegedReady = privReady,
            privilegedActionResult = privResult,
        )
    }

    fun filterApps(
        apps: List<InstalledApp>,
        query: String,
        appFilter: Set<AppFilter>,
        blocked: Set<String>,
        sortBy: UninstallSortBy,
        direction: SortDirection,
    ): List<InstalledApp> {
        val filtered = apps.filter { app ->
            val typeFilters = appFilter.filter { it == AppFilter.User || it == AppFilter.System }
            val matchesType = if (typeFilters.isEmpty()) {
                true
            } else {
                (AppFilter.User in appFilter && !app.isSystemApp) ||
                (AppFilter.System in appFilter && app.isSystemApp)
            }

            val matchesState = if (AppFilter.Disabled in appFilter) {
                !app.enabled
            } else {
                app.enabled
            }

            if (AppFilter.Blocked in appFilter && app.packageName !in blocked) return@filter false
            if (!(matchesType && matchesState)) return@filter false

            if (query.isBlank()) return@filter true
            app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
        }
        return applySort(filtered, sortBy, direction)
    }

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
