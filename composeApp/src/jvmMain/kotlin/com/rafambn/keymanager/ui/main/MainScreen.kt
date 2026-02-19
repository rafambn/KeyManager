package com.rafambn.keymanager.ui.main

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rafambn.keymanager.ObserveAsEvents
import com.rafambn.keymanager.keytool.enums.EntryType
import com.rafambn.keymanager.keytool.model.KeyInfo
import com.rafambn.keymanager.ui.main.panes.ThreePaneLayout
import com.rafambn.keymanager.ui.main.panes.KeysPane
import com.rafambn.keymanager.ui.main.panes.KeystoresPane
import com.rafambn.keymanager.ui.main.panes.NavigationPane
import com.rafambn.keymanager.selectDestinationFile
import com.rafambn.keymanager.ui.navigation.DialogResult
import com.rafambn.keymanager.ui.navigation.ResultStore
import com.rafambn.keymanager.ui.navigation.Route
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

@Composable
fun MainScreen(
    resultStore: ResultStore,
    onNavigate: (Route) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveAsEvents(viewModel.eventChannel){
        when (it){
            is ShowError -> {
                snackbarHostState.showSnackbar(
                    message = it.message,
                    duration = SnackbarDuration.Long,
                    withDismissAction = true
                )
            }
        }
    }

    // Observe dialog results and process them
    LaunchedEffect(resultStore) {
        snapshotFlow { resultStore.resultStateMap.toMap() }.collect { results ->

            // UnlockKeystore
            results["unlock_keystore"]?.let { result ->
                val unlock = result as? DialogResult.UnlockKeystore
                if (unlock != null) {
                    viewModel.unlockKeystore(unlock.sessionId, unlock.password)
                    resultStore.removeResult<DialogResult.UnlockKeystore>("unlock_keystore")
                }
            }

            // CreateKeystore
            results["create_keystore"]?.let { result ->
                val create = result as? DialogResult.CreateKeystore
                if (create != null) {
                    viewModel.createKeystore(create.file, create.password)
                    resultStore.removeResult<DialogResult.CreateKeystore>("create_keystore")
                }
            }

            // CreateKey
            results["create_key"]?.let { result ->
                val create = result as? DialogResult.CreateKey
                if (create != null) {
                    viewModel.createKey(
                        alias = create.alias,
                        dn = create.dn,
                        validityDays = create.validity,
                        keyAlgorithm = create.keyAlgorithm,
                        keySize = create.keySize,
                        signatureAlgorithm = create.signatureAlgorithm,
                        ecCurve = create.ecCurve
                    )
                    resultStore.removeResult<DialogResult.CreateKey>("create_key")
                }
            }

            // RenameKey
            results["rename_key"]?.let { result ->
                val rename = result as? DialogResult.Rename
                if (rename != null) {
                    viewModel.renameKey(rename.oldAlias, rename.newAlias)
                    resultStore.removeResult<DialogResult.Rename>("rename_key")
                }
            }

            // DeleteKey
            results["delete_key"]?.let { result ->
                val delete = result as? DialogResult.Delete
                if (delete != null) {
                    viewModel.deleteKey(delete.alias)
                    resultStore.removeResult<DialogResult.Delete>("delete_key")
                }
            }

            // MoveKey
            results["move_key"]?.let { result ->
                val move = result as? DialogResult.Move
                if (move != null) {
                    viewModel.moveKey(move.alias, move.targetFile, move.targetPassword)
                    resultStore.removeResult<DialogResult.Move>("move_key")
                }
            }

            // BulkMoveKeys
            results["bulk_move_keys"]?.let { result ->
                val bulkMove = result as? DialogResult.BulkMove
                if (bulkMove != null) {
                    viewModel.setSelectedBulkAliases(bulkMove.aliases)
                    viewModel.moveSelectedKeys(bulkMove.targetFile, bulkMove.targetPassword)
                    resultStore.removeResult<DialogResult.BulkMove>("bulk_move_keys")
                    resultStore.removeResult<List<String>>("bulk_move_selected_aliases")
                }
            }

            // ChangeKeystorePassword
            results["change_keystore_password"]?.let { result ->
                val change = result as? DialogResult.ChangePassword
                if (change != null) {
                    viewModel.changeStorePassword(change.newPassword)
                    resultStore.removeResult<DialogResult.ChangePassword>("change_keystore_password")
                }
            }

            // ExportCert
            results["export_cert"]?.let { result ->
                val export = result as? DialogResult.ExportCert
                if (export != null) {
                    viewModel.exportCertificate(export.alias, export.file, export.asPem)
                    resultStore.removeResult<DialogResult.ExportCert>("export_cert")
                }
            }
        }
    }

    val allKeys = state.selectedSession?.keystoreInfo?.entries?.map { entry ->
        KeyInfo(
            alias = entry.alias,
            algorithm = entry.algorithm ?: "Unknown",
            entryType = entry.entryType,
            details = entry.owner ?: "",
            creationDate = entry.creationDate,
            validFrom = entry.validFrom,
            validUntil = entry.validUntil,
            fingerprint = entry.fingerprint,
            owner = entry.owner,
            issuer = entry.issuer,
            certificateChainLength = entry.certificateChainLength,
            serialNumber = entry.serialNumber
        )
    }?.sortedBy { it.alias } ?: emptyList()
    val filteredKeys = allKeys
        .filter { key ->
            if (state.keySearchQuery.isBlank()) true
            else key.alias.contains(state.keySearchQuery, ignoreCase = true) ||
                    key.details.contains(state.keySearchQuery, ignoreCase = true)
        }
        .filter { key ->
            when (state.keyTypeFilter) {
                KeyTypeFilter.ALL -> true
                KeyTypeFilter.PRIVATE_KEY -> key.entryType == EntryType.PRIVATE_KEY
                KeyTypeFilter.TRUSTED_CERT -> key.entryType == EntryType.TRUSTED_CERT
                KeyTypeFilter.SECRET_KEY -> key.entryType == EntryType.SECRET_KEY
            }
        }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        ThreePaneLayout(
            leftPane = {
                NavigationPane(
                    onCreateKeystore = { onNavigate(Route.CreateKeystore) },
                    onOpenKeystore = {
                        val file = selectFile("Select Keystore")
                        if (file != null) {
                            viewModel.openKeystore(file)
                        }
                    },
                    onBulkMove = {
                        val aliases = viewModel.getKeysForSelectedKeystore().map { it.alias }
                        if (aliases.isNotEmpty()) {
                            onNavigate(Route.BulkMoveSelect(aliases))
                        }
                    },
                    onSettings = { onNavigate(Route.Settings) }
                )
            },
            middlePane = {
                KeystoresPane(
                    recentKeystores = state.recentKeystores,
                    openKeystores = state.sortedKeystores,
                    selectedKeystoreId = state.selectedKeystoreId,
                    searchQuery = state.keystoreSearchQuery,
                    sortOrder = state.keystoreSortOrder,
                    onSearchQueryChange = viewModel::setKeystoreSearchQuery,
                    onSortOrderChange = viewModel::setKeystoreSortOrder,
                    onOpenRecent = viewModel::openRecentKeystore,
                    onRemoveRecent = viewModel::removeFromRecent,
                    onSelectKeystore = viewModel::selectKeystore,
                    onUnlockKeystore = { sessionId: String -> onNavigate(Route.UnlockKeystore(sessionId)) },
                    onLockKeystore = viewModel::lockKeystore,
                    onCloseKeystore = viewModel::closeKeystore,
                    onCreateKey = { sessionId: String -> onNavigate(Route.CreateKey(sessionId)) },
                    onChangePassword = { sessionId: String -> onNavigate(Route.ChangeKeystorePassword(sessionId)) }
                )
            },
            rightPane = {
                KeysPane(
                    selectedSession = state.selectedSession,
                    keys = filteredKeys,
                    selectedKeyAlias = state.selectedKeyAlias,
                    searchQuery = state.keySearchQuery,
                    typeFilter = state.keyTypeFilter,
                    onSearchQueryChange = viewModel::setKeySearchQuery,
                    onTypeFilterChange = viewModel::setKeyTypeFilter,
                    onRefresh = {
                        state.selectedKeystoreId?.let { viewModel.refreshKeystore(it) }
                    },
                    onSelectKey = viewModel::selectKey,
                    onViewDetails = { alias: String ->
                        val keys = viewModel.getKeysForSelectedKeystore()
                        val keyInfo = keys.find { it.alias == alias }
                        if (keyInfo != null) {
                            resultStore.setResult("key_details_data", keyInfo)
                            onNavigate(Route.KeyDetails(alias))
                        }
                    },
                    onExport = { alias: String -> onNavigate(Route.ExportCert(alias)) },
                    onCopyFingerprint = { alias: String ->
                        val keys = viewModel.getKeysForSelectedKeystore()
                        val keyInfo = keys.find { it.alias == alias }
                        if (keyInfo != null && keyInfo.fingerprint != null) {
                            copyToClipboard(keyInfo.fingerprint)
                        }
                    },
                    onRename = { alias: String -> onNavigate(Route.Rename(alias)) },
                    onMove = { alias: String ->
                        val file = selectDestinationFile("Select Destination Keystore")
                        if (file != null) {
                            onNavigate(Route.MovePassword(alias, file.absolutePath))
                        }
                    },
                    onDelete = { alias: String -> onNavigate(Route.DeleteConfirmation(alias)) }
                )
            }
        )
    }
}

fun selectFile(title: String, mode: Int = FileDialog.LOAD): File? {
    val dialog = FileDialog(null as Frame?, title, mode)
    dialog.isVisible = true
    return if (dialog.directory != null && dialog.file != null) {
        File(dialog.directory, dialog.file)
    } else null
}

private fun copyToClipboard(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val selection = StringSelection(text)
    clipboard.setContents(selection, selection)
}
