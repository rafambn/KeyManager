package com.rafambn.keymanager.ui.navigation

import com.rafambn.keymanager.keytool.enums.ECCurve
import com.rafambn.keymanager.keytool.enums.KeyAlgorithm
import com.rafambn.keymanager.keytool.enums.SignatureAlgorithm
import java.io.File

sealed interface DialogResult {
    data class UnlockKeystore(val sessionId: String, val password: String) : DialogResult
    data class CreateKey(
        val alias: String,
        val dn: String,
        val validity: Int,
        val keyAlgorithm: KeyAlgorithm,
        val keySize: Int?,
        val signatureAlgorithm: SignatureAlgorithm?,
        val ecCurve: ECCurve?
    ) : DialogResult
    data class Rename(val oldAlias: String, val newAlias: String) : DialogResult
    data class Delete(val alias: String) : DialogResult
    data class Move(val alias: String, val targetFile: File, val targetPassword: String) : DialogResult
    data class BulkMove(val aliases: List<String>, val targetFile: File, val targetPassword: String) : DialogResult
    data class CreateKeystore(val file: File, val password: String, val format: String) : DialogResult
    data class ChangePassword(val sessionId: String, val oldPassword: String, val newPassword: String) : DialogResult
    data class ExportCert(val alias: String, val file: File, val asPem: Boolean) : DialogResult
}
