package com.expensetracker.app.domain.model

import com.expensetracker.app.data.local.entity.TransactionType
import java.time.LocalDate

data class Expense(
    val id: Long = 0,
    val amount: Double,
    val note: String,
    val categoryId: Long?,
    val type: TransactionType,
    val date: LocalDate,
    val createdAt: Long = System.currentTimeMillis()
)

data class ExpenseWithCategory(
    val expense: Expense,
    val category: Category?
)

data class MonthlyStats(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val categoryBreakdown: List<CategoryBreakdown>
)

data class CategoryBreakdown(
    val category: Category?,
    val amount: Double,
    val percentage: Float
)
