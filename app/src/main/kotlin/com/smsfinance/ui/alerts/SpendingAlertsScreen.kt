package com.smsfinance.ui.alerts
import com.smsfinance.R

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.AlertCheckResult
import com.smsfinance.domain.model.AlertPeriod
import com.smsfinance.domain.model.SpendingAlert
import com.smsfinance.ui.components.*
import com.smsfinance.ui.onboarding.ALL_SENDERS
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.SpendingAlertsViewModel

// ── Preset definition — name/tip resolved at runtime via stringResource ────────
internal data class AlertPreset(
    val emoji: String,
    val nameRes: Int,
    val limit: Double,
    val period: AlertPeriod,
    val tipRes: Int
)

private val PRESETS = listOf(
    AlertPreset("🍔", R.string.preset_daily_food_name,     15_000.0,  AlertPeriod.DAILY,   R.string.preset_daily_food_tip),
    AlertPreset("📱", R.string.preset_mobile_data_name,    30_000.0,  AlertPeriod.MONTHLY, R.string.preset_mobile_data_tip),
    AlertPreset("🚌", R.string.preset_transport_name,      50_000.0,  AlertPeriod.WEEKLY,  R.string.preset_transport_tip),
    AlertPreset("🛒", R.string.preset_groceries_name,     150_000.0,  AlertPeriod.MONTHLY, R.string.preset_groceries_tip),
    AlertPreset("💸", R.string.preset_total_spending_name,500_000.0,  AlertPeriod.MONTHLY, R.string.preset_total_spending_tip),
    AlertPreset("🏥", R.string.preset_health_name,         80_000.0,  AlertPeriod.MONTHLY, R.string.preset_health_tip),
)

