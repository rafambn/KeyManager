package unidesk.com.br.keymanager.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import org.jetbrains.compose.resources.stringResource
import keymanager.composeapp.generated.resources.*

@Composable
fun PasswordDialog(
    title: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title ?: stringResource(Res.string.password_dialog_title)) },
        text = {
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(Res.string.password_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .onKeyEvent {
                        if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyDown) {
                            onConfirm(password)
                            true
                        } else {
                            false
                        }
                    }
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(password) }) {
                Text(stringResource(Res.string.unlock))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
