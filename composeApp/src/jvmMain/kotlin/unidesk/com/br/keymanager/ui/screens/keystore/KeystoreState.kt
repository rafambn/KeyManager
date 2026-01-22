package unidesk.com.br.keymanager.ui.screens.keystore

import unidesk.com.br.keymanager.core.KeyInfo
import java.io.File

data class KeystoreState(
    val aliases: List<KeyInfo> = emptyList(),
    val currentFile: File? = null,
    val errorMessage: String? = null,
    val isKeystoreLoaded: Boolean = false,
    val isLoading: Boolean = false
)
