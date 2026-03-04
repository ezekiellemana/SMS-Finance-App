package com.smsfinance.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smsfinance.R
import com.smsfinance.domain.model.AlertCheckResult
import com.smsfinance.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages all app notifications:
 * - Spending alert notifications (when limit is reached)
 * - New transaction detected notifications
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ALERTS = "spending_alerts"
        const val CHANNEL_TRANSACTIONS = "new_transactions"
        private const val ALERT_NOTIFICATION_BASE_ID = 1000
        private const val TRANSACTION_NOTIFICATION_ID = 2000
    }

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels (required for Android 8+).
     */
    private fun createNotificationChannels() {
        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            context.getString(R.string.notification_channel_alerts),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when spending limits are reached"
            enableVibration(true)
        }

        val txChannel = NotificationChannel(
            CHANNEL_TRANSACTIONS,
            context.getString(R.string.notification_channel_transactions),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifies when a new financial SMS is detected"
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(alertChannel)
        manager.createNotificationChannel(txChannel)
    }

    /**
     * Fire a notification for a triggered spending alert.
     */
    fun showSpendingAlertNotification(result: AlertCheckResult) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "alerts")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, result.alert.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val percentStr = "%.0f".format(result.percentUsed)
        val body = context.getString(
            R.string.alert_triggered_body,
            percentStr,
            result.alert.name
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.alert_triggered_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Use unique ID per alert so multiple alerts don't overwrite each other
        val notifId = ALERT_NOTIFICATION_BASE_ID + result.alert.id.toInt()
        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted — silently skip
        }
    }

    /**
     * Show a notification when a new SMS transaction is detected.
     */
    fun showNewTransactionNotification(source: String, amount: Double, isDeposit: Boolean) {
        val sign = if (isDeposit) "+" else "-"
        val body = "$sign TZS ${"%.0f".format(amount)} from $source"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSACTIONS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.new_transaction_detected))
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(TRANSACTION_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted — silently skip
        }
    }
}
