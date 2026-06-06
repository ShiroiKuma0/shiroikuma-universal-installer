package app.pwhs.universalinstaller.presentation.setting.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.composable.ColorPickerDialog
import app.pwhs.universalinstaller.presentation.composable.FontPickerDialog
import app.pwhs.universalinstaller.presentation.composable.SettingsSection
import app.pwhs.universalinstaller.ui.theme.AppSurface
import app.pwhs.universalinstaller.ui.theme.BottomBarTheme
import app.pwhs.universalinstaller.ui.theme.ButtonStyle
import app.pwhs.universalinstaller.ui.theme.TextStyleOverride
import app.pwhs.universalinstaller.ui.theme.FontWeightOption
import app.pwhs.universalinstaller.ui.theme.SurfaceTheme
import app.pwhs.universalinstaller.ui.theme.composeFontFamily
import app.pwhs.universalinstaller.ui.theme.fontDisplayName
import kotlin.math.roundToInt

// Indent levels (dp): section header (0) → sub-header (1) → item (2) → per-button control (3).
// Deeply indented so the hierarchy reads clearly: sub-headers sit where items used to, and so on.
private const val L1 = 72
private const val L2 = 108
private const val L3 = 144

/** One overridable colour role on a surface, with friendly label and get/set on a [SurfaceTheme]. */
private enum class ColorSlot(
    @StringRes val labelRes: Int,
    val get: (SurfaceTheme) -> Int?,
    val set: (SurfaceTheme, Int?) -> SurfaceTheme,
    val mainOnly: Boolean = false,
) {
    Accent(R.string.ui_role_accent, { it.accent }, { t, v -> t.copy(accent = v) }),
    TitleText(R.string.ui_role_title, { it.titleText }, { t, v -> t.copy(titleText = v) }),
    SecondaryText(R.string.ui_role_secondary, { it.secondaryText }, { t, v -> t.copy(secondaryText = v) }),
    Card(R.string.ui_role_card, { it.card }, { t, v -> t.copy(card = v) }),
    Background(R.string.ui_role_background, { it.background }, { t, v -> t.copy(background = v) }),
    Danger(R.string.ui_role_danger, { it.danger }, { t, v -> t.copy(danger = v) }),
    Success(R.string.ui_role_success, { it.success }, { t, v -> t.copy(success = v) }),
    Highlight(R.string.ui_role_highlight, { it.highlight }, { t, v -> t.copy(highlight = v) }),
    TopIcon(R.string.ui_role_top_icon, { it.topIconColor }, { t, v -> t.copy(topIconColor = v) }, mainOnly = true),
}

/** A dialog button that can be styled individually: friendly label, storage key (slot), and the dialog
 *  stage it appears in (used to group the selector chips). */
private enum class ButtonSlot(@StringRes val labelRes: Int, val key: String, @StringRes val stageRes: Int) {
    Menu(R.string.ui_btn_menu, "menu", R.string.ui_stage_prepare),
    Install(R.string.ui_btn_install, "install", R.string.ui_stage_prepare),
    Cancel(R.string.ui_btn_cancel, "cancel", R.string.ui_stage_prepare),
    Back(R.string.ui_btn_back, "back", R.string.ui_stage_options),
    Background(R.string.ui_btn_background, "background", R.string.ui_stage_installing),
    Done(R.string.ui_btn_done, "done", R.string.ui_stage_success),
    Open(R.string.ui_btn_open, "open", R.string.ui_stage_success),
    Close(R.string.ui_btn_close, "close", R.string.ui_stage_failed),
}

/** A dialog text category that can be styled individually: friendly label, storage key, and the area
 *  it appears in (used to group the selector chips). */
