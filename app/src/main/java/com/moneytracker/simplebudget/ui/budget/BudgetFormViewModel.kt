package com.moneytracker.simplebudget.ui.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.simplebudget.R
import com.moneytracker.simplebudget.data.preferences.PreferencesManager
import com.moneytracker.simplebudget.data.repository.BudgetRepository
import com.moneytracker.simplebudget.data.repository.CategoryRepository
import com.moneytracker.simplebudget.domain.model.Budget
import com.moneytracker.simplebudget.domain.model.BudgetPeriod
import com.moneytracker.simplebudget.domain.model.BudgetScope
import com.moneytracker.simplebudget.domain.model.Category
import com.moneytracker.simplebudget.domain.model.CategoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject

enum class BudgetFormField { NONE, AMOUNT, CATEGORY, SCOPE }

private const val FREE_BUDGET_LIMIT = 3

sealed class BudgetFormEvent {
    data object Saved : BudgetFormEvent()
    data object SavedAndContinue : BudgetFormEvent()
    data object Deleted : BudgetFormEvent()
    data class PremiumRequired(val existingBudgets: List<Budget>) : BudgetFormEvent()
    data class ValidationError(val messageResId: Int) : BudgetFormEvent()
}

data class BudgetFormUiState(
    val amountText: String = "",
    val currentField: BudgetFormField = BudgetFormField.NONE,
    val selectedScope: BudgetScope = BudgetScope.THIS_PERIOD_ONLY,
    val pendingConflicts: List<Budget>? = null,
    val selectedParentCategoryId: Long? = null,
    val selectedSubcategoryId: Long? = null,
    val isOverallBudget: Boolean = false
)

