package com.smsfinance.ui.alerts

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.AlertCheckResult
import com.smsfinance.domain.model.AlertPeriod
import com.smsfinance.domain.model.SpendingAlert
import com.smsfinance.ui.components.*
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.SpendingAlertsViewModel

@Suppress("DEPRECATION")
@Composable
fun SpendingAlertsScreen(
    viewModel: SpendingAlertsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog  by remember { mutableStateOf(false) }
    var editingAlert   by remember { mutableStateOf<SpendingAlert?>(null) }

    // FAB pulse animation
    val fabPulse = rememberInfiniteTransition(label = "fab")
    val fabGlow  by fabPulse.animateFloat(.50f, .90f,
        infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "fg")
    val fabScale by fabPulse.animateFloat(.97f, 1.03f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "fs")

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            Row(
                Modifier.fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextWhite)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Spending Alerts", fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, color = TextWhite)
                    Text("Set limits to stay on budget",
                        fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(Modifier.width(48.dp))
            }
        }
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
                        Text("No alerts yet", fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Tap the button below to create\nyour first spending limit",
                            fontSize = 13.sp, color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
                            alert    = alert,
                            progress = uiState.alertProgress[alert.id],
                            onEdit   = { editingAlert = alert },
                            onDelete = { viewModel.deleteAlert(alert) },
                            onToggle = { viewModel.toggleAlert(alert) }
                        )
                    }
                }
            }

            // ── Big glowing FAB ───────────────────────────────────────────────
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow ring
                Box(
                    Modifier.size(82.dp)
                        .graphicsLayer { alpha = fabGlow }
                        .background(
                            Brush.radialGradient(listOf(OrangeWarn.copy(.50f), Color.Transparent)),
                            CircleShape
                        )
                )
                // FAB
                Box(
                    Modifier
                        .size(64.dp)
                        .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(OrangeWarn, Color(0xFFFF6D00)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick  = { showAddDialog = true },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(Icons.Default.Add, "New Alert",
                            tint = Color(0xFF1A0A00), modifier = Modifier.size(32.dp))
                    }
                }
                // Label below FAB
                Text(
                    "New Alert",
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = OrangeWarn.copy(.8f),
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = 54.dp)
                )
            }
        }
    }

    if (showAddDialog || editingAlert != null) {
        AlertDialog_AddEdit(
            existing  = editingAlert,
            onSave    = { alert -> viewModel.saveAlert(alert); showAddDialog = false; editingAlert = null },
            onDismiss = { showAddDialog = false; editingAlert = null }
        )
    }
}

@Composable
fun AlertCard(
    alert: SpendingAlert, progress: AlertCheckResult?,
    onEdit: () -> Unit, onDelete: () -> Unit, onToggle: () -> Unit
) {
    val pct      = progress?.percentUsed ?: 0.0
    val barColor = when {
        pct >= 100 -> ErrorRed
        pct >= 80  -> OrangeWarn
        else       -> AccentTeal
    }
    val animProg by animateFloatAsState(
        (pct / 100.0).coerceIn(0.0, 1.0).toFloat(), tween(700), label = "prog")

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C2740))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header row
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
                    Text(alert.period.label, fontSize = 11.sp, color = TextSecondary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Switch(
                    checked         = alert.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors          = SwitchDefaults.colors(
                        checkedThumbColor  = AccentTeal,
                        checkedTrackColor  = AccentTeal.copy(.25f),
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = TextSecondary.copy(.15f)
                    ),
                    modifier = Modifier.size(width = 44.dp, height = 26.dp)
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Edit, null, Modifier.size(15.dp), tint = TextSecondary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Delete, null, Modifier.size(15.dp), tint = ErrorRed.copy(.7f))
                }
            }
        }

        // Amount row
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Spent", fontSize = 10.sp, color = TextSecondary, letterSpacing = .4.sp)
                Text("TZS ${fmtAmt(progress?.currentSpending ?: 0.0)}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = barColor)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Limit", fontSize = 10.sp, color = TextSecondary, letterSpacing = .4.sp)
                Text("TZS ${fmtAmt(alert.limitAmount)}",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhite.copy(.7f))
            }
        }

        // Progress bar
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
            .background(barColor.copy(.12f))) {
            Box(Modifier.fillMaxWidth(animProg).fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(barColor.copy(.7f), barColor)
                    ),
                    RoundedCornerShape(3.dp)
                ))
        }

        // Percent + remaining
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("${"%.0f".format(pct)}% used",
                fontSize = 11.sp, color = barColor, fontWeight = FontWeight.SemiBold)
            Text("TZS ${fmtAmt(progress?.remaining ?: alert.limitAmount)} left",
                fontSize = 11.sp, color = TextSecondary)
        }

        // Warning banner
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

@Composable
fun AlertDialog_AddEdit(
    existing: SpendingAlert?,
    onSave: (SpendingAlert) -> Unit,
    onDismiss: () -> Unit
) {
    var name           by remember { mutableStateOf(existing?.name ?: "") }
    var limitAmount    by remember { mutableStateOf(existing?.limitAmount?.let { "%.0f".format(it) } ?: "") }
    var selectedPeriod by remember { mutableStateOf(existing?.period ?: AlertPeriod.MONTHLY) }
    var notifyAt       by remember { mutableStateOf(existing?.notifyAtPercent?.toString() ?: "80") }
    var nameError      by remember { mutableStateOf(false) }
    var amountError    by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF1C2740),
        title = {
            Text(if (existing == null) "New Spending Alert" else "Edit Alert",
                fontWeight = FontWeight.Bold, color = TextWhite)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; nameError = false },
                    label = { Text("Alert Name") },
                    placeholder = { Text("e.g. Monthly Food") },
                    isError = nameError, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = limitAmount, onValueChange = { limitAmount = it; amountError = false },
                    label = { Text("Limit Amount") },
                    isError = amountError, prefix = { Text("TZS ") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                Text("Period", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlertPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick  = { selectedPeriod = period },
                            label    = { Text(period.label, fontSize = 12.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentTeal.copy(.15f),
                                selectedLabelColor     = AccentTeal
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = notifyAt, onValueChange = { notifyAt = it },
                    label = { Text("Notify at (%)") }, suffix = { Text("%") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = limitAmount.replace(",", "").toDoubleOrNull()
                    nameError = name.isBlank()
                    amountError = parsed == null || parsed <= 0
                    if (!nameError && !amountError) {
                        onSave(SpendingAlert(
                            id             = existing?.id ?: 0L,
                            name           = name.trim(),
                            limitAmount    = parsed!!,
                            period         = selectedPeriod,
                            notifyAtPercent = notifyAt.toIntOrNull()?.coerceIn(1, 100) ?: 80
                        ))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = Color(0xFF0A1628))
            ) { Text("Save Alert", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}