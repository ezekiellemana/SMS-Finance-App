package com.smsfinance.ui.investments

import android.app.DatePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.Investment
import com.smsfinance.domain.model.InvestmentType
import com.smsfinance.domain.model.PROFILE_COLORS
import com.smsfinance.ui.theme.*
import com.smsfinance.ui.theme.ErrorRed
import com.smsfinance.viewmodel.InvestmentViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InvestmentScreen(
    viewModel: InvestmentViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Investment?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = AccentTeal) {
                Icon(Icons.Default.Add, "Add", tint = BgPrimary)
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                    Column {
                        Text("Investments", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                            Text("Track savings goals & portfolio", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            }
            // Portfolio summary
            if (!uiState.isLoading) {
                item { PortfolioSummaryCard(uiState.totalValue, uiState.totalInvested,
                    uiState.totalGain, uiState.totalGainPercent) }
            }

            // Empty state
            if (uiState.investments.isEmpty() && !uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            Spacer(Modifier.height(12.dp))
                            Text("No investments yet", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text("Add savings goals, fixed deposits, shares\nor any investment to track",
                                fontSize = 13.sp, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                }
            }

            // Investment cards
            items(uiState.investments, key = { it.id }) { inv ->
                InvestmentCard(inv,
                    onEdit = { editingItem = inv },
                    onDelete = { viewModel.delete(inv) })
            }
        }
    }

    if (showAddDialog || editingItem != null) {
        InvestmentDialog(
            existing = editingItem,
            onSave = { viewModel.save(it); showAddDialog = false; editingItem = null },
            onDismiss = { showAddDialog = false; editingItem = null }
        )
    }
}

@Composable
fun PortfolioSummaryCard(totalValue: Double, totalInvested: Double, gain: Double, gainPct: Double) {
    val isProfit = gain >= 0
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(Modifier.fillMaxWidth()
            .background(Brush.linearGradient(
                if (isProfit) listOf(Color(0xFF1A3040), Color(0xFF1E3A3A))
                else listOf(Color(0xFFB71C1C), Color(0xFFC62828))
            )).padding(20.dp)) {
            Column {
                Text("Portfolio Overview", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text("TZS ${fmt(totalValue)}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        null, tint = Color.White, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${"%.1f".format(gainPct)}%  (${if (isProfit) "+" else ""}TZS ${fmt(gain)})",
                        color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text("Invested: TZS ${fmt(totalInvested)}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun InvestmentCard(inv: Investment, onEdit: () -> Unit, onDelete: () -> Unit) {
    val animProg by animateFloatAsState(inv.progressToTarget.toFloat(), label = "prog")
    val color = runCatching { Color(android.graphics.Color.parseColor(inv.color)) }.getOrDefault(AccentTeal)
    val isProfit = inv.isProfit
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(0.15f)),
                        Alignment.Center) { Text(inv.icon, fontSize = 20.sp) }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(inv.name, fontWeight = FontWeight.Bold)
                        Text(inv.type.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                Row {
                    IconButton(onClick = onEdit, Modifier.size(32.dp)) { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) }
                    IconButton(onClick = onDelete, Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text("Current Value", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("TZS ${fmt(inv.currentValue)}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Gain / Loss", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("${if (isProfit) "+" else ""}TZS ${fmt(inv.gain)}",
                        fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = if (isProfit) AccentTeal else ErrorRed)
                }
            }
            // Progress bar (if savings goal with target)
            inv.targetAmount?.let { target ->
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Progress to goal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("${"%.0f".format(inv.progressToTarget * 100)}%", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = { animProg }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = color, trackColor = color.copy(alpha = 0.15f))
                Text("Target: TZS ${fmt(target)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
            // Interest projection
            if (inv.interestRate > 0) {
                Spacer(Modifier.height(6.dp))
                val projected12m = inv.projectValue(12)
                Text("📈 Projected in 12 months: TZS ${fmt(projected12m)} (@${inv.interestRate}% p.a.)",
                    fontSize = 11.sp, color = AccentTeal)
            }
            // Maturity date
            inv.maturityDate?.let {
                Text("Matures: ${dateFmt.format(Date(it))}", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun InvestmentDialog(existing: Investment?, onSave: (Investment) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: InvestmentType.SAVINGS_GOAL) }
    var initial by remember { mutableStateOf(existing?.initialAmount?.let { "%.0f".format(it) } ?: "") }
    var current by remember { mutableStateOf(existing?.currentValue?.let { "%.0f".format(it) } ?: "") }
    var target by remember { mutableStateOf(existing?.targetAmount?.let { "%.0f".format(it) } ?: "") }
    var rate by remember { mutableStateOf(existing?.interestRate?.let { "%.1f".format(it) } ?: "") }
    var institution by remember { mutableStateOf(existing?.institution ?: "") }
    var startDate by remember { mutableStateOf(existing?.startDate ?: System.currentTimeMillis()) }
    var maturityDate by remember { mutableStateOf(existing?.maturityDate) }
    var selectedColor by remember { mutableStateOf(existing?.color ?: "#00C853") }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Investment" else "Edit Investment", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Name e.g. CRDB Fixed Deposit") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                // Type selector
                Text("Investment Type", style = MaterialTheme.typography.labelMedium)
                InvestmentType.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t },
                                label = { Text("${t.icon} ${t.label}", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
                OutlinedTextField(value = initial, onValueChange = { initial = it },
                    label = { Text("Initial Amount (TZS)") }, prefix = { Text("TZS ") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                OutlinedTextField(value = current, onValueChange = { current = it },
                    label = { Text("Current Value (TZS)") }, prefix = { Text("TZS ") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                OutlinedTextField(value = target, onValueChange = { target = it },
                    label = { Text("Target Amount (optional)") }, prefix = { Text("TZS ") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                OutlinedTextField(value = rate, onValueChange = { rate = it },
                    label = { Text("Annual Interest Rate (%)") }, suffix = { Text("%") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                OutlinedTextField(value = institution, onValueChange = { institution = it },
                    label = { Text("Institution (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                // Start date
                OutlinedButton(onClick = {
                    val cal = Calendar.getInstance().also { it.timeInMillis = startDate }
                    DatePickerDialog(context, { _, y, m, d ->
                        cal.set(y, m, d); startDate = cal.timeInMillis
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Start: ${dateFmt.format(Date(startDate))}")
                }

                // Color picker
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PROFILE_COLORS.forEach { hex ->
                        val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(AccentTeal)
                        Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(c)
                            .then(if (selectedColor == hex) Modifier else Modifier)
                            .padding(if (selectedColor == hex) 2.dp else 0.dp)) {
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)).background(c)
                                .let { m -> if (selectedColor == hex) m.background(Color.White.copy(0.3f)) else m }) {}
    
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val ini = initial.replace(",", "").toDoubleOrNull() ?: return@Button
                val cur = current.replace(",", "").toDoubleOrNull() ?: ini
                if (name.isBlank()) return@Button
                onSave(Investment(
                    id = existing?.id ?: 0L,
                    userId = existing?.userId ?: 1L,
                    name = name.trim(), icon = type.icon, color = selectedColor,
                    type = type, initialAmount = ini, currentValue = cur,
                    targetAmount = target.replace(",", "").toDoubleOrNull(),
                    interestRate = rate.toDoubleOrNull() ?: 0.0,
                    startDate = startDate, maturityDate = maturityDate,
                    institution = institution.trim(),
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun fmt(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(amount)
