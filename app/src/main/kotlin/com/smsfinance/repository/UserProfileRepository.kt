package com.smsfinance.repository

import com.smsfinance.data.dao.UserProfileDao
import com.smsfinance.data.entity.UserProfileEntity
import com.smsfinance.domain.model.UserProfile
import com.smsfinance.util.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val dao: UserProfileDao,
    private val prefs: PreferencesManager
) {
    fun getAllProfiles(): Flow<List<UserProfile>> =
        dao.getAllProfiles().map { list -> list.map { it.toDomain() } }

    fun getActiveProfile(): Flow<UserProfile?> =
        dao.getActiveProfile().map { it?.toDomain() }

    suspend fun getActiveProfileOnce(): UserProfile? =
        dao.getActiveProfileOnce()?.toDomain()

    suspend fun insertProfile(profile: UserProfile): Long =
        dao.insert(profile.toEntity())

    suspend fun updateProfile(profile: UserProfile) {
        dao.update(profile.toEntity())
        // If this is the active profile, mirror color change to widget prefs
        if (profile.isActive) prefs.mirrorProfileColorToWidgetPrefs(profile.color)
    }

    suspend fun deleteProfile(profile: UserProfile) =
        dao.delete(profile.toEntity())

    /** Switch active profile — deactivates all others, then mirrors color to widget SharedPrefs */
    suspend fun switchToProfile(id: Long) {
        dao.deactivateAll()
        dao.setActive(id)
        // Mirror the new active profile color so widgets pick it up on next update
        dao.getActiveProfileOnce()?.color?.let { prefs.mirrorProfileColorToWidgetPrefs(it) }
    }

    /**
     * Ensure at least one profile exists.
     * Uses the name entered during onboarding; falls back to "My Account"
     * if onboarding has not yet been completed.
     */
    suspend fun ensureDefaultProfile() {
        if (dao.getCount() == 0) {
            val displayName = prefs.getUserName().trim().ifEmpty { "My Account" }
            dao.insert(
                UserProfileEntity(
                    name        = displayName,
                    avatarEmoji = "👤",
                    color       = "#00C853",
                    isActive    = true
                )
            )
        }
        // Always mirror current active profile color on startup
        dao.getActiveProfileOnce()?.color?.let { prefs.mirrorProfileColorToWidgetPrefs(it) }
    }

    /**
     * Called right after onboarding saves the user's name.
     * Updates the active profile row so it shows the real name immediately,
     * even on existing installs where "My Account" was already in the DB.
     */
    suspend fun syncActiveProfileName() {
        val name = prefs.getUserName().trim().ifEmpty { return }
        val active = dao.getActiveProfileOnce() ?: return
        if (active.name != name) {
            dao.update(active.copy(name = name))
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