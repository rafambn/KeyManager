package unidesk.com.br.keymanager.core.repository

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import unidesk.com.br.keymanager.core.model.RecentKeystore
import java.util.prefs.Preferences

class SettingsRepository {

    private val settings: Settings = PreferencesSettings(
        Preferences.userRoot().node("keymanager")
    )

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    private val _recentKeystores = MutableStateFlow<List<RecentKeystore>>(emptyList())
    val recentKeystores: StateFlow<List<RecentKeystore>> = _recentKeystores.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _defaultKeystoreFormat = MutableStateFlow("PKCS12")
    val defaultKeystoreFormat: StateFlow<String> = _defaultKeystoreFormat.asStateFlow()

    private val _autoLockTimeoutMinutes = MutableStateFlow(0)
    val autoLockTimeoutMinutes: StateFlow<Int> = _autoLockTimeoutMinutes.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        // Load recent keystores
        val recentJson = settings.getStringOrNull(KEY_RECENT_KEYSTORES)
        if (recentJson != null) {
            try {
                _recentKeystores.value = json.decodeFromString<List<RecentKeystore>>(recentJson)
            } catch (e: Exception) {
                _recentKeystores.value = emptyList()
            }
        }

        // Load dark mode preference
        _isDarkMode.value = settings.getBoolean(KEY_DARK_MODE, false)

        // Load default keystore format
        _defaultKeystoreFormat.value = settings.getString(KEY_DEFAULT_FORMAT, "PKCS12")

        // Load auto-lock timeout
        _autoLockTimeoutMinutes.value = settings.getInt(KEY_AUTO_LOCK_TIMEOUT, 0)
    }

    fun addRecentKeystore(path: String, name: String, keystoreType: String? = null) {
        val now = System.currentTimeMillis()
        val newRecent = RecentKeystore(
            path = path,
            name = name,
            lastOpened = now,
            keystoreType = keystoreType
        )

        val currentList = _recentKeystores.value.toMutableList()

        // Remove existing entry for the same path
        currentList.removeAll { it.path == path }

        // Add new entry at the beginning
        currentList.add(0, newRecent)

        // Limit to 10 recent entries
        val trimmedList = currentList.take(MAX_RECENT_KEYSTORES)

        _recentKeystores.value = trimmedList
        saveRecentKeystores()
    }

    fun removeRecentKeystore(path: String) {
        val currentList = _recentKeystores.value.toMutableList()
        currentList.removeAll { it.path == path }
        _recentKeystores.value = currentList
        saveRecentKeystores()
    }

    fun clearRecentKeystores() {
        _recentKeystores.value = emptyList()
        saveRecentKeystores()
    }

    private fun saveRecentKeystores() {
        val jsonString = json.encodeToString(_recentKeystores.value)
        settings.putString(KEY_RECENT_KEYSTORES, jsonString)
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        settings.putBoolean(KEY_DARK_MODE, enabled)
    }

    fun setDefaultKeystoreFormat(format: String) {
        _defaultKeystoreFormat.value = format
        settings.putString(KEY_DEFAULT_FORMAT, format)
    }

    fun setAutoLockTimeout(minutes: Int) {
        _autoLockTimeoutMinutes.value = minutes
        settings.putInt(KEY_AUTO_LOCK_TIMEOUT, minutes)
    }

    companion object {
        private const val KEY_RECENT_KEYSTORES = "recent_keystores"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_DEFAULT_FORMAT = "default_keystore_format"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        private const val MAX_RECENT_KEYSTORES = 10
    }
}
