package unidesk.com.br.keymanager.ui.main

sealed interface AppEvent {
    data class ShowError(val message: String) : AppEvent
    data object ClearError : AppEvent
    data class KeystoreOpened(val sessionId: String) : AppEvent
    data class KeystoreClosed(val sessionId: String) : AppEvent
}