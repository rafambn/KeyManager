package unidesk.com.br.keymanager.core.model

data class CertificateInfo(
    val owner: String,
    val issuer: String,
    val serialNumber: String,
    val validFrom: String,
    val validUntil: String,
    val algorithm: String,
    val fingerprints: Map<String, String>
)
