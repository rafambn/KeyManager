package unidesk.com.br.keymanager.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import unidesk.com.br.keymanager.core.domain.EntryType
import unidesk.com.br.keymanager.core.model.KeyInfo
import unidesk.com.br.keymanager.ui.components.common.ValidityBadge
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun KeyDetailsDialog(
    keyInfo: KeyInfo,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(500.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Key Details",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(24.dp))

                // Alias
                DetailRow(label = "Alias", value = keyInfo.alias)

                // Type
                val typeLabel = when (keyInfo.entryType) {
                    EntryType.PRIVATE_KEY -> "Private Key Entry"
                    EntryType.TRUSTED_CERT -> "Trusted Certificate Entry"
                    EntryType.SECRET_KEY -> "Secret Key Entry"
                    EntryType.UNKNOWN -> "Unknown Entry Type"
                }
                DetailRow(label = "Entry Type", value = typeLabel)

                // Algorithm
                DetailRow(label = "Algorithm", value = keyInfo.algorithm)

                // Certificate Chain
                keyInfo.certificateChainLength?.let {
                    DetailRow(label = "Certificate Chain", value = "$it certificate(s)")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Owner
                keyInfo.owner?.let {
                    DetailRow(label = "Owner", value = it)
                }

                // Issuer
                keyInfo.issuer?.let {
                    DetailRow(label = "Issuer", value = it)
                }

                // Serial Number
                keyInfo.serialNumber?.let {
                    DetailRow(label = "Serial Number", value = it)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Validity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Validity",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ValidityBadge(validUntil = keyInfo.validUntil)
                }

                Spacer(Modifier.height(8.dp))

                keyInfo.validFrom?.let {
                    DetailRow(label = "Valid From", value = it)
                }

                keyInfo.validUntil?.let {
                    DetailRow(label = "Valid Until", value = it)
                }

                // Creation Date
                keyInfo.creationDate?.let {
                    DetailRow(label = "Created", value = it)
                }

                // Fingerprint
                keyInfo.fingerprint?.let { fingerprint ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fingerprint (SHA-256)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = fingerprint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                clipboard.setContents(StringSelection(fingerprint), null)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy fingerprint",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
