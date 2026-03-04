package com.smsfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionFilter
import com.smsfinance.domain.model.TransactionType
import com.smsfinance.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionUiState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedFilter: TransactionFilter = TransactionFilter(),
    val visibleCount: Int = 20,
    val hasMore: Boolean = false
)

/**
 * ViewModel for the Transactions list screen.
 * Handles filtering, editing, and deleting transactions.
 */
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter())
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    init {
        observeTransactions()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTransactions() {
        viewModelScope.launch {
            _filter.flatMapLatest { currentFilter ->
                val flow = when {
                    currentFilter.startDate != null && currentFilter.endDate != null ->
                        repository.getTransactionsByDateRange(
                            currentFilter.startDate,
                            currentFilter.endDate
                        )
                    else -> repository.getAllTransactions()
                }

                // Apply type filter in-memory for flexibility
                flow.map { list ->
                    list.filter { tx ->
                        currentFilter.type == null || tx.type == currentFilter.type
                    }.filter { tx ->
                        currentFilter.source == null || tx.source == currentFilter.source
                    }
                }
            }.catch { e ->
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }.collect { allTransactions ->
                val visible = _uiState.value.visibleCount
                _uiState.value = _uiState.value.copy(
                    transactions = allTransactions.take(visible),
                    isLoading = false,
                    hasMore = allTransactions.size > visible
                )
            }
        }
    }

    fun loadMore() {
        _uiState.value = _uiState.value.copy(visibleCount = _uiState.value.visibleCount + 20)
        observeTransactions()
    }

    fun applyFilter(filter: TransactionFilter) {
        _filter.value = filter
    }

    fun clearFilter() {
        _filter.value = TransactionFilter()
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction.copy(isManual = true))
        }
    }

    fun insertManualTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.insertTransaction(transaction.copy(isManual = true))
        }
    }
}