package unidesk.com.br.keymanager.core

// CRL info (for printCrl)
data class CrlInfo(
    val issuer: String,
    val thisUpdate: String,
    val nextUpdate: String?,
    val revokedCertificates: List<String>
)
