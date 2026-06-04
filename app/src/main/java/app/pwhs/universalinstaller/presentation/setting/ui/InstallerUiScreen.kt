package app.pwhs.universalinstaller.presentation.setting.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.composable.AccentPalette
import app.pwhs.universalinstaller.presentation.composable.ColorPickerDialog
import app.pwhs.universalinstaller.presentation.composable.ColorSwatch
import app.pwhs.universalinstaller.presentation.composable.FontPickerDialog
import app.pwhs.universalinstaller.presentation.composable.SettingsSection
import app.pwhs.universalinstaller.ui.theme.FontWeightOption
import app.pwhs.universalinstaller.ui.theme.composeFontFamily
import app.pwhs.universalinstaller.ui.theme.fontDisplayName
import app.pwhs.universalinstaller.ui.theme.importFont
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InstallerUiScreen(
    modifier: Modifier = Modifier,
    viewModel: InstallerUiViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showFontPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val invalidFontMessage = stringResource(R.string.font_invalid)
    val pickFontLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = context.importFont(uri)
            if (name != null) {
                viewModel.setFontFamily(name)
            } else {
                scope.launch { snackbarHostState.showSnackbar(invalidFontMessage) }
            }
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.shiroikuma_ui_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Typeface ────────────────────────────────────────────────
            item {
                SettingsSection(title = stringResource(R.string.ui_section_typeface), icon = Icons.Rounded.TextFields) {
                    // Live sample, drawn in the current global typography.
                    Text(
                        text = stringResource(R.string.font_sample_text),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    SubHeader(stringResource(R.string.theme_font))
                    IndentRow(onClick = { showFontPicker = true }) {
                        Text(
                            text = context.fontDisplayName(state.fontFamily),
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = composeFontFamily(context, state.fontFamily) ?: FontFamily.Default,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(R.string.theme_add_font),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    SubHeader(stringResource(R.string.theme_weight))
                    FlowRow(
                        modifier = Modifier.padding(start = 28.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FontWeightOption.entries.forEach { option ->
                            FilterChip(
                                selected = state.fontWeight == option.value,
                                onClick = { viewModel.setFontWeight(option.value) },
                                label = { Text(stringResource(option.labelRes)) },
                            )
                        }
                    }

                    SubHeader(stringResource(R.string.theme_size))
                    SliderRow(
                        value = state.fontScale,
                        valueRange = 0.85f..1.30f,
                        steps = 8,
                        valueLabel = "${(state.fontScale * 100).roundToInt()}%",
                        onChange = { viewModel.setFontScale(it) },
                    )

                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    IndentRow(onClick = { viewModel.setMonoTechnical(!state.monoTechnical) }) {
                        Icon(
                            Icons.Rounded.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp).size(22.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.ui_mono_technical_title), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(R.string.ui_mono_technical_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = state.monoTechnical, onCheckedChange = { viewModel.setMonoTechnical(it) })
                    }
                }
            }

            // ── Color ───────────────────────────────────────────────────
            item {
                SettingsSection(title = stringResource(R.string.ui_section_color), icon = Icons.Rounded.Palette) {
                    SubHeader(stringResource(R.string.ui_accent_color))
                    FlowRow(
                        modifier = Modifier.padding(start = 28.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AccentPalette.forEach { swatch ->
                            ColorSwatch(
                                color = swatch,
                                selected = state.accentColor == swatch.toArgbInt(),
                                onClick = { viewModel.setAccentColor(swatch.toArgbInt()) },
                            )
                        }
                    }
                    IndentRow(onClick = { showColorPicker = true }) {
                        Text(
                            stringResource(R.string.ui_accent_custom),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    IndentRow(onClick = { viewModel.setAccentColor(0) }) {
                        Text(
                            stringResource(R.string.ui_accent_reset),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── Shape ───────────────────────────────────────────────────
            item {
                SettingsSection(title = stringResource(R.string.ui_section_shape), icon = Icons.Rounded.RoundedCorner) {
                    SubHeader(stringResource(R.string.ui_corner_roundness))
                    SliderRow(
                        value = state.cornerScale,
                        valueRange = 0f..2f,
                        steps = 7,
                        valueLabel = "×${"%.2f".format(state.cornerScale)}",
                        onChange = { viewModel.setCornerScale(it) },
                    )
                    // Live preview — uses the global shapes, which re-theme as the slider moves.
                    Card(
                        modifier = Modifier
                            .padding(start = 28.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {}
                }
            }

            // ── Link to the stock Theme screen ──────────────────────────
            item {
                SettingsSection(title = stringResource(R.string.theme_screen_title), icon = Icons.Rounded.DarkMode) {
                    IndentRow(onClick = {
                        context.startActivity(
                            android.content.Intent(context, app.pwhs.universalinstaller.presentation.setting.theme.ThemeActivity::class.java)
                        )
                    }) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.ui_open_theme), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(R.string.ui_open_theme_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFontPicker) {
        FontPickerDialog(
            onDismiss = { showFontPicker = false },
            onAddFont = {
                showFontPicker = false
                pickFontLauncher.launch(arrayOf("*/*"))
            },
            onPick = { fileName ->
                showFontPicker = false
                viewModel.setFontFamily(fileName)
            },
        )
    }

    if (showColorPicker) {
        val initial = if (state.accentColor != 0) Color(state.accentColor) else MaterialTheme.colorScheme.primary
        ColorPickerDialog(
            initial = initial,
            onDismiss = { showColorPicker = false },
            onPick = { picked ->
                showColorPicker = false
                viewModel.setAccentColor(picked.toArgbInt())
            },
        )
    }
}

private fun Color.toArgbInt(): Int = this.toArgb()

@Composable
private fun SubHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun IndentRow(
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(start = 28.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun SliderRow(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(start = 28.dp, end = 16.dp, bottom = 8.dp)) {
        Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = value, onValueChange = onChange, valueRange = valueRange, steps = steps)
    }
}
