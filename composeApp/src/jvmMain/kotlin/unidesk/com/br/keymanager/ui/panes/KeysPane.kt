package unidesk.com.br.keymanager.ui.panes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import unidesk.com.br.keymanager.core.model.KeyInfo
import unidesk.com.br.keymanager.core.model.KeystoreSession
import unidesk.com.br.keymanager.ui.components.cards.KeyCard
import unidesk.com.br.keymanager.ui.components.common.EmptyState
import unidesk.com.br.keymanager.ui.components.common.EmptyStateDefaults
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
                icon = EmptyStateDefaults.NoSelection.icon,
                title = EmptyStateDefaults.NoSelection.title,
                description = EmptyStateDefaults.NoSelection.description
            )
        } else if (!selectedSession.isUnlocked) {
            EmptyState(
                icon = Icons.Default.Lock,
                title = "Keystore Locked",
                description = "Unlock the keystore to view its contents"
            )
        } else {
            // Header
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
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Search and Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Search keys...",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
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
                        text = { Text("All") },
                        onClick = {
                            onTypeFilterChange(KeyTypeFilter.ALL)
                            showFilterMenu = false
                        },
                        enabled = typeFilter != KeyTypeFilter.ALL
                    )
                    DropdownMenuItem(
                        text = { Text("Private Keys") },
                        onClick = {
                            onTypeFilterChange(KeyTypeFilter.PRIVATE_KEY)
                            showFilterMenu = false
                        },
                        enabled = typeFilter != KeyTypeFilter.PRIVATE_KEY
                    )
                    DropdownMenuItem(
                        text = { Text("Trusted Certs") },
                        onClick = {
                            onTypeFilterChange(KeyTypeFilter.TRUSTED_CERT)
                            showFilterMenu = false
                        },
                        enabled = typeFilter != KeyTypeFilter.TRUSTED_CERT
                    )
                    DropdownMenuItem(
                        text = { Text("Secret Keys") },
                        onClick = {
                            onTypeFilterChange(KeyTypeFilter.SECRET_KEY)
                            showFilterMenu = false
                        },
                        enabled = typeFilter != KeyTypeFilter.SECRET_KEY
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Keys List
            if (keys.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Key,
                    title = "No keys found",
                    description = if (searchQuery.isNotEmpty() || typeFilter != KeyTypeFilter.ALL) {
                        "Try adjusting your search or filter"
                    } else {
                        "Create a new key or import certificates"
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
