package app.pwhs.universalinstaller.util.extension

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toFile

/**
 * Best-effort display name for [uri], or "" when it cannot be read.
 *
 * Querying another app's provider throws SecurityException the moment its grant lapses, and this
 * runs on the main thread from composition — an unreadable URI used to take the whole app down
 * with it rather than surface as an error the user could read.
 */
fun ContentResolver.getDisplayName(uri: Uri): String = runCatching {
    if (uri.scheme == ContentResolver.SCHEME_FILE) {
        return@runCatching uri.toFile().name
    }
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null).use { cursor ->
        if (cursor == null || !cursor.moveToFirst()) "" else cursor.getString(0).orEmpty()
    }
}.getOrDefault("")