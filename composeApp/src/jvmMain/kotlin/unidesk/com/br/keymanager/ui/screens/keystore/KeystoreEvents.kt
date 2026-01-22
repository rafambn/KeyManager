package unidesk.com.br.keymanager.ui.screens.keystore

sealed interface KeystoreEvents {
    data class ShowError(val message: String) : KeystoreEvents
    data object ClearError : KeystoreEvents
}