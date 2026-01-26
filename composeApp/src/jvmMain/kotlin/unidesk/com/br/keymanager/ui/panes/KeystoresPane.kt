package unidesk.com.br.keymanager.ui.panes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import keymanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import unidesk.com.br.keymanager.core.model.KeystoreSession
import unidesk.com.br.keymanager.core.model.RecentKeystore
import unidesk.com.br.keymanager.ui.components.cards.KeystoreCard
import unidesk.com.br.keymanager.ui.components.cards.RecentKeystoreCard
import unidesk.com.br.keymanager.ui.components.common.EmptyState
import unidesk.com.br.keymanager.ui.components.common.SearchBar
import unidesk.com.br.keymanager.ui.state.KeystoreSortOrder

@Composable
fun KeystoresPane(
    recentKeystores: List<RecentKeystore>,
    openKeystores: List<KeystoreSession>,
    selectedKeystoreId: String?,
    searchQuery: String,
    sortOrder: KeystoreSortOrder,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (KeystoreSortOrder) -> Unit,
    onOpenRecent: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onSelectKeystore: (String) -> Unit,
    onUnlockKeystore: (String) -> Unit,
    onLockKeystore: (String) -> Unit,
    onCloseKeystore: (String) -> Unit,
    onCreateKey: (String) -> Unit,
    onChangePassword: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }


    val openPaths = openKeystores.map { it.path }.toSet()
    val filteredRecents = recentKeystores.filter { it.path !in openPaths }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = stringResource(Res.string.search_keystores),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { showSortMenu = true }) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = stringResource(Res.string.action_sort),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.sort_recent)) },
                    onClick = {
                        onSortOrderChange(KeystoreSortOrder.RECENT)
                        showSortMenu = false
                    },
                    enabled = sortOrder != KeystoreSortOrder.RECENT
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.sort_name)) },
                    onClick = {
                        onSortOrderChange(KeystoreSortOrder.NAME)
                        showSortMenu = false
                    },
                    enabled = sortOrder != KeystoreSortOrder.NAME
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.sort_path)) },
                    onClick = {
                        onSortOrderChange(KeystoreSortOrder.PATH)
                        showSortMenu = false
                    },
                    enabled = sortOrder != KeystoreSortOrder.PATH
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (openKeystores.isEmpty() && filteredRecents.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Folder,
                title = stringResource(Res.string.empty_no_keystores),
                description = stringResource(Res.string.empty_no_keystores_desc)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                if (filteredRecents.isNotEmpty()) {
                    item {
                        SectionHeader(title = stringResource(Res.string.table_header_recent))
                    }
                    items(
                        items = filteredRecents,
                        key = { "recent_${it.path}" }
                    ) { recent ->
                        RecentKeystoreCard(
                            recentKeystore = recent,
                            onOpen = { onOpenRecent(recent.path) },
                            onRemove = { onRemoveRecent(recent.path) }
                        )
                    }
                }


                if (openKeystores.isNotEmpty()) {
                    item {
                        if (filteredRecents.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                        }
                        SectionHeader(title = stringResource(Res.string.table_header_open))
                    }
                    items(
                        items = openKeystores,
                        key = { "open_${it.id}" }
                    ) { session ->
                        KeystoreCard(
                            session = session,
                            isSelected = session.id == selectedKeystoreId,
                            onSelect = { onSelectKeystore(session.id) },
                            onUnlock = { onUnlockKeystore(session.id) },
                            onLock = { onLockKeystore(session.id) },
                            onClose = { onCloseKeystore(session.id) },
                            onCreateKey = { onCreateKey(session.id) },
                            onChangePassword = { onChangePassword(session.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
    }
}
