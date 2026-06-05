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
import app.pwhs.universalinstaller.ui.theme.ButtonStyle
import app.pwhs.universalinstaller.ui.theme.TextStyleOverride
import app.pwhs.universalinstaller.ui.theme.FontWeightOption
import app.pwhs.universalinstaller.ui.theme.SurfaceTheme
import app.pwhs.universalinstaller.ui.theme.composeFontFamily
import app.pwhs.universalinstaller.ui.theme.fontDisplayName
import kotlin.math.roundToInt

// Indent levels (dp): section header (0) → sub-header (1) → item (2) → per-button control (3).
private const val L1 = 36
private const val L2 = 72
private const val L3 = 108

/** One overridable colour role on a surface, with friendly label and get/set on a [SurfaceTheme]. */
private enum class ColorSlot(
    @StringRes val labelRes: Int,
    val get: (SurfaceTheme) -> Int?,
    val set: (SurfaceTheme, Int?) -> SurfaceTheme,
) {
    Accent(R.string.ui_role_accent, { it.accent }, { t, v -> t.copy(accent = v) }),
    TitleText(R.string.ui_role_title, { it.titleText }, { t, v -> t.copy(titleText = v) }),
    SecondaryText(R.string.ui_role_secondary, { it.secondaryText }, { t, v -> t.copy(secondaryText = v) }),
    Card(R.string.ui_role_card, { it.card }, { t, v -> t.copy(card = v) }),
    Background(R.string.ui_role_background, { it.background }, { t, v -> t.copy(background = v) }),
    Danger(R.string.ui_role_danger, { it.danger }, { t, v -> t.copy(danger = v) }),
    Success(R.string.ui_role_success, { it.success }, { t, v -> t.copy(success = v) }),
    Highlight(R.string.ui_role_highlight, { it.highlight }, { t, v -> t.copy(highlight = v) }),
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
private enum class TextCat(@StringRes val labelRes: Int, val key: String, @StringRes val groupRes: Int) {
    AppLabel(R.string.ui_txt_app_label, "app_label", R.string.ui_stage_prepare),
    PackageName(R.string.ui_txt_package_name, "package_name", R.string.ui_stage_prepare),
    Version(R.string.ui_txt_version, "version", R.string.ui_stage_prepare),
    FileSize(R.string.ui_txt_file_size, "file_size", R.string.ui_stage_prepare),
    Chip(R.string.ui_txt_chip, "chip", R.string.ui_stage_prepare),
    StatusTitle(R.string.ui_txt_status_title, "status_title", R.string.ui_group_status),
    StatusMessage(R.string.ui_txt_status_message, "status_message", R.string.ui_group_status),
    MenuHeading(R.string.ui_txt_menu_heading, "menu_heading", R.string.ui_stage_options),
    Tab(R.string.ui_txt_tab, "tab", R.string.ui_stage_options),
    SectionTitle(R.string.ui_txt_section_title, "section_title", R.string.ui_stage_options),
    SectionDesc(R.string.ui_txt_section_desc, "section_desc", R.string.ui_stage_options),
    DetailLabel(R.string.ui_txt_detail_label, "detail_label", R.string.ui_stage_options),
    DetailValue(R.string.ui_txt_detail_value, "detail_value", R.string.ui_stage_options),
    OptionTitle(R.string.ui_txt_option_title, "option_title", R.string.ui_stage_options),
    OptionDesc(R.string.ui_txt_option_desc, "option_desc", R.string.ui_stage_options),
    Permission(R.string.ui_txt_permission, "permission", R.string.ui_stage_options),
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
    showBorder: Boolean = false,
    showButtons: Boolean = false,
    showTexts: Boolean = false,
) {
    var colorEdit by remember { mutableStateOf<ColorEdit?>(null) }
    var fontEdit by remember { mutableStateOf<FontEdit?>(null) }
    var selectedButton by remember { mutableStateOf(ButtonSlot.Menu) }
    var selectedText by remember { mutableStateOf(TextCat.AppLabel) }

    SettingsSection(title = title, icon = icon) {
        SubHeader(stringResource(R.string.ui_section_color))
        // When per-text categories are available (the dialog), title/secondary text colours are set there,
        // per category — so the broad Title/Secondary roles are hidden here to avoid redundancy.
        ColorSlot.entries
            .filterNot { showTexts && (it == ColorSlot.TitleText || it == ColorSlot.SecondaryText) }
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
            // Chips grouped by the area each text appears in.
            TextCat.entries.groupBy { it.groupRes }.forEach { (groupRes, cats) ->
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
private fun WidthSlider(indent: Int, value: Float?, onChange: (Float?) -> Unit) {
    Column(Modifier.padding(start = indent.dp, end = 16.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value?.let { "${stringResource(R.string.ui_border_width)} — ${"%.1f".format(it)} dp" }
                    ?: stringResource(R.string.ui_inherit),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                TextButton(onClick = { onChange(null) }) { Text(stringResource(R.string.ui_inherit)) }
            }
        }
        Slider(value = value ?: 0f, onValueChange = { onChange(it) }, valueRange = 0f..6f, steps = 11)
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
