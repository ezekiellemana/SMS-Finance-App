package com.smsfinance.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
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

/**
 * Small home screen widget (2×2).
 * NO @AndroidEntryPoint — Hilt injection in AppWidgetProvider is unreliable
 * across launchers. Use application context + manual service locator instead.
 */
class SmallFinanceWidget : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Force update when widget is first added
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            android.content.ComponentName(context, SmallFinanceWidget::class.java)
        )
        onUpdate(context, manager, ids)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_small)

        // ── Tap → open MainActivity ───────────────────────────────────────────
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, appWidgetId,          // unique requestCode per widget ID
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_small_root, pendingIntent)

        // Push placeholder immediately so widget isn't blank while loading
        appWidgetManager.updateAppWidget(appWidgetId, views)

        scope.launch {
            try {
                // ── Get prefs directly — no Hilt needed ──────────────────────
                val prefs = context.getSharedPreferences("smart_money_prefs", Context.MODE_PRIVATE)
                val themeName  = prefs.getString("widget_theme", WidgetTheme.SMART_DARK.name)
                val privacy    = prefs.getBoolean("privacy_mode", false)
                val theme = WidgetTheme.entries.firstOrNull { it.name == themeName }
                    ?: WidgetTheme.SMART_DARK

                applyTheme(views, theme)

                if (privacy) {
                    views.setTextViewText(R.id.tv_balance, "TZS ••••••")
                    views.setTextViewText(R.id.tv_income, "••••")
                    views.setTextViewText(R.id.tv_expenses, "••••")
                    views.setTextViewText(R.id.tv_last_updated, "Privacy mode on")
                } else {
                    // ── Query DB directly via Room ────────────────────────────
                    val db = getDatabase(context)
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
                views.setTextViewText(R.id.tv_balance, "TZS ---")
                views.setTextViewText(R.id.tv_last_updated, "Tap to open")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun applyTheme(views: RemoteViews, theme: WidgetTheme) {
        views.setInt(R.id.widget_small_root, "setBackgroundColor",
            blendColors(theme.bgColorStart, theme.bgColorEnd))
        views.setTextColor(R.id.tv_balance_label, adjustAlpha(theme.textColor, 0.7f))
        views.setTextColor(R.id.tv_balance, theme.textColor)
        views.setTextColor(R.id.tv_income, theme.accentColor)
        views.setTextColor(R.id.tv_expenses, Color.parseColor("#FF5C5C"))
    }

    private fun monthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }

    private fun fmt(amount: Double): String =
        "TZS ${NumberFormat.getNumberInstance(Locale.US)
            .apply { maximumFractionDigits = 0 }.format(amount)}"

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun blendColors(c1: Int, c2: Int): Int {
        val r = (Color.red(c1) + Color.red(c2)) / 2
        val g = (Color.green(c1) + Color.green(c2)) / 2
        val b = (Color.blue(c1) + Color.blue(c2)) / 2
        return Color.argb(220, r, g, b)
    }
}

private fun getDatabase(context: Context) =
    com.smsfinance.data.database.AppDatabase.getInstance(context)