package unidesk.com.br.keymanager.ui.keystore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import keymanager.composeapp.generated.resources.*
import unidesk.com.br.keymanager.core.KeystoreRepository
import java.io.File

class KeystoreViewModel(
    private val repository: KeystoreRepository
) : ViewModel(), KeystoreEventsProvider {

    private val _state = MutableStateFlow(KeystoreState())
    
    val state = _state
        .onStart {
            // Perform any initial data loading here if necessary
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = KeystoreState()
        )

    private val _eventChannel = Channel<KeystoreEvents>(Channel.BUFFERED)
    override val eventChannel = _eventChannel.receiveAsFlow()

    fun loadKeystoreFile(file: File) {
        _state.update { it.copy(currentFile = file, errorMessage = null) }
    }

    fun unlockKeystore(password: String) {
        val file = _state.value.currentFile ?: return
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.loadKeystore(file, password)
                }
                val newAliases = repository.getKeys()
                _state.update { 
                    it.copy(
                        aliases = newAliases, 
                        isKeystoreLoaded = true,
                        isLoading = false
                    ) 
                }
            } catch (e: Exception) {
                val msg = getString(Res.string.error_load_keystore).format(e.message ?: "")
                _state.update { it.copy(errorMessage = msg, isLoading = false) }
                _eventChannel.trySend(KeystoreEvents.ShowError(msg))
                e.printStackTrace()
            }
        }
    }

    fun deleteAlias(alias: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteAlias(alias)
                }
                refreshAliases()
            } catch (e: Exception) {
                val msg = getString(Res.string.error_delete_alias).format(e.message ?: "")
                _state.update { it.copy(errorMessage = msg) }
                _eventChannel.trySend(KeystoreEvents.ShowError(msg))
            }
        }
    }

    fun renameAlias(oldAlias: String, newAlias: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.renameAlias(oldAlias, newAlias)
                }
                refreshAliases()
            } catch (e: Exception) {
                val msg = getString(Res.string.error_rename_alias).format(e.message ?: "")
                _state.update { it.copy(errorMessage = msg) }
                _eventChannel.trySend(KeystoreEvents.ShowError(msg))
            }
        }
    }

    fun moveAlias(alias: String, targetFile: File, targetPassword: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.moveAlias(alias, targetFile, targetPassword)
                }
                refreshAliases()
            } catch (e: Exception) {
                val msg = getString(Res.string.error_move_alias).format(e.message ?: "")
                _state.update { it.copy(errorMessage = msg) }
                _eventChannel.trySend(KeystoreEvents.ShowError(msg))
            }
        }
    }

    fun createKey(alias: String, dn: String, validityDays: Int) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.addCertificate(alias, dn, validityDays)
                }
                refreshAliases()
            } catch (e: Exception) {
                val msg = getString(Res.string.error_create_key).format(e.message ?: "")
                _state.update { it.copy(errorMessage = msg) }
                _eventChannel.trySend(KeystoreEvents.ShowError(msg))
            }
        }
    }

    fun toggleSelection(alias: String) {
        // Not used anymore in the new Bulk Move flow, but keeping for compatibility if needed or removing
    }

    fun clearSelection() {
        // Not used anymore
    }

    fun selectAll() {
        // Not used anymore
    }

    fun moveSelectedAliases(aliasesToMove: List<String>, targetFile: File, targetPassword: String) {
        if (aliasesToMove.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val errors = mutableListOf<String>()
            
            withContext(Dispatchers.IO) {
                aliasesToMove.forEach { alias ->
                    try {
                        repository.moveAlias(alias, targetFile, targetPassword)
                    } catch (e: Exception) {
                        errors.add("$alias: ${e.message}")
                    }
                }
            }

            if (errors.isNotEmpty()) {
                val errorDetails = errors.joinToString("\n")
                val msg = getString(Res.string.error_bulk_move).format(errorDetails)
                _state.update { it.copy(errorMessage = msg, isLoading = false) }
                _eventChannel.trySend(KeystoreEvents.ShowError(msg))
            } else {
                _state.update { it.copy(isLoading = false) }
            }
            refreshAliases()
        }
    }

    fun refresh() {
        val file = _state.value.currentFile ?: return
        val currentPassword = repository.getCurrentPassword()?.let { String(it) } ?: return
        
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.loadKeystore(file, currentPassword)
                }
                refreshAliases()
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                val msg = getString(Res.string.error_load_keystore).format(e.message ?: "")
                _state.update { it.copy(errorMessage = msg, isLoading = false) }
                _eventChannel.trySend(KeystoreEvents.ShowError(msg))
            }
        }
    }

    private fun refreshAliases() {
        val newAliases = repository.getKeys()
        _state.update { it.copy(aliases = newAliases) }
    }
    
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
        _eventChannel.trySend(KeystoreEvents.ClearError)
    }
    
    fun closeKeystore() {
        _state.update { KeystoreState() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = KeystoreRepository()
                KeystoreViewModel(repository = repository)
            }
        }
    }
}
