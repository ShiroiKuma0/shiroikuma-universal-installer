package app.pwhs.updater.domain

import app.pwhs.updater.domain.provider.GitHubReleaseProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseProviderTest {

    @Test
    fun extractRepoPath_validUrls_extractsCorrectly() {
        assertEquals(
            "JunkFood02/Seal",
            GitHubReleaseProvider.extractRepoPath("https://github.com/JunkFood02/Seal"),
        )
        assertEquals(
            "JunkFood02/Seal",
            GitHubReleaseProvider.extractRepoPath("https://github.com/JunkFood02/Seal/releases/tag/v1.12.0"),
        )
        assertEquals(
            "revanced/revanced-manager",
            GitHubReleaseProvider.extractRepoPath("github.com/revanced/revanced-manager.git"),
        )
    }

    @Test
    fun extractCleanVersion_variousTags_cleansPrefixes() {
        assertEquals("1.12.0", GitHubReleaseProvider.extractCleanVersion("v1.12.0"))
        assertEquals("2.0.1", GitHubReleaseProvider.extractCleanVersion("release-2.0.1"))
        assertEquals("0.9.4", GitHubReleaseProvider.extractCleanVersion("app-0.9.4"))
        assertEquals("1.0", GitHubReleaseProvider.extractCleanVersion("1.0"))
    }

    @Test
    fun canHandle_githubUrls_returnsTrue() {
        val provider = GitHubReleaseProvider()
        assertTrue(provider.canHandle("https://github.com/vfsfitvnm/ViMusic"))
        assertFalse(provider.canHandle("https://gitlab.com/fdroid/fdroidclient"))
    }
}
