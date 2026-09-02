package app.pwhs.universalinstaller.wearos.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import app.pwhs.universalinstaller.wearos.R
import app.pwhs.universalinstaller.wearos.data.WearApkInfo
import app.pwhs.universalinstaller.wearos.presentation.theme.UniversalInstallerTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ApkDetailScreen(
    apkId: String,
    onInstallSuccess: () -> Unit,
    onDelete: () -> Unit,
    viewModel: DetailViewModel = koinViewModel(parameters = { parametersOf(apkId) }),
) {
    val apkInfo by viewModel.apkInfo.collectAsState()
    val installState by viewModel.installState.collectAsState()

    // Navigate back on success
    if (installState is InstallState.Success) {
        onInstallSuccess()
        return
    }

    ApkDetailContent(
        apkInfo = apkInfo,
        installState = installState,
        onInstall = viewModel::install,
        onDelete = {
            viewModel.delete()
            onDelete()
        },
    )
}

@Composable
private fun ApkDetailContent(
    apkInfo: WearApkInfo?,
    installState: InstallState,
    onInstall: () -> Unit,
    onDelete: () -> Unit,
) {
    UniversalInstallerTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            val transformationSpec = rememberTransformationSpec()

            ScreenScaffold(
                scrollState = listState,
                edgeButton = {
                    // Delete button at the bottom edge
                    if (installState == InstallState.Idle && apkInfo != null) {
                        EdgeButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                },
            ) { contentPadding ->
                TransformingLazyColumn(
                    contentPadding = contentPadding,
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Title
                    item {
                        ListHeader(
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text(apkInfo?.appName ?: stringResource(R.string.loading))
                        }
                    }

                    when (installState) {
                        InstallState.Installing -> {
                            item {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .transformedHeight(this, transformationSpec),
                                )
                            }
                            item {
                                Text(
                                    text = stringResource(R.string.installing),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .transformedHeight(this, transformationSpec),
                                )
                            }
                        }

                        is InstallState.Failed -> {
                            item {
                                Text(
                                    text = stringResource(R.string.install_failed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                        .transformedHeight(this, transformationSpec),
                                )
                            }
                            item {
                                Text(
                                    text = installState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                        .transformedHeight(this, transformationSpec),
                                )
                            }
                            item {
                                Button(
                                    onClick = onInstall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .transformedHeight(this, transformationSpec),
                                    transformation = SurfaceTransformation(transformationSpec),
                                ) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }

                        else -> {
                            // Idle — show APK info + install button
                            if (apkInfo != null) {
                                item {
                                    Text(
                                        text = apkInfo.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp)
                                            .transformedHeight(this, transformationSpec),
                                    )
                                }
                                item {
                                    Text(
                                        text = "v${apkInfo.versionName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .transformedHeight(this, transformationSpec),
                                    )
                                }
                                item {
                                    Button(
                                        onClick = onInstall,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .transformedHeight(this, transformationSpec),
                                        transformation = SurfaceTransformation(transformationSpec),
                                    ) {
                                        Text(stringResource(R.string.install))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun ApkDetailPreview() {
    ApkDetailContent(
        apkInfo = WearApkInfo(
            id = "1", fileName = "app.apk", appName = "Sample App",
            packageName = "com.example.sample", versionName = "1.2.3",
            versionCode = 123, sizeBytes = 10_000_000L, cachedFilePath = "",
        ),
        installState = InstallState.Idle,
        onInstall = {},
        onDelete = {},
    )
}
