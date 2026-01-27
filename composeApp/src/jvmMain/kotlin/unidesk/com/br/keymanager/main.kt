package unidesk.com.br.keymanager

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import keymanager.composeapp.generated.resources.Res
import keymanager.composeapp.generated.resources.app_icon
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension
import java.awt.Window

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Keystore Manager",
        icon = painterResource(Res.drawable.app_icon),
        state = rememberWindowState(width = 1200.dp, height = 600.dp)
    ) {
        LaunchedEffect(Unit) {
            val window = Window.getWindows().lastOrNull()
            window?.minimumSize = Dimension(1200, 600)
        }
        App()
    }
}