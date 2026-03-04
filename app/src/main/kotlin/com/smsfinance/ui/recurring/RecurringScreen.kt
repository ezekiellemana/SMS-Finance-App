package com.smsfinance.ui.recurring

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.RecurringFrequency
import com.smsfinance.domain.model.RecurringTransaction
import com.smsfinance.domain.model.RECURRING_PRESETS
import com.smsfinance.domain.model.TransactionType
import com.smsfinance.ui.theme.*
import com.smsfinance.ui.theme.ErrorRed
import com.smsfinance.viewmodel.RecurringViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.smsfinance.ui.components.AppScreenScaffold

@Composable
fun RecurringScreen(
    viewModel: RecurringViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<RecurringTransaction?>(null) }

    AppScreenScaffold(
        title = "Recurring",
        subtitle = "Auto-detect & track regular payments",
        onNavigateBack = onNavigateBack,
        actions = {
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, null, tint = AccentTeal)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Auto-detect card ─────────────────────────────────────────────
            item { AutoDetectCard(isDetecting = uiState.isDetecting, onDetect = { viewModel.detectPatterns() }) }

            // ── AI Suggestions ───────────────────────────────────────────────
            if (uiState.suggestions.isNotEmpty()) {
                item {
                    Text("💡 Detected Patterns", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        modifier = Modifier.padding(top = 4.dp))
                }
                items(uiState.suggestions, key = { "sug_${it.source}_${it.type}" }) { suggestion ->
                    SuggestionCard(
                        suggestion = suggestion,
                        onAccept = { viewModel.acceptSuggestion(suggestion) },
                        onDismiss = { viewModel.dismissSuggestion(suggestion) }
                    )
                }
            }

            // ── Recurring list ───────────────────────────────────────────────
            if (uiState.recurring.isNotEmpty()) {
                item {
                    Text("Your Recurring Transactions", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        modifier = Modifier.padding(top = 4.dp))
                }
                items(uiState.recurring, key = { it.id }) { item ->
                    RecurringCard(
                        item = item,
                        onEdit = { editingItem = item },
                        onDelete = { viewModel.delete(item) }
                    )
                }
            }

            // ── Empty state ──────────────────────────────────────────────────
            if (uiState.recurring.isEmpty() && uiState.suggestions.isEmpty() && !uiState.isDetecting) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Autorenew, null, Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            Spacer(Modifier.height(12.dp))
                            Text("No recurring transactions", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text("Tap 'Auto Detect' to find patterns\nor use + to add manually",
                                fontSize = 13.sp, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingItem != null) {
        RecurringDialog(
            existing = editingItem,
            onSave = { viewModel.save(it); showAddDialog = false; editingItem = null },
            onDismiss = { showAddDialog = false; editingItem = null }
        )
    }
}

@Composable
fun AutoDetectCard(isDetecting: Boolean, onDetect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(Modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF7B1FA2))))
            .padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🤖 Auto Detect", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Scan transaction history to find recurring patterns",
                        color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
                Button(onClick = onDetect, enabled = !isDetecting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))) {
                    if (isDetecting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Detect", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SuggestionCard(suggestion: RecurringTransaction, onAccept: () -> Unit, onDismiss: () -> Unit) {
    val color = if (suggestion.type == TransactionType.DEPOSIT) AccentTeal else ErrorRed
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(suggestion.icon, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(suggestion.source, fontWeight = FontWeight.Bold)
                Text("~TZS ${fmt(suggestion.expectedAmount)} · ${suggestion.frequency.label}",
                    fontSize = 12.sp, color = color)
            }
            Row {
                IconButton(onClick = onAccept) {
                    Icon(Icons.Default.Check, null, tint = AccentTeal)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun RecurringCard(item: RecurringTransaction, onEdit: () -> Unit, onDelete: () -> Unit) {
    val color = if (item.type == TransactionType.DEPOSIT) AccentTeal else ErrorRed
    val dateFmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                Alignment.Center) { Text(item.icon, fontSize = 20.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold)
                Text("TZS ${fmt(item.expectedAmount)} · ${item.frequency.label}", fontSize = 12.sp, color = color)
                item.nextExpected?.let {
                    Text("Next: ${dateFmt.format(Date(it))}", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun RecurringDialog(existing: RecurringTransaction?, onSave: (RecurringTransaction) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var icon by remember { mutableStateOf(existing?.icon ?: "🔄") }
    var amount by remember { mutableStateOf(existing?.expectedAmount?.let { "%.0f".format(it) } ?: "") }
    var source by remember { mutableStateOf(existing?.source ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: TransactionType.WITHDRAWAL) }
    var frequency by remember { mutableStateOf(existing?.frequency ?: RecurringFrequency.MONTHLY) }
    var reminderEnabled by remember { mutableStateOf(existing?.reminderEnabled ?: true) }
    var usePreset by remember { mutableStateOf(existing == null) }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Recurring" else "Edit Recurring", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 500.dp)) {
                if (usePreset) {
                    Text("Quick Presets", style = MaterialTheme.typography.labelMedium)
                    RECURRING_PRESETS.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { (pName, pIcon, pType) ->
                                FilterChip(
                                    selected = false,
                                    onClick = { name = pName; icon = pIcon; type = pType; usePreset = false },
                                    label = { Text("$pIcon $pName", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = amount, onValueChange = { amount = it },
                    label = { Text("Expected Amount (TZS)") }, prefix = { Text("TZS ") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                OutlinedTextField(value = source, onValueChange = { source = it },
                    label = { Text("Source / Sender") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                // Type toggle
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.entries.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t },
                            label = { Text(t.label) })
                    }
                }
                // Frequency
                Text("Frequency", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecurringFrequency.entries.forEach { f ->
                        FilterChip(selected = frequency == f, onClick = { frequency = f },
                            label = { Text(f.label, fontSize = 11.sp) })
                    }
                }
                // Reminder toggle
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Reminder notification", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.replace(",", "").toDoubleOrNull() ?: return@Button
                if (name.isBlank()) return@Button
                onSave(RecurringTransaction(
                    id = existing?.id ?: 0L,
                    userId = existing?.userId ?: 1L,
                    name = name.trim(), icon = icon, type = type,
                    expectedAmount = amt, source = source.trim(),
                    frequency = frequency, reminderEnabled = reminderEnabled,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun fmt(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(amount)
