package app.pwhs.universalinstaller.presentation.setting.ui

import android.content.Context
import android.util.Base64
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.BuildConfig
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.universalinstaller.ui.theme.fontsDir
import app.pwhs.universalinstaller.ui.theme.invalidateFontCache
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Everything settable in the app, split into logical export/import categories. Each category
 * owns the DataStore keys it covers; the union is the full app configuration. Device-local
 * keys (SAF grants: UI_EXPORT_DIR, APK_EXTRACTOR_OUTPUT_PATH) are deliberately excluded —
 * a persisted URI permission is meaningless on another device or after a reinstall.
 *
 * [id] doubles as the archive entry name (`<id>.json`) and as the id accepted in the automation
 * contract's `items` extra, so these strings are wire-visible: do not rename them.
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
    ;

    companion object {
        fun byId(id: String): ConfigCategory? = entries.firstOrNull { it.id == id }
    }
}

/**
 * One-file backup of the whole app configuration — the Kōjiki-style category ZIP (白い熊,
 * 2026-07-25): a `manifest.json` plus one `<category id>.json` per selected category, plus the
 * imported font files under `fonts/`. The fonts ride along because the per-surface themes
 * reference them by filename — without them a restored config would silently fall back to the
 * default font — and they are their own selectable sub-option ([FONTS_ITEM_ID]) since they are
 * the only bulky part of the archive.
 *
 * Every category is an independent entry: import iterates the categories present in the archive,
 * skips absent ones, and merges preference values per key (never clears), so an old export stays
 * importable forever. Pre-ZIP single-JSON exports still import, via [importLegacyJson].
 *
 * The export core is headless: [export] takes a selection, an [OutputStream] and a progress
 * callback, so the Export/Import panel and the automation receiver are two thin callers of the
 * same code. Nothing here touches the automation token — it lives in a SharedPreferences file of
 * its own and no DataStore key carries it, so it can never travel in a backup.
 */
object UiConfigBackup {
    const val FORMAT = "shiroikuma-universalinstaller-export"
    const val VERSION = 3

    /** `type` of the pre-ZIP single-JSON export, still accepted on import. */
    private const val LEGACY_TYPE = "shiroikuma-universalinstaller-ui-config"

    /**
     * Export file name prefix. The family convention (白い熊, 2026-07-25) is the app's English
     * dash-separated name plus a datetime and nothing else — no version, no infix, no suffix —
     * so every sister app's backups sort and read alike in one shared directory:
     * `shiroikuma-universal-installer_2026-07-25_18-58-23.zip`.
     */
    const val EXPORT_PREFIX = "shiroikuma-universal-installer_"

    /** The imported-fonts sub-option of [ConfigCategory.UiTheme] — a wire id, do not rename. */
    const val FONTS_ITEM_ID = "ui_theme.fonts"

    private const val MANIFEST_ENTRY = "manifest.json"
    private const val FONTS_DIR = "fonts"
    private const val MAX_ENTRY_BYTES = 48L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 256L * 1024 * 1024

    /** What one export/import covers: whole categories plus the fonts sub-option. */
    data class Selection(val cats: Set<ConfigCategory>, val fonts: Boolean) {
        val isEmpty: Boolean get() = cats.isEmpty() && !fonts

        /** Top-level categories represented in the archive (fonts count as `ui_theme`). */
        val categoryCount: Int
            get() = (cats + if (fonts) setOf(ConfigCategory.UiTheme) else emptySet()).size

        companion object {
            fun all(): Selection = Selection(ConfigCategory.entries.toSet(), fonts = true)
        }
    }

    /** One line of the automation contract's category list: `id<TAB>label[<TAB>parent]`. */
    data class Item(val id: String, val label: String, val parent: String? = null)

    /** The exportable items, parents before their children (contract order). */
    fun items(context: Context): List<Item> = buildList {
        ConfigCategory.entries.forEach { cat ->
            add(Item(cat.id, context.getString(cat.labelRes)))
            if (cat == ConfigCategory.UiTheme) {
                add(Item(FONTS_ITEM_ID, context.getString(R.string.eim_cat_fonts), cat.id))
            }
        }
    }

    /**
     * Resolves the automation contract's `items` ids to a [Selection]; null when any id is
     * unknown (the caller reports the whole list back as an error). A parent id without its
     * children means that category's own data only — `ui_theme` alone excludes the fonts.
     */
    fun selectionOf(ids: List<String>): Selection? {
        val cats = LinkedHashSet<ConfigCategory>()
        var fonts = false
        ids.forEach { id ->
            when {
                id == FONTS_ITEM_ID -> fonts = true
                else -> cats += ConfigCategory.byId(id) ?: return null
            }
        }
        return Selection(cats, fonts)
    }

