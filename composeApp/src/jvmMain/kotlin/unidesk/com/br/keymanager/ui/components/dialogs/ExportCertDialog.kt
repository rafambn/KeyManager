package unidesk.com.br.keymanager.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun ExportCertDialog(
    alias: String,
    onDismiss: () -> Unit,
    onExport: (File, Boolean) -> Unit
) {
    var filePath by remember { mutableStateOf("") }
    var asPem by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(400.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Export Certificate",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Alias: $alias",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))

                // Format Selection
                Text(
                    text = "Format",
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = asPem,
                        onClick = { asPem = true }
                    )
                    Text(
                        text = "PEM (Base64 encoded)",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !asPem,
                        onClick = { asPem = false }
                    )
                    Text(
                        text = "DER (Binary)",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // File Path
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = filePath,
                        onValueChange = { filePath = it },
                        label = { Text("Save to") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            val dialog = FileDialog(null as Frame?, "Save Certificate", FileDialog.SAVE)
                            val extension = if (asPem) ".pem" else ".der"
                            dialog.file = "$alias$extension"
                            dialog.isVisible = true
                            if (dialog.directory != null && dialog.file != null) {
                                var selectedFile = dialog.file
                                // Ensure correct extension
                                if (!selectedFile.endsWith(extension)) {
                                    selectedFile = if (selectedFile.contains(".")) {
                                        selectedFile.substringBeforeLast(".") + extension
                                    } else {
                                        selectedFile + extension
                                    }
                                }
                                filePath = File(dialog.directory, selectedFile).absolutePath
                            }
                        }
                    ) {
                        Text("Browse")
                    }
                }

                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            when {
                                filePath.isBlank() -> error = "Please select a file path"
                                else -> onExport(File(filePath), asPem)
                            }
                        },
                        enabled = filePath.isNotBlank()
                    ) {
                        Text("Export")
                    }
                }
            }
        }
    }
}
