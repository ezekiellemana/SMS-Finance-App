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

/**
 * Medium home screen widget (4×3).
 * NO @AndroidEntryPoint — direct DB + SharedPrefs access for max compatibility.
 */
class MediumFinanceWidget : AppWidgetProvider() {

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
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, MediumFinanceWidget::class.java))
        onUpdate(context, manager, ids)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_medium)

        // ── Tap → open MainActivity ───────────────────────────────────────────
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_medium_root, pendingIntent)

        // Immediate placeholder
        appWidgetManager.updateAppWidget(appWidgetId, views)

        scope.launch {
            try {
                val prefs = context.getSharedPreferences("smart_money_prefs", Context.MODE_PRIVATE)
                val themeName = prefs.getString("widget_theme", WidgetTheme.SMART_DARK.name)
                val privacy   = prefs.getBoolean("privacy_mode", false)
                val theme = WidgetTheme.entries.firstOrNull { it.name == themeName }
                    ?: WidgetTheme.SMART_DARK

                // Apply background
                views.setInt(R.id.widget_medium_root, "setBackgroundColor",
                    blendColors(theme.bgColorStart, theme.bgColorEnd))

                val textColor    = theme.textColor
                val accentColor  = theme.accentColor
                val expenseColor = Color.parseColor("#FF5C5C")

                views.setTextColor(R.id.tv_balance_medium, textColor)
                views.setTextColor(R.id.tv_income_medium, accentColor)
                views.setTextColor(R.id.tv_expense_medium, expenseColor)

                if (privacy) {
                    views.setTextViewText(R.id.tv_balance_medium, "TZS ••••••")
                    views.setTextViewText(R.id.tv_income_medium, "↑ ••••")
                    views.setTextViewText(R.id.tv_expense_medium, "↓ ••••")
                } else {
                    val db = getDatabase(context)
                    val (start, end) = monthRange()
                    val income   = db.transactionDao().getTotalIncomeDirect(start, end) ?: 0.0
                    val expenses = db.transactionDao().getTotalExpensesDirect(start, end) ?: 0.0
                    val balance  = income - expenses

                    views.setTextViewText(R.id.tv_balance_medium, fmt(balance))
                    views.setTextViewText(R.id.tv_income_medium, "↑ ${fmt(income)}")
                    views.setTextViewText(R.id.tv_expense_medium, "↓ ${fmt(expenses)}")
                }

                // ── Last 3 transactions ───────────────────────────────────────
                val txRows = listOf(
                    TxViewIds(R.id.tx1_type_icon, R.id.tx1_source, R.id.tx1_amount, R.id.tx1_date),
                    TxViewIds(R.id.tx2_type_icon, R.id.tx2_source, R.id.tx2_amount, R.id.tx2_date),
                    TxViewIds(R.id.tx3_type_icon, R.id.tx3_source, R.id.tx3_amount, R.id.tx3_date)
                )

                val db = getDatabase(context)
                val transactions = db.transactionDao().getRecentTransactionsDirect(3)

                txRows.forEachIndexed { i, ids ->
                    val tx = transactions.getOrNull(i)
                    if (tx != null) {
                        val isDeposit = tx.type == "DEPOSIT"
                        val amtColor  = if (isDeposit) theme.accentColor else Color.parseColor("#FF5C5C")
                        val typeIcon  = if (isDeposit) "↑" else "↓"
                        val sign      = if (isDeposit) "+" else "-"

                        views.setTextViewText(ids.typeIconId, typeIcon)
                        views.setTextColor(ids.typeIconId, amtColor)
                        views.setTextViewText(ids.sourceId, tx.source)
                        views.setTextColor(ids.sourceId, textColor)

                        val amtText = if (privacy) "••••" else "$sign ${fmt(tx.amount)}"
                        views.setTextViewText(ids.amountId, amtText)
                        views.setTextColor(ids.amountId, amtColor)

                        views.setTextViewText(ids.dateId,
                            SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                                .format(Date(tx.date)))
                        views.setTextColor(ids.dateId, adjustAlpha(textColor, 0.6f))
                    } else {
                        views.setTextViewText(ids.sourceId, "")
                        views.setTextViewText(ids.amountId, "")
                        views.setTextViewText(ids.dateId, "")
                        views.setTextViewText(ids.typeIconId, "")
                    }
                }
            } catch (e: Exception) {
                views.setTextViewText(R.id.tv_balance_medium, "TZS ---")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private data class TxViewIds(
        val typeIconId: Int, val sourceId: Int,
        val amountId: Int,   val dateId: Int
    )

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

    private fun blendColors(c1: Int, c2: Int): Int {
        val r = (Color.red(c1) + Color.red(c2)) / 2
        val g = (Color.green(c1) + Color.green(c2)) / 2
        val b = (Color.blue(c1) + Color.blue(c2)) / 2
        return Color.argb(220, r, g, b)
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}

private fun getDatabase(context: Context) =
    com.smsfinance.data.database.AppDatabase.getInstance(context)