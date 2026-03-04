package com.smsfinance.repository

import com.smsfinance.data.dao.DailyTotal
import com.smsfinance.data.dao.SourceTotal
import com.smsfinance.data.dao.TransactionDao
import com.smsfinance.data.entity.TransactionEntity
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository layer bridging Room entities and domain models.
 * Single source of truth for transaction data.
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val dao: TransactionDao
) {

    // ─── READ ─────────────────────────────────────────────────────────────────

    fun getAllTransactions(): Flow<List<Transaction>> =
        dao.getAllTransactions().map { entities -> entities.map { it.toDomain() } }

    fun getRecentTransactions(count: Int): Flow<List<Transaction>> =
        dao.getRecentTransactions(count).map { entities -> entities.map { it.toDomain() } }

    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<Transaction>> =
        dao.getTransactionsByDateRange(start, end).map { entities -> entities.map { it.toDomain() } }

    fun getTotalIncome(start: Long, end: Long): Flow<Double> =
        dao.getTotalIncome(start, end)

    fun getTotalExpenses(start: Long, end: Long): Flow<Double> =
        dao.getTotalExpenses(start, end)

    fun getTransactionCount(): Flow<Int> =
        dao.getTransactionCount()

    fun getIncomeBySource(start: Long, end: Long): Flow<List<SourceTotal>> =
        dao.getIncomeBySource(start, end)

    fun getExpensesBySource(start: Long, end: Long): Flow<List<SourceTotal>> =
        dao.getExpensesBySource(start, end)

    fun getDailyTotals(start: Long, end: Long): Flow<List<DailyTotal>> =
        dao.getDailyTotals(start, end)

    // ─── WRITE ────────────────────────────────────────────────────────────────

    suspend fun insertTransaction(transaction: Transaction): Long =
        dao.insertTransaction(transaction.toEntity())

    suspend fun updateTransaction(transaction: Transaction) =
        dao.updateTransaction(transaction.toEntity())

    suspend fun deleteTransaction(transaction: Transaction) =
        dao.deleteTransaction(transaction.toEntity())

    suspend fun deleteById(id: Long) =
        dao.deleteById(id)

    // ─── MAPPERS ──────────────────────────────────────────────────────────────

    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        amount = amount,
        type = TransactionType.fromString(type),
        source = source,
        date = date,
        description = description,
        isManual = isManual,
        reference = reference,
        category = category
    )

    private fun Transaction.toEntity() = TransactionEntity(
        id = id,
        amount = amount,
        type = type.name,
        source = source,
        date = date,
        description = description,
        isManual = isManual,
        reference = reference,
        category = category
    )
}