package com.smsfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.domain.model.Budget
import com.smsfinance.domain.model.BudgetProgress
import com.smsfinance.repository.BudgetRepository
import com.smsfinance.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetUiState(
    val budgetProgress: List<BudgetProgress> = emptyList(),
    val totalBudgeted: Double = 0.0,
    val totalSpent: Double = 0.0,
    val isLoading: Boolean = true
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileRepository.getActiveProfile()
                .filterNotNull()
                .flatMapLatest { profile ->
                    budgetRepository.getBudgetProgressForCurrentMonth(profile.id)
                }
                .collect { progressList ->
                    _uiState.value = BudgetUiState(
                        budgetProgress = progressList,
                        totalBudgeted = progressList.sumOf { it.budget.amount },
                        totalSpent = progressList.sumOf { it.spent },
                        isLoading = false
                    )
                }
        }
    }

    fun saveBudget(budget: Budget) = viewModelScope.launch {
        if (budget.id == 0L) budgetRepository.insertBudget(budget)
        else budgetRepository.updateBudget(budget)
    }

    fun deleteBudget(budget: Budget) = viewModelScope.launch {
        budgetRepository.deleteBudget(budget)
    }
}
