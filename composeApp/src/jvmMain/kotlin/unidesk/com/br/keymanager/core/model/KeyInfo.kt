package unidesk.com.br.keymanager.core.model

data class KeyInfo(
    val alias: String,
    val algorithm: String, // e.g., RSA, EC
    val type: String, // e.g., "PrivateKeyEntry", "TrustedCertificateEntry"
    val details: String // e.g., "CN=..., O=..."
)
