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
}
