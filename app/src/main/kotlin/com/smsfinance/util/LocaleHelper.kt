package com.smsfinance.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Helper to apply locale changes at runtime.
 * Call wrap() in Application.attachBaseContext() and Activity.attachBaseContext().
 */
object LocaleHelper {

    const val LANG_ENGLISH = "en"
    const val LANG_SWAHILI = "sw"

    /**
     * Wrap context with the selected locale.
     */
    fun wrap(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    /**
     * Apply locale to a context using saved preference.
     */
    fun applyLocale(context: Context, language: String): Context {
        return wrap(context, language)
    }
}
