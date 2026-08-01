package app.pwhs.universalinstaller.ui.theme

import app.pwhs.core.domain.AppThemePreset
import app.pwhs.core.domain.ThemeMode

/**
 * Compiled-in 白い熊 look: black background, yellow text and items, no Material You.
 *
 * Upstream ships every UI preference as "unset → inherit Material", which meant a fresh install of
 * the fork came up in upstream's orange (and, on Android 12+, in wallpaper-derived Material You
 * colours), and the black/yellow theme only existed as DataStore values 白い熊 had configured by
 * hand — lost on every clean install. These values are the *defaults* instead: every UI preference
 * resolves to them when no key is stored, so a fresh install already looks right, the Installer UI
 * page shows real values rather than blanks, and "reset" in that page returns here rather than to
 * upstream's Material defaults. A stored preference still wins over everything below.
 */
object ForkUiDefaults {
    /** Material Yellow 500 — the 白い熊 accent, used for text, icons, borders and the accent role. */
    val Yellow: Int = 0xFFFFEB3B.toInt()

    /** The same yellow at 70% alpha — secondary text, so hierarchy survives a monochrome palette. */
    val YellowDim: Int = 0xB3FFEB3B.toInt()

    /** Pure black — backgrounds, cards and the dialog surface (AMOLED-friendly). */
    val Black: Int = 0xFF000000.toInt()

    /** Yellow hairline around the install-dialog card, in dp. */
    val BorderWidth: Float = 2f

    // ── App-level theme defaults ───────────────────────────────────────────────
    /** Dark, not System: the black/yellow palette is a dark theme and must not follow the system. */
    val Mode: ThemeMode = ThemeMode.Dark

    /** Material You off — wallpaper colours would override the fork's yellow accent. */
    const val DynamicColor: Boolean = false

    /** AMOLED on, so the stock surfaces are true black like the surface overrides. */
    const val Amoled: Boolean = true

    /** The yellow colour preset (fork-added; upstream's presets are unchanged). */
    val Preset: AppThemePreset = AppThemePreset.Yellow
}
