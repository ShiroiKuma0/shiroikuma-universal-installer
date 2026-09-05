package app.pwhs.universalinstaller.presentation.setting.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R
import app.pwhs.universalinstaller.presentation.setting.PreferencesKeys
import app.pwhs.universalinstaller.util.CustomShellExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CustomAuthorizerCard(
    command: String,
    onCommandChange: (String) -> Unit,
    onTestCommand: suspend (String) -> Result<String>,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Result<String>?>(null) }

    // Maintain local state so typing is immediate and smooth without async DataStore jitter/cursor jumps
    var localCommand by rememberSaveable { mutableStateOf(command) }

    // Sync from external state (e.g. initial load or reset from outside)
    LaunchedEffect(command) {
        if (command != localCommand) {
            localCommand = command
        }
    }

    // Debounce updates back to DataStore
    LaunchedEffect(localCommand) {
        if (localCommand != command) {
            delay(300L)
            onCommandChange(localCommand)
        }
    }

    // Flush any pending changes when leaving composition
    val currentLocalCommand by rememberUpdatedState(localCommand)
    val currentExternalCommand by rememberUpdatedState(command)
    DisposableEffect(Unit) {
        onDispose {
            if (currentLocalCommand != currentExternalCommand) {
                onCommandChange(currentLocalCommand)
            }
        }
    }

    val validation = remember(localCommand) { CustomShellExecutor.validateCommand(localCommand) }
    val isError = validation is CustomShellExecutor.ValidationResult.Error
    val errorMessage = when (validation) {
        is CustomShellExecutor.ValidationResult.Error -> when (validation.reason) {
            CustomShellExecutor.ValidationErrorReason.BLANK ->
                stringResource(R.string.setting_custom_authorizer_err_blank)
            CustomShellExecutor.ValidationErrorReason.DANGEROUS_COMMAND ->
                stringResource(R.string.setting_custom_authorizer_err_dangerous, validation.detail)
            CustomShellExecutor.ValidationErrorReason.NOT_AUTHORIZER_BINARY ->
                stringResource(R.string.setting_custom_authorizer_err_not_authorizer, validation.detail)
            CustomShellExecutor.ValidationErrorReason.MISSING_PLACEHOLDER ->
                stringResource(R.string.setting_custom_authorizer_err_missing_placeholder, validation.detail)
        }
        else -> null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = localCommand,
                onValueChange = {
                    localCommand = it
                    testResult = null
                },
                modifier = Modifier.fillMaxWidth(),
                isError = isError,
                label = { Text(stringResource(R.string.setting_custom_authorizer_cmd_label)) },
                placeholder = { Text(stringResource(R.string.setting_custom_authorizer_cmd_hint)) },
                leadingIcon = {
                    Icon(Icons.Rounded.Terminal, contentDescription = null)
                },
                trailingIcon = {
                    if (localCommand != PreferencesKeys.DEFAULT_CUSTOM_AUTHORIZER_COMMAND) {
                        IconButton(
                            onClick = {
                                localCommand = PreferencesKeys.DEFAULT_CUSTOM_AUTHORIZER_COMMAND
                                onCommandChange(PreferencesKeys.DEFAULT_CUSTOM_AUTHORIZER_COMMAND)
                                testResult = null
                            },
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.RotateLeft,
                                contentDescription = stringResource(R.string.setting_custom_authorizer_reset_default),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                supportingText = {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.setting_custom_authorizer_cmd_desc),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                singleLine = false,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            if (localCommand != command) {
                                onCommandChange(localCommand)
                            }
                            isTesting = true
                            testResult = onTestCommand(localCommand)
                            isTesting = false
                        }
                    },
                    enabled = !isTesting && !isError && localCommand.isNotBlank(),
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.setting_custom_authorizer_test))
                }
            }

            AnimatedVisibility(visible = testResult != null) {
                testResult?.let { res ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (res.isSuccess) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(
                                    R.string.setting_custom_authorizer_test_success,
                                    res.getOrNull().orEmpty(),
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        } else {
                            Icon(
                                Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(
                                    R.string.setting_custom_authorizer_test_failed,
                                    res.exceptionOrNull()?.message.orEmpty(),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
