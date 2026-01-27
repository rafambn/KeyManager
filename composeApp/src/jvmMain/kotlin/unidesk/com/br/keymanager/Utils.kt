package unidesk.com.br.keymanager

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun selectDestinationFile(title: String = "Select Destination"): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isVisible = true
    return if (dialog.directory != null && dialog.file != null) {
        File(dialog.directory, dialog.file)
    } else null
}