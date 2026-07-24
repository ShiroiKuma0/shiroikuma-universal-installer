package app.pwhs.universalinstaller.presentation.setting.ui

import android.content.Context
import android.util.Base64
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.ui.theme.fontsDir
import app.pwhs.universalinstaller.ui.theme.invalidateFontCache
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Everything settable in the app, split into logical export/import categories. Each category
 * owns the DataStore keys it covers; the union is the full app configuration. Device-local
 * keys (SAF grants: UI_EXPORT_DIR, APK_EXTRACTOR_OUTPUT_PATH) are deliberately excluded —
 * a persisted URI permission is meaningless on another device or after a reinstall.
 */
enum class ConfigCategory(val id: String, @StringRes val labelRes: Int) {
    UiTheme("ui_theme", R.string.eim_cat_ui_theme),
    AppTheme("app_theme", R.string.eim_cat_app_theme),
    InstallBehavior("install_behavior", R.string.eim_cat_install),
    Engines("engines", R.string.eim_cat_engines),
    Profiles("profiles", R.string.eim_cat_profiles),
    Security("security", R.string.eim_cat_security),
    Sync("sync", R.string.eim_cat_sync),
    Manage("manage", R.string.eim_cat_manage),
}

/**
 * One-file backup of the app configuration: the preference values of the selected
 * [ConfigCategory]s plus (for the UI-theme category) any imported font files, base64-encoded,
 * so the look can be restored verbatim on a new device with a single import. The fonts ride
 * along because the per-surface themes reference them by filename — without them a restored
 * config would silently fall back to the default font.
 */
object UiConfigBackup {
    private const val TYPE = "shiroikuma-universalinstaller-ui-config"
    private const val VERSION = 2

    private class KeySet(
        val strings: List<Preferences.Key<String>> = emptyList(),
        val ints: List<Preferences.Key<Int>> = emptyList(),
        val floats: List<Preferences.Key<Float>> = emptyList(),
        val bools: List<Preferences.Key<Boolean>> = emptyList(),
        val stringSets: List<Preferences.Key<Set<String>>> = emptyList(),
    )

    private val keySets: Map<ConfigCategory, KeySet> = mapOf(
        ConfigCategory.UiTheme to KeySet(
            strings = listOf(
                PreferencesKeys.UI_FONT_FAMILY,
                PreferencesKeys.UI_DIALOG_THEME,
                PreferencesKeys.UI_MAIN_THEME,
                PreferencesKeys.UI_BOTTOM_BAR_THEME,
                PreferencesKeys.UI_RECENT_COLORS,
            ),
            ints = listOf(PreferencesKeys.UI_FONT_WEIGHT, PreferencesKeys.UI_ACCENT_COLOR),
            floats = listOf(PreferencesKeys.UI_FONT_SCALE, PreferencesKeys.UI_CORNER_SCALE),
            bools = listOf(PreferencesKeys.UI_MONO_TECHNICAL),
        ),
        ConfigCategory.AppTheme to KeySet(
            strings = listOf(PreferencesKeys.THEME_MODE, PreferencesKeys.THEME_PRESET),
            bools = listOf(PreferencesKeys.DYNAMIC_COLOR, PreferencesKeys.AMOLED_MODE),
        ),
        ConfigCategory.InstallBehavior to KeySet(
            strings = listOf(PreferencesKeys.INSTALLER_OVERRIDES),
            ints = listOf(PreferencesKeys.INSTALL_USER_ID),
            bools = listOf(
                PreferencesKeys.USE_SHIZUKU,
                PreferencesKeys.USE_ROOT,
                PreferencesKeys.DELETE_APK_AFTER_INSTALL,
                PreferencesKeys.AUTO_OPEN_AFTER_INSTALL,
                PreferencesKeys.AUTO_CONFIRM_EXTERNAL_INSTALL,
                PreferencesKeys.DIALOG_INSTALL_MODE,
                PreferencesKeys.SHOW_DOWNLOAD_TAB,
            ),
        ),
        ConfigCategory.Engines to KeySet(
            strings = listOf(
                PreferencesKeys.SHIZUKU_INSTALLER_PACKAGE_NAME,
                PreferencesKeys.ROOT_INSTALLER_PACKAGE_NAME,
            ),
            bools = listOf(
                PreferencesKeys.SHIZUKU_BYPASS_LOW_TARGET_SDK,
                PreferencesKeys.SHIZUKU_ALLOW_TEST,
                PreferencesKeys.SHIZUKU_REPLACE_EXISTING,
                PreferencesKeys.SHIZUKU_REQUEST_DOWNGRADE,
                PreferencesKeys.SHIZUKU_GRANT_ALL_PERMISSIONS,
                PreferencesKeys.SHIZUKU_ALL_USERS,
                PreferencesKeys.SHIZUKU_SET_INSTALL_SOURCE,
                PreferencesKeys.SHIZUKU_UNINSTALL_KEEP_DATA,
                PreferencesKeys.SHIZUKU_UNINSTALL_ALL_USERS,
                PreferencesKeys.ROOT_BYPASS_LOW_TARGET_SDK,
                PreferencesKeys.ROOT_ALLOW_TEST,
                PreferencesKeys.ROOT_REPLACE_EXISTING,
                PreferencesKeys.ROOT_REQUEST_DOWNGRADE,
                PreferencesKeys.ROOT_GRANT_ALL_PERMISSIONS,
                PreferencesKeys.ROOT_ALL_USERS,
                PreferencesKeys.ROOT_SET_INSTALL_SOURCE,
            ),
        ),
        ConfigCategory.Profiles to KeySet(
            strings = listOf(PreferencesKeys.INSTALLER_PROFILES, PreferencesKeys.APP_PROFILE_MAPPING),
        ),
        ConfigCategory.Security to KeySet(
            strings = listOf(PreferencesKeys.VIRUSTOTAL_API_KEY),
            bools = listOf(
                PreferencesKeys.STRICT_VIRUSTOTAL_CHECK,
                PreferencesKeys.BIOMETRIC_LOCK_INSTALL,
                PreferencesKeys.BIOMETRIC_LOCK_UNINSTALL,
            ),
        ),
        ConfigCategory.Sync to KeySet(
            strings = listOf(PreferencesKeys.SYNC_PIN_CODE, PreferencesKeys.SYNC_SERVER_PORT),
            bools = listOf(PreferencesKeys.SYNC_REQUIRE_PIN),
        ),
        ConfigCategory.Manage to KeySet(
            strings = listOf(
                PreferencesKeys.MANAGE_SORT_BY,
                PreferencesKeys.MANAGE_SORT_DIRECTION,
                PreferencesKeys.MANAGE_GROUP_BY,
                PreferencesKeys.APK_EXTRACTOR_FILENAME_TEMPLATE,
                PreferencesKeys.APK_EXTRACTOR_SPLIT_FORMAT,
            ),
            stringSets = listOf(PreferencesKeys.MANAGE_APP_FILTER),
        ),
    )

