package unidesk.com.br.keymanager

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import unidesk.com.br.keymanager.ui.keystore.KeystoreScreen
import unidesk.com.br.keymanager.ui.theme.AppTheme

@Composable
@Preview
fun App() {
    AppTheme {
        KeystoreScreen()
    }
}