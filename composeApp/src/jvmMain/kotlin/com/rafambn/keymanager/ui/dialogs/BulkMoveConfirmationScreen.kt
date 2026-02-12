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
fun BulkMoveConfirmationScreen(
    selectedAliasesSize: Int,
    fileName: String,
    onConfirmMove: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val bulkMoveMsg = remember(selectedAliasesSize, fileName) {
        runBlocking {
            getString(Res.string.move_bulk_confirmation_format).format(
                selectedAliasesSize,
                fileName
            )
        }
    }

    Surface(color = Color.Transparent) {
        AlertDialog(
            onDismissRequest = onNavigateBack,
            title = { Text(stringResource(Res.string.move_bulk_dialog_title)) },
            text = { Text(bulkMoveMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmMove()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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