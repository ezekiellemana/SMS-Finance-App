package com.smsfinance.ui.dashboard
import com.smsfinance.R

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.animateColorAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType
import com.smsfinance.ui.components.*
import com.smsfinance.ui.components.PulsingDot
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.DashboardViewModel
import com.smsfinance.viewmodel.MultiUserViewModel
import com.smsfinance.viewmodel.ServiceBalance
import com.smsfinance.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// ── Greeting ──────────────────────────────────────────────────────────────────
private data class GreetingData(val greeting: String, val emoji: String, val message: String)

private fun buildGreeting(
    name: String,
    morning: String, afternoon: String, evening: String, night: String,
    msgMorning: String, msgAfternoon: String, msgEvening: String, msgNight: String
): GreetingData {
    val first = name.trim().split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: ""
    val suffix = if (first.isNotBlank()) ", $first" else ""
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11  -> GreetingData("$morning$suffix",  "☀️",  msgMorning)
        in 12..16 -> GreetingData("$afternoon$suffix","🌤️", msgAfternoon)
        in 17..20 -> GreetingData("$evening$suffix",  "🌇",  msgEvening)
        else      -> GreetingData("$night$suffix",    "🌙",  msgNight)
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
    settingsVm: SettingsViewModel = hiltViewModel(),
    onNavigateToTransactions: () -> Unit,
    onNavigateToDetail: (Long) -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCharts: () -> Unit = {},
    fromOnboarding: Boolean = false
) {
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    val multiState  by multiUserVm.uiState.collectAsStateWithLifecycle()
    // Persisted in DataStore — survives app restarts and process death
    val privacyMode by settingsVm.privacyMode.collectAsStateWithLifecycle()
    val haptic      = LocalHapticFeedback.current


    // Derive the profile accent colour — falls back to AccentTeal if none set
    val profileAccent: Color = remember(multiState.activeProfile?.color) {
        runCatching {
            val hex = multiState.activeProfile?.color ?: return@runCatching AccentTeal
            Color(hex.toColorInt())
        }.getOrElse { AccentTeal }
    }

    // ── Staggered entrance animation (only after onboarding) ─────────────────
    var showTopBar      by remember { mutableStateOf(!fromOnboarding) }
    var showGreeting    by remember { mutableStateOf(!fromOnboarding) }
    var showBalance     by remember { mutableStateOf(!fromOnboarding) }
    var showActions     by remember { mutableStateOf(!fromOnboarding) }
    var showActivity    by remember { mutableStateOf(!fromOnboarding) }
    if (fromOnboarding) {
        LaunchedEffect(Unit) {
            delay(120);  showTopBar   = true
            delay(160);  showGreeting = true
            delay(200);  showBalance  = true
            delay(180);  showActions  = true
            delay(220);  showActivity = true
        }
    }
    val enterSpec = { offsetY: Int ->
        fadeIn(tween(420, easing = EaseOutCubic)) +
                slideInVertically(tween(420, easing = EaseOutCubic)) { offsetY }
    }

    Scaffold(containerColor = BgPrimary) { padding ->

        // ── Skeleton on first load (no user data yet) ──────────────────────

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

                // ── Top navigation bar ──────────────────────────────────
                AnimatedVisibility(visible = showTopBar, enter = enterSpec(-60)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1A2438).copy(0.95f), BgPrimary.copy(0.85f))
                                )
                            )
                            .border(1.dp,
                                Brush.linearGradient(listOf(
                                    profileAccent.copy(0.35f), profileAccent.copy(0.08f)
                                )),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically
                    ) {
                        // Left: breathing dot + brand name + subtitle
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PulsingDot(color = profileAccent, size = 8.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text("Smart Money",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextWhite,
                                    letterSpacing = (-0.3).sp)
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.size(5.dp).background(profileAccent, CircleShape))
                                    Text(stringResource(R.string.auto_tracking_active),
                                        fontSize = 10.sp, color = profileAccent.copy(0.85f),
                                        letterSpacing = 0.2.sp)
                                }
                            }
                        }

                        // Right: icon buttons with pill backgrounds
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically) {

                            // Privacy toggle
                            TopNavIcon(
                                icon     = if (privacyMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                tint     = if (privacyMode) profileAccent else TextSecondary,
                                active   = privacyMode,
                                accent   = profileAccent,
                                onClick  = { settingsVm.setPrivacyMode(!privacyMode) }
                            )
                            // Charts
                            TopNavIcon(
                                icon    = Icons.Default.BarChart,
                                tint    = TextSecondary,
                                accent  = profileAccent,
                                onClick = onNavigateToCharts
                            )
                            // Search
                            TopNavIcon(
                                icon    = Icons.Default.Search,
                                tint    = TextSecondary,
                                accent  = profileAccent,
                                onClick = onNavigateToSearch
                            )
                            // Settings
                            TopNavIcon(
                                icon    = Icons.Default.Settings,
                                tint    = TextSecondary,
                                accent  = profileAccent,
                                onClick = onNavigateToSettings
                            )
                        }
                    }
                } // end AnimatedVisibility topBar

                // Greeting
                AnimatedVisibility(visible = showGreeting, enter = enterSpec(-50)) {
                    GreetingCard(uiState.userName)
                } // end AnimatedVisibility greeting

                // Hero balance
                AnimatedVisibility(visible = showBalance, enter = enterSpec(-70)) {
                    HeroBalanceCard(
                        balance         = uiState.summary.estimatedBalance,
                        income          = uiState.allTimeIncome,
                        expenses        = uiState.allTimeExpenses,
                        isLoading       = uiState.isLoading,
                        privacyMode     = privacyMode,
                        profileAccent   = profileAccent,
                        serviceBalances = uiState.serviceBalances
                    )
                } // end AnimatedVisibility balance

                // Quick actions
                AnimatedVisibility(visible = showActions, enter = enterSpec(-40)) {
                    QuickActionsRow(onNavigateToTransactions, onNavigateToCharts)
                } // end AnimatedVisibility actions
            }

            Spacer(Modifier.height(10.dp))

            // ── RECENT ACTIVITY — no scroll, fits exactly what the screen has left ──
            // Wrapped in AnimatedVisibility for stagger entrance from onboarding
            // BoxWithConstraints measures remaining height.
            // Shows only as many cards as fit — flush on every screen size.
            AnimatedVisibility(
                visible = showActivity,
                enter   = enterSpec(-60),
                modifier = Modifier.weight(1f)
            ) {
                BoxWithConstraints(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp)
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
                                            if (result > 0) stringResource(R.string.dash_new_tx, result) else stringResource(R.string.dash_up_to_date),
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
                                EmptyState("📭", stringResource(R.string.dash_no_tx),
                                    stringResource(R.string.dash_no_tx_sub))
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                visibleTx.forEach { tx ->
                                    TransactionRow(tx, privacyMode, profileAccent,
                                        onClick = { onNavigateToDetail(tx.id) })
                                }
                            }
                        }
                    }
                }
            } // end AnimatedVisibility activity
        }

    }
}

