package unidesk.com.br.keymanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import keymanager.composeapp.generated.resources.*
import unidesk.com.br.keymanager.core.KeyInfo

@Composable
fun AliasCard(
    keyInfo: KeyInfo,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (keyInfo.type == "type_key") Icons.Default.Key else Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(text = keyInfo.alias, style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssistChip(
                                onClick = {}, 
                                label = { Text(keyInfo.algorithm) },
                                modifier = Modifier.height(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            val typeLabel = when(keyInfo.type) {
                                "type_key" -> stringResource(Res.string.type_key)
                                "type_certificate" -> stringResource(Res.string.type_certificate)
                                else -> stringResource(Res.string.type_unknown)
                            }
                            Text(
                                text = typeLabel, 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onMove) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(Res.string.move_action))
                    }
                    IconButton(onClick = onRename) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit_action))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.delete_button), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (keyInfo.details.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = keyInfo.details, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}
