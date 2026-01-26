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

    private val _selectedLanguage = MutableStateFlow("pt-BR")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {

        val recentJson = settings.getStringOrNull(KEY_RECENT_KEYSTORES)
        if (recentJson != null) {
            try {
                _recentKeystores.value = json.decodeFromString<List<RecentKeystore>>(recentJson)
            } catch (e: Exception) {
                _recentKeystores.value = emptyList()
            }
        }


        _isDarkMode.value = settings.getBoolean(KEY_DARK_MODE, false)

        _defaultKeystoreFormat.value = settings.getString(KEY_DEFAULT_FORMAT, "PKCS12")

        _autoLockTimeoutMinutes.value = settings.getInt(KEY_AUTO_LOCK_TIMEOUT, 0)

        _selectedLanguage.value = settings.getString(KEY_SELECTED_LANGUAGE, "pt-BR")
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


        currentList.removeAll { it.path == path }


        currentList.add(0, newRecent)


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

    fun setLanguage(languageCode: String) {
        _selectedLanguage.value = languageCode
        settings.putString(KEY_SELECTED_LANGUAGE, languageCode)


        val (lang, country) = if (languageCode.contains("-")) {
            val parts = languageCode.split("-")
            parts[0] to parts[1]
        } else {
            languageCode to ""
        }

        System.setProperty("user.language", lang)
        if (country.isNotEmpty()) {
            System.setProperty("user.country", country)
        }


        val locale = if (country.isNotEmpty()) {
            java.util.Locale(lang, country)
        } else {
            java.util.Locale(lang)
        }
        java.util.Locale.setDefault(locale)
    }

    companion object {
        private const val KEY_RECENT_KEYSTORES = "recent_keystores"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_DEFAULT_FORMAT = "default_keystore_format"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        private const val KEY_SELECTED_LANGUAGE = "selected_language"
        private const val MAX_RECENT_KEYSTORES = 10
    }
}
