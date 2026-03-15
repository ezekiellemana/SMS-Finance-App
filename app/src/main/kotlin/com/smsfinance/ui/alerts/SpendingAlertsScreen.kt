@file:Suppress("ASSIGNED_BUT_NEVER_READ_VARIABLE")
@file:OptIn(ExperimentalMaterial3Api::class)
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
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.AlertCheckResult
import com.smsfinance.domain.model.AlertPeriod
import com.smsfinance.domain.model.SpendingAlert
import com.smsfinance.ui.components.*
import com.smsfinance.ui.onboarding.ALL_SENDERS
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.SpendingAlertsViewModel

// ── Preset definition ─────────────────────────────────────────────────────────
data class AlertPreset(
    val emoji: String,
    val nameRes: Int,
    val limit: Double,
    val period: AlertPeriod,
    val tipRes: Int
)

private val PRESETS = listOf(
    AlertPreset("🍔", R.string.preset_daily_food_name,      15_000.0, AlertPeriod.DAILY,   R.string.preset_daily_food_tip),
    AlertPreset("📱", R.string.preset_mobile_data_name,     30_000.0, AlertPeriod.MONTHLY, R.string.preset_mobile_data_tip),
    AlertPreset("🚌", R.string.preset_transport_name,       50_000.0, AlertPeriod.WEEKLY,  R.string.preset_transport_tip),
    AlertPreset("🛒", R.string.preset_groceries_name,      150_000.0, AlertPeriod.MONTHLY, R.string.preset_groceries_tip),
    AlertPreset("💸", R.string.preset_total_spending_name, 500_000.0, AlertPeriod.MONTHLY, R.string.preset_total_spending_tip),
    AlertPreset("🏥", R.string.preset_health_name,          80_000.0, AlertPeriod.MONTHLY, R.string.preset_health_tip),
)

// ── Main screen ───────────────────────────────────────────────────────────────
@Composable
fun SpendingAlertsScreen(
    viewModel: SpendingAlertsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val profileColor  = LocalProfileColor.current

    var showAddDialog  by remember { mutableStateOf(false) }
    var editingAlert   by remember { mutableStateOf<SpendingAlert?>(null) }
    var activePreset   by remember { mutableStateOf<AlertPreset?>(null) }

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
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { AlertHowItWorksBanner(profileColor) }

                if (uiState.alerts.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(stringResource(R.string.alert_your_alerts),
                                fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextWhite)
                            Text("${uiState.alerts.size}", fontSize = 12.sp, color = profileColor)
                        }
                    }
                    items(uiState.alerts, key = { it.id }) { alert ->
                        SpendingAlertCard(
                            alert        = alert,
                            progress     = uiState.alertProgress[alert.id],
                            profileColor = profileColor,
                            onEdit       = { editingAlert = alert },
                            onDelete     = { viewModel.deleteAlert(alert) },
                            onToggle     = { viewModel.toggleAlert(alert) }
                        )
                    }
                }

                if (uiState.alerts.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(140.dp), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🔔", fontSize = 44.sp)
                                Text(stringResource(R.string.alert_no_alerts_yet),
                                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                Text(stringResource(R.string.alert_no_alerts_use_preset),
                                    fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                item {
                    var presetsExpanded by remember { mutableStateOf(false) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(profileColor.copy(0.07f))
                                .border(1.dp, profileColor.copy(0.2f), RoundedCornerShape(14.dp))
                                .clickable(remember { MutableInteractionSource() }, null) {
                                    presetsExpanded = !presetsExpanded
                                }
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            Arrangement.SpaceBetween, Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("⚡", fontSize = 14.sp)
                                Text(stringResource(R.string.alert_quick_presets),
                                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = profileColor)
                            }
                            Icon(if (presetsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null, tint = profileColor.copy(0.7f), modifier = Modifier.size(18.dp))
                        }
                        AnimatedVisibility(presetsExpanded,
                            enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                            exit  = fadeOut(tween(150)) + shrinkVertically(tween(200))
                        ) {
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(PRESETS) { p ->
                                    AlertPresetChip(p, profileColor) {
                                        activePreset    = p
                                        showAddDialog   = true
                                        presetsExpanded = false
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(4.dp)) }
            }

            Column(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(82.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(82.dp).graphicsLayer { alpha = fabGlow }
                        .background(Brush.radialGradient(listOf(OrangeWarn.copy(.50f), Color.Transparent)), CircleShape))
                    Box(
                        Modifier.size(60.dp)
                            .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                            .shadow(14.dp, CircleShape).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(OrangeWarn, Color(0xFFFF6D00)))),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { activePreset = null; showAddDialog = true },
                            modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.Add, stringResource(R.string.alert_new_label),
                                tint = Color(0xFF1A0A00), modifier = Modifier.size(30.dp))
                        }
                    }
                }
                Text(stringResource(R.string.alert_new_label), fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, color = OrangeWarn.copy(.85f))
            }
        }
    }

    if (showAddDialog || editingAlert != null) {
        SpendingAlertDialog(
            existing     = editingAlert,
            preset       = activePreset,
            profileColor = profileColor,
            onSave       = { alert ->
                viewModel.saveAlert(alert)
                showAddDialog = false; editingAlert = null; activePreset = null
            },
            onDismiss = { showAddDialog = false; editingAlert = null; activePreset = null }
        )
    }
}

