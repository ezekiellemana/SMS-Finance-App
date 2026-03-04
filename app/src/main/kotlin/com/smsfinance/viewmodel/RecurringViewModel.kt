package com.smsfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.domain.model.RecurringTransaction
import com.smsfinance.repository.RecurringTransactionRepository
import com.smsfinance.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringUiState(
    val recurring: List<RecurringTransaction> = emptyList(),
    val suggestions: List<RecurringTransaction> = emptyList(),
    val isDetecting: Boolean = false,
    val isLoading: Boolean = true
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val repository: RecurringTransactionRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileRepository.getActiveProfile()
                .filterNotNull()
                .flatMapLatest { profile ->
                    repository.getAll(profile.id)
                }
                .collect { list ->
                    _uiState.value = _uiState.value.copy(recurring = list, isLoading = false)
                }
        }
    }

    fun save(r: RecurringTransaction) = viewModelScope.launch {
        if (r.id == 0L) repository.insert(r) else repository.update(r)
    }

    fun delete(r: RecurringTransaction) = viewModelScope.launch {
        repository.delete(r)
    }

    fun detectPatterns() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isDetecting = true)
        val profile = userProfileRepository.getActiveProfileOnce()
        val suggestions = repository.detectPatterns(profile?.id ?: 1L)
        _uiState.value = _uiState.value.copy(isDetecting = false, suggestions = suggestions)
    }

    fun acceptSuggestion(r: RecurringTransaction) = viewModelScope.launch {
        repository.insert(r)
        _uiState.value = _uiState.value.copy(
            suggestions = _uiState.value.suggestions.filter { it != r }
        )
    }

    fun dismissSuggestion(r: RecurringTransaction) {
        _uiState.value = _uiState.value.copy(
            suggestions = _uiState.value.suggestions.filter { it != r }
        )
    }
}
