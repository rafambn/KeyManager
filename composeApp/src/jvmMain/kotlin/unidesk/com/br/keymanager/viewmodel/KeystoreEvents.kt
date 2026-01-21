package unidesk.com.br.keymanager.viewmodel

import kotlinx.coroutines.flow.Flow

sealed interface KeystoreEvents {
    data class ShowError(val message: String) : KeystoreEvents
    data object ClearError : KeystoreEvents
}

interface KeystoreEventsProvider {
    val eventChannel: Flow<KeystoreEvents>
}