// ── How it works banner ───────────────────────────────────────────────────────
@Composable
fun AlertHowItWorksBanner(accent: Color) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
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

// ── Preset chip ───────────────────────────────────────────────────────────────
@Composable
fun AlertPresetChip(p: AlertPreset, accent: Color, onTap: () -> Unit) {
    val name = stringResource(p.nameRes)
    val tip  = stringResource(p.tipRes)
    Column(
        Modifier.width(130.dp).clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1C2537))
            .border(1.dp, accent.copy(0.18f), RoundedCornerShape(18.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onTap)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(accent.copy(0.12f)), Alignment.Center) {
            Text(p.emoji, fontSize = 22.sp)
        }
        Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp,
            color = TextWhite, textAlign = TextAlign.Center, maxLines = 1)
        Text("TZS ${fmtAmt(p.limit)}", fontSize = 11.sp, color = accent,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(p.period.label, fontSize = 10.sp, color = TextSecondary, textAlign = TextAlign.Center)
        Text(tip, fontSize = 10.sp, color = TextSecondary, textAlign = TextAlign.Center,
            maxLines = 2, lineHeight = 13.sp)
        Box(Modifier.size(24.dp).clip(CircleShape).background(accent.copy(0.15f)), Alignment.Center) {
            Icon(Icons.Default.Add, null, tint = accent, modifier = Modifier.size(13.dp))
        }
    }
}

