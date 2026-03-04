package com.smsfinance.domain.model

enum class InvestmentType(val label: String, val icon: String) {
    SAVINGS_GOAL("Savings Goal", "🎯"),
    FIXED_DEPOSIT("Fixed Deposit", "🏦"),
    SHARES("Shares / Stocks", "📊"),
    BONDS("Bonds / Treasury", "📜"),
    REAL_ESTATE("Real Estate", "🏠"),
    BUSINESS("Business", "🏪"),
    OTHER("Other", "💼")
}

data class Investment(
    val id: Long = 0,
    val userId: Long = 1L,
    val name: String,
    val icon: String = "📈",
    val color: String = "#00C853",
    val type: InvestmentType,
    val initialAmount: Double,
    val currentValue: Double,
    val targetAmount: Double? = null,
    val interestRate: Double = 0.0,
    val startDate: Long,
    val maturityDate: Long? = null,
    val institution: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val gain: Double get() = currentValue - initialAmount
    val gainPercent: Double get() = if (initialAmount > 0) (gain / initialAmount) * 100 else 0.0
    val isProfit: Boolean get() = gain >= 0
    val progressToTarget: Double get() = targetAmount?.let {
        if (it > 0) (currentValue / it).coerceIn(0.0, 1.0) else 0.0
    } ?: 0.0

    /** Simple compound interest projection for next 12 months */
    fun projectValue(months: Int): Double {
        if (interestRate <= 0) return currentValue
        val monthlyRate = interestRate / 100.0 / 12.0
        return currentValue * Math.pow(1 + monthlyRate, months.toDouble())
    }
}
