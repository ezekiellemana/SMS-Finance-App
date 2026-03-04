package com.smsfinance.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType
import com.smsfinance.ui.components.*
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToTransactions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCharts: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var privacyMode by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BgPrimary
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = AccentTeal, strokeWidth = 3.dp)
            }
            return@Scaffold
        }

        ScreenEnterAnimation {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PulsingDot(size = 9.dp)
                            Column {
                                Text("Smart Money", style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold, color = TextWhite)
                                Text("Auto-tracking active", fontSize = 11.sp, color = AccentTeal)
                            }
                        }
                        Row {
                            IconButton(onClick = { privacyMode = !privacyMode }) {
                                Icon(if (privacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = TextSecondary)
                            }
                            IconButton(onClick = onNavigateToCharts) {
                                Icon(Icons.Default.BarChart, null, tint = TextSecondary)
                            }
                            IconButton(onClick = onNavigateToSearch) {
                                Icon(Icons.Default.Search, null, tint = TextSecondary)
                            }
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, null, tint = TextSecondary)
                            }
                        }
                    }
                }
                item {
                    HeroBalanceCard(uiState.summary.estimatedBalance,
                        uiState.summary.monthlyIncome, uiState.summary.monthlyExpenses, privacyMode)
                }
                item { QuickActionsRow(onNavigateToTransactions, onNavigateToCharts) }
                if (uiState.chartData.isNotEmpty()) {
                    item { MiniBarChartCard(uiState.chartData, privacyMode) }
                }
                item {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Recent Activity", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = TextWhite)
                        TextButton(onClick = onNavigateToTransactions) {
                            Text("See all", color = AccentTeal, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(2.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                                Modifier.size(14.dp), AccentTeal)
                        }
                    }
                }
                if (uiState.recentTransactions.isEmpty()) {
                    item { EmptyState("📭", "No transactions yet",
                        "Financial SMS messages will appear here automatically") }
                } else {
                    items(uiState.recentTransactions.take(3)) { tx ->
                        TransactionRow(tx, privacyMode, onClick = onNavigateToTransactions)
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun HeroBalanceCard(balance: Double, income: Double, expenses: Double, privacyMode: Boolean) {
    val animBal by animateFloatAsState(balance.toFloat(), tween(1200, easing = EaseOutCubic), label = "bal")

    // Breathing glow — slow infinite pulse on the radial glow alpha
    val glowPulse = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glowPulse.animateFloat(
        initialValue = 0.12f, targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            tween(2800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "glowAlpha"
    )
    // Subtle card scale breathe
    val cardScale by glowPulse.animateFloat(
        initialValue = 1.000f, targetValue = 1.004f,
        animationSpec = infiniteRepeatable(
            tween(3200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "cardScale"
    )

    Box(
        modifier = Modifier.fillMaxWidth().height(215.dp)
            .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(
                listOf(Color(0xFF1A3040), Color(0xFF1E3A3A), Color(0xFF1F2E3A))
            ))
            .drawBehind {
                // Breathing teal glow top-right
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(AccentTeal.copy(alpha = glowAlpha), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.15f),
                        radius = size.width * 0.55f
                    )
                )
                // Secondary subtle blue glow bottom-left
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF1E90FF).copy(alpha = glowAlpha * 0.4f), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.9f),
                        radius = size.width * 0.4f
                    )
                )
            }
    ) {
        Column(Modifier.fillMaxSize().padding(22.dp), Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column {
                    Text("Estimated Balance", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (privacyMode) "TZS ••••••" else "TZS ${fmtAmt(animBal.toDouble())}",
                        color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
                // Breathing "This Month" badge
                val badgeAlpha by glowPulse.animateFloat(
                    initialValue = 0.75f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse
                    ), label = "badge"
                )
                Surface(
                    color = AccentTeal.copy(alpha = 0.15f * badgeAlpha + 0.05f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.graphicsLayer { alpha = badgeAlpha }
                ) {
                    Text("This Month", color = AccentTeal, fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
            HorizontalDivider(color = Color(0xFF3A4558).copy(alpha = 0.6f))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                HeroStat("Income", income, Icons.AutoMirrored.Filled.TrendingUp,
                    AccentTeal, privacyMode, glowPulse)
                HeroStat("Expenses", expenses, Icons.AutoMirrored.Filled.TrendingDown,
                    ErrorRed, privacyMode, glowPulse)
                val saved = income - expenses
                HeroStat("Saved", saved, Icons.Default.Savings,
                    if (saved >= 0) AccentLight else ErrorRed, privacyMode, glowPulse)
            }
        }
    }
}

@Composable
private fun HeroStat(
    label: String, value: Double, icon: ImageVector, color: Color, privacy: Boolean,
    transition: InfiniteTransition? = null
) {
    // Icon breathes independently with slight offset per-stat via different durations
    val iconScale by (transition ?: rememberInfiniteTransition(label = "s")).animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "iconScale"
    )
    val iconAlpha by (transition ?: rememberInfiniteTransition(label = "sa")).animateFloat(
        initialValue = 0.6f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "iconAlpha"
    )
    Column {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(icon, null,
                tint = color.copy(iconAlpha),
                modifier = Modifier.size(12.dp).graphicsLayer {
                    scaleX = iconScale; scaleY = iconScale
                }
            )
            Text(label, color = TextSecondary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(2.dp))
        Text(if (privacy) "••••" else fmtAmt(value),
            color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun QuickActionsRow(onTransactions: () -> Unit, onCharts: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickAction(Modifier.weight(1f), "Transactions", Icons.Default.Receipt,
            AccentTeal, onTransactions)
        QuickAction(Modifier.weight(1f), "Analytics", Icons.Default.BarChart,
            AccentLight, onCharts)
    }
}

@Composable
private fun QuickAction(mod: Modifier, label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val pulse = rememberInfiniteTransition(label = "qa")
    val iconGlow by pulse.animateFloat(
        initialValue = 0.10f, targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "qaGlow"
    )
    val iconScale by pulse.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "qaScale"
    )
    GlassCard(mod.clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        cornerRadius = 14.dp) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(color.copy(iconGlow)),
                Alignment.Center
            ) {
                Icon(icon, null, tint = color,
                    modifier = Modifier.size(18.dp).graphicsLayer {
                        scaleX = iconScale; scaleY = iconScale
                    }
                )
            }
            Text(label, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, color = TextWhite)
        }
    }
}

