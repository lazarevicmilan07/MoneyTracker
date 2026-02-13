package com.expensetracker.app.ui.accounts

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.domain.model.AccountType
import com.expensetracker.app.data.repository.AccountRepository
import com.expensetracker.app.domain.model.Account
import com.expensetracker.app.domain.model.AccountWithBalance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    val accountsState: StateFlow<AccountsListState> = accountRepository.getAllAccountsWithBalances()
        .map { accountsWithBalances ->
            AccountsListState(
                accounts = accountsWithBalances,
                totalBalance = accountsWithBalances.sumOf { it.currentBalance }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountsListState())

    private val _events = MutableSharedFlow<AccountEvent>()
    val events = _events.asSharedFlow()

    init {
        initializeDefaultAccount()
    }

    private fun initializeDefaultAccount() {
        viewModelScope.launch {
            accountRepository.initializeDefaultAccount()
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            editingAccount = null,
            dialogName = "",
            dialogType = AccountType.BANK,
            dialogIcon = "account_balance",
            dialogColor = Color(0xFF4CAF50),
            dialogInitialBalance = ""
        )
    }

    fun showEditDialog(account: Account) {
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            editingAccount = account,
            dialogName = account.name,
            dialogType = account.type,
            dialogIcon = account.icon,
            dialogColor = account.color,
            dialogInitialBalance = if (account.initialBalance != 0.0) account.initialBalance.toString() else ""
        )
    }

    fun hideDialog() {
        _uiState.value = _uiState.value.copy(showDialog = false)
    }

    fun updateDialogName(name: String) {
        _uiState.value = _uiState.value.copy(dialogName = name)
    }

    fun updateDialogType(type: AccountType) {
        _uiState.value = _uiState.value.copy(dialogType = type)
    }

    fun updateDialogIcon(icon: String) {
        _uiState.value = _uiState.value.copy(dialogIcon = icon)
    }

    fun updateDialogColor(color: Color) {
        _uiState.value = _uiState.value.copy(dialogColor = color)
    }

    fun updateDialogInitialBalance(balance: String) {
        _uiState.value = _uiState.value.copy(dialogInitialBalance = balance)
    }

    fun saveAccount() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.dialogName.isBlank()) {
                _events.emit(AccountEvent.ShowError("Account name cannot be empty"))
                return@launch
            }

            val initialBalance = state.dialogInitialBalance.toDoubleOrNull() ?: 0.0

            val account = Account(
                id = state.editingAccount?.id ?: 0,
                name = state.dialogName.trim(),
                type = state.dialogType,
                icon = state.dialogIcon,
                color = state.dialogColor,
                initialBalance = initialBalance,
                isDefault = state.editingAccount?.isDefault ?: false
            )

            if (state.editingAccount != null) {
                accountRepository.updateAccount(account)
            } else {
                accountRepository.insertAccount(account)
            }

            hideDialog()
            _events.emit(AccountEvent.AccountSaved)
        }
    }

    fun toggleDefault(account: Account) {
        viewModelScope.launch {
            if (account.isDefault) {
                accountRepository.clearDefaultAccount(account.id)
            } else {
                accountRepository.setDefaultAccount(account.id)
            }
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            val accountCount = accountRepository.getAccountCount()
            if (accountCount <= 1) {
                _events.emit(AccountEvent.ShowError("Cannot delete the last account"))
                return@launch
            }
            accountRepository.deleteAccount(account)
            _events.emit(AccountEvent.AccountDeleted)
        }
    }
}

data class AccountsUiState(
    val showDialog: Boolean = false,
    val editingAccount: Account? = null,
    val dialogName: String = "",
    val dialogType: AccountType = AccountType.BANK,
    val dialogIcon: String = "account_balance",
    val dialogColor: Color = Color(0xFF4CAF50),
    val dialogInitialBalance: String = ""
)

data class AccountsListState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val totalBalance: Double = 0.0
)

sealed class AccountEvent {
    data object AccountSaved : AccountEvent()
    data object AccountDeleted : AccountEvent()
    data class ShowError(val message: String) : AccountEvent()
}
