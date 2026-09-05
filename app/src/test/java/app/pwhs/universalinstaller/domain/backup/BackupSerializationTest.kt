package app.pwhs.universalinstaller.domain.backup

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        encodeDefaults = true
    }

    @Test
    fun `serialize and deserialize UniversalInstallerBackup v2 preserves all fields`() {
        val backup = UniversalInstallerBackup(
            version = 2,
            exportedAt = 1700000000000L,
            appVersion = "1.5.0",
            settings = AppSettingsBackupDto(
                themeMode = "Dark",
                useShizuku = true,
                virusTotalApiKey = "vt_test_api_key_123",
                blockedPackages = setOf("com.malware.bad", "com.telemetry.ad"),
            ),
            sourceTokens = SourceTokensBackupDto(
                githubToken = "ghp_mock_token_abc",
                gitlabToken = "glpat_mock_token_xyz",
            ),
            trackedApps = listOf(
                TrackedAppBackupDto(
                    packageName = "org.mozilla.firefox",
                    appName = "Firefox",
                    sourceUrl = "https://github.com/mozilla-mobile/fenix",
                    sourceType = "GITHUB",
                )
            ),
            uninstallLogs = listOf(
                UninstallLogBackupDto(
                    packageName = "com.sample.app",
                    appName = "Sample App",
                    success = true,
                    uninstalledAt = 1699999999000L,
                )
            ),
        )

        val encoded = json.encodeToString(UniversalInstallerBackup.serializer(), backup)
        assertTrue(encoded.contains("vt_test_api_key_123"))
        assertTrue(encoded.contains("ghp_mock_token_abc"))
        assertTrue(encoded.contains("org.mozilla.firefox"))
        assertTrue(encoded.contains("com.sample.app"))

        val decoded = json.decodeFromString(UniversalInstallerBackup.serializer(), encoded)
        assertEquals(2, decoded.version)
        assertEquals("1.5.0", decoded.appVersion)
        assertEquals("Dark", decoded.settings?.themeMode)
        assertEquals(true, decoded.settings?.useShizuku)
        assertEquals(2, decoded.settings?.blockedPackages?.size)
        assertEquals("ghp_mock_token_abc", decoded.sourceTokens?.githubToken)
        assertEquals(1, decoded.trackedApps?.size)
        assertEquals("Firefox", decoded.trackedApps?.first()?.appName)
        assertEquals(1, decoded.uninstallLogs?.size)
    }

    @Test
    fun `serialize and deserialize EncryptedBackupEnvelope works`() {
        val envelope = EncryptedBackupEnvelope(
            isEncrypted = true,
            version = 2,
            algorithm = "AES-256-GCM",
            kdf = "PBKDF2WithHmacSHA256",
            iterations = 100_000,
            salt = "c2FsdA==",
            iv = "aXY=",
            ciphertext = "Y2lwaGVydGV4dA==",
        )

        val encoded = json.encodeToString(EncryptedBackupEnvelope.serializer(), envelope)
        val decoded = json.decodeFromString(EncryptedBackupEnvelope.serializer(), encoded)

        assertTrue(decoded.isEncrypted)
        assertEquals(2, decoded.version)
        assertEquals("c2FsdA==", decoded.salt)
        assertEquals("Y2lwaGVydGV4dA==", decoded.ciphertext)
    }
}
