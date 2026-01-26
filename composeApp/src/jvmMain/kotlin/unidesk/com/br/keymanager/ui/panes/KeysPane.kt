package unidesk.com.br.keymanager.ui.panes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import keymanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import unidesk.com.br.keymanager.keytool.model.KeyInfo
import unidesk.com.br.keymanager.keytool.model.KeystoreSession
import unidesk.com.br.keymanager.ui.components.cards.KeyCard
import unidesk.com.br.keymanager.ui.components.common.EmptyState
import unidesk.com.br.keymanager.ui.components.common.SearchBar
import unidesk.com.br.keymanager.ui.state.KeyTypeFilter

@Composable
fun KeysPane(
    selectedSession: KeystoreSession?,
    keys: List<KeyInfo>,
    selectedKeyAlias: String?,
    searchQuery: String,
    typeFilter: KeyTypeFilter,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterChange: (KeyTypeFilter) -> Unit,
    onRefresh: () -> Unit,
    onSelectKey: (String) -> Unit,
    onViewDetails: (String) -> Unit,
    onExport: (String) -> Unit,
    onCopyFingerprint: (String) -> Unit,
    onRename: (String) -> Unit,
    onMove: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterMenu by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (selectedSession == null) {
            EmptyState(
                icon = Icons.Default.Folder,
                title = stringResource(Res.string.empty_no_selection),
                description = stringResource(Res.string.empty_no_selection_desc)
            )
        } else if (!selectedSession.isUnlocked) {
            EmptyState(
                icon = Icons.Default.Lock,
                title = stringResource(Res.string.empty_keystore_locked),
                description = stringResource(Res.string.empty_keystore_locked_desc)
            )
        } else {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedSession.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (selectedSession.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.action_refresh),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = stringResource(Res.string.search_keys),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(Res.string.action_filter),
                        tint = if (typeFilter != KeyTypeFilter.ALL) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.filter_all)) },
                        onClick = {
                            onTypeFilterChange(KeyTypeFilter.ALL)
                            showFilterMenu = false
                        },
                        enabled = typeFilter != KeyTypeFilter.ALL
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.filter_private_keys)) },
                        onClick = {
                            onTypeFilterChange(KeyTypeFilter.PRIVATE_KEY)
                            showFilterMenu = false
                        },
                        enabled = typeFilter != KeyTypeFilter.PRIVATE_KEY
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.filter_trusted_certs)) },
                        onClick = {
                            onTypeFilterChange(KeyTypeFilter.TRUSTED_CERT)
                            showFilterMenu = false
                        },
                        enabled = typeFilter != KeyTypeFilter.TRUSTED_CERT
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.filter_secret_keys)) },
                        onClick = {
                            onTypeFilterChange(KeyTypeFilter.SECRET_KEY)
                            showFilterMenu = false
                        },
                        enabled = typeFilter != KeyTypeFilter.SECRET_KEY
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            if (keys.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Key,
                    title = stringResource(Res.string.empty_no_results),
                    description = if (searchQuery.isNotEmpty() || typeFilter != KeyTypeFilter.ALL) {
                        stringResource(Res.string.empty_no_results_desc)
                    } else {
                        stringResource(Res.string.empty_no_keys_desc)
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = keys,
                        key = { it.alias }
                    ) { keyInfo ->
                        KeyCard(
                            keyInfo = keyInfo,
                            isSelected = keyInfo.alias == selectedKeyAlias,
                            onSelect = { onSelectKey(keyInfo.alias) },
                            onViewDetails = { onViewDetails(keyInfo.alias) },
                            onExport = { onExport(keyInfo.alias) },
                            onCopyFingerprint = { onCopyFingerprint(keyInfo.alias) },
                            onRename = { onRename(keyInfo.alias) },
                            onMove = { onMove(keyInfo.alias) },
                            onDelete = { onDelete(keyInfo.alias) }
                        )
                    }
                }
            }
        }
    }
}
