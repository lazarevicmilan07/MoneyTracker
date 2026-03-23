package com.moneytracker.simplebudget.data.preferences

import android.content.Context

object LanguagePreferences {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "language_code"
    private const val DEFAULT = "en"

    data class AppLanguage(val code: String, val nativeName: String)

    val supportedLanguages = listOf(
        AppLanguage("en", "English"),
        AppLanguage("es", "Español"),
        AppLanguage("pt", "Português"),
        AppLanguage("de", "Deutsch"),
        AppLanguage("fr", "Français"),
        AppLanguage("ru", "Русский"),
        AppLanguage("sr", "Srpski")
    )

    fun getLanguage(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, DEFAULT) ?: DEFAULT

    fun setLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }
}
