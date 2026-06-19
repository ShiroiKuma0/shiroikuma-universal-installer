package app.pwhs.universalinstaller.presentation.setting.ui

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.ui.theme.fontsDir
import app.pwhs.universalinstaller.ui.theme.invalidateFontCache
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.io.File

/**
 * One-file backup of the 白い熊 Installer UI configuration: the UI preference values plus any imported
 * font files (base64-encoded), so the look can be restored verbatim on a new device with a single
 * import. The fonts ride along because the per-surface themes reference them by filename — without them
 * a restored config would silently fall back to the default font.
 */
object UiConfigBackup {
    private const val TYPE = "shiroikuma-universalinstaller-ui-config"
    private const val VERSION = 1

    // String-valued UI prefs (includes the per-surface theme JSON blobs and the theme mode).
    private val stringKeys = listOf(
        PreferencesKeys.UI_FONT_FAMILY,
        PreferencesKeys.UI_DIALOG_THEME,
        PreferencesKeys.UI_MAIN_THEME,
        PreferencesKeys.UI_BOTTOM_BAR_THEME,
        PreferencesKeys.UI_RECENT_COLORS,
        PreferencesKeys.THEME_MODE,
    )
    private val intKeys = listOf(PreferencesKeys.UI_FONT_WEIGHT, PreferencesKeys.UI_ACCENT_COLOR)
    private val floatKeys = listOf(PreferencesKeys.UI_FONT_SCALE, PreferencesKeys.UI_CORNER_SCALE)
    private val boolKeys = listOf(
        PreferencesKeys.UI_MONO_TECHNICAL,
        PreferencesKeys.DYNAMIC_COLOR,
        PreferencesKeys.AMOLED_MODE,
    )

    /** Serialise the current UI config (prefs + imported fonts) to a JSON string. */
    suspend fun export(context: Context): String {
        val prefs = context.dataStore.data.first()
        val p = JSONObject()
        stringKeys.forEach { k -> prefs[k]?.let { p.put(k.name, it) } }
        intKeys.forEach { k -> prefs[k]?.let { p.put(k.name, it) } }
        floatKeys.forEach { k -> prefs[k]?.let { p.put(k.name, it.toDouble()) } }
        boolKeys.forEach { k -> prefs[k]?.let { p.put(k.name, it) } }

        val fonts = JSONObject()
        fontsDir(context).listFiles()?.filter { it.isFile }?.forEach { f ->
            fonts.put(f.name, Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
        }

        return JSONObject().apply {
            put("type", TYPE)
            put("version", VERSION)
            put("prefs", p)
            put("fonts", fonts)
        }.toString(2)
    }

    /** Restore a config produced by [export]. Returns true on success. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    suspend fun import(context: Context, content: String): Boolean {
        val root = try { JSONObject(content) } catch (e: Exception) { return false }
        if (root.optString("type") != TYPE) return false
        val p = root.optJSONObject("prefs") ?: return false

        // Restore fonts first so theme values that reference them resolve immediately.
        root.optJSONObject("fonts")?.let { fonts ->
            val dir = fontsDir(context)
            val names = fonts.keys()
            while (names.hasNext()) {
                val name = names.next()
                try {
                    File(dir, name).writeBytes(Base64.decode(fonts.getString(name), Base64.NO_WRAP))
                } catch (e: Exception) {
                    // Skip a bad entry rather than aborting the whole import.
                }
            }
            invalidateFontCache()
        }

        context.dataStore.edit { prefs ->
            stringKeys.forEach { k -> if (p.has(k.name)) prefs[k] = p.getString(k.name) }
            intKeys.forEach { k -> if (p.has(k.name)) prefs[k] = p.getInt(k.name) }
            floatKeys.forEach { k -> if (p.has(k.name)) prefs[k] = p.getDouble(k.name).toFloat() }
            boolKeys.forEach { k -> if (p.has(k.name)) prefs[k] = p.getBoolean(k.name) }
        }
        return true
    }
}
