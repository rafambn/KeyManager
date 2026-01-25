package unidesk.com.br.keymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import unidesk.com.br.keymanager.core.KeytoolResult
import unidesk.com.br.keymanager.core.api.KeyToolAPI
import unidesk.com.br.keymanager.core.domain.EntryType
import unidesk.com.br.keymanager.core.model.KeyInfo
import unidesk.com.br.keymanager.core.model.KeystoreSession
import unidesk.com.br.keymanager.core.repository.SettingsRepository
import unidesk.com.br.keymanager.ui.state.AppState
import unidesk.com.br.keymanager.ui.state.KeyTypeFilter
import unidesk.com.br.keymanager.ui.state.KeystoreSortOrder
import java.io.File

sealed class AppEvent {
    data class ShowError(val message: String) : AppEvent()
    data object ClearError : AppEvent()
    data class KeystoreOpened(val sessionId: String) : AppEvent()
    data class KeystoreClosed(val sessionId: String) : AppEvent()
}

class AppViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AppState())

    val state: StateFlow<AppState> = combine(
        _state,
        settingsRepository.recentKeystores
    ) { appState, recentKeystores ->
        appState.copy(recentKeystores = recentKeystores)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppState()
    )

    val isDarkMode: StateFlow<Boolean> = settingsRepository.isDarkMode
    val defaultKeystoreFormat: StateFlow<String> = settingsRepository.defaultKeystoreFormat
    val autoLockTimeoutMinutes: StateFlow<Int> = settingsRepository.autoLockTimeoutMinutes
    val selectedLanguage: StateFlow<String> = settingsRepository.selectedLanguage

    private val _eventChannel = Channel<AppEvent>(Channel.BUFFERED)
    val eventChannel = _eventChannel.receiveAsFlow()

    private var selectedBulkAliases = mutableListOf<String>()

    fun getSelectedBulkAliases(): List<String> = selectedBulkAliases.toList()

    fun setSelectedBulkAliases(aliases: List<String>) {
        selectedBulkAliases.clear()
        selectedBulkAliases.addAll(aliases)
    }

    fun clearSelectedBulkAliases() {
        selectedBulkAliases.clear()
    }

    // Keystore Operations

    fun openKeystore(file: File) {
        // Check if already open
        val existingSession = _state.value.keystoreSessions.values.find { it.path == file.absolutePath }
        if (existingSession != null) {
            selectKeystore(existingSession.id)
            return
        }

        // Create new session in locked state
        val session = KeystoreSession(
            file = file,
            isLoading = false
        )

        _state.update { state ->
            state.copy(
                keystoreSessions = state.keystoreSessions + (session.id to session),
                selectedKeystoreId = session.id
            )
        }
    }

    fun unlockKeystore(sessionId: String, password: String) {
        val session = _state.value.keystoreSessions[sessionId] ?: return

        _state.update { state ->
            state.copy(
                keystoreSessions = state.keystoreSessions + (sessionId to session.copy(isLoading = true, errorMessage = null))
            )
        }

        viewModelScope.launch {
            when (val result = KeyToolAPI.list(session.file, password, verbose = true)) {
                is KeytoolResult.Success -> {
                    val updatedSession = session.copy(
                        keystoreInfo = result.data,
                        storePassword = password,
                        isLoading = false,
                        errorMessage = null
                    )

                    _state.update { state ->
                        state.copy(
                            keystoreSessions = state.keystoreSessions + (sessionId to updatedSession)
                        )
                    }

                    // Add to recent keystores
                    settingsRepository.addRecentKeystore(
                        path = session.path,
                        name = session.name,
                        keystoreType = result.data.type
                    )

                    _eventChannel.trySend(AppEvent.KeystoreOpened(sessionId))
                }

                is KeytoolResult.Error -> {
                    val errorMessage = "Failed to unlock keystore: ${result.message}"
                    _state.update { state ->
                        state.copy(
                            keystoreSessions = state.keystoreSessions + (sessionId to session.copy(
                                isLoading = false,
                                errorMessage = errorMessage
                            ))
                        )
                    }
                    _eventChannel.trySend(AppEvent.ShowError(errorMessage))
                }
            }
        }
    }

    fun lockKeystore(sessionId: String) {
        val session = _state.value.keystoreSessions[sessionId] ?: return

        val lockedSession = session.copy(
            keystoreInfo = null,
            storePassword = null,
            keyPasswords = emptyMap(),
            errorMessage = null
        )

        _state.update { state ->
            state.copy(
                keystoreSessions = state.keystoreSessions + (sessionId to lockedSession)
            )
        }
    }

    fun closeKeystore(sessionId: String) {
        _state.update { state ->
            val newSessions = state.keystoreSessions - sessionId
            val newSelectedId = if (state.selectedKeystoreId == sessionId) {
                newSessions.keys.firstOrNull()
            } else {
                state.selectedKeystoreId
            }

            state.copy(
                keystoreSessions = newSessions,
                selectedKeystoreId = newSelectedId,
                selectedKeyAlias = if (state.selectedKeystoreId == sessionId) null else state.selectedKeyAlias
            )
        }

        _eventChannel.trySend(AppEvent.KeystoreClosed(sessionId))
    }

    fun createKeystore(file: File, password: String, format: String) {
        viewModelScope.launch {
            // Create keystore by adding a temporary key
            val tempAlias = "__temp_init_key__"
            val result = KeyToolAPI.genKeyPair(
                keystore = file,
                storepass = password,
                alias = tempAlias,
                keypass = password,
                dname = "CN=Temporary",
                validity = 1
            )

            when (result) {
                is KeytoolResult.Success -> {
                    // Delete the temporary key
                    KeyToolAPI.delete(file, password, tempAlias)
                    // Open the keystore
                    openKeystore(file)
                    settingsRepository.addRecentKeystore(file.absolutePath, file.name)
                    _eventChannel.trySend(AppEvent.ShowError("Keystore created successfully"))
                }
                is KeytoolResult.Error -> {
                    _eventChannel.trySend(AppEvent.ShowError("Failed to create keystore: ${result.message}"))
                }
            }
        }
    }

    fun selectKeystore(sessionId: String?) {
        _state.update { state ->
            state.copy(
                selectedKeystoreId = sessionId,
                selectedKeyAlias = null
            )
        }
    }

    fun refreshKeystore(sessionId: String) {
        val session = _state.value.keystoreSessions[sessionId] ?: return
        val password = session.storePassword ?: return

        _state.update { state ->
            state.copy(
                keystoreSessions = state.keystoreSessions + (sessionId to session.copy(isLoading = true))
            )
        }

        viewModelScope.launch {
            when (val result = KeyToolAPI.list(session.file, password, verbose = true)) {
                is KeytoolResult.Success -> {
                    val updatedSession = session.copy(
                        keystoreInfo = result.data,
                        isLoading = false
                    )

                    _state.update { state ->
                        state.copy(
                            keystoreSessions = state.keystoreSessions + (sessionId to updatedSession)
                        )
                    }
                }

                is KeytoolResult.Error -> {
                    val errorMessage = "Failed to refresh keystore: ${result.message}"
                    _state.update { state ->
                        state.copy(
                            keystoreSessions = state.keystoreSessions + (sessionId to session.copy(
                                isLoading = false,
                                errorMessage = errorMessage
                            ))
                        )
                    }
                    _eventChannel.trySend(AppEvent.ShowError(errorMessage))
                }
            }
        }
    }

    fun openRecentKeystore(path: String) {
        val file = File(path)
        if (file.exists()) {
            openKeystore(file)
        } else {
            settingsRepository.removeRecentKeystore(path)
            _eventChannel.trySend(AppEvent.ShowError("Keystore file no longer exists: $path"))
        }
    }

    fun removeFromRecent(path: String) {
        settingsRepository.removeRecentKeystore(path)
    }

    // Key Operations

    fun selectKey(alias: String?) {
        _state.update { state ->
            state.copy(selectedKeyAlias = alias)
        }
    }

    fun getKeysForSelectedKeystore(): List<KeyInfo> {
        val session = _state.value.selectedSession ?: return emptyList()
        val keystoreInfo = session.keystoreInfo ?: return emptyList()

        return keystoreInfo.entries.map { entry ->
            KeyInfo(
                alias = entry.alias,
                algorithm = entry.algorithm ?: "Unknown",
                entryType = entry.entryType,
                details = entry.owner ?: "",
                creationDate = entry.creationDate,
                validFrom = entry.validFrom,
                validUntil = entry.validUntil,
                fingerprint = entry.fingerprint,
                owner = entry.owner,
                issuer = entry.issuer,
                certificateChainLength = entry.certificateChainLength,
                serialNumber = entry.serialNumber
            )
        }.sortedBy { it.alias }
    }

    fun getFilteredKeys(): List<KeyInfo> {
        val allKeys = getKeysForSelectedKeystore()
        val searchQuery = _state.value.keySearchQuery
        val typeFilter = _state.value.keyTypeFilter

        return allKeys
            .filter { key ->
                if (searchQuery.isBlank()) true
                else key.alias.contains(searchQuery, ignoreCase = true) ||
                        key.details.contains(searchQuery, ignoreCase = true)
            }
            .filter { key ->
                when (typeFilter) {
                    KeyTypeFilter.ALL -> true
                    KeyTypeFilter.PRIVATE_KEY -> key.entryType == EntryType.PRIVATE_KEY
                    KeyTypeFilter.TRUSTED_CERT -> key.entryType == EntryType.TRUSTED_CERT
                    KeyTypeFilter.SECRET_KEY -> key.entryType == EntryType.SECRET_KEY
                }
            }
    }

    fun deleteKey(alias: String) {
        val session = _state.value.selectedSession ?: return
        val password = session.storePassword ?: return

        viewModelScope.launch {
            when (val result = KeyToolAPI.delete(session.file, password, alias)) {
                is KeytoolResult.Success -> {
                    refreshKeystore(session.id)
                    if (_state.value.selectedKeyAlias == alias) {
                        selectKey(null)
                    }
                }

                is KeytoolResult.Error -> {
                    _eventChannel.trySend(AppEvent.ShowError("Failed to delete key: ${result.message}"))
                }
            }
        }
    }

    fun renameKey(oldAlias: String, newAlias: String, keyPassword: String? = null) {
        val session = _state.value.selectedSession ?: return
        val storePassword = session.storePassword ?: return
        val effectiveKeyPassword = keyPassword ?: session.keyPasswords[oldAlias] ?: storePassword

        viewModelScope.launch {
            when (val result = KeyToolAPI.changeAlias(
                keystore = session.file,
                storepass = storePassword,
                alias = oldAlias,
                destalias = newAlias,
                keypass = effectiveKeyPassword
            )) {
                is KeytoolResult.Success -> {
                    // Update key password cache if exists
                    if (session.keyPasswords.containsKey(oldAlias)) {
                        val newPasswords = session.keyPasswords - oldAlias + (newAlias to effectiveKeyPassword)
                        _state.update { state ->
                            state.copy(
                                keystoreSessions = state.keystoreSessions + (session.id to session.copy(
                                    keyPasswords = newPasswords
                                ))
                            )
                        }
                    }
                    refreshKeystore(session.id)
                    if (_state.value.selectedKeyAlias == oldAlias) {
                        selectKey(newAlias)
                    }
                }

                is KeytoolResult.Error -> {
                    _eventChannel.trySend(AppEvent.ShowError("Failed to rename key: ${result.message}"))
                }
            }
        }
    }

    fun moveKey(alias: String, targetFile: File, targetPassword: String) {
        val session = _state.value.selectedSession ?: return
        val sourcePassword = session.storePassword ?: return
        val keyPassword = session.keyPasswords[alias] ?: sourcePassword

        viewModelScope.launch {
            when (val importResult = KeyToolAPI.importKeystore(
                srcKeystore = session.file,
                srcStorepass = sourcePassword,
                srcAlias = alias,
                srcKeypass = keyPassword,
                destKeystore = targetFile,
                destStorepass = targetPassword,
                destKeypass = targetPassword
            )) {
                is KeytoolResult.Success -> {
                    when (val deleteResult = KeyToolAPI.delete(session.file, sourcePassword, alias)) {
                        is KeytoolResult.Success -> {
                            refreshKeystore(session.id)
                            // Also refresh target if it's open
                            val targetSession = _state.value.keystoreSessions.values.find { it.path == targetFile.absolutePath }
                            targetSession?.let { refreshKeystore(it.id) }

                            if (_state.value.selectedKeyAlias == alias) {
                                selectKey(null)
                            }
                        }

                        is KeytoolResult.Error -> {
                            _eventChannel.trySend(AppEvent.ShowError("Key copied but failed to delete from source: ${deleteResult.message}"))
                        }
                    }
                }

                is KeytoolResult.Error -> {
                    _eventChannel.trySend(AppEvent.ShowError("Failed to move key: ${importResult.message}"))
                }
            }
        }
    }

    fun createKey(alias: String, dn: String, validityDays: Int, keyPassword: String? = null) {
        val session = _state.value.selectedSession ?: return
        val storePassword = session.storePassword ?: return
        val effectiveKeyPassword = keyPassword ?: storePassword

        viewModelScope.launch {
            when (val result = KeyToolAPI.genKeyPair(
                keystore = session.file,
                storepass = storePassword,
                alias = alias,
                keypass = effectiveKeyPassword,
                dname = dn,
                validity = validityDays
            )) {
                is KeytoolResult.Success -> {
                    // Cache key password if different from store password
                    if (keyPassword != null && keyPassword != storePassword) {
                        _state.update { state ->
                            state.copy(
                                keystoreSessions = state.keystoreSessions + (session.id to session.copy(
                                    keyPasswords = session.keyPasswords + (alias to keyPassword)
                                ))
                            )
                        }
                    }
                    refreshKeystore(session.id)
                }

                is KeytoolResult.Error -> {
                    _eventChannel.trySend(AppEvent.ShowError("Failed to create key: ${result.message}"))
                }
            }
        }
    }

    fun moveSelectedKeys(targetFile: File, targetPassword: String) {
        val session = _state.value.selectedSession ?: return
        val sourcePassword = session.storePassword ?: return
        val aliasesToMove = selectedBulkAliases.toList()

        if (aliasesToMove.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isGlobalLoading = true) }
            val errors = mutableListOf<String>()

            aliasesToMove.forEach { alias ->
                val keyPassword = session.keyPasswords[alias] ?: sourcePassword

                when (val importResult = KeyToolAPI.importKeystore(
                    srcKeystore = session.file,
                    srcStorepass = sourcePassword,
                    srcAlias = alias,
                    srcKeypass = keyPassword,
                    destKeystore = targetFile,
                    destStorepass = targetPassword,
                    destKeypass = targetPassword
                )) {
                    is KeytoolResult.Success -> {
                        when (val deleteResult = KeyToolAPI.delete(session.file, sourcePassword, alias)) {
                            is KeytoolResult.Success -> { /* Success */ }
                            is KeytoolResult.Error -> {
                                errors.add("$alias: Copied but failed to delete from source: ${deleteResult.message}")
                            }
                        }
                    }

                    is KeytoolResult.Error -> {
                        errors.add("$alias: ${importResult.message}")
                    }
                }
            }

            if (errors.isNotEmpty()) {
                _eventChannel.trySend(AppEvent.ShowError("Some keys failed to move:\n${errors.joinToString("\n")}"))
            }

            clearSelectedBulkAliases()
            refreshKeystore(session.id)

            // Also refresh target if it's open
            val targetSession = _state.value.keystoreSessions.values.find { it.path == targetFile.absolutePath }
            targetSession?.let { refreshKeystore(it.id) }

            _state.update { it.copy(isGlobalLoading = false) }
        }
    }

    fun setKeyPassword(alias: String, keyPassword: String) {
        val session = _state.value.selectedSession ?: return

        _state.update { state ->
            state.copy(
                keystoreSessions = state.keystoreSessions + (session.id to session.copy(
                    keyPasswords = session.keyPasswords + (alias to keyPassword)
                ))
            )
        }
    }

    // Advanced Operations

    fun exportCertificate(alias: String, outputFile: File, asPem: Boolean) {
        val session = _state.value.selectedSession ?: return
        val storePassword = session.storePassword ?: return

        viewModelScope.launch {
            when (val result = KeyToolAPI.exportCert(
                keystore = session.file,
                storepass = storePassword,
                alias = alias,
                file = outputFile,
                rfc = asPem
            )) {
                is KeytoolResult.Success -> {
                    _eventChannel.trySend(AppEvent.ShowError("Certificate exported successfully to ${outputFile.name}"))
                }

                is KeytoolResult.Error -> {
                    _eventChannel.trySend(AppEvent.ShowError("Failed to export certificate: ${result.message}"))
                }
            }
        }
    }

    fun inspectCrl(file: File) {
        viewModelScope.launch {
            when (val result = KeyToolAPI.printCrl(file, verbose = true)) {
                is KeytoolResult.Success -> {
                    // Return CRL info through event
                    _eventChannel.trySend(AppEvent.ShowError("CRL inspection completed"))
                }

                is KeytoolResult.Error -> {
                    _eventChannel.trySend(AppEvent.ShowError("Failed to inspect CRL: ${result.message}"))
                }
            }
        }
    }

    fun changeStorePassword(newPassword: String) {
        val session = _state.value.selectedSession ?: return
        val currentPassword = session.storePassword ?: return

        viewModelScope.launch {
            when (val result = KeyToolAPI.storePasswd(
                keystore = session.file,
                storepass = currentPassword,
                newStorepass = newPassword
            )) {
                is KeytoolResult.Success -> {
                    // Update the session with new password
                    _state.update { state ->
                        state.copy(
                            keystoreSessions = state.keystoreSessions + (session.id to session.copy(
                                storePassword = newPassword
                            ))
                        )
                    }
                    _eventChannel.trySend(AppEvent.ShowError("Keystore password changed successfully"))
                }

                is KeytoolResult.Error -> {
                    _eventChannel.trySend(AppEvent.ShowError("Failed to change keystore password: ${result.message}"))
                }
            }
        }
    }

    fun changeKeyPassword(alias: String, oldPassword: String, newPassword: String) {
        val session = _state.value.selectedSession ?: return
        val storePassword = session.storePassword ?: return

        viewModelScope.launch {
            when (val result = KeyToolAPI.keyPasswd(
                keystore = session.file,
                storepass = storePassword,
                alias = alias,
                keypass = oldPassword,
                newKeypass = newPassword
            )) {
                is KeytoolResult.Success -> {
                    // Update the cached key password
                    _state.update { state ->
                        state.copy(
                            keystoreSessions = state.keystoreSessions + (session.id to session.copy(
                                keyPasswords = session.keyPasswords - alias + (alias to newPassword)
                            ))
                        )
                    }
                    _eventChannel.trySend(AppEvent.ShowError("Key password changed successfully"))
                }

                is KeytoolResult.Error -> {
                    _eventChannel.trySend(AppEvent.ShowError("Failed to change key password: ${result.message}"))
                }
            }
        }
    }

    // Search and Filter

    fun setKeystoreSearchQuery(query: String) {
        _state.update { it.copy(keystoreSearchQuery = query) }
    }

    fun setKeySearchQuery(query: String) {
        _state.update { it.copy(keySearchQuery = query) }
    }

    fun setKeystoreSortOrder(order: KeystoreSortOrder) {
        _state.update { it.copy(keystoreSortOrder = order) }
    }

    fun setKeyTypeFilter(filter: KeyTypeFilter) {
        _state.update { it.copy(keyTypeFilter = filter) }
    }

    // Settings

    fun setDarkMode(enabled: Boolean) {
        settingsRepository.setDarkMode(enabled)
    }

    fun setDefaultKeystoreFormat(format: String) {
        settingsRepository.setDefaultKeystoreFormat(format)
    }

    fun setAutoLockTimeout(minutes: Int) {
        settingsRepository.setAutoLockTimeout(minutes)
    }

    fun setLanguage(languageCode: String) {
        settingsRepository.setLanguage(languageCode)
        _eventChannel.trySend(AppEvent.ShowError("Language changed successfully."))
    }

    // Error handling

    fun clearError() {
        _state.update { it.copy(globalError = null) }
        _eventChannel.trySend(AppEvent.ClearError)
    }

    fun clearKeystoreError(sessionId: String) {
        val session = _state.value.keystoreSessions[sessionId] ?: return
        _state.update { state ->
            state.copy(
                keystoreSessions = state.keystoreSessions + (sessionId to session.copy(errorMessage = null))
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val settingsRepository = SettingsRepository()
                AppViewModel(settingsRepository = settingsRepository)
            }
        }
    }
}
