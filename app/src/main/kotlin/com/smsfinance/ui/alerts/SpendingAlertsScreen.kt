package com.smsfinance.ui.alerts
import com.smsfinance.R

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.smsfinance.ui.components.LocalProfileColor
import com.smsfinance.ui.components.*
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.SpendingAlertsViewModel

@Composable
fun SpendingAlertsScreen(
    viewModel: SpendingAlertsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val profileColor   = LocalProfileColor.current
    var showAddDialog  by remember { mutableStateOf(false) }
    var editingAlert   by remember { mutableStateOf<SpendingAlert?>(null) }

    val fabPulse = rememberInfiniteTransition(label = "fab")
    val fabGlow  by fabPulse.animateFloat(.50f, .90f,
        infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "fg")
    val fabScale by fabPulse.animateFloat(.97f, 1.03f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "fs")

    AppScreenScaffold(
        title          = stringResource(R.string.spending_alerts_title),
        subtitle       = stringResource(R.string.set_limits_budget),
        onNavigateBack = onNavigateBack
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = AccentTeal, strokeWidth = 3.dp)
                }
                uiState.alerts.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔔", fontSize = 52.sp)
                        Text(stringResource(R.string.no_alerts_yet), fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, color = TextWhite)
                        Text(stringResource(R.string.no_alerts_yet_sub),
                            fontSize = 13.sp, color = TextSecondary,
                            textAlign = TextAlign.Center)
                    }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
            }

            // ── Glowing FAB ───────────────────────────────────────────────────
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(82.dp)
                        .graphicsLayer { alpha = fabGlow }
                        .background(
                            Brush.radialGradient(listOf(OrangeWarn.copy(.50f), Color.Transparent)),
                            CircleShape
                        )
                )
                Box(
                    Modifier
                        .size(64.dp)
                        .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(OrangeWarn, Color(0xFFFF6D00)))),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.Add, "New Alert",
                            tint = Color(0xFF1A0A00), modifier = Modifier.size(32.dp))
                    }
                }
                Text("New Alert", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = OrangeWarn.copy(.8f),
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = 54.dp))
            }
        }
    }

    if (showAddDialog || editingAlert != null) {
        AlertCardDialog(
            existing    = editingAlert,
            profileColor = profileColor,
            onSave      = { alert -> viewModel.saveAlert(alert); showAddDialog = false; editingAlert = null },
            onDismiss   = { showAddDialog = false; editingAlert = null }
        )
    }
}

