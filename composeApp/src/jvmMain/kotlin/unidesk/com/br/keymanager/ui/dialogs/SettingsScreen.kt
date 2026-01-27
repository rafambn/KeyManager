package unidesk.com.br.keymanager.ui.dialogs

import androidx.compose.runtime.Composable
import unidesk.com.br.keymanager.ui.main.AppViewModel

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
