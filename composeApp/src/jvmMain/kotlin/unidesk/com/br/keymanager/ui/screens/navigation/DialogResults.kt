package unidesk.com.br.keymanager.ui.screens.navigation

import java.io.File

sealed interface DialogResult {
    data class CreateKey(val alias: String, val dn: String, val validity: Int) : DialogResult
    data class Rename(val oldAlias: String, val newAlias: String) : DialogResult
    data class Delete(val alias: String) : DialogResult
    data class Move(val alias: String, val targetFile: File, val targetPassword: String) : DialogResult
    data class BulkMove(val aliases: List<String>, val targetFile: File, val targetPassword: String) : DialogResult
}
