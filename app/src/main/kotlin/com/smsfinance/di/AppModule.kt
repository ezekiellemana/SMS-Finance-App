@file:Suppress("DEPRECATION")
package com.smsfinance.di

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.smsfinance.data.dao.*
import com.smsfinance.data.database.AppDatabase
import com.smsfinance.repository.*
import com.smsfinance.util.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Suppress("DEPRECATION")
    @Provides @Singleton
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(context, "sms_finance_secure_prefs", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase = AppDatabase.getInstance(context)

    @Provides @Singleton fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides @Singleton fun provideSpendingAlertDao(db: AppDatabase): SpendingAlertDao = db.spendingAlertDao()
    @Provides @Singleton fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()
    @Provides @Singleton fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides @Singleton fun provideRecurringTransactionDao(db: AppDatabase): RecurringTransactionDao = db.recurringTransactionDao()
    @Provides @Singleton fun provideInvestmentDao(db: AppDatabase): InvestmentDao = db.investmentDao()

    @Provides @Singleton
    fun provideTransactionRepository(dao: TransactionDao): TransactionRepository = TransactionRepository(dao)

    @Provides @Singleton
    fun provideSpendingAlertRepository(dao: SpendingAlertDao, txRepo: TransactionRepository): SpendingAlertRepository =
        SpendingAlertRepository(dao, txRepo)

    @Provides @Singleton
    fun provideBudgetRepository(dao: BudgetDao, txRepo: TransactionRepository): BudgetRepository =
        BudgetRepository(dao, txRepo)

    @Provides @Singleton
    fun provideRecurringTransactionRepository(dao: RecurringTransactionDao, txRepo: TransactionRepository): RecurringTransactionRepository =
        RecurringTransactionRepository(dao, txRepo)

    @Provides @Singleton
    fun provideInvestmentRepository(dao: InvestmentDao): InvestmentRepository = InvestmentRepository(dao)

    @Provides @Singleton fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper = NotificationHelper(context)
    @Provides @Singleton fun provideAiPredictionEngine(): AiPredictionEngine = AiPredictionEngine()
    @Provides @Singleton fun provideExportManager(@ApplicationContext context: Context): ExportManager = ExportManager(context)
    @Provides @Singleton fun provideCloudBackupManager(@ApplicationContext context: Context): CloudBackupManager = CloudBackupManager(context)
}