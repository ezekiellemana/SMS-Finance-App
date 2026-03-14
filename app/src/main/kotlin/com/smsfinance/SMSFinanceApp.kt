package com.smsfinance

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.smsfinance.service.SmsMonitorService
import com.smsfinance.util.LocaleHelper
import com.smsfinance.worker.ReminderWorker
import com.smsfinance.worker.SmsSyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SMSFinanceApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun attachBaseContext(base: Context) {
        val prefs = base.getSharedPreferences("app_language", MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en"
        super.attachBaseContext(LocaleHelper.wrap(base, lang))
    }

    override fun onCreate() {
        super.onCreate()
        ReminderWorker.schedule(this)
        SmsSyncWorker.schedule(this)   // periodic inbox scan every 15 min
        SmsMonitorService.start(this)  // foreground service keeps process warm
    }
}