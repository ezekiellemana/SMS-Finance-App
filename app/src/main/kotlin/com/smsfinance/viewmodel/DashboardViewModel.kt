package com.smsfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.domain.model.ChartDataPoint
import com.smsfinance.domain.model.FinancialSummary
import com.smsfinance.domain.model.Transaction
import com.smsfinance.repository.TransactionRepository
import com.smsfinance.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val summary: FinancialSummary             = FinancialSummary(0.0, 0.0, 0.0, 0),
    val allTimeIncome: Double                 = 0.0,   // ALL SMS income ever imported
    val allTimeExpenses: Double               = 0.0,   // ALL SMS expenses ever imported
    val recentTransactions: List<Transaction> = emptyList(),
    val chartData: List<ChartDataPoint>       = emptyList(),
    val userName: String                      = "",
    val openingBalance: Double                = 0.0,
    val isLoading: Boolean                    = true,
    val error: String?                        = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { observeData() }

    private fun observeData() {
        viewModelScope.launch {
            try {
                // ── Fully reactive pipeline ─────────────────────────────────────────
                // DataStore flows (userName, openingBalances, setupAt) re-emit the
                // moment onboarding writes them → balance appears instantly.
                // Room flows (allIncome, allExpenses, allTx) re-emit on every DB write.
                // We nest two combine() calls because Kotlin's typed overload caps at 5.

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
                    // Only count transactions after onboarding toward the running balance.
                    // setupAt == 0 means first-install with no onboarding timestamp —
                    // include all transactions in that case.
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
                        isLoading          = false
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

    // ── Chart data: daily income/expense totals for the current month ──────────
    private fun buildChartData(
        allTx: List<Transaction>,
        monthRange: Pair<Long, Long>
    ): List<ChartDataPoint> {
        val (start, end) = monthRange
        return allTx
            .filter { it.date in start until end }
            .groupBy { tx ->
                // Bucket by calendar day
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

    // ── Parse opening balance JSON {"NMB":"250000","MPESA":"50000"} ────────────
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