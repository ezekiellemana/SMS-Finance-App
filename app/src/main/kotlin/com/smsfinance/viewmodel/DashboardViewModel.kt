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

            // ── Read identity + opening balance ────────────────────────────────
            // Opening balance is typed by the user during onboarding.
            // Stored in DataStore ONLY — never as a Room transaction.
            // Must NEVER be changed by any SMS import.
            val userName       = prefs.getUserName()
            val openingBalance = parseOpeningBalances(prefs.getOpeningBalances())

            // ── Gate for balance calculation ───────────────────────────────────
            // newTxStart = the moment the user finished onboarding.
            // Only transactions AFTER this timestamp affect the running balance,
            // because everything before is already captured in openingBalance.
            // On first install (setupAt == 0) we use Long.MIN_VALUE so the first
            // batch of imported SMS all contribute to the balance correctly.
            val setupAt    = prefs.getSetupCompletedAt()
            val newTxStart = if (setupAt > 0L) setupAt else Long.MIN_VALUE

            // Emit name + opening balance immediately so UI shows something
            _uiState.update { it.copy(userName = userName, openingBalance = openingBalance) }

            try {
                // 5-way combine for fully reactive real-time dashboard:
                //  1. newIncome    — deposits after onboarding  → feeds balance formula
                //  2. newExpenses  — withdrawals after onboarding → feeds balance formula
                //  3. allIncome    — ALL deposits ever          → hero Income stat
                //  4. allExpenses  — ALL withdrawals ever       → hero Expenses stat
                //  5. allTx        — every transaction          → recent activity list
                // All 5 are Room Flows — any DB write (live SMS or import) re-emits instantly.
                combine(
                    repository.getTotalIncome(newTxStart, Long.MAX_VALUE),
                    repository.getTotalExpenses(newTxStart, Long.MAX_VALUE),
                    repository.getTotalIncome(Long.MIN_VALUE, Long.MAX_VALUE),
                    repository.getTotalExpenses(Long.MIN_VALUE, Long.MAX_VALUE),
                    repository.getAllTransactions()
                ) { newIncome, newExpenses, allIncome, allExpenses, allTx ->

                    // Balance = what user had + every deposit since onboarding
                    //                        − every withdrawal since onboarding
                    val currentBalance = openingBalance + newIncome - newExpenses

                    // Chart data for current month
                    val monthRange = getMonthRange()
                    // (chart is derived from allTx inline to avoid extra combine layer)
                    val chartData = buildChartData(allTx, monthRange)

                    DashboardUiState(
                        summary = FinancialSummary(
                            estimatedBalance = currentBalance,
                            monthlyIncome    = newIncome,
                            monthlyExpenses  = newExpenses,
                            transactionCount = allTx.size
                        ),
                        allTimeIncome      = allIncome,
                        allTimeExpenses    = allExpenses,
                        recentTransactions = allTx,
                        chartData          = chartData,
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