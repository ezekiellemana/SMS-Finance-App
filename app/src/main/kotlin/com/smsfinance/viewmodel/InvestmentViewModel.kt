package com.smsfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.domain.model.Investment
import com.smsfinance.repository.InvestmentRepository
import com.smsfinance.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvestmentUiState(
    val investments: List<Investment> = emptyList(),
    val totalValue: Double = 0.0,
    val totalInvested: Double = 0.0,
    val isLoading: Boolean = true
) {
    val totalGain: Double get() = totalValue - totalInvested
    val totalGainPercent: Double get() = if (totalInvested > 0) (totalGain / totalInvested) * 100 else 0.0
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class InvestmentViewModel @Inject constructor(
    private val repository: InvestmentRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvestmentUiState())
    val uiState: StateFlow<InvestmentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileRepository.getActiveProfile()
                .filterNotNull()
                .flatMapLatest { profile ->
                    combine(
                        repository.getAll(profile.id),
                        repository.getTotalValue(profile.id),
                        repository.getTotalInvested(profile.id)
                    ) { list, value, invested ->
                        InvestmentUiState(list, value, invested, false)
                    }
                }.collect { _uiState.value = it }
        }
    }

    fun save(i: Investment) = viewModelScope.launch {
        if (i.id == 0L) repository.insert(i) else repository.update(i)
    }
    fun delete(i: Investment) = viewModelScope.launch { repository.delete(i) }
}
