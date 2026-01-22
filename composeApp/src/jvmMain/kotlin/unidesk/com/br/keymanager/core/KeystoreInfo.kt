package unidesk.com.br.keymanager.core

// Keystore listing result
data class KeystoreInfo(
    val type: String,
    val provider: String,
    val entryCount: Int,
    val entries: List<KeystoreEntry>
)
