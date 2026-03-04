package com.smsfinance.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.util.LocaleHelper
import com.smsfinance.util.PreferencesManager
import com.smsfinance.widget.WidgetUpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val widgetUpdateManager: WidgetUpdateManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val privacyMode: StateFlow<Boolean> = preferencesManager.privacyModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val biometricEnabled: StateFlow<Boolean> = preferencesManager.biometricEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val pinEnabled: StateFlow<Boolean> = preferencesManager.pinEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val darkMode: StateFlow<Boolean> = preferencesManager.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val language: StateFlow<String> = preferencesManager.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocaleHelper.LANG_ENGLISH)

    val widgetTheme: StateFlow<String> = preferencesManager.widgetThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "GREEN_DARK")

    // initialValue is read synchronously in init{} below so the Flow never
    // starts at false for a user who has already completed onboarding.
    private val _onboardingDoneInit = kotlinx.coroutines.runBlocking {
        preferencesManager.isOnboardingDone()
    }
    val onboardingDone: StateFlow<Boolean> = preferencesManager.onboardingDoneFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _onboardingDoneInit)

    fun setOnboardingDone() = viewModelScope.launch { preferencesManager.setOnboardingDone() }

    // Becomes true once DataStore has emitted its first real value.
    // MainActivity uses this to avoid rendering navigation with stale defaults.
    val prefsReady: StateFlow<Boolean> = preferencesManager.onboardingDoneFlow
        .map { true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setPrivacyMode(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setPrivacyMode(enabled)
        // Refresh widgets so privacy mode takes effect immediately
        widgetUpdateManager.updateAllWidgets(appContext)
    }

    fun setBiometricEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setBiometricEnabled(enabled)
    }

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setDarkMode(enabled)
    }

    fun setWidgetTheme(theme: String) = viewModelScope.launch {
        preferencesManager.setWidgetTheme(theme)
        // Immediately refresh all home screen widgets with the new theme
        widgetUpdateManager.updateAllWidgets(appContext)
    }

    fun setLanguage(lang: String, context: Context) = viewModelScope.launch {
        preferencesManager.setLanguage(lang)
        context.applicationContext
            .getSharedPreferences("app_language", Context.MODE_PRIVATE)
            .edit().putString("language", lang).apply()
        LocaleHelper.applyLocale(context, lang)
    }

    fun setPin(pin: String) = viewModelScope.launch {
        preferencesManager.setPinHash(sha256(pin))
        preferencesManager.setPinEnabled(true)
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = preferencesManager.getPinHash() ?: return false
        return sha256(pin) == stored
    }

    fun disablePin() = viewModelScope.launch { preferencesManager.setPinEnabled(false) }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}