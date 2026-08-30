package app.pwhs.updater.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedAppDao {
    @Query("SELECT * FROM tracked_apps ORDER BY appName ASC")
    fun getAllTrackedApps(): Flow<List<TrackedAppEntity>>

    @Query("SELECT * FROM tracked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): TrackedAppEntity?

    @Query("SELECT * FROM tracked_apps WHERE packageName = :packageName LIMIT 1")
    fun getByPackageNameFlow(packageName: String): Flow<TrackedAppEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: TrackedAppEntity)

    @Update
    suspend fun update(entity: TrackedAppEntity)

    @Delete
    suspend fun delete(entity: TrackedAppEntity)

    @Query("DELETE FROM tracked_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("SELECT COUNT(*) FROM tracked_apps WHERE latestVersionName IS NOT NULL AND latestVersionName != '' AND latestVersionName != currentVersionName")
    fun getUpdateCountFlow(): Flow<Int>
}
