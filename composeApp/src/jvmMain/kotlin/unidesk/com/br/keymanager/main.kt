package unidesk.com.br.keymanager

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import keymanager.composeapp.generated.resources.*

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Keystore Manager",
        icon = painterResource(Res.drawable.app_icon)
    ) {
        App()
    }
}