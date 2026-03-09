package com.smsfinance.viewmodel

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.repository.UserProfileRepository
import com.smsfinance.util.LocaleHelper
import com.smsfinance.util.PreferencesManager
import com.smsfinance.widget.WidgetUpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val userProfileRepository: UserProfileRepository,
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

    /** Live list of the user's money services with their opening balances. */
    val serviceBalances: StateFlow<List<ServiceBalance>> =
        combine(prefs.selectedSendersFlow, prefs.openingBalancesFlow) { sendersJson, balJson ->
            val balObj = try { JSONObject(balJson) } catch (_: Exception) { JSONObject() }
            val selected = try {
                val arr = JSONArray(sendersJson)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (_: Exception) { emptyList() }
            selected.mapNotNull { id ->
                val meta = DashboardViewModel.KNOWN_SERVICES[id] ?: return@mapNotNull null
                ServiceBalance(id, meta.first, meta.second, meta.third, balObj.optDouble(id, 0.0))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun setWidgetTheme(theme: String) = viewModelScope.launch {
        prefs.setWidgetTheme(theme)
        widgetUpdateManager.updateAllWidgets(appContext)
    }

    fun setLanguage(lang: String, context: Context) = viewModelScope.launch {
        // 1. Persist to both DataStore and SharedPreferences so both
        //    Application.attachBaseContext and Activity.attachBaseContext
        //    read the same value on the next cold start or recreate().
        prefs.setLanguage(lang)
        context.getSharedPreferences("app_language", Context.MODE_PRIVATE)
            .edit { putString("language", lang) }

        // 2. Unwrap the ContextWrapper chain to reach the real Activity.
        //    LocalContext.current in Compose is a ContextThemeWrapper, not
        //    the Activity itself, so a direct cast always returns null.
        context.findActivity()?.recreate()
    }

    /** Walk the ContextWrapper chain until we find an Activity or give up. */
    private fun Context.findActivity(): Activity? {
        var ctx: Context = this
        repeat(10) {                       // guard against infinite loops
            if (ctx is Activity) return ctx
            ctx = (ctx as? ContextWrapper)?.baseContext ?: return null
        }
        return null
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
    /** Update the opening balance for a single service without touching others. */
    fun updateServiceBalance(serviceId: String, newBalance: Double) = viewModelScope.launch {
        val current = try { JSONObject(prefs.getOpeningBalances()) }
        catch (_: Exception) { JSONObject() }
        if (newBalance > 0.0) current.put(serviceId, newBalance)
        else current.remove(serviceId)
        prefs.setOpeningBalances(current.toString())
    }

    /** Add a new service to the user's selected list and set its opening balance. */
    fun addService(serviceId: String, openingBalance: Double) = viewModelScope.launch {
        // Read current selected senders from the StateFlow's cached value
        val currentSenders = serviceBalances.value.map { it.id }.toMutableList()
        if (serviceId !in currentSenders) {
            currentSenders.add(serviceId)
            prefs.setSelectedSenders(JSONArray(currentSenders).toString())
        }
        if (openingBalance > 0.0) {
            updateServiceBalance(serviceId, openingBalance)
        }
    }

    fun saveUserSetup(
        name: String,
        selectedSenders: List<String>,
        openingBalances: Map<String, String>
    ) = viewModelScope.launch {
        prefs.setUserName(name.trim())
        prefs.setSetupCompletedAt(System.currentTimeMillis())
        prefs.setSelectedSenders(JSONArray(selectedSenders).toString())

        // Sync the name into the active DB profile immediately so Family
        // Accounts shows the real name without requiring a fresh install.
        userProfileRepository.syncActiveProfileName()

        // Opening balances are stored purely in DataStore — NOT as transactions.
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