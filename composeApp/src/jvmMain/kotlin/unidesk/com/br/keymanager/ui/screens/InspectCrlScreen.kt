package unidesk.com.br.keymanager.ui.screens

import androidx.compose.runtime.Composable
import unidesk.com.br.keymanager.ui.components.dialogs.InspectCrlDialog

@Composable
fun InspectCrlScreen(
    onNavigateBack: () -> Unit
) {
    InspectCrlDialog(
        onDismiss = onNavigateBack
    )
}
