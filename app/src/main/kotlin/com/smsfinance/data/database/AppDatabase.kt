package com.smsfinance.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smsfinance.data.dao.*
import com.smsfinance.data.entity.*

@Database(
    entities = [
        TransactionEntity::class,
        SpendingAlertEntity::class,
        UserProfileEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        InvestmentEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun spendingAlertDao(): SpendingAlertDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun investmentDao(): InvestmentDao

    companion object {
        private const val DB_NAME = "sms_finance.db"
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, DB_NAME
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}