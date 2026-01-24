package unidesk.com.br.keymanager.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import unidesk.com.br.keymanager.core.domain.EntryType
import unidesk.com.br.keymanager.core.model.KeyInfo
import unidesk.com.br.keymanager.ui.layout.ThreePaneLayout
import unidesk.com.br.keymanager.ui.panes.KeysPane
import unidesk.com.br.keymanager.ui.panes.KeystoresPane
import unidesk.com.br.keymanager.ui.panes.NavigationPane
import unidesk.com.br.keymanager.ui.state.KeyTypeFilter
import unidesk.com.br.keymanager.ui.viewmodel.AppEvent
import unidesk.com.br.keymanager.ui.viewmodel.AppViewModel
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun MainScreen(
    viewModel: AppViewModel,
    onCreateKeystore: () -> Unit,
    onOpenKeystore: (File) -> Unit,
    onBulkMove: () -> Unit,
    onSettings: () -> Unit,
    onUnlockKeystore: (String) -> Unit,
    onCreateKey: (String) -> Unit,
    onChangeKeystorePassword: (String) -> Unit,
    onViewKeyDetails: (String) -> Unit,
    onExportKey: (String) -> Unit,
    onCopyFingerprint: (String) -> Unit,
    onRenameKey: (String) -> Unit,
    onMoveKey: (String) -> Unit,
    onDeleteKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect events and show snackbar
    LaunchedEffect(viewModel) {
        viewModel.eventChannel.collect { event ->
            when (event) {
                is AppEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long,
                        withDismissAction = true
                    )
                }
                else -> {}
            }
        }
    }

    // Compute filtered keys directly from state to ensure synchronization
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
                    onCreateKeystore = onCreateKeystore,
                    onOpenKeystore = {
                        val file = selectFile("Select Keystore")
                        if (file != null) {
                            onOpenKeystore(file)
                        }
                    },
                    onBulkMove = onBulkMove,
                    onSettings = onSettings
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
                    onUnlockKeystore = onUnlockKeystore,
                    onLockKeystore = viewModel::lockKeystore,
                    onCloseKeystore = viewModel::closeKeystore,
                    onCreateKey = onCreateKey,
                    onChangePassword = onChangeKeystorePassword
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
                    onViewDetails = onViewKeyDetails,
                    onExport = onExportKey,
                    onCopyFingerprint = onCopyFingerprint,
                    onRename = onRenameKey,
                    onMove = onMoveKey,
                    onDelete = onDeleteKey
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

fun selectSaveFile(title: String): File? {
    return selectFile(title, FileDialog.SAVE)
}
