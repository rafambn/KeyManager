package unidesk.com.br.keymanager.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RenameDialog(
    currentAlias: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newAlias by remember { mutableStateOf(currentAlias) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Alias") },
        text = {
            Column {
                Text("Current: $currentAlias")
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = newAlias,
                    onValueChange = { newAlias = it },
                    label = { Text("New Alias") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(newAlias) }) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
