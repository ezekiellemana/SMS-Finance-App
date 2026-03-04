package com.smsfinance.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sending update broadcasts to all registered home screen widgets.
 * Holds ApplicationContext safely as a @Singleton.
 * Called after each new transaction is saved or settings change.
 */
@Singleton
class WidgetUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun updateAllWidgets() {
        sendWidgetUpdate(SmallFinanceWidget::class.java)
        sendWidgetUpdate(MediumFinanceWidget::class.java)
    }

    // Keep overload for callers that pass context (e.g. SmsReceiver)
    fun updateAllWidgets(ctx: Context) = updateAllWidgets()

    private fun sendWidgetUpdate(widgetClass: Class<*>) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, widgetClass))
        if (ids.isNotEmpty()) {
            val intent = Intent(context, widgetClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}