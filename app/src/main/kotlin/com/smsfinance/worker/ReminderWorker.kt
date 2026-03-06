package com.smsfinance.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.smsfinance.data.dao.RecurringTransactionDao
import com.smsfinance.data.dao.UserProfileDao
import com.smsfinance.ui.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Runs daily via WorkManager.
 * Checks recurring transactions and fires a reminder notification.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val recurringDao: RecurringTransactionDao,
    private val profileDao: UserProfileDao
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "daily_reminder_check"
        const val CHANNEL_REMINDERS = "transaction_reminders"
        private const val NOTIF_BASE_ID = 5000

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(calculateDelayToMorning(), TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /** Delay until 8 AM tomorrow */
        private fun calculateDelayToMorning(): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                if (get(Calendar.HOUR_OF_DAY) >= 8) add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            return maxOf(0L, target.timeInMillis - now.timeInMillis)
        }
    }

    override suspend fun doWork(): ListenableWorker.Result {
        return try {
            createNotificationChannel()
            checkAndNotify()
            ListenableWorker.Result.success()
        } catch (e: Exception) {
            ListenableWorker.Result.retry()
        }
    }

    private suspend fun checkAndNotify() {
        val activeProfile = profileDao.getActiveProfileOnce() ?: return
        val now = System.currentTimeMillis()

        // ✅ Use first() instead of collect{} — gets one emission and stops
        val activeList = recurringDao.getActive(activeProfile.id).first()

        val dateFmt = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())

        activeList.forEach { recurring ->
            if (!recurring.reminderEnabled) return@forEach
            val nextExpected = recurring.nextExpected ?: return@forEach

            val daysUntilDue = (nextExpected - now) / (1000 * 60 * 60 * 24)
            if (daysUntilDue in 0..recurring.reminderDaysBefore.toLong()) {
                val typeLabel = if (recurring.type == "DEPOSIT") "income" else "payment"
                val dueText = if (daysUntilDue == 0L) "due today" else "due in $daysUntilDue day(s)"
                val body = "${recurring.name} ($typeLabel) is $dueText — ${dateFmt.format(Date(nextExpected))}"

                showReminder(
                    id = NOTIF_BASE_ID + recurring.id.toInt(),
                    title = "💰 Payment Reminder",
                    body = body
                )
            }
        }
    }

    private fun showReminder(id: Int, title: String, body: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "recurring")
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(id, notification)
        } catch (_: SecurityException) {}
    }

    private fun createNotificationChannel() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Transaction Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for upcoming recurring transactions"
            enableVibration(true)
        }
        notificationManager?.createNotificationChannel(channel)
    }
}
