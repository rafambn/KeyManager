package unidesk.com.br.keymanager.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.*

object LocalAppLocale {
    private var defaultLocale: Locale? = null
    private val LocalLocale = staticCompositionLocalOf { Locale.getDefault().toString() }

    val current: String
        @Composable
        get() = LocalLocale.current

    @Composable
    infix fun provides(value: String?): ProvidedValue<String> {
        if (defaultLocale == null) {
            defaultLocale = Locale.getDefault()
        }

        val newLocale = when {
            value == null -> defaultLocale!!
            value.contains("-") -> {
                val (lang, country) = value.split("-")
                Locale.Builder().setLanguage(lang).setRegion(country).build()
            }
            else -> Locale.Builder().setLanguage(value).build()
        }
        Locale.setDefault(newLocale)
        return LocalLocale provides newLocale.toString()
    }
}