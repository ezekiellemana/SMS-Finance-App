package com.smsfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.domain.model.ChartDataPoint
import com.smsfinance.domain.model.FinancialSummary
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType
import com.smsfinance.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val summary: FinancialSummary = FinancialSummary(0.0, 0.0, 0.0, 0),
    val recentTransactions: List<Transaction> = emptyList(),
    val chartData: List<ChartDataPoint> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * ViewModel for the Dashboard screen.
 * Exposes financial summary, recent transactions, and chart data.
 * Uses StateFlow for reactive UI updates.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // ─── Date range filter ────────────────────────────────────────────────────
    private val _selectedRange = MutableStateFlow(getDefaultRange())
    val selectedRange: StateFlow<Pair<Long, Long>> = _selectedRange.asStateFlow()

    init {
        observeData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeData() {
        viewModelScope.launch {
            _selectedRange.flatMapLatest { (start, end) ->
                // Combine all flows together
                combine(
                    repository.getTotalIncome(start, end),
                    repository.getTotalExpenses(start, end),
                    repository.getRecentTransactions(20),
                    repository.getTransactionCount(),
                    repository.getDailyTotals(start, end)
                ) { income, expenses, recent, count, daily ->

                    val balance = income - expenses
                    val summary = FinancialSummary(
                        estimatedBalance = balance,
                        monthlyIncome = income,
                        monthlyExpenses = expenses,
                        transactionCount = count
                    )

                    val chartData = daily.map { dayTotal ->
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = dayTotal.day * 86400000L
                        ChartDataPoint(
                            label = "${cal.get(Calendar.DAY_OF_MONTH)}",
                            income = dayTotal.income.toFloat(),
                            expense = dayTotal.expense.toFloat()
                        )
                    }

                    DashboardUiState(
                        summary = summary,
                        recentTransactions = recent,
                        chartData = chartData,
                        isLoading = false
                    )
                }
            }.catch { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setDateRange(start: Long, end: Long) {
        _selectedRange.value = Pair(start, end)
    }

    fun setCurrentMonth() {
        _selectedRange.value = getDefaultRange()
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    private fun getDefaultRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return Pair(start, cal.timeInMillis)
    }
}
