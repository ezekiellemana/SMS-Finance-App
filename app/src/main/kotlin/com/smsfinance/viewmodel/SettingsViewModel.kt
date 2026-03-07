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
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val widgetUpdateManager: WidgetUpdateManager,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    val privacyMode: StateFlow<Boolean> = prefs.privacyModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val biometricEnabled: StateFlow<Boolean> = prefs.biometricEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val pinEnabled: StateFlow<Boolean> = prefs.pinEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val darkMode: StateFlow<Boolean> = prefs.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val language: StateFlow<String> = prefs.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocaleHelper.LANG_ENGLISH)

    val widgetTheme: StateFlow<String> = prefs.widgetThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "GREEN_DARK")

    // Read synchronously so StateFlow never flickers to false on relaunch
    private val onboardingDoneInit = runBlocking { prefs.isOnboardingDone() }
    val onboardingDone: StateFlow<Boolean> = prefs.onboardingDoneFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), onboardingDoneInit)

    // Becomes true once DataStore has emitted its first real value
    val prefsReady: StateFlow<Boolean> = prefs.onboardingDoneFlow
        .map { true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    @Suppress("unused") val userDisplayName: StateFlow<String> = prefs.userNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // ── Setters ───────────────────────────────────────────────────────────────

    fun setOnboardingDone() = viewModelScope.launch { prefs.setOnboardingDone() }

    fun setPrivacyMode(enabled: Boolean) = viewModelScope.launch {
        prefs.setPrivacyMode(enabled)
        widgetUpdateManager.updateAllWidgets(appContext)
    }

    fun setBiometricEnabled(enabled: Boolean) = viewModelScope.launch {
        prefs.setBiometricEnabled(enabled)
    }

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch {
        prefs.setDarkMode(enabled)
    }

    fun setWidgetTheme(theme: String) = viewModelScope.launch {
        prefs.setWidgetTheme(theme)
        widgetUpdateManager.updateAllWidgets(appContext)
    }

    fun setLanguage(lang: String, context: Context) = viewModelScope.launch {
        prefs.setLanguage(lang)
        context.getSharedPreferences("app_language", Context.MODE_PRIVATE)
            .edit().putString("language", lang).apply()
        // Recreate the Activity so the new locale takes effect immediately
        (context as? android.app.Activity)?.recreate()
    }

    fun setPin(pin: String) = viewModelScope.launch {
        prefs.setPinHash(sha256(pin))
        prefs.setPinEnabled(true)
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = prefs.getPinHash() ?: return false
        return sha256(pin) == stored
    }

    fun disablePin() = viewModelScope.launch { prefs.setPinEnabled(false) }

    // ── User setup (called from onboarding page 2) ────────────────────────────
    fun saveUserSetup(
        name: String,
        selectedSenders: List<String>,
        openingBalances: Map<String, String>
    ) = viewModelScope.launch {
        prefs.setUserName(name.trim())
        prefs.setSetupCompletedAt(System.currentTimeMillis())
        prefs.setSelectedSenders(JSONArray(selectedSenders).toString())

        // Opening balances are stored purely in DataStore — NOT as transactions.
        // This keeps them separate from real SMS-derived transactions so they
        // never appear in history or distort income/expense calculations.
        val balancesJson = JSONObject()
        openingBalances.forEach { (senderId, balStr) ->
            val amount = balStr.replace(",", "").toDoubleOrNull() ?: return@forEach
            if (amount <= 0.0) return@forEach
            balancesJson.put(senderId, amount)
        }
        prefs.setOpeningBalances(balancesJson.toString())
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}