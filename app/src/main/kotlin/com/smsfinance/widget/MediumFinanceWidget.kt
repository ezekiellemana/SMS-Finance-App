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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MediumFinanceWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, MediumFinanceWidget::class.java))
        onUpdate(context, mgr, ids)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Immediate placeholder
        val views = RemoteViews(context.packageName, R.layout.widget_medium)
        views.setTextViewText(R.id.tv_balance_medium, "TZS ---")
        views.setTextViewText(R.id.tv_income_medium, "↑ ---")
        views.setTextViewText(R.id.tv_expense_medium, "↓ ---")
        views.setOnClickPendingIntent(
            R.id.widget_medium_root,
            PendingIntent.getActivity(
                context, appWidgetId,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        appWidgetManager.updateAppWidget(appWidgetId, views)

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val appCtx = context.applicationContext
                val prefs = appCtx.getSharedPreferences("smart_money_prefs", Context.MODE_PRIVATE)
                val theme = WidgetTheme.entries.firstOrNull {
                    it.name == prefs.getString("widget_theme", WidgetTheme.SMART_DARK.name)
                } ?: WidgetTheme.SMART_DARK
                val privacy = prefs.getBoolean("privacy_mode", false)

                val v = RemoteViews(appCtx.packageName, R.layout.widget_medium)
                v.setOnClickPendingIntent(
                    R.id.widget_medium_root,
                    PendingIntent.getActivity(
                        appCtx, appWidgetId,
                        Intent(appCtx, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                val expColor = WidgetThemeHelper.expenseColor()
                v.setTextColor(R.id.tv_balance_medium, theme.textColor)
                v.setTextColor(R.id.tv_income_medium, theme.accentColor)
                v.setTextColor(R.id.tv_expense_medium, expColor)

                // Balance
                if (privacy) {
                    v.setTextViewText(R.id.tv_balance_medium, "TZS ••••••")
                    v.setTextViewText(R.id.tv_income_medium, "↑ ••••")
                    v.setTextViewText(R.id.tv_expense_medium, "↓ ••••")
                } else {
                    val db = com.smsfinance.data.database.AppDatabase.getInstance(appCtx)
                    val dao = db.transactionDao()
                    val (start, end) = monthRange()
                    val income   = dao.getTotalIncomeDirect(start, end) ?: 0.0
                    val expenses = dao.getTotalExpensesDirect(start, end) ?: 0.0
                    val balance  = income - expenses
                    v.setTextViewText(R.id.tv_balance_medium, fmt(balance))
                    v.setTextViewText(R.id.tv_income_medium, "↑ ${fmt(income)}")
                    v.setTextViewText(R.id.tv_expense_medium, "↓ ${fmt(expenses)}")
                }

                // Recent 3 transactions
                val txRows = listOf(
                    TxIds(R.id.tx1_type_icon, R.id.tx1_source, R.id.tx1_amount, R.id.tx1_date),
                    TxIds(R.id.tx2_type_icon, R.id.tx2_source, R.id.tx2_amount, R.id.tx2_date),
                    TxIds(R.id.tx3_type_icon, R.id.tx3_source, R.id.tx3_amount, R.id.tx3_date)
                )
                val db2 = com.smsfinance.data.database.AppDatabase.getInstance(appCtx)
                val txList = db2.transactionDao().getRecentTransactionsDirect(3)

                txRows.forEachIndexed { i, ids ->
                    val tx = txList.getOrNull(i)
                    if (tx != null) {
                        val isDeposit = tx.type == "DEPOSIT"
                        val amtColor  = if (isDeposit) theme.accentColor else expColor
                        v.setTextViewText(ids.icon, if (isDeposit) "↑" else "↓")
                        v.setTextColor(ids.icon, amtColor)
                        v.setTextViewText(ids.source, tx.source)
                        v.setTextColor(ids.source, theme.textColor)
                        val amtText = if (privacy) "••••"
                        else "${if (isDeposit) "+" else "-"} ${fmt(tx.amount)}"
                        v.setTextViewText(ids.amount, amtText)
                        v.setTextColor(ids.amount, amtColor)
                        v.setTextViewText(ids.date,
                            SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                                .format(Date(tx.date)))
                        v.setTextColor(ids.date,
                            WidgetThemeHelper.adjustAlpha(theme.textColor, 0.55f))
                    } else {
                        listOf(ids.icon, ids.source, ids.amount, ids.date).forEach { id ->
                            v.setTextViewText(id, "")
                        }
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, v)

            } catch (e: Exception) {
                try {
                    val fallback = RemoteViews(context.applicationContext.packageName, R.layout.widget_medium)
                    fallback.setTextViewText(R.id.tv_balance_medium, "TZS 0")
                    fallback.setTextViewText(R.id.tv_income_medium, "↑ 0")
                    fallback.setTextViewText(R.id.tv_expense_medium, "↓ 0")
                    appWidgetManager.updateAppWidget(appWidgetId, fallback)
                } catch (_: Exception) {}
            }
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