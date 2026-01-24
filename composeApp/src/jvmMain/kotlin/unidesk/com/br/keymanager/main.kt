package unidesk.com.br.keymanager

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.compose.resources.painterResource
import keymanager.composeapp.generated.resources.*
import java.awt.Dimension

fun main() = application {
    // Load and set locale BEFORE any Compose resources are accessed
    val prefs = java.util.prefs.Preferences.userRoot().node("keymanager")
    val savedLanguage = prefs.get("selected_language", "pt-BR")

    val (lang, country) = if (savedLanguage.contains("-")) {
        savedLanguage.split("-")
    } else {
        listOf(savedLanguage, "")
    }

    System.setProperty("user.language", lang)
    if (country.isNotEmpty()) {
        System.setProperty("user.country", country)
    }

    val windowState = rememberWindowState(width = 1200.dp, height = 600.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Keystore Manager",
        icon = painterResource(Res.drawable.app_icon),
        state = windowState
    ) {
        LaunchedEffect(Unit) {
            val window = java.awt.Window.getWindows().lastOrNull()
            window?.minimumSize = Dimension(1200, 600)
        }

        App()
    }
}