package com.rafambn.keymanager.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import keymanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.rafambn.keymanager.repo.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    repository: SettingsRepository,
    onDismiss: () -> Unit
) {
    val isDarkMode by repository.isDarkMode.collectAsState(initial = false)
    val defaultFormat by repository.defaultKeystoreFormat.collectAsState(initial = "PKCS12")
    val autoLockTimeout by repository.autoLockTimeoutMinutes.collectAsState(initial = 0)
    val selectedLanguage by repository.selectedLanguage.collectAsState(initial = "pt-BR")

    var formatExpanded by remember { mutableStateOf(false) }
    var timeoutExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

    val formats = listOf("PKCS12", "JKS")
    val timeoutOptions = listOf(
        0 to stringResource(Res.string.timeout_never),
        5 to stringResource(Res.string.timeout_5min),
        10 to stringResource(Res.string.timeout_10min),
        15 to stringResource(Res.string.timeout_15min),
        30 to stringResource(Res.string.timeout_30min),
        60 to stringResource(Res.string.timeout_1hour)
    )
    val languages =
        mapOf("en" to stringResource(Res.string.lang_english), "pt-BR" to stringResource(Res.string.lang_portuguese))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .widthIn(min = 400.dp, max = 600.dp)
                .heightIn(max = 700.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(Res.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(24.dp))


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(Res.string.settings_dark_mode),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(Res.string.settings_dark_mode_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { repository.setDarkMode(it) }
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))


                Text(
                    text = stringResource(Res.string.settings_keystore_format),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.settings_keystore_format_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = formatExpanded,
                    onExpandedChange = { formatExpanded = it }
                ) {
                    OutlinedTextField(
                        value = defaultFormat,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = formatExpanded,
                        onDismissRequest = { formatExpanded = false }
                    ) {
                        formats.forEach { format ->
                            DropdownMenuItem(
                                text = { Text(format) },
                                onClick = {
                                    repository.setDefaultKeystoreFormat(format)
                                    formatExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))


                Text(
                    text = stringResource(Res.string.settings_auto_lock),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.settings_auto_lock_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = timeoutExpanded,
                    onExpandedChange = { timeoutExpanded = it }
                ) {
                    OutlinedTextField(
                        value = timeoutOptions.find { it.first == autoLockTimeout }?.second ?: "Never",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeoutExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = timeoutExpanded,
                        onDismissRequest = { timeoutExpanded = false }
                    ) {
                        timeoutOptions.forEach { (minutes, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    repository.setAutoLockTimeout(minutes)
                                    timeoutExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))


                Text(
                    text = stringResource(Res.string.settings_language),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.settings_language_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = languageExpanded,
                    onExpandedChange = { languageExpanded = it }
                ) {
                    OutlinedTextField(
                        value = languages[selectedLanguage] ?: "English",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false }
                    ) {
                        languages.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    repository.setLanguage(code)
                                    languageExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text(stringResource(Res.string.settings_close))
                    }
                }
            }
        }
    }
}
