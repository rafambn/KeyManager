package unidesk.com.br.keymanager.ui.dialogs

import androidx.compose.runtime.Composable
import unidesk.com.br.keymanager.keytool.model.KeyInfo

@Composable
fun KeyDetailsScreen(
    keyInfo: KeyInfo,
    onNavigateBack: () -> Unit
) {
    KeyDetailsDialog(
        keyInfo = keyInfo,
        onDismiss = onNavigateBack
    )
}