    fun exportFileName(): String =
        EXPORT_PREFIX + SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date()) + ".zip"

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

    // ── export ────────────────────────────────────────────────────────────────────────────

    /**
     * Writes the [selection] to [output] as the category ZIP. [onProgress] (done, total, label)
     * fires after each written part — the automation receiver forwards it as contract progress
     * broadcasts; UI callers omit it. Returns the number of top-level categories written.
     */
    suspend fun export(
        context: Context,
        selection: Selection,
        output: OutputStream,
        onProgress: ((done: Int, total: Int, label: String) -> Unit)? = null,
    ): Int {
        val prefs = context.dataStore.data.first()
        val cats = ConfigCategory.entries.filter { it in selection.cats }
        val total = cats.size + if (selection.fonts) 1 else 0
        var done = 0
        ZipOutputStream(output.buffered()).use { zip ->
            writeEntry(zip, MANIFEST_ENTRY, manifest(context, selection).toByteArray())
            cats.forEach { cat ->
                writeEntry(zip, "${cat.id}.json", categoryJson(prefs, cat).toByteArray())
                done++
                onProgress?.invoke(done, total, context.getString(cat.labelRes))
            }
            if (selection.fonts) {
                fontsDir(context).listFiles()?.filter { it.isFile }?.forEach { file ->
                    writeEntry(zip, "$FONTS_DIR/${file.name}", file.readBytes())
                }
                done++
                onProgress?.invoke(done, total, context.getString(R.string.eim_cat_fonts))
            }
        }
        return selection.categoryCount
    }

    private fun manifest(context: Context, selection: Selection): String {
        val topLevel = ConfigCategory.entries.filter {
            it in selection.cats || (selection.fonts && it == ConfigCategory.UiTheme)
        }
        return JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("app", context.packageName)
            put("appVersion", BuildConfig.VERSION_NAME)
            put("createdTs", System.currentTimeMillis())
            put("categories", JSONArray(topLevel.map { it.id }))
            put(
                "items",
                JSONArray(
                    ConfigCategory.entries.filter { it in selection.cats }.map { it.id } +
                        if (selection.fonts) listOf(FONTS_ITEM_ID) else emptyList(),
                ),
            )
        }.toString(2)
    }

    /** One category's preference values, keyed by DataStore key name. */
    private fun categoryJson(prefs: Preferences, cat: ConfigCategory): String {
        val ks = keySets.getValue(cat)
        val p = JSONObject()
        ks.strings.forEach { k -> prefs[k]?.let { p.put(k.name, it) } }
        ks.ints.forEach { k -> prefs[k]?.let { p.put(k.name, it) } }
        ks.floats.forEach { k -> prefs[k]?.let { p.put(k.name, it.toDouble()) } }
        ks.bools.forEach { k -> prefs[k]?.let { p.put(k.name, it) } }
        ks.stringSets.forEach { k -> prefs[k]?.let { p.put(k.name, JSONArray(it.toList())) } }
        return p.toString(2)
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }

    // ── import ────────────────────────────────────────────────────────────────────────────

    /**
     * Restores the [selection] from a backup produced by [export] — or from a pre-ZIP single-JSON
     * export. Returns a per-category "label: n" summary, or failure when the file is not one of
     * ours or nothing in it matched the selection.
     */
    suspend fun import(context: Context, bytes: ByteArray, selection: Selection): Result<String> {
        if (!isZip(bytes)) return importLegacyJson(context, bytes.decodeToString(), selection)

        val entries = runCatching { readZip(ByteArrayInputStream(bytes)) }
            .getOrElse { return Result.failure(IllegalArgumentException(it.message ?: "unreadable archive")) }
        entries[MANIFEST_ENTRY]?.let { raw ->
            val format = runCatching { JSONObject(raw.decodeToString()).optString("format") }.getOrNull()
            if (format != FORMAT) return Result.failure(IllegalArgumentException("not a $FORMAT file"))
        }

        // Fonts first, so theme values that reference them resolve immediately.
        var fontCount = 0
        if (selection.fonts) {
            val dir = fontsDir(context)
            entries.filterKeys { it.startsWith("$FONTS_DIR/") }.forEach { (name, content) ->
                val base = File(name).name
                if (base.isNotEmpty() && runCatching { File(dir, base).writeBytes(content) }.isSuccess) {
                    fontCount++
                }
            }
            if (fontCount > 0) invalidateFontCache()
        }

        val counts = LinkedHashMap<ConfigCategory, Int>()
        context.dataStore.edit { prefs ->
            ConfigCategory.entries.filter { it in selection.cats }.forEach { cat ->
                val raw = entries["${cat.id}.json"] ?: return@forEach
                val values = runCatching { JSONObject(raw.decodeToString()) }.getOrNull() ?: return@forEach
                val n = applyCategory(prefs, cat, values)
                if (n > 0) counts[cat] = n
            }
        }
        return summarize(context, counts, fontCount)
    }

    /**
     * The pre-ZIP export: one JSON object with a flat `prefs` map covering every category and
     * base64-encoded `fonts`. Filtering is by key, so a v1 UI-only file imports fine too.
     */
    private suspend fun importLegacyJson(
        context: Context,
        content: String,
        selection: Selection,
    ): Result<String> {
        val root = runCatching { JSONObject(content) }.getOrNull()
            ?: return Result.failure(IllegalArgumentException("not a JSON file"))
        if (root.optString("type") != LEGACY_TYPE) {
            return Result.failure(IllegalArgumentException("not a $FORMAT file"))
        }
        val p = root.optJSONObject("prefs")
            ?: return Result.failure(IllegalArgumentException("no settings in file"))

        var fontCount = 0
        if (selection.fonts) {
            root.optJSONObject("fonts")?.let { fonts ->
                val dir = fontsDir(context)
                val names = fonts.keys()
                while (names.hasNext()) {
                    val name = names.next()
                    val decoded = runCatching {
                        File(dir, name).writeBytes(Base64.decode(fonts.getString(name), Base64.NO_WRAP))
                    }
                    if (decoded.isSuccess) fontCount++
                }
                if (fontCount > 0) invalidateFontCache()
            }
        }

        val counts = LinkedHashMap<ConfigCategory, Int>()
        context.dataStore.edit { prefs ->
            ConfigCategory.entries.filter { it in selection.cats }.forEach { cat ->
                val n = applyCategory(prefs, cat, p)
                if (n > 0) counts[cat] = n
            }
        }
        return summarize(context, counts, fontCount)
    }

    /** Merges the values present in [values] into [prefs] for [cat]; returns how many landed. */
    private fun applyCategory(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        cat: ConfigCategory,
        values: JSONObject,
    ): Int {
        var n = 0
        val ks = keySets.getValue(cat)
        ks.strings.forEach { k -> if (values.has(k.name)) { prefs[k] = values.getString(k.name); n++ } }
        ks.ints.forEach { k -> if (values.has(k.name)) { prefs[k] = values.getInt(k.name); n++ } }
        ks.floats.forEach { k ->
            if (values.has(k.name)) { prefs[k] = values.getDouble(k.name).toFloat(); n++ }
        }
        ks.bools.forEach { k -> if (values.has(k.name)) { prefs[k] = values.getBoolean(k.name); n++ } }
        ks.stringSets.forEach { k ->
            if (values.has(k.name)) {
                val arr = values.getJSONArray(k.name)
                prefs[k] = (0 until arr.length()).map { arr.getString(it) }.toSet()
                n++
            }
        }
        return n
    }

    private fun summarize(
        context: Context,
        counts: Map<ConfigCategory, Int>,
        fontCount: Int,
    ): Result<String> {
        if (counts.isEmpty() && fontCount == 0) {
            return Result.failure(IllegalArgumentException("no matching settings in file"))
        }
        val lines = counts.entries.map { (cat, n) -> "${context.getString(cat.labelRes)}: $n" } +
            if (fontCount > 0) listOf("${context.getString(R.string.eim_cat_fonts)}: $fontCount") else emptyList()
        return Result.success(lines.joinToString("\n"))
    }

    // ── zip helpers ───────────────────────────────────────────────────────────────────────

    /** True when [bytes] starts with the ZIP local-file magic (`PK`). */
    fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()

    /** All entries into memory, bounded per-entry and in total. */
    private fun readZip(input: InputStream): Map<String, ByteArray> {
        val out = LinkedHashMap<String, ByteArray>()
        var total = 0L
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) { zip.closeEntry(); continue }
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
                var entryBytes = 0L
                while (true) {
                    val read = zip.read(chunk)
                    if (read == -1) break
                    entryBytes += read
                    total += read
                    require(entryBytes <= MAX_ENTRY_BYTES) { "archive entry too large: ${entry.name}" }
                    require(total <= MAX_TOTAL_BYTES) { "archive too large" }
                    buffer.write(chunk, 0, read)
                }
                out[entry.name] = buffer.toByteArray()
                zip.closeEntry()
            }
        }
        return out
    }
}
