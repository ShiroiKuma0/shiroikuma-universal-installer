package app.pwhs.universalinstaller.presentation.manage.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import app.pwhs.universalinstaller.domain.model.InstalledApp
import app.pwhs.universalinstaller.util.AndroidAutoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object InstalledAppsLoader {

    suspend fun loadInstalledApps(context: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }

        // Last-used lookup: single batch query over the past year
        val lastUsedMap = ManageUsageStatsHelper.queryLastUsedMap(context)

        // Batch query for Android Auto services — single batch query for all apps
        val autoServicePackages: Set<String> = runCatching {
            val mediaIntent = Intent("android.media.browse.MediaBrowserService")
            val carAppIntent = Intent("androidx.car.app.CarAppService")
            val mediaList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentServices(mediaIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentServices(mediaIntent, 0)
            }
            val carList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentServices(carAppIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentServices(carAppIntent, 0)
            }
            (mediaList.mapNotNull { it.serviceInfo?.packageName } +
                carList.mapNotNull { it.serviceInfo?.packageName }).toSet()
        }.getOrDefault(emptySet())

        installedInfos.map { appInfo ->
            val pkgInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(
                        appInfo.packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(appInfo.packageName, 0)
                }
            } catch (_: Exception) { null }

            val sourceDir = appInfo.sourceDir
            val sizeBytes = if (!sourceDir.isNullOrBlank()) {
                runCatching { File(sourceDir).length() }.getOrDefault(0L)
            } else 0L

            var installer: String? = null
            var initiating: String? = null
            var originating: String? = null

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val sourceInfo = pm.getInstallSourceInfo(appInfo.packageName)
                    installer = sourceInfo.installingPackageName
                    initiating = sourceInfo.initiatingPackageName
                    originating = sourceInfo.originatingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    installer = pm.getInstallerPackageName(appInfo.packageName)
                }
            } catch (_: Exception) { null }

            val isAaSupported = autoServicePackages.contains(appInfo.packageName) ||
                AndroidAutoCompat.supportsAndroidAuto(context, appInfo.packageName)

            InstalledApp(
                packageName = appInfo.packageName,
                appName = appInfo.loadLabel(pm).toString(),
                versionName = pkgInfo?.versionName ?: "",
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                sizeBytes = sizeBytes,
                installedAt = pkgInfo?.firstInstallTime ?: 0L,
                lastUpdatedAt = pkgInfo?.lastUpdateTime ?: 0L,
                lastUsedAt = lastUsedMap[appInfo.packageName] ?: 0L,
                hasSplits = !appInfo.splitSourceDirs.isNullOrEmpty(),
                enabled = appInfo.enabled,
                installerPackage = installer,
                initiatingPackage = initiating,
                originatingPackage = originating,
                isAndroidAutoSupported = isAaSupported,
            )
        }
    }
}
