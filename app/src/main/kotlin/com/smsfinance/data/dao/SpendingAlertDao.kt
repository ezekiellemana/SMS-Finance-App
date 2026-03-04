package com.smsfinance.data.dao

import androidx.room.*
import com.smsfinance.data.entity.SpendingAlertEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for spending alerts.
 */
@Dao
interface SpendingAlertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: SpendingAlertEntity): Long

    @Update
    suspend fun updateAlert(alert: SpendingAlertEntity)

    @Delete
    suspend fun deleteAlert(alert: SpendingAlertEntity)

    @Query("DELETE FROM spending_alerts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM spending_alerts ORDER BY created_at DESC")
    fun getAllAlerts(): Flow<List<SpendingAlertEntity>>

    @Query("SELECT * FROM spending_alerts WHERE is_enabled = 1")
    suspend fun getEnabledAlerts(): List<SpendingAlertEntity>

    @Query("SELECT * FROM spending_alerts WHERE id = :id")
    suspend fun getAlertById(id: Long): SpendingAlertEntity?
}
