package unidesk.com.br.keymanager.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

object EmptyStateDefaults {
    val NoKeystores = EmptyStateConfig(
        icon = Icons.Default.Folder,
        title = "No keystores open",
        description = "Open or create a keystore to get started"
    )

    val NoKeys = EmptyStateConfig(
        icon = Icons.Default.Key,
        title = "No keys in keystore",
        description = "Create a new key or import certificates"
    )

    val KeystoreLocked = EmptyStateConfig(
        icon = Icons.Default.Lock,
        title = "Keystore locked",
        description = "Enter the password to unlock"
    )

    val NoSelection = EmptyStateConfig(
        icon = Icons.Default.Folder,
        title = "No keystore selected",
        description = "Select a keystore from the list to view its contents"
    )
}

data class EmptyStateConfig(
    val icon: ImageVector,
    val title: String,
    val description: String
)
