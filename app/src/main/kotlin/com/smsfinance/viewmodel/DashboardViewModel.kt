package com.smsfinance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsfinance.domain.model.ChartDataPoint
import com.smsfinance.domain.model.FinancialSummary
import com.smsfinance.domain.model.Transaction
import com.smsfinance.repository.TransactionRepository
import com.smsfinance.util.PreferencesManager
import com.smsfinance.util.SmsHistoryImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import javax.inject.Inject

/** One money service entry shown in the hero card and settings. */
data class ServiceBalance(
    val id: String,
    val displayName: String,
    val emoji: String,
    val category: String,
    val openingBalance: Double
)

data class DashboardUiState(
    val summary: FinancialSummary             = FinancialSummary(0.0, 0.0, 0.0, 0),
    val allTimeIncome: Double                 = 0.0,
    val allTimeExpenses: Double               = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val chartData: List<ChartDataPoint>       = emptyList(),
    val userName: String                      = "",
    val openingBalance: Double                = 0.0,
    val serviceBalances: List<ServiceBalance> = emptyList(),
    val isLoading: Boolean                    = true,
    val isRefreshing: Boolean                 = false,
    val refreshResult: Int?                   = null,
    val error: String?                        = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val prefs: PreferencesManager,
    private val smsHistoryImporter: SmsHistoryImporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { observeData() }

    // ── Refresh from SMS inbox (triggered by the ↻ button) ───────────────────

    fun refreshFromInbox() {
        if (_uiState.value.isRefreshing) return   // prevent double-tap
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, refreshResult = null) }
            val newCount = try {
                smsHistoryImporter.refreshUnread()
            } catch (_: Exception) {
                0
            }
            _uiState.update { it.copy(isRefreshing = false, refreshResult = newCount) }
        }
    }

    /** Call this after the UI has displayed the result toast/badge. */
    fun clearRefreshResult() {
        _uiState.update { it.copy(refreshResult = null) }
    }

    // ── Reactive data pipeline ────────────────────────────────────────────────

    private fun observeData() {
        viewModelScope.launch {
            try {
                // Combine 4 prefs flows into a single array (combine() max is 5 params)
                val prefsFlow = combine(
                    prefs.userNameFlow,
                    prefs.openingBalancesFlow,
                    prefs.setupCompletedAtFlow,
                    prefs.selectedSendersFlow
                ) { name, balJson, setupAt, sendersJson ->
                    arrayOf(name, balJson, setupAt.toString(), sendersJson)
                }

                combine(
                    prefsFlow,
                    repository.getTotalIncome(Long.MIN_VALUE, Long.MAX_VALUE),
                    repository.getTotalExpenses(Long.MIN_VALUE, Long.MAX_VALUE),
                    repository.getAllTransactions()
                ) { prefs, allIncome, allExpenses, allTx ->
                    val userName     = prefs[0]
                    val balancesJson = prefs[1]
                    val setupAt      = prefs[2].toLong()
                    val sendersJson  = prefs[3]

                    val openingBalance = parseOpeningBalances(balancesJson)
                    val newTxStart  = if (setupAt > 0L) setupAt else Long.MIN_VALUE
                    val newIncome   = allTx.filter { it.type.name == "DEPOSIT" && it.date >= newTxStart }.sumOf { it.amount }
                    val newExpenses = allTx.filter { it.type.name == "WITHDRAWAL" && it.date >= newTxStart }.sumOf { it.amount }
                    val balance     = openingBalance + newIncome - newExpenses

                    DashboardUiState(
                        summary = FinancialSummary(
                            estimatedBalance = balance,
                            monthlyIncome    = newIncome,
                            monthlyExpenses  = newExpenses,
                            transactionCount = allTx.size
                        ),
                        allTimeIncome      = allIncome,
                        allTimeExpenses    = allExpenses,
                        recentTransactions = allTx,
                        chartData          = buildChartData(allTx, getMonthRange()),
                        serviceBalances    = parseServiceBalances(sendersJson, balancesJson),
                        userName           = userName,
                        openingBalance     = openingBalance,
                        isLoading          = false,
                        // Preserve refresh state that observeData() doesn't own
                        isRefreshing       = _uiState.value.isRefreshing,
                        refreshResult      = _uiState.value.refreshResult
                    )
                }
                    .catch { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                    .collect { state ->
                        _uiState.value = state
                    }

            } catch (ex: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ex.message) }
            }
        }
    }

    private fun buildChartData(
        allTx: List<Transaction>,
        monthRange: Pair<Long, Long>
    ): List<ChartDataPoint> {
        val (start, end) = monthRange
        return allTx
            .filter { it.date in start until end }
            .groupBy { tx ->
                Calendar.getInstance().apply { timeInMillis = tx.date }
                    .get(Calendar.DAY_OF_MONTH)
            }
            .entries
            .sortedBy { it.key }
            .map { (day, txList) ->
                ChartDataPoint(
                    label   = "$day",
                    income  = txList.filter { it.type.name == "DEPOSIT" }
                        .sumOf { it.amount }.toFloat(),
                    expense = txList.filter { it.type.name == "WITHDRAWAL" }
                        .sumOf { it.amount }.toFloat()
                )
            }
    }

    private fun parseOpeningBalances(json: String): Double {
        return try {
            val obj = JSONObject(json)
            var total = 0.0
            obj.keys().forEach { key -> total += obj.optDouble(key, 0.0) }
            total
        } catch (_: Exception) { 0.0 }
    }

    /**
     * Build the per-service balance list shown in the hero card.
     * Only services that appear in [sendersJson] (user's selected services)
     * are included; balance defaults to 0 if not yet set.
     */
    private fun parseServiceBalances(sendersJson: String, balancesJson: String): List<ServiceBalance> {
        val balObj = try { JSONObject(balancesJson) } catch (_: Exception) { JSONObject() }
        val selected = try {
            val arr = JSONArray(sendersJson)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) { emptyList() }

        return selected.mapNotNull { id ->
            val meta = KNOWN_SERVICES[id] ?: return@mapNotNull null
            ServiceBalance(
                id             = id,
                displayName    = meta.first,
                emoji          = meta.second,
                category       = meta.third,
                openingBalance = balObj.optDouble(id, 0.0)
            )
        }
    }

    @Suppress("SpellCheckingInspection")
    companion object {
        /** Master catalogue — matches OnboardingScreen's ALL_SENDERS list. */
        val KNOWN_SERVICES: Map<String, Triple<String, String, String>> = mapOf(
            "NMB"       to Triple("NMB Bank",               "🏦", "Bank"),
            "CRDB"      to Triple("CRDB Bank",              "🏦", "Bank"),
            "NBC"       to Triple("NBC Bank",               "🏦", "Bank"),
            "EQUITY"    to Triple("Equity Bank",            "🏦", "Bank"),
            "STANBIC"   to Triple("Stanbic Bank",           "🏦", "Bank"),
            "ABSA"      to Triple("ABSA Bank",              "🏦", "Bank"),
            "EXIM"      to Triple("EXIM Bank",              "🏦", "Bank"),
            "DTB"       to Triple("DTB Bank",               "🏦", "Bank"),
            "MPESA"     to Triple("M-Pesa",                 "📱", "Mobile Money"),
            "MIXX"      to Triple("Mixx by Yas",            "📱", "Mobile Money"),
            "AIRTEL"    to Triple("Airtel Money",           "📱", "Mobile Money"),
            "HALOPESA"  to Triple("HaloPesa",               "📱", "Mobile Money"),
            "TPESA"     to Triple("T-Pesa",                 "📱", "Mobile Money"),
            "AZAMPESA"  to Triple("AzamPesa",               "📱", "Mobile Money"),
            "SELCOMPESA" to Triple("SelcomPesa",            "📱", "Mobile Money"),
            "EZYPESA"   to Triple("EzyPesa",                "📱", "Mobile Money"),
            "NALA"      to Triple("NALA",                   "📱", "Mobile Money")
        )
    }

    private fun getMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return Pair(start, cal.timeInMillis)
    }
}