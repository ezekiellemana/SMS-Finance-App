package com.smsfinance.data.dao

import androidx.room.*
import com.smsfinance.data.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecurringTransactionEntity): Long

    @Update
    suspend fun update(entity: RecurringTransactionEntity)

    @Delete
    suspend fun delete(entity: RecurringTransactionEntity)

    @Query("SELECT * FROM recurring_transactions WHERE user_id = :userId ORDER BY name ASC")
    fun getAll(userId: Long): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE user_id = :userId AND is_active = 1")
    fun getActive(userId: Long): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: Long): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE user_id = :userId AND next_expected BETWEEN :from AND :to")
    suspend fun getDueInRange(userId: Long, from: Long, to: Long): List<RecurringTransactionEntity>

    @Query("UPDATE recurring_transactions SET last_occurrence = :ts, next_expected = :next WHERE id = :id")
    suspend fun updateOccurrence(id: Long, ts: Long, next: Long)
}
