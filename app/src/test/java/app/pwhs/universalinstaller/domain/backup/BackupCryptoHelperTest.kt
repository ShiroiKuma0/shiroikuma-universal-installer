package app.pwhs.universalinstaller.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoHelperTest {

    @Test
    fun `encrypt and decrypt with correct password produces original content`() {
        val originalText = """{"version":2,"theme":"dark","tokens":["ghp_12345"]}"""
        val password = "SuperSecretPassword123!"

        val envelope = BackupCryptoHelper.encrypt(originalText, password)

        assertTrue(envelope.isEncrypted)
        assertEquals(2, envelope.version)
        assertEquals("AES-256-GCM", envelope.algorithm)
        assertNotEquals(originalText, envelope.ciphertext)

        val decryptedText = BackupCryptoHelper.decrypt(envelope, password)
        assertEquals(originalText, decryptedText)
    }

    @Test
    fun `decrypt with wrong password throws InvalidPasswordException`() {
        val originalText = "confidential backup payload"
        val correctPassword = "CorrectPassword_456"
        val wrongPassword = "WrongPassword_789"

        val envelope = BackupCryptoHelper.encrypt(originalText, correctPassword)

        assertThrows(InvalidPasswordException::class.java) {
            BackupCryptoHelper.decrypt(envelope, wrongPassword)
        }
    }

    @Test
    fun `tampered ciphertext fails decryption`() {
        val originalText = "unaltered content"
        val password = "SecurePassword"

        val envelope = BackupCryptoHelper.encrypt(originalText, password)

        val tamperedCiphertext = if (envelope.ciphertext.startsWith("A")) "B" + envelope.ciphertext.substring(1) else "A" + envelope.ciphertext.substring(1)
        val tamperedEnvelope = envelope.copy(ciphertext = tamperedCiphertext)

        assertThrows(InvalidPasswordException::class.java) {
            BackupCryptoHelper.decrypt(tamperedEnvelope, password)
        }
    }
}
