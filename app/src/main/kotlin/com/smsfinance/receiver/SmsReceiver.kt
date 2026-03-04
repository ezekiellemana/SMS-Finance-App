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
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Listens for incoming SMS messages.
 * On financial SMS: parses → saves → fires notification → checks alerts → updates widget.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    private val TAG = "SmsReceiver"

    @Inject lateinit var repository: TransactionRepository
    @Inject lateinit var alertRepository: SpendingAlertRepository
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var widgetUpdateManager: WidgetUpdateManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val grouped = messages.groupBy { it.originatingAddress ?: "" }

        grouped.forEach { (sender, parts) ->
            if (!SmsPatternEngine.isFinancialSender(sender)) return@forEach

            val fullBody = parts.joinToString("") { it.messageBody }
            val timestamp = parts.first().timestampMillis
            val pendingResult = goAsync()

            scope.launch {
                try {
                    val transaction = SmsPatternEngine.parse(sender, fullBody, timestamp)
                    if (transaction != null) {
                        repository.insertTransaction(transaction)
                        Log.i(TAG, "Saved: ${transaction.type} ${transaction.amount}")

                        // 🔔 New transaction notification
                        notificationHelper.showNewTransactionNotification(
                            source = transaction.source,
                            amount = transaction.amount,
                            isDeposit = transaction.type == TransactionType.DEPOSIT
                        )

                        // 🔔 Check spending alerts after every withdrawal
                        if (transaction.type == TransactionType.WITHDRAWAL) {
                            val results = alertRepository.checkAllAlerts()
                            results.filter { it.isTriggered }.forEach { result ->
                                notificationHelper.showSpendingAlertNotification(result)
                            }
                        }

                        // 📱 Refresh home screen widget
                        widgetUpdateManager.updateAllWidgets(context)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
