package com.smsfinance.data.dao

import androidx.room.*
import com.smsfinance.data.entity.InvestmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(e: InvestmentEntity): Long
    @Update suspend fun update(e: InvestmentEntity)
    @Delete suspend fun delete(e: InvestmentEntity)

    @Query("SELECT * FROM investments WHERE user_id = :userId ORDER BY created_at DESC")
    fun getAll(userId: Long): Flow<List<InvestmentEntity>>

    @Query("SELECT COALESCE(SUM(current_value), 0.0) FROM investments WHERE user_id = :userId")
    fun getTotalValue(userId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(initial_amount), 0.0) FROM investments WHERE user_id = :userId")
    fun getTotalInvested(userId: Long): Flow<Double>
}
