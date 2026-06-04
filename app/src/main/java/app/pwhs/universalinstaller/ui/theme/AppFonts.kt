package app.pwhs.universalinstaller.ui.theme

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import app.pwhs.universalinstaller.R
import java.io.File

/**
 * External-font support for the "白い熊 Installer UI" page — a Compose port of the sister forks'
 * `extensions/Fonts.kt`. The user can pick a `.ttf`/`.otf`, which is copied into the app's private
 * fonts dir and applied app-wide as the [FontFamily] of every Material text style.
 */

private val FONT_EXTENSIONS = setOf("ttf", "otf")

/** Sentinel family values that can't be real filenames. "" = system default. */
const val MONOSPACE_FONT = "@monospace"

/** A selectable font weight. value 0 = leave the family's own default weight. */
enum class FontWeightOption(val value: Int, @StringRes val labelRes: Int) {
    DEFAULT(0, R.string.font_weight_default),
    THIN(100, R.string.font_weight_thin),
    LIGHT(300, R.string.font_weight_light),
    REGULAR(400, R.string.font_weight_regular),
    MEDIUM(500, R.string.font_weight_medium),
    SEMIBOLD(600, R.string.font_weight_semibold),
    BOLD(700, R.string.font_weight_bold),
    BLACK(900, R.string.font_weight_black);

    companion object {
        fun fromValue(value: Int) = entries.firstOrNull { it.value == value } ?: DEFAULT
    }
}

/** One pickable font; an empty [fileName] means "system / global default". */
data class FontOption(val displayName: String, val fileName: String)

private val familyCache = HashMap<String, FontFamily>()

/** Private dir holding imported font files (shared across the whole app). */
fun fontsDir(context: Context): File = File(context.filesDir, "fonts").apply { mkdirs() }

/** Built-in families + every font the user has imported. */
fun Context.availableFontOptions(): List<FontOption> {
    val options = mutableListOf(
        FontOption(getString(R.string.font_system_default), ""),
        FontOption(getString(R.string.font_monospace), MONOSPACE_FONT),
    )
    fontsDir(this).listFiles()
        ?.filter { it.isFile && it.extension.lowercase() in FONT_EXTENSIONS }
        ?.sortedBy { it.name.lowercase() }
        ?.forEach { options.add(FontOption(it.nameWithoutExtension, it.name)) }
    return options
}

/** Human-readable name for a stored family value. */
fun Context.fontDisplayName(fileName: String): String = when {
    fileName.isEmpty() -> getString(R.string.font_system_default)
    fileName == MONOSPACE_FONT -> getString(R.string.font_monospace)
    else -> File(fileName).nameWithoutExtension
}

/**
 * Compose [FontFamily] for a stored family value, cached. Returns `null` for the system default so
 * callers can leave the base typography's family untouched. A bad/corrupt file falls back to default.
 */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
fun composeFontFamily(context: Context, fileName: String): FontFamily? = when {
    fileName.isEmpty() -> null
    fileName == MONOSPACE_FONT -> FontFamily.Monospace
    else -> familyCache.getOrPut(fileName) {
        try {
            FontFamily(Font(File(fontsDir(context), fileName)))
        } catch (e: Exception) {
            FontFamily.Default
        }
    }
}

/** Monospace family when "monospace for technical text" is on, else null (inherit). */
@Composable
fun technicalFontFamily(): FontFamily? =
    if (LocalMonoTechnical.current) FontFamily.Monospace else null

/** Copy a picked font file into the shared app fonts dir; returns its filename, or null on failure. */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
fun Context.importFont(uri: Uri): String? {
    val name = fontFileName(uri) ?: return null
    if (name.substringAfterLast('.', "").lowercase() !in FONT_EXTENSIONS) {
        return null
    }
    val bytes = try {
        contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    } catch (e: Exception) {
        return null
    }
    return try {
        File(fontsDir(this), name).writeBytes(bytes)
        familyCache.remove(name)
        name
    } catch (e: Exception) {
        null
    }
}

private fun Context.fontFileName(uri: Uri): String? {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) {
                return cursor.getString(index)
            }
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/')
}
