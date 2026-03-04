package com.smsfinance.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun PinScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onAuthenticated: () -> Unit,
    onBiometricRequest: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var shakeOffset by remember { mutableStateOf(0f) }
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()

    val shakeAnim by animateFloatAsState(
        targetValue = shakeOffset,
        animationSpec = spring(dampingRatio = 0.3f, stiffness = 800f),
        label = "shake"
    )

    Box(
        Modifier.fillMaxSize()
            .background(BgPrimary)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(AccentTeal.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.25f),
                        radius = size.width * 0.7f
                    )
                )
            }
    ) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo area
            Box(
                Modifier.size(80.dp).clip(RoundedCornerShape(24.dp))
                    .background(AccentTeal.copy(alpha = 0.15f))
                    .drawBehind {
                        val sw = 1.5f
                        drawRoundRect(
                            color = AccentTeal.copy(alpha = 0.4f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(sw)
                        )
                    },
                Alignment.Center
            ) { Text("💎", fontSize = 36.sp) }

            Spacer(Modifier.height(24.dp))
            Text("Smart Money", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold, color = TextWhite)
            Spacer(Modifier.height(6.dp))
            Text("Enter your PIN to continue", color = TextSecondary, fontSize = 14.sp)

            Spacer(Modifier.height(40.dp))

            // PIN dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.offset(x = shakeAnim.dp)
            ) {
                repeat(6) { i ->
                    val filled = i < pin.length
                    val dotScale by animateFloatAsState(
                        if (filled) 1.2f else 1f,
                        spring(dampingRatio = 0.5f), label = "dot$i"
                    )
                    Box(
                        Modifier.size(14.dp).scale(dotScale)
                            .background(
                                if (filled) AccentTeal else Color(0xFF3A4558),
                                CircleShape
                            )
                    )
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Surface(color = ErrorRed.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Text(it, color = ErrorRed, fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }

            Spacer(Modifier.height(44.dp))

            // Number pad
            val rows = listOf(
                listOf("1","2","3"), listOf("4","5","6"),
                listOf("7","8","9"), listOf("bio","0","del")
            )
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.padding(vertical = 7.dp)) {
                    row.forEach { key ->
                        when (key) {
                            "del" -> PinKey(Modifier.size(76.dp), isSpecial = true, onClick = {
                                if (pin.isNotEmpty()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    pin = pin.dropLast(1); errorMessage = null
                                }
                            }) { Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = TextSecondary) }

                            "bio" -> if (biometricEnabled) {
                                PinKey(Modifier.size(76.dp), isSpecial = true,
                                    onClick = onBiometricRequest) {
                                    Icon(Icons.Default.Fingerprint, null, tint = AccentTeal)
                                }
                            } else { Spacer(Modifier.size(76.dp)) }

                            else -> PinKey(Modifier.size(76.dp), onClick = {
                                if (pin.length < 6) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    pin += key; errorMessage = null
                                    if (pin.length >= 4) {
                                        scope.launch {
                                            if (viewModel.verifyPin(pin)) {
                                                onAuthenticated()
                                            } else if (pin.length == 6) {
                                                errorMessage = "Incorrect PIN"
                                                shakeOffset = 10f
                                                kotlinx.coroutines.delay(400)
                                                shakeOffset = 0f; pin = ""
                                            }
                                        }
                                    }
                                }
                            }) { Text(key, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = TextWhite) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKey(
    modifier: Modifier,
    isSpecial: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = if (isSpecial) Color(0xFF252F40) else Color(0xFF2C3546),
        tonalElevation = 0.dp
    ) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { content() }
    }
}
