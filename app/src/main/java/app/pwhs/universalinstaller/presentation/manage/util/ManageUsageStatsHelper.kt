package app.pwhs.universalinstaller.presentation.manage.util

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import app.pwhs.universalinstaller.presentation.manage.StorageBreakdown
import app.pwhs.universalinstaller.presentation.manage.UsageBucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar

object ManageUsageStatsHelper {

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun queryUsageBuckets(context: Context, packageName: String): List<UsageBucket> = withContext(Dispatchers.IO) {
        if (!hasUsageAccess(context)) return@withContext emptyList()
        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = cal.timeInMillis
            val dayMs = 24L * 60 * 60 * 1000
            val sevenAgo = todayStart - 6L * dayMs
            val raw = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                sevenAgo,
                now,
            ) ?: return@withContext emptyList()
            val buckets = LongArray(7)
            for (row in raw) {
                if (row.packageName != packageName) continue
                val rowStart = row.firstTimeStamp
                val idx = ((rowStart - sevenAgo) / dayMs).toInt().coerceIn(0, 6)
                buckets[idx] += row.totalTimeInForeground
            }
            (0 until 7).map { i ->
                UsageBucket(
                    dayStartMillis = sevenAgo + i * dayMs,
                    foregroundMillis = buckets[i],
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun queryStorageStats(context: Context, packageName: String): StorageBreakdown? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@withContext null
        try {
            val ssm = context.getSystemService(StorageStatsManager::class.java)
                ?: return@withContext null
            val uuid = StorageManager.UUID_DEFAULT
            val stats = ssm.queryStatsForPackage(
                uuid,
                packageName,
                Process.myUserHandle(),
            )
            StorageBreakdown(
                appBytes = stats.appBytes,
                dataBytes = stats.dataBytes,
                cacheBytes = stats.cacheBytes,
            )
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    fun queryLastUsedMap(context: Context): Map<String, Long> {
        if (!hasUsageAccess(context)) return emptyMap()
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val yearAgo = now - 365L * 24 * 60 * 60 * 1000
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_YEARLY, yearAgo, now
            ) ?: return emptyMap()
            val map = HashMap<String, Long>(stats.size)
            for (s in stats) {
                val t = s.lastTimeUsed
                if (t <= 0L) continue
                val prev = map[s.packageName] ?: 0L
                if (t > prev) map[s.packageName] = t
            }
            map
        } catch (e: Exception) {
            Timber.w(e, "queryUsageStats failed")
            emptyMap()
        }
    }
}