private enum class TextCat(
    @StringRes val labelRes: Int,
    val key: String,
    @StringRes val groupRes: Int,
    val surface: AppSurface,
) {
    AppLabel(R.string.ui_txt_app_label, "app_label", R.string.ui_stage_prepare, AppSurface.Dialog),
    PackageName(R.string.ui_txt_package_name, "package_name", R.string.ui_stage_prepare, AppSurface.Dialog),
    Version(R.string.ui_txt_version, "version", R.string.ui_stage_prepare, AppSurface.Dialog),
    VersionOld(R.string.ui_txt_version_old, "version_old", R.string.ui_stage_prepare, AppSurface.Dialog),
    FileSize(R.string.ui_txt_file_size, "file_size", R.string.ui_stage_prepare, AppSurface.Dialog),
    Chip(R.string.ui_txt_chip, "chip", R.string.ui_stage_prepare, AppSurface.Dialog),
    StatusTitle(R.string.ui_txt_status_title, "status_title", R.string.ui_group_status, AppSurface.Dialog),
    StatusMessage(R.string.ui_txt_status_message, "status_message", R.string.ui_group_status, AppSurface.Dialog),
    MenuHeading(R.string.ui_txt_menu_heading, "menu_heading", R.string.ui_stage_options, AppSurface.Dialog),
    Tab(R.string.ui_txt_tab, "tab", R.string.ui_stage_options, AppSurface.Dialog),
    SectionTitle(R.string.ui_txt_section_title, "section_title", R.string.ui_stage_options, AppSurface.Dialog),
    SectionDesc(R.string.ui_txt_section_desc, "section_desc", R.string.ui_stage_options, AppSurface.Dialog),
    DetailLabel(R.string.ui_txt_detail_label, "detail_label", R.string.ui_stage_options, AppSurface.Dialog),
    DetailValue(R.string.ui_txt_detail_value, "detail_value", R.string.ui_stage_options, AppSurface.Dialog),
    OptionTitle(R.string.ui_txt_option_title, "option_title", R.string.ui_stage_options, AppSurface.Dialog),
    OptionDesc(R.string.ui_txt_option_desc, "option_desc", R.string.ui_stage_options, AppSurface.Dialog),
    Permission(R.string.ui_txt_permission, "permission", R.string.ui_stage_options, AppSurface.Dialog),

    // Main page (Install Package screen)
    StorageLabel(R.string.ui_txt_storage_label, "storage_label", R.string.ui_group_main_page, AppSurface.Main),
    StorageValue(R.string.ui_txt_storage_value, "storage_value", R.string.ui_group_main_page, AppSurface.Main),
    MainTab(R.string.ui_txt_tab, "tab", R.string.ui_group_main_page, AppSurface.Main),
    MainOptionTitle(R.string.ui_txt_option_title, "option_title", R.string.ui_group_main_page, AppSurface.Main),
    MainOptionDesc(R.string.ui_txt_option_desc, "option_desc", R.string.ui_group_main_page, AppSurface.Main),
}

// Pending edits drive a single shared picker dialog (used by every colour/font row in the section).
private class ColorEdit(val current: Int?, val onSet: (Int?) -> Unit)
private class FontEdit(val current: String?, val onSet: (String?) -> Unit)

