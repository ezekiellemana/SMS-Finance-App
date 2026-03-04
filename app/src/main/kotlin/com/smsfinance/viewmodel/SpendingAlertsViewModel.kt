package com.smsfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.domain.model.AlertCheckResult
import com.smsfinance.domain.model.SpendingAlert
import com.smsfinance.repository.SpendingAlertRepository
import com.smsfinance.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val alerts: List<SpendingAlert> = emptyList(),
    val alertProgress: Map<Long, AlertCheckResult> = emptyMap(),
    val isLoading: Boolean = true
)

/**
 * ViewModel for the Spending Alerts screen.
 */
@HiltViewModel
class SpendingAlertsViewModel @Inject constructor(
    private val repository: SpendingAlertRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        loadAlerts()
    }

    private fun loadAlerts() {
        viewModelScope.launch {
            repository.getAllAlerts().collect { alerts ->
                _uiState.value = _uiState.value.copy(alerts = alerts, isLoading = false)
                // Load progress for each alert
                alerts.forEach { alert ->
                    launch {
                        repository.getAlertProgress(alert).collect { result ->
                            val updated = _uiState.value.alertProgress.toMutableMap()
                            updated[alert.id] = result
                            _uiState.value = _uiState.value.copy(alertProgress = updated)

                            // Fire notification if triggered
                            if (result.isTriggered) {
                                notificationHelper.showSpendingAlertNotification(result)
                            }
                        }
                    }
                }
            }
        }
    }

    fun saveAlert(alert: SpendingAlert) {
        viewModelScope.launch {
            if (alert.id == 0L) {
                repository.insertAlert(alert)
            } else {
                repository.updateAlert(alert)
            }
        }
    }

    fun deleteAlert(alert: SpendingAlert) {
        viewModelScope.launch {
            repository.deleteAlert(alert)
        }
    }

    fun toggleAlert(alert: SpendingAlert) {
        viewModelScope.launch {
            repository.updateAlert(alert.copy(isEnabled = !alert.isEnabled))
        }
    }
}
