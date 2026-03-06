package com.smsfinance.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sms_finance_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        val PRIVACY_MODE         = booleanPreferencesKey("privacy_mode")
        val BIOMETRIC_ENABLED    = booleanPreferencesKey("biometric_enabled")
        val PIN_ENABLED          = booleanPreferencesKey("pin_enabled")
        val PIN_HASH             = stringPreferencesKey("pin_hash")
        val DARK_MODE            = booleanPreferencesKey("dark_mode")
        val ONBOARDING_DONE      = booleanPreferencesKey("onboarding_done")
        val LANGUAGE             = stringPreferencesKey("language")
        val WIDGET_THEME         = stringPreferencesKey("widget_theme")
        val SMS_HISTORY_IMPORTED = booleanPreferencesKey("sms_history_imported")
        val USER_NAME            = stringPreferencesKey("user_name")
        val SELECTED_SENDERS     = stringPreferencesKey("selected_senders")
        val OPENING_BALANCES      = stringPreferencesKey("opening_balances")
        val SETUP_COMPLETED_AT    = longPreferencesKey("setup_completed_at")
    }

    // ── Flows (observed by ViewModels) ────────────────────────────────────────
    val privacyModeFlow:      Flow<Boolean> = context.dataStore.data.map { it[PRIVACY_MODE]      ?: false }
    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED] ?: false }
    val pinEnabledFlow:       Flow<Boolean> = context.dataStore.data.map { it[PIN_ENABLED]       ?: false }
    val darkModeFlow:         Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE]         ?: false }
    val languageFlow:         Flow<String>  = context.dataStore.data.map { it[LANGUAGE]          ?: "en" }
    val widgetThemeFlow:      Flow<String>  = context.dataStore.data.map { it[WIDGET_THEME]      ?: "GREEN_DARK" }
    val onboardingDoneFlow:   Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_DONE]   ?: false }
    val userNameFlow:         Flow<String>  = context.dataStore.data.map { it[USER_NAME]         ?: "" }

    // ── One-shot reads (suspend) ──────────────────────────────────────────────
    suspend fun getPinHash(): String?       = context.dataStore.data.first()[PIN_HASH]
    suspend fun isOnboardingDone(): Boolean = context.dataStore.data.first()[ONBOARDING_DONE]      ?: false
    suspend fun isSmsHistoryImported(): Boolean = context.dataStore.data.first()[SMS_HISTORY_IMPORTED] ?: false

    // ── Writes ────────────────────────────────────────────────────────────────
    suspend fun setPrivacyMode(enabled: Boolean)     = context.dataStore.edit { it[PRIVACY_MODE]         = enabled }
    suspend fun setBiometricEnabled(enabled: Boolean)= context.dataStore.edit { it[BIOMETRIC_ENABLED]    = enabled }
    suspend fun setPinEnabled(enabled: Boolean)      = context.dataStore.edit { it[PIN_ENABLED]          = enabled }
    suspend fun setPinHash(hash: String)             = context.dataStore.edit { it[PIN_HASH]             = hash }
    suspend fun setDarkMode(enabled: Boolean)        = context.dataStore.edit { it[DARK_MODE]            = enabled }
    suspend fun setLanguage(lang: String)            = context.dataStore.edit { it[LANGUAGE]             = lang }
    suspend fun setWidgetTheme(theme: String)        = context.dataStore.edit { it[WIDGET_THEME]         = theme }
    suspend fun setOnboardingDone()                  = context.dataStore.edit { it[ONBOARDING_DONE]      = true }
    suspend fun setSmsHistoryImported(done: Boolean) = context.dataStore.edit { it[SMS_HISTORY_IMPORTED] = done }
    suspend fun setUserName(name: String)            = context.dataStore.edit { it[USER_NAME]            = name }
    suspend fun setSelectedSenders(json: String)     = context.dataStore.edit { it[SELECTED_SENDERS]     = json }
    suspend fun setOpeningBalances(json: String)      = context.dataStore.edit { it[OPENING_BALANCES]      = json }
    suspend fun getSetupCompletedAt(): Long    = context.dataStore.data.first()[SETUP_COMPLETED_AT] ?: 0L
    suspend fun getUserName(): String          = context.dataStore.data.first()[USER_NAME]        ?: ""
    suspend fun getOpeningBalances(): String   = context.dataStore.data.first()[OPENING_BALANCES]  ?: "{}"
    suspend fun setSetupCompletedAt(ts: Long)             = context.dataStore.edit { it[SETUP_COMPLETED_AT]   = ts }
}