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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small (2×2) home screen widget.
 * - Auto-updates on SMS arrival and theme change
 * - Profile-colour accent stripe
 * - Smooth fade-in
 * - Privacy mode
 * - Language-aware formatting
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
                val theme        = resolveTheme(WidgetDataLoader.widgetThemeName(appCtx))
                val privacy      = WidgetDataLoader.isPrivacyOn(appCtx)
                val data         = WidgetDataLoader.load(appCtx, recentCount = 1)
                val expColor     = WidgetThemeHelper.expenseColor()
                val profileColor = WidgetDataLoader.profileColor(appCtx)
                val lang         = WidgetDataLoader.widgetLanguage(appCtx)
                val locale       = Locale(lang)

                val textPrimary = theme.textColor
                val textMuted   = WidgetThemeHelper.adjustAlpha(textPrimary, 0.55f)
                val textFaint   = WidgetThemeHelper.adjustAlpha(textPrimary, 0.35f)
                val accentColor = theme.accentColor

                val views = RemoteViews(appCtx.packageName, R.layout.widget_small)

                // ── Theme background ─────────────────────────────────────────────
                views.setInt(R.id.widget_small_root, "setBackgroundResource",
                    WidgetThemeHelper.bgDrawable(theme))

                // ── Profile-colour accent stripe ─────────────────────────────────
                // ── Profile-colour accent stripe + live dot ──────────────────────────
                views.setInt(R.id.tv_accent_small, "setBackgroundColor", profileColor)
                views.setInt(R.id.tv_live_dot, "setBackgroundColor", profileColor)

                // ── Text colors ──────────────────────────────────────────────────
                views.setTextColor(R.id.tv_balance,       textPrimary)
                views.setTextColor(R.id.tv_balance_label, textMuted)
                views.setTextColor(R.id.tv_income,        accentColor)
                views.setTextColor(R.id.tv_expenses,      expColor)
                views.setTextColor(R.id.tv_last_updated,  textFaint)

                // ── Balance + stats ──────────────────────────────────────────────
                if (privacy) {
                    views.setTextViewText(R.id.tv_balance,      "TZS ••••••")
                    views.setTextViewText(R.id.tv_income,        "••••")
                    views.setTextViewText(R.id.tv_expenses,      "••••")
                    views.setTextViewText(R.id.tv_last_updated,  "🔒 Private")
                } else {
                    views.setTextViewText(R.id.tv_balance,  WidgetDataLoader.fmtTzs(data.balance))
                    views.setTextViewText(R.id.tv_income,   WidgetDataLoader.fmtShort(data.income))
                    views.setTextViewText(R.id.tv_expenses, WidgetDataLoader.fmtShort(data.expenses))
                    val timeFmt = SimpleDateFormat("HH:mm", locale)
                    views.setTextViewText(R.id.tv_last_updated, "↻ ${timeFmt.format(Date())}")
                }

                views.setOnClickPendingIntent(R.id.widget_small_root, makeIntent(appCtx, appWidgetId))

                // ── Push fully built views, then fade in ─────────────────────────
                appWidgetManager.updateAppWidget(appWidgetId, views)

                delay(50)
                val steps = listOf(0.4f, 0.65f, 0.82f, 0.93f, 1f)
                steps.forEach { alpha ->
                    delay(25)
                    val fade = RemoteViews(appCtx.packageName, R.layout.widget_small)
                    fade.setFloat(R.id.widget_small_root, "setAlpha", alpha)
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetId, fade)
                }

            } catch (_: Exception) {
                showError(appCtx, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun showPlaceholder(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val v = RemoteViews(ctx.packageName, R.layout.widget_small)
        val theme = resolveTheme(WidgetDataLoader.widgetThemeName(ctx))
        v.setInt(R.id.widget_small_root, "setBackgroundResource", WidgetThemeHelper.bgDrawable(theme))
        v.setFloat(R.id.widget_small_root, "setAlpha", 0.3f)
        v.setTextViewText(R.id.tv_balance, "...")
        v.setOnClickPendingIntent(R.id.widget_small_root, makeIntent(ctx, id))
        mgr.updateAppWidget(id, v)
    }

    private fun showError(ctx: Context, mgr: AppWidgetManager, id: Int) {
        try {
            val v = RemoteViews(ctx.packageName, R.layout.widget_small)
            v.setInt(R.id.widget_small_root, "setBackgroundResource",
                WidgetThemeHelper.bgDrawable(resolveTheme(WidgetDataLoader.widgetThemeName(ctx))))
            v.setFloat(R.id.widget_small_root, "setAlpha", 1f)
            v.setTextViewText(R.id.tv_balance,  "TZS 0")
            v.setTextViewText(R.id.tv_income,   "0")
            v.setTextViewText(R.id.tv_expenses, "0")
            v.setTextViewText(R.id.tv_last_updated, "Tap to open")
            v.setOnClickPendingIntent(R.id.widget_small_root, makeIntent(ctx, id))
            mgr.updateAppWidget(id, v)
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