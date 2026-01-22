package unidesk.com.br.keymanager.ui.screens.keystore

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import keymanager.composeapp.generated.resources.Res
import keymanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import unidesk.com.br.keymanager.ui.components.AliasCard
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeystoreContent(
    state: KeystoreState,
    onBulkMoveSelect: () -> Unit,
    onCreateKey: () -> Unit,
    onRefresh: () -> Unit,
    onOpenKeystore: (File) -> Unit,
    onRename: (String) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, File) -> Unit,
    onClearError: () -> Unit
) {
    val selectKeystoreTitle = stringResource(Res.string.select_keystore_title)
    val selectDestinationTitle = stringResource(Res.string.select_destination_keystore_title)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = state.currentFile?.name ?: stringResource(Res.string.app_name))
                        if (state.currentFile != null) {
                            Text(
                                text = state.currentFile.parent ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ),
                actions = {
                    if (state.isKeystoreLoaded) {
                        IconButton(onClick = onBulkMoveSelect) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(Res.string.move_action)
                            )
                        }
                        IconButton(onClick = onCreateKey) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_key))
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.refresh_action))
                        }
                    }
                    IconButton(onClick = {
                        val file = selectKeystoreFile(selectKeystoreTitle)
                        if (file != null) {
                            onOpenKeystore(file)
                        }
                    }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = stringResource(Res.string.open_keystore))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.isKeystoreLoaded) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.aliases) { keyInfo ->
                        AliasCard(
                            keyInfo = keyInfo,
                            onRename = { onRename(keyInfo.alias) },
                            onDelete = { onDelete(keyInfo.alias) },
                            onMove = {
                                val file = selectDestinationFile(selectDestinationTitle)
                                if (file != null) {
                                    onMove(keyInfo.alias, file)
                                }
                            }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(Res.string.open_keystore_hint), style = MaterialTheme.typography.titleMedium)
                }
            }

            state.errorMessage?.let { error ->
                AlertDialog(
                    onDismissRequest = onClearError,
                    title = { Text(stringResource(Res.string.error_title)) },
                    text = { Text(error) },
                    confirmButton = {
                        Button(onClick = onClearError) { Text(stringResource(Res.string.ok)) }
                    }
                )
            }
        }
    }
}

fun selectKeystoreFile(title: String = "Select Keystore"): java.io.File? {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, title, java.awt.FileDialog.LOAD)
    dialog.isVisible = true
    return if (dialog.directory != null && dialog.file != null) {
        java.io.File(dialog.directory, dialog.file)
    } else null
}

fun selectDestinationFile(title: String = "Select Destination"): java.io.File? {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, title, java.awt.FileDialog.LOAD)
    dialog.isVisible = true
    return if (dialog.directory != null && dialog.file != null) {
        java.io.File(dialog.directory, dialog.file)
    } else null
}