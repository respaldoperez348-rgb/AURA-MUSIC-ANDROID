package com.auramusic.app.i18n

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import com.auramusic.app.data.models.AppLanguageOption
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YouTubeLocale
import java.util.Locale

/**
 * Gestiona el idioma de la interfaz de AURA Music y sincroniza la región/idioma
 * del catálogo de YouTube Music (`YouTube.locale`).
 */
object AppLanguage {

    fun systemLanguageCode(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.systemLocales?.get(0)
        } else {
            @Suppress("DEPRECATION")
            Resources.getSystem().configuration.locales.get(0)
        } ?: Locale.getDefault()
        return locale.language.lowercase()
    }

    fun resolvedCode(context: Context, option: AppLanguageOption): String {
        return if (option == AppLanguageOption.SYSTEM) {
            val sys = systemLanguageCode(context)
            when {
                sys.startsWith("es") -> "es"
                sys.startsWith("pt") -> "pt"
                sys.startsWith("fr") -> "fr"
                else -> "en"
            }
        } else {
            option.tag
        }
    }

    fun applyLanguage(context: Context, option: AppLanguageOption, activity: Activity? = null) {
        val code = resolvedCode(context, option)
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)

        YouTube.locale = YouTubeLocale(
            gl = when (code) {
                "es" -> "ES"
                "pt" -> "BR"
                "fr" -> "FR"
                else -> "US"
            },
            hl = code
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val lm = context.getSystemService(LocaleManager::class.java)
                val targetList = if (option == AppLanguageOption.SYSTEM) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(code)
                }
                if (lm.applicationLocales != targetList) {
                    lm.applicationLocales = targetList
                }
            }
        } else {
            runCatching {
                @Suppress("DEPRECATION")
                val res = context.resources
                @Suppress("DEPRECATION")
                val config = Configuration(res.configuration).apply {
                    setLocale(locale)
                }
                @Suppress("DEPRECATION")
                res.updateConfiguration(config, res.displayMetrics)
                activity?.let { act ->
                    @Suppress("DEPRECATION")
                    act.resources.updateConfiguration(config, act.resources.displayMetrics)
                }
            }
        }
    }
}
