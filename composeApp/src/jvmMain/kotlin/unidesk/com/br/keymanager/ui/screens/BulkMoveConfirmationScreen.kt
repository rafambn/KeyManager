package unidesk.com.br.keymanager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import keymanager.composeapp.generated.resources.*

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