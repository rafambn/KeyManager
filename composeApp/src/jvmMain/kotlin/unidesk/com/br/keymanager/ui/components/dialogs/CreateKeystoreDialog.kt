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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateKeystoreDialog(
    onDismiss: () -> Unit,
    onCreateKeystore: (File, String, String) -> Unit
) {
    var filePath by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("PKCS12") }
    var formatExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val formats = listOf("PKCS12", "JKS")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(400.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Create New Keystore",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(24.dp))

                // File Path
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = filePath,
                        onValueChange = { filePath = it },
                        label = { Text("File Path") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            val dialog = FileDialog(null as Frame?, "Save Keystore", FileDialog.SAVE)
                            dialog.file = if (format == "PKCS12") "keystore.p12" else "keystore.jks"
                            dialog.isVisible = true
                            if (dialog.directory != null && dialog.file != null) {
                                filePath = File(dialog.directory, dialog.file).absolutePath
                            }
                        }
                    ) {
                        Text("Browse")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Format Dropdown
                ExposedDropdownMenuBox(
                    expanded = formatExpanded,
                    onExpandedChange = { formatExpanded = it }
                ) {
                    OutlinedTextField(
                        value = format,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Format") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = formatExpanded,
                        onDismissRequest = { formatExpanded = false }
                    ) {
                        formats.forEach { f ->
                            DropdownMenuItem(
                                text = { Text(f) },
                                onClick = {
                                    format = f
                                    formatExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                // Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = confirmPassword.isNotEmpty() && password != confirmPassword
                )

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
                                password.isBlank() -> error = "Password is required"
                                password.length < 6 -> error = "Password must be at least 6 characters"
                                password != confirmPassword -> error = "Passwords do not match"
                                else -> {
                                    onCreateKeystore(File(filePath), password, format)
                                }
                            }
                        },
                        enabled = filePath.isNotBlank() && password.isNotBlank() && password == confirmPassword
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}
