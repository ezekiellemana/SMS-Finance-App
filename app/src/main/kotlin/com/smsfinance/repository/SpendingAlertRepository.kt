package com.smsfinance.repository

import com.smsfinance.data.dao.SpendingAlertDao
import com.smsfinance.data.entity.SpendingAlertEntity
import com.smsfinance.domain.model.AlertPeriod
import com.smsfinance.domain.model.AlertCheckResult
import com.smsfinance.domain.model.SpendingAlert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendingAlertRepository @Inject constructor(
    private val dao: SpendingAlertDao,
    private val transactionRepository: TransactionRepository
) {

    fun getAllAlerts(): Flow<List<SpendingAlert>> =
        dao.getAllAlerts().map { it.map { e -> e.toDomain() } }

    suspend fun insertAlert(alert: SpendingAlert): Long =
        dao.insertAlert(alert.toEntity())

    suspend fun updateAlert(alert: SpendingAlert) =
        dao.updateAlert(alert.toEntity())

    suspend fun deleteAlert(alert: SpendingAlert) =
        dao.deleteAlert(alert.toEntity())

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    /**
     * Check all enabled alerts against current spending.
     * Returns list of triggered alerts with progress details.
     */
    suspend fun checkAllAlerts(): List<AlertCheckResult> {
        val enabledAlerts = dao.getEnabledAlerts()
        return enabledAlerts.map { entity ->
            val alert = entity.toDomain()
            val (start, end) = getDateRangeForPeriod(alert.period)
            // Use first() to get snapshot value
            var spending = 0.0
            transactionRepository.getTotalExpenses(start, end).collect { spending = it }
            val percent = if (alert.limitAmount > 0) (spending / alert.limitAmount) * 100 else 0.0
            AlertCheckResult(
                alert = alert,
                currentSpending = spending,
                percentUsed = percent,
                isTriggered = percent >= alert.notifyAtPercent,
                remaining = maxOf(0.0, alert.limitAmount - spending)
            )
        }
    }

    /**
     * Get spending progress for a specific alert (as Flow for UI).
     */
    fun getAlertProgress(alert: SpendingAlert): Flow<AlertCheckResult> {
        val (start, end) = getDateRangeForPeriod(alert.period)
        return transactionRepository.getTotalExpenses(start, end).map { spending ->
            val percent = if (alert.limitAmount > 0) (spending / alert.limitAmount) * 100 else 0.0
            AlertCheckResult(
                alert = alert,
                currentSpending = spending,
                percentUsed = percent,
                isTriggered = percent >= alert.notifyAtPercent,
                remaining = maxOf(0.0, alert.limitAmount - spending)
            )
        }
    }

    private fun getDateRangeForPeriod(period: AlertPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        when (period) {
            AlertPeriod.DAILY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
            }
            AlertPeriod.WEEKLY -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
            }
            AlertPeriod.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
            }
        }
        return Pair(cal.timeInMillis, end)
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun SpendingAlertEntity.toDomain() = SpendingAlert(
        id = id,
        name = name,
        limitAmount = limitAmount,
        period = AlertPeriod.fromString(period),
        isEnabled = isEnabled,
        notifyAtPercent = notifyAtPercent,
        createdAt = createdAt
    )

    private fun SpendingAlert.toEntity() = SpendingAlertEntity(
        id = id,
        name = name,
        limitAmount = limitAmount,
        period = period.name,
        isEnabled = isEnabled,
        notifyAtPercent = notifyAtPercent,
        createdAt = createdAt
    )
}
