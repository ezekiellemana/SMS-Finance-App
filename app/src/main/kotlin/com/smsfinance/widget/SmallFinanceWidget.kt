package com.smsfinance.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small (2×2) home screen widget.
 *
 * Shows:
 *  - Estimated balance  (opening balance + new income − new expenses since setupAt)
 *  - Monthly income
 *  - Monthly expenses
 *  - Last-updated timestamp
 *
 * Data is loaded via [WidgetDataLoader] which mirrors the exact balance formula
 * used by DashboardViewModel, reading opening balances from SharedPreferences
 * (mirrored there by PreferencesManager on every save).
 */
class SmallFinanceWidget : AppWidgetProvider() {

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
        val ids = mgr.getAppWidgetIds(ComponentName(context, SmallFinanceWidget::class.java))
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
                val theme   = resolveTheme(WidgetDataLoader.widgetThemeName(appCtx))
                val privacy = WidgetDataLoader.isPrivacyOn(appCtx)
                val data    = WidgetDataLoader.load(appCtx, recentCount = 1)

                val views = RemoteViews(appCtx.packageName, R.layout.widget_small)

                // Theme background drawable — must use setInt / setBackgroundResource
                // because setBackgroundColor crashes on some launchers (Samsung, MIUI)
                views.setInt(R.id.widget_small_root, "setBackgroundResource",
                    WidgetThemeHelper.bgDrawable(theme))

                views.setTextColor(R.id.tv_balance,  theme.textColor)
                views.setTextColor(R.id.tv_income,   theme.accentColor)
                views.setTextColor(R.id.tv_expenses, WidgetThemeHelper.expenseColor())

                if (privacy) {
                    views.setTextViewText(R.id.tv_balance,      "TZS \u2022\u2022\u2022\u2022\u2022\u2022")
                    views.setTextViewText(R.id.tv_income,       "\u2022\u2022\u2022\u2022")
                    views.setTextViewText(R.id.tv_expenses,     "\u2022\u2022\u2022\u2022")
                    views.setTextViewText(R.id.tv_last_updated, "Privacy on")
                } else {
                    views.setTextViewText(R.id.tv_balance,  WidgetDataLoader.fmtTzs(data.balance))
                    views.setTextViewText(R.id.tv_income,   WidgetDataLoader.fmtShort(data.income))
                    views.setTextViewText(R.id.tv_expenses, WidgetDataLoader.fmtShort(data.expenses))
                    views.setTextViewText(R.id.tv_last_updated,
                        "Updated ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}")
                }

                views.setOnClickPendingIntent(R.id.widget_small_root, makeIntent(appCtx, appWidgetId))
                appWidgetManager.updateAppWidget(appWidgetId, views)

            } catch (_: Exception) {
                showError(appCtx, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun showPlaceholder(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(ctx.packageName, R.layout.widget_small)
        views.setTextViewText(R.id.tv_balance,      "Loading...")
        views.setTextViewText(R.id.tv_income,       "---")
        views.setTextViewText(R.id.tv_expenses,     "---")
        views.setTextViewText(R.id.tv_last_updated, "Smart Money")
        views.setOnClickPendingIntent(R.id.widget_small_root, makeIntent(ctx, id))
        mgr.updateAppWidget(id, views)
    }

    private fun showError(ctx: Context, mgr: AppWidgetManager, id: Int) {
        try {
            val views = RemoteViews(ctx.packageName, R.layout.widget_small)
            views.setTextViewText(R.id.tv_balance,      "TZS 0")
            views.setTextViewText(R.id.tv_income,       "0")
            views.setTextViewText(R.id.tv_expenses,     "0")
            views.setTextViewText(R.id.tv_last_updated, "Tap to open")
            views.setOnClickPendingIntent(R.id.widget_small_root, makeIntent(ctx, id))
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