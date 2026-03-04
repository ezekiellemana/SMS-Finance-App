package com.smsfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.repository.TransactionRepository
import com.smsfinance.util.AiPredictionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiUiState(
    val result: AiPredictionEngine.PredictionResult? = null,
    val isLoading: Boolean = true,
    val isAnalyzing: Boolean = false
)

/**
 * ViewModel for AI Predictions screen.
 * Collects transaction history and runs the prediction engine.
 */
@HiltViewModel
class AiPredictionsViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val predictionEngine: AiPredictionEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    init {
        runAnalysis()
    }

    fun runAnalysis() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            repository.getAllTransactions()
                .take(1)  // snapshot — no need to re-run on every change
                .collect { transactions ->
                    val result = predictionEngine.analyze(transactions)
                    _uiState.value = AiUiState(
                        result = result,
                        isLoading = false,
                        isAnalyzing = false
                    )
                }
        }
    }
}
