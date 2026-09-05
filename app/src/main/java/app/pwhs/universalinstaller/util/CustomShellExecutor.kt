package app.pwhs.universalinstaller.util

import android.content.Context
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.PrintWriter
import java.util.concurrent.TimeUnit

/**
 * Executes shell commands through a user-configured authorizer template.
 *
 * Supports two execution modes inspired by InstallerX-Revived:
 * 1. Placeholder mode: When the template contains `{command}`, the placeholder is replaced
 *    with the command arguments (e.g. `su -c {command}` or `sh /path/to/rish -c {command}`).
 * 2. Stdin terminal mode: When no placeholder is present, the process is started with the
 *    template tokens (e.g. `su 1000` or `su`), and the command is piped to its stdin.
 */
object CustomShellExecutor {

    const val COMMAND_PLACEHOLDER = "{command}"

    data class Result(
        val code: Int,
        val out: List<String>,
        val err: List<String>,
    ) {
        val isSuccess: Boolean get() = code == 0
        val text: String get() = (out + err).joinToString("\n").trim()
    }

    suspend fun getCommandTemplate(context: Context): String {
        val prefs = runCatching { context.dataStore.data.first() }.getOrNull()
        return prefs?.get(PreferencesKeys.CUSTOM_AUTHORIZER_COMMAND)?.trim()?.ifBlank { null }
            ?: PreferencesKeys.DEFAULT_CUSTOM_AUTHORIZER_COMMAND
    }

    suspend fun exec(
        context: Context,
        command: String,
        timeoutSeconds: Long = 60L,
    ): Result {
        val template = getCommandTemplate(context)
        return execWithTemplate(template, command, timeoutSeconds)
    }

    suspend fun testCommand(template: String): kotlin.Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val result = execWithTemplate(template, "id", timeoutSeconds = 8L)
            if (result.isSuccess) {
                val line = result.out.firstOrNull { it.contains("uid=", ignoreCase = true) }
                    ?: result.out.firstOrNull()?.trim()
                    ?: "OK (exit 0)"
                line.trim()
            } else {
                val err = result.err.joinToString("\n").ifBlank { result.out.joinToString("\n") }
                throw RuntimeException("Exit code ${result.code}: ${err.ifBlank { "Command failed" }}")
            }
        }
    }

    suspend fun execWithTemplate(
        template: String,
        command: String,
        timeoutSeconds: Long = 60L,
    ): Result = withContext(Dispatchers.IO) {
        val cleanTemplate = template.trim().ifBlank { PreferencesKeys.DEFAULT_CUSTOM_AUTHORIZER_COMMAND }
        val parts = parseCommand(cleanTemplate)
        if (parts.isEmpty()) {
            return@withContext Result(-1, emptyList(), listOf("Empty custom authorizer command"))
        }

        try {
            val hasPlaceholder = parts.any { it.contains(COMMAND_PLACEHOLDER) }
            val process = if (hasPlaceholder) {
                val cmdList = parts.map { part ->
                    if (part.contains(COMMAND_PLACEHOLDER)) {
                        part.replace(COMMAND_PLACEHOLDER, command)
                    } else {
                        part
                    }
                }
                ProcessBuilder(cmdList).start()
            } else {
                val process = ProcessBuilder(parts).start()
                PrintWriter(process.outputStream, true).use { writer ->
                    writer.println(command)
                    writer.println("exit $?")
                }
                process
            }

            val outLines = mutableListOf<String>()
            val errLines = mutableListOf<String>()

            val outReader = Thread {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { outLines.add(it) }
                }
            }
            val errReader = Thread {
                process.errorStream.bufferedReader().useLines { lines ->
                    lines.forEach { errLines.add(it) }
                }
            }
            outReader.start()
            errReader.start()

            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext Result(-1, outLines, listOf("Command timed out after ${timeoutSeconds}s"))
            }

            outReader.join(1000)
            errReader.join(1000)

            Result(process.exitValue(), outLines, errLines)
        } catch (e: Exception) {
            Timber.e(e, "CustomShellExecutor failed to execute command: $command")
            Result(-1, emptyList(), listOf(e.message ?: "Execution failed"))
        }
    }

    /**
     * Splits command string into tokens respecting single and double quotes.
     */
    fun parseCommand(command: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inSingleQuote = false
        var inDoubleQuote = false
        var escaped = false

        for (char in command) {
            when {
                escaped -> {
                    current.append(char)
                    escaped = false
                }
                char == '\\' -> {
                    escaped = true
                }
                char == '\'' && !inDoubleQuote -> {
                    inSingleQuote = !inSingleQuote
                }
                char == '"' && !inSingleQuote -> {
                    inDoubleQuote = !inDoubleQuote
                }
                char.isWhitespace() && !inSingleQuote && !inDoubleQuote -> {
                    if (current.isNotEmpty()) {
                        result.add(current.toString())
                        current.setLength(0)
                    }
                }
                else -> {
                    current.append(char)
                }
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        return result
    }
}
