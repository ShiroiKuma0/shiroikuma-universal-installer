package app.pwhs.universalinstaller.presentation.setting.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.core.data.local.dataStore
import app.pwhs.universalinstaller.ui.theme.AppSurface
import app.pwhs.universalinstaller.ui.theme.BottomBarTheme
import app.pwhs.universalinstaller.ui.theme.ForkUiDefaults
import app.pwhs.universalinstaller.ui.theme.BottomBarThemeStore
import app.pwhs.universalinstaller.ui.theme.SurfaceTheme
import app.pwhs.universalinstaller.ui.theme.SurfaceThemeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Common prefix of every config export file name (also matches pre-category UI-only exports). */
private const val EXPORT_PREFIX = UiConfigBackup.EXPORT_PREFIX

/** State of the "latest export in the configured directory" query. */
sealed interface LastExport {
    data object NoDir : LastExport
    data object None : LastExport
    data class Found(val name: String, val formatted: String) : LastExport
}

data class InstallerUiState(
    val fontFamily: String = "",
    val fontWeight: Int = 0,
    val fontScale: Float = 1f,
    val monoTechnical: Boolean = false,
    val accentColor: Int = ForkUiDefaults.Yellow,
    val cornerScale: Float = 1f,
)

/** Reads/writes the six 白い熊 Installer UI preferences; the theme picks them up via DataStore. */
class InstallerUiViewModel(private val application: Application) : ViewModel() {

    val uiState: StateFlow<InstallerUiState> = application.dataStore.data
        .map { prefs ->
            InstallerUiState(
                fontFamily = prefs[PreferencesKeys.UI_FONT_FAMILY] ?: "",
                fontWeight = prefs[PreferencesKeys.UI_FONT_WEIGHT] ?: 0,
                fontScale = prefs[PreferencesKeys.UI_FONT_SCALE] ?: 1f,
                monoTechnical = prefs[PreferencesKeys.UI_MONO_TECHNICAL] ?: false,
                accentColor = prefs[PreferencesKeys.UI_ACCENT_COLOR] ?: ForkUiDefaults.Yellow,
                cornerScale = prefs[PreferencesKeys.UI_CORNER_SCALE] ?: 1f,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InstallerUiState())

    // Per-surface overrides (install dialog / main page).
    val dialogTheme: StateFlow<SurfaceTheme> = surfaceFlow(AppSurface.Dialog)
    val mainTheme: StateFlow<SurfaceTheme> = surfaceFlow(AppSurface.Main)

    private fun surfaceFlow(surface: AppSurface): StateFlow<SurfaceTheme> =
        application.dataStore.data
            .map { SurfaceThemeStore.from(it, surface) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SurfaceTheme())

    fun setSurfaceTheme(surface: AppSurface, theme: SurfaceTheme) =
        edit { it[SurfaceThemeStore.key(surface)] = SurfaceThemeStore.serialize(theme) }

    // App-wide bottom navigation bar theme.
    val bottomBarTheme: StateFlow<BottomBarTheme> = application.dataStore.data
        .map { BottomBarThemeStore.from(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BottomBarTheme())

    fun setBottomBarTheme(theme: BottomBarTheme) =
        edit { it[PreferencesKeys.UI_BOTTOM_BAR_THEME] = BottomBarThemeStore.serialize(theme) }

    // ── Export / import of the app configuration (prefs + imported fonts) ──

    /** SAF tree URI of the export directory ("" = not set). Device-local, never exported. */
    val exportDir: StateFlow<String> = application.dataStore.data
        .map { it[PreferencesKeys.UI_EXPORT_DIR] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Persist the picked directory (keeping the grant across reboots) and rescan it. */
    fun setExportDir(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                application.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            application.dataStore.edit { it[PreferencesKeys.UI_EXPORT_DIR] = uri.toString() }
        }
    }

    // The newest export in the configured directory; rescanned whenever the directory changes
    // or [refreshLastExport] is bumped (page open, after each export).
    private val refreshTick = MutableStateFlow(0)
    val lastExport: StateFlow<LastExport> =
        combine(
            application.dataStore.data.map { it[PreferencesKeys.UI_EXPORT_DIR] ?: "" },
            refreshTick,
        ) { dir, _ -> dir }
            .map { dir -> withContext(Dispatchers.IO) { scanLastExport(dir) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LastExport.NoDir)

    fun refreshLastExport() {
        refreshTick.value += 1
    }

    private fun scanLastExport(dirUri: String): LastExport {
        if (dirUri.isEmpty()) return LastExport.NoDir
        val dir = runCatching { DocumentFile.fromTreeUri(application, Uri.parse(dirUri)) }
            .getOrNull()?.takeIf { it.isDirectory } ?: return LastExport.NoDir
        val newest = runCatching {
            dir.listFiles().filter { file ->
                val name = file.name.orEmpty()
                // The current category ZIP, and pre-ZIP single-JSON exports of the same family.
                file.isFile && name.startsWith(EXPORT_PREFIX) &&
                    (name.endsWith(".zip") || name.endsWith(".json"))
            }.maxByOrNull { it.lastModified() }
        }.getOrNull() ?: return LastExport.None
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
            .format(Date(newest.lastModified()))
        return LastExport.Found(newest.name ?: "", stamp)
    }

    fun exportFileName(): String = UiConfigBackup.exportFileName()

    /** One-tap export into the configured directory; [onResult] carries the file name. */
    fun exportConfigToDir(selection: UiConfigBackup.Selection, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val dirStr = application.dataStore.data.first()[PreferencesKeys.UI_EXPORT_DIR]
                        ?: error("no export directory set")
                    val dir = DocumentFile.fromTreeUri(application, Uri.parse(dirStr))
                        ?.takeIf { it.isDirectory } ?: error("export directory unavailable")
                    val name = exportFileName()
                    val file = dir.createFile("application/zip", name)
                        ?: error("cannot create file in the export directory")
                    application.contentResolver.openOutputStream(file.uri)
                        ?.use { UiConfigBackup.export(application, selection, it) }
                        ?: error("cannot write the file")
                    file.name ?: name
                }
            }
            if (res.isSuccess) refreshLastExport()
            onResult(res)
        }
    }

