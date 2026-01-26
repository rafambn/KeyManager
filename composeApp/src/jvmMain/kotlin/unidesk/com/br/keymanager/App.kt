package unidesk.com.br.keymanager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLocalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import unidesk.com.br.keymanager.core.repository.SettingsRepository
import unidesk.com.br.keymanager.ui.locale.LocalAppLocale
import unidesk.com.br.keymanager.ui.screens.navigation.NavigationRoot
import unidesk.com.br.keymanager.ui.theme.AppTheme
import java.util.Locale

@Composable
@Preview
fun App() {
    val settingsRepository = SettingsRepository()
    val lifecycle = LocalLifecycleOwner.current

    val isDarkMode by settingsRepository.isDarkMode.collectAsStateWithLifecycle(false, lifecycle)
    val selectedLanguage by settingsRepository.selectedLanguage.collectAsStateWithLifecycle(
        Locale.getDefault().language,
        lifecycle
    )

    AppTheme(useDarkTheme = isDarkMode, languageCode = selectedLanguage) {
        NavigationRoot()
    }
}