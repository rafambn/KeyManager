package unidesk.com.br.keymanager

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.compose.resources.painterResource
import keymanager.composeapp.generated.resources.*

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Keystore Manager",
        icon = painterResource(Res.drawable.app_icon),
        state = rememberWindowState(
            width = 1200.dp,
            height = 500.dp
        )
    ) {
        App()
    }
}