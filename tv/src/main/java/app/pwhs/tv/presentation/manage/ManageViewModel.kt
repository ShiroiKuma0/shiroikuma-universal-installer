package app.pwhs.tv.presentation.manage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.pwhs.core.data.AppRepository
import app.pwhs.core.domain.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppFilter { User, System, Disabled }
enum class SortBy { Name, Size, Date }

data class ManageUiState(
    val apps: List<InstalledApp> = emptyList(),
    val filteredApps: List<InstalledApp> = emptyList(),
    val isLoading: Boolean = true,
    val filter: AppFilter = AppFilter.User,
    val sortBy: SortBy = SortBy.Name,
    val searchQuery: String = ""
)

class ManageViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AppRepository(application.applicationContext)

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _filter = MutableStateFlow(AppFilter.User)
    private val _sortBy = MutableStateFlow(SortBy.Name)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<ManageUiState> = combine(
        _apps, _isLoading, _filter, _sortBy, _searchQuery
    ) { apps, loading, filter, sortBy, query ->
        val filtered = apps.filter { app ->
            val matchesFilter = when (filter) {
                AppFilter.User -> !app.isSystemApp && app.enabled
                AppFilter.System -> app.isSystemApp
                AppFilter.Disabled -> !app.enabled
            }
            val matchesQuery = query.isBlank() || 
                app.appName.contains(query, ignoreCase = true) || 
                app.packageName.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }.let { list ->
            when (sortBy) {
                SortBy.Name -> list.sortedBy { it.appName.lowercase() }
                SortBy.Size -> list.sortedByDescending { it.sizeBytes }
                SortBy.Date -> list.sortedByDescending { it.installedAt }
            }
        }
        ManageUiState(apps, filtered, loading, filter, sortBy, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManageUiState())

    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val allApps = withContext(Dispatchers.IO) { repo.getInstalledApps(includeSystem = true) }
            _apps.value = allApps
            _isLoading.value = false
        }
    }

    fun setFilter(filter: AppFilter) {
        _filter.value = filter
    }

    fun setSortBy(sortBy: SortBy) {
        _sortBy.value = sortBy
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