// ── Alert card — profile-colour border ─────────────────────────────────────────
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

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border    = BorderStroke(1.dp, profileColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                            .background(barColor.copy(.14f)), Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, null,
                            tint = barColor, modifier = Modifier.size(22.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(alert.name, fontWeight = FontWeight.Bold,
                            fontSize = 15.sp, color = TextWhite)
                        Text(alert.period.label, fontSize = 11.sp, color = TextSecondary)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Switch(
                        checked         = alert.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor   = AccentTeal,
                            checkedTrackColor   = AccentTeal.copy(.25f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = TextSecondary.copy(.15f)
                        ),
                        modifier = Modifier.size(width = 44.dp, height = 26.dp)
                    )
                    IconButton(onClick = onEdit,   modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit,   null, Modifier.size(15.dp), tint = TextSecondary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, null, Modifier.size(15.dp), tint = ErrorRed.copy(.7f))
                    }
                }
            }

            // Amount row
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(stringResource(R.string.spent), fontSize = 10.sp,
                        color = TextSecondary, letterSpacing = .4.sp)
                    Text("TZS ${fmtAmt(progress?.currentSpending ?: 0.0)}",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = barColor)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(stringResource(R.string.limit), fontSize = 10.sp,
                        color = TextSecondary, letterSpacing = .4.sp)
                    Text("TZS ${fmtAmt(alert.limitAmount)}",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhite.copy(.7f))
                }
            }

            // Progress bar
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                .background(barColor.copy(.12f))) {
                Box(Modifier.fillMaxWidth(animProg).fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(listOf(barColor.copy(.7f), barColor)),
                        RoundedCornerShape(3.dp)
                    ))
            }

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("${"%.0f".format(pct)}% used",
                    fontSize = 11.sp, color = barColor, fontWeight = FontWeight.SemiBold)
                Text("TZS ${fmtAmt(progress?.remaining ?: alert.limitAmount)} left",
                    fontSize = 11.sp, color = TextSecondary)
            }

            if (pct >= 80) {
                val (bg, fg, msg) = when {
                    pct >= 100 -> Triple(ErrorRed.copy(.12f), ErrorRed, "🚨  Limit exceeded!")
                    else       -> Triple(OrangeWarn.copy(.10f), OrangeWarn, "⚠️  Approaching limit!")
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(bg).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(msg, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Add / Edit alert — card sheet matching transaction card style ──────────────
@Composable
fun AlertCardDialog(
    existing: SpendingAlert?,
    profileColor: Color,
    onSave: (SpendingAlert) -> Unit,
    onDismiss: () -> Unit
) {
    var name           by remember { mutableStateOf(existing?.name ?: "") }
    var limitAmount    by remember { mutableStateOf(existing?.limitAmount?.let { "%.0f".format(it) } ?: "") }
    var selectedPeriod by remember { mutableStateOf(existing?.period ?: AlertPeriod.MONTHLY) }
    var notifyAt       by remember { mutableStateOf(existing?.notifyAtPercent?.toString() ?: "80") }
    var nameError      by remember { mutableStateOf(false) }
    var amountError    by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape     = RoundedCornerShape(28.dp),
            colors    = CardDefaults.cardColors(containerColor = Color(0xFF141E2E)),
            border    = BorderStroke(1.5.dp, profileColor.copy(alpha = 0.45f)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            // Profile-colour pill handle
            Box(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.width(44.dp).height(4.dp)
                        .clip(CircleShape).background(profileColor.copy(.45f))
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Header ────────────────────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.size(38.dp).clip(RoundedCornerShape(11.dp))
                                .background(OrangeWarn.copy(.14f)),
                            Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, null,
                                tint = OrangeWarn, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                if (existing == null) "New Spending Alert" else "Edit Alert",
                                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextWhite
                            )
                            Text("Set your spending limit",
                                fontSize = 11.sp, color = OrangeWarn.copy(.75f))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null,
                            tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }

                // Profile-colour accent divider
                Box(
                    Modifier.fillMaxWidth().height(1.dp).background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, profileColor.copy(.4f), Color.Transparent)
                        )
                    )
                )

                // ── Name field ────────────────────────────────────────────────
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label       = { Text(stringResource(R.string.alert_name_label)) },
                    placeholder = { Text("e.g. Monthly Food") },
                    isError     = nameError,
                    singleLine  = true,
                    modifier    = Modifier.fillMaxWidth(),
                    shape       = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = profileColor,
                        focusedLabelColor  = profileColor
                    )
                )

                // ── Limit field ───────────────────────────────────────────────
                OutlinedTextField(
                    value = limitAmount,
                    onValueChange = { limitAmount = it; amountError = false },
                    label   = { Text(stringResource(R.string.limit_amount_label)) },
                    isError = amountError,
                    prefix  = { Text("TZS ") },
                    singleLine = true,
                    modifier   = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number),
                    shape  = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = profileColor,
                        focusedLabelColor  = profileColor
                    )
                )

                // ── Period selector card with profile border ───────────────────
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color(0xFF1C2740)),
                    border    = BorderStroke(1.dp, profileColor.copy(.28f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.period_label),
                            fontSize = 11.sp, color = TextSecondary,
                            fontWeight = FontWeight.SemiBold, letterSpacing = .5.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AlertPeriod.entries.forEach { period ->
                                FilterChip(
                                    selected = selectedPeriod == period,
                                    onClick  = { selectedPeriod = period },
                                    label    = { Text(period.label, fontSize = 12.sp) },
                                    colors   = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = profileColor.copy(.15f),
                                        selectedLabelColor     = profileColor
                                    )
                                )
                            }
                        }
                    }
                }

                // ── Notify at % ───────────────────────────────────────────────
                OutlinedTextField(
                    value = notifyAt,
                    onValueChange = { notifyAt = it },
                    label  = { Text(stringResource(R.string.notify_at_pct)) },
                    suffix = { Text("%") },
                    singleLine = true,
                    modifier   = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number),
                    shape  = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = profileColor,
                        focusedLabelColor  = profileColor
                    )
                )

                // ── Save button ───────────────────────────────────────────────
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
                                notifyAtPercent = notifyAt.toIntOrNull()?.coerceIn(1, 100) ?: 80
                            ))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = profileColor,
                        contentColor   = Color(0xFF05142A)
                    )
                ) {
                    Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.save_alert), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}