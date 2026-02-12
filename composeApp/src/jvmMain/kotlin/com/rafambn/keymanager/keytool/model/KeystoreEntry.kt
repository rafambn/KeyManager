package com.rafambn.keymanager.keytool.model

import com.rafambn.keymanager.keytool.enums.EntryType

data class KeystoreEntry(
    val alias: String,
    val creationDate: String,
    val entryType: EntryType,
    val certificateChainLength: Int?,
    val owner: String?,
    val issuer: String?,
    val algorithm: String?,
    val serialNumber: String?,
    val validFrom: String?,
    val validUntil: String?,
    val fingerprint: String?
)