/**
 * Settings card for one [SurfaceTheme] (install dialog or main page): colour-role rows, an optional
 * border, optional per-button styling, and font controls — all with an "inherit global" option.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SurfaceThemeSection(
    title: String,
    icon: ImageVector,
    theme: SurfaceTheme,
    onChange: (SurfaceTheme) -> Unit,
    recents: List<Int>,
    onRecordRecent: (Int) -> Unit,
    onRequestFontImport: ((String) -> Unit) -> Unit,
    surface: AppSurface,
    showBorder: Boolean = false,
    showProgress: Boolean = false,
    showSuccess: Boolean = false,
    showButtons: Boolean = false,
    showTexts: Boolean = false,
) {
    var colorEdit by remember { mutableStateOf<ColorEdit?>(null) }
    var fontEdit by remember { mutableStateOf<FontEdit?>(null) }
    var selectedButton by remember { mutableStateOf(ButtonSlot.Menu) }
    var selectedText by remember(surface) { mutableStateOf(TextCat.entries.first { it.surface == surface }) }

    SettingsSection(title = title, icon = icon) {
        SubHeader(stringResource(R.string.ui_section_color))
        // When per-text categories are available (the dialog), title/secondary text colours are set there,
        // per category — so the broad Title/Secondary roles are hidden here to avoid redundancy.
        ColorSlot.entries
            .filterNot { showTexts && (it == ColorSlot.TitleText || it == ColorSlot.SecondaryText) }
            .filter { !it.mainOnly || surface == AppSurface.Main }
            .forEach { slot ->
                ColorRow(L2, stringResource(slot.labelRes), slot.get(theme)) {
                    colorEdit = ColorEdit(slot.get(theme)) { onChange(slot.set(theme, it)) }
                }
            }

        if (showBorder) {
            SubHeader(stringResource(R.string.ui_role_border))
            ColorRow(L2, stringResource(R.string.ui_border_color), theme.borderColor) {
                colorEdit = ColorEdit(theme.borderColor) { onChange(theme.copy(borderColor = it)) }
            }
            WidthSlider(L2, theme.borderWidth) { onChange(theme.copy(borderWidth = it)) }
        }

        if (showProgress) {
            SubHeader(stringResource(R.string.ui_section_progress))
            Text(
                text = stringResource(R.string.ui_progress_hint),
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = L2.dp, end = 16.dp, top = 2.dp, bottom = 4.dp),
            )
            ColorRow(L2, stringResource(R.string.ui_progress_color), theme.progressColor) {
                colorEdit = ColorEdit(theme.progressColor) { onChange(theme.copy(progressColor = it)) }
            }
            WidthSlider(
                L2, theme.progressThickness,
                labelRes = R.string.ui_progress_thickness,
                valueRange = 1f..16f, steps = 14, nullValue = 4f,
            ) { onChange(theme.copy(progressThickness = it)) }
        }

        if (showSuccess) {
            SubHeader(stringResource(R.string.ui_section_success))
            Text(
                text = stringResource(R.string.ui_success_hint),
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = L2.dp, end = 16.dp, top = 2.dp, bottom = 4.dp),
            )
            ColorRow(L2, stringResource(R.string.ui_success_circle), theme.successCircle) {
                colorEdit = ColorEdit(theme.successCircle) { onChange(theme.copy(successCircle = it)) }
            }
            WidthSlider(
                L2, theme.successCircleThickness,
                labelRes = R.string.ui_success_circle_thickness,
                valueRange = 0.5f..8f, steps = 14, nullValue = 3f,
            ) { onChange(theme.copy(successCircleThickness = it)) }
            ColorRow(L2, stringResource(R.string.ui_success_tick), theme.successTick) {
                colorEdit = ColorEdit(theme.successTick) { onChange(theme.copy(successTick = it)) }
            }
            WidthSlider(
                L2, theme.successTickThickness,
                labelRes = R.string.ui_success_tick_thickness,
                valueRange = 1f..10f, steps = 17, nullValue = 4f,
            ) { onChange(theme.copy(successTickThickness = it)) }
        }

        if (showButtons) {
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
            SubHeader(stringResource(R.string.ui_section_buttons))
            Text(
                text = stringResource(R.string.ui_buttons_hint),
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = L2.dp, end = 16.dp, top = 2.dp, bottom = 4.dp),
            )
            // Chips grouped by the dialog stage each button appears in.
            ButtonSlot.entries.groupBy { it.stageRes }.forEach { (stageRes, slots) ->
                Text(
                    text = stringResource(stageRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = L2.dp, top = 8.dp, bottom = 2.dp),
                )
                FlowRow(
                    modifier = Modifier.padding(start = L2.dp, end = 16.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    slots.forEach { b ->
                        FilterChip(
                            selected = selectedButton == b,
                            onClick = { selectedButton = b },
                            label = { Text(stringResource(b.labelRes)) },
                        )
                    }
                }
            }
            // Make the button being edited unmistakable.
            Text(
                text = stringResource(R.string.ui_btn_editing, stringResource(selectedButton.labelRes)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = L2.dp, top = 12.dp, bottom = 2.dp),
            )
            val key = selectedButton.key
            val bs = theme.buttons[key] ?: ButtonStyle()
            fun upd(next: ButtonStyle) {
                val map = theme.buttons.toMutableMap()
                if (next == ButtonStyle()) map.remove(key) else map[key] = next
                onChange(theme.copy(buttons = map))
            }
            ColorRow(L3, stringResource(R.string.ui_btn_bg), bs.bg) {
                colorEdit = ColorEdit(bs.bg) { upd(bs.copy(bg = it)) }
            }
            ColorRow(L3, stringResource(R.string.ui_btn_content), bs.content) {
                colorEdit = ColorEdit(bs.content) { upd(bs.copy(content = it)) }
            }
            ColorRow(L3, stringResource(R.string.ui_border_color), bs.borderColor) {
                colorEdit = ColorEdit(bs.borderColor) { upd(bs.copy(borderColor = it)) }
            }
            WidthSlider(L3, bs.borderWidth) { upd(bs.copy(borderWidth = it)) }
            FieldLabel(L3, stringResource(R.string.theme_font))
            FontRow(L3, bs.fontFamily) { fontEdit = FontEdit(bs.fontFamily) { upd(bs.copy(fontFamily = it)) } }
            FieldLabel(L3, stringResource(R.string.theme_weight))
            WeightChips(L3, bs.fontWeight) { upd(bs.copy(fontWeight = it)) }
            FieldLabel(L3, stringResource(R.string.theme_size))
            ScaleSlider(L3, bs.fontScale) { upd(bs.copy(fontScale = it)) }
        }

        if (showTexts) {
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
            SubHeader(stringResource(R.string.ui_section_texts))
            Text(
                text = stringResource(R.string.ui_texts_hint),
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = L2.dp, end = 16.dp, top = 2.dp, bottom = 4.dp),
            )
            // Chips grouped by the area each text appears in (only this surface's categories).
            TextCat.entries.filter { it.surface == surface }.groupBy { it.groupRes }.forEach { (groupRes, cats) ->
                Text(
                    text = stringResource(groupRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = L2.dp, top = 8.dp, bottom = 2.dp),
                )
                FlowRow(
                    modifier = Modifier.padding(start = L2.dp, end = 16.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    cats.forEach { c ->
                        FilterChip(
                            selected = selectedText == c,
                            onClick = { selectedText = c },
                            label = { Text(stringResource(c.labelRes)) },
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.ui_btn_editing, stringResource(selectedText.labelRes)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = L2.dp, top = 12.dp, bottom = 2.dp),
            )
            val tkey = selectedText.key
            val ts = theme.texts[tkey] ?: TextStyleOverride()
            fun updT(next: TextStyleOverride) {
                val map = theme.texts.toMutableMap()
                if (next == TextStyleOverride()) map.remove(tkey) else map[tkey] = next
                onChange(theme.copy(texts = map))
            }
            ColorRow(L3, stringResource(R.string.ui_txt_color), ts.color) {
                colorEdit = ColorEdit(ts.color) { updT(ts.copy(color = it)) }
            }
            FieldLabel(L3, stringResource(R.string.theme_font))
            FontRow(L3, ts.fontFamily) { fontEdit = FontEdit(ts.fontFamily) { updT(ts.copy(fontFamily = it)) } }
            FieldLabel(L3, stringResource(R.string.theme_weight))
            WeightChips(L3, ts.fontWeight) { updT(ts.copy(fontWeight = it)) }
            FieldLabel(L3, stringResource(R.string.theme_size))
            ScaleSlider(L3, ts.fontScale) { updT(ts.copy(fontScale = it)) }
        }

        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

        SubHeader(stringResource(R.string.theme_font))
        FontRow(L2, theme.fontFamily) { fontEdit = FontEdit(theme.fontFamily) { onChange(theme.copy(fontFamily = it)) } }

        SubHeader(stringResource(R.string.theme_weight))
        WeightChips(L2, theme.fontWeight) { onChange(theme.copy(fontWeight = it)) }

        SubHeader(stringResource(R.string.theme_size))
        ScaleSlider(L2, theme.fontScale) { onChange(theme.copy(fontScale = it)) }

        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChange(SurfaceTheme()) }
                .padding(start = L1.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Replay, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp).size(20.dp))
            Text(stringResource(R.string.ui_reset_section), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }
    }

    // One shared picker for every colour row (recents/palette hotpicks + inherit).
    colorEdit?.let { edit ->
        val initial = edit.current?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
        ColorPickerDialog(
            initial = initial,
            recents = recents,
            onInherit = { edit.onSet(null); colorEdit = null },
            onPick = { c -> edit.onSet(c.toArgb()); onRecordRecent(c.toArgb()); colorEdit = null },
            onDismiss = { colorEdit = null },
        )
    }
    fontEdit?.let { edit ->
        FontPickerDialog(
            onDismiss = { fontEdit = null },
            onAddFont = {
                val apply = edit.onSet
                fontEdit = null
                onRequestFontImport { name -> apply(name) }
            },
            onPick = { fileName -> edit.onSet(fileName); fontEdit = null },
            onInherit = { edit.onSet(null); fontEdit = null },
        )
    }
}

/**
 * Settings card for the app-wide bottom navigation bar: six colour rows (container, selected/unselected
 * icon & text, indicator) each with an "inherit default" option, plus a reset.
 */
