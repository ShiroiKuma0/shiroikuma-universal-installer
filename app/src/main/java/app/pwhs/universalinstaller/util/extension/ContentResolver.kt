package app.pwhs.universalinstaller.util.extension

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toFile

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Best-effort display name for [uri], or "" when it cannot be read.
 *
 * Handles file://, content://, http://, https://, and safely decodes URL percent-encoding
 * (such as SMB and network storage provider paths) into clean human-readable filenames.
 */
fun ContentResolver.getDisplayName(uri: Uri): String = runCatching {
    when (uri.scheme) {
        ContentResolver.SCHEME_FILE -> {
            decodePath(uri.toFile().name)
        }
        "http", "https" -> {
            val lastSegment = uri.lastPathSegment?.substringBefore('?')?.substringBefore('#')
            decodePath(lastSegment.orEmpty().ifEmpty { "download.apk" })
        }
        else -> {
            val queriedName = query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null).use { cursor ->
                if (cursor == null || !cursor.moveToFirst()) "" else cursor.getString(0).orEmpty()
            }
            if (queriedName.isNotBlank()) {
                decodePath(queriedName)
            } else {
                decodePath(uri.lastPathSegment.orEmpty())
            }
        }
    }
}.getOrDefault("").ifEmpty {
    decodePath(uri.lastPathSegment.orEmpty())
}

/**
 * Decodes URL percent-encoding (e.g. `%20` -> space) while preserving literal plus signs.
 */
private fun decodePath(name: String): String = runCatching {
    if (!name.contains('%')) return name
    val encodedWithPlusEscaped = name.replace("+", "%2B")
    URLDecoder.decode(encodedWithPlusEscaped, StandardCharsets.UTF_8.name())
}.getOrDefault(name)