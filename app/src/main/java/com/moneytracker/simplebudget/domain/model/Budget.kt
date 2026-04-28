package com.moneytracker.simplebudget.domain.model

enum class BudgetPeriod { MONTHLY, YEARLY }

enum class BudgetScope {
    THIS_PERIOD_ONLY,
    THIS_AND_3_BEFORE,
    THIS_AND_6_BEFORE,
    THIS_AND_ALL_BEFORE,
    ALL_PERIODS,
    THIS_AND_3_FUTURE,
    THIS_AND_6_FUTURE,
    THIS_AND_FUTURE
}

data class Budget(
    val id: Long = 0,
    val categoryId: Long?,
    val subcategoryId: Long? = null,
    val amount: Double,
    val period: BudgetPeriod,
    val year: Int,
    val month: Int? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val groupId: String? = null
)

data class BudgetWithProgress(
    val budget: Budget,
    val category: Category?,
    val subcategory: Category? = null,
    val spent: Double,
    val remaining: Double,
    val percentage: Float
)