@Composable
fun BottomBarThemeSection(
    title: String,
    icon: ImageVector,
    theme: BottomBarTheme,
    onChange: (BottomBarTheme) -> Unit,
    recents: List<Int>,
    onRecordRecent: (Int) -> Unit,
) {
    var colorEdit by remember { mutableStateOf<ColorEdit?>(null) }
    SettingsSection(title = title, icon = icon) {
        ColorRow(L2, stringResource(R.string.ui_bottom_bar_container), theme.container) {
            colorEdit = ColorEdit(theme.container) { onChange(theme.copy(container = it)) }
        }
        ColorRow(L2, stringResource(R.string.ui_bottom_bar_selected_icon), theme.selectedIcon) {
            colorEdit = ColorEdit(theme.selectedIcon) { onChange(theme.copy(selectedIcon = it)) }
        }
        ColorRow(L2, stringResource(R.string.ui_bottom_bar_selected_text), theme.selectedText) {
            colorEdit = ColorEdit(theme.selectedText) { onChange(theme.copy(selectedText = it)) }
        }
        ColorRow(L2, stringResource(R.string.ui_bottom_bar_indicator), theme.indicator) {
            colorEdit = ColorEdit(theme.indicator) { onChange(theme.copy(indicator = it)) }
        }
        ColorRow(L2, stringResource(R.string.ui_bottom_bar_unselected_icon), theme.unselectedIcon) {
            colorEdit = ColorEdit(theme.unselectedIcon) { onChange(theme.copy(unselectedIcon = it)) }
        }
        ColorRow(L2, stringResource(R.string.ui_bottom_bar_unselected_text), theme.unselectedText) {
            colorEdit = ColorEdit(theme.unselectedText) { onChange(theme.copy(unselectedText = it)) }
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChange(BottomBarTheme()) }
                .padding(start = L1.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Replay, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp).size(20.dp))
            Text(stringResource(R.string.ui_reset_section), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
    colorEdit?.let { edit ->
        val initial = edit.current?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
        ColorPickerDialog(
            initial = initial,
            recents = recents,
            onInherit = { edit.onSet(null); colorEdit = null },
            onPick = { c -> edit.onSet(c.toArgb()); onRecordRecent(c.toArgb()); colorEdit = null },
            onDismiss = { colorEdit = null },
        )
    }
}

@Composable
private fun ColorRow(indent: Int, label: String, value: Int?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = indent.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        InheritableSwatch(value)
    }
}

