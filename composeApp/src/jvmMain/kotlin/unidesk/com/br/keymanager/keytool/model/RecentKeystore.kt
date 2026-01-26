package unidesk.com.br.keymanager.keytool.model

import kotlinx.serialization.Serializable

@Serializable
data class RecentKeystore(
    val path: String,
    val name: String,
    val lastOpened: Long,
    val keystoreType: String? = null
)
