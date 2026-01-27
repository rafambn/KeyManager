package unidesk.com.br.keymanager.ui.dialogs

import androidx.compose.runtime.Composable
import unidesk.com.br.keymanager.repo.SettingsRepository

@Composable
fun SettingsScreen(
    repository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    SettingsDialog(
        repository = repository,
        onDismiss = onNavigateBack
    )
}
