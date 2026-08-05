package app.pwhs.universalinstaller.presentation.setting.installui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.domain.model.ExternalOpenMode
import app.pwhs.universalinstaller.domain.model.InstallUiStyle
import app.pwhs.universalinstaller.presentation.setting.SettingViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Everything about how the installer *looks* and how it asks, split out of Settings > Installation.
 *
 * That section had grown to ten controls in four idioms — segmented buttons, switches, radios and
 * thumbnails — in one scrolling run. Worse, two of them decided the same thing ("Don't ask, just
 * install" against the auto-confirm switch) and one applied only to a mode the user might not have
 * picked, with nothing on screen saying so. Splitting the appearance questions out leaves the
 * Installation section to the engine and lets the real dependencies show as nesting.
 */
@Composable
fun InstallUiScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val externalOpenMode by viewModel.externalOpenMode.collectAsState()
    val installUiStyle by viewModel.installUiStyle.collectAsState()
    val context = LocalContext.current

    InstallUiContent(
        modifier = modifier,
        mode = externalOpenMode,
        style = installUiStyle,
        autoConfirm = uiState.autoConfirmExternalInstall,
        showDownloadTab = uiState.showDownloadTab,
        onModeChange = viewModel::setExternalOpenMode,
        onStyleChange = viewModel::setInstallUiStyle,
        onAutoConfirmChange = viewModel::setAutoConfirmExternalInstall,
        onShowDownloadTabChange = viewModel::setShowDownloadTab,
        onBack = { (context as? android.app.Activity)?.finish() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstallUiContent(
    modifier: Modifier = Modifier,
    mode: ExternalOpenMode = ExternalOpenMode.Dialog,
    style: InstallUiStyle = InstallUiStyle.Dialog,
    autoConfirm: Boolean = false,
    showDownloadTab: Boolean = true,
    onModeChange: (ExternalOpenMode) -> Unit = {},
    onStyleChange: (InstallUiStyle) -> Unit = {},
    onAutoConfirmChange: (Boolean) -> Unit = {},
    onShowDownloadTabChange: (Boolean) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // The card only exists in this mode, so everything about where it sits belongs to it.
    val cardIsShown = mode == ExternalOpenMode.Dialog

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.install_ui_screen_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back_cd),
                        )
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
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // The answer to "what will this look like", shown rather than named.
            item { InstallUiPreview(mode = mode, style = style) }

            item { SectionLabel(stringResource(R.string.setting_external_open_title)) }

            items(EXTERNAL_OPEN_OPTIONS.size) { index ->
                val option = EXTERNAL_OPEN_OPTIONS[index]
                ModeRow(
                    title = stringResource(option.titleRes),
                    description = stringResource(option.descriptionRes),
                    selected = option.mode == mode,
                    onSelect = { if (option.mode != mode) onModeChange(option.mode) },
                )
            }

            // Nested, and gone entirely for the two modes that draw no card — which is what makes
            // the old contradiction with auto-confirm disappear instead of needing explaining.
            item {
                AnimatedVisibility(visible = cardIsShown) {
                    Column {
                        SectionLabel(
                            text = stringResource(R.string.install_ui_card_group),
                            indented = true,
                        )
                        CardPositionPicker(
                            current = style,
                            onChange = onStyleChange,
                        )
                        SwitchRow(
                            title = stringResource(R.string.setting_auto_confirm_title),
                            description = stringResource(R.string.install_ui_auto_confirm_sub),
                            checked = autoConfirm,
                            onCheckedChange = onAutoConfirmChange,
                            indented = true,
                        )
                    }
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    thickness = 0.5.dp,
                )
            }

            item { SectionLabel(stringResource(R.string.install_ui_main_screen_group)) }

            item {
                SwitchRow(
                    title = stringResource(R.string.setting_show_download_tab_title),
                    description = stringResource(R.string.setting_show_download_tab_subtitle),
                    checked = showDownloadTab,
                    onCheckedChange = onShowDownloadTabChange,
                )
            }
        }
    }
}

