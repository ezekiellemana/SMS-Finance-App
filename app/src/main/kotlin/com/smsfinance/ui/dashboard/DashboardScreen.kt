package com.smsfinance.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType
import com.smsfinance.ui.components.*
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.DashboardViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// ── Greeting ──────────────────────────────────────────────────────────────────
private data class GreetingData(val greeting: String, val emoji: String, val message: String)

private fun buildGreeting(name: String): GreetingData {
    val first = name.trim().split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: ""
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11  -> GreetingData(
            "Good morning${if (first.isNotBlank()) ", $first" else ""}",
            "☀️", "Here's your financial snapshot for today"
        )
        in 12..16 -> GreetingData(
            "Good afternoon${if (first.isNotBlank()) ", $first" else ""}",
            "🌤️", "Your money is being tracked automatically"
        )
        in 17..20 -> GreetingData(
            "Good evening${if (first.isNotBlank()) ", $first" else ""}",
            "🌇", "Here's how your day looked financially"
        )
        else      -> GreetingData(
            "Hey${if (first.isNotBlank()) ", $first" else ""}",
            "🌙", "Late night — your finances are safe"
        )
    }
}

// ── Typewriter ────────────────────────────────────────────────────────────────
@Composable
private fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    charDelayMs: Long = 38L,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified
) {
    var displayed by remember(text) { mutableStateOf("") }
    LaunchedEffect(text) {
        displayed = ""
        text.forEach { char -> displayed += char; delay(charDelayMs) }
    }
    val cursorAlpha by rememberInfiniteTransition(label = "cur")
        .animateFloat(1f, 0f, infiniteRepeatable(tween(530), RepeatMode.Reverse), label = "ca")
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(displayed, style = style, color = color)
        if (displayed.length < text.length)
            Text("|", style = style, color = AccentTeal.copy(alpha = cursorAlpha),
                modifier = Modifier.alpha(cursorAlpha))
    }
}

// ── Dashboard screen ──────────────────────────────────────────────────────────
@Suppress("DEPRECATION")
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToTransactions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCharts: () -> Unit = {}
) {
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    var privacyMode by remember { mutableStateOf(false) }

    Scaffold(containerColor = BgPrimary) { padding ->

        // Show greeting card immediately using cached userName even while loading
        if (uiState.isLoading && uiState.userName.isBlank()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = AccentTeal, strokeWidth = 3.dp)
            }
            return@Scaffold
        }

        ScreenEnterAnimation {
            // Single LazyColumn — recent activity rows are direct items, no nested scroll
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ── Top bar ───────────────────────────────────────────────────
                item(key = "topbar") {
                    Row(
                        Modifier.fillMaxWidth(),
                        Arrangement.SpaceBetween, Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PulsingDot(size = 9.dp)
                            Column {
                                Text("Smart Money",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold, color = TextWhite)
                                Text("Auto-tracking active", fontSize = 11.sp, color = AccentTeal)
                            }
                        }
                        Row {
                            IconButton(onClick = { privacyMode = !privacyMode }) {
                                Icon(if (privacyMode) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
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

                // ── Greeting ──────────────────────────────────────────────────
                item(key = "greeting") {
                    GreetingCard(uiState.userName)
                }

                // ── Hero balance card ─────────────────────────────────────────
                item(key = "hero") {
                    HeroBalanceCard(
                        balance        = uiState.summary.estimatedBalance,
                        income         = uiState.summary.monthlyIncome,
                        expenses       = uiState.summary.monthlyExpenses,
                        privacyMode    = privacyMode
                    )
                }

                // ── Quick actions ─────────────────────────────────────────────
                item(key = "actions") {
                    QuickActionsRow(onNavigateToTransactions, onNavigateToCharts)
                }

                // ── Chart ─────────────────────────────────────────────────────
                if (uiState.chartData.isNotEmpty()) {
                    item(key = "chart") {
                        MiniBarChartCard(uiState.chartData, privacyMode)
                    }
                }

                // ── Recent Activity header ────────────────────────────────────
                item(key = "activity_header") {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column {
                            Text("Recent Activity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = TextWhite)
                            if (uiState.recentTransactions.isNotEmpty()) {
                                Text(
                                    "${uiState.recentTransactions.size} transaction${if (uiState.recentTransactions.size > 1) "s" else ""}",
                                    fontSize = 11.sp, color = TextSecondary
                                )
                            }
                        }
                        if (uiState.recentTransactions.size > 5) {
                            TextButton(onClick = onNavigateToTransactions) {
                                Text("See all", color = AccentTeal,
                                    style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.width(2.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                                    Modifier.size(14.dp), AccentTeal)
                            }
                        }
                    }
                }

                // ── Transactions — direct items, no nested scroll ─────────────
                if (uiState.recentTransactions.isEmpty()) {
                    item(key = "empty") {
                        EmptyState("📭", "No transactions yet",
                            "Financial SMS messages will appear here automatically")
                    }
                } else {
                    itemsIndexed(
                        uiState.recentTransactions,
                        key = { _, tx -> tx.id }
                    ) { _, tx ->
                        TransactionRow(tx, privacyMode, onClick = onNavigateToTransactions)
                    }
                }
            }
        }
    }
}

// ── Greeting card ─────────────────────────────────────────────────────────────
@Composable
private fun GreetingCard(userName: String) {
    val greet = remember(userName) { buildGreeting(userName) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val cardAlpha by animateFloatAsState(if (visible) 1f else 0f, tween(500), label = "ga")
    val cardSlide by animateFloatAsState(if (visible) 0f else 12f, tween(500, easing = EaseOut), label = "gs")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = cardAlpha; translationY = cardSlide }
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1E2D42), Color(0xFF1B2E38))))
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(AccentTeal.copy(.10f), Color.Transparent),
                        Offset(size.width * .85f, size.height * .3f), size.width * .55f
                    )
                )
            }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // Emoji badge
            Box(Modifier.size(46.dp).clip(CircleShape).background(AccentTeal.copy(.10f)),
                Alignment.Center) {
                val emojiScale by rememberInfiniteTransition(label = "emj")
                    .animateFloat(1f, 1.10f,
                        infiniteRepeatable(tween(2400, easing = EaseInOut), RepeatMode.Reverse),
                        label = "es")
                Text(greet.emoji, fontSize = 20.sp, modifier = Modifier.scale(emojiScale))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                TypewriterText(
                    text        = greet.greeting,
                    charDelayMs = 42L,
                    style       = MaterialTheme.typography.titleMedium.copy(
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = .2.sp
                    ),
                    color = TextWhite
                )
                var subVisible by remember(greet.greeting) { mutableStateOf(false) }
                LaunchedEffect(greet.greeting) {
                    delay(greet.greeting.length * 42L + 150L); subVisible = true
                }
                val subAlpha by animateFloatAsState(if (subVisible) 1f else 0f, tween(500), label = "sub")
                Text(greet.message, fontSize = 12.sp,
                    color = TextSecondary.copy(alpha = subAlpha), fontStyle = FontStyle.Italic)
            }
        }
    }
}

// ── Hero balance card ─────────────────────────────────────────────────────────
@Composable
fun HeroBalanceCard(
    balance: Double,
    income: Double, expenses: Double, privacyMode: Boolean
) {
    val animBal    by animateFloatAsState(balance.toFloat(), tween(1200, easing = EaseOutCubic), label = "bal")
    val glowPulse  = rememberInfiniteTransition(label = "glow")
    val glowAlpha  by glowPulse.animateFloat(.10f, .26f,
        infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ga")
    val cardScale  by glowPulse.animateFloat(1.000f, 1.003f,
        infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "cs")

    Box(
        Modifier.fillMaxWidth()
            .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(
                listOf(Color(0xFF1A3040), Color(0xFF1E3A3A), Color(0xFF1F2E3A))
            ))
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(AccentTeal.copy(glowAlpha), Color.Transparent),
                        Offset(size.width * .82f, size.height * .18f), size.width * .55f
                    )
                )
            }
            .padding(horizontal = 22.dp, vertical = 22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {

            // Opening balance label
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(AccentTeal.copy(.6f)))
                Text("Current Balance", fontSize = 12.sp, color = TextSecondary,
                    fontWeight = FontWeight.Medium, letterSpacing = .5.sp)
            }

            // Main balance number
            AnimatedContent(privacyMode, label = "balanceVisibility") { hidden ->
                Text(
                    if (hidden) "TZS ••••••"
                    else "TZS ${fmtAmt(animBal.toDouble())}",
                    fontSize = 34.sp, fontWeight = FontWeight.ExtraBold,
                    color = TextWhite, letterSpacing = (-.5).sp
                )
            }

            // Divider
            Box(Modifier.fillMaxWidth().height(.5.dp).background(TextSecondary.copy(.15f)))

            // Stats row — Income and Expenses from ALL SMS transactions
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.spacedBy(0.dp)
            ) {
                // Income
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp))
                            .background(AccentTeal.copy(.15f)), Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, null,
                                tint = AccentTeal, modifier = Modifier.size(10.dp))
                        }
                        Text("Income", fontSize = 11.sp, color = TextSecondary)
                    }
                    Text(
                        if (privacyMode) "••••" else "TZS ${fmtAmt(income)}",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentTeal
                    )
                }

                // Vertical separator
                Box(Modifier.width(.5.dp).height(36.dp).background(TextSecondary.copy(.15f))
                    .align(Alignment.CenterVertically))

                Spacer(Modifier.width(16.dp))

                // Expenses
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp))
                            .background(ErrorRed.copy(.15f)), Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.TrendingDown, null,
                                tint = ErrorRed, modifier = Modifier.size(10.dp))
                        }
                        Text("Expenses", fontSize = 11.sp, color = TextSecondary)
                    }
                    Text(
                        if (privacyMode) "••••" else "TZS ${fmtAmt(expenses)}",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ErrorRed
                    )
                }
            }
        }
    }
}

// ── Quick actions ─────────────────────────────────────────────────────────────
@Composable
fun QuickActionsRow(onTransactions: () -> Unit, onCharts: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickAction(Modifier.weight(1f), "Transactions", Icons.Default.Receipt, AccentTeal, onTransactions)
        QuickAction(Modifier.weight(1f), "Analytics", Icons.Default.BarChart, AccentLight, onCharts)
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier, label: String, icon: ImageVector, color: Color, onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val pulse  = rememberInfiniteTransition(label = "qa")
    val glow   by pulse.animateFloat(.08f, .20f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "qg")
    val sc     by pulse.animateFloat(.96f, 1.04f,
        infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "qs")

    GlassCard(modifier.clickable {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick()
    }, cornerRadius = 14.dp) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(glow)),
                Alignment.Center) {
                Icon(icon, null, tint = color,
                    modifier = Modifier.size(18.dp).graphicsLayer { scaleX = sc; scaleY = sc })
            }
            Text(label, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, color = TextWhite)
        }
    }
}

// ── Mini bar chart ────────────────────────────────────────────────────────────
@Composable
fun MiniBarChartCard(data: List<com.smsfinance.domain.model.ChartDataPoint>, privacyMode: Boolean) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("This Month", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = TextWhite)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChartDot(AccentTeal, "Income")
                    ChartDot(ErrorRed, "Expense")
                }
            }
            Spacer(Modifier.height(14.dp))
            if (privacyMode) {
                Box(Modifier.fillMaxWidth().height(80.dp), Alignment.Center) {
                    Text("Hidden in privacy mode", color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall)
                }
            } else {
                val maxVal = data.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1f) ?: 1f
                Row(Modifier.fillMaxWidth().height(80.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom) {
                    data.takeLast(14).forEach { pt ->
                        val animI by animateFloatAsState(pt.income / maxVal * 70,
                            tween(700, easing = EaseOutCubic), label = "i")
                        val animE by animateFloatAsState(pt.expense / maxVal * 70,
                            tween(700, easing = EaseOutCubic), label = "e")
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(1.dp)) {
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
private fun ChartDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

// ── Transaction row ───────────────────────────────────────────────────────────
@Composable
fun TransactionRow(transaction: Transaction, privacyMode: Boolean, onClick: () -> Unit) {
    val isDeposit   = transaction.type == TransactionType.DEPOSIT
    val haptic      = LocalHapticFeedback.current
    val accentColor = if (isDeposit) AccentTeal else ErrorRed

    GlassCard(
        Modifier.fillMaxWidth().clickable {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick()
        }, cornerRadius = 16.dp
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                .background(accentColor.copy(.12f)), Alignment.Center) {
                Icon(
                    if (isDeposit) Icons.AutoMirrored.Filled.CallReceived
                    else Icons.AutoMirrored.Filled.CallMade,
                    null, tint = accentColor, modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(transaction.source,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, color = TextWhite)
                Text(formatDate(transaction.date),
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (privacyMode) "••••"
                    else "${if (isDeposit) "+" else "-"} TZS ${fmtAmt(transaction.amount)}",
                    fontWeight = FontWeight.Bold, color = accentColor, fontSize = 14.sp
                )
                Surface(color = accentColor.copy(.1f), shape = RoundedCornerShape(4.dp)) {
                    Text(if (isDeposit) "IN" else "OUT", fontSize = 9.sp,
                        fontWeight = FontWeight.Bold, color = accentColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun formatDate(ts: Long): String =
    SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(Date(ts))

private fun fmtAmt(amt: Double): String = String.format(Locale.US, "%,.0f", amt)
