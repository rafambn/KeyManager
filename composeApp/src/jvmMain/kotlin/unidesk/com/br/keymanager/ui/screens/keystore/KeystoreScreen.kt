package unidesk.com.br.keymanager.ui.screens.keystore

import androidx.compose.runtime.Composable
import unidesk.com.br.keymanager.ui.screens.navigation.ResultStore
import java.io.File

/**
 * DEPRECATED: This screen is superseded by MainScreen in ui.screens package.
 * It is kept here only for reference. All keystore management is now handled by
 * MainScreen with AppViewModel.
 */
@Deprecated("Use MainScreen instead", level = DeprecationLevel.WARNING)
@Composable
fun KeystoreScreen(
    resultStore: ResultStore,
    onBulkMoveSelect: (List<String>) -> Unit,
    onCreateKey: () -> Unit,
    onOpenKeystore: (File) -> Unit,
    onRename: (String) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, File) -> Unit
) {
    // Empty implementation - use MainScreen instead
}
