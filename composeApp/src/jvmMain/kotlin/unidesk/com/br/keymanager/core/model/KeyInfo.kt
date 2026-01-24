package unidesk.com.br.keymanager.core.model

import unidesk.com.br.keymanager.core.domain.EntryType

data class KeyInfo(
    val alias: String,
    val algorithm: String,
    val entryType: EntryType,
    val details: String,
    val creationDate: String? = null,
    val validFrom: String? = null,
    val validUntil: String? = null,
    val fingerprint: String? = null,
    val owner: String? = null,
    val issuer: String? = null,
    val certificateChainLength: Int? = null,
    val serialNumber: String? = null
) {
    // Legacy compatibility property
    val type: String get() = when (entryType) {
        EntryType.PRIVATE_KEY -> "type_key"
        EntryType.TRUSTED_CERT -> "type_certificate"
        EntryType.SECRET_KEY -> "type_secret"
        EntryType.UNKNOWN -> "type_unknown"
    }
}
