package unidesk.com.br.keymanager.core.model

import kotlinx.serialization.Serializable

@Serializable
data class RecentKeystore(
    val path: String,
    val name: String,
    val lastOpened: Long,
    val keystoreType: String? = null
)
