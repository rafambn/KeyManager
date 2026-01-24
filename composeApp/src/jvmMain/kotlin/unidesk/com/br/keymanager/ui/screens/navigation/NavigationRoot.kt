package unidesk.com.br.keymanager.ui.screens.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
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
import unidesk.com.br.keymanager.ui.screens.keystore.selectDestinationFile
import unidesk.com.br.keymanager.ui.viewmodel.AppViewModel
import java.io.File

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier
) {
    val resultStore = rememberResultStore()
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)

    val backStack: NavBackStack<NavKey> = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Route.Main::class, Route.Main.serializer())
                    subclass(Route.Password::class, Route.Password.serializer())
                    subclass(Route.UnlockKeystore::class, Route.UnlockKeystore.serializer())
                    subclass(Route.CreateKeystore::class, Route.CreateKeystore.serializer())
                    subclass(Route.CreateKey::class, Route.CreateKey.serializer())
                    subclass(Route.Rename::class, Route.Rename.serializer())
                    subclass(Route.DeleteConfirmation::class, Route.DeleteConfirmation.serializer())
                    subclass(Route.MovePassword::class, Route.MovePassword.serializer())
                    subclass(Route.MoveConfirmation::class, Route.MoveConfirmation.serializer())
                    subclass(Route.BulkMoveSelect::class, Route.BulkMoveSelect.serializer())
                    subclass(Route.BulkMovePassword::class, Route.BulkMovePassword.serializer())
                    subclass(Route.BulkMoveConfirmation::class, Route.BulkMoveConfirmation.serializer())
                    subclass(Route.ChangeKeystorePassword::class, Route.ChangeKeystorePassword.serializer())
                    subclass(Route.KeyDetails::class, Route.KeyDetails.serializer())
                    subclass(Route.ExportCert::class, Route.ExportCert.serializer())
                    subclass(Route.InspectCrl::class, Route.InspectCrl.serializer())
                    subclass(Route.Settings::class, Route.Settings.serializer())
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
                MainScreen(
                    viewModel = appViewModel,
                    onCreateKeystore = { backStack.add(Route.CreateKeystore) },
                    onOpenKeystore = { file -> appViewModel.openKeystore(file) },
                    onBulkMove = {
                        val aliases = appViewModel.getKeysForSelectedKeystore().map { it.alias }
                        if (aliases.isNotEmpty()) {
                            backStack.add(Route.BulkMoveSelect(aliases))
                        }
                    },
                    onInspectCrl = { backStack.add(Route.InspectCrl) },
                    onSettings = { backStack.add(Route.Settings) },
                    onUnlockKeystore = { sessionId -> backStack.add(Route.UnlockKeystore(sessionId)) },
                    onCreateKey = { sessionId -> backStack.add(Route.CreateKey(sessionId)) },
                    onChangeKeystorePassword = { sessionId -> backStack.add(Route.ChangeKeystorePassword(sessionId)) },
                    onViewKeyDetails = { alias -> backStack.add(Route.KeyDetails(alias)) },
                    onExportKey = { alias -> backStack.add(Route.ExportCert(alias)) },
                    onCopyFingerprint = { alias ->
                        val keys = appViewModel.getKeysForSelectedKeystore()
                        val keyInfo = keys.find { it.alias == alias }
                        keyInfo?.fingerprint?.let { fingerprint ->
                            copyToClipboard(fingerprint)
                        }
                    },
                    onRenameKey = { alias -> backStack.add(Route.Rename(alias)) },
                    onMoveKey = { alias ->
                        val file = selectDestinationFile("Select Destination Keystore")
                        if (file != null) {
                            backStack.add(Route.MovePassword(alias, file.absolutePath))
                        }
                    },
                    onDeleteKey = { alias -> backStack.add(Route.DeleteConfirmation(alias)) }
                )
            }

            entry<Route.UnlockKeystore>(
                metadata = DialogSceneStrategy.dialog()
            ) { key ->
                PasswordScreen(
                    title = "Unlock Keystore",
                    onUnlock = { password ->
                        appViewModel.unlockKeystore(key.sessionId, password)
                        backStack.pop()
                    },
                    onNavigateBack = { backStack.pop() }
                )
            }

            entry<Route.CreateKeystore>(
                metadata = DialogSceneStrategy.dialog()
            ) {
                CreateKeystoreScreen(
                    onCreateKeystore = { file, password, format ->
                        resultStore.setResult("create_keystore_result", DialogResult.CreateKeystore(file, password, format))
                        backStack.pop()
                    },
                    onNavigateBack = { backStack.pop() }
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
            ) { key ->
                CreateKeyScreen(
                    onCreateKey = { alias, dn, validity ->
                        appViewModel.createKey(alias, dn, validity)
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
                        appViewModel.renameKey(key.alias, newAlias)
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
                        appViewModel.deleteKey(key.alias)
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
                        appViewModel.moveKey(key.alias, File(key.filePath), key.password)
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
                            appViewModel.setSelectedBulkAliases(selected)
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
                        appViewModel.moveSelectedKeys(File(key.filePath), key.password)
                        backStack.pop()
                        backStack.pop()
                        backStack.pop()
                    },
                    onNavigateBack = { backStack.pop() }
                )
            }

            entry<Route.ChangeKeystorePassword>(
                metadata = DialogSceneStrategy.dialog()
            ) { key ->
                ChangePasswordScreen(
                    title = "Change Keystore Password",
                    onConfirm = { oldPassword, newPassword ->
                        resultStore.setResult("change_password_result",
                            DialogResult.ChangePassword(key.sessionId, oldPassword, newPassword))
                        backStack.pop()
                    },
                    onNavigateBack = { backStack.pop() }
                )
            }

            entry<Route.KeyDetails>(
                metadata = DialogSceneStrategy.dialog()
            ) { key ->
                val keys = appViewModel.getKeysForSelectedKeystore()
                val keyInfo = keys.find { it.alias == key.alias }
                if (keyInfo != null) {
                    KeyDetailsScreen(
                        keyInfo = keyInfo,
                        onNavigateBack = { backStack.pop() }
                    )
                }
            }

            entry<Route.ExportCert>(
                metadata = DialogSceneStrategy.dialog()
            ) { key ->
                ExportCertScreen(
                    alias = key.alias,
                    onExport = { file, asPem ->
                        resultStore.setResult("export_cert_result", DialogResult.ExportCert(key.alias, file, asPem))
                        backStack.pop()
                    },
                    onNavigateBack = { backStack.pop() }
                )
            }

            entry<Route.InspectCrl>(
                metadata = DialogSceneStrategy.dialog()
            ) {
                InspectCrlScreen(
                    onNavigateBack = { backStack.pop() }
                )
            }

            entry<Route.Settings>(
                metadata = DialogSceneStrategy.dialog()
            ) {
                SettingsScreen(
                    viewModel = appViewModel,
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

private fun copyToClipboard(text: String) {
    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
    val selection = java.awt.datatransfer.StringSelection(text)
    clipboard.setContents(selection, selection)
}
