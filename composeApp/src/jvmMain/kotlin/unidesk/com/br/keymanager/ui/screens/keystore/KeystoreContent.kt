package unidesk.com.br.keymanager.ui.screens.keystore

fun selectDestinationFile(title: String = "Select Destination"): java.io.File? {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, title, java.awt.FileDialog.LOAD)
    dialog.isVisible = true
    return if (dialog.directory != null && dialog.file != null) {
        java.io.File(dialog.directory, dialog.file)
    } else null
}