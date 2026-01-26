package unidesk.com.br.keymanager.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

object LocalAppLocale {
    private var defaultLocale: Locale? = null

    val current: String
        @Composable
        get() = Locale.getDefault().language

    @Composable
    infix fun provides(value: String?): ProvidedValue<*> {
        val newLocale = when {
            value == null -> Locale.getDefault()
            value.contains("-") -> {
                val (lang, country) = value.split("-")
                Locale(lang, country)
            }

            else -> Locale(value)
        }
        Locale.setDefault(newLocale)
        System.setProperty("user.language", newLocale.language)
        System.setProperty("user.country", newLocale.country)

        return compositionLocalOf { "en" } provides newLocale.language
    }
}