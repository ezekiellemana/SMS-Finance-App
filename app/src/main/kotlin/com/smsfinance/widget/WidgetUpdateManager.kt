package com.smsfinance.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Triggers a data refresh for every active home screen widget.
 *
 * Called from:
 *  - [com.smsfinance.receiver.SmsReceiver]  — after a new transaction is saved
 *  - [com.smsfinance.receiver.BootReceiver] — after device reboot
 *  - [com.smsfinance.viewmodel.SettingsViewModel] — when privacy mode or theme changes
 *
 * The broadcast causes each widget's onUpdate() to fire, which re-runs
 * the data loader coroutine and pushes fresh RemoteViews.
 */
@Singleton
class WidgetUpdateManager @Inject constructor() {

    fun updateAllWidgets(context: Context) {
        triggerUpdate(context, SmallFinanceWidget::class.java)
        triggerUpdate(context, MediumFinanceWidget::class.java)
    }

    private fun triggerUpdate(context: Context, widgetClass: Class<*>) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, widgetClass))
        if (ids.isEmpty()) return          // no widgets of this type on the home screen

        val intent = Intent(context, widgetClass).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }
}