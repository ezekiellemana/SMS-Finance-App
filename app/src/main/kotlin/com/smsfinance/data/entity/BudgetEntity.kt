package com.smsfinance.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Monthly category budget.
 * e.g. "Food" = TZS 200,000/month for profile 1.
 */
@Entity(tableName = "budgets")
data class BudgetEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Profile this budget belongs to */
    @ColumnInfo(name = "user_id")
    val userId: Long = 1L,

    /** Category name e.g. "Food", "Transport", "Bills" */
    @ColumnInfo(name = "category")
    val category: String,

    /** Budget icon emoji */
    @ColumnInfo(name = "icon")
    val icon: String = "💰",

    /** Hex color for the category */
    @ColumnInfo(name = "color")
    val color: String = "#00C853",

    /** Monthly budget amount in TZS */
    @ColumnInfo(name = "amount")
    val amount: Double,

    /** Keywords to match SMS transactions to this category (comma-separated) */
    @ColumnInfo(name = "keywords")
    val keywords: String = "",

    /** Month this budget applies to (1–12), 0 = every month */
    @ColumnInfo(name = "month")
    val month: Int = 0,

    /** Year this budget applies to, 0 = every year */
    @ColumnInfo(name = "year")
    val year: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
