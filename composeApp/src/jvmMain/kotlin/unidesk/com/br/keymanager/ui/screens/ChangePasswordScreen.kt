package unidesk.com.br.keymanager.ui.screens

import androidx.compose.runtime.Composable
import unidesk.com.br.keymanager.ui.components.dialogs.ChangePasswordDialog

@Composable
fun ChangePasswordScreen(
    title: String,
    onConfirm: (oldPassword: String, newPassword: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    ChangePasswordDialog(
        title = title,
        onDismiss = onNavigateBack,
        onConfirm = onConfirm
    )
}
