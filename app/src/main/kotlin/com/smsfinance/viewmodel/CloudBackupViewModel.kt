package com.smsfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.repository.TransactionRepository
import com.smsfinance.util.CloudBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class BackupUiState(
    val isSignedIn: Boolean = false,
    val signedInEmail: String? = null,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val isLoadingInfo: Boolean = false,
    val lastBackupDate: String? = null,
    val lastBackupSize: String? = null,
    val transactionCount: Int = 0,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class CloudBackupViewModel @Inject constructor(
    private val backupManager: CloudBackupManager,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        refreshSignInState()
        viewModelScope.launch {
            transactionRepository.getTransactionCount().collect { count ->
                _uiState.value = _uiState.value.copy(transactionCount = count)
            }
        }
    }

    fun refreshSignInState() {
        val isSignedIn = backupManager.isSignedIn()
        val email = backupManager.getSignedInEmail()
        _uiState.value = _uiState.value.copy(isSignedIn = isSignedIn, signedInEmail = email)
        if (isSignedIn) loadBackupInfo()
    }

    private fun loadBackupInfo() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoadingInfo = true)
        val info = backupManager.getBackupInfo()
        val dateStr = info?.lastModified?.let {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(it))
        }
        val sizeStr = info?.sizeBytes?.let { bytes ->
            when {
                bytes < 1024 -> "${bytes}B"
                bytes < 1024 * 1024 -> "${bytes / 1024}KB"
                else -> "${"%.1f".format(bytes / (1024.0 * 1024))}MB"
            }
        }
        _uiState.value = _uiState.value.copy(
            isLoadingInfo = false,
            lastBackupDate = dateStr ?: "Never backed up",
            lastBackupSize = sizeStr
        )
    }

    fun backup() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isBackingUp = true, errorMessage = null, successMessage = null)
        var txList = listOf<com.smsfinance.domain.model.Transaction>()
        transactionRepository.getAllTransactions().take(1).collect { txList = it }
        when (val result = backupManager.backupToDrive(txList)) {
            is CloudBackupManager.BackupResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    isBackingUp = false,
                    successMessage = "✅ Backed up ${result.count} transactions to Google Drive"
                )
                loadBackupInfo()
            }
            is CloudBackupManager.BackupResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isBackingUp = false,
                    errorMessage = "Backup failed: ${result.message}"
                )
            }
        }
    }

    fun restore() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isRestoring = true, errorMessage = null, successMessage = null)
        when (val result = backupManager.restoreFromDrive()) {
            is CloudBackupManager.RestoreResult.Success -> {
                // Insert restored transactions (skip existing by ID)
                result.transactions.forEach { tx ->
                    try { transactionRepository.insertTransaction(tx) } catch (_: Exception) {}
                }
                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    successMessage = "✅ Restored ${result.transactions.size} transactions from Google Drive"
                )
            }
            is CloudBackupManager.RestoreResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    errorMessage = "Restore failed: ${result.message}"
                )
            }
        }
    }

    fun buildSignInIntent() = backupManager.getSignInIntent()

    fun signOut() {
        backupManager.signOut()
        _uiState.value = _uiState.value.copy(
            isSignedIn = false, signedInEmail = null,
            lastBackupDate = null, lastBackupSize = null
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }
}