@HiltViewModel
class BudgetFormViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val budgetId: Long = savedStateHandle.get<Long>("budgetId") ?: 0L
    val initialYear: Int = savedStateHandle.get<Int>("year") ?: YearMonth.now().year
    val initialMonth: Int = savedStateHandle.get<Int>("month") ?: YearMonth.now().monthValue
    val initialPeriod: BudgetPeriod = BudgetPeriod.valueOf(
        savedStateHandle.get<String>("period") ?: BudgetPeriod.MONTHLY.name
    )

    private val _existingBudget = MutableStateFlow<Budget?>(null)
    val existingBudget: StateFlow<Budget?> = _existingBudget.asStateFlow()

    private val _uiState = MutableStateFlow(BudgetFormUiState())
    val uiState: StateFlow<BudgetFormUiState> = _uiState.asStateFlow()

    private val _events = Channel<BudgetFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val rootCategories: StateFlow<List<Category>> =
        categoryRepository.getRootCategoriesByType(CategoryType.EXPENSE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> =
        categoryRepository.getAllCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPremium: StateFlow<Boolean> = preferencesManager.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currency: StateFlow<String> = preferencesManager.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val currencySymbolAfter: StateFlow<Boolean> = preferencesManager.currencySymbolAfter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var shouldClearAmount = false
    private var pendingBudget: Budget? = null
    private var pendingScope: BudgetScope? = null
    private var pendingAndContinue: Boolean = false

    init {
        when {
            budgetId != 0L -> viewModelScope.launch {
                val budget = budgetRepository.getBudgetById(budgetId)
                _existingBudget.value = budget
                if (budget != null) {
                    _uiState.value = _uiState.value.copy(
                        amountText = String.format(Locale.ROOT, "%.2f", budget.amount),
                        selectedParentCategoryId = budget.categoryId,
                        selectedSubcategoryId = budget.subcategoryId,
                        isOverallBudget = budget.categoryId == null
                    )
                }
            }
            else -> _uiState.value = _uiState.value.copy(currentField = BudgetFormField.CATEGORY)
        }
    }

    fun appendToAmount(digit: String) {
        if (shouldClearAmount) {
            shouldClearAmount = false
            _uiState.value = _uiState.value.copy(amountText = if (digit == ".") "0." else digit)
            return
        }
        val current = _uiState.value.amountText
        if (digit == "." && current.contains(".")) return
        if (digit == "." && current.isEmpty()) {
            _uiState.value = _uiState.value.copy(amountText = "0.")
            return
        }
        _uiState.value = _uiState.value.copy(amountText = current + digit)
    }

    fun deleteLastDigit() {
        if (shouldClearAmount) {
            shouldClearAmount = false
            _uiState.value = _uiState.value.copy(amountText = "")
            return
        }
        val current = _uiState.value.amountText
        if (current.isNotEmpty()) _uiState.value = _uiState.value.copy(amountText = current.dropLast(1))
    }

    fun formatAmount() {
        val d = _uiState.value.amountText.toDoubleOrNull() ?: return
        _uiState.value = _uiState.value.copy(amountText = String.format(Locale.ROOT, "%.2f", d))
    }

    fun setCurrentField(field: BudgetFormField) {
        if (field == BudgetFormField.AMOUNT && _uiState.value.amountText.isNotEmpty()) {
            shouldClearAmount = true
        }
        _uiState.value = _uiState.value.copy(currentField = field)
    }

    fun setScope(scope: BudgetScope) {
        _uiState.value = _uiState.value.copy(selectedScope = scope)
    }

    fun selectCategory(categoryId: Long) {
        _uiState.value = _uiState.value.copy(
            selectedParentCategoryId = categoryId,
            selectedSubcategoryId = null,
            isOverallBudget = false
        )
    }

    fun selectSubcategory(subcategoryId: Long) {
        _uiState.value = _uiState.value.copy(selectedSubcategoryId = subcategoryId, isOverallBudget = false)
    }

    fun clearSubcategorySelection() {
        _uiState.value = _uiState.value.copy(selectedSubcategoryId = null, isOverallBudget = false)
    }

    fun selectOverallBudget() {
        _uiState.value = _uiState.value.copy(
            selectedParentCategoryId = null,
            selectedSubcategoryId = null,
            isOverallBudget = true
        )
    }

    fun save(categoryId: Long?, subcategoryId: Long?, period: BudgetPeriod, yearMonth: YearMonth, isOverall: Boolean = false) {
        if (!validate(categoryId, isOverall)) return
        viewModelScope.launch {
            if (budgetId == 0L && !isPremium.value && budgetRepository.getActiveBudgetCount() >= FREE_BUDGET_LIMIT) {
                _events.send(BudgetFormEvent.PremiumRequired(budgetRepository.getActiveBudgetsSync()))
                return@launch
            }
            doSave(categoryId, subcategoryId, period, yearMonth, andContinue = false)
        }
    }

    fun saveAndContinue(categoryId: Long?, subcategoryId: Long?, period: BudgetPeriod, yearMonth: YearMonth, isOverall: Boolean = false) {
        if (!validate(categoryId, isOverall)) return
        viewModelScope.launch {
            if (!isPremium.value && budgetRepository.getActiveBudgetCount() >= FREE_BUDGET_LIMIT) {
                _events.send(BudgetFormEvent.PremiumRequired(budgetRepository.getActiveBudgetsSync()))
                return@launch
            }
            doSave(categoryId, subcategoryId, period, yearMonth, andContinue = true)
        }
    }

    private suspend fun doSave(
        categoryId: Long?,
        subcategoryId: Long?,
        period: BudgetPeriod,
        yearMonth: YearMonth,
        andContinue: Boolean
    ) {
        val scope = if (isPremium.value) _uiState.value.selectedScope else BudgetScope.THIS_PERIOD_ONLY
        val amount = _uiState.value.amountText.toDoubleOrNull()?.takeIf { it > 0 } ?: return
        val month = if (period == BudgetPeriod.MONTHLY) yearMonth.monthValue else null
        val budget = if (budgetId != 0L) {
            (_existingBudget.value ?: return).copy(
                categoryId = categoryId,
                subcategoryId = subcategoryId,
                amount = amount,
                period = period,
                year = yearMonth.year,
                month = month
            )
        } else {
            Budget(
                id = 0L,
                categoryId = categoryId,
                subcategoryId = subcategoryId,
                amount = amount,
                period = period,
                year = yearMonth.year,
                month = month
            )
        }
        val conflicts = budgetRepository.findConflictsForScope(budget, scope, excludeId = budgetId)
        if (conflicts.isEmpty()) {
            budgetRepository.saveBudgetForScope(budget, scope)
            emitSaveEvent(andContinue)
        } else {
            pendingBudget = budget
            pendingScope = scope
            pendingAndContinue = andContinue
            _uiState.value = _uiState.value.copy(pendingConflicts = conflicts)
        }
    }

    fun confirmAndSave() {
        val budget = pendingBudget ?: return
        val scope = pendingScope ?: return
        val andContinue = pendingAndContinue
        _uiState.value = _uiState.value.copy(pendingConflicts = null)
        pendingBudget = null
        pendingScope = null
        viewModelScope.launch {
            budgetRepository.saveBudgetForScope(budget, scope)
            emitSaveEvent(andContinue)
        }
    }

    fun cancelSave() {
        pendingBudget = null
        pendingScope = null
        _uiState.value = _uiState.value.copy(pendingConflicts = null)
    }

    private suspend fun emitSaveEvent(andContinue: Boolean) {
        if (andContinue) {
            _uiState.value = BudgetFormUiState(currentField = BudgetFormField.CATEGORY)
            _events.send(BudgetFormEvent.SavedAndContinue)
        } else {
            _events.send(BudgetFormEvent.Saved)
        }
    }

    private fun validate(categoryId: Long?, isOverall: Boolean): Boolean {
        if (!isOverall && categoryId == null) {
            viewModelScope.launch { _events.send(BudgetFormEvent.ValidationError(R.string.budget_error_category)) }
            return false
        }
        val amount = _uiState.value.amountText.toDoubleOrNull()?.takeIf { it > 0 }
        if (amount == null) {
            viewModelScope.launch { _events.send(BudgetFormEvent.ValidationError(R.string.budget_error_amount)) }
            return false
        }
        return true
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget)
            _events.send(BudgetFormEvent.Deleted)
        }
    }

    suspend fun hasSubcategories(categoryId: Long): Boolean =
        categoryRepository.hasSubcategories(categoryId)
}
