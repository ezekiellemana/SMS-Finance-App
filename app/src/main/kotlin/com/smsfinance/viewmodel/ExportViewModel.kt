package com.smsfinance.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.repository.TransactionRepository
import com.smsfinance.repository.UserProfileRepository
import com.smsfinance.util.ExportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class ExportUiState(
    val isExporting: Boolean = false,
    val exportedUri: Uri? = null,
    val shareIntent: Intent? = null,
    val error: String? = null,
    val transactionCount: Int = 0
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userProfileRepository: UserProfileRepository,
    private val exportManager: ExportManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            transactionRepository.getTransactionCount().collect { count ->
                _uiState.value = _uiState.value.copy(transactionCount = count)
            }
        }
    }

    fun exportToExcel(
        startDate: Long? = null,
        endDate: Long? = null
    ) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isExporting = true, error = null)
        try {
            val profile = userProfileRepository.getActiveProfileOnce()
            val transactions = getTransactions(startDate, endDate)
            val uri = exportManager.exportToExcel(transactions, profile?.name ?: "My Account")
            val intent = exportManager.shareFile(uri,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            _uiState.value = _uiState.value.copy(
                isExporting = false, exportedUri = uri, shareIntent = intent)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isExporting = false, error = e.message)
        }
    }

    fun exportToPdf(
        startDate: Long? = null,
        endDate: Long? = null
    ) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isExporting = true, error = null)
        try {
            val profile = userProfileRepository.getActiveProfileOnce()
            val transactions = getTransactions(startDate, endDate)
            val uri = exportManager.exportToPdf(transactions, profile?.name ?: "My Account")
            val intent = exportManager.shareFile(uri, "application/pdf")
            _uiState.value = _uiState.value.copy(
                isExporting = false, exportedUri = uri, shareIntent = intent)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isExporting = false, error = e.message)
        }
    }

    fun clearShareIntent() {
        _uiState.value = _uiState.value.copy(shareIntent = null, exportedUri = null)
    }

    private suspend fun getTransactions(startDate: Long?, endDate: Long?): List<com.smsfinance.domain.model.Transaction> {
        val start = startDate ?: 0L
        val end = endDate ?: System.currentTimeMillis()
        var result = listOf<com.smsfinance.domain.model.Transaction>()
        transactionRepository.getTransactionsByDateRange(start, end).take(1).collect { result = it }
        return result
    }
}
