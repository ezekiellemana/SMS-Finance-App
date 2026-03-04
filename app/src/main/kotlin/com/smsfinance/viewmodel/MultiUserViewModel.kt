package com.smsfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.domain.model.UserProfile
import com.smsfinance.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

data class MultiUserUiState(
    val profiles: List<UserProfile> = emptyList(),
    val activeProfile: UserProfile? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class MultiUserViewModel @Inject constructor(
    private val repository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MultiUserUiState())
    val uiState: StateFlow<MultiUserUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultProfile()
            combine(
                repository.getAllProfiles(),
                repository.getActiveProfile()
            ) { profiles, active ->
                MultiUserUiState(profiles = profiles, activeProfile = active, isLoading = false)
            }.collect { _uiState.value = it }
        }
    }

    fun addProfile(profile: UserProfile) = viewModelScope.launch {
        repository.insertProfile(profile)
    }

    fun updateProfile(profile: UserProfile) = viewModelScope.launch {
        repository.updateProfile(profile)
    }

    fun deleteProfile(profile: UserProfile) = viewModelScope.launch {
        if (_uiState.value.profiles.size > 1) {
            repository.deleteProfile(profile)
            // If deleted profile was active, switch to first remaining
            if (profile.isActive) {
                _uiState.value.profiles.firstOrNull { it.id != profile.id }
                    ?.let { repository.switchToProfile(it.id) }
            }
        }
    }

    fun switchProfile(profile: UserProfile) = viewModelScope.launch {
        repository.switchToProfile(profile.id)
    }

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
