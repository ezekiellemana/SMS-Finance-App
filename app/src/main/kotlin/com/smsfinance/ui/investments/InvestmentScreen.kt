package com.smsfinance.ui.investments
import com.smsfinance.R

import android.app.DatePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
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
import com.smsfinance.viewmodel.InvestmentViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.smsfinance.ui.components.BigFab

@Suppress("DEPRECATION")
@Composable
fun InvestmentScreen(
    viewModel: InvestmentViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Investment?>(null) }

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextWhite)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.investments), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextWhite)
                    Text(stringResource(R.string.investments_sub), fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(Modifier.width(48.dp))
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!uiState.isLoading) {
                    item { PortfolioSummaryCard(uiState.totalValue, uiState.totalInvested,
                        uiState.totalGain, uiState.totalGainPercent) }
                }
                if (uiState.investments.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 56.dp), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("📈", fontSize = 52.sp)
                                Text(stringResource(R.string.no_investments_yet), fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold, color = TextWhite)
                                Text(stringResource(R.string.no_investments_sub),
                                    fontSize = 13.sp, textAlign = TextAlign.Center, color = TextSecondary)
                            }
                        }
                    }
                }
                items(uiState.investments, key = { it.id }) { inv ->
                    InvestmentCard(inv, onEdit = { editingItem = inv }, onDelete = { viewModel.delete(inv) })
                }
            }

            // Big FAB
            BigFab(
                label   = "Add Investment",
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
                accentColor = AccentLight
            )
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
                Text(stringResource(R.string.portfolio_overview), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
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
                Text(stringResource(R.string.invested_label, fmt(totalInvested)), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun InvestmentCard(inv: Investment, onEdit: () -> Unit, onDelete: () -> Unit) {
    val animProg by animateFloatAsState(inv.progressToTarget.toFloat(), label = "prog")
    val color = runCatching { Color(inv.color.toColorInt()) }.getOrElse { AccentTeal }
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
                    Text(stringResource(R.string.current_value), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("TZS ${fmt(inv.currentValue)}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.gain_loss), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("${if (isProfit) "+" else ""}TZS ${fmt(inv.gain)}",
                        fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = if (isProfit) AccentTeal else ErrorRed)
                }
            }
            // Progress bar (if savings goal with target)
            inv.targetAmount?.let { target ->
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.progress_to_goal), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("${"%.0f".format(inv.progressToTarget * 100)}%", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = { animProg }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = color, trackColor = color.copy(alpha = 0.15f))
                Text(stringResource(R.string.target_tzs, fmt(target)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
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
                Text(stringResource(R.string.matures, dateFmt.format(Date(it))), fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
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
    var startDate by remember { mutableLongStateOf(existing?.startDate ?: System.currentTimeMillis()) }
    var maturityDate by remember { mutableStateOf(existing?.maturityDate) }
    var selectedColor by remember { mutableStateOf(existing?.color ?: "#00C853") }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Investment" else "Edit Investment", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.investment_name_hint)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                // Type selector
                Text(stringResource(R.string.investment_type), style = MaterialTheme.typography.labelMedium)
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
                    label = { Text(stringResource(R.string.initial_amount_tzs)) }, prefix = { Text("TZS ") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                OutlinedTextField(value = current, onValueChange = { current = it },
                    label = { Text(stringResource(R.string.current_value_tzs)) }, prefix = { Text("TZS ") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                OutlinedTextField(value = target, onValueChange = { target = it },
                    label = { Text(stringResource(R.string.target_amount_opt)) }, prefix = { Text("TZS ") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                OutlinedTextField(value = rate, onValueChange = { rate = it },
                    label = { Text(stringResource(R.string.annual_interest)) }, suffix = { Text("%") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                OutlinedTextField(value = institution, onValueChange = { institution = it },
                    label = { Text(stringResource(R.string.institution_opt)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                // Start date
                OutlinedButton(onClick = {
                    val cal = Calendar.getInstance().also { it.timeInMillis = startDate }
                    DatePickerDialog(context, { _, y, m, d ->
                        cal.set(y, m, d); startDate = cal.timeInMillis
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.start_date, dateFmt.format(Date(startDate))))
                }

                // Colour picker
                Text(stringResource(R.string.color_label), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PROFILE_COLORS.forEach { hex ->
                        val boxColor = runCatching { Color(hex.toColorInt()) }.getOrElse { AccentTeal }
                        Box(
                            Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                                .background(boxColor)
                                .clickable { selectedColor = hex }
                                .padding(if (selectedColor == hex) 3.dp else 0.dp),
                            Alignment.Center
                        ) {
                            if (selectedColor == hex)
                                Icon(Icons.Default.Check, null, Modifier.size(14.dp), Color.White)
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
            }) { Text(stringResource(R.string.save_label)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

private fun fmt(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(amount)