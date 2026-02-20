package com.rafambn.keymanager.ui.main.panes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rafambn.keymanager.BuildConfig
import keymanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun NavigationPane(
    onCreateKeystore: () -> Unit,
    onOpenKeystore: () -> Unit,
    onBulkMove: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appTitle = stringResource(Res.string.app_title)
    val createKeystoreLabel = stringResource(Res.string.nav_create_keystore)
    val openKeystoreLabel = stringResource(Res.string.nav_open_keystore)
    val bulkMoveLabel = stringResource(Res.string.nav_bulk_move)
    val settingsLabel = stringResource(Res.string.nav_settings)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = appTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(24.dp))


            Button(
                onClick = onCreateKeystore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(createKeystoreLabel)
            }

            Spacer(Modifier.height(8.dp))

            FilledTonalButton(
                onClick = onOpenKeystore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(openKeystoreLabel)
            }

            Spacer(Modifier.height(8.dp))

            FilledTonalButton(
                onClick = onBulkMove,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(bulkMoveLabel)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))


            TextButton(
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(settingsLabel)
            }
        }


        Text(
            text = "v${BuildConfig.APP_VERSION}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
