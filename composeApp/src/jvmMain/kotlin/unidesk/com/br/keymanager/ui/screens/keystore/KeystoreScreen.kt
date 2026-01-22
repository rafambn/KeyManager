package unidesk.com.br.keymanager.ui.screens.keystore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import unidesk.com.br.keymanager.ui.screens.navigation.DialogResult
import unidesk.com.br.keymanager.ui.screens.navigation.ResultStore
import java.io.File

@Composable
fun KeystoreScreen(
    resultStore: ResultStore,
    onBulkMoveSelect: (List<String>) -> Unit,
    onCreateKey: () -> Unit,
    onOpenKeystore: (File) -> Unit,
    onRename: (String) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, File) -> Unit
) {
    val viewModel = viewModel<KeystoreViewModel>(factory = KeystoreViewModel.Factory)
    val state by viewModel.state.collectAsState()

    val unlockPassword = resultStore.getResultState<String?>("unlock_password")
    LaunchedEffect(unlockPassword) {
        unlockPassword?.let { password ->
            viewModel.unlockKeystore(password)
            resultStore.removeResult<String?>("unlock_password")
        }
    }

    val createKeyResult = resultStore.getResultState<DialogResult.CreateKey?>("create_key_result")
    LaunchedEffect(createKeyResult) {
        createKeyResult?.let { result ->
            viewModel.createKey(result.alias, result.dn, result.validity)
            resultStore.removeResult<DialogResult.CreateKey?>("create_key_result")
        }
    }

    val renameResult = resultStore.getResultState<DialogResult.Rename?>("rename_result")
    LaunchedEffect(renameResult) {
        renameResult?.let { result ->
            viewModel.renameAlias(result.oldAlias, result.newAlias)
            resultStore.removeResult<DialogResult.Rename?>("rename_result")
        }
    }

    val deleteResult = resultStore.getResultState<DialogResult.Delete?>("delete_result")
    LaunchedEffect(deleteResult) {
        deleteResult?.let { result ->
            viewModel.deleteAlias(result.alias)
            resultStore.removeResult<DialogResult.Delete?>("delete_result")
        }
    }

    val moveResult = resultStore.getResultState<DialogResult.Move?>("move_result")
    LaunchedEffect(moveResult) {
        moveResult?.let { result ->
            viewModel.moveAlias(result.alias, result.targetFile, result.targetPassword)
            resultStore.removeResult<DialogResult.Move?>("move_result")
        }
    }

    val bulkMoveResult = resultStore.getResultState<DialogResult.BulkMove?>("bulk_move_result")
    LaunchedEffect(bulkMoveResult) {
        bulkMoveResult?.let { result ->
            viewModel.setSelectedBulkAliases(result.aliases)
            viewModel.moveSelectedAliases(result.targetFile, result.targetPassword)
            resultStore.removeResult<DialogResult.BulkMove?>("bulk_move_result")
        }
    }

    KeystoreContent(
        state = state,
        onBulkMoveSelect = { onBulkMoveSelect(state.aliases.map { it.alias }) },
        onCreateKey = onCreateKey,
        onRefresh = { viewModel.refresh() },
        onOpenKeystore = { file ->
            viewModel.loadKeystoreFile(file)
            onOpenKeystore(file)
        },
        onRename = onRename,
        onDelete = onDelete,
        onMove = onMove,
        onClearError = { viewModel.clearError() }
    )
}
