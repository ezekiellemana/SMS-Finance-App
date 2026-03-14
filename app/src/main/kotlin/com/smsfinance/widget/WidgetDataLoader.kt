package com.smsfinance.widget

import android.content.Context
import androidx.core.graphics.toColorInt
import com.smsfinance.data.database.AppDatabase
import com.smsfinance.data.entity.TransactionEntity
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * Shared data-loading logic for all home screen widgets.
 *
 * Balance formula (mirrors DashboardViewModel exactly):
 *   balance = openingBalance + newIncome(after setupAt) − newExpenses(after setupAt)
 *
 * Opening balances are stored in DataStore as JSON, never as fake Room transactions.
 * All DB calls are suspend functions — call from a coroutine scope.
 */
object WidgetDataLoader {

    // ── Keys that mirror PreferencesManager / DataStore ─────────────────────
    private const val PREFS_NAME        = "smart_money_prefs"   // SharedPreferences used by widget
    private const val KEY_WIDGET_THEME  = "widget_theme"
    private const val KEY_PRIVACY        = "privacy_mode"
    private const val KEY_LANGUAGE       = "widget_language"
    private const val KEY_PROFILE_COLOR  = "widget_profile_color"

    // DataStore is written by the app. Widgets can't use DataStore directly
    // (no Hilt context) so we read the DataStore prefs file via a plain
    // suspend read through the DataStore API via a temporary DataStore instance.
    // Simpler alternative: the app always mirrors DataStore → SharedPreferences
    // for widget consumption. We do that in PreferencesManager already via
    // the "smart_money_prefs" SharedPreferences. So we just read those keys.
    private const val KEY_OPENING_BAL   = "widget_opening_balance_tzs"   // total TZS, mirrored by app
    private const val KEY_SETUP_AT      = "widget_setup_at"              // mirrored by app

    // ── Public data class returned to widgets ─────────────────────────────────
    data class WidgetData(
        val balance:  Double,
        val income:   Double,
        val expenses: Double,
        val recent:   List<TransactionEntity>
    )

    suspend fun load(context: Context, recentCount: Int = 3): WidgetData {
        val appCtx = context.applicationContext
        val prefs  = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Read opening balance and setupAt that the app mirrors into SharedPreferences
        val openingBalance = prefs.getFloat(KEY_OPENING_BAL, 0f).toDouble()
        val setupAt        = prefs.getLong(KEY_SETUP_AT,      0L)

        val db  = AppDatabase.getInstance(appCtx)
        val dao = db.transactionDao()

        // Only transactions AFTER setup count toward the running balance
        val newIncome    = dao.getTotalIncomeAfter(setupAt)    ?: 0.0
        val newExpenses  = dao.getTotalExpensesAfter(setupAt)  ?: 0.0
        val balance      = openingBalance + newIncome - newExpenses

        // Monthly stats (for income/expense display on widget — all-time from month start)
        val (monthStart, monthEnd) = monthRange()
        val monthIncome   = dao.getTotalIncomeDirect(monthStart, monthEnd)   ?: 0.0
        val monthExpenses = dao.getTotalExpensesDirect(monthStart, monthEnd) ?: 0.0

        val recent = dao.getRecentTransactionsDirect(recentCount)

        return WidgetData(
            balance  = balance,
            income   = monthIncome,
            expenses = monthExpenses,
            recent   = recent
        )
    }

    fun isPrivacyOn(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PRIVACY, false)

    fun widgetThemeName(context: Context): String =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WIDGET_THEME, "SMART_DARK") ?: "SMART_DARK"

    // ── Formatting helpers ────────────────────────────────────────────────────

    fun fmtTzs(amount: Double): String =
        "TZS ${NumberFormat.getNumberInstance(Locale.US)
            .apply { maximumFractionDigits = 0 }.format(amount)}"

    fun fmtShort(amount: Double): String =
        NumberFormat.getNumberInstance(Locale.US)
            .apply { maximumFractionDigits = 0 }.format(amount)

    // ── Date helpers ──────────────────────────────────────────────────────────

    fun widgetLanguage(context: Context): String =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "en") ?: "en"

    fun profileColor(context: Context): Int {
        @Suppress("SpellCheckingInspection")
        val defaultColor = "#3DDAD7"
        val hex = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROFILE_COLOR, defaultColor) ?: defaultColor
        return try {
            hex.toColorInt()
        } catch (_: Exception) {
            defaultColor.toColorInt()
        }
    }

    private fun monthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }
}