private data class ExternalOpenOption(
    val mode: ExternalOpenMode,
    val titleRes: Int,
    val descriptionRes: Int,
)

private val EXTERNAL_OPEN_OPTIONS = listOf(
    ExternalOpenOption(
        ExternalOpenMode.Dialog,
        R.string.setting_external_open_dialog,
        R.string.setting_external_open_dialog_sub,
    ),
    ExternalOpenOption(
        ExternalOpenMode.Notification,
        R.string.setting_external_open_notification,
        R.string.setting_external_open_notification_sub,
    ),
    ExternalOpenOption(
        ExternalOpenMode.AutoNotification,
        R.string.setting_external_open_auto_notification,
        R.string.setting_external_open_auto_notification_sub,
    ),
)

@Composable
private fun SectionLabel(text: String, indented: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = if (indented) 40.dp else 24.dp,
            end = 24.dp,
            top = 16.dp,
            bottom = 8.dp,
        ),
    )
}

@Composable
private fun ModeRow(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    indented: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(start = if (indented) 40.dp else 24.dp, end = 24.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * A phone-shaped frame with the install card drawn where it will actually land, or a notification
 * shade line when the chosen mode never draws a card.
 *
 * This is the part that answers "what does this look like" without the user having to pick an
 * option and go install something to find out.
 */
@Composable
private fun InstallUiPreview(mode: ExternalOpenMode, style: InstallUiStyle) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(width = 150.dp, height = 264.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                when (mode) {
                    // No window at all: what the user sees is a notification, so draw that.
                    ExternalOpenMode.Notification,
                    ExternalOpenMode.AutoNotification -> MiniNotification(
                        modifier = Modifier.align(Alignment.TopCenter),
                        accent = accent,
                    )

                    ExternalOpenMode.Dialog -> MiniCard(
                        modifier = Modifier.align(
                            if (style == InstallUiStyle.Sheet) Alignment.BottomCenter else Alignment.Center,
                        ),
                        widthFraction = if (style == InstallUiStyle.Sheet) 1f else 0.86f,
                        accent = accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniCard(modifier: Modifier, widthFraction: Float, accent: Color) {
    Column(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        MiniLine(0.7f, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        MiniLine(1f, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
        Spacer(Modifier.height(2.dp))
        MiniLine(0.45f, accent, height = 7.dp)
    }
}

@Composable
private fun MiniNotification(modifier: Modifier, accent: Color) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accent),
        )
        Spacer(Modifier.width(6.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MiniLine(0.8f, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            MiniLine(1f, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
        }
    }
}

/** One bar standing in for a line of text or a button. */
@Composable
private fun MiniLine(widthFraction: Float, color: Color, height: Dp = 4.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}

/**
 * Centered or bottom, as two thumbnails.
 *
 * Kept from the Settings screen this moved out of: naming the two Material components asked the
 * user to picture them, where a drawing just shows it.
 */
@Composable
private fun CardPositionPicker(
    current: InstallUiStyle,
    onChange: (InstallUiStyle) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 40.dp, end = 24.dp, top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(
            InstallUiStyle.Dialog to R.string.setting_install_ui_style_dialog,
            InstallUiStyle.Sheet to R.string.setting_install_ui_style_sheet,
        ).forEach { (style, labelRes) ->
            PositionThumbnail(
                style = style,
                label = stringResource(labelRes),
                selected = style == current,
                onClick = { if (style != current) onChange(style) },
            )
        }
    }
}

@Composable
private fun PositionThumbnail(
    style: InstallUiStyle,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .size(width = 84.dp, height = 112.dp)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(9.dp),
                contentAlignment = if (style == InstallUiStyle.Sheet) {
                    Alignment.BottomCenter
                } else {
                    Alignment.Center
                },
            ) {
                MiniCard(
                    modifier = Modifier,
                    widthFraction = if (style == InstallUiStyle.Sheet) 1f else 0.82f,
                    accent = accent,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
