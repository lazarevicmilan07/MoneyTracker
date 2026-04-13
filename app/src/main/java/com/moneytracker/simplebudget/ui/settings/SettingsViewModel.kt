package com.moneytracker.simplebudget.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.simplebudget.data.preferences.PreferencesManager
import com.moneytracker.simplebudget.data.preferences.ThemeMode
import com.moneytracker.simplebudget.data.preferences.UserPreferences
import com.moneytracker.simplebudget.ui.reports.SubcategoryDisplayMode
import com.moneytracker.simplebudget.domain.usecase.BackupRestoreUseCase
import com.moneytracker.simplebudget.domain.usecase.CurrencyConversionUseCase
import com.moneytracker.simplebudget.domain.usecase.ExportPeriodParams
import com.moneytracker.simplebudget.domain.usecase.ExportUseCase
import java.time.YearMonth
import com.moneytracker.simplebudget.notifications.BackupReminderManager
import com.moneytracker.simplebudget.notifications.BackupReminderPreferences
import com.moneytracker.simplebudget.notifications.ReminderManager
import com.moneytracker.simplebudget.notifications.ReminderPreferences
import com.moneytracker.simplebudget.notifications.ReminderSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val exportUseCase: ExportUseCase,
    private val backupRestoreUseCase: BackupRestoreUseCase,
    private val reminderPreferences: ReminderPreferences,
    private val reminderManager: ReminderManager,
    private val backupReminderPreferences: BackupReminderPreferences,
    private val backupReminderManager: BackupReminderManager,
    private val currencyConversionUseCase: CurrencyConversionUseCase
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = preferencesManager.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val reminderSettings: StateFlow<ReminderSettings> = reminderPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderSettings())

    val backupReminderSettings: StateFlow<ReminderSettings> = backupReminderPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderSettings())

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun setCurrency(currency: String) {
        val currentCurrency = userPreferences.value.currency
        if (currency == currentCurrency) return
        viewModelScope.launch {
            val count = currencyConversionUseCase.getTransactionCount()
            if (count == 0) {
                preferencesManager.setCurrency(currency)
            } else {
                _uiState.update {
                    it.copy(
                        conversionState = CurrencyConversionUiState(
                            fromCurrency = currentCurrency,
                            toCurrency = currency,
                            transactionCount = count
                        )
                    )
                }
                fetchExchangeRate(currentCurrency, currency)
            }
        }
    }

    fun fetchExchangeRate(from: String, to: String) {
        viewModelScope.launch {
            val result = currencyConversionUseCase.fetchRate(from, to)
            _uiState.update { s ->
                val newRateState = result.fold(
                    onSuccess = { RateState.Success(it) },
                    onFailure = { RateState.Failed }
                )
                val prefilled = result.getOrNull()?.let { "%.4f".format(it) }
                s.copy(
                    conversionState = s.conversionState?.copy(
                        rateState = newRateState,
                        manualRateInput = prefilled ?: s.conversionState.manualRateInput
                    )
                )
            }
        }
    }

    fun onManualRateChanged(input: String) {
        _uiState.update { s ->
            s.copy(conversionState = s.conversionState?.copy(manualRateInput = input))
        }
    }

    fun switchToManualEntry() {
        _uiState.update { s ->
            s.copy(conversionState = s.conversionState?.copy(isManualEntry = true))
        }
    }

    fun confirmConvert() {
        val state = _uiState.value.conversionState ?: return
        val rate = if (state.rateState is RateState.Success && !state.isManualEntry) {
            state.rateState.rate
        } else {
            state.manualRateInput.toDoubleOrNull() ?: return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(conversionState = it.conversionState?.copy(isConverting = true)) }
            val result = currencyConversionUseCase.convertAllAmounts(rate)
            if (result.isSuccess) {
                preferencesManager.setCurrency(state.toCurrency)
                _uiState.update { it.copy(conversionState = null) }
                _events.emit(SettingsEvent.CurrencyConversionSuccess(state.toCurrency))
            } else {
                _uiState.update { it.copy(conversionState = it.conversionState?.copy(isConverting = false)) }
                _events.emit(SettingsEvent.ShowError("Conversion failed. Please try again."))
            }
        }
    }

    fun skipConvert() {
        val toCurrency = _uiState.value.conversionState?.toCurrency ?: return
        viewModelScope.launch {
            preferencesManager.setCurrency(toCurrency)
            _uiState.update { it.copy(conversionState = null) }
        }
    }

    fun dismissConversionDialog() {
        _uiState.update { it.copy(conversionState = null) }
    }

    fun setCurrencySymbolAfter(after: Boolean) {
        viewModelScope.launch {
            preferencesManager.setCurrencySymbolAfter(after)
        }
    }

    fun setSubcategoryDisplayMode(mode: SubcategoryDisplayMode) {
        viewModelScope.launch {
            preferencesManager.setSubcategoryDisplayModeOrdinal(mode.ordinal)
        }
    }

    fun showCurrencyPicker() {
        _uiState.value = _uiState.value.copy(showCurrencyPicker = true)
    }

    fun hideCurrencyPicker() {
        _uiState.value = _uiState.value.copy(showCurrencyPicker = false)
    }

    fun exportToExcel(context: Context, uri: Uri, isMonthly: Boolean, months: List<YearMonth>, years: List<Int>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExportingExcel = true)
            val result = exportUseCase.exportToExcel(context, uri, isMonthly, months, years)
            _uiState.value = _uiState.value.copy(isExportingExcel = false)
            if (result.isSuccess) {
                _events.emit(SettingsEvent.ExportSuccess("Excel"))
            } else {
                _events.emit(SettingsEvent.ShowError(result.exceptionOrNull()?.message ?: "Export failed"))
            }
        }
    }

    fun exportToPdf(context: Context, uri: Uri, isMonthly: Boolean, months: List<YearMonth>, years: List<Int>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExportingPdf = true)
            val result = exportUseCase.exportToPdf(context, uri, isMonthly, months, years)
            _uiState.value = _uiState.value.copy(isExportingPdf = false)
            if (result.isSuccess) {
                _events.emit(SettingsEvent.ExportSuccess("PDF"))
            } else {
                _events.emit(SettingsEvent.ShowError(result.exceptionOrNull()?.message ?: "Export failed"))
            }
        }
    }

    fun backup(context: Context, uri: Uri, isMonthly: Boolean, months: List<YearMonth>, years: List<Int>) {
        viewModelScope.launch {
            if (!userPreferences.value.isPremium) {
                _events.emit(SettingsEvent.ShowPremiumRequired("Backup"))
                return@launch
            }
            _uiState.value = _uiState.value.copy(isBackingUp = true)
            val result = backupRestoreUseCase.backup(context, uri, isMonthly, months, years)
            _uiState.value = _uiState.value.copy(isBackingUp = false)
            if (result.isSuccess) {
                _events.emit(SettingsEvent.BackupSuccess)
            } else {
                _events.emit(SettingsEvent.ShowError(result.exceptionOrNull()?.message ?: "Backup failed"))
            }
        }
    }

    fun restore(context: Context, uri: Uri) {
        viewModelScope.launch {
            if (!userPreferences.value.isPremium) {
                _events.emit(SettingsEvent.ShowPremiumRequired("Restore"))
                return@launch
            }
            _uiState.value = _uiState.value.copy(isRestoring = true)
            val result = backupRestoreUseCase.restore(context, uri)
            _uiState.value = _uiState.value.copy(isRestoring = false)
            if (result.isSuccess) {
                _events.emit(SettingsEvent.RestoreSuccess)
            } else {
                _events.emit(SettingsEvent.ShowError(result.exceptionOrNull()?.message ?: "Restore failed"))
            }
        }
    }

    fun setPremium(isPremium: Boolean) {
        viewModelScope.launch {
            preferencesManager.setPremium(isPremium)
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val updated = reminderPreferences.settings.first().copy(enabled = enabled)
            reminderPreferences.save(updated)
            if (enabled) reminderManager.scheduleReminder(updated)
            else reminderManager.cancelReminder()
        }
    }

    fun updateReminderSettings(settings: ReminderSettings) {
        viewModelScope.launch {
            reminderPreferences.save(settings)
            if (settings.enabled) reminderManager.scheduleReminder(settings)
        }
    }

    fun setBackupReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !userPreferences.value.isPremium) {
                _events.emit(SettingsEvent.ShowPremiumRequired("Backup Reminder"))
                return@launch
            }
            val updated = backupReminderPreferences.settings.first().copy(enabled = enabled)
            backupReminderPreferences.save(updated)
            if (enabled) backupReminderManager.scheduleReminder(updated)
            else backupReminderManager.cancelReminder()
        }
    }

    fun updateBackupReminderSettings(settings: ReminderSettings) {
        viewModelScope.launch {
            backupReminderPreferences.save(settings)
            if (settings.enabled) backupReminderManager.scheduleReminder(settings)
        }
    }
}

data class SettingsUiState(
    val showCurrencyPicker: Boolean = false,
    val isExportingExcel: Boolean = false,
    val isExportingPdf: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val conversionState: CurrencyConversionUiState? = null
)

data class CurrencyConversionUiState(
    val fromCurrency: String,
    val toCurrency: String,
    val transactionCount: Int,
    val rateState: RateState = RateState.Loading,
    val manualRateInput: String = "",
    val isManualEntry: Boolean = false,
    val isConverting: Boolean = false
)

sealed class RateState {
    data object Loading : RateState()
    data class Success(val rate: Double) : RateState()
    data object Failed : RateState()
}

sealed class SettingsEvent {
    data class ExportSuccess(val type: String) : SettingsEvent()
    data object BackupSuccess : SettingsEvent()
    data object RestoreSuccess : SettingsEvent()
    data class ShowPremiumRequired(val feature: String) : SettingsEvent()
    data class ShowError(val message: String) : SettingsEvent()
    data class CurrencyConversionSuccess(val currency: String) : SettingsEvent()
}
