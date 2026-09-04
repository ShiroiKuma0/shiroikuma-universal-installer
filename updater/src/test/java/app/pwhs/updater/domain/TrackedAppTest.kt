package app.pwhs.updater.domain

import app.pwhs.updater.domain.model.TrackedApp
import app.pwhs.updater.domain.model.UpdateSourceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackedAppTest {

    @Test
    fun hasUpdate_karingStyleTagWithVersionCodeSuffix_returnsFalse() {
        val app = trackedApp(
            currentVersionName = "1.2.24",
            currentVersionCode = 2709,
            latestVersionName = "1.2.24.2709",
        )

        assertFalse(app.hasUpdate)
    }

    @Test
    fun hasUpdate_semverPatchIncrease_returnsTrue() {
        val app = trackedApp(
            currentVersionName = "1.2.24",
            currentVersionCode = 2709,
            latestVersionName = "1.2.25",
        )

        assertTrue(app.hasUpdate)
    }

    @Test
    fun hasUpdate_differentVersionCodeSuffix_returnsTrue() {
        val app = trackedApp(
            currentVersionName = "1.2.24",
            currentVersionCode = 2708,
            latestVersionName = "1.2.24.2709",
        )

        assertTrue(app.hasUpdate)
    }

    private fun trackedApp(
        currentVersionName: String,
        currentVersionCode: Long,
        latestVersionName: String,
    ) = TrackedApp(
        packageName = "com.nebula.karing",
        appName = "Karing",
        sourceType = UpdateSourceType.GITHUB,
        sourceUrl = "https://github.com/KaringX/karing",
        currentVersionName = currentVersionName,
        currentVersionCode = currentVersionCode,
        latestVersionName = latestVersionName,
    )
}
