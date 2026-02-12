package com.rafambn.keymanager.ui.main

import com.rafambn.keymanager.keytool.model.KeystoreSession
import com.rafambn.keymanager.keytool.model.RecentKeystore

data class AppState(
    val keystoreSessions: Map<String, KeystoreSession> = emptyMap(),
    val selectedKeystoreId: String? = null,
    val selectedKeyAlias: String? = null,
    val recentKeystores: List<RecentKeystore> = emptyList(),
    val isGlobalLoading: Boolean = false,
    val globalError: String? = null,
    val keystoreSearchQuery: String = "",
    val keySearchQuery: String = "",
    val keystoreSortOrder: KeystoreSortOrder = KeystoreSortOrder.RECENT,
    val keyTypeFilter: KeyTypeFilter = KeyTypeFilter.ALL
) {
    val selectedSession: KeystoreSession?
        get() = selectedKeystoreId?.let { keystoreSessions[it] }

    val openKeystores: List<KeystoreSession>
        get() = keystoreSessions.values.toList()

    val filteredKeystores: List<KeystoreSession>
        get() = if (keystoreSearchQuery.isBlank()) {
            openKeystores
        } else {
            openKeystores.filter { session ->
                session.name.contains(keystoreSearchQuery, ignoreCase = true) ||
                        session.path.contains(keystoreSearchQuery, ignoreCase = true)
            }
        }

    val sortedKeystores: List<KeystoreSession>
        get() = when (keystoreSortOrder) {
            KeystoreSortOrder.RECENT -> filteredKeystores
            KeystoreSortOrder.NAME -> filteredKeystores.sortedBy { it.name.lowercase() }
            KeystoreSortOrder.PATH -> filteredKeystores.sortedBy { it.path.lowercase() }
        }
}

enum class KeystoreSortOrder {
    RECENT,
    NAME,
    PATH
}

enum class KeyTypeFilter {
    ALL,
    PRIVATE_KEY,
    TRUSTED_CERT,
    SECRET_KEY
}
