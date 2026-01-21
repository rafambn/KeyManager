package unidesk.com.br.keymanager.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import keymanager.composeapp.generated.resources.*

@Composable
fun BulkMoveDialog(
    aliases: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var selectedAliases by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.move_bulk_dialog_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { selectedAliases = aliases.toSet() }) {
                        Text(stringResource(Res.string.select_all))
                    }
                    TextButton(onClick = { selectedAliases = emptySet() }) {
                        Text(stringResource(Res.string.deselect_all))
                    }
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(aliases) { alias ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedAliases.contains(alias),
                                onCheckedChange = { checked ->
                                    selectedAliases = if (checked) {
                                        selectedAliases + alias
                                    } else {
                                        selectedAliases - alias
                                    }
                                }
                            )
                            Text(text = alias, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedAliases.toList()) },
                enabled = selectedAliases.isNotEmpty()
            ) {
                Text(stringResource(Res.string.move_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