// ── Alert card ────────────────────────────────────────────────────────────────
@Composable
fun SpendingAlertCard(
    alert: SpendingAlert,
    progress: AlertCheckResult?,
    profileColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    val pct      = progress?.percentUsed ?: 0.0
    val barColor = when { pct >= 100 -> ErrorRed; pct >= 80 -> OrangeWarn; else -> AccentTeal }
    val animProg by animateFloatAsState((pct / 100.0).coerceIn(0.0, 1.0).toFloat(),
        tween(700), label = "prog")
    val sourceLabel = alert.sourceFilter?.let {
        ALL_SENDERS.find { s -> s.id == it }?.displayName ?: it
    }

    Card(
        modifier  = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
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
                        Icon(Icons.Default.Notifications, null, tint = barColor,
                            modifier = Modifier.size(22.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(alert.name, fontWeight = FontWeight.Bold,
                            fontSize = 15.sp, color = TextWhite)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(alert.period.label, fontSize = 11.sp, color = TextSecondary)
                            if (sourceLabel != null) {
                                Box(Modifier.size(3.dp).background(TextSecondary.copy(0.4f), CircleShape))
                                Text(sourceLabel, fontSize = 11.sp, color = profileColor.copy(0.8f))
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Switch(checked = alert.isEnabled, onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentTeal, checkedTrackColor = AccentTeal.copy(.25f),
                            uncheckedThumbColor = TextSecondary, uncheckedTrackColor = TextSecondary.copy(.15f)
                        ), modifier = Modifier.size(width = 44.dp, height = 26.dp))
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, null, Modifier.size(15.dp), tint = TextSecondary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, null, Modifier.size(15.dp), tint = ErrorRed.copy(.7f))
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
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(stringResource(R.string.alert_limit_label),
                        fontSize = 10.sp, color = TextSecondary, letterSpacing = .4.sp)
                    Text("TZS ${fmtAmt(alert.limitAmount)}", fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = TextWhite.copy(.7f))
                }
            }

            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(barColor.copy(.12f))) {
                Box(Modifier.fillMaxWidth(animProg).fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(barColor.copy(.7f), barColor)),
                        RoundedCornerShape(4.dp)))
            }

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("${"%.0f".format(pct)}%", fontSize = 11.sp,
                    color = barColor, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.alert_left, fmtAmt(progress?.remaining ?: alert.limitAmount)),
                    fontSize = 11.sp, color = TextSecondary)
            }

            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Default.NotificationsActive, null, tint = TextSecondary,
                    modifier = Modifier.size(11.dp))
                Text(stringResource(R.string.alert_notifies_at, alert.notifyAtPercent),
                    fontSize = 10.sp, color = TextSecondary)
            }

            AnimatedVisibility(pct >= alert.notifyAtPercent) {
                val (bg, fg, msg) = when {
                    pct >= 100 -> Triple(ErrorRed.copy(.12f), ErrorRed,
                        stringResource(R.string.alert_exceeded))
                    else -> Triple(OrangeWarn.copy(.10f), OrangeWarn,
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

// ── Add / Edit dialog ─────────────────────────────────────────────────────────
@Composable
fun SpendingAlertDialog(
    existing: SpendingAlert?,
    preset: AlertPreset?,
    profileColor: Color,
    onSave: (SpendingAlert) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val presetName = preset?.nameRes?.let { stringResource(it) }

    var name           by remember { mutableStateOf(existing?.name ?: presetName ?: "") }
    var limitAmount    by remember { mutableStateOf(
        existing?.limitAmount?.let { fmtAmt(it) } ?: preset?.limit?.let { fmtAmt(it) } ?: "") }
    var selectedPeriod by remember { mutableStateOf(existing?.period ?: preset?.period ?: AlertPeriod.MONTHLY) }
    var notifyAt       by remember { mutableStateOf(existing?.notifyAtPercent?.toString() ?: "80") }
    var sourceFilter   by remember { mutableStateOf(existing?.sourceFilter) }
    var keywordFilter  by remember { mutableStateOf(existing?.keywordFilter ?: "") }
    var categoryFilter by remember { mutableStateOf(existing?.categoryFilter) }
    var showSourcePicker   by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var nameError   by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    val periodDailyTip   = stringResource(R.string.alert_period_daily_tip)
    val periodWeeklyTip  = stringResource(R.string.alert_period_weekly_tip)
    val periodMonthlyTip = stringResource(R.string.alert_period_monthly_tip)

    val categories = listOf(
        "🍔" to "Food", "🚌" to "Transport", "🏠" to "Rent",
        "💊" to "Health", "📚" to "Education", "🛒" to "Shopping",
        "💡" to "Utilities", "🎉" to "Entertainment", "✈️" to "Travel",
        "📱" to "Airtime", "💰" to "Savings", "🔧" to "Repairs"
    )
    val sourceLabel = sourceFilter?.let {
        ALL_SENDERS.find { s -> s.id == it }?.let { s -> "${s.emoji} ${s.displayName}" }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color(0xFF141E2E),
        tonalElevation   = 0.dp,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), Alignment.Center) {
                Box(Modifier.width(36.dp).height(3.dp).clip(CircleShape)
                    .background(profileColor.copy(.45f)))
            }
        }
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp))
                        .background(OrangeWarn.copy(.14f)), Alignment.Center) {
                        Icon(Icons.Default.Notifications, null, tint = OrangeWarn,
                            modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(when { existing != null -> stringResource(R.string.alert_edit)
                            presetName != null -> stringResource(R.string.alert_preset_prefix, presetName)
                            else -> stringResource(R.string.alert_new_spending) },
                            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text(stringResource(R.string.alert_set_limit_auto),
                            fontSize = 11.sp, color = OrangeWarn.copy(.75f))
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = TextSecondary,
                        modifier = Modifier.size(20.dp))
                }
            }

            // Name
            OutlinedTextField(value = name, onValueChange = { name = it; nameError = false },
                label = { Text(stringResource(R.string.alert_name_label)) },
                placeholder = { Text(stringResource(R.string.alert_name_placeholder)) },
                isError = nameError, singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = profileColor, focusedLabelColor = profileColor))

            // Limit
            OutlinedTextField(value = limitAmount, onValueChange = { limitAmount = it; amountError = false },
                label = { Text(stringResource(R.string.alert_limit_label2)) },
                isError = amountError, prefix = { Text("TZS ") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = profileColor, focusedLabelColor = profileColor))

            // Period
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2740)),
                border = BorderStroke(1.dp, profileColor.copy(.28f)),
                elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.alert_period_label), fontSize = 11.sp,
                        color = TextSecondary, fontWeight = FontWeight.SemiBold, letterSpacing = .5.sp)
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
                    Text("ℹ️  ${when (selectedPeriod) {
                        AlertPeriod.DAILY   -> periodDailyTip
                        AlertPeriod.WEEKLY  -> periodWeeklyTip
                        AlertPeriod.MONTHLY -> periodMonthlyTip
                    }}", fontSize = 11.sp, color = TextSecondary)
                }
            }

            // Smart tip
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1C2740)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 14.sp)
                    Text(stringResource(R.string.alert_smart_tip_title), fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, color = OrangeWarn)
                }
                Text(stringResource(R.string.alert_smart_tip_body),
                    fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
            }

            // Category filter
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2740)),
                border = BorderStroke(1.dp, profileColor.copy(.28f)),
                elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text(stringResource(R.string.alert_category_filter_title), fontSize = 11.sp,
                            color = TextSecondary, fontWeight = FontWeight.SemiBold, letterSpacing = .5.sp)
                        Text(stringResource(R.string.alert_category_filter_sub),
                            fontSize = 11.sp, color = TextSecondary.copy(0.6f))
                    }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (categoryFilter != null) profileColor.copy(0.10f) else Color.White.copy(0.04f))
                        .clickable(remember { MutableInteractionSource() }, null) {
                            showCategoryPicker = !showCategoryPicker
                        }.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val catEntry = categories.find { it.second == categoryFilter }
                        Text(catEntry?.first ?: "🏷️", fontSize = 16.sp)
                        Text(categoryFilter ?: stringResource(R.string.alert_no_category),
                            fontSize = 13.sp,
                            color = if (categoryFilter != null) profileColor else TextSecondary,
                            modifier = Modifier.weight(1f))
                        if (categoryFilter != null) {
                            IconButton(onClick = { categoryFilter = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = TextSecondary,
                                    modifier = Modifier.size(13.dp))
                            }
                        } else {
                            Icon(Icons.Default.ExpandMore, null, tint = TextSecondary,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                    AnimatedVisibility(showCategoryPicker) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HorizontalDivider(color = profileColor.copy(0.15f))
                            categories.chunked(3).forEach { row ->
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { (emoji, label) ->
                                        val isSel = categoryFilter == label
                                        Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                            .background(if (isSel) profileColor.copy(.18f) else Color.White.copy(.04f))
                                            .clickable(remember { MutableInteractionSource() }, null) {
                                                categoryFilter = if (isSel) null else label
                                                showCategoryPicker = false
                                            }.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(emoji, fontSize = 18.sp)
                                                Text(label, fontSize = 10.sp,
                                                    color = if (isSel) profileColor else TextSecondary)
                                            }
                                        }
                                    }
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }

            // Keyword filter
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2740)),
                border = BorderStroke(1.dp, profileColor.copy(.28f)),
                elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.alert_keyword_filter_title), fontSize = 11.sp,
                        color = TextSecondary, fontWeight = FontWeight.SemiBold, letterSpacing = .5.sp)
                    Text(stringResource(R.string.alert_keyword_filter_sub),
                        fontSize = 11.sp, color = TextSecondary.copy(0.6f))
                    OutlinedTextField(value = keywordFilter, onValueChange = { keywordFilter = it },
                        placeholder = { Text(stringResource(R.string.alert_keyword_placeholder),
                            color = TextSecondary.copy(.5f), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary,
                            modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            AnimatedVisibility(keywordFilter.isNotEmpty()) {
                                IconButton(onClick = { keywordFilter = "" },
                                    modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Close, null, tint = TextSecondary,
                                        modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = profileColor.copy(.6f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            cursorColor = profileColor,
                            focusedContainerColor = Color(0xFF141E2E),
                            unfocusedContainerColor = Color(0xFF141E2E)),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                }
            }

            // Service filter
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2740)),
                border = BorderStroke(1.dp, profileColor.copy(.28f)),
                elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text(stringResource(R.string.alert_track_service), fontSize = 11.sp,
                            color = TextSecondary, fontWeight = FontWeight.SemiBold, letterSpacing = .5.sp)
                        Text(stringResource(R.string.alert_track_optional),
                            fontSize = 11.sp, color = TextSecondary.copy(0.6f))
                    }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (sourceFilter != null) profileColor.copy(0.10f) else Color.White.copy(0.04f))
                        .clickable(remember { MutableInteractionSource() }, null) {
                            showSourcePicker = !showSourcePicker
                        }.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.AccountBalance, null,
                            tint = if (sourceFilter != null) profileColor else TextSecondary,
                            modifier = Modifier.size(16.dp))
                        Text(sourceLabel ?: stringResource(R.string.alert_all_services),
                            fontSize = 13.sp,
                            color = if (sourceFilter != null) profileColor else TextSecondary,
                            modifier = Modifier.weight(1f))
                        if (sourceFilter != null) {
                            IconButton(onClick = { sourceFilter = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = TextSecondary,
                                    modifier = Modifier.size(13.dp))
                            }
                        } else {
                            Icon(Icons.Default.ExpandMore, null, tint = TextSecondary,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                    AnimatedVisibility(showSourcePicker) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HorizontalDivider(color = profileColor.copy(0.15f))
                            ALL_SENDERS.groupBy { it.category }.forEach { (cat, senders) ->
                                val catLabel = if (cat == "Bank") stringResource(R.string.alert_banks_label)
                                else stringResource(R.string.alert_mobile_money_label)
                                Text(catLabel, fontSize = 11.sp, color = profileColor,
                                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                                senders.forEach { sender ->
                                    val isSel = sourceFilter == sender.id
                                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) profileColor.copy(.12f) else Color.Transparent)
                                        .clickable(remember { MutableInteractionSource() }, null) {
                                            sourceFilter = if (isSel) null else sender.id
                                            showSourcePicker = false
                                        }.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(sender.emoji, fontSize = 15.sp)
                                        Text(sender.displayName, fontSize = 13.sp,
                                            color = if (isSel) profileColor else TextWhite,
                                            modifier = Modifier.weight(1f))
                                        if (isSel) Icon(Icons.Default.Check, null,
                                            tint = profileColor, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Notify at %
            OutlinedTextField(value = notifyAt, onValueChange = { notifyAt = it },
                label = { Text(stringResource(R.string.alert_notify_label)) },
                suffix = { Text("%") },
                supportingText = { Text(stringResource(R.string.alert_notify_hint)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = profileColor, focusedLabelColor = profileColor))

            // Save
            Button(
                onClick = {
                    val parsed = limitAmount.replace(",", "").toDoubleOrNull()
                    nameError   = name.isBlank()
                    amountError = parsed == null || parsed <= 0
                    if (!nameError && !amountError) {
                        onSave(SpendingAlert(
                            id              = existing?.id ?: 0L,
                            name            = name.trim(),
                            limitAmount     = parsed!!,
                            period          = selectedPeriod,
                            notifyAtPercent = notifyAt.toIntOrNull()?.coerceIn(1, 100) ?: 80,
                            sourceFilter    = sourceFilter,
                            keywordFilter   = keywordFilter.trim().takeIf { it.isNotEmpty() },
                            categoryFilter  = categoryFilter
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
                Text(stringResource(R.string.save_alert), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}