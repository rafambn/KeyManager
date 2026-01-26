package unidesk.com.br.keymanager.core.model
data class CertRequestInfo(
    val subject: String,
    val algorithm: String,
    val extensions: List<String>
)