    /** Save-As fallback export to an explicit [uri]; [onResult] carries the file name. */
    fun exportConfigTo(
        uri: Uri,
        selection: UiConfigBackup.Selection,
        onResult: (Result<String>) -> Unit,
    ) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    application.contentResolver.openOutputStream(uri)
                        ?.use { UiConfigBackup.export(application, selection, it) }
                        ?: error("cannot write the file")
                    DocumentFile.fromSingleUri(application, uri)?.name
                        ?: uri.lastPathSegment ?: "export"
                }
            }
            if (res.isSuccess) refreshLastExport()
            onResult(res)
        }
    }

    /** Restore the selected categories from [uri]; [onResult] carries a per-category summary. */
    fun importConfig(
        uri: Uri,
        selection: UiConfigBackup.Selection,
        onResult: (Result<String>) -> Unit,
    ) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = application.contentResolver.openInputStream(uri)
                        ?.use { it.readBytes() } ?: error("cannot read the file")
                    UiConfigBackup.import(application, bytes, selection).getOrThrow()
                }
            }
            onResult(res)
        }
    }

    // Recently-picked colours (most-recent first, deduped, capped) — shown as one-touch picker hotpicks.
    val recentColors: StateFlow<List<Int>> = application.dataStore.data
        .map { prefs ->
            (prefs[PreferencesKeys.UI_RECENT_COLORS] ?: "")
                .split(",").mapNotNull { it.trim().toIntOrNull() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun recordRecentColor(argb: Int) = edit { prefs ->
        val current = (prefs[PreferencesKeys.UI_RECENT_COLORS] ?: "")
            .split(",").mapNotNull { it.trim().toIntOrNull() }
        val updated = (listOf(argb) + current).distinct().take(12)
        prefs[PreferencesKeys.UI_RECENT_COLORS] = updated.joinToString(",")
    }

    fun setFontFamily(value: String) = edit { it[PreferencesKeys.UI_FONT_FAMILY] = value }
    fun setFontWeight(value: Int) = edit { it[PreferencesKeys.UI_FONT_WEIGHT] = value }
    fun setFontScale(value: Float) = edit { it[PreferencesKeys.UI_FONT_SCALE] = value }
    fun setMonoTechnical(value: Boolean) = edit { it[PreferencesKeys.UI_MONO_TECHNICAL] = value }
    fun setAccentColor(value: Int) = edit { it[PreferencesKeys.UI_ACCENT_COLOR] = value }
    fun setCornerScale(value: Float) = edit { it[PreferencesKeys.UI_CORNER_SCALE] = value }

    private fun edit(block: (MutablePreferences) -> Unit) {
        viewModelScope.launch { application.dataStore.edit(block) }
    }
}
