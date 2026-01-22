package unidesk.com.br.keymanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import keymanager.composeapp.generated.resources.*

@Composable
fun BulkMoveSelectScreen(
    aliases: List<String>,
    onAliasesSelected: (List<String>) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedAliases by remember { mutableStateOf(setOf<String>()) }

    Surface(color = Color.Transparent) {
        AlertDialog(
            onDismissRequest = onNavigateBack,
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
                    onClick = {
                        if (selectedAliases.isNotEmpty()) {
                            onAliasesSelected(selectedAliases.toList())
                        }
                    },
                    enabled = selectedAliases.isNotEmpty()
                ) {
                    Text(stringResource(Res.string.move_button))
                }
            },
            dismissButton = {
                TextButton(onClick = onNavigateBack) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}