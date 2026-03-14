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
 * Medium (4×2) home screen widget.
 * - Auto-updates on SMS arrival (SmsReceiver → WidgetUpdateManager)
 * - Auto-updates on theme change (SettingsViewModel.setWidgetTheme → updateAllWidgets)
 * - Profile-colour accent stripe at top
 * - Smooth fade-in on every refresh
 * - Privacy mode hides all values
 * - Language-aware formatting
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
        // Show placeholder immediately so widget is never blank
        showPlaceholder(appCtx, appWidgetManager, appWidgetId)

        scope.launch {
            try {
                val theme        = resolveTheme(WidgetDataLoader.widgetThemeName(appCtx))
                val privacy      = WidgetDataLoader.isPrivacyOn(appCtx)
                val data         = WidgetDataLoader.load(appCtx, recentCount = 3)
                val expColor     = WidgetThemeHelper.expenseColor()
                val profileColor = WidgetDataLoader.profileColor(appCtx)
                val lang         = WidgetDataLoader.widgetLanguage(appCtx)
                val locale       = Locale(lang)

                val textPrimary  = theme.textColor
                val textMuted    = WidgetThemeHelper.adjustAlpha(textPrimary, 0.55f)
                val accentColor  = theme.accentColor

                val v = RemoteViews(appCtx.packageName, R.layout.widget_medium)

                // ── Theme background ────────────────────────────────────────────
                v.setInt(R.id.widget_medium_root, "setBackgroundResource",
                    WidgetThemeHelper.bgDrawable(theme))

                // ── Profile-colour accent stripe ────────────────────────────────
                // ── Profile-colour accent stripe + live dot ─────────────────────────
                v.setInt(R.id.tv_accent_medium, "setBackgroundColor", profileColor)
                v.setInt(R.id.tv_live_dot_medium, "setBackgroundColor", profileColor)

                // ── Text colors ──────────────────────────────────────────────────
                v.setTextColor(R.id.tv_balance_medium,     textPrimary)
                v.setTextColor(R.id.tv_income_medium,      accentColor)
                v.setTextColor(R.id.tv_expense_medium,     expColor)

                // ── Last-updated timestamp ──────────────────────────────────────
                val timeFmt = SimpleDateFormat("HH:mm", locale)
                v.setTextViewText(R.id.tv_last_updated_medium, timeFmt.format(Date()))
                v.setTextColor(R.id.tv_last_updated_medium,
                    WidgetThemeHelper.adjustAlpha(textPrimary, 0.35f))

                // ── Balance + pills ──────────────────────────────────────────────
                if (privacy) {
                    v.setTextViewText(R.id.tv_balance_medium, "TZS ••••••")
                    v.setTextViewText(R.id.tv_income_medium,  "↑ ••••")
                    v.setTextViewText(R.id.tv_expense_medium, "↓ ••••")
                } else {
                    v.setTextViewText(R.id.tv_balance_medium, WidgetDataLoader.fmtTzs(data.balance))
                    v.setTextViewText(R.id.tv_income_medium,  "↑ ${WidgetDataLoader.fmtTzs(data.income)}")
                    v.setTextViewText(R.id.tv_expense_medium, "↓ ${WidgetDataLoader.fmtTzs(data.expenses)}")
                }

                // ── Transaction rows ─────────────────────────────────────────────
                val txRows = listOf(
                    TxIds(R.id.tx1_type_icon, R.id.tx1_source, R.id.tx1_amount, R.id.tx1_date),
                    TxIds(R.id.tx2_type_icon, R.id.tx2_source, R.id.tx2_amount, R.id.tx2_date),
                    TxIds(R.id.tx3_type_icon, R.id.tx3_source, R.id.tx3_amount, R.id.tx3_date)
                )
                val dateFmt = SimpleDateFormat("dd MMM, HH:mm", locale)

                txRows.forEachIndexed { i, ids ->
                    val tx = data.recent.getOrNull(i)
                    if (tx != null) {
                        val isDeposit = tx.type == "DEPOSIT"
                        val rowAccent = if (isDeposit) accentColor else expColor
                        v.setTextViewText(ids.icon,   if (isDeposit) "↑" else "↓")
                        v.setTextColor(ids.icon,       rowAccent)
                        v.setTextViewText(ids.source,  tx.source)
                        v.setTextColor(ids.source,     textPrimary)
                        val amtText = if (privacy) "••••"
                        else "${if (isDeposit) "+" else "-"} ${WidgetDataLoader.fmtTzs(tx.amount)}"
                        v.setTextViewText(ids.amount,  amtText)
                        v.setTextColor(ids.amount,     rowAccent)
                        v.setTextViewText(ids.date,    if (privacy) "" else dateFmt.format(Date(tx.date)))
                        v.setTextColor(ids.date,       textMuted)
                    } else {
                        listOf(ids.icon, ids.source, ids.amount, ids.date)
                            .forEach { id -> v.setTextViewText(id, "") }
                    }
                }

                v.setOnClickPendingIntent(R.id.widget_medium_root, makeIntent(appCtx, appWidgetId))

                // ── Push fully built views, then fade in ─────────────────────────
                appWidgetManager.updateAppWidget(appWidgetId, v)

                // Smooth fade-in via alpha steps (fade the root, it's now a real LinearLayout)
                delay(50)
                val steps = listOf(0.4f, 0.65f, 0.82f, 0.93f, 1f)
                steps.forEach { alpha ->
                    delay(25)
                    val fade = RemoteViews(appCtx.packageName, R.layout.widget_medium)
                    fade.setFloat(R.id.widget_medium_root, "setAlpha", alpha)
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetId, fade)
                }

            } catch (_: Exception) {
                showError(appCtx, appWidgetManager, appWidgetId)
            }
        }
    }

    private data class TxIds(val icon: Int, val source: Int, val amount: Int, val date: Int)

    private fun showPlaceholder(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val v = RemoteViews(ctx.packageName, R.layout.widget_medium)
        val theme = resolveTheme(WidgetDataLoader.widgetThemeName(ctx))
        v.setInt(R.id.widget_medium_root, "setBackgroundResource", WidgetThemeHelper.bgDrawable(theme))
        v.setFloat(R.id.widget_medium_root, "setAlpha", 0.3f)
        v.setTextViewText(R.id.tv_balance_medium, "...")
        v.setOnClickPendingIntent(R.id.widget_medium_root, makeIntent(ctx, id))
        mgr.updateAppWidget(id, v)
    }

    private fun showError(ctx: Context, mgr: AppWidgetManager, id: Int) {
        try {
            val v = RemoteViews(ctx.packageName, R.layout.widget_medium)
            v.setInt(R.id.widget_medium_root, "setBackgroundResource",
                WidgetThemeHelper.bgDrawable(resolveTheme(WidgetDataLoader.widgetThemeName(ctx))))
            v.setFloat(R.id.widget_medium_root, "setAlpha", 1f)
            v.setTextViewText(R.id.tv_balance_medium, "TZS 0")
            v.setTextViewText(R.id.tv_income_medium,  "↑ 0")
            v.setTextViewText(R.id.tv_expense_medium, "↓ 0")
            v.setOnClickPendingIntent(R.id.widget_medium_root, makeIntent(ctx, id))
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