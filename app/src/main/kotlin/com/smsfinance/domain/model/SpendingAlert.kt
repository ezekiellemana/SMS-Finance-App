package com.smsfinance.domain.model

/**
 * Domain model for a spending alert.
 */
data class SpendingAlert(
    val id: Long = 0,
    val name: String,
    val limitAmount: Double,
    val period: AlertPeriod,
    val isEnabled: Boolean = true,
    val notifyAtPercent: Int = 80,
    val sourceFilter: String? = null,   // null = all sources, "MPESA" = M-Pesa only etc.
    /** Comma-separated keywords matched against description/reference (e.g. "shoprite,luku,carrefour") */
    val keywordFilter: String? = null,
    /** Match transactions tagged with this category (e.g. "Food", "Transport") */
    val categoryFilter: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AlertPeriod(val label: String, val labelSw: String) {
    DAILY("Daily", "Kila Siku"),
    WEEKLY("Weekly", "Kila Wiki"),
    MONTHLY("Monthly", "Kila Mwezi");

    companion object {
        fun fromString(value: String) = entries.firstOrNull {
            it.name == value
        } ?: MONTHLY
    }
}

/**
 * Result of checking an alert against current spending.
 */
data class AlertCheckResult(
    val alert: SpendingAlert,
    val currentSpending: Double,
    val percentUsed: Double,
    val isTriggered: Boolean,
    val remaining: Double
)