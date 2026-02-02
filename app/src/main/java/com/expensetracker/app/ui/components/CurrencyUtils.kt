package com.expensetracker.app.ui.components

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Formats a currency amount using the locale that naturally owns the currency,
 * so USD shows "$" instead of "US$", GBP shows "£" instead of "GB£", etc.
 */
fun formatCurrency(amount: Double, currencyCode: String): String {
    return try {
        val currency = Currency.getInstance(currencyCode)
        val locale = findLocaleForCurrency(currencyCode)
        val format = NumberFormat.getCurrencyInstance(locale)
        format.currency = currency
        format.format(amount)
    } catch (e: Exception) {
        val symbol = getCurrencySymbol(currencyCode)
        "$symbol${String.format("%.2f", amount)}"
    }
}

/**
 * Returns the currency symbol (e.g. "$", "€", "£") for a given currency code,
 * using the currency's native locale so it doesn't get country-prefixed.
 */
fun getCurrencySymbol(currencyCode: String): String {
    return try {
        val locale = findLocaleForCurrency(currencyCode)
        Currency.getInstance(currencyCode).getSymbol(locale)
    } catch (e: Exception) {
        currencyCode
    }
}

private fun findLocaleForCurrency(currencyCode: String): Locale {
    // Find a locale whose default currency matches, so formatting is native
    return Locale.getAvailableLocales().firstOrNull { locale ->
        try {
            Currency.getInstance(locale)?.currencyCode == currencyCode
        } catch (e: Exception) {
            false
        }
    } ?: Locale.getDefault()
}
