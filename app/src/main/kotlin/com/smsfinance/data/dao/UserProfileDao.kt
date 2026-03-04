package com.smsfinance.data.dao

import androidx.room.*
import com.smsfinance.data.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity): Long

    @Update
    suspend fun update(profile: UserProfileEntity)

    @Delete
    suspend fun delete(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles ORDER BY created_at ASC")
    fun getAllProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE is_active = 1 LIMIT 1")
    fun getActiveProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveProfileOnce(): UserProfileEntity?

    @Query("UPDATE user_profiles SET is_active = 0")
    suspend fun deactivateAll()

    @Query("UPDATE user_profiles SET is_active = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun getCount(): Int
}
