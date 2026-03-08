package com.smsfinance.ui.dashboard
import com.smsfinance.R

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.core.graphics.toColorInt
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.foundation.border
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType
import com.smsfinance.ui.components.*
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.DashboardViewModel
import com.smsfinance.viewmodel.MultiUserViewModel
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
        else -> GreetingData(
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
    multiUserVm: MultiUserViewModel = hiltViewModel(),
    onNavigateToTransactions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCharts: () -> Unit = {}
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val multiState    by multiUserVm.uiState.collectAsStateWithLifecycle()
    var privacyMode   by remember { mutableStateOf(false) }
    val haptic        = LocalHapticFeedback.current

    // Derive the profile accent colour — falls back to AccentTeal if none set
    val profileAccent: Color = remember(multiState.activeProfile?.color) {
        runCatching {
            val hex = multiState.activeProfile?.color ?: return@runCatching AccentTeal
            Color(hex.toColorInt())
        }.getOrElse { AccentTeal }
    }

    Scaffold(containerColor = BgPrimary) { padding ->

        if (uiState.isLoading && uiState.userName.isBlank()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = AccentTeal, strokeWidth = 3.dp)
            }
            return@Scaffold
        }

        // ── Fixed outer column — top section static, transactions scroll ──────
        Column(
            Modifier.fillMaxSize().padding(padding)
        ) {

            // ── TOP STATIC SECTION — never scrolls ────────────────────────────
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // Top bar
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PulsingDot(size = 9.dp)
                        Column {
                            Text("Smart Money",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(stringResource(R.string.auto_tracking_active), fontSize = 11.sp, color = AccentTeal)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        IconButton(onClick = { privacyMode = !privacyMode }) {
                            Icon(if (privacyMode) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility, null, tint = TextSecondary)
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

                // Greeting
                GreetingCard(uiState.userName)

                // Hero balance
                HeroBalanceCard(
                    balance       = uiState.summary.estimatedBalance,
                    income        = uiState.allTimeIncome,
                    expenses      = uiState.allTimeExpenses,
                    isLoading     = uiState.isLoading,
                    privacyMode   = privacyMode,
                    profileAccent = profileAccent
                )

                // Quick actions
                QuickActionsRow(onNavigateToTransactions, onNavigateToCharts)
            }

            Spacer(Modifier.height(10.dp))

            // ── RECENT ACTIVITY — no scroll, fits exactly what the screen has left ──
            // BoxWithConstraints measures the remaining height and shows only as many
            // cards as fit, keeping the layout flush on every screen size.
            BoxWithConstraints(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)
            ) {
                // Each card is ~72dp tall + 9dp gap; header row is ~52dp
                val cardH   = 72.dp
                val gapH    = 9.dp
                val headerH = 52.dp
                val available = maxHeight - headerH
                val maxCards = if (available > 0.dp) {
                    ((available + gapH) / (cardH + gapH)).toInt().coerceAtLeast(1)
                } else 1
                val visibleTx = uiState.recentTransactions.take(maxCards)

                Column(Modifier.fillMaxWidth()) {
                    // ── Recent Activity header row ─────────────────────────
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically
                    ) {
                        // Left: title + subtitle
                        Column {
                            Text(stringResource(R.string.recent_activity),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = TextWhite)
                            if (uiState.recentTransactions.isNotEmpty()) {
                                Text(
                                    "${uiState.recentTransactions.size} transaction" +
                                            if (uiState.recentTransactions.size > 1) "s" else "",
                                    fontSize = 11.sp, color = TextSecondary
                                )
                            }
                        }

                        // Right: refresh button + optional "See all"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // ── Refresh badge (shows count of newly found tx) ──
                            val result = uiState.refreshResult
                            if (result != null) {
                                LaunchedEffect(result) {
                                    delay(3_000)
                                    viewModel.clearRefreshResult()
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (result > 0) AccentTeal.copy(.18f)
                                    else TextSecondary.copy(.12f),
                                    modifier = Modifier.clickable { viewModel.clearRefreshResult() }
                                ) {
                                    Text(
                                        if (result > 0) "+$result new" else "Up to date",
                                        fontSize = 10.sp,
                                        color = if (result > 0) AccentTeal else TextSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // ── Spinning refresh icon button ──────────────────
                            val spinAngle by rememberInfiniteTransition(label = "spin")
                                .animateFloat(
                                    0f, 360f,
                                    infiniteRepeatable(tween(700, easing = LinearEasing)),
                                    label = "sa"
                                )
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.isRefreshing) AccentTeal.copy(.18f)
                                        else TextSecondary.copy(.08f)
                                    )
                                    .clickable(enabled = !uiState.isRefreshing) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.refreshFromInbox()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh SMS",
                                    tint = if (uiState.isRefreshing) AccentTeal
                                    else TextSecondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .rotate(if (uiState.isRefreshing) spinAngle else 0f)
                                )
                            }

                            // ── "See all" link ────────────────────────────────
                            if (uiState.recentTransactions.size > maxCards) {
                                TextButton(onClick = onNavigateToTransactions,
                                    contentPadding = PaddingValues(0.dp)) {
                                    Text(stringResource(R.string.see_all), color = AccentTeal, fontSize = 13.sp)
                                    Spacer(Modifier.width(3.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                                        Modifier.size(13.dp), AccentTeal)
                                }
                            }
                        }
                    }

                    // Transaction cards — exactly as many as fit, no scroll
                    if (uiState.recentTransactions.isEmpty()) {
                        Box(Modifier.fillMaxWidth(), Alignment.Center) {
                            EmptyState("📭", "No transactions yet",
                                "Financial SMS messages will appear here automatically")
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            visibleTx.forEach { tx ->
                                TransactionRow(tx, privacyMode, profileAccent,
                                    onClick = onNavigateToTransactions)
                            }
                        }
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
    val cardSlide by animateFloatAsState(if (visible) 0f else 10f, tween(500, easing = EaseOut), label = "gs")

    Box(
        Modifier.fillMaxWidth()
            .graphicsLayer { alpha = cardAlpha; translationY = cardSlide }
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1E2D42), Color(0xFF1B2E38))))
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(AccentTeal.copy(.09f), Color.Transparent),
                        Offset(size.width * .85f, size.height * .3f), size.width * .5f
                    )
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(AccentTeal.copy(.10f)),
                Alignment.Center) {
                val emojiScale by rememberInfiniteTransition(label = "emj")
                    .animateFloat(1f, 1.10f,
                        infiniteRepeatable(tween(2400, easing = EaseInOut), RepeatMode.Reverse), label = "es")
                Text(greet.emoji, fontSize = 18.sp, modifier = Modifier.scale(emojiScale))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TypewriterText(
                    text = greet.greeting, charDelayMs = 42L,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold, letterSpacing = .2.sp),
                    color = TextWhite
                )
                var subVisible by remember(greet.greeting) { mutableStateOf(false) }
                LaunchedEffect(greet.greeting) {
                    delay(greet.greeting.length * 42L + 150L); subVisible = true
                }
                val subAlpha by animateFloatAsState(if (subVisible) 1f else 0f, tween(500), label = "sub")
                Text(greet.message, fontSize = 11.sp,
                    color = TextSecondary.copy(alpha = subAlpha), fontStyle = FontStyle.Italic)
            }
        }
    }
}

