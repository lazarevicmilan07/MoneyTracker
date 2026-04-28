package com.moneytracker.simplebudget.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.simplebudget.data.preferences.PreferencesManager
import com.moneytracker.simplebudget.data.repository.BudgetRepository
import com.moneytracker.simplebudget.data.repository.CategoryRepository
import com.moneytracker.simplebudget.domain.model.Budget
import com.moneytracker.simplebudget.domain.model.BudgetPeriod
import com.moneytracker.simplebudget.domain.model.BudgetWithProgress
import com.moneytracker.simplebudget.domain.model.Category
import com.moneytracker.simplebudget.domain.model.CategoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

private const val FREE_BUDGET_LIMIT = 3

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _selectedPeriodFilter = MutableStateFlow(BudgetPeriod.MONTHLY)
    val selectedPeriodFilter: StateFlow<BudgetPeriod> = _selectedPeriodFilter.asStateFlow()

    val currency: StateFlow<String> = preferencesManager.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val currencySymbolAfter: StateFlow<Boolean> = preferencesManager.currencySymbolAfter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isPremium: StateFlow<Boolean> = preferencesManager.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgets: StateFlow<List<BudgetWithProgress>> =
        combine(_selectedMonth, _selectedPeriodFilter) { month, period -> month to period }
            .flatMapLatest { (month, period) ->
                budgetRepository.getBudgetsWithProgress(month.year, month.monthValue, period)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rootExpenseCategories: StateFlow<List<Category>> = categoryRepository.getRootCategoriesByType(CategoryType.EXPENSE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectPreviousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun selectNextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun selectMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    fun selectPreviousYear() {
        _selectedMonth.value = _selectedMonth.value.minusYears(1)
    }

    fun selectNextYear() {
        _selectedMonth.value = _selectedMonth.value.plusYears(1)
    }

    fun selectPeriodFilter(period: BudgetPeriod) {
        _selectedPeriodFilter.value = period
    }

    fun canAddBudget(onBlocked: () -> Unit, onAllowed: () -> Unit) {
        viewModelScope.launch {
            val isPremiumUser = preferencesManager.isPremium.stateIn(viewModelScope).value
            val count = budgetRepository.getActiveBudgetCount()
            if (!isPremiumUser && count >= FREE_BUDGET_LIMIT) {
                onBlocked()
            } else {
                onAllowed()
            }
        }
    }

    fun saveBudget(
        categoryId: Long?,
        subcategoryId: Long?,
        amount: Double,
        period: BudgetPeriod,
        existingId: Long = 0
    ) {
        viewModelScope.launch {
            val selected = _selectedMonth.value
            val budget = Budget(
                id = existingId,
                categoryId = categoryId,
                subcategoryId = subcategoryId,
                amount = amount,
                period = period,
                year = selected.year,
                month = if (period == BudgetPeriod.MONTHLY) selected.monthValue else null
            )
            if (existingId == 0L) {
                budgetRepository.insertBudget(budget)
            } else {
                budgetRepository.updateBudget(budget)
            }
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget)
        }
    }

    suspend fun hasSubcategories(categoryId: Long): Boolean =
        categoryRepository.hasSubcategories(categoryId)
}
