package app.pwhs.updater.data.repo

import app.pwhs.updater.domain.model.TrackedApp
import kotlinx.coroutines.flow.Flow

interface AppUpdateRepository {
    fun getAllTrackedApps(): Flow<List<TrackedApp>>
    fun getTrackedApp(packageName: String): Flow<TrackedApp?>
    fun getUpdateCount(): Flow<Int>
    suspend fun saveTrackedApp(app: TrackedApp)
    suspend fun removeTrackedApp(packageName: String)
    suspend fun checkForUpdate(packageName: String, apiToken: String? = null): Result<TrackedApp>
    suspend fun checkAllUpdates(apiToken: String? = null): List<TrackedApp>
}