// ── Hero balance card ─────────────────────────────────────────────────────────
@Composable
fun HeroBalanceCard(
    balance: Double,
    income: Double,
    expenses: Double,
    isLoading: Boolean,
    privacyMode: Boolean,
    profileAccent: Color = AccentTeal
) {
    val animBal   by animateFloatAsState(balance.toFloat(), tween(1200, easing = EaseOutCubic), label = "bal")
    val animInc   by animateFloatAsState(income.toFloat(),  tween(900,  easing = EaseOutCubic), label = "inc")
    val animExp   by animateFloatAsState(expenses.toFloat(),tween(900,  easing = EaseOutCubic), label = "exp")

    val glowPulse = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glowPulse.animateFloat(.09f, .24f,
        infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ga")
    val cardScale by glowPulse.animateFloat(1.000f, 1.003f,
        infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "cs")

    // Shimmer sweep for loading state
    val shimmer = rememberInfiniteTransition(label = "shim")
    val shimX  by shimmer.animateFloat(
        -1f, 2f,
        infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "sx"
    )

    Box(
        Modifier.fillMaxWidth()
            .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
            .clip(RoundedCornerShape(22.dp))
            // Profile-colour glowing border
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        profileAccent.copy(alpha = 0.7f),
                        profileAccent.copy(alpha = 0.3f),
                        profileAccent.copy(alpha = 0.7f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .background(Brush.linearGradient(
                listOf(Color(0xFF1A3040), Color(0xFF1E3A3A), Color(0xFF1F2E3A))
            ))
            .drawBehind {
                // Ambient glow orb — uses profile accent colour
                drawCircle(
                    Brush.radialGradient(
                        listOf(profileAccent.copy(glowAlpha), Color.Transparent),
                        Offset(size.width * .82f, size.height * .18f), size.width * .55f
                    )
                )
                // Shimmer sweep — only while loading
                if (isLoading) {
                    drawRect(
                        Brush.linearGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(.07f),
                                Color.Transparent
                            ),
                            start = Offset(size.width * shimX, 0f),
                            end   = Offset(size.width * (shimX + .4f), size.height)
                        )
                    )
                }
            }
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // ── Balance label + number ─────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    // Dot: pulsing green when live, muted while loading
                    Box(
                        Modifier.size(6.dp).clip(CircleShape)
                            .background(if (isLoading) TextSecondary.copy(.4f) else AccentTeal.copy(.7f))
                    )
                    Text(
                        if (isLoading) "Calculating balance…" else "Current Balance",
                        fontSize = 11.sp, color = TextSecondary,
                        fontWeight = FontWeight.Medium, letterSpacing = .5.sp
                    )
                }
                AnimatedContent(privacyMode, label = "bv") { hidden ->
                    if (isLoading) {
                        // Skeleton placeholder — same line height as the real number
                        Box(
                            Modifier
                                .width(180.dp).height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TextSecondary.copy(.12f))
                        )
                    } else {
                        Text(
                            if (hidden) "TZS ••••••" else "TZS ${fmtAmt(animBal.toDouble())}",
                            fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
                            color = TextWhite, letterSpacing = (-.3).sp
                        )
                    }
                }
            }

            // Thin divider
            Box(Modifier.fillMaxWidth().height(.5.dp).background(TextSecondary.copy(.12f)))

            // ── Income / Expenses — all-time SMS totals ────────────────────────
            Row(Modifier.fillMaxWidth()) {
                StatItem(
                    icon     = Icons.AutoMirrored.Filled.TrendingUp,
                    label    = "Total Income",
                    value    = if (privacyMode) "••••"
                    else if (isLoading) "—"
                    else "TZS ${fmtAmt(animInc.toDouble())}",
                    color    = AccentTeal,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier.width(.5.dp).height(38.dp)
                        .background(TextSecondary.copy(.15f))
                        .align(Alignment.CenterVertically)
                )
                StatItem(
                    icon     = Icons.AutoMirrored.Filled.TrendingDown,
                    label    = "Total Expenses",
                    value    = if (privacyMode) "••••"
                    else if (isLoading) "—"
                    else "TZS ${fmtAmt(animExp.toDouble())}",
                    color    = ErrorRed,
                    modifier = Modifier.weight(1f).padding(start = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun StatItem(icon: ImageVector, label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(15.dp).clip(RoundedCornerShape(4.dp)).background(color.copy(.15f)), Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(9.dp))
            }
            Text(label, fontSize = 10.sp, color = TextSecondary, letterSpacing = .3.sp)
        }
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
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
    val glow   by pulse.animateFloat(.08f, .18f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "qg")
    val sc     by pulse.animateFloat(.96f, 1.04f,
        infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "qs")

    GlassCard(modifier.clickable {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick()
    }, cornerRadius = 14.dp) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(glow)),
                Alignment.Center) {
                Icon(icon, null, tint = color,
                    modifier = Modifier.size(17.dp).graphicsLayer { scaleX = sc; scaleY = sc })
            }
            Text(label, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, color = TextWhite)
        }
    }
}

