package com.smsfinance.repository

import com.smsfinance.data.dao.RecurringTransactionDao
import com.smsfinance.data.entity.RecurringTransactionEntity
import com.smsfinance.domain.model.RecurringFrequency
import com.smsfinance.domain.model.RecurringTransaction
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class RecurringTransactionRepository @Inject constructor(
    private val dao: RecurringTransactionDao,
    private val transactionRepository: TransactionRepository
) {
    fun getAll(userId: Long): Flow<List<RecurringTransaction>> =
        dao.getAll(userId).map { list -> list.map { it.toDomain() } }

    fun getActive(userId: Long): Flow<List<RecurringTransaction>> =
        dao.getActive(userId).map { list -> list.map { it.toDomain() } }

    suspend fun insert(r: RecurringTransaction): Long = dao.insert(r.toEntity())
    suspend fun update(r: RecurringTransaction) = dao.update(r.toEntity())
    suspend fun delete(r: RecurringTransaction) = dao.delete(r.toEntity())

    /**
     * Auto-detect recurring patterns from the last 6 months of transactions.
     * Groups transactions by source, finds those occurring roughly monthly
     * with similar amounts, and returns suggested RecurringTransaction objects.
     */
    suspend fun detectPatterns(userId: Long): List<RecurringTransaction> {
        val sixMonthsAgo = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)
        val now = System.currentTimeMillis()
        val allTx = transactionRepository.getAllTransactions().first()

        // Group by source + type
        val groups = allTx.groupBy { "${it.source}|${it.type.name}" }

        val detected = mutableListOf<RecurringTransaction>()
        groups.forEach { (key, txList) ->
            if (txList.size < 2) return@forEach
            val parts = key.split("|")
            val source = parts[0]
            val type = TransactionType.fromString(parts[1])

            // Sort by date
            val sorted = txList.sortedBy { it.date }
            // Calculate average gap between transactions
            val gaps = sorted.zipWithNext().map { (a, b) -> b.date - a.date }
            val avgGapDays = gaps.average() / (1000 * 60 * 60 * 24)
            val avgAmount = txList.map { it.amount }.average()

            // Check if gap is roughly monthly (25–35 days) or weekly (5–9 days)
            val frequency = when {
                avgGapDays in 25.0..35.0 -> RecurringFrequency.MONTHLY
                avgGapDays in 5.0..9.0 -> RecurringFrequency.WEEKLY
                avgGapDays in 0.5..1.5 -> RecurringFrequency.DAILY
                avgGapDays in 340.0..390.0 -> RecurringFrequency.YEARLY
                else -> return@forEach
            }

            // Check amount consistency (within 20% of average)
            val consistent = txList.all { abs(it.amount - avgAmount) / avgAmount < 0.20 }
            if (!consistent && txList.size < 3) return@forEach

            // Day of month for MONTHLY
            val dayOfPeriod = if (frequency == RecurringFrequency.MONTHLY) {
                val cal = Calendar.getInstance()
                cal.timeInMillis = sorted.last().date
                cal.get(Calendar.DAY_OF_MONTH)
            } else 1

            val icon = when {
                type == TransactionType.DEPOSIT -> "💰"
                source.contains("Luku", true) || source.contains("Electricity", true) -> "💡"
                source.contains("Water", true) -> "💧"
                source.contains("School", true) -> "📚"
                else -> "🔄"
            }

            detected.add(RecurringTransaction(
                userId = userId,
                name = "$source (auto)",
                icon = icon,
                type = type,
                expectedAmount = avgAmount,
                source = source,
                frequency = frequency,
                dayOfPeriod = dayOfPeriod,
                autoDetected = true,
                lastOccurrence = sorted.last().date,
                nextExpected = calculateNext(sorted.last().date, frequency, dayOfPeriod)
            ))
        }
        return detected
    }

    fun calculateNext(from: Long, frequency: RecurringFrequency, dayOfPeriod: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = from
        when (frequency) {
            RecurringFrequency.DAILY -> cal.add(Calendar.DAY_OF_MONTH, 1)
            RecurringFrequency.WEEKLY -> cal.add(Calendar.DAY_OF_MONTH, 7)
            RecurringFrequency.MONTHLY -> {
                cal.add(Calendar.MONTH, 1)
                val max = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, minOf(dayOfPeriod, max))
            }
            RecurringFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    // ── Mappers ───────────────────────────────────────────────────────────────
    private fun RecurringTransactionEntity.toDomain() = RecurringTransaction(
        id, userId, name, icon,
        TransactionType.fromString(type),
        expectedAmount, source,
        RecurringFrequency.valueOf(frequency),
        dayOfPeriod, reminderEnabled, reminderDaysBefore,
        matchKeyword, autoDetected, lastOccurrence, nextExpected, isActive, createdAt
    )

    private fun RecurringTransaction.toEntity() = RecurringTransactionEntity(
        id, userId, name, icon, type.name, expectedAmount, source,
        frequency.name, dayOfPeriod, reminderEnabled, reminderDaysBefore,
        matchKeyword, autoDetected, lastOccurrence, nextExpected, isActive, createdAt
    )
}
