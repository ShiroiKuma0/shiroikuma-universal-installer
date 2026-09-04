package app.pwhs.universalinstaller.presentation.manage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import app.pwhs.universalinstaller.R

private const val EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents"

internal fun formatExtractorOutputPath(
    context: Context,
    path: String,
): String {
    if (path.isBlank()) return context.getString(R.string.backup_output_path_default)
    if (!path.startsWith("content://")) return path

    val uri = Uri.parse(path)
    readableExternalStoragePath(
        uri = uri,
        internalStorageLabel = context.getString(R.string.backup_output_path_internal_storage),
    )?.let { return it }

    return DocumentFile.fromTreeUri(context, uri)
        ?.name
        ?.takeIf { it.isNotBlank() && !it.startsWith("content://") }
        ?: context.getString(R.string.backup_output_path_selected_folder)
}

private fun readableExternalStoragePath(
    uri: Uri,
    internalStorageLabel: String,
): String? {
    if (uri.authority != EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY) return null

    val encodedTreeId = uri.pathSegments
        .windowed(size = 2, step = 1)
        .firstOrNull { it.first() == "tree" }
        ?.lastOrNull()
        ?: return null

    val treeId = Uri.decode(encodedTreeId)
    val relativePath = treeId.substringAfter(':', missingDelimiterValue = treeId)
    return relativePath.ifBlank { internalStorageLabel }
}
