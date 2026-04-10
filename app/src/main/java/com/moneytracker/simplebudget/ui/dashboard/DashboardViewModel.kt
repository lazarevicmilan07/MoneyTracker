package com.moneytracker.simplebudget.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.simplebudget.data.preferences.PreferencesManager
import com.moneytracker.simplebudget.data.repository.AccountRepository
import com.moneytracker.simplebudget.data.repository.CategoryRepository
import com.moneytracker.simplebudget.data.repository.ExpenseRepository
import com.moneytracker.simplebudget.domain.model.Category
import com.moneytracker.simplebudget.domain.model.CategoryBreakdown
import com.moneytracker.simplebudget.domain.model.ExpenseWithCategory
import com.moneytracker.simplebudget.domain.model.MonthlyStats
import com.moneytracker.simplebudget.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import com.moneytracker.simplebudget.domain.model.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class TransactionFilter(
    val transactionTypes: Set<TransactionType> = emptySet(),
    val categoryIds: Set<Long> = emptySet(),
    val subcategoryIds: Set<Long> = emptySet(),
    val accountIds: Set<Long> = emptySet(),
    val searchQuery: String = ""
) {
    val isActive: Boolean
        get() = transactionTypes.isNotEmpty() || categoryIds.isNotEmpty() ||
                subcategoryIds.isNotEmpty() || accountIds.isNotEmpty() || searchQuery.isNotBlank()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _filter = MutableStateFlow(TransactionFilter())
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    private val _selectedTransactionIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTransactionIds: StateFlow<Set<Long>> = _selectedTransactionIds.asStateFlow()

    val isMultiSelectMode: StateFlow<Boolean> = _selectedTransactionIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val filteredTransactions: StateFlow<List<ExpenseWithCategory>> = combine(_uiState, _filter) { state, filter ->
        if (!filter.isActive) {
            state.recentTransactions
        } else {
            // Which parent categories have at least one subcategory selected
            val parentIdsWithSubFilter = filter.subcategoryIds
                .mapNotNull { subId -> state.categories.find { it.id == subId }?.parentCategoryId }
                .toSet()

            state.recentTransactions.filter { txn ->
                (filter.transactionTypes.isEmpty() || txn.expense.type in filter.transactionTypes) &&
                (filter.categoryIds.isEmpty() || txn.expense.categoryId in filter.categoryIds) &&
                // Only enforce subcategory match if this transaction's category has subcategories selected;
                // categories with no subcategory selected are not restricted to a specific subcategory
                (filter.subcategoryIds.isEmpty() ||
                    txn.expense.categoryId !in parentIdsWithSubFilter ||
                    txn.expense.subcategoryId in filter.subcategoryIds) &&
                (filter.accountIds.isEmpty() || txn.expense.accountId in filter.accountIds || txn.expense.toAccountId in filter.accountIds) &&
                (filter.searchQuery.isBlank() || txn.expense.note.contains(filter.searchQuery, ignoreCase = true))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currency: StateFlow<String> = preferencesManager.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val currencySymbolAfter: StateFlow<Boolean> = preferencesManager.currencySymbolAfter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isPremium: StateFlow<Boolean> = preferencesManager.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        initializeData()
        loadData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            categoryRepository.initializeDefaultCategories()
            accountRepository.initializeDefaultAccount()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            // Combine month selection, categories, and expenses reactively
            combine(
                categoryRepository.getAllCategories(),
                accountRepository.getAllAccounts(),
                _selectedMonth.flatMapLatest { month ->
                    expenseRepository.getExpensesByMonth(month.year, month.monthValue)
                }
            ) { categories, accounts, expenses ->
                Triple(categories, accounts, expenses)
            }.collect { (categories, accounts, expenses) ->
                updateUiState(categories, accounts, expenses)
            }
        }
    }

    private fun updateUiState(categories: List<Category>, accounts: List<com.moneytracker.simplebudget.domain.model.Account>, expenses: List<com.moneytracker.simplebudget.domain.model.Expense>) {
        val categoriesMap = categories.associateBy { it.id }
        val accountsMap = accounts.associateBy { it.id }

        // Calculate totals from the current expenses list (reactive)
        val totalIncome = expenses
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val totalExpense = expenses
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        // Calculate category breakdown for expenses only
        val categoryTotals = expenses
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, expenseList) -> expenseList.sumOf { it.amount } }

        val totalCategoryExpense = categoryTotals.values.sum()
        val breakdown = categoryTotals.map { (categoryId, amount) ->
            CategoryBreakdown(
                category = categoryId?.let { categoriesMap[it] },
                amount = amount,
                percentage = if (totalCategoryExpense > 0) (amount / totalCategoryExpense * 100).toFloat() else 0f
            )
        }.sortedByDescending { it.amount }

        val expensesWithCategory = expenses.map { expense ->
            ExpenseWithCategory(
                expense = expense,
                category = expense.categoryId?.let { categoriesMap[it] },
                subcategory = expense.subcategoryId?.let { categoriesMap[it] },
                account = expense.accountId?.let { accountsMap[it] },
                toAccount = expense.toAccountId?.let { accountsMap[it] }
            )
        }

        _uiState.value = DashboardUiState(
            isLoading = false,
            monthlyStats = MonthlyStats(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = totalIncome - totalExpense,
                categoryBreakdown = breakdown
            ),
            recentTransactions = expensesWithCategory,
            categories = categories,
            accounts = accounts
        )
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

    fun toggleTransactionTypeFilter(type: TransactionType?) {
        _filter.update { current ->
            if (type == null) current.copy(transactionTypes = emptySet())
            else {
                val updated = if (type in current.transactionTypes) current.transactionTypes - type
                              else current.transactionTypes + type
                current.copy(transactionTypes = updated)
            }
        }
    }

    fun toggleCategoryFilter(categoryId: Long) {
        val allCategories = _uiState.value.categories
        _filter.update { current ->
            if (categoryId in current.categoryIds) {
                val childIds = allCategories.filter { it.parentCategoryId == categoryId }.map { it.id }.toSet()
                current.copy(
                    categoryIds = current.categoryIds - categoryId,
                    subcategoryIds = current.subcategoryIds - childIds
                )
            } else {
                current.copy(categoryIds = current.categoryIds + categoryId)
            }
        }
    }

    fun toggleSubcategoryFilter(subcategoryId: Long) {
        _filter.update { current ->
            val updated = if (subcategoryId in current.subcategoryIds) current.subcategoryIds - subcategoryId
                          else current.subcategoryIds + subcategoryId
            current.copy(subcategoryIds = updated)
        }
    }

    fun toggleAccountFilter(accountId: Long) {
        _filter.update { current ->
            val updated = if (accountId in current.accountIds) current.accountIds - accountId
                          else current.accountIds + accountId
            current.copy(accountIds = updated)
        }
    }

    fun setSearchQuery(query: String) {
        _filter.update { it.copy(searchQuery = query) }
    }

    fun clearFilters() {
        _filter.value = TransactionFilter()
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            expenseRepository.deleteExpenseById(expenseId)
        }
    }

    // --- Multiselect ---

    fun toggleTransactionSelection(id: Long) {
        _selectedTransactionIds.update { ids -> if (id in ids) ids - id else ids + id }
    }

    fun clearSelection() {
        _selectedTransactionIds.value = emptySet()
    }

    fun deleteSelectedTransactions() {
        viewModelScope.launch {
            _selectedTransactionIds.value.toList().forEach { id ->
                expenseRepository.deleteExpenseById(id)
            }
            clearSelection()
        }
    }

    fun bulkUpdateDate(date: LocalDate) {
        viewModelScope.launch {
            _selectedTransactionIds.value.toList().forEach { id ->
                val expense = expenseRepository.getExpenseById(id) ?: return@forEach
                expenseRepository.updateExpense(expense.copy(date = date))
            }
            clearSelection()
        }
    }

    fun bulkUpdateCategory(categoryId: Long?, subcategoryId: Long?) {
        viewModelScope.launch {
            _selectedTransactionIds.value.toList().forEach { id ->
                val expense = expenseRepository.getExpenseById(id) ?: return@forEach
                expenseRepository.updateExpense(expense.copy(categoryId = categoryId, subcategoryId = subcategoryId))
            }
            clearSelection()
        }
    }

    fun bulkUpdateAccount(accountId: Long?) {
        viewModelScope.launch {
            _selectedTransactionIds.value.toList().forEach { id ->
                val expense = expenseRepository.getExpenseById(id) ?: return@forEach
                expenseRepository.updateExpense(expense.copy(accountId = accountId))
            }
            clearSelection()
        }
    }

    fun bulkUpdateNote(note: String) {
        viewModelScope.launch {
            _selectedTransactionIds.value.toList().forEach { id ->
                val expense = expenseRepository.getExpenseById(id) ?: return@forEach
                expenseRepository.updateExpense(expense.copy(note = note))
            }
            clearSelection()
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val monthlyStats: MonthlyStats = MonthlyStats(0.0, 0.0, 0.0, emptyList()),
    val recentTransactions: List<ExpenseWithCategory> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList()
)
