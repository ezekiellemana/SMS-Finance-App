package com.smsfinance.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.smsfinance.R
import com.smsfinance.domain.model.WidgetTheme
import com.smsfinance.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MediumFinanceWidget : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, MediumFinanceWidget::class.java))
        onUpdate(context, manager, ids)
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_medium)

            val pi = PendingIntent.getActivity(
                context, appWidgetId,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_medium_root, pi)

            // Placeholder immediately
            views.setTextViewText(R.id.tv_balance_medium, "TZS ---")
            views.setTextViewText(R.id.tv_income_medium, "↑ ---")
            views.setTextViewText(R.id.tv_expense_medium, "↓ ---")
            appWidgetManager.updateAppWidget(appWidgetId, views)

            scope.launch {
                try {
                    val prefs = context.getSharedPreferences("smart_money_prefs", Context.MODE_PRIVATE)
                    val theme = WidgetTheme.entries.firstOrNull {
                        it.name == prefs.getString("widget_theme", WidgetTheme.SMART_DARK.name)
                    } ?: WidgetTheme.SMART_DARK
                    val privacy = prefs.getBoolean("privacy_mode", false)

                    // Safe drawable background — works on all launchers
                    views.setInt(R.id.widget_medium_root, "setBackgroundResource",
                        WidgetThemeHelper.bgDrawable(theme))

                    val expenseColor = WidgetThemeHelper.expenseColor()
                    views.setTextColor(R.id.tv_balance_medium, theme.textColor)
                    views.setTextColor(R.id.tv_income_medium, theme.accentColor)
                    views.setTextColor(R.id.tv_expense_medium, expenseColor)

                    if (privacy) {
                        views.setTextViewText(R.id.tv_balance_medium, "TZS ••••••")
                        views.setTextViewText(R.id.tv_income_medium, "↑ ••••")
                        views.setTextViewText(R.id.tv_expense_medium, "↓ ••••")
                    } else {
                        val db = com.smsfinance.data.database.AppDatabase.getInstance(context)
                        val (start, end) = monthRange()
                        val income   = db.transactionDao().getTotalIncomeDirect(start, end) ?: 0.0
                        val expenses = db.transactionDao().getTotalExpensesDirect(start, end) ?: 0.0
                        val balance  = income - expenses

                        views.setTextViewText(R.id.tv_balance_medium, fmt(balance))
                        views.setTextViewText(R.id.tv_income_medium, "↑ ${fmt(income)}")
                        views.setTextViewText(R.id.tv_expense_medium, "↓ ${fmt(expenses)}")
                    }

                    // Recent 3 transactions
                    val txRows = listOf(
                        TxIds(R.id.tx1_type_icon, R.id.tx1_source, R.id.tx1_amount, R.id.tx1_date),
                        TxIds(R.id.tx2_type_icon, R.id.tx2_source, R.id.tx2_amount, R.id.tx2_date),
                        TxIds(R.id.tx3_type_icon, R.id.tx3_source, R.id.tx3_amount, R.id.tx3_date)
                    )
                    val db = com.smsfinance.data.database.AppDatabase.getInstance(context)
                    val transactions = db.transactionDao().getRecentTransactionsDirect(3)

                    txRows.forEachIndexed { i, ids ->
                        val tx = transactions.getOrNull(i)
                        if (tx != null) {
                            val isDeposit = tx.type == "DEPOSIT"
                            val amtColor  = if (isDeposit) theme.accentColor else expenseColor
                            views.setTextViewText(ids.icon, if (isDeposit) "↑" else "↓")
                            views.setTextColor(ids.icon, amtColor)
                            views.setTextViewText(ids.source, tx.source)
                            views.setTextColor(ids.source, theme.textColor)
                            views.setTextViewText(ids.amount,
                                if (privacy) "••••"
                                else "${if (isDeposit) "+" else "-"} ${fmt(tx.amount)}")
                            views.setTextColor(ids.amount, amtColor)
                            views.setTextViewText(ids.date,
                                SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(tx.date)))
                            views.setTextColor(ids.date,
                                WidgetThemeHelper.adjustAlpha(theme.textColor, 0.55f))
                        } else {
                            views.setTextViewText(ids.icon, "")
                            views.setTextViewText(ids.source, "")
                            views.setTextViewText(ids.amount, "")
                            views.setTextViewText(ids.date, "")
                        }
                    }
                } catch (e: Exception) {
                    views.setTextViewText(R.id.tv_balance_medium, "TZS 0")
                    views.setTextViewText(R.id.tv_income_medium, "↑ 0")
                    views.setTextViewText(R.id.tv_expense_medium, "↓ 0")
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            try {
                val safe = RemoteViews(context.packageName, R.layout.widget_medium)
                safe.setTextViewText(R.id.tv_balance_medium, "Smart Money")
                appWidgetManager.updateAppWidget(appWidgetId, safe)
            } catch (_: Exception) {}
        }
    }

    private data class TxIds(val icon: Int, val source: Int, val amount: Int, val date: Int)

    private fun monthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }

    private fun fmt(amount: Double): String =
        "TZS ${NumberFormat.getNumberInstance(Locale.US)
            .apply { maximumFractionDigits = 0 }.format(amount)}"
}
