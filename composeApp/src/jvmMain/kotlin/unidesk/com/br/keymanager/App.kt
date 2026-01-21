package unidesk.com.br.keymanager

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import unidesk.com.br.keymanager.ui.KeystoreScreen
import unidesk.com.br.keymanager.viewmodel.MainViewModel

@Composable
@Preview
fun App() {
    MaterialTheme {
        val viewModel = viewModel<MainViewModel>(factory = MainViewModel.Factory)
        KeystoreScreen(viewModel)
    }
}