package com.rafambn.keymanager.ui.dialogs

import androidx.compose.runtime.Composable
import com.rafambn.keymanager.repo.SettingsRepository

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
