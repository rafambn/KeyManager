package unidesk.com.br.keymanager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import keymanager.composeapp.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun RenameScreen(
    alias: String,
    onRenameConfirm: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var newAlias by remember { mutableStateOf(alias) }
    val currentAliasText = remember(alias) {
        runBlocking { getString(Res.string.current_alias_format).format(alias) }
    }

    Surface(color = Color.Transparent) {
        AlertDialog(
            onDismissRequest = onNavigateBack,
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
                Button(onClick = {
                    onRenameConfirm(newAlias)
                }) {
                    Text(stringResource(Res.string.rename_button))
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