@Composable
fun SpendingAlertsScreen(
    viewModel: SpendingAlertsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val profileColor  = LocalProfileColor.current

    @Suppress("ASSIGNED_BUT_NEVER_READ_VARIABLE")
    var showAddDialog by remember { mutableStateOf(false) }
    @Suppress("ASSIGNED_BUT_NEVER_READ_VARIABLE")
    var editingAlert  by remember { mutableStateOf<SpendingAlert?>(null) }
    @Suppress("ASSIGNED_BUT_NEVER_READ_VARIABLE")
    var preset        by remember { mutableStateOf<AlertPreset?>(null) }

    val fabPulse = rememberInfiniteTransition(label = "fab")
    val fabGlow  by fabPulse.animateFloat(.50f, .90f,
        infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "fg")
    val fabScale by fabPulse.animateFloat(.97f, 1.03f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "fs")

    AppScreenScaffold(
        title          = stringResource(R.string.spending_alerts_title),
        subtitle       = stringResource(R.string.alert_live_subtitle),
        onNavigateBack = onNavigateBack,
        showBackButton = false
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp,
                    // Extra bottom padding so FAB + label never cover last item
                    bottom = 140.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // How it works banner
                item { HowItWorksBanner(profileColor) }

                // Active alerts header
                if (uiState.alerts.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween,
                            Alignment.CenterVertically) {
                            Text(stringResource(R.string.alert_your_alerts),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp, color = TextWhite)
                            Text("${uiState.alerts.size}",
                                fontSize = 12.sp, color = profileColor)
                        }
                    }
                    items(uiState.alerts, key = { it.id }) { alert ->
                        AlertCard(
                            alert        = alert,
                            progress     = uiState.alertProgress[alert.id],
                            profileColor = profileColor,
                            onEdit       = { editingAlert = alert },
                            onDelete     = { viewModel.deleteAlert(alert) },
                            onToggle     = { viewModel.toggleAlert(alert) }
                        )
                    }
                }

                // Empty state
                if (uiState.alerts.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(140.dp), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🔔", fontSize = 44.sp)
                                Text(stringResource(R.string.alert_no_alerts_yet),
                                    fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                    color = TextWhite)
                                Text(stringResource(R.string.alert_no_alerts_use_preset),
                                    fontSize = 13.sp, color = TextSecondary,
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                // Presets section
                item {
                    Text(stringResource(R.string.alert_quick_presets),
                        fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextWhite)
                }
                item {
                    Text(stringResource(R.string.alert_preset_hint),
                        fontSize = 12.sp, color = TextSecondary)
                }
                items(PRESETS) { p ->
                    PresetCard(p, profileColor) {
                        preset = p; showAddDialog = true
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // ── Glowing FAB — floats above content, never overlaps ────────────
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // FAB glow + button
                Box(
                    Modifier.size(82.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow ring
                    Box(
                        Modifier.size(82.dp)
                            .graphicsLayer { alpha = fabGlow }
                            .background(
                                Brush.radialGradient(listOf(OrangeWarn.copy(.50f), Color.Transparent)),
                                CircleShape
                            )
                    )
                    // Button
                    Box(
                        Modifier
                            .size(60.dp)
                            .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                            .shadow(14.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(OrangeWarn, Color(0xFFFF6D00)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { preset = null; showAddDialog = true },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.Default.Add, stringResource(R.string.alert_new_label),
                                tint = Color(0xFF1A0A00), modifier = Modifier.size(30.dp))
                        }
                    }
                }
                // Label sits BELOW the FAB, never inside or overlapping
                Text(
                    stringResource(R.string.alert_new_label),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OrangeWarn.copy(.85f)
                )
            }
        }
    }

    if (showAddDialog || editingAlert != null) {
        AlertCardDialog(
            existing     = editingAlert,
            preset       = preset,
            profileColor = profileColor,
            onSave       = { alert ->
                viewModel.saveAlert(alert)
                run { showAddDialog = false; editingAlert = null; preset = null }
            },
            onDismiss = {
                run { showAddDialog = false; editingAlert = null; preset = null }
            }
        )
    }
}

// ── How it works banner ───────────────────────────────────────────────────────

@Composable
private fun HowItWorksBanner(accent: Color) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(0.08f))
            .border(1.dp, accent.copy(0.25f), RoundedCornerShape(16.dp))
            .clickable(remember { MutableInteractionSource() }, null) { expanded = !expanded }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⚡", fontSize = 18.sp)
                Column {
                    Text(stringResource(R.string.alert_live_tracking),
                        fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accent)
                    Text(stringResource(R.string.alert_live_subtitle2),
                        fontSize = 11.sp, color = TextSecondary)
                }
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null, tint = accent.copy(0.7f), modifier = Modifier.size(18.dp))
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HorizontalDivider(color = accent.copy(0.15f))
                listOf(
                    "📩" to stringResource(R.string.alert_how_sms),
                    "🔢" to stringResource(R.string.alert_how_sum),
                    "📊" to stringResource(R.string.alert_how_progress),
                    "🔔" to stringResource(R.string.alert_how_notify)
                ).forEach { (emoji, text) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(emoji, fontSize = 13.sp)
                        Text(text, fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

// ── Preset card ───────────────────────────────────────────────────────────────

@Composable
private fun PresetCard(p: AlertPreset, accent: Color, onTap: () -> Unit) {
    val name = stringResource(p.nameRes)
    val tip  = stringResource(p.tipRes)
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C2537))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(11.dp))
            .background(accent.copy(0.12f)), Alignment.Center) {
            Text(p.emoji, fontSize = 19.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, color = TextWhite)
            Text("TZS ${fmtAmt(p.limit)} / ${p.period.label}  •  $tip",
                fontSize = 11.sp, color = TextSecondary)
        }
        Box(Modifier.size(28.dp).clip(CircleShape).background(accent.copy(0.14f)),
            Alignment.Center) {
            Icon(Icons.Default.Add, null, tint = accent, modifier = Modifier.size(15.dp))
        }
    }
}

