package com.smsfinance.domain.model

/**
 * Widget color theme presets.
 * SMART_DARK is now the default — matches the app brand palette.
 */
enum class WidgetTheme(
    val displayName: String,
    val emoji: String,
    val bgColorStart: Int,
    val bgColorEnd: Int,
    val textColor: Int,
    val accentColor: Int
) {
    SMART_DARK(
        "Smart Dark", "💎",
        0xFF1F2633.toInt(), 0xFF2C3546.toInt(),
        0xFFFFFFFF.toInt(), 0xFF3DDAD7.toInt()
    ),
    TEAL_FRESH(
        "Teal", "🦚",
        0xFF0D2E2E.toInt(), 0xFF1A3A3A.toInt(),
        0xFFFFFFFF.toInt(), 0xFF5CE1E6.toInt()
    ),
    BLUE_OCEAN(
        "Ocean", "🌊",
        0xFF0D47A1.toInt(), 0xFF1565C0.toInt(),
        0xFFFFFFFF.toInt(), 0xFF82B1FF.toInt()
    ),
    PURPLE_NIGHT(
        "Night", "🌙",
        0xFF1A0A2E.toInt(), 0xFF2D1054.toInt(),
        0xFFFFFFFF.toInt(), 0xFFCE93D8.toInt()
    ),
    SUNSET_ORANGE(
        "Sunset", "🌅",
        0xFF2A1400.toInt(), 0xFF3D1F00.toInt(),
        0xFFFFFFFF.toInt(), 0xFFFFCC80.toInt()
    ),
    ROSE_GOLD(
        "Rose Gold", "🌹",
        0xFF2A0A10.toInt(), 0xFF3D0A20.toInt(),
        0xFFFFFFFF.toInt(), 0xFFFF80AB.toInt()
    ),
    DARK_MINIMAL(
        "Dark", "🖤",
        0xFF0D0D0D.toInt(), 0xFF1A1A1A.toInt(),
        0xFFFFFFFF.toInt(), 0xFF3DDAD7.toInt()
    ),
    LIGHT_CLEAN(
        "Light", "☀️",
        0xFFEEF4FB.toInt(), 0xFFFFFFFF.toInt(),
        0xFF1A2233.toInt(), 0xFF1A9E9B.toInt()   // dark navy text, deeper teal accent for contrast
    ),
    GREEN_DARK(
        "Forest", "🌿",
        0xFF0D2010.toInt(), 0xFF1A3A1E.toInt(),
        0xFFFFFFFF.toInt(), 0xFF69F0AE.toInt()
    )
}