package app.pwhs.tv.install

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TvInstallBackendTest {
    @Test fun readyShizukuDoesNotTriggerRootPrompt() = runBlocking {
        assertEquals(TvInstallBackend.Shizuku, TvInstallBackend.select(true, true,
            shizukuReady = { true }, rootReady = { error("Must not request root") }))
    }

    @Test fun stoppedOrRevokedShizukuFallsBackToRoot() = runBlocking {
        assertEquals(TvInstallBackend.Root, TvInstallBackend.select(true, true,
            shizukuReady = { false }, rootReady = { true }))
    }

    @Test fun unavailablePrivilegesUseSystemInstaller() = runBlocking {
        assertEquals(TvInstallBackend.System, TvInstallBackend.select(true, true,
            shizukuReady = { false }, rootReady = { false }))
    }

    @Test fun disabledBackendsAreNotProbed() = runBlocking {
        assertEquals(TvInstallBackend.System, TvInstallBackend.select(false, false,
            shizukuReady = { error("Shizuku disabled") }, rootReady = { error("Root disabled") }))
    }
}
