package com.expensetracker.app.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.entity.TransactionType
import com.expensetracker.app.data.repository.CategoryRepository
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Expense
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val expenseId: Long? = savedStateHandle.get<Long>("expenseId")?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<TransactionEvent>()
    val events = _events.asSharedFlow()

    init {
        if (expenseId != null) {
            loadExpense(expenseId)
        }
    }

    private fun loadExpense(id: Long) {
        viewModelScope.launch {
            expenseRepository.getExpenseById(id)?.let { expense ->
                _uiState.value = _uiState.value.copy(
                    amount = expense.amount.toString(),
                    note = expense.note,
                    selectedCategoryId = expense.categoryId,
                    transactionType = expense.type,
                    selectedDate = expense.date,
                    isEditing = true
                )
            }
        }
    }

    fun updateAmount(amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) {
            _uiState.value = _uiState.value.copy(amount = filtered)
        }
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun selectCategory(categoryId: Long?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun selectTransactionType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(transactionType = type)
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    fun saveTransaction() {
        viewModelScope.launch {
            val state = _uiState.value
            val amount = state.amount.toDoubleOrNull()

            if (amount == null || amount <= 0) {
                _events.emit(TransactionEvent.ShowError("Please enter a valid amount"))
                return@launch
            }

            val expense = Expense(
                id = expenseId ?: 0,
                amount = amount,
                note = state.note,
                categoryId = state.selectedCategoryId,
                type = state.transactionType,
                date = state.selectedDate
            )

            if (expenseId != null) {
                expenseRepository.updateExpense(expense)
            } else {
                expenseRepository.insertExpense(expense)
            }

            _events.emit(TransactionEvent.TransactionSaved)
        }
    }
}

data class TransactionUiState(
    val amount: String = "",
    val note: String = "",
    val selectedCategoryId: Long? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val selectedDate: LocalDate = LocalDate.now(),
    val isEditing: Boolean = false
)

sealed class TransactionEvent {
    data object TransactionSaved : TransactionEvent()
    data class ShowError(val message: String) : TransactionEvent()
}
