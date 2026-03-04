package com.smsfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.repository.TransactionRepository
import com.smsfinance.ui.charts.MonthData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ChartsUiState(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val expensesBySource: List<Pair<String, Double>> = emptyList(),
    val incomeBySource: List<Pair<String, Double>> = emptyList(),
    val monthlyTrend: List<MonthData> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartsUiState())
    val uiState: StateFlow<ChartsUiState> = _uiState.asStateFlow()

    fun loadData(period: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val (start, end) = getDateRange(period)
            combine(
                repository.getTotalIncome(start, end),
                repository.getTotalExpenses(start, end),
                repository.getExpensesBySource(start, end),
                repository.getIncomeBySource(start, end),
                repository.getAllTransactions()
            ) { income, expenses, expBySrc, incBySrc, allTx ->

                // Build monthly trend from all transactions
                val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
                val trend = buildMonthlyTrend(allTx, period)

                ChartsUiState(
                    totalIncome = income,
                    totalExpenses = expenses,
                    expensesBySource = expBySrc.map { Pair(it.source, it.total) }
                        .sortedByDescending { it.second },
                    incomeBySource = incBySrc.map { Pair(it.source, it.total) }
                        .sortedByDescending { it.second },
                    monthlyTrend = trend,
                    isLoading = false
                )
            }.catch {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.collect {
                _uiState.value = it
            }
        }
    }

    private fun buildMonthlyTrend(
        allTx: List<com.smsfinance.domain.model.Transaction>,
        period: Int
    ): List<MonthData> {
        val monthsBack = when (period) { 0 -> 1; 1 -> 3; else -> 12 }
        val cal = Calendar.getInstance()
        val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
        val result = mutableListOf<MonthData>()

        for (i in monthsBack - 1 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            c.set(Calendar.DAY_OF_MONTH, 1)
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0)
            val monthStart = c.timeInMillis
            c.add(Calendar.MONTH, 1)
            val monthEnd = c.timeInMillis

            val monthTx = allTx.filter { it.date in monthStart until monthEnd }
            val income = monthTx.filter { it.type == com.smsfinance.domain.model.TransactionType.DEPOSIT }
                .sumOf { it.amount }
            val expense = monthTx.filter { it.type == com.smsfinance.domain.model.TransactionType.WITHDRAWAL }
                .sumOf { it.amount }

            result.add(MonthData(
                label = monthFmt.format(Date(monthStart)),
                income = income,
                expense = expense
            ))
        }
        return result
    }

    private fun getDateRange(period: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        when (period) {
            0 -> { // This month
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
            }
            1 -> { // Last 3 months
                cal.add(Calendar.MONTH, -3)
                cal.set(Calendar.DAY_OF_MONTH, 1)
            }
            2 -> { // This year
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
            }
        }
        return Pair(cal.timeInMillis, end)
    }
}
