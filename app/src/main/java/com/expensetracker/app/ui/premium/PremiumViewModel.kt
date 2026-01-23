package com.expensetracker.app.ui.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.billing.BillingManager
import com.expensetracker.app.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PremiumEvent>()
    val events = _events.asSharedFlow()

    init {
        loadPremiumDetails()
        observeBillingEvents()
    }

    private fun loadPremiumDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val price = billingManager.getPremiumPrice()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                price = price ?: "$4.99" // Fallback price
            )
        }
    }

    private fun observeBillingEvents() {
        viewModelScope.launch {
            billingManager.purchaseState.collect { state ->
                when (state) {
                    is BillingManager.PurchaseState.Success -> {
                        preferencesManager.setPremium(true)
                        _events.emit(PremiumEvent.PurchaseSuccess)
                    }
                    is BillingManager.PurchaseState.Error -> {
                        _events.emit(PremiumEvent.PurchaseError(state.message))
                    }
                    is BillingManager.PurchaseState.Canceled -> {
                        _events.emit(PremiumEvent.PurchaseCanceled)
                    }
                    else -> {}
                }
            }
        }
    }

    fun purchasePremium(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            billingManager.launchBillingFlow(activity)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val isPremium = billingManager.checkPurchases()
            if (isPremium) {
                preferencesManager.setPremium(true)
                _events.emit(PremiumEvent.PurchaseSuccess)
            } else {
                _events.emit(PremiumEvent.PurchaseError("No previous purchase found"))
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}

data class PremiumUiState(
    val isLoading: Boolean = false,
    val price: String? = null
)

sealed class PremiumEvent {
    data object PurchaseSuccess : PremiumEvent()
    data class PurchaseError(val message: String) : PremiumEvent()
    data object PurchaseCanceled : PremiumEvent()
}
