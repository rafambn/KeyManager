package unidesk.com.br.keymanager.ui.screens.keystore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import unidesk.com.br.keymanager.core.repository.KeystoreRepository
@Deprecated("Use AppViewModel instead", level = DeprecationLevel.WARNING)
class KeystoreViewModel(
    private val repository: KeystoreRepository
) : ViewModel() {
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = KeystoreRepository()
                KeystoreViewModel(repository = repository)
            }
        }
    }
}
