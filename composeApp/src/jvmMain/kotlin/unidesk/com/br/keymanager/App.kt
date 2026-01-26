package unidesk.com.br.keymanager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import unidesk.com.br.keymanager.ui.screens.navigation.NavigationRoot
import unidesk.com.br.keymanager.ui.theme.AppTheme
import unidesk.com.br.keymanager.ui.viewmodel.AppViewModel

@Composable
@Preview
fun App() {
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val isDarkMode by appViewModel.isDarkMode.collectAsState()
    val selectedLanguage by appViewModel.selectedLanguage.collectAsState()

    key(selectedLanguage) {
        AppTheme(useDarkTheme = isDarkMode) {
            NavigationRoot()
        }
    }
}