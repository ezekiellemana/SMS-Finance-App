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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SmallFinanceWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val appCtx = context.applicationContext

        // Show safe placeholder immediately
        val placeholder = RemoteViews(appCtx.packageName, R.layout.widget_small)
        placeholder.setTextViewText(R.id.tv_balance, "Loading...")
        placeholder.setTextViewText(R.id.tv_income, "---")
        placeholder.setTextViewText(R.id.tv_expenses, "---")
        placeholder.setTextViewText(R.id.tv_last_updated, "Smart Money")
        placeholder.setOnClickPendingIntent(R.id.widget_small_root, makePendingIntent(appCtx, appWidgetId))
        appWidgetManager.updateAppWidget(appWidgetId, placeholder)

        GlobalScope.launch(Dispatchers.IO) {
            val views = RemoteViews(appCtx.packageName, R.layout.widget_small)
            views.setOnClickPendingIntent(R.id.widget_small_root, makePendingIntent(appCtx, appWidgetId))
            try {
                val prefs = appCtx.getSharedPreferences("smart_money_prefs", Context.MODE_PRIVATE)
                val theme = WidgetTheme.entries.firstOrNull {
                    it.name == prefs.getString("widget_theme", WidgetTheme.SMART_DARK.name)
                } ?: WidgetTheme.SMART_DARK
                val privacy = prefs.getBoolean("privacy_mode", false)

                views.setTextColor(R.id.tv_balance, theme.textColor)
                views.setTextColor(R.id.tv_income, theme.accentColor)
                views.setTextColor(R.id.tv_expenses, 0xFFFF5C5C.toInt())

                if (privacy) {
                    views.setTextViewText(R.id.tv_balance, "TZS ••••••")
                    views.setTextViewText(R.id.tv_income, "••••")
                    views.setTextViewText(R.id.tv_expenses, "••••")
                    views.setTextViewText(R.id.tv_last_updated, "Privacy on")
                } else {
                    val db = com.smsfinance.data.database.AppDatabase.getInstance(appCtx)
                    val (start, end) = monthRange()
                    val income = db.transactionDao().getTotalIncomeDirect(start, end) ?: 0.0
                    val expenses = db.transactionDao().getTotalExpensesDirect(start, end) ?: 0.0
                    views.setTextViewText(R.id.tv_balance, fmt(income - expenses))
                    views.setTextViewText(R.id.tv_income, fmt(income))
                    views.setTextViewText(R.id.tv_expenses, fmt(expenses))
                    views.setTextViewText(R.id.tv_last_updated,
                        "Updated ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}")
                }
            } catch (e: Exception) {
                views.setTextViewText(R.id.tv_balance, "TZS 0")
                views.setTextViewText(R.id.tv_income, "0")
                views.setTextViewText(R.id.tv_expenses, "0")
                views.setTextViewText(R.id.tv_last_updated, "Tap to open")
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun makePendingIntent(context: Context, widgetId: Int): PendingIntent =
        PendingIntent.getActivity(
            context, widgetId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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

    private fun fmt(amount: Double) =
        "TZS ${NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(amount)}"
}