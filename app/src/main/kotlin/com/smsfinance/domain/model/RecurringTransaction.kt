package com.smsfinance.domain.model

import com.smsfinance.domain.model.TransactionType

enum class RecurringFrequency(val label: String, val days: Int) {
    DAILY("Daily", 1),
    WEEKLY("Weekly", 7),
    MONTHLY("Monthly", 30),
    YEARLY("Yearly", 365)
}

data class RecurringTransaction(
    val id: Long = 0,
    val userId: Long = 1L,
    val name: String,
    val icon: String = "🔄",
    val type: TransactionType,
    val expectedAmount: Double,
    val source: String,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val dayOfPeriod: Int = 1,
    val reminderEnabled: Boolean = true,
    val reminderDaysBefore: Int = 1,
    val matchKeyword: String = "",
    val autoDetected: Boolean = false,
    val lastOccurrence: Long? = null,
    val nextExpected: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

val RECURRING_PRESETS = listOf(
    Triple("Monthly Salary", "💼", TransactionType.DEPOSIT),
    Triple("Rent Payment", "🏠", TransactionType.WITHDRAWAL),
    Triple("Electricity Bill", "💡", TransactionType.WITHDRAWAL),
    Triple("Water Bill", "💧", TransactionType.WITHDRAWAL),
    Triple("Internet Bill", "📡", TransactionType.WITHDRAWAL),
    Triple("School Fees", "📚", TransactionType.WITHDRAWAL),
    Triple("Insurance Premium", "🛡️", TransactionType.WITHDRAWAL),
    Triple("Loan Repayment", "🏦", TransactionType.WITHDRAWAL),
    Triple("Business Revenue", "📈", TransactionType.DEPOSIT),
    Triple("Weekly Allowance", "👛", TransactionType.DEPOSIT)
)
