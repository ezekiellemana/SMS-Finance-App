package com.smsfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.data.dao.TransactionDao
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType
import com.smsfinance.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Transaction> = emptyList(),
    val isSearching: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val typeFilter: TransactionType? = null
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _typeFilter = MutableStateFlow<TransactionType?>(null)
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(300)
                .combine(_typeFilter) { q, filter -> Pair(q, filter) }
                .flatMapLatest { (q, filter) ->
                    if (q.length < 2) {
                        flowOf(emptyList())
                    } else {
                        repository.getAllTransactions().map { all ->
                            all.filter { tx ->
                                val matchesQuery = tx.source.contains(q, ignoreCase = true) ||
                                    tx.description.contains(q, ignoreCase = true) ||
                                    tx.reference.contains(q, ignoreCase = true) ||
                                    tx.amount.toString().contains(q)
                                val matchesType = filter == null || tx.type == filter
                                matchesQuery && matchesType
                            }.take(100)
                        }
                    }
                }
                .collect { results ->
                    _uiState.value = _uiState.value.copy(results = results, isSearching = false)
                }
        }
    }

    fun setQuery(q: String) {
        _query.value = q
        _uiState.value = _uiState.value.copy(query = q, isSearching = q.length >= 2)
    }

    fun setTypeFilter(type: TransactionType?) {
        _typeFilter.value = type
        _uiState.value = _uiState.value.copy(typeFilter = type)
    }

    fun saveRecentSearch(query: String) {
        if (query.isBlank() || query.length < 2) return
        val current = _recentSearches.value.toMutableList()
        current.remove(query)
        current.add(0, query)
        val trimmed = current.take(8)
        _recentSearches.value = trimmed
        _uiState.value = _uiState.value.copy(recentSearches = trimmed)
    }

    fun clearSearch() {
        _query.value = ""
        _uiState.value = _uiState.value.copy(query = "", results = emptyList())
    }
}
