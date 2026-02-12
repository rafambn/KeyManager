package com.rafambn.keymanager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rafambn.keymanager.repo.SettingsRepository
import com.rafambn.keymanager.ui.navigation.NavigationRoot
import com.rafambn.keymanager.ui.theme.AppTheme
import java.util.*

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