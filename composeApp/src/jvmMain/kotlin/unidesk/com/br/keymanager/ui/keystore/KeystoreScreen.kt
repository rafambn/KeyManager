package unidesk.com.br.keymanager.ui.keystore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import unidesk.com.br.keymanager.core.KeystoreRepository

@Composable
fun KeystoreScreen() {
    val viewModel = remember { KeystoreViewModel(repository = KeystoreRepository()) }
    val state = viewModel.state.collectAsState()

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
        state = state.value,
        onLoadKeystore = viewModel::loadKeystoreFile,
        onUnlockKeystore = viewModel::unlockKeystore,
        onRefresh = viewModel::refresh,
        onCreateKey = viewModel::createKey,
        onRenameAlias = viewModel::renameAlias,
        onDeleteAlias = viewModel::deleteAlias,
        onMoveAlias = viewModel::moveAlias,
        onMoveSelected = viewModel::moveSelectedAliases,
        onClearError = viewModel::clearError
    )
}
