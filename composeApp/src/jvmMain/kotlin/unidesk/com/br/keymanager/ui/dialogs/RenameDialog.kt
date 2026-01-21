package unidesk.com.br.keymanager.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.runBlocking
import keymanager.composeapp.generated.resources.*

@Composable
fun RenameDialog(
    currentAlias: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newAlias by remember { mutableStateOf(currentAlias) }
    val currentAliasText = remember(currentAlias) {
        runBlocking { getString(Res.string.current_alias_format).format(currentAlias) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.rename_dialog_title)) },
        text = {
            Column {
                Text(currentAliasText)
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = newAlias,
                    onValueChange = { newAlias = it },
                    label = { Text(stringResource(Res.string.new_alias_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(newAlias) }) {
                Text(stringResource(Res.string.rename_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
