package com.smsfinance.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * CompositionLocal that carries the active user profile's accent colour
 * throughout the entire composition tree.
 *
 * Provided once at the root in MainActivity:
 *   CompositionLocalProvider(LocalProfileColor provides resolvedColor) { … }
 *
 * Read anywhere with:
 *   val profileColor = LocalProfileColor. current
 */
val LocalProfileColor = compositionLocalOf { Color(0xFF00C853) }