package com.expensetracker.app.ui.components

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

private fun getAppDecimalSymbols(): DecimalFormatSymbols {
    val symbols = DecimalFormatSymbols(Locale.getDefault())
    symbols.groupingSeparator = '.'
    symbols.decimalSeparator = ','
    return symbols
}

/**
 * Formats a currency amount with the currency symbol.
 * Always uses dot for thousands and comma for decimals (e.g. 150.000,00).
 */
fun formatCurrency(amount: Double, currencyCode: String): String {
    return try {
        val symbol = getCurrencySymbol(currencyCode)
        val symbols = getAppDecimalSymbols()
        val format = DecimalFormat("#,##0.00", symbols)
        "$symbol${format.format(amount)}"
    } catch (e: Exception) {
        val symbol = getCurrencySymbol(currencyCode)
        "$symbol${String.format("%.2f", amount)}"
    }
}

/**
 * Formats a number without the currency symbol.
 * Always uses dot for thousands and comma for decimals (e.g. 150.000,00).
 */
fun formatNumber(amount: Double, currencyCode: String): String {
    return try {
        val symbols = getAppDecimalSymbols()
        val format = DecimalFormat("#,##0.00", symbols)
        format.format(amount)
    } catch (e: Exception) {
        String.format("%.2f", amount)
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

/**
 * Formats a raw amount input string with proper separators while preserving
 * the exact digits the user has typed (no rounding or trailing zero addition).
 * Uses dot for thousands and comma for decimals (e.g. "200000.00" -> "200.000,00").
 */
fun formatAmountInput(raw: String): String {
    if (raw.isEmpty()) return "0"

    val negative = raw.startsWith("-")
    val abs = if (negative) raw.substring(1) else raw

    val parts = abs.split(".")
    val intPart = parts[0].ifEmpty { "0" }
    val decPart = if (parts.size > 1) parts[1] else null

    // Format integer part with dot as thousands separator
    val formatted = intPart.reversed().chunked(3).joinToString(".").reversed()

    val result = if (decPart != null) {
        "$formatted,$decPart"
    } else {
        formatted
    }

    return if (negative) "-$result" else result
}
