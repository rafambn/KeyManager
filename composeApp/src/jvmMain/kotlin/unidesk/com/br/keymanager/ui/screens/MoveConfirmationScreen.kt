package unidesk.com.br.keymanager.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import keymanager.composeapp.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun MoveConfirmationScreen(
    alias: String,
    fileName: String,
    onMoveConfirm: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val moveMsg = remember(alias, fileName) {
        runBlocking { getString(Res.string.move_confirmation_format).format(alias, fileName) }
    }

    Surface(color = Color.Transparent) {
        AlertDialog(
            onDismissRequest = onNavigateBack,
            title = { Text(stringResource(Res.string.move_dialog_title)) },
            text = { Text(moveMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        onMoveConfirm()
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