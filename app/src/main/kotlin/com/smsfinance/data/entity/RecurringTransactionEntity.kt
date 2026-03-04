package com.smsfinance.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A detected or manually created recurring transaction pattern.
 * e.g. "Salary from NMB every 1st of month — ~TZS 1,200,000"
 */
@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long = 1L,

    /** Display name e.g. "Monthly Salary", "Rent Payment" */
    @ColumnInfo(name = "name")
    val name: String,

    /** Icon emoji */
    @ColumnInfo(name = "icon")
    val icon: String = "🔄",

    /** DEPOSIT or WITHDRAWAL */
    @ColumnInfo(name = "type")
    val type: String,

    /** Expected amount (average from history) */
    @ColumnInfo(name = "expected_amount")
    val expectedAmount: Double,

    /** Source/sender e.g. "NMB Bank" */
    @ColumnInfo(name = "source")
    val source: String,

    /**
     * Frequency: DAILY | WEEKLY | MONTHLY | YEARLY
     */
    @ColumnInfo(name = "frequency")
    val frequency: String = "MONTHLY",

    /** Day of month (1–31) for MONTHLY, or day of week (1–7) for WEEKLY */
    @ColumnInfo(name = "day_of_period")
    val dayOfPeriod: Int = 1,

    /** Whether to send a reminder notification before due date */
    @ColumnInfo(name = "reminder_enabled")
    val reminderEnabled: Boolean = true,

    /** How many days before to remind */
    @ColumnInfo(name = "reminder_days_before")
    val reminderDaysBefore: Int = 1,

    /** Keyword to auto-match incoming SMS to this recurring tx */
    @ColumnInfo(name = "match_keyword")
    val matchKeyword: String = "",

    /** Was this auto-detected from transaction history? */
    @ColumnInfo(name = "auto_detected")
    val autoDetected: Boolean = false,

    /** Timestamp of last matched transaction */
    @ColumnInfo(name = "last_occurrence")
    val lastOccurrence: Long? = null,

    /** Next expected date (millis) */
    @ColumnInfo(name = "next_expected")
    val nextExpected: Long? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