@Composable
private fun FontRow(indent: Int, fontFamily: String?, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = indent.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val label = fontFamily?.let { context.fontDisplayName(it) } ?: stringResource(R.string.ui_inherit)
        val family = fontFamily?.let { composeFontFamily(context, it) } ?: FontFamily.Default
        Text(label, style = MaterialTheme.typography.bodyLarge, fontFamily = family)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun WeightChips(indent: Int, weight: Int?, onChange: (Int?) -> Unit) {
    FlowRow(
        modifier = Modifier.padding(start = indent.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = weight == null, onClick = { onChange(null) }, label = { Text(stringResource(R.string.ui_inherit)) })
        FontWeightOption.entries.forEach { option ->
            FilterChip(
                selected = weight == option.value,
                onClick = { onChange(option.value) },
                label = { Text(stringResource(option.labelRes)) },
            )
        }
    }
}

@Composable
private fun WidthSlider(
    indent: Int,
    value: Float?,
    @StringRes labelRes: Int = R.string.ui_border_width,
    valueRange: ClosedFloatingPointRange<Float> = 0f..6f,
    steps: Int = 11,
    nullValue: Float = 0f,
    onChange: (Float?) -> Unit,
) {
    Column(Modifier.padding(start = indent.dp, end = 16.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value?.let { "${stringResource(labelRes)} — ${"%.1f".format(it)} dp" }
                    ?: stringResource(R.string.ui_inherit),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                TextButton(onClick = { onChange(null) }) { Text(stringResource(R.string.ui_inherit)) }
            }
        }
        Slider(value = value ?: nullValue, onValueChange = { onChange(it) }, valueRange = valueRange, steps = steps)
    }
}

@Composable
private fun ScaleSlider(indent: Int, value: Float?, onChange: (Float?) -> Unit) {
    Column(Modifier.padding(start = indent.dp, end = 16.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value?.let { "${(it * 100).roundToInt()}%" } ?: stringResource(R.string.ui_inherit),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                TextButton(onClick = { onChange(null) }) { Text(stringResource(R.string.ui_inherit)) }
            }
        }
        Slider(value = value ?: 1f, onValueChange = { onChange(it) }, valueRange = 0.85f..1.30f, steps = 8)
    }
}

@Composable
private fun InheritableSwatch(value: Int?) {
    // Non-interactive — the enclosing row owns the tap, so tapping the swatch opens the picker too.
    if (value != null) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(value)))
    } else {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A caption for an individual control, indented to [indent] (used inside the per-button block). */
@Composable
private fun FieldLabel(indent: Int, text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = indent.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun SubHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = L1.dp, top = 12.dp, bottom = 2.dp),
    )
}
