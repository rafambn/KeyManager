package com.rafambn.keymanager.ui.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import keymanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun BulkMovePasswordScreen(
    onPasswordConfirmed: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    Surface(color = Color.Transparent) {
        AlertDialog(
            onDismissRequest = onNavigateBack,
            title = { Text(stringResource(Res.string.destination_password_title)) },
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
                                onPasswordConfirmed(password)
                                true
                            } else {
                                false
                            }
                        }
                )
            },
            confirmButton = {
                Button(onClick = {
                    onPasswordConfirmed(password)
                }) {
                    Text(stringResource(Res.string.ok))
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