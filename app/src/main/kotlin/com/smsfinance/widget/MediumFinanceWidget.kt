package com.smsfinance.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.smsfinance.R
import com.smsfinance.data.entity.TransactionEntity
import com.smsfinance.domain.model.WidgetTheme
import com.smsfinance.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Medium (4×2) home screen widget.
 *
 * Shows:
 *  - Estimated balance  (opening balance + new income - new expenses since setupAt)
 *  - Monthly income and expenses
 *  - Last 3 transactions with type arrow, source, amount, date
 *
 * Data loaded via [WidgetDataLoader] — same balance formula as DashboardViewModel.
 */
class MediumFinanceWidget : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val appCtx = context.applicationContext
        showPlaceholder(appCtx, appWidgetManager, appWidgetId)

        scope.launch {
            try {
                val theme      = resolveTheme(WidgetDataLoader.widgetThemeName(appCtx))
                val privacy    = WidgetDataLoader.isPrivacyOn(appCtx)
                val data       = WidgetDataLoader.load(appCtx, recentCount = 3)
                val expColor   = WidgetThemeHelper.expenseColor()

                val v = RemoteViews(appCtx.packageName, R.layout.widget_medium)

                // Theme background
                v.setInt(R.id.widget_medium_root, "setBackgroundResource",
                    WidgetThemeHelper.bgDrawable(theme))

                // Header text colours
                v.setTextColor(R.id.tv_balance_medium, theme.textColor)
                v.setTextColor(R.id.tv_income_medium,  theme.accentColor)
                v.setTextColor(R.id.tv_expense_medium, expColor)

                // Balance + monthly stats
                if (privacy) {
                    v.setTextViewText(R.id.tv_balance_medium, "TZS ••••••")
                    v.setTextViewText(R.id.tv_income_medium,  "↑ ••••")
                    v.setTextViewText(R.id.tv_expense_medium, "↓ ••••")
                } else {
                    v.setTextViewText(R.id.tv_balance_medium, WidgetDataLoader.fmtTzs(data.balance))
                    v.setTextViewText(R.id.tv_income_medium,  "↑ ${WidgetDataLoader.fmtTzs(data.income)}")
                    v.setTextViewText(R.id.tv_expense_medium, "↓ ${WidgetDataLoader.fmtTzs(data.expenses)}")
                }

                // Recent 3 transaction rows
                val txRows = listOf(
                    TxIds(R.id.tx1_type_icon, R.id.tx1_source, R.id.tx1_amount, R.id.tx1_date),
                    TxIds(R.id.tx2_type_icon, R.id.tx2_source, R.id.tx2_amount, R.id.tx2_date),
                    TxIds(R.id.tx3_type_icon, R.id.tx3_source, R.id.tx3_amount, R.id.tx3_date)
                )
                val dateFmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

                txRows.forEachIndexed { i, ids ->
                    val tx = data.recent.getOrNull(i)
                    if (tx != null) {
                        val isDeposit = tx.type == "DEPOSIT"
                        val amtColor  = if (isDeposit) theme.accentColor else expColor
                        v.setTextViewText(ids.icon,   if (isDeposit) "↑" else "↓")
                        v.setTextColor(ids.icon,       amtColor)
                        v.setTextViewText(ids.source,  tx.source)
                        v.setTextColor(ids.source,     theme.textColor)
                        val amtText = if (privacy) "••••"
                        else "${if (isDeposit) "+" else "-"} ${WidgetDataLoader.fmtTzs(tx.amount)}"
                        v.setTextViewText(ids.amount,  amtText)
                        v.setTextColor(ids.amount,     amtColor)
                        v.setTextViewText(ids.date,    dateFmt.format(Date(tx.date)))
                        v.setTextColor(ids.date,
                            WidgetThemeHelper.adjustAlpha(theme.textColor, 0.55f))
                    } else {
                        listOf(ids.icon, ids.source, ids.amount, ids.date)
                            .forEach { id -> v.setTextViewText(id, "") }
                    }
                }

                // Tap → open app
                v.setOnClickPendingIntent(R.id.widget_medium_root, makeIntent(appCtx, appWidgetId))
                appWidgetManager.updateAppWidget(appWidgetId, v)

            } catch (_: Exception) {
                showError(appCtx, appWidgetManager, appWidgetId)
            }
        }
    }

    private data class TxIds(val icon: Int, val source: Int, val amount: Int, val date: Int)

    private fun showPlaceholder(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(ctx.packageName, R.layout.widget_medium)
        views.setTextViewText(R.id.tv_balance_medium, "TZS ---")
        views.setTextViewText(R.id.tv_income_medium,  "↑ ---")
        views.setTextViewText(R.id.tv_expense_medium, "↓ ---")
        views.setOnClickPendingIntent(R.id.widget_medium_root, makeIntent(ctx, id))
        mgr.updateAppWidget(id, views)
    }

    private fun showError(ctx: Context, mgr: AppWidgetManager, id: Int) {
        try {
            val views = RemoteViews(ctx.packageName, R.layout.widget_medium)
            views.setTextViewText(R.id.tv_balance_medium, "TZS 0")
            views.setTextViewText(R.id.tv_income_medium,  "↑ 0")
            views.setTextViewText(R.id.tv_expense_medium, "↓ 0")
            views.setOnClickPendingIntent(R.id.widget_medium_root, makeIntent(ctx, id))
            mgr.updateAppWidget(id, views)
        } catch (_: Exception) {}
    }

    private fun makeIntent(context: Context, widgetId: Int): PendingIntent =
        PendingIntent.getActivity(
            context, widgetId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun resolveTheme(name: String): WidgetTheme =
        WidgetTheme.entries.firstOrNull { it.name == name } ?: WidgetTheme.SMART_DARK
}