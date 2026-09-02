package app.pwhs.universalinstaller.presentation.install.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import app.pwhs.universalinstaller.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

object SourceFileDeleter {

    fun deleteSourceFile(context: Context, uri: Uri): Boolean {
        // 1. Direct file:// scheme
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            val path = uri.path
            if (path == null) {
                Timber.e("No path on file uri: $uri")
                return false
            }
            return runCatching { File(path).delete() }
                .onFailure { Timber.e(it, "Failed to delete source file: $uri") }
                .getOrDefault(false)
        }

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        // 2. DocumentsContract.deleteDocument
        val docDeleted = runCatching {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } else {
                false
            }
        }.getOrDefault(false)
        if (docDeleted) return true

        // 3. DocumentFile delete
        val docFileDeleted = runCatching {
            DocumentFile.fromSingleUri(context, uri)?.delete() == true
        }.getOrDefault(false)
        if (docFileDeleted) return true

        // 4. ContentResolver.delete
        val crDeleted = runCatching {
            context.contentResolver.delete(uri, null, null) > 0
        }.getOrDefault(false)
        if (crDeleted) return true

        // 5. Direct path resolution fallback
        return runCatching {
            val resolvedPath = resolveFilePathFromUri(context, uri)
            if (resolvedPath != null) {
                val file = File(resolvedPath)
                file.exists() && file.delete()
            } else {
                false
            }
        }.onFailure { Timber.e(it, "Failed to delete resolved file from uri: $uri") }
            .getOrDefault(false)
    }

    private fun resolveFilePathFromUri(context: Context, uri: Uri): String? {
        return runCatching {
            val rawPath = uri.path
            if (rawPath != null) {
                val storageIdx = rawPath.indexOf("/storage/")
                if (storageIdx != -1) {
                    val candidate = rawPath.substring(storageIdx)
                    if (File(candidate).exists()) return candidate
                }
            }

            if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                if (docId.startsWith("primary:")) {
                    val relativePath = docId.substringAfter("primary:")
                    val file = File(android.os.Environment.getExternalStorageDirectory(), relativePath)
                    if (file.exists()) return file.absolutePath
                }
            }

            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val colIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (colIdx != -1) {
                        val filePath = cursor.getString(colIdx)
                        if (!filePath.isNullOrBlank() && File(filePath).exists()) {
                            return filePath
                        }
                    }
                }
            }
            null
        }.getOrNull()
    }

    suspend fun deleteSourceFileAndWarn(context: Context, uri: Uri) {
        if (deleteSourceFile(context, uri)) {
            Timber.d("Deleted source file: $uri")
            return
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                context.getString(R.string.install_delete_source_failed),
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
