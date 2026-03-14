package com.smsfinance.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.smsfinance.R
import com.smsfinance.ui.MainActivity

/**
 * Foreground service that keeps the process warm for instant SMS detection.
 *
 * Android 8+ aggressively kills background processes. Running as a foreground
 * service prevents this — the SmsReceiver BroadcastReceiver fires instantly
 * even when the app has been backgrounded or the screen is off.
 *
 * The notification is PRIORITY_MIN so it collapses silently in the shade.
 */
class SmsMonitorService : Service() {

    companion object {
        const val CHANNEL_ID   = "sms_monitor_channel"
        const val NOTIF_ID     = 9001
        @Suppress("SpellCheckingInspection")
        const val ACTION_START = "com.smsfinance.START_MONITOR"
        @Suppress("SpellCheckingInspection")
        const val ACTION_STOP  = "com.smsfinance.STOP_MONITOR"

        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, SmsMonitorService::class.java).apply { action = ACTION_START }
            )
        }

        @Suppress("unused")  // Public API — may be called from settings to disable monitoring
        fun stop(context: Context) {
            context.stopService(Intent(context, SmsMonitorService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotification())
        return START_STICKY   // system restarts if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_splash_logo)
            .setContentTitle(getString(R.string.monitor_notif_title))
            .setContentText(getString(R.string.monitor_notif_body))
            .setContentIntent(open)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .build()
    }

    private fun createChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(NotificationChannel(
            CHANNEL_ID,
            getString(R.string.monitor_channel_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.monitor_channel_desc)
            setShowBadge(false)
        })
    }
}