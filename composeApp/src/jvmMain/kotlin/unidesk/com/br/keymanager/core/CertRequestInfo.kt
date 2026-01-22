package unidesk.com.br.keymanager.core

// CSR info (for printCertReq)
data class CertRequestInfo(
    val subject: String,
    val algorithm: String,
    val extensions: List<String>
)
