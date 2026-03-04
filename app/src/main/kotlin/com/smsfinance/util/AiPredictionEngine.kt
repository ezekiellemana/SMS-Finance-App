package com.smsfinance.util

import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * AI Spending Prediction Engine
 *
 * Uses statistical analysis on historical transaction data to produce:
 * 1. Next-month expense prediction (weighted moving average)
 * 2. Spending trend (increasing / decreasing / stable)
 * 3. Savings potential
 * 4. Top expense source
 * 5. Personalised savings tips
 *
 * Runs entirely on-device — no internet required.
 */
@Singleton
class AiPredictionEngine @Inject constructor() {

    data class PredictionResult(
        val predictedNextMonthExpense: Double,
        val predictedNextMonthIncome: Double,
        val trend: SpendingTrend,
        val avgDailySpending: Double,
        val avgMonthlyIncome: Double,
        val topExpenseSource: String,
        val savingsPotential: Double,
        val monthlyForecast: List<MonthForecast>,
        val savingsTip: String,
        val dataMonths: Int,
        val hasEnoughData: Boolean
    )

    data class MonthForecast(
        val label: String,   // "Jan", "Feb", etc.
        val income: Double,
        val expense: Double,
        val isPrediction: Boolean
    )

    enum class SpendingTrend { INCREASING, DECREASING, STABLE }

