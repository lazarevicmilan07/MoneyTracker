package com.moneytracker.simplebudget.domain.usecase

import com.moneytracker.simplebudget.data.preferences.PreferencesManager
import com.moneytracker.simplebudget.data.remote.ExchangeRateRepository
import com.moneytracker.simplebudget.data.repository.ExpenseRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyConversionUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val preferencesManager: PreferencesManager
) {

    suspend fun getTransactionCount(): Int =
        expenseRepository.getExpenseCount()

    suspend fun fetchRate(from: String, to: String): Result<Double> =
        exchangeRateRepository.fetchRate(from, to)

    suspend fun convertAllAmounts(rate: Double): Result<Unit> =
        runCatching { expenseRepository.multiplyAllAmounts(rate) }

    suspend fun applyNewCurrency(currency: String) =
        preferencesManager.setCurrency(currency)
}