// ── Transaction row ───────────────────────────────────────────────────────────
@Composable
fun TransactionRow(
    transaction: Transaction,
    privacyMode: Boolean,
    profileAccent: Color = AccentTeal,
    onClick: () -> Unit
) {
    val isDeposit   = transaction.type == TransactionType.DEPOSIT
    val haptic      = LocalHapticFeedback.current
    val txColor     = if (isDeposit) AccentTeal else ErrorRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            // Profile-colour border wraps the whole card
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        profileAccent.copy(alpha = 0.55f),
                        profileAccent.copy(alpha = 0.20f),
                        profileAccent.copy(alpha = 0.55f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                Brush.horizontalGradient(
                    listOf(
                        txColor.copy(.08f),
                        txColor.copy(.04f),
                        Color(0xFF1E2840)
                    )
                )
            )
            .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Round colored icon circle
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(txColor.copy(.18f)),
            Alignment.Center
        ) {
            Icon(
                if (isDeposit) Icons.AutoMirrored.Filled.CallReceived
                else Icons.AutoMirrored.Filled.CallMade,
                null, tint = txColor, modifier = Modifier.size(18.dp)
            )
        }

        // Source + date
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(transaction.source,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 1, color = TextWhite)
            Text(formatDate(transaction.date),
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        // Amount + small circle badge
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (privacyMode) "••••"
                else "${if (isDeposit) "+" else "-"} TZS ${fmtAmt(transaction.amount)}",
                fontWeight = FontWeight.Bold, color = txColor, fontSize = 13.sp
            )
            Box(
                Modifier.size(22.dp).clip(CircleShape).background(txColor.copy(.18f)),
                Alignment.Center
            ) {
                Text(
                    if (isDeposit) "IN" else "OUT",
                    fontSize = 6.sp, fontWeight = FontWeight.ExtraBold, color = txColor
                )
            }
        }
    }
}


// ── Helpers ───────────────────────────────────────────────────────────────────
private fun formatDate(ts: Long): String =
    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(ts))