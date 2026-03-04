package com.smsfinance.domain.model

data class UserProfile(
    val id: Long = 0,
    val name: String,
    val avatarEmoji: String = "👤",
    val color: String = "#00C853",
    val isActive: Boolean = false,
    val pinHash: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

val DEFAULT_AVATARS = listOf("👨", "👩", "👦", "👧", "👴", "👵", "🧑", "👤")
val PROFILE_COLORS = listOf(
    "#00C853", "#2962FF", "#FF6D00", "#AA00FF",
    "#D50000", "#00BCD4", "#FF4081", "#546E7A"
)
