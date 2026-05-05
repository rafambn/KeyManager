package com.rafambn.keymanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.*
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import com.rafambn.keymanager.keytool.model.KeyInfo
import com.rafambn.keymanager.ui.dialogs.BulkMoveConfirmationScreen
import com.rafambn.keymanager.ui.dialogs.BulkMovePasswordScreen
import com.rafambn.keymanager.ui.dialogs.BulkMoveSelectScreen
import com.rafambn.keymanager.ui.dialogs.ChangePasswordScreen
import com.rafambn.keymanager.ui.dialogs.CreateKeyScreen
import com.rafambn.keymanager.ui.dialogs.CreateKeystoreScreen
import com.rafambn.keymanager.ui.dialogs.DeleteConfirmationScreen
import com.rafambn.keymanager.ui.dialogs.ExportCertScreen
import com.rafambn.keymanager.ui.dialogs.KeyDetailsScreen
import com.rafambn.keymanager.ui.dialogs.MoveConfirmationScreen
import com.rafambn.keymanager.ui.dialogs.MovePasswordScreen
import com.rafambn.keymanager.ui.dialogs.PasswordScreen
import com.rafambn.keymanager.ui.dialogs.RenameScreen
import com.rafambn.keymanager.ui.dialogs.SettingsScreen
import com.rafambn.keymanager.selectDestinationFile
import com.rafambn.keymanager.repo.SettingsRepository
import com.rafambn.keymanager.ui.main.MainScreen
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
        sceneStrategies = listOf(dialogStrategy),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Route.Main> {
                MainScreen(
                    resultStore = resultStore,
                    onNavigate = { route -> backStack.add(route) }
                )
            }

            entry<Route.UnlockKeystore>(
                metadata = DialogSceneStrategy.dialog()
            ) { key ->
                PasswordScreen(
                    title = "Unlock Keystore",
                    onUnlock = { password ->
                        resultStore.setResult(
                            "unlock_keystore",
                            DialogResult.UnlockKeystore(key.sessionId, password)
                        )
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
                        resultStore.setResult(
                            "create_keystore",
                            DialogResult.CreateKeystore(file, password, format)
                        )
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
            ) {
                CreateKeyScreen(
                    onCreateKey = { alias, dn, validity, keyAlgorithm, keySize, signatureAlgorithm, ecCurve ->
                        resultStore.setResult(
                            "create_key",
                            DialogResult.CreateKey(alias, dn, validity, keyAlgorithm, keySize, signatureAlgorithm, ecCurve)
                        )
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
                        resultStore.setResult(
                            "rename_key",
                            DialogResult.Rename(key.alias, newAlias)
                        )
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
                        resultStore.setResult(
                            "delete_key",
                            DialogResult.Delete(key.alias)
                        )
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
                        resultStore.setResult(
                            "move_key",
                            DialogResult.Move(key.alias, File(key.filePath), key.password)
                        )
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
                            resultStore.setResult("bulk_move_selected_aliases", selected)
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
                        resultStore.setResult(
                            "bulk_move_keys",
                            DialogResult.BulkMove(key.aliases, File(key.filePath), key.password)
                        )
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
                        resultStore.setResult(
                            "change_keystore_password",
                            DialogResult.ChangePassword(key.sessionId, oldPassword, newPassword)
                        )
                        backStack.pop()
                    },
                    onNavigateBack = { backStack.pop() }
                )
            }

            entry<Route.KeyDetails>(
                metadata = DialogSceneStrategy.dialog()
            ) {
                val keyInfo = resultStore.getResultState<KeyInfo>("key_details_data")
                if (keyInfo != null) {
                    KeyDetailsScreen(
                        keyInfo = keyInfo,
                        onNavigateBack = {
                            resultStore.removeResult<KeyInfo>("key_details_data")
                            backStack.pop()
                        }
                    )
                }
            }

            entry<Route.ExportCert>(
                metadata = DialogSceneStrategy.dialog()
            ) { key ->
                ExportCertScreen(
                    alias = key.alias,
                    onExport = { file, asPem ->
                        resultStore.setResult(
                            "export_cert",
                            DialogResult.ExportCert(key.alias, file, asPem)
                        )
                        backStack.pop()
                    },
                    onNavigateBack = { backStack.pop() }
                )
            }

            entry<Route.Settings>(
                metadata = DialogSceneStrategy.dialog()
            ) {
                val settingsRepository = remember { SettingsRepository() }
                SettingsScreen(
                    repository = settingsRepository,
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
