package app.pwhs.updater.presentation.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.pwhs.core.ui.theme.Spacing

@Composable
fun SourceTokensDialog(
    initialGithubToken: String,
    initialGitlabToken: String,
    initialCodebergToken: String,
    onDismiss: () -> Unit,
    onSave: (githubToken: String, gitlabToken: String, codebergToken: String) -> Unit,
) {
    var githubToken by remember { mutableStateOf(initialGithubToken) }
    var gitlabToken by remember { mutableStateOf(initialGitlabToken) }
    var codebergToken by remember { mutableStateOf(initialCodebergToken) }

    var showGithubSecret by remember { mutableStateOf(false) }
    var showGitlabSecret by remember { mutableStateOf(false) }
    var showCodebergSecret by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = "Source API Tokens",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Configure API tokens globally to avoid rate limiting and access private repositories across all tracked apps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(Spacing.L))

                // GitHub Token
                OutlinedTextField(
                    value = githubToken,
                    onValueChange = { githubToken = it },
                    label = { Text("GitHub Token (PAT)") },
                    placeholder = { Text("ghp_xxxxxxxxxxxx") },
                    singleLine = true,
                    visualTransformation = if (showGithubSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showGithubSecret = !showGithubSecret }) {
                            Icon(
                                imageVector = if (showGithubSecret) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    supportingText = { Text("Increases rate limit from 60 to 5,000 req/hr") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(Spacing.M))

                // GitLab Token
                OutlinedTextField(
                    value = gitlabToken,
                    onValueChange = { gitlabToken = it },
                    label = { Text("GitLab Token") },
                    placeholder = { Text("glpat-xxxxxxxxxxxx") },
                    singleLine = true,
                    visualTransformation = if (showGitlabSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showGitlabSecret = !showGitlabSecret }) {
                            Icon(
                                imageVector = if (showGitlabSecret) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    supportingText = { Text("For GitLab repository releases") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(Spacing.M))

                // Codeberg Token
                OutlinedTextField(
                    value = codebergToken,
                    onValueChange = { codebergToken = it },
                    label = { Text("Codeberg / Gitea Token") },
                    placeholder = { Text("Token for Codeberg") },
                    singleLine = true,
                    visualTransformation = if (showCodebergSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showCodebergSecret = !showCodebergSecret }) {
                            Icon(
                                imageVector = if (showCodebergSecret) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    supportingText = { Text("For Codeberg & Forgejo / Gitea releases") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(githubToken.trim(), gitlabToken.trim(), codebergToken.trim())
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
