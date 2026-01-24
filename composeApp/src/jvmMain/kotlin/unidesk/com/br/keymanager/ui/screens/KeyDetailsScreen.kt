package unidesk.com.br.keymanager.ui.screens

import androidx.compose.runtime.Composable
import unidesk.com.br.keymanager.core.model.KeyInfo
import unidesk.com.br.keymanager.ui.components.dialogs.KeyDetailsDialog

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
