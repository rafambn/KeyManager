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
    data object CreateKey : Route

    @Serializable
    data class Rename(val alias: String) : Route

    @Serializable
    data class DeleteConfirmation(val alias: String) : Route

    @Serializable
    data class MovePassword(val alias: String, val filePath: String) : Route

    @Serializable
    data class MoveConfirmation(val alias: String, val filePath: String, val password: String) : Route

    @Serializable
    data object BulkMoveSelect : Route

    @Serializable
    data class BulkMovePassword(val filePath: String) : Route

    @Serializable
    data class BulkMoveConfirmation(val filePath: String, val password: String) : Route
}