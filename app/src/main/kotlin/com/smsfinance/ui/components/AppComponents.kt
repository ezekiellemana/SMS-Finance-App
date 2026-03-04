package com.smsfinance.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smsfinance.ui.theme.AccentTeal
import com.smsfinance.ui.theme.BgSecondary
import com.smsfinance.ui.theme.TextSecondary

@Composable
fun ScreenEnterAnimation(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(380, easing = EaseOutCubic)) { it / 12 }
    ) { content() }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = AccentTeal,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = BgSecondary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A4558))
    ) { Column(content = content) }
}

@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(gradientColors))
    ) { content() }
}

@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    backgroundColor: Color,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    cornerRadius: Dp = 12.dp
) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(cornerRadius)).background(backgroundColor),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(iconSize)) }
}

@Composable
fun AppNavRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = AccentTeal,
    iconBg: Color = AccentTeal.copy(alpha = 0.12f),
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick()
        },
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconBadge(icon, iconTint, iconBg, size = 42.dp, iconSize = 20.dp)
                Column {
                    Text(title, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary)
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null,
                modifier = Modifier.size(14.dp), tint = TextSecondary)
        }
    }
}

@Composable
fun AppSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = AccentTeal,
    iconBg: Color = AccentTeal.copy(alpha = 0.12f),
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconBadge(icon, iconTint, iconBg, size = 42.dp, iconSize = 20.dp)
                Column {
                    Text(title, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Switch(
                checked = checked, onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = AccentTeal,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = Color(0xFF3A4558)
                )
            )
        }
    }
}

@Composable
fun PulsingDot(color: Color = AccentTeal, size: Dp = 8.dp) {
    val inf = rememberInfiniteTransition(label = "pulse")
    val alpha by inf.animateFloat(1f, 0.25f,
        infiniteRepeatable(tween(950, easing = EaseInOutSine), RepeatMode.Reverse), label = "a")
    Box(Modifier.size(size).background(color.copy(alpha = alpha), CircleShape))
}

@Composable
fun LabelDivider(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalDivider(Modifier.weight(1f), color = Color(0xFF3A4558))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        HorizontalDivider(Modifier.weight(1f), color = Color(0xFF3A4558))
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, fontSize = 52.sp)
        Text(title, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, style = MaterialTheme.typography.bodySmall,
            color = TextSecondary, textAlign = TextAlign.Center)
    }
}

fun fmtAmt(amount: Double): String =
    java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
        .apply { maximumFractionDigits = 0 }.format(amount)
