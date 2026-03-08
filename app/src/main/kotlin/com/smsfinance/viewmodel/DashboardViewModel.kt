package com.smsfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.domain.model.ChartDataPoint
import com.smsfinance.domain.model.FinancialSummary
import com.smsfinance.domain.model.Transaction
import com.smsfinance.repository.TransactionRepository
import com.smsfinance.util.PreferencesManager
import com.smsfinance.util.SmsHistoryImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val summary: FinancialSummary             = FinancialSummary(0.0, 0.0, 0.0, 0),
    val allTimeIncome: Double                 = 0.0,
    val allTimeExpenses: Double               = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val chartData: List<ChartDataPoint>       = emptyList(),
    val userName: String                      = "",
    val openingBalance: Double                = 0.0,
    val isLoading: Boolean                    = true,
    val isRefreshing: Boolean                 = false,   // ← true while inbox scan runs
    val refreshResult: Int?                   = null,    // ← # new tx found (null = idle)
    val error: String?                        = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val prefs: PreferencesManager,
    private val smsHistoryImporter: SmsHistoryImporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { observeData() }

    // ── Refresh from SMS inbox (triggered by the ↻ button) ───────────────────

    fun refreshFromInbox() {
        if (_uiState.value.isRefreshing) return   // prevent double-tap
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, refreshResult = null) }
            val newCount = try {
                smsHistoryImporter.refreshUnread()
            } catch (e: Exception) {
                0
            }
            _uiState.update { it.copy(isRefreshing = false, refreshResult = newCount) }
        }
    }

    /** Call this after the UI has displayed the result toast/badge. */
    fun clearRefreshResult() {
        _uiState.update { it.copy(refreshResult = null) }
    }

    // ── Reactive data pipeline ────────────────────────────────────────────────

    private fun observeData() {
        viewModelScope.launch {
            try {
                val prefsFlow: Flow<Triple<String, String, Long>> = combine(
                    prefs.userNameFlow,
                    prefs.openingBalancesFlow,
                    prefs.setupCompletedAtFlow
                ) { name, balJson, setupAt -> Triple(name, balJson, setupAt) }

                combine(
                    prefsFlow,
                    repository.getTotalIncome(Long.MIN_VALUE, Long.MAX_VALUE),
                    repository.getTotalExpenses(Long.MIN_VALUE, Long.MAX_VALUE),
                    repository.getAllTransactions()
                ) { (userName, balancesJson, setupAt), allIncome, allExpenses, allTx ->

                    val openingBalance = parseOpeningBalances(balancesJson)
                    val newTxStart  = if (setupAt > 0L) setupAt else Long.MIN_VALUE
                    val newIncome   = allTx.filter { it.type.name == "DEPOSIT"    && it.date >= newTxStart }.sumOf { it.amount }
                    val newExpenses = allTx.filter { it.type.name == "WITHDRAWAL" && it.date >= newTxStart }.sumOf { it.amount }
                    val balance     = openingBalance + newIncome - newExpenses

                    DashboardUiState(
                        summary = FinancialSummary(
                            estimatedBalance = balance,
                            monthlyIncome    = newIncome,
                            monthlyExpenses  = newExpenses,
                            transactionCount = allTx.size
                        ),
                        allTimeIncome      = allIncome,
                        allTimeExpenses    = allExpenses,
                        recentTransactions = allTx,
                        chartData          = buildChartData(allTx, getMonthRange()),
                        userName           = userName,
                        openingBalance     = openingBalance,
                        isLoading          = false,
                        // Preserve refresh state that observeData() doesn't own
                        isRefreshing       = _uiState.value.isRefreshing,
                        refreshResult      = _uiState.value.refreshResult
                    )
                }
                    .catch { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                    .collect { state ->
                        _uiState.value = state
                    }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun buildChartData(
        allTx: List<Transaction>,
        monthRange: Pair<Long, Long>
    ): List<ChartDataPoint> {
        val (start, end) = monthRange
        return allTx
            .filter { it.date in start until end }
            .groupBy { tx ->
                Calendar.getInstance().apply { timeInMillis = tx.date }
                    .get(Calendar.DAY_OF_MONTH)
            }
            .entries
            .sortedBy { it.key }
            .map { (day, txList) ->
                ChartDataPoint(
                    label   = "$day",
                    income  = txList.filter { it.type.name == "DEPOSIT" }
                        .sumOf { it.amount }.toFloat(),
                    expense = txList.filter { it.type.name == "WITHDRAWAL" }
                        .sumOf { it.amount }.toFloat()
                )
            }
    }

    private fun parseOpeningBalances(json: String): Double {
        return try {
            val obj = JSONObject(json)
            var total = 0.0
            obj.keys().forEach { key -> total += obj.optDouble(key, 0.0) }
            total
        } catch (_: Exception) { 0.0 }
    }

    private fun getMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return Pair(start, cal.timeInMillis)
    }
}