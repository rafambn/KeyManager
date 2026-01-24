package unidesk.com.br.keymanager.ui.screens

import androidx.compose.runtime.Composable
import unidesk.com.br.keymanager.ui.components.dialogs.SettingsDialog
import unidesk.com.br.keymanager.ui.viewmodel.AppViewModel

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    SettingsDialog(
        viewModel = viewModel,
        onDismiss = onNavigateBack
    )
}
