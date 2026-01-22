package unidesk.com.br.keymanager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import keymanager.composeapp.generated.resources.*

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