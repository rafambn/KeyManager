package com.rafambn.keymanager.ui.dialogs

import androidx.compose.runtime.Composable
import java.io.File

@Composable
fun ExportCertScreen(
    alias: String,
    onExport: (File, Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    ExportCertDialog(
        alias = alias,
        onDismiss = onNavigateBack,
        onExport = onExport
    )
}
