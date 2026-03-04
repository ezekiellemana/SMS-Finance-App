package com.smsfinance.ui.alerts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.R
import com.smsfinance.domain.model.AlertPeriod
import com.smsfinance.domain.model.AlertCheckResult
import com.smsfinance.domain.model.SpendingAlert
import com.smsfinance.ui.components.*
import com.smsfinance.ui.theme.*
import com.smsfinance.ui.theme.OrangeWarn
import com.smsfinance.viewmodel.SpendingAlertsViewModel
import java.text.NumberFormat
import java.util.Locale
import com.smsfinance.ui.components.AppScreenScaffold

@Composable
fun SpendingAlertsScreen(
    viewModel: SpendingAlertsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAlert by remember { mutableStateOf<SpendingAlert?>(null) }

    AppScreenScaffold(
        title = "Spending Alerts",
        subtitle = "Set limits to stay on budget",
        onNavigateBack = onNavigateBack,
        actions = {
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, null, tint = AccentTeal)
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = AccentTeal, strokeWidth = 3.dp)
            }
            return@AppScreenScaffold
        }
        if (uiState.alerts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                EmptyState("🔔", "No alerts yet", "Tap + to create your first spending limit")
            }
        } else {
            ScreenEnterAnimation {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.alerts, key = { it.id }) { alert ->
                        AlertCard(
                            alert = alert,
                            progress = uiState.alertProgress[alert.id],
                            onEdit = { editingAlert = alert },
                            onDelete = { viewModel.deleteAlert(alert) },
                            onToggle = { viewModel.toggleAlert(alert) }
                        )
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }

    if (showAddDialog || editingAlert != null) {
        AlertDialog_AddEdit(
            existing = editingAlert,
            onSave = { alert -> viewModel.saveAlert(alert); showAddDialog = false; editingAlert = null },
            onDismiss = { showAddDialog = false; editingAlert = null }
        )
    }
}

@Composable
fun AlertCard(
    alert: SpendingAlert,
    progress: AlertCheckResult?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    val pct = progress?.percentUsed ?: 0.0
    val barColor = when {
        pct >= 100 -> ErrorRed
        pct >= 80  -> OrangeWarn
        else       -> AccentTeal
    }
    val animProg by animateFloatAsState(
        (pct / 100.0).coerceIn(0.0, 1.0).toFloat(),
        tween(700), label = "prog"
    )

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Icons.Default.Notifications, barColor,
                        barColor.copy(0.12f), size = 42.dp, iconSize = 20.dp)
                    Column {
                        Text(alert.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(alert.period.label, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = alert.isEnabled, onCheckedChange = { onToggle() },
                        modifier = Modifier.size(width = 44.dp, height = 28.dp))
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("TZS ${fmtAmt(progress?.currentSpending ?: 0.0)} spent",
                    fontSize = 12.sp, color = barColor, fontWeight = FontWeight.SemiBold)
                Text("%.0f%%".format(pct), fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, color = barColor)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { animProg },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = barColor.copy(0.15f)
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Remaining: TZS ${fmtAmt(progress?.remaining ?: alert.limitAmount)}",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Limit: TZS ${fmtAmt(alert.limitAmount)}",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (pct >= 80) {
                Spacer(Modifier.height(10.dp))
                val (bannerBg, bannerText, msg) = when {
                    pct >= 100 -> Triple(ErrorRed.copy(0.1f), ErrorRed, "🚨  Limit exceeded!")
                    else       -> Triple(OrangeWarn.copy(0.1f), OrangeWarn, "⚠️  Approaching limit!")
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(bannerBg).padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(msg, color = bannerText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
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
    var name         by remember { mutableStateOf(existing?.name ?: "") }
    var limitAmount  by remember { mutableStateOf(existing?.limitAmount?.let { "%.0f".format(it) } ?: "") }
    var selectedPeriod by remember { mutableStateOf(existing?.period ?: AlertPeriod.MONTHLY) }
    var notifyAt     by remember { mutableStateOf(existing?.notifyAtPercent?.toString() ?: "80") }
    var nameError    by remember { mutableStateOf(false) }
    var amountError  by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Alert" else "Edit Alert",
            fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it; nameError = false },
                    label = { Text("Alert Name") }, placeholder = { Text("e.g. Monthly Food") },
                    isError = nameError, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = limitAmount,
                    onValueChange = { limitAmount = it; amountError = false },
                    label = { Text("Limit Amount") }, isError = amountError, prefix = { Text("TZS ") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp))
                Text("Period", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AlertPeriod.entries.forEach { period ->
                        FilterChip(selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.label, fontSize = 12.sp) })
                    }
                }
                OutlinedTextField(value = notifyAt, onValueChange = { notifyAt = it },
                    label = { Text("Notify at (%)") }, suffix = { Text("%") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = limitAmount.replace(",","").toDoubleOrNull()
                nameError = name.isBlank(); amountError = parsed == null || parsed <= 0
                if (!nameError && !amountError) {
                    onSave(SpendingAlert(id = existing?.id ?: 0L, name = name.trim(),
                        limitAmount = parsed!!, period = selectedPeriod,
                        notifyAtPercent = notifyAt.toIntOrNull()?.coerceIn(1,100) ?: 80))
                }
            }) { Text("Save Alert") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
