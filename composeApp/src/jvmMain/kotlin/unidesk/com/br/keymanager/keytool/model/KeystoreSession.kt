package unidesk.com.br.keymanager.keytool.model

import java.io.File
import java.util.*

data class KeystoreSession(
    val id: String = UUID.randomUUID().toString(),
    val file: File,
    val keystoreInfo: KeystoreInfo? = null,
    val storePassword: String? = null,
    val keyPasswords: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isUnlocked: Boolean get() = storePassword != null
    val name: String get() = file.name
    val path: String get() = file.absolutePath
}
