package com.smsfinance.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Smart Money Design System ─────────────────────────────────────────────────
val BgPrimary       = Color(0xFF1F2633)   // Primary background
val BgSecondary     = Color(0xFF2C3546)   // Cards, surfaces
val AccentTeal      = Color(0xFF3DDAD7)   // Primary accent
val AccentLight     = Color(0xFF5CE1E6)   // Light accent / hover
val TextWhite       = Color(0xFFFFFFFF)   // Primary text
val TextSecondary   = Color(0xFFAAB4C3)   // Secondary text
val ErrorRed        = Color(0xFFFF5C5C)   // Error / expense

// Legacy aliases (used by charts and existing code)
val GreenPrimary    = AccentTeal
val GreenDark       = Color(0xFF2AB8B5)
val GreenLight      = AccentLight
val GreenSoft       = BgSecondary
val RedExpense      = ErrorRed
val RedSoft         = Color(0xFF3A2233)
val BlueAccent      = Color(0xFF3D8EF0)
val OrangeWarn      = Color(0xFFFFAB40)
val SurfaceDark     = BgPrimary
val SurfaceCardDark = BgSecondary

private val AppColorScheme = darkColorScheme(
    primary              = AccentTeal,
    onPrimary            = BgPrimary,
    primaryContainer     = Color(0xFF1E3A3A),
    onPrimaryContainer   = AccentLight,
    secondary            = AccentLight,
    onSecondary          = BgPrimary,
    secondaryContainer   = Color(0xFF1E3540),
    onSecondaryContainer = AccentLight,
    background           = BgPrimary,
    surface              = BgSecondary,
    surfaceVariant       = Color(0xFF2A3347),
    onSurface            = TextWhite,
    onSurfaceVariant     = TextSecondary,
    onBackground         = TextWhite,
    outline              = Color(0xFF3A4558),
    outlineVariant       = Color(0xFF2E3A4A),
    error                = ErrorRed,
    errorContainer       = Color(0xFF3A1F1F),
    tertiary             = OrangeWarn
)

val AppTypography = Typography(
    displayLarge   = TextStyle(fontWeight = FontWeight.Black,    fontSize = 57.sp, letterSpacing = (-0.25).sp),
    headlineLarge  = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 26.sp, letterSpacing = (-0.3).sp),
    headlineSmall  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = (-0.2).sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall     = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 14.sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 11.sp, letterSpacing = 0.5.sp)
)

@Composable
fun SMSFinanceTheme(
    darkTheme: Boolean = true,   // parameter kept for API compatibility; always dark
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = AppTypography,
        content     = content
    )
}