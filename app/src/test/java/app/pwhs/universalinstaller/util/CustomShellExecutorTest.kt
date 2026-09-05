package app.pwhs.universalinstaller.util

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomShellExecutorTest {

    @Test
    fun parseCommand_splitsTokensCorrectly() {
        val tokens1 = CustomShellExecutor.parseCommand("su -c {command}")
        assertEquals(listOf("su", "-c", "{command}"), tokens1)

        val tokens2 = CustomShellExecutor.parseCommand("su 1000")
        assertEquals(listOf("su", "1000"), tokens2)

        val tokens3 = CustomShellExecutor.parseCommand("sh /path/to/rish -c {command}")
        assertEquals(listOf("sh", "/path/to/rish", "-c", "{command}"), tokens3)

        val tokens4 = CustomShellExecutor.parseCommand("sh -c 'echo \"hello world\"'")
        assertEquals(listOf("sh", "-c", "echo \"hello world\""), tokens4)
    }

    @Test
    fun execWithTemplate_placeholderMode_runsCommand() = runBlocking {
        val result = CustomShellExecutor.execWithTemplate("sh -c {command}", "echo hello_custom")
        assertTrue(result.isSuccess)
        assertEquals(0, result.code)
        assertTrue(result.out.any { it.contains("hello_custom") })
    }

    @Test
    fun execWithTemplate_stdinMode_runsCommand() = runBlocking {
        val result = CustomShellExecutor.execWithTemplate("sh", "echo hello_stdin")
        assertTrue(result.isSuccess)
        assertEquals(0, result.code)
        assertTrue(result.out.any { it.contains("hello_stdin") })
    }

    @Test
    fun testCommand_executesSuccessfully() = runBlocking {
        val testRes = CustomShellExecutor.testCommand("sh -c {command}")
        assertTrue(testRes.isSuccess)
        val text = testRes.getOrNull().orEmpty()
        assertTrue(text.isNotBlank())
    }

    @Test
    fun validateCommand_detectsDangerousCommands() {
        val res1 = CustomShellExecutor.validateCommand("rm -rf /")
        assertTrue(res1 is CustomShellExecutor.ValidationResult.Error)
        assertEquals(CustomShellExecutor.ValidationErrorReason.DANGEROUS_COMMAND, (res1 as CustomShellExecutor.ValidationResult.Error).reason)

        val res2 = CustomShellExecutor.validateCommand("su -c 'rm -rf {command}'")
        assertTrue(res2 is CustomShellExecutor.ValidationResult.Error)
        assertEquals(CustomShellExecutor.ValidationErrorReason.DANGEROUS_COMMAND, (res2 as CustomShellExecutor.ValidationResult.Error).reason)

        val res3 = CustomShellExecutor.validateCommand("reboot")
        assertTrue(res3 is CustomShellExecutor.ValidationResult.Error)
        assertEquals(CustomShellExecutor.ValidationErrorReason.DANGEROUS_COMMAND, (res3 as CustomShellExecutor.ValidationResult.Error).reason)
    }

    @Test
    fun validateCommand_detectsNonAuthorizerUtilities() {
        val res1 = CustomShellExecutor.validateCommand("ls -la")
        assertTrue(res1 is CustomShellExecutor.ValidationResult.Error)
        assertEquals(CustomShellExecutor.ValidationErrorReason.NOT_AUTHORIZER_BINARY, (res1 as CustomShellExecutor.ValidationResult.Error).reason)

        val res2 = CustomShellExecutor.validateCommand("cat /etc/hosts")
        assertTrue(res2 is CustomShellExecutor.ValidationResult.Error)
        assertEquals(CustomShellExecutor.ValidationErrorReason.NOT_AUTHORIZER_BINARY, (res2 as CustomShellExecutor.ValidationResult.Error).reason)
    }

    @Test
    fun validateCommand_detectsMissingPlaceholder() {
        val res = CustomShellExecutor.validateCommand("su -c")
        assertTrue(res is CustomShellExecutor.ValidationResult.Error)
        assertEquals(CustomShellExecutor.ValidationErrorReason.MISSING_PLACEHOLDER, (res as CustomShellExecutor.ValidationResult.Error).reason)
    }

    @Test
    fun validateCommand_acceptsValidAuthorizerCommands() {
        assertTrue(CustomShellExecutor.validateCommand("su -c {command}") is CustomShellExecutor.ValidationResult.Valid)
        assertTrue(CustomShellExecutor.validateCommand("su 1000") is CustomShellExecutor.ValidationResult.Valid)
        assertTrue(CustomShellExecutor.validateCommand("ksu -c {command}") is CustomShellExecutor.ValidationResult.Valid)
        assertTrue(CustomShellExecutor.validateCommand("rish -c {command}") is CustomShellExecutor.ValidationResult.Valid)
        assertTrue(CustomShellExecutor.validateCommand("/system/bin/sh -c {command}") is CustomShellExecutor.ValidationResult.Valid)
    }

    @Test
    fun execWithTemplate_rejectsDangerousCommands() = runBlocking {
        val res = CustomShellExecutor.execWithTemplate("rm -rf {command}", "id")
        assertTrue(!res.isSuccess)
        assertTrue(res.err.any { it.contains("Unsafe or invalid authorizer command") })
    }
}