    /** Serialise the current config for the selected [cats] (prefs + imported fonts) to JSON. */
    suspend fun export(context: Context, cats: Set<ConfigCategory>): String {
        val prefs = context.dataStore.data.first()
        val p = JSONObject()
        cats.forEach { cat ->
            val ks = keySets.getValue(cat)
            ks.strings.forEach { k -> prefs[k]?.let { p.put(k.name, it) } }
            ks.ints.forEach { k -> prefs[k]?.let { p.put(k.name, it) } }
            ks.floats.forEach { k -> prefs[k]?.let { p.put(k.name, it.toDouble()) } }
            ks.bools.forEach { k -> prefs[k]?.let { p.put(k.name, it) } }
            ks.stringSets.forEach { k -> prefs[k]?.let { p.put(k.name, JSONArray(it.toList())) } }
        }

        val fonts = JSONObject()
        if (ConfigCategory.UiTheme in cats) {
            fontsDir(context).listFiles()?.filter { it.isFile }?.forEach { f ->
                fonts.put(f.name, Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
            }
        }

        return JSONObject().apply {
            put("type", TYPE)
            put("version", VERSION)
            put("categories", JSONArray(cats.map { it.id }))
            put("prefs", p)
            put("fonts", fonts)
        }.toString(2)
    }

    /**
     * Restore the selected [cats] from a config produced by [export] (v1 UI-only files import
     * fine — filtering is by key, not by the file's own category list). Returns a per-category
     * "label: n" summary, or failure when the file is not a config export or nothing matched.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    suspend fun import(context: Context, content: String, cats: Set<ConfigCategory>): Result<String> {
        val root = try { JSONObject(content) } catch (e: Exception) { null }
            ?: return Result.failure(IllegalArgumentException("not a JSON file"))
        if (root.optString("type") != TYPE) {
            return Result.failure(IllegalArgumentException("not a $TYPE file"))
        }
        val p = root.optJSONObject("prefs")
            ?: return Result.failure(IllegalArgumentException("no settings in file"))

        // Restore fonts first so theme values that reference them resolve immediately.
        var fontCount = 0
        if (ConfigCategory.UiTheme in cats) {
            root.optJSONObject("fonts")?.let { fonts ->
                val dir = fontsDir(context)
                val names = fonts.keys()
                while (names.hasNext()) {
                    val name = names.next()
                    try {
                        File(dir, name).writeBytes(Base64.decode(fonts.getString(name), Base64.NO_WRAP))
                        fontCount++
                    } catch (e: Exception) {
                        // Skip a bad entry rather than aborting the whole import.
                    }
                }
                if (fontCount > 0) invalidateFontCache()
            }
        }

        val counts = LinkedHashMap<ConfigCategory, Int>()
        context.dataStore.edit { prefs ->
            cats.forEach { cat ->
                var n = 0
                val ks = keySets.getValue(cat)
                ks.strings.forEach { k -> if (p.has(k.name)) { prefs[k] = p.getString(k.name); n++ } }
                ks.ints.forEach { k -> if (p.has(k.name)) { prefs[k] = p.getInt(k.name); n++ } }
                ks.floats.forEach { k -> if (p.has(k.name)) { prefs[k] = p.getDouble(k.name).toFloat(); n++ } }
                ks.bools.forEach { k -> if (p.has(k.name)) { prefs[k] = p.getBoolean(k.name); n++ } }
                ks.stringSets.forEach { k ->
                    if (p.has(k.name)) {
                        val arr = p.getJSONArray(k.name)
                        prefs[k] = (0 until arr.length()).map { arr.getString(it) }.toSet()
                        n++
                    }
                }
                if (n > 0) counts[cat] = n
            }
        }
        if (ConfigCategory.UiTheme in cats && fontCount > 0) {
            counts[ConfigCategory.UiTheme] = (counts[ConfigCategory.UiTheme] ?: 0) + fontCount
        }
        if (counts.isEmpty()) {
            return Result.failure(IllegalArgumentException("no matching settings in file"))
        }
        return Result.success(
            counts.entries.joinToString("\n") { (cat, n) -> "${context.getString(cat.labelRes)}: $n" }
        )
    }
}
