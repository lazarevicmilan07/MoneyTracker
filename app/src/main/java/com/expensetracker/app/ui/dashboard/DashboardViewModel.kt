package com.expensetracker.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.entity.TransactionType
import com.expensetracker.app.data.preferences.PreferencesManager
import com.expensetracker.app.data.repository.CategoryRepository
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.CategoryBreakdown
import com.expensetracker.app.domain.model.ExpenseWithCategory
import com.expensetracker.app.domain.model.MonthlyStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val currency: StateFlow<String> = preferencesManager.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val isPremium: StateFlow<Boolean> = preferencesManager.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        initializeCategories()
        loadData()
    }

    private fun initializeCategories() {
        viewModelScope.launch {
            categoryRepository.initializeDefaultCategories()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                _selectedMonth,
                categoryRepository.getAllCategories()
            ) { month, categories ->
                Pair(month, categories)
            }.collect { (month, categories) ->
                loadMonthData(month, categories)
            }
        }
    }

    private suspend fun loadMonthData(month: YearMonth, categories: List<Category>) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        val categoriesMap = categories.associateBy { it.id }
        val income = expenseRepository.getTotalIncomeByMonth(month.year, month.monthValue)
        val expense = expenseRepository.getTotalExpenseByMonth(month.year, month.monthValue)
        val categoryTotals = expenseRepository.getCategoryTotalsByMonth(month.year, month.monthValue)

        val totalExpense = categoryTotals.values.sum()
        val breakdown = categoryTotals.map { (categoryId, amount) ->
            CategoryBreakdown(
                category = categoryId?.let { categoriesMap[it] },
                amount = amount,
                percentage = if (totalExpense > 0) (amount / totalExpense * 100).toFloat() else 0f
            )
        }.sortedByDescending { it.amount }

        expenseRepository.getExpensesByMonth(month.year, month.monthValue).collect { expenses ->
            val expensesWithCategory = expenses.map { expense ->
                ExpenseWithCategory(
                    expense = expense,
                    category = expense.categoryId?.let { categoriesMap[it] }
                )
            }

            _uiState.value = DashboardUiState(
                isLoading = false,
                monthlyStats = MonthlyStats(
                    totalIncome = income,
                    totalExpense = expense,
                    balance = income - expense,
                    categoryBreakdown = breakdown
                ),
                recentTransactions = expensesWithCategory.take(10),
                categories = categories
            )
        }
    }

    fun selectMonth(yearMonth: YearMonth) {
        _selectedMonth.value = yearMonth
    }

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            expenseRepository.deleteExpenseById(expenseId)
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val monthlyStats: MonthlyStats = MonthlyStats(0.0, 0.0, 0.0, emptyList()),
    val recentTransactions: List<ExpenseWithCategory> = emptyList(),
    val categories: List<Category> = emptyList()
)
