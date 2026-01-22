package unidesk.com.br.keymanager.ui.screens.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import unidesk.com.br.keymanager.ui.screens.*
import unidesk.com.br.keymanager.ui.screens.keystore.*
import java.io.File

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier
) {
    val resultStore = rememberResultStore()
    
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Route.Main::class, Route.Main.serializer())
                    subclass(Route.Password::class, Route.Password.serializer())
                    subclass(Route.CreateKey::class, Route.CreateKey.serializer())
                    subclass(Route.Rename::class, Route.Rename.serializer())
                    subclass(Route.DeleteConfirmation::class, Route.DeleteConfirmation.serializer())
                    subclass(Route.MovePassword::class, Route.MovePassword.serializer())
                    subclass(Route.MoveConfirmation::class, Route.MoveConfirmation.serializer())
                    subclass(Route.BulkMoveSelect::class, Route.BulkMoveSelect.serializer())
                    subclass(Route.BulkMovePassword::class, Route.BulkMovePassword.serializer())
                    subclass(Route.BulkMoveConfirmation::class, Route.BulkMoveConfirmation.serializer())
                }
            }
        },
        Route.Main
    )

    val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        onBack = { backStack.pop() },
        sceneStrategy = dialogStrategy,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Route.Main> {
                KeystoreScreen(
                    resultStore = resultStore,
                    onBulkMoveSelect = { aliases -> backStack.add(Route.BulkMoveSelect(aliases)) },
                    onCreateKey = { backStack.add(Route.CreateKey) },
                    onOpenKeystore = { file ->
                        backStack.add(Route.Password("Unlock Keystore", file.absolutePath))
                    },
                    onRename = { alias ->
                        backStack.add(Route.Rename(alias))
                    },
                    onDelete = { alias ->
                        backStack.add(Route.DeleteConfirmation(alias))
                    },
                    onMove = { alias, destination ->
                        backStack.add(Route.MovePassword(alias, destination.absolutePath))
                    }
                )
            }
            entry<Route.Password>(
                metadata = DialogSceneStrategy.dialog()
            ) { key ->
                PasswordScreen(
                    title = key.title,
                    onUnlock = { password ->
                        resultStore.setResult("unlock_password", password)
                        backStack.pop()
                    },
                    onNavigateBack = { backStack.pop() }
                )
            }
            entry<Route.CreateKey>(
                metadata = DialogSceneStrategy.dialog()
            ) {
                CreateKeyScreen(
                    onCreateKey = { alias, dn, validity ->
                        resultStore.setResult("create_key_result", DialogResult.CreateKey(alias, dn, validity))
                        backStack.pop()
                    },
                    onNavigateBack = { backStack.pop() }
                )
            }
            entry<Route.Rename>(
                metadata = DialogSceneStrategy.dialog()
            ) { key ->
                RenameScreen(
                    alias = key.alias,
                    onRenameConfirm = { newAlias ->
                        resultStore.setResult("rename_result", DialogResult.Rename(key.alias, newAlias))
                        backStack.pop()
                    },
                    onNavigateBack = { backStack.pop() }
                )
            }
            entry<Route.DeleteConfirmation>(
                metadata = DialogSceneStrategy.dialog()
            ) { key ->
                DeleteConfirmationScreen(
                    alias = key.alias,
                    onDeleteConfirm = {
                        resultStore.setResult("delete_result", DialogResult.Delete(key.alias))
                        backStack.pop()
                    },
                    onNavigateBack = { backStack.pop() }
                )
            }
            entry<Route.MovePassword>(
                metadata = DialogSceneStrategy.dialog()
            ) { key ->
                MovePasswordScreen(
                    onPasswordConfirmed = { password ->
                        backStack.add(Route.MoveConfirmation(key.alias, key.filePath, password))
                    },
                    onNavigateBack = { backStack.pop() }
                )
            }
            entry<Route.MoveConfirmation>(
                metadata = DialogSceneStrategy.dialog()
            ) { key ->
                MoveConfirmationScreen(
                    alias = key.alias,
                    fileName = File(key.filePath).name,
                    onMoveConfirm = {
                        resultStore.setResult("move_result", DialogResult.Move(key.alias, File(key.filePath), key.password))
                        backStack.pop()
                        backStack.pop()
                    },
                    onNavigateBack = { backStack.pop() }
                )
            }
            entry<Route.BulkMoveSelect>(
                metadata = DialogSceneStrategy.dialog()
            ) { key ->
                BulkMoveSelectScreen(
                    aliases = key.aliases,
                    onAliasesSelected = { selected ->
                        val file = selectDestinationFile("Select Destination Keystore")
                        if (file != null) {
                            backStack.add(Route.BulkMovePassword(selected, file.absolutePath))
                        }
                    },
                    onNavigateBack = { backStack.pop() }
                )
            }
            entry<Route.BulkMovePassword>(
                metadata = DialogSceneStrategy.dialog()
            ) { key ->
                BulkMovePasswordScreen(
                    onPasswordConfirmed = { password ->
                        backStack.add(Route.BulkMoveConfirmation(key.aliases, key.filePath, password))
                    },
                    onNavigateBack = { backStack.pop() }
                )
            }
            entry<Route.BulkMoveConfirmation>(
                metadata = DialogSceneStrategy.dialog()
            ) { key ->
                BulkMoveConfirmationScreen(
                    selectedAliasesSize = key.aliases.size,
                    fileName = File(key.filePath).name,
                    onConfirmMove = {
                        resultStore.setResult("bulk_move_result", DialogResult.BulkMove(key.aliases, File(key.filePath), key.password))
                        backStack.pop()
                        backStack.pop()
                        backStack.pop()
                    },
                    onNavigateBack = { backStack.pop() }
                )
            }
        }
    )
}

private fun NavBackStack<NavKey>.pop() {
    if (this.size > 1) {
        this.removeAt(this.lastIndex)
    }
}
