package unidesk.com.br.keymanager.core.repository

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import unidesk.com.br.keymanager.core.model.RecentKeystore
import java.util.Locale
import java.util.prefs.Preferences

@OptIn(ExperimentalSettingsApi::class)
class SettingsRepository {

    private val settings = PreferencesSettings(
        Preferences.userRoot().node("keymanager")
    )

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    val recentKeystores = settings.getStringOrNullFlow(KEY_RECENT_KEYSTORES).map {
        it?.let {
            json.decodeFromString<List<RecentKeystore>>(it)
        } ?: emptyList()
    }

    val isDarkMode = settings.getBooleanFlow(KEY_DARK_MODE, false)

    val defaultKeystoreFormat = settings.getStringFlow(KEY_DEFAULT_FORMAT, "PKCS12")

    val autoLockTimeoutMinutes = settings.getIntFlow(KEY_AUTO_LOCK_TIMEOUT, 0)

    val selectedLanguage = settings.getStringFlow(KEY_SELECTED_LANGUAGE, "pt-BR")

    suspend fun addRecentKeystore(path: String, name: String, keystoreType: String? = null) {
        val now = System.currentTimeMillis()
        val newRecent = RecentKeystore(
            path = path,
            name = name,
            lastOpened = now,
            keystoreType = keystoreType
        )

        val mutableList = recentKeystores.last().toMutableList()
        mutableList.removeAll { it.path == path }
        mutableList.add(0, newRecent)
        val trimmedList = mutableList.take(MAX_RECENT_KEYSTORES)
        saveRecentKeystores(trimmedList)
    }

    suspend fun removeRecentKeystore(path: String) {
        val currentList = recentKeystores.last().toMutableList()
        currentList.removeAll { it.path == path }
        saveRecentKeystores(currentList)
    }

    fun clearRecentKeystores() {
        saveRecentKeystores(emptyList())
    }

    private fun saveRecentKeystores(keystoreList: List<RecentKeystore>) {
        val jsonString = json.encodeToString(keystoreList)
        settings.putString(KEY_RECENT_KEYSTORES, jsonString)
    }

    fun setDarkMode(enabled: Boolean) {
        settings.putBoolean(KEY_DARK_MODE, enabled)
    }

    fun setDefaultKeystoreFormat(format: String) {
        settings.putString(KEY_DEFAULT_FORMAT, format)
    }

    fun setAutoLockTimeout(minutes: Int) {
        settings.putInt(KEY_AUTO_LOCK_TIMEOUT, minutes)
    }

    fun setLanguage(languageCode: String) {
        settings.putString(KEY_SELECTED_LANGUAGE, languageCode)
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
