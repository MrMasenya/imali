package za.ac.imali.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun apply(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            "zu" -> Locale.forLanguageTag("zu")
            "af" -> Locale.forLanguageTag("af")
            else -> Locale.ENGLISH
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
