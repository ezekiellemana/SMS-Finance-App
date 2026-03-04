package com.smsfinance.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long = 1L,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "icon") val icon: String = "📈",
    @ColumnInfo(name = "color") val color: String = "#00C853",
    @ColumnInfo(name = "type") val type: String,           // SAVINGS_GOAL | FIXED_DEPOSIT | SHARES | BONDS | REAL_ESTATE | OTHER
    @ColumnInfo(name = "initial_amount") val initialAmount: Double,
    @ColumnInfo(name = "current_value") val currentValue: Double,
    @ColumnInfo(name = "target_amount") val targetAmount: Double? = null,
    @ColumnInfo(name = "interest_rate") val interestRate: Double = 0.0,  // annual %
    @ColumnInfo(name = "start_date") val startDate: Long,
    @ColumnInfo(name = "maturity_date") val maturityDate: Long? = null,
    @ColumnInfo(name = "institution") val institution: String = "",
    @ColumnInfo(name = "notes") val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
