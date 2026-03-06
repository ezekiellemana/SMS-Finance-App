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

            // Read user identity + opening balance instantly — emitted before DB queries
            val userName       = prefs.getUserName()
            val openingBalance = parseOpeningBalances(prefs.getOpeningBalances())

            _uiState.value = _uiState.value.copy(
                userName       = userName,
                openingBalance = openingBalance
            )

            // All time range — income/expenses come from ALL imported SMS transactions
            val allTimeStart = 0L
            val allTimeEnd   = Long.MAX_VALUE

            // Month range — used only for the bar chart
            val monthRange = getMonthRange()

            try {
                combine(
                    repository.getTotalIncome(allTimeStart, allTimeEnd),
                    repository.getTotalExpenses(allTimeStart, allTimeEnd),
                    repository.getAllTransactions(),
                    repository.getDailyTotals(monthRange.first, monthRange.second)
                ) { totalIncome: Double, totalExpenses: Double,
                    allTx: List<Transaction>, daily ->

                    // Balance = what user said they have (opening) + SMS income − SMS expenses
                    // Opening balance is stored in DataStore only, never as a DB transaction
                    val currentBalance = openingBalance + totalIncome - totalExpenses

                    val summary = FinancialSummary(
                        estimatedBalance = currentBalance,
                        monthlyIncome    = totalIncome,
                        monthlyExpenses  = totalExpenses,
                        transactionCount = allTx.size
                    )

                    val chartData = daily.map { dayTotal ->
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = dayTotal.day * 86400000L
                        }
                        ChartDataPoint(
                            label   = "${cal.get(Calendar.DAY_OF_MONTH)}",
                            income  = dayTotal.income.toFloat(),
                            expense = dayTotal.expense.toFloat()
                        )
                    }

                    DashboardUiState(
                        summary            = summary,
                        recentTransactions = allTx,   // ALL transactions, no limit — screen handles display
                        chartData          = chartData,
                        userName           = userName,
                        openingBalance     = openingBalance,
                        isLoading          = false
                    )
                }.catch { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
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
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return Pair(start, cal.timeInMillis)
    }
}