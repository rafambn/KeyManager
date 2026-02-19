package com.rafambn.keymanager.keytool.model

data class TlsInfo(
    val enabledProtocols: List<String>,
    val enabledCipherSuites: List<String>
)
