package unidesk.com.br.keymanager.ui.dialogs

import androidx.compose.runtime.Composable
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
