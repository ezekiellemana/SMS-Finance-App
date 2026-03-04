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
    @ApplicationContext private val context: Context
) {
    companion object {
        val PRIVACY_MODE    = booleanPreferencesKey("privacy_mode")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN_ENABLED     = booleanPreferencesKey("pin_enabled")
        val PIN_HASH        = stringPreferencesKey("pin_hash")
        val DARK_MODE       = booleanPreferencesKey("dark_mode")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LANGUAGE        = stringPreferencesKey("language")
        val WIDGET_THEME    = stringPreferencesKey("widget_theme")
    }

    val privacyModeFlow: Flow<Boolean>      = context.dataStore.data.map { it[PRIVACY_MODE] ?: false }
    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED] ?: false }
    val pinEnabledFlow: Flow<Boolean>       = context.dataStore.data.map { it[PIN_ENABLED] ?: false }
    val darkModeFlow: Flow<Boolean>         = context.dataStore.data.map { it[DARK_MODE] ?: false }
    val languageFlow: Flow<String>          = context.dataStore.data.map { it[LANGUAGE] ?: "en" }
    val widgetThemeFlow: Flow<String>       = context.dataStore.data.map { it[WIDGET_THEME] ?: "GREEN_DARK" }
    val onboardingDoneFlow: Flow<Boolean>   = context.dataStore.data.map { it[ONBOARDING_DONE] ?: false }

    suspend fun isPrivacyModeEnabled(): Boolean = context.dataStore.data.first()[PRIVACY_MODE] ?: false
    suspend fun isBiometricEnabled(): Boolean   = context.dataStore.data.first()[BIOMETRIC_ENABLED] ?: false
    suspend fun isPinEnabled(): Boolean         = context.dataStore.data.first()[PIN_ENABLED] ?: false
    suspend fun getPinHash(): String?           = context.dataStore.data.first()[PIN_HASH]
    suspend fun getLanguage(): String           = context.dataStore.data.first()[LANGUAGE] ?: "en"

    suspend fun setPrivacyMode(enabled: Boolean)  = context.dataStore.edit { it[PRIVACY_MODE] = enabled }
    suspend fun setBiometricEnabled(enabled: Boolean) = context.dataStore.edit { it[BIOMETRIC_ENABLED] = enabled }
    suspend fun setPinEnabled(enabled: Boolean)   = context.dataStore.edit { it[PIN_ENABLED] = enabled }
    suspend fun setPinHash(hash: String)          = context.dataStore.edit { it[PIN_HASH] = hash }
    suspend fun setDarkMode(enabled: Boolean)     = context.dataStore.edit { it[DARK_MODE] = enabled }
    suspend fun setLanguage(lang: String)         = context.dataStore.edit { it[LANGUAGE] = lang }
    suspend fun setWidgetTheme(theme: String)     = context.dataStore.edit { it[WIDGET_THEME] = theme }
    suspend fun getWidgetTheme(): String          = context.dataStore.data.first()[WIDGET_THEME] ?: "GREEN_DARK"
    suspend fun setOnboardingDone()               = context.dataStore.edit { it[ONBOARDING_DONE] = true }
    suspend fun isOnboardingDone(): Boolean       = context.dataStore.data.first()[ONBOARDING_DONE] ?: false
}
