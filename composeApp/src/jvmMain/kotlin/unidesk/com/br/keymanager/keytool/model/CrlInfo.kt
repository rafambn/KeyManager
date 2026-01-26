package unidesk.com.br.keymanager.keytool.model

data class CrlInfo(
    val issuer: String,
    val thisUpdate: String,
    val nextUpdate: String?,
    val revokedCertificates: List<String>
)
