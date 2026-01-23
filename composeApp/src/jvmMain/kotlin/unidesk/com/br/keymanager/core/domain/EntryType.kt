package unidesk.com.br.keymanager.core.domain

// Entry types in a keystore
enum class EntryType {
    PRIVATE_KEY,      // PrivateKeyEntry
    TRUSTED_CERT,     // trustedCertEntry
    SECRET_KEY,       // SecretKeyEntry
    UNKNOWN
}
