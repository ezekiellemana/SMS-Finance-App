package com.smsfinance.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for a user-defined spending alert.
 * When spending in a period exceeds [limitAmount], a notification is fired.
 */
@Entity(tableName = "spending_alerts")
data class SpendingAlertEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** User-defined name e.g. "Monthly Food" */
    @ColumnInfo(name = "name")
    val name: String,

    /** Spending limit in TZS */
    @ColumnInfo(name = "limit_amount")
    val limitAmount: Double,

    /** DAILY / WEEKLY / MONTHLY */
    @ColumnInfo(name = "period")
    val period: String,

    /** Whether this alert is active */
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    /** Percentage threshold to notify at (default 80%) */
    @ColumnInfo(name = "notify_at_percent")
    val notifyAtPercent: Int = 80,

    /** Optional: only track spending from this source (e.g. "MPESA", null = all) */
    @ColumnInfo(name = "source_filter", defaultValue = "NULL")
    val sourceFilter: String? = null,

    @ColumnInfo(name = "keyword_filter", defaultValue = "NULL")
    val keywordFilter: String? = null,

    @ColumnInfo(name = "category_filter", defaultValue = "NULL")
    val categoryFilter: String? = null,

    /** Timestamp alert was created */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)