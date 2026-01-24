package unidesk.com.br.keymanager.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ThreePaneLayout(
    leftPane: @Composable () -> Unit,
    middlePane: @Composable () -> Unit,
    rightPane: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        // Left Pane - Fixed width 240dp
        Box(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            leftPane()
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Middle Pane - Fixed width 320dp
        Box(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            middlePane()
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Right Pane - Expands to fill remaining space
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            rightPane()
        }
    }
}
