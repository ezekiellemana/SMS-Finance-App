package com.smsfinance.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single financial transaction
 * extracted from an SMS message.
 *
 * Supports 100k+ transactions via efficient indexing.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Transaction amount in TZS */
    @ColumnInfo(name = "amount")
    val amount: Double,

    /** "DEPOSIT" or "WITHDRAWAL" */
    @ColumnInfo(name = "type", index = true)
    val type: String,

    /** Source bank/service: NMB, CRDB, M-Pesa, Mixx */
    @ColumnInfo(name = "source")
    val source: String,

    /** Unix timestamp in milliseconds */
    @ColumnInfo(name = "date", index = true)
    val date: Long,

    /** Full or partial SMS message for reference */
    @ColumnInfo(name = "description")
    val description: String,

    /** Whether the user manually edited this record */
    @ColumnInfo(name = "is_manual")
    val isManual: Boolean = false,

    /** Reference/transaction number if available */
    @ColumnInfo(name = "reference")
    val reference: String = "",

    /** Category: Mobile Money, Bank, Loan, Transfer, Cash Out */
    @ColumnInfo(name = "category")
    val category: String = ""
)