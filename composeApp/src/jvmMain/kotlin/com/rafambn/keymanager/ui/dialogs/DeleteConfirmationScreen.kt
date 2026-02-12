package com.rafambn.keymanager.ui.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import keymanager.composeapp.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeleteConfirmationScreen(
    alias: String,
    onDeleteConfirm: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val deleteMsg = remember(alias) {
        runBlocking { getString(Res.string.delete_confirmation_format).format(alias) }
    }

    Surface(color = Color.Transparent) {
        AlertDialog(
            onDismissRequest = onNavigateBack,
            title = { Text(stringResource(Res.string.delete_dialog_title)) },
            text = { Text(deleteMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteConfirm()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(Res.string.delete_button))
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