@Composable
fun MiniBarChartCard(data: List<com.smsfinance.domain.model.ChartDataPoint>, privacyMode: Boolean) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("This Month", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = TextWhite)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChartLegendDot(AccentTeal, "Income")
                    ChartLegendDot(ErrorRed, "Expense")
                }
            }
            Spacer(Modifier.height(14.dp))
            if (privacyMode) {
                Box(Modifier.fillMaxWidth().height(90.dp), Alignment.Center) {
                    Text("Hidden in privacy mode", color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall)
                }
            } else {
                val maxVal = data.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1f) ?: 1f
                Row(modifier = Modifier.fillMaxWidth().height(90.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom) {
                    data.takeLast(14).forEach { pt ->
                        val animI by animateFloatAsState(pt.income / maxVal * 80,
                            tween(700, easing = EaseOutCubic), label = "i")
                        val animE by animateFloatAsState(pt.expense / maxVal * 80,
                            tween(700, easing = EaseOutCubic), label = "e")
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                Box(Modifier.width(4.dp).height(animI.coerceAtLeast(2f).dp)
                                    .background(AccentTeal, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
                                Box(Modifier.width(4.dp).height(animE.coerceAtLeast(2f).dp)
                                    .background(ErrorRed, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun TransactionRow(transaction: Transaction, privacyMode: Boolean, onClick: () -> Unit) {
    val isDeposit = transaction.type == TransactionType.DEPOSIT
    val haptic = LocalHapticFeedback.current
    val accentColor = if (isDeposit) AccentTeal else ErrorRed
    GlassCard(
        Modifier.fillMaxWidth().clickable {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick()
        },
        cornerRadius = 16.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(0.12f)),
                Alignment.Center
            ) {
                Icon(
                    if (isDeposit) Icons.AutoMirrored.Filled.CallReceived
                    else Icons.AutoMirrored.Filled.CallMade,
                    null, tint = accentColor, modifier = Modifier.size(22.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(transaction.source, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, color = TextWhite)
                Text(formatDate(transaction.date), style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (privacyMode) "••••"
                    else "${if (isDeposit) "+" else "-"} TZS ${fmtAmt(transaction.amount)}",
                    fontWeight = FontWeight.Bold, color = accentColor, fontSize = 14.sp
                )
                Surface(color = accentColor.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text(if (isDeposit) "IN" else "OUT", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                }
            }
        }
    }
}

fun formatCurrency(amount: Double) = "TZS ${fmtAmt(amount)}"
private fun formatDate(ts: Long) =
    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(ts))