// ── Top nav icon button with optional active pill background ─────────────────

@Composable
private fun TopNavIcon(
    icon: ImageVector,
    tint: Color,
    accent: Color,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (active) accent.copy(0.18f) else Color.Transparent,
        tween(200), label = "iconBg"
    )
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null,
            tint = if (active) accent else tint,
            modifier = Modifier.size(20.dp))
    }
}

// ── Greeting card ─────────────────────────────────────────────────────────────
@Composable
private fun GreetingCard(userName: String) {
    val greetMorning   = stringResource(R.string.dash_good_morning)
    val greetAfternoon = stringResource(R.string.dash_good_afternoon)
    val greetEvening   = stringResource(R.string.dash_good_evening)
    val greetNight     = stringResource(R.string.dash_hey)
    val msgMorning     = stringResource(R.string.dash_snapshot)
    val msgAfternoon   = stringResource(R.string.dash_tracked)
    val msgEvening     = stringResource(R.string.dash_day_looked)
    val msgNight       = stringResource(R.string.dash_late_night)
    val greet = remember(userName, greetMorning, greetAfternoon, greetEvening, greetNight) {
        buildGreeting(userName, greetMorning, greetAfternoon, greetEvening, greetNight,
            msgMorning, msgAfternoon, msgEvening, msgNight)
    }
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
    profileAccent: Color = AccentTeal,
    serviceBalances: List<ServiceBalance> = emptyList()
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
                // Ambient glow orb — radius passed to drawCircle so it stays contained
                val orbRadius = size.width * 0.42f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(profileAccent.copy(glowAlpha), Color.Transparent),
                        center = Offset(size.width * .82f, size.height * .18f),
                        radius = orbRadius
                    ),
                    radius = orbRadius,
                    center = Offset(size.width * .82f, size.height * .18f)
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
                    // Dot: pulsing profile colour when live
                    if (isLoading) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(TextSecondary.copy(.4f)))
                    } else {
                        PulsingDot(color = profileAccent, size = 6.dp)
                    }
                    Text(
                        if (isLoading) stringResource(R.string.dash_calculating) else stringResource(R.string.dash_current_balance),
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

            // ── Bottom row: income/expenses LEFT | services RIGHT ──────────────
            // height(IntrinsicSize.Min) makes the Row wrap its tallest child
            // instead of expanding to fill remaining screen height.
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.Top) {

                // Left — income & expenses totals
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatItem(
                        icon     = Icons.AutoMirrored.Filled.TrendingUp,
                        label    = stringResource(R.string.dash_total_income),
                        value    = if (privacyMode) "••••"
                        else if (isLoading) "—"
                        else "TZS ${fmtAmt(animInc.toDouble())}",
                        color    = AccentTeal,
                        modifier = Modifier.fillMaxWidth()
                    )
                    StatItem(
                        icon     = Icons.AutoMirrored.Filled.TrendingDown,
                        label    = stringResource(R.string.dash_total_expenses),
                        value    = if (privacyMode) "••••"
                        else if (isLoading) "—"
                        else "TZS ${fmtAmt(animExp.toDouble())}",
                        color    = ErrorRed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Vertical divider
                if (serviceBalances.isNotEmpty()) {
                    Box(
                        Modifier.width(.5.dp).fillMaxHeight().padding(vertical = 2.dp)
                            .background(TextSecondary.copy(.15f))
                    )

                    Spacer(Modifier.width(12.dp))

                    // Right — per-service opening balances (scrollable if many)
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            stringResource(R.string.dash_my_services),
                            fontSize = 9.sp,
                            color    = TextSecondary,
                            letterSpacing = .5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        serviceBalances.forEach { svc ->
                            ServiceBalanceRow(svc, privacyMode)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceBalanceRow(svc: ServiceBalance, privacyMode: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(svc.emoji, fontSize = 10.sp)
            Text(
                svc.displayName,
                fontSize    = 9.sp,
                color       = TextSecondary,
                maxLines    = 1,
                modifier    = Modifier.widthIn(max = 64.dp),
                overflow    = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Text(
            if (privacyMode) "••••"
            else if (svc.openingBalance == 0.0) "—"
            else "TZS ${fmtAmt(svc.openingBalance)}",
            fontSize   = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (svc.openingBalance > 0.0 && !privacyMode) AccentTeal else TextSecondary
        )
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
        QuickAction(Modifier.weight(1f), stringResource(R.string.dash_btn_transactions), Icons.Default.Receipt, AccentTeal, onTransactions)
        QuickAction(Modifier.weight(1f), stringResource(R.string.dash_btn_analytics), Icons.Default.BarChart, AccentLight, onCharts)
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