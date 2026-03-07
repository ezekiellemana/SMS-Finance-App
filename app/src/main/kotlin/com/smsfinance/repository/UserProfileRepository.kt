package com.smsfinance.repository

import com.smsfinance.data.dao.UserProfileDao
import com.smsfinance.data.entity.UserProfileEntity
import com.smsfinance.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val dao: UserProfileDao
) {
    fun getAllProfiles(): Flow<List<UserProfile>> =
        dao.getAllProfiles().map { list -> list.map { it.toDomain() } }

    fun getActiveProfile(): Flow<UserProfile?> =
        dao.getActiveProfile().map { it?.toDomain() }

    suspend fun getActiveProfileOnce(): UserProfile? =
        dao.getActiveProfileOnce()?.toDomain()

    suspend fun insertProfile(profile: UserProfile): Long =
        dao.insert(profile.toEntity())

    suspend fun updateProfile(profile: UserProfile) =
        dao.update(profile.toEntity())

    suspend fun deleteProfile(profile: UserProfile) =
        dao.delete(profile.toEntity())

    /** Switch active profile — deactivates all others first */
    suspend fun switchToProfile(id: Long) {
        dao.deactivateAll()
        dao.setActive(id)
    }

    /** Ensure at least one "default" profile exists */
    suspend fun ensureDefaultProfile() {
        if (dao.getCount() == 0) {
            dao.insert(
                UserProfileEntity(
                    name = "My Account",
                    avatarEmoji = "👤",
                    color = "#00C853",
                    isActive = true
                )
            )
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────
    private fun UserProfileEntity.toDomain() = UserProfile(
        id, name, avatarEmoji, color, isActive, pinHash, createdAt, photoUri
    )
    private fun UserProfile.toEntity() = UserProfileEntity(
        id, name, avatarEmoji, color, isActive, pinHash, createdAt, photoUri
    )
}