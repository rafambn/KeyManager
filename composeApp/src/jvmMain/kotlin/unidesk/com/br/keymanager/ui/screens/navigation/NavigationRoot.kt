package unidesk.com.br.keymanager.ui.screens.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
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
    val viewModel = viewModel<KeystoreViewModel>(factory = KeystoreViewModel.Factory)
    val state by viewModel.state.collectAsState()

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

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = { key ->
            when (key) {
                is Route.Main -> {
                    NavEntry(key) {
                        KeystoreContent(
                            state = state,
                            onBulkMoveSelect = { backStack.add(Route.BulkMoveSelect) },
                            onCreateKey = { backStack.add(Route.CreateKey) },
                            onRefresh = { viewModel.refresh() },
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
                            },
                            onClearError = { viewModel.clearError() }
                        )
                    }
                }
                is Route.Password -> {
                    NavEntry(key) {
                        PasswordScreen(
                            title = key.title,
                            onUnlock = { password ->
                                viewModel.loadKeystoreFile(File(key.filePath))
                                viewModel.unlockKeystore(password)
                                backStack.pop()
                            },
                            onNavigateBack = { backStack.pop() }
                        )
                    }
                }
                is Route.CreateKey -> {
                    NavEntry(key) {
                        CreateKeyScreen(
                            onCreateKey = { alias, dn, validity ->
                                viewModel.createKey(alias, dn, validity)
                                backStack.pop()
                            },
                            onNavigateBack = { backStack.pop() }
                        )
                    }
                }
                is Route.Rename -> {
                    NavEntry(key) {
                        RenameScreen(
                            alias = key.alias,
                            onRenameConfirm = { newAlias ->
                                viewModel.renameAlias(key.alias, newAlias)
                                backStack.pop()
                            },
                            onNavigateBack = { backStack.pop() }
                        )
                    }
                }
                is Route.DeleteConfirmation -> {
                    NavEntry(key) {
                        DeleteConfirmationScreen(
                            alias = key.alias,
                            onDeleteConfirm = {
                                viewModel.deleteAlias(key.alias)
                                backStack.pop()
                            },
                            onNavigateBack = { backStack.pop() }
                        )
                    }
                }
                is Route.MovePassword -> {
                    NavEntry(key) {
                        MovePasswordScreen(
                            onPasswordConfirmed = { password ->
                                backStack.add(Route.MoveConfirmation(key.alias, key.filePath, password))
                            },
                            onNavigateBack = { backStack.pop() }
                        )
                    }
                }
                is Route.MoveConfirmation -> {
                    NavEntry(key) {
                        MoveConfirmationScreen(
                            alias = key.alias,
                            fileName = File(key.filePath).name,
                            onMoveConfirm = {
                                viewModel.moveAlias(key.alias, File(key.filePath), key.password)
                                // Pop both confirmation and password screen
                                backStack.pop()
                                backStack.pop()
                            },
                            onNavigateBack = { backStack.pop() }
                        )
                    }
                }
                is Route.BulkMoveSelect -> {
                    NavEntry(key) {
                        BulkMoveSelectScreen(
                            aliases = state.aliases.map { it.alias },
                            onAliasesSelected = { selected ->
                                viewModel.setSelectedBulkAliases(selected)
                                val file = selectDestinationFile("Select Destination Keystore")
                                if (file != null) {
                                    backStack.add(Route.BulkMovePassword(file.absolutePath))
                                }
                            },
                            onNavigateBack = { backStack.pop() }
                        )
                    }
                }
                is Route.BulkMovePassword -> {
                    NavEntry(key) {
                        BulkMovePasswordScreen(
                            onPasswordConfirmed = { password ->
                                backStack.add(Route.BulkMoveConfirmation(key.filePath, password))
                            },
                            onNavigateBack = { backStack.pop() }
                        )
                    }
                }
                is Route.BulkMoveConfirmation -> {
                    NavEntry(key) {
                        BulkMoveConfirmationScreen(
                            selectedAliasesSize = viewModel.getSelectedBulkAliases().size,
                            fileName = File(key.filePath).name,
                            onConfirmMove = {
                                viewModel.moveSelectedAliases(File(key.filePath), key.password)
                                backStack.pop()
                                backStack.pop()
                                backStack.pop()
                            },
                            onNavigateBack = { backStack.pop() }
                        )
                    }
                }
                else -> error("Unknown Route: $key")
            }
        }
    )
}

private fun NavBackStack<NavKey>.pop() {
    if (this.size > 1) {
        this.removeAt(this.lastIndex)
    }
}