// ── Alert card ────────────────────────────────────────────────────────────────

@Composable
fun AlertCard(
    alert: SpendingAlert,
    progress: AlertCheckResult?,
    profileColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    val pct      = progress?.percentUsed ?: 0.0
    val barColor = when {
        pct >= 100 -> ErrorRed
        pct >= 80  -> OrangeWarn
        else       -> AccentTeal
    }
    val animProg by animateFloatAsState(
        (pct / 100.0).coerceIn(0.0, 1.0).toFloat(), tween(700), label = "prog")
    val sourceLabel = alert.sourceFilter?.let {
        ALL_SENDERS.find { s -> s.id == it }?.displayName ?: it
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF1C2537)),
        border    = BorderStroke(1.dp, profileColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                        .background(barColor.copy(.14f)), Alignment.Center) {
                        Icon(Icons.Default.Notifications, null,
                            tint = barColor, modifier = Modifier.size(22.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(alert.name, fontWeight = FontWeight.Bold,
                            fontSize = 15.sp, color = TextWhite)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(alert.period.label, fontSize = 11.sp, color = TextSecondary)
                            if (sourceLabel != null) {
                                Box(Modifier.size(3.dp).background(TextSecondary.copy(0.4f), CircleShape))
                                Text(sourceLabel, fontSize = 11.sp,
                                    color = profileColor.copy(0.8f))
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Switch(checked = alert.isEnabled, onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor   = AccentTeal,
                            checkedTrackColor   = AccentTeal.copy(.25f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = TextSecondary.copy(.15f)
                        ),
                        modifier = Modifier.size(width = 44.dp, height = 26.dp))
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, null, Modifier.size(15.dp), tint = TextSecondary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, null,
                            Modifier.size(15.dp), tint = ErrorRed.copy(.7f))
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(stringResource(R.string.alert_spent_label),
                        fontSize = 10.sp, color = TextSecondary, letterSpacing = .4.sp)
                    Text("TZS ${fmtAmt(progress?.currentSpending ?: 0.0)}",
                        fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = barColor)
                }
                Column(horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(stringResource(R.string.alert_limit_label),
                        fontSize = 10.sp, color = TextSecondary, letterSpacing = .4.sp)
                    Text("TZS ${fmtAmt(alert.limitAmount)}",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = TextWhite.copy(.7f))
                }
            }

            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(barColor.copy(.12f))) {
                Box(Modifier.fillMaxWidth(animProg).fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(barColor.copy(.7f), barColor)),
                        RoundedCornerShape(4.dp)))
            }

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("${"%.0f".format(pct)}%",
                    fontSize = 11.sp, color = barColor, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.alert_left, fmtAmt(progress?.remaining ?: alert.limitAmount)),
                    fontSize = 11.sp, color = TextSecondary)
            }

            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Default.NotificationsActive, null,
                    tint = TextSecondary, modifier = Modifier.size(11.dp))
                Text(stringResource(R.string.alert_notifies_at, alert.notifyAtPercent),
                    fontSize = 10.sp, color = TextSecondary)
            }

            AnimatedVisibility(pct >= alert.notifyAtPercent) {
                val (bg, fg, msg) = when {
                    pct >= 100 -> Triple(ErrorRed.copy(.12f), ErrorRed,
                        stringResource(R.string.alert_exceeded))
                    else       -> Triple(OrangeWarn.copy(.10f), OrangeWarn,
                        stringResource(R.string.alert_approaching, "%.0f".format(alert.notifyAtPercent.toDouble())))
                }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(bg).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(msg, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Add / Edit alert dialog ───────────────────────────────────────────────────

@Composable
internal fun AlertCardDialog(
    existing: SpendingAlert?,
    preset: AlertPreset?,
    profileColor: Color,
    onSave: (SpendingAlert) -> Unit,
    onDismiss: () -> Unit
) {
    val presetName = preset?.nameRes?.let { stringResource(it) }

    var name           by remember { mutableStateOf(existing?.name ?: presetName ?: "") }
    var limitAmount    by remember { mutableStateOf(
        existing?.limitAmount?.let { fmtAmt(it) } ?: preset?.limit?.let { fmtAmt(it) } ?: "") }
    var selectedPeriod by remember { mutableStateOf(existing?.period ?: preset?.period ?: AlertPeriod.MONTHLY) }
    var notifyAt       by remember { mutableStateOf(existing?.notifyAtPercent?.toString() ?: "80") }
    var sourceFilter     by remember { mutableStateOf(existing?.sourceFilter) }
    var keywordFilter    by remember { mutableStateOf(existing?.keywordFilter ?: "") }
    var categoryFilter   by remember { mutableStateOf(existing?.categoryFilter) }
    var showSourcePicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val categories = listOf(
        "🍔" to "Food", "🚌" to "Transport", "🏠" to "Rent",
        "💊" to "Health", "📚" to "Education", "🛒" to "Shopping",
        "💡" to "Utilities", "🎉" to "Entertainment", "✈️" to "Travel",
        "📱" to "Airtime", "💰" to "Savings", "🔧" to "Repairs"
    )
    var nameError      by remember { mutableStateOf(false) }
    var amountError    by remember { mutableStateOf(false) }

    val sourceLabel = sourceFilter?.let {
        ALL_SENDERS.find { s -> s.id == it }?.let { s -> "${s.emoji} ${s.displayName}" }
    }

    val periodDailyTip   = stringResource(R.string.alert_period_daily_tip)
    val periodWeeklyTip  = stringResource(R.string.alert_period_weekly_tip)
    val periodMonthlyTip = stringResource(R.string.alert_period_monthly_tip)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape     = RoundedCornerShape(28.dp),
            colors    = CardDefaults.cardColors(containerColor = Color(0xFF141E2E)),
            border    = BorderStroke(1.5.dp, profileColor.copy(alpha = 0.45f)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), Alignment.Center) {
                Box(Modifier.width(44.dp).height(4.dp)
                    .clip(CircleShape).background(profileColor.copy(.45f)))
            }

            Column(
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween,
                    Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp))
                            .background(OrangeWarn.copy(.14f)), Alignment.Center) {
                            Icon(Icons.Default.Notifications, null,
                                tint = OrangeWarn, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                when {
                                    existing != null -> stringResource(R.string.alert_edit)
                                    presetName != null -> stringResource(R.string.alert_preset_prefix, presetName)
                                    else -> stringResource(R.string.alert_new_spending)
                                },
                                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(stringResource(R.string.alert_set_limit_auto),
                                fontSize = 11.sp, color = OrangeWarn.copy(.75f))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null,
                            tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent,
                        profileColor.copy(.4f), Color.Transparent))))

                OutlinedTextField(
                    value = name, onValueChange = { name = it; nameError = false },
                    label       = { Text(stringResource(R.string.alert_name_label)) },
                    placeholder = { Text(stringResource(R.string.alert_name_placeholder)) },
                    isError = nameError, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = profileColor, focusedLabelColor = profileColor)
                )

                OutlinedTextField(
                    value = limitAmount, onValueChange = { limitAmount = it; amountError = false },
                    label   = { Text(stringResource(R.string.alert_limit_label2)) },
                    isError = amountError, prefix = { Text("TZS ") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = profileColor, focusedLabelColor = profileColor)
                )

                // Period
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2740)),
                    border = BorderStroke(1.dp, profileColor.copy(.28f)),
                    elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.alert_period_label),
                            fontSize = 11.sp, color = TextSecondary,
                            fontWeight = FontWeight.SemiBold, letterSpacing = .5.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AlertPeriod.entries.forEach { period ->
                                FilterChip(selected = selectedPeriod == period,
                                    onClick = { selectedPeriod = period },
                                    label = { Text(period.label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = profileColor.copy(.15f),
                                        selectedLabelColor = profileColor))
                            }
                        }
                        val periodTip = when (selectedPeriod) {
                            AlertPeriod.DAILY   -> periodDailyTip
                            AlertPeriod.WEEKLY  -> periodWeeklyTip
                            AlertPeriod.MONTHLY -> periodMonthlyTip
                        }
                        Text("ℹ️  $periodTip", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                // Source filter
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2740)),
                    border = BorderStroke(1.dp, profileColor.copy(.28f)),
                    elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column {
                            Text(stringResource(R.string.alert_track_service),
                                fontSize = 11.sp, color = TextSecondary,
                                fontWeight = FontWeight.SemiBold, letterSpacing = .5.sp)
                            Text(stringResource(R.string.alert_track_optional),
                                fontSize = 11.sp, color = TextSecondary.copy(0.6f))
                        }
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sourceFilter != null)
                                    profileColor.copy(0.10f) else Color.White.copy(0.04f))
                                .clickable(remember { MutableInteractionSource() }, null) {
                                    showSourcePicker = !showSourcePicker
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.AccountBalance, null,
                                tint = if (sourceFilter != null) profileColor else TextSecondary,
                                modifier = Modifier.size(16.dp))
                            Text(sourceLabel ?: stringResource(R.string.alert_all_services),
                                fontSize = 13.sp,
                                color = if (sourceFilter != null) profileColor else TextSecondary,
                                modifier = Modifier.weight(1f))
                            if (sourceFilter != null) {
                                IconButton(onClick = { sourceFilter = null },
                                    modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null,
                                        tint = TextSecondary, modifier = Modifier.size(13.dp))
                                }
                            } else {
                                Icon(Icons.Default.ExpandMore, null,
                                    tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                        AnimatedVisibility(showSourcePicker) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                HorizontalDivider(color = profileColor.copy(0.15f))
                                val groups = ALL_SENDERS.groupBy { it.category }
                                groups.forEach { (cat, senders) ->
                                    val catLabel = if (cat == "Bank")
                                        stringResource(R.string.alert_banks_label)
                                    else stringResource(R.string.alert_mobile_money_label)
                                    Text(catLabel, fontSize = 11.sp, color = profileColor,
                                        fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                                    senders.forEach { sender ->
                                        val isSelected = sourceFilter == sender.id
                                        Row(
                                            Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected)
                                                    profileColor.copy(.12f) else Color.Transparent)
                                                .clickable(remember { MutableInteractionSource() }, null) {
                                                    sourceFilter = if (isSelected) null else sender.id
                                                    showSourcePicker = false
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(sender.emoji, fontSize = 15.sp)
                                            Text(sender.displayName, fontSize = 13.sp,
                                                color = if (isSelected) profileColor else TextWhite,
                                                modifier = Modifier.weight(1f))
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, null,
                                                    tint = profileColor, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── How does tracking work — smart tip ───────────────────────
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1C2740))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 14.sp)
                        Text(stringResource(R.string.alert_smart_tip_title),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = OrangeWarn)
                    }
                    Text(stringResource(R.string.alert_smart_tip_body),
                        fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
                }

                // ── Category filter ───────────────────────────────────────────
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2740)),
                    border = BorderStroke(1.dp, profileColor.copy(.28f)),
                    elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween,
                            Alignment.CenterVertically) {
                            Column {
                                Text(stringResource(R.string.alert_category_filter_title),
                                    fontSize = 11.sp, color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold, letterSpacing = .5.sp)
                                Text(stringResource(R.string.alert_category_filter_sub),
                                    fontSize = 11.sp, color = TextSecondary.copy(0.6f))
                            }
                        }
                        // Selected category or picker
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (categoryFilter != null)
                                    profileColor.copy(0.10f) else Color.White.copy(0.04f))
                                .clickable(remember { MutableInteractionSource() }, null) {
                                    showCategoryPicker = !showCategoryPicker
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val catEntry = categories.find { it.second == categoryFilter }
                            Text(catEntry?.first ?: "🏷️", fontSize = 16.sp)
                            Text(
                                categoryFilter ?: stringResource(R.string.alert_no_category),
                                fontSize = 13.sp,
                                color = if (categoryFilter != null) profileColor else TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            if (categoryFilter != null) {
                                IconButton(onClick = { categoryFilter = null },
                                    modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null,
                                        tint = TextSecondary, modifier = Modifier.size(13.dp))
                                }
                            } else {
                                Icon(Icons.Default.ExpandMore, null,
                                    tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                        AnimatedVisibility(showCategoryPicker) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                HorizontalDivider(color = profileColor.copy(0.15f))
                                val rows = categories.chunked(3)
                                rows.forEach { row ->
                                    Row(Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach { (emoji, label) ->
                                            val isSelected = categoryFilter == label
                                            Box(
                                                Modifier.weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected)
                                                        profileColor.copy(.18f) else Color.White.copy(.04f))
                                                    .clickable(remember { MutableInteractionSource() }, null) {
                                                        categoryFilter = if (isSelected) null else label
                                                        showCategoryPicker = false
                                                    }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(emoji, fontSize = 18.sp)
                                                    Text(label, fontSize = 10.sp,
                                                        color = if (isSelected) profileColor else TextSecondary)
                                                }
                                            }
                                        }
                                        // Fill empty cells
                                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Keyword filter ────────────────────────────────────────────
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2740)),
                    border = BorderStroke(1.dp, profileColor.copy(.28f)),
                    elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.alert_keyword_filter_title),
                            fontSize = 11.sp, color = TextSecondary,
                            fontWeight = FontWeight.SemiBold, letterSpacing = .5.sp)
                        Text(stringResource(R.string.alert_keyword_filter_sub),
                            fontSize = 11.sp, color = TextSecondary.copy(0.6f))
                        OutlinedTextField(
                            value = keywordFilter,
                            onValueChange = { keywordFilter = it },
                            placeholder = { Text(stringResource(R.string.alert_keyword_placeholder),
                                color = TextSecondary.copy(.5f), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null,
                                tint = TextSecondary, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                AnimatedVisibility(keywordFilter.isNotEmpty()) {
                                    IconButton(onClick = { keywordFilter = "" },
                                        modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.Close, null,
                                            tint = TextSecondary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = profileColor.copy(.6f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = profileColor,
                                focusedContainerColor = Color(0xFF141E2E),
                                unfocusedContainerColor = Color(0xFF141E2E)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Notify at %
                OutlinedTextField(
                    value = notifyAt, onValueChange = { notifyAt = it },
                    label  = { Text(stringResource(R.string.alert_notify_label)) },
                    suffix = { Text("%") },
                    supportingText = { Text(stringResource(R.string.alert_notify_hint)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = profileColor, focusedLabelColor = profileColor)
                )

                Button(
                    onClick = {
                        val parsed = limitAmount.replace(",", "").toDoubleOrNull()
                        nameError   = name.isBlank()
                        amountError = parsed == null || parsed <= 0
                        if (!nameError && !amountError) {
                            onSave(SpendingAlert(
                                id             = existing?.id ?: 0L,
                                name           = name.trim(),
                                limitAmount    = parsed!!,
                                period         = selectedPeriod,
                                notifyAtPercent = notifyAt.toIntOrNull()?.coerceIn(1, 100) ?: 80,
                                sourceFilter   = sourceFilter,
                                keywordFilter  = keywordFilter.trim().takeIf { it.isNotEmpty() },
                                categoryFilter = categoryFilter
                            ))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = profileColor, contentColor = Color(0xFF05142A))
                ) {
                    Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.save_alert),
                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}