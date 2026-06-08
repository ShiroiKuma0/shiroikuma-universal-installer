package app.pwhs.core.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import app.pwhs.core.domain.ApkFile

/**
 * Finds installable APK/bundle files already on the device via MediaStore. On Android TV
 * there's no file picker and no all-files-access settings screen, so MediaStore (with
 * READ_EXTERNAL_STORAGE on API ≤32) is the practical way to list sideloaded packages.
 *
 * Returns content URIs the installer reads through the resolver — no direct file paths.
 */
object DownloadsApkScanner {

    private val BUNDLE_EXTS = setOf("apks", "xapk", "apkm", "apk+")

    fun scan(context: Context): List<ApkFile> {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
        )
        val nameCol = MediaStore.Files.FileColumns.DISPLAY_NAME
        val selection = listOf("apk", "apks", "xapk", "apkm")
            .joinToString(" OR ") { "$nameCol LIKE '%.$it'" }

        val out = mutableListOf<ApkFile>()
        runCatching {
            context.contentResolver.query(collection, projection, selection, null, "$nameCol ASC")
                ?.use { c ->
                    val idIdx = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameIdx = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val sizeIdx = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                    while (c.moveToNext()) {
                        val name = c.getString(nameIdx) ?: continue
                        val uri = ContentUris.withAppendedId(collection, c.getLong(idIdx))
                        out += ApkFile(
                            uri = uri.toString(),
                            displayName = name,
                            sizeBytes = c.getLong(sizeIdx),
                            isBundle = name.substringAfterLast('.', "").lowercase() in BUNDLE_EXTS,
                        )
                    }
                }
        }
        return out
    }
}
