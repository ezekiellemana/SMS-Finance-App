package com.smsfinance.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.smsfinance.util.SmsHistoryImporter
import com.smsfinance.widget.WidgetUpdateManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager task — runs every 15 minutes.
 *
 * Scans the device SMS inbox for any financial messages that the
 * live BroadcastReceiver may have missed (e.g. when the device was off,
 * in flight mode, or the process was killed by the system).
 *
 * This is a safety net — the live BroadcastReceiver handles SMS in real-time.
 * WorkManager guarantees eventual execution even across reboots.
 */
@HiltWorker
class SmsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val smsHistoryImporter: SmsHistoryImporter,
    private val widgetUpdateManager: WidgetUpdateManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val newCount = smsHistoryImporter.refreshUnread()
            if (newCount > 0) widgetUpdateManager.updateAllWidgets(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "SmsSyncWork"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SmsSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)   // run even on low battery
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,   // don't reset timer if already scheduled
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}