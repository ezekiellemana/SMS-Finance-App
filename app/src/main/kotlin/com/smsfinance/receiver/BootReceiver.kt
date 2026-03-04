package com.smsfinance.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smsfinance.widget.WidgetUpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Restores home screen widgets after device reboot.
 * Android clears widget data on restart, so we need to re-populate.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var widgetUpdateManager: WidgetUpdateManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                widgetUpdateManager.updateAllWidgets(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
