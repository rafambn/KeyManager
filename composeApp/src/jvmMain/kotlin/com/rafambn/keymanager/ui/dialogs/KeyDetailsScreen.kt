package com.rafambn.keymanager.ui.dialogs

import androidx.compose.runtime.Composable
import com.rafambn.keymanager.keytool.model.KeyInfo

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
