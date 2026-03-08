package com.smsfinance.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.smsfinance.domain.model.TransactionType
import com.smsfinance.repository.SpendingAlertRepository
import com.smsfinance.repository.TransactionRepository
import com.smsfinance.util.NotificationHelper
import com.smsfinance.util.SmsPatternEngine
import com.smsfinance.widget.WidgetUpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Instant SMS transaction detector.
 *
 * Each incoming financial SMS is parsed, deduplicated, saved, and reflected
 * in the widget within the same BroadcastReceiver window via goAsync().
 *
 * Deduplication by (amount, type, timestamp) prevents double-saving when
 * the Dashboard refresh scanner runs concurrently.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    private val tag = "SmsReceiver"

    @Inject lateinit var repository: TransactionRepository
    @Inject lateinit var alertRepository: SpendingAlertRepository
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var widgetUpdateManager: WidgetUpdateManager

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            action != "android.provider.Telephony.SMS_DELIVER") return

        // SDK_INT is always >= 26 in this project — use the typed API directly
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        // Group parts by sender so multipart SMS is reassembled before parsing
        val grouped = messages.groupBy { it.originatingAddress?.trim() ?: "" }

        grouped.forEach { (sender, parts) ->
            if (sender.isBlank()) return@forEach
            if (!SmsPatternEngine.isFinancialSender(sender)) return@forEach

            val fullBody  = parts.joinToString("") { it.messageBody ?: "" }
            val timestamp = parts.minOf { it.timestampMillis }
                .takeIf { it > 0L } ?: System.currentTimeMillis()

            if (fullBody.isBlank()) return@forEach

            // goAsync keeps the receiver alive past the 10-second window
            val pendingResult = goAsync()

            // SupervisorJob: a parse failure on one sender won't cancel others
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    handleSms(context, sender, fullBody, timestamp)
                } catch (e: Exception) {
                    Log.e(tag, "Error processing SMS from $sender: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun handleSms(
        context: Context,
        sender: String,
        body: String,
        timestamp: Long
    ) {
        val transaction = SmsPatternEngine.parse(sender, body, timestamp) ?: run {
            Log.d(tag, "No pattern matched [$sender]: ${body.take(60)}")
            return
        }

        // Skip if the inbox refresh scanner already saved this exact message
        if (repository.existsByAmountTypeDate(transaction.amount, transaction.type.name, transaction.date)) {
            Log.d(tag, "Dedup skip: ${transaction.type} ${transaction.amount}")
            return
        }

        repository.insertTransaction(transaction)
        Log.i(tag, "Saved: ${transaction.type} ${transaction.amount} from $sender")

        notificationHelper.showNewTransactionNotification(
            source    = transaction.source,
            amount    = transaction.amount,
            isDeposit = transaction.type == TransactionType.DEPOSIT
        )

        if (transaction.type == TransactionType.WITHDRAWAL) {
            runCatching {
                alertRepository.checkAllAlerts()
                    .filter { it.isTriggered }
                    .forEach { notificationHelper.showSpendingAlertNotification(it) }
            }
        }

        widgetUpdateManager.updateAllWidgets(context)
    }
}