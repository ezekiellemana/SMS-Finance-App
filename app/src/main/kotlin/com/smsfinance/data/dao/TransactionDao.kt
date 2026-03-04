package com.smsfinance.data.dao

import androidx.room.*
import com.smsfinance.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for financial transactions.
 * All queries return Flow for reactive UI updates.
 * Optimized for large datasets (100k+ transactions).
 */
@Dao
interface TransactionDao {

    // ─── INSERT / UPDATE / DELETE ────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ─── QUERIES ─────────────────────────────────────────────────────────────

    /** All transactions ordered by date descending */
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    /** Paginated query for large datasets */
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsPaged(limit: Int, offset: Int): List<TransactionEntity>

    /** Filter by date range */
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    /** Filter by type (DEPOSIT / WITHDRAWAL) */
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    /** Get last N transactions (for widget display) */
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :count")
    fun getRecentTransactions(count: Int): Flow<List<TransactionEntity>>

    /** Direct (non-Flow) for widget use — no Hilt required */
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :count")
    suspend fun getRecentTransactionsDirect(count: Int): List<TransactionEntity>

    /** Total income (deposits) in a date range */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'DEPOSIT' AND date BETWEEN :startDate AND :endDate")
    fun getTotalIncome(startDate: Long, endDate: Long): Flow<Double>

    /** Direct (non-Flow) for widget use */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'DEPOSIT' AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalIncomeDirect(startDate: Long, endDate: Long): Double?

    /** Total expenses (withdrawals) in a date range */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'WITHDRAWAL' AND date BETWEEN :startDate AND :endDate")
    fun getTotalExpenses(startDate: Long, endDate: Long): Flow<Double>

    /** Direct (non-Flow) for widget use */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'WITHDRAWAL' AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpensesDirect(startDate: Long, endDate: Long): Double?

    /** Count of all transactions */
    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCount(): Flow<Int>

    /** Income per source for analytics */
    @Query("SELECT source, SUM(amount) as total FROM transactions WHERE type = 'DEPOSIT' AND date BETWEEN :startDate AND :endDate GROUP BY source")
    fun getIncomeBySource(startDate: Long, endDate: Long): Flow<List<SourceTotal>>

    /** Expenses per source for analytics */
    @Query("SELECT source, SUM(amount) as total FROM transactions WHERE type = 'WITHDRAWAL' AND date BETWEEN :startDate AND :endDate GROUP BY source")
    fun getExpensesBySource(startDate: Long, endDate: Long): Flow<List<SourceTotal>>

    /** Daily totals for chart rendering */
    @Query("""
        SELECT 
            date / 86400000 as day,
            SUM(CASE WHEN type = 'DEPOSIT' THEN amount ELSE 0 END) as income,
            SUM(CASE WHEN type = 'WITHDRAWAL' THEN amount ELSE 0 END) as expense
        FROM transactions 
        WHERE date BETWEEN :startDate AND :endDate 
        GROUP BY day 
        ORDER BY day
    """)
    fun getDailyTotals(startDate: Long, endDate: Long): Flow<List<DailyTotal>>
    // ─── SEARCH ──────────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM transactions 
        WHERE (source LIKE '%' || :query || '%' 
            OR description LIKE '%' || :query || '%'
            OR reference LIKE '%' || :query || '%')
        ORDER BY date DESC LIMIT 200
    """)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE (source LIKE '%' || :query || '%' 
            OR description LIKE '%' || :query || '%')
        AND type = :type
        ORDER BY date DESC LIMIT 200
    """)
    fun searchByTypeAndQuery(query: String, type: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE amount BETWEEN :minAmt AND :maxAmt
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date DESC LIMIT 200
    """)
    fun searchByAmountRange(minAmt: Double, maxAmt: Double, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

}

/** Helper data class for aggregated source totals */
data class SourceTotal(
    val source: String,
    val total: Double
)

/** Helper data class for daily chart data */
data class DailyTotal(
    val day: Long,
    val income: Double,
    val expense: Double
)


