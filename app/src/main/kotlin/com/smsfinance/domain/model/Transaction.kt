package com.smsfinance.domain.model

/**
 * Domain model for a financial transaction.
 * Decoupled from Room entity for clean architecture.
 */
data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val source: String,
    val date: Long,
    val description: String,
    val isManual: Boolean = false,
    val reference: String = "",
    val category: String = ""
)

enum class TransactionType(val label: String) {
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal");

    companion object {
        fun fromString(value: String): TransactionType {
            return when (value.uppercase()) {
                "DEPOSIT" -> DEPOSIT
                else -> WITHDRAWAL
            }
        }
    }
}

/**
 * Summary data for dashboard display and widget.
 */
data class FinancialSummary(
    val estimatedBalance: Double,
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val transactionCount: Int
)

/**
 * Filter options for transaction list.
 */
data class TransactionFilter(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val type: TransactionType? = null,
    val source: String? = null
)

/**
 * Chart data point for analytics.
 */
data class ChartDataPoint(
    val label: String,
    val income: Float,
    val expense: Float
)