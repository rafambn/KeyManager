package unidesk.com.br.keymanager.ui.screens.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Main : Route

    @Serializable
    data class Password(val title: String, val filePath: String) : Route

    @Serializable
    data class UnlockKeystore(val sessionId: String) : Route

    @Serializable
    data object CreateKeystore : Route

    @Serializable
    data class CreateKey(val sessionId: String) : Route

    @Serializable
    data class Rename(val alias: String) : Route

    @Serializable
    data class DeleteConfirmation(val alias: String) : Route

    @Serializable
    data class MovePassword(val alias: String, val filePath: String) : Route

    @Serializable
    data class MoveConfirmation(val alias: String, val filePath: String, val password: String) : Route

    @Serializable
    data class BulkMoveSelect(val aliases: List<String>) : Route

    @Serializable
    data class BulkMovePassword(val aliases: List<String>, val filePath: String) : Route

    @Serializable
    data class BulkMoveConfirmation(val aliases: List<String>, val filePath: String, val password: String) : Route

    @Serializable
    data class ChangeKeystorePassword(val sessionId: String) : Route

    @Serializable
    data class KeyDetails(val alias: String) : Route

    @Serializable
    data class ExportCert(val alias: String) : Route

    @Serializable
    data object Settings : Route
}