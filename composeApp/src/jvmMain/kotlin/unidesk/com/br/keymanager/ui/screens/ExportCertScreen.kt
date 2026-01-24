package unidesk.com.br.keymanager.ui.screens

import androidx.compose.runtime.Composable
import unidesk.com.br.keymanager.ui.components.dialogs.ExportCertDialog
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
