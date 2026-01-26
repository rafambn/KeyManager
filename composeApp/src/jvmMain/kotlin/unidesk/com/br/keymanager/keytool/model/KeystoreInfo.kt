package unidesk.com.br.keymanager.keytool.model

data class KeystoreInfo(
    val type: String,
    val provider: String,
    val entryCount: Int,
    val entries: List<KeystoreEntry>
)