    /**
     * Main prediction entry point.
     * @param transactions Full transaction history
     */
    fun analyze(transactions: List<Transaction>): PredictionResult {
        if (transactions.size < 5) {
            return insufficientDataResult()
        }

        // ── Group transactions by month ───────────────────────────────────────
        val byMonth = groupByMonth(transactions)
        val months = byMonth.keys.sorted()

        if (months.size < 2) return insufficientDataResult()

        // ── Calculate monthly totals ──────────────────────────────────────────
        val monthlyIncome = months.associateWith { month ->
            byMonth[month]!!
                .filter { it.type == TransactionType.DEPOSIT }
                .sumOf { it.amount }
        }
        val monthlyExpense = months.associateWith { month ->
            byMonth[month]!!
                .filter { it.type == TransactionType.WITHDRAWAL }
                .sumOf { it.amount }
        }

        // ── Weighted moving average (recent months weigh more) ────────────────
        val weights = generateWeights(months.size)
        val predictedExpense = weightedAverage(months.map { monthlyExpense[it] ?: 0.0 }, weights)
        val predictedIncome = weightedAverage(months.map { monthlyIncome[it] ?: 0.0 }, weights)

        // ── Trend detection ───────────────────────────────────────────────────
        val trend = detectTrend(months, monthlyExpense)

        // ── Average daily spending ────────────────────────────────────────────
        val totalDays = maxOf(1, daysBetween(
            transactions.minOf { it.date },
            transactions.maxOf { it.date }
        ))
        val totalExpense = transactions.filter { it.type == TransactionType.WITHDRAWAL }.sumOf { it.amount }
        val avgDailySpending = totalExpense / totalDays

        // ── Average monthly income ────────────────────────────────────────────
        val avgMonthlyIncome = monthlyIncome.values.average()

        // ── Top expense source ────────────────────────────────────────────────
        val topSource = transactions
            .filter { it.type == TransactionType.WITHDRAWAL }
            .groupBy { it.source }
            .maxByOrNull { (_, txs) -> txs.sumOf { it.amount } }
            ?.key ?: "N/A"

        // ── Savings potential ─────────────────────────────────────────────────
        val savingsPotential = maxOf(0.0, predictedIncome - predictedExpense)

        // ── Build monthly forecast (past + next month) ─────────────────────────
        val forecast = buildForecast(months, monthlyIncome, monthlyExpense, predictedIncome, predictedExpense)

        // ── Generate personalised savings tip ─────────────────────────────────
        val tip = generateTip(trend, savingsPotential, avgDailySpending, topSource, predictedExpense)

        return PredictionResult(
            predictedNextMonthExpense = predictedExpense,
            predictedNextMonthIncome = predictedIncome,
            trend = trend,
            avgDailySpending = avgDailySpending,
            avgMonthlyIncome = avgMonthlyIncome,
            topExpenseSource = topSource,
            savingsPotential = savingsPotential,
            monthlyForecast = forecast,
            savingsTip = tip,
            dataMonths = months.size,
            hasEnoughData = true
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Group transactions into year-month buckets (key = "YYYY-MM") */
    private fun groupByMonth(transactions: List<Transaction>): Map<String, List<Transaction>> {
        return transactions.groupBy { tx ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = tx.date
            "%04d-%02d".format(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1
            )
        }
    }

    /** Generate exponentially increasing weights (most recent = highest weight) */
    private fun generateWeights(count: Int): List<Double> {
        val raw = (1..count).map { it.toDouble() }
        val sum = raw.sum()
        return raw.map { it / sum }
    }

    /** Weighted average of a list using given weights */
    private fun weightedAverage(values: List<Double>, weights: List<Double>): Double {
        return values.zip(weights).sumOf { (v, w) -> v * w }
    }

    /** Detect if spending is going up, down, or staying flat */
    private fun detectTrend(
        months: List<String>,
        monthlyExpense: Map<String, Double>
    ): SpendingTrend {
        if (months.size < 2) return SpendingTrend.STABLE
        val recent = monthlyExpense[months.last()] ?: 0.0
        val previous = monthlyExpense[months[months.size - 2]] ?: 0.0
        val changePercent = if (previous > 0) ((recent - previous) / previous) * 100 else 0.0
        return when {
            changePercent > 10 -> SpendingTrend.INCREASING
            changePercent < -10 -> SpendingTrend.DECREASING
            else -> SpendingTrend.STABLE
        }
    }

    /** Number of days between two timestamps */
    private fun daysBetween(start: Long, end: Long): Int {
        return ((end - start) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
    }

    /** Build forecast list: last 5 real months + 1 predicted month */
    private fun buildForecast(
        months: List<String>,
        income: Map<String, Double>,
        expense: Map<String, Double>,
        predIncome: Double,
        predExpense: Double
    ): List<MonthForecast> {
        val result = mutableListOf<MonthForecast>()

        // Last up to 5 real months
        months.takeLast(5).forEach { key ->
            val parts = key.split("-")
            val cal = Calendar.getInstance()
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, 1)
            val label = android.text.format.DateFormat.format("MMM", cal.time).toString()
            result.add(
                MonthForecast(
                    label = label,
                    income = income[key] ?: 0.0,
                    expense = expense[key] ?: 0.0,
                    isPrediction = false
                )
            )
        }

        // Next month prediction
        val nextCal = Calendar.getInstance()
        nextCal.add(Calendar.MONTH, 1)
        val nextLabel = android.text.format.DateFormat.format("MMM", nextCal.time).toString()
        result.add(
            MonthForecast(
                label = "$nextLabel*",
                income = predIncome,
                expense = predExpense,
                isPrediction = true
            )
        )
        return result
    }

    /** Generate a practical savings tip based on analysis */
    private fun generateTip(
        trend: SpendingTrend,
        savingsPotential: Double,
        avgDaily: Double,
        topSource: String,
        predictedExpense: Double
    ): String {
        return when {
            trend == SpendingTrend.INCREASING ->
                "Your spending increased this month. Consider reviewing your $topSource transactions — they're your top expense source."
            savingsPotential > 50_000 ->
                "You could save TZS ${"%.0f".format(savingsPotential)} next month. Try setting a spending alert for $topSource to track progress."
            avgDaily > 30_000 ->
                "Your daily spending averages TZS ${"%.0f".format(avgDaily)}. Small daily cuts can save TZS ${"%.0f".format(avgDaily * 0.1 * 30)} per month."
            trend == SpendingTrend.DECREASING ->
                "Great work! Your spending is trending down. Keep it up by monitoring your $topSource spending regularly."
            else ->
                "Your finances look stable. Consider setting a savings goal — even TZS ${"%.0f".format(predictedExpense * 0.05)} per month adds up over time."
        }
    }

    private fun insufficientDataResult() = PredictionResult(
        predictedNextMonthExpense = 0.0,
        predictedNextMonthIncome = 0.0,
        trend = SpendingTrend.STABLE,
        avgDailySpending = 0.0,
        avgMonthlyIncome = 0.0,
        topExpenseSource = "",
        savingsPotential = 0.0,
        monthlyForecast = emptyList(),
        savingsTip = "",
        dataMonths = 0,
        hasEnoughData = false
    )
}
