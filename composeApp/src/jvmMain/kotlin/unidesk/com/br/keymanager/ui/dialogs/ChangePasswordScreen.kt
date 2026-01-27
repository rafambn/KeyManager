package unidesk.com.br.keymanager.ui.dialogs

import androidx.compose.runtime.Composable

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
