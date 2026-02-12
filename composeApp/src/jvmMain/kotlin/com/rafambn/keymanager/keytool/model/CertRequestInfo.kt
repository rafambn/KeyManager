package com.rafambn.keymanager.keytool.model

data class CertRequestInfo(
    val subject: String,
    val algorithm: String,
    val extensions: List<String>
)
