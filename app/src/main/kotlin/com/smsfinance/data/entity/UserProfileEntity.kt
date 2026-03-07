package com.smsfinance.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a family member / user profile.
 * Each profile has its own transactions, budgets, and alerts
 * filtered by [id] stored in each related entity's userId column.
 */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    /** Emoji avatar e.g. "👨", "👩", "👦", "👧" */
    @ColumnInfo(name = "avatar_emoji")
    val avatarEmoji: String = "👤",

    /** Hex color for this profile card e.g. "#00C853" */
    @ColumnInfo(name = "color")
    val color: String = "#00C853",

    /** Whether this is the currently active profile */
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,

    /** Optional PIN hash specific to this profile (null = no lock) */
    @ColumnInfo(name = "pin_hash")
    val pinHash: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /** Local file URI for profile photo (copied into app-private storage) */
    @ColumnInfo(name = "photo_uri")
    val photoUri: String? = null
)