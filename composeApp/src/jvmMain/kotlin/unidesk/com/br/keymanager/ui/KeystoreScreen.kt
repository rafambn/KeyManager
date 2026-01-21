package unidesk.com.br.keymanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import keymanager.composeapp.generated.resources.Res
import keymanager.composeapp.generated.resources.*
import unidesk.com.br.keymanager.ui.components.AliasCard
import unidesk.com.br.keymanager.ui.dialogs.ConfirmationDialog
import unidesk.com.br.keymanager.ui.dialogs.CreateKeyDialog
import unidesk.com.br.keymanager.ui.dialogs.PasswordDialog
import unidesk.com.br.keymanager.ui.dialogs.RenameDialog
import unidesk.com.br.keymanager.viewmodel.KeystoreState
import unidesk.com.br.keymanager.viewmodel.MainViewModel
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import org.jetbrains.compose.resources.stringResource
import unidesk.com.br.keymanager.viewmodel.KeystoreEvents

@Composable
fun KeystoreScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.eventChannel.collect { event ->
            when (event) {
                is KeystoreEvents.ShowError -> {
                    // Handled by state.errorMessage for now, can add Snackbar later
                    println("Event Error: ${event.message}")
                }

                KeystoreEvents.ClearError -> {
                    // Handled by state
                }
            }
        }
    }

    KeystoreContent(
        state = state,
        onLoadKeystore = viewModel::loadKeystoreFile,
        onUnlockKeystore = viewModel::unlockKeystore,
        onCreateKey = viewModel::createKey,
        onRenameAlias = viewModel::renameAlias,
        onDeleteAlias = viewModel::deleteAlias,
        onMoveAlias = viewModel::moveAlias,
        onClearError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeystoreContent(
    state: KeystoreState,
    onLoadKeystore: (File) -> Unit,
    onUnlockKeystore: (String) -> Unit,
    onCreateKey: (alias: String, dn: String, validity: Int) -> Unit,
    onRenameAlias: (oldAlias: String, newAlias: String) -> Unit,
    onDeleteAlias: (alias: String) -> Unit,
    onMoveAlias: (alias: String, targetFile: File, password: String) -> Unit,
    onClearError: () -> Unit
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    var aliasToRename by remember { mutableStateOf<String?>(null) }
    var aliasToDelete by remember { mutableStateOf<String?>(null) }

    // Move state
    var aliasToMove by remember { mutableStateOf<String?>(null) }
    var moveTargetFile by remember { mutableStateOf<File?>(null) }
    var showMovePasswordDialog by remember { mutableStateOf(false) }

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
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_key))
                        }
                    }
                    IconButton(onClick = {
                        val file = openFileDialog(
                            mode = FileDialog.LOAD,
                            title = selectKeystoreTitle
                        )
                        if (file != null) {
                            onLoadKeystore(file)
                            showPasswordDialog = true
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
                            onRename = { aliasToRename = keyInfo.alias },
                            onDelete = { aliasToDelete = keyInfo.alias },
                            onMove = {
                                val file = openFileDialog(
                                    mode = FileDialog.LOAD,
                                    title = selectDestinationTitle
                                )
                                if (file != null) {
                                    aliasToMove = keyInfo.alias
                                    moveTargetFile = file
                                    showMovePasswordDialog = true
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

    // Dialogs
    if (showPasswordDialog) {
        PasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { password ->
                onUnlockKeystore(password)
                showPasswordDialog = false
            }
        )
    }

    if (showMovePasswordDialog && aliasToMove != null && moveTargetFile != null) {
        PasswordDialog(
            title = stringResource(Res.string.destination_password_title),
            onDismiss = {
                showMovePasswordDialog = false
                aliasToMove = null
                moveTargetFile = null
            },
            onConfirm = { password ->
                onMoveAlias(aliasToMove!!, moveTargetFile!!, password)
                showMovePasswordDialog = false
                aliasToMove = null
                moveTargetFile = null
            }
        )
    }

    if (showCreateDialog) {
        CreateKeyDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { alias, dn, validity ->
                onCreateKey(alias, dn, validity)
                showCreateDialog = false
            }
        )
    }

    aliasToRename?.let { alias ->
        RenameDialog(
            currentAlias = alias,
            onDismiss = { aliasToRename = null },
            onConfirm = { newAlias ->
                onRenameAlias(alias, newAlias)
                aliasToRename = null
            }
        )
    }

    aliasToDelete?.let { alias ->
        ConfirmationDialog(
            title = stringResource(Res.string.delete_dialog_title),
            message = stringResource(Res.string.delete_confirmation_format, alias),
            onConfirm = {
                onDeleteAlias(alias)
                aliasToDelete = null
            },
            onDismiss = { aliasToDelete = null }
        )
    }
}

fun openFileDialog(mode: Int, title: String): File? {
    val dialog = FileDialog(null as Frame?, title, mode)
    dialog.isVisible = true
    return if (dialog.directory != null && dialog.file != null) {
        File(dialog.directory, dialog.file)
    } else null
}
