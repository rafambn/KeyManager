package unidesk.com.br.keymanager.ui.screens

import androidx.compose.runtime.Composable
import unidesk.com.br.keymanager.ui.components.dialogs.CreateKeystoreDialog
import java.io.File

@Composable
fun CreateKeystoreScreen(
    onCreateKeystore: (File, String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    CreateKeystoreDialog(
        onDismiss = onNavigateBack,
        onCreateKeystore = onCreateKeystore
    )
}
