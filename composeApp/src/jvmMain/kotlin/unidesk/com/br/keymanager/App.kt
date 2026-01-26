package unidesk.com.br.keymanager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import unidesk.com.br.keymanager.core.repository.SettingsRepository
import unidesk.com.br.keymanager.ui.screens.navigation.NavigationRoot
import unidesk.com.br.keymanager.ui.theme.AppTheme

@Composable
@Preview
fun App() {
    val settingsRepository = SettingsRepository()

    val isDarkMode by settingsRepository.isDarkMode.collectAsStateWithLifecycle()
    val selectedLanguage by settingsRepository.selectedLanguage.collectAsStateWithLifecycle()

    key(selectedLanguage) {
        AppTheme(useDarkTheme = isDarkMode) {
            NavigationRoot()
        }
    }
}