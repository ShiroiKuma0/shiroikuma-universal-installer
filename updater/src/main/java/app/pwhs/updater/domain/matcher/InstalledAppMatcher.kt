package app.pwhs.updater.domain.matcher

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import timber.log.Timber

data class InstalledAppMatchResult(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
)

object InstalledAppMatcher {

    private fun normalize(text: String): String {
        return text.lowercase().replace(Regex("[^a-z0-9]"), "")
    }

    fun getInstalledVersion(pm: PackageManager, packageName: String): Pair<String, Long>? {
        return runCatching {
            val pkgInfo = pm.getPackageInfo(packageName, 0)
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }
            Pair(pkgInfo.versionName ?: "1.0", vCode)
        }.getOrNull()
    }

    fun findMatch(
        pm: PackageManager,
        repoUrl: String,
        candidateName: String?,
        assetNames: List<String> = emptyList(),
    ): InstalledAppMatchResult? {
        val rawRepo = repoUrl.substringBefore('?').removeSuffix(".git").substringAfterLast('/')
        val normRepo = normalize(rawRepo)
        if (normRepo.isBlank()) return null

        val normCandidate = candidateName?.let { normalize(it) }?.takeIf { it.isNotBlank() }

        val normAssets = assetNames.map { asset ->
            val clean = asset.substringBeforeLast('.').replace(Regex("""[-_]v?\d.*$"""), "")
            normalize(clean)
        }.filter { it.length >= 3 }

        return runCatching {
            val installedPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                pm.getInstalledPackages(0)
            }

            var bestMatch: InstalledAppMatchResult? = null
            var bestScore = 0

            for (pkg in installedPackages) {
                val appInfo = pkg.applicationInfo ?: continue
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val label = runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrDefault("")
                val normLabel = normalize(label)
                val normPkg = normalize(pkg.packageName)

                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkg.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkg.versionCode.toLong()
                }
                val verName = pkg.versionName ?: "1.0"

                // 1. Exact matches (Score: 100)
                if (normLabel == normRepo || (normCandidate != null && normLabel == normCandidate)) {
                    return InstalledAppMatchResult(pkg.packageName, label, verName, vCode)
                }
                if (normPkg == normRepo || (normCandidate != null && normPkg == normCandidate)) {
                    return InstalledAppMatchResult(pkg.packageName, label, verName, vCode)
                }

                // 2. Package contains repo name (e.g. apppwhsuniversalinstaller contains universalinstaller) (Score: 80)
                if (normPkg.contains(normRepo) && normRepo.length >= 4) {
                    val score = 80 + (if (!isSystem) 10 else 0)
                    if (score > bestScore) {
                        bestScore = score
                        bestMatch = InstalledAppMatchResult(pkg.packageName, label, verName, vCode)
                    }
                }

                // 3. Label contains repo name or vice versa (Score: 60)
                if (normLabel.isNotBlank() && (normLabel.contains(normRepo) || normRepo.contains(normLabel)) && normLabel.length >= 4) {
                    val score = 60 + (if (!isSystem) 10 else 0)
                    if (score > bestScore) {
                        bestScore = score
                        bestMatch = InstalledAppMatchResult(pkg.packageName, label, verName, vCode)
                    }
                }

                // 4. Asset names matching package or label (Score: 50)
                for (normAsset in normAssets) {
                    if (normPkg.contains(normAsset) || normLabel.contains(normAsset)) {
                        val score = 50 + (if (!isSystem) 10 else 0)
                        if (score > bestScore) {
                            bestScore = score
                            bestMatch = InstalledAppMatchResult(pkg.packageName, label, verName, vCode)
                        }
                    }
                }
            }

            bestMatch
        }.onFailure { Timber.e(it, "InstalledAppMatcher matching failed") }.getOrNull()
    }
}
