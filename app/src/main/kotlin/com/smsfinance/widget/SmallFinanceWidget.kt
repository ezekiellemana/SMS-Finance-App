package com.smsfinance.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
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

class SmallFinanceWidget : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            android.content.ComponentName(context, SmallFinanceWidget::class.java)
        )
        onUpdate(context, manager, ids)
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_small)

            // Tap → open app
            val pi = PendingIntent.getActivity(
                context, appWidgetId,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_small_root, pi)

            // Show immediately with placeholder so widget is never blank
            views.setTextViewText(R.id.tv_balance, "TZS ---")
            views.setTextViewText(R.id.tv_income, "---")
            views.setTextViewText(R.id.tv_expenses, "---")
            views.setTextViewText(R.id.tv_last_updated, "Smart Money")
            appWidgetManager.updateAppWidget(appWidgetId, views)

            // Load real data in background
            scope.launch {
                try {
                    val prefs = context.getSharedPreferences("smart_money_prefs", Context.MODE_PRIVATE)
                    val theme = WidgetTheme.entries.firstOrNull {
                        it.name == prefs.getString("widget_theme", WidgetTheme.SMART_DARK.name)
                    } ?: WidgetTheme.SMART_DARK
                    val privacy = prefs.getBoolean("privacy_mode", false)

                    // Use drawable background — safe on ALL launchers
                    views.setInt(R.id.widget_small_root, "setBackgroundResource",
                        WidgetThemeHelper.bgDrawable(theme))

                    views.setTextColor(R.id.tv_balance_label,
                        WidgetThemeHelper.adjustAlpha(theme.textColor, 0.65f))
                    views.setTextColor(R.id.tv_balance, theme.textColor)
                    views.setTextColor(R.id.tv_income, theme.accentColor)
                    views.setTextColor(R.id.tv_expenses, WidgetThemeHelper.expenseColor())
                    views.setTextColor(R.id.tv_last_updated,
                        WidgetThemeHelper.adjustAlpha(theme.textColor, 0.4f))

                    if (privacy) {
                        views.setTextViewText(R.id.tv_balance, "TZS ••••••")
                        views.setTextViewText(R.id.tv_income, "••••")
                        views.setTextViewText(R.id.tv_expenses, "••••")
                        views.setTextViewText(R.id.tv_last_updated, "Privacy on")
                    } else {
                        val db = com.smsfinance.data.database.AppDatabase.getInstance(context)
                        val (start, end) = monthRange()
                        val income   = db.transactionDao().getTotalIncomeDirect(start, end) ?: 0.0
                        val expenses = db.transactionDao().getTotalExpensesDirect(start, end) ?: 0.0
                        val balance  = income - expenses

                        views.setTextViewText(R.id.tv_balance, fmt(balance))
                        views.setTextViewText(R.id.tv_income, fmt(income))
                        views.setTextViewText(R.id.tv_expenses, fmt(expenses))
                        views.setTextViewText(R.id.tv_last_updated,
                            "Updated ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}")
                    }
                } catch (e: Exception) {
                    views.setTextViewText(R.id.tv_balance, "TZS 0")
                    views.setTextViewText(R.id.tv_income, "0")
                    views.setTextViewText(R.id.tv_expenses, "0")
                    views.setTextViewText(R.id.tv_last_updated, "Tap to refresh")
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            // Absolute last resort — widget must never show "Can't load"
            try {
                val safe = RemoteViews(context.packageName, R.layout.widget_small)
                safe.setTextViewText(R.id.tv_balance, "Smart Money")
                safe.setTextViewText(R.id.tv_last_updated, "Tap to open")
                appWidgetManager.updateAppWidget(appWidgetId, safe)
            } catch (_: Exception) {}
        }
    }

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
