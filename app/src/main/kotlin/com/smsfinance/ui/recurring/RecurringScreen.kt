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
import androidx.compose.ui.draw.clip
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
import com.smsfinance.ui.components.BigFab
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.RecurringViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
@Composable
fun RecurringScreen(
    viewModel: RecurringViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem   by remember { mutableStateOf<RecurringTransaction?>(null) }

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
                    Text("Recurring", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextWhite)
                    Text("Auto-detect & track regular payments", fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(Modifier.width(48.dp))
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Auto-detect card
                item { AutoDetectCard(uiState.isDetecting) { viewModel.detectPatterns() } }

                // Detected patterns
                if (uiState.suggestions.isNotEmpty()) {
                    item {
                        Text("💡 Detected Patterns", fontWeight = FontWeight.Bold,
                            fontSize = 15.sp, color = TextWhite, modifier = Modifier.padding(top = 4.dp))
                    }
                    items(uiState.suggestions, key = { "sug_${it.source}_${it.type}" }) { suggestion ->
                        SuggestionCard(suggestion,
                            onAccept  = { viewModel.acceptSuggestion(suggestion) },
                            onDismiss = { viewModel.dismissSuggestion(suggestion) })
                    }
                }

                // Recurring list
                if (uiState.recurring.isNotEmpty()) {
                    item {
                        Text("Your Recurring Transactions", fontWeight = FontWeight.Bold,
                            fontSize = 15.sp, color = TextWhite, modifier = Modifier.padding(top = 4.dp))
                    }
                    items(uiState.recurring, key = { it.id }) { item ->
                        RecurringCard(item,
                            onEdit   = { editingItem = item },
                            onDelete = { viewModel.delete(item) })
                    }
                }

                // Empty state
                if (uiState.recurring.isEmpty() && uiState.suggestions.isEmpty() && !uiState.isDetecting) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 56.dp), Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("🔄", fontSize = 52.sp)
                                Text("No recurring transactions", fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold, color = TextWhite)
                                Text("Tap Auto Detect to find patterns\nor use the button below to add one",
                                    fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            // ── Big FAB ───────────────────────────────────────────────────────
            BigFab(
                label   = "Add Recurring",
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp)
            )
        }
    }

    if (showAddDialog || editingItem != null) {
        RecurringDialog(
            existing  = editingItem,
            onSave    = { viewModel.save(it); showAddDialog = false; editingItem = null },
            onDismiss = { showAddDialog = false; editingItem = null }
        )
    }
}

// ── Auto-detect card ──────────────────────────────────────────────────────────
@Composable
private fun AutoDetectCard(isDetecting: Boolean, onDetect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF163040), Color(0xFF1A2E40)))
            )
            .padding(16.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Auto Detect Patterns", fontWeight = FontWeight.Bold,
                fontSize = 15.sp, color = TextWhite)
            Text("Scan transactions to find regular payments",
                fontSize = 12.sp, color = TextSecondary)
        }
        Spacer(Modifier.width(12.dp))
        if (isDetecting) {
            CircularProgressIndicator(Modifier.size(28.dp), color = AccentTeal, strokeWidth = 2.5.dp)
        } else {
            Button(
                onClick = onDetect,
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentTeal, contentColor = Color(0xFF0A1628)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Search, null, Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Scan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// Keep remaining composables unchanged from before —
// SuggestionCard, RecurringCard, RecurringDialog all stay below

@Composable
private fun SuggestionCard(
    suggestion: RecurringTransaction,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AccentTeal.copy(.07f))
            .padding(14.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(suggestion.source, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, color = TextWhite)
            Text("${suggestion.type.label} · ${suggestion.frequency.label}",
                fontSize = 12.sp, color = AccentTeal)
            Text("TZS ${fmt(suggestion.expectedAmount)}", fontSize = 11.sp, color = TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = onDismiss,
                colors  = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) { Text("Skip", fontSize = 12.sp) }
            Button(
                onClick = onAccept,
                shape  = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentTeal, contentColor = Color(0xFF0A1628)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) { Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun RecurringCard(
    item: RecurringTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDeposit = item.type == TransactionType.DEPOSIT
    val accent    = if (isDeposit) AccentTeal else ErrorRed

    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C2740))
            .padding(14.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(.12f)), Alignment.Center
            ) {
                Icon(
                    if (isDeposit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    null, tint = accent, modifier = Modifier.size(20.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.source, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextWhite)
                Text("${item.frequency.label} · TZS ${fmt(item.expectedAmount)}",
                    fontSize = 12.sp, color = accent)
                item.nextExpected?.let {
                    Text("Next: ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(it))}",
                        fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Edit, null, Modifier.size(15.dp), tint = TextSecondary)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Delete, null, Modifier.size(15.dp), tint = ErrorRed.copy(.7f))
            }
        }
    }
}

@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "UNUSED_VALUE")
@Composable
fun RecurringDialog(
    existing: RecurringTransaction?,
    onSave: (RecurringTransaction) -> Unit,
    onDismiss: () -> Unit
) {
    var source        by remember { mutableStateOf(existing?.source ?: "") }
    var amountStr     by remember { mutableStateOf(existing?.expectedAmount?.let { "%.0f".format(it) } ?: "") }
    var selectedType  by remember { mutableStateOf(existing?.type ?: TransactionType.WITHDRAWAL) }
    var selectedFreq  by remember { mutableStateOf(existing?.frequency ?: RecurringFrequency.MONTHLY) }
    var usePreset     by remember { mutableStateOf(existing == null) }
    var sourceError   by remember { mutableStateOf(false) }
    var amountError   by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF1C2740),
        title = {
            Text(if (existing == null) "Add Recurring" else "Edit Recurring",
                fontWeight = FontWeight.Bold, color = TextWhite)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (usePreset) {
                    Text("Quick Presets", fontSize = 12.sp, color = TextSecondary)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(RECURRING_PRESETS) { (presetName, presetIcon, _) ->
                            FilterChip(
                                selected = source == presetName,
                                onClick  = { source = presetName; usePreset = false },
                                label    = { Text("$presetIcon $presetName", fontSize = 11.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentTeal.copy(.15f),
                                    selectedLabelColor     = AccentTeal
                                )
                            )
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(.06f))
                }
                OutlinedTextField(
                    value = source, onValueChange = { source = it; sourceError = false },
                    label = { Text("Source / Label") }, isError = sourceError,
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = amountStr, onValueChange = { amountStr = it; amountError = false },
                    label = { Text("Amount") }, prefix = { Text("TZS ") },
                    isError = amountError, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                Text("Type", fontSize = 12.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.entries.forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick  = { selectedType = t },
                            label    = { Text(t.label, fontSize = 12.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentTeal.copy(.15f),
                                selectedLabelColor     = AccentTeal
                            )
                        )
                    }
                }
                Text("Frequency", fontSize = 12.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecurringFrequency.entries.forEach { f ->
                        FilterChip(
                            selected = selectedFreq == f,
                            onClick  = { selectedFreq = f },
                            label    = { Text(f.label, fontSize = 11.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentTeal.copy(.15f),
                                selectedLabelColor     = AccentTeal
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amountStr.replace(",", "").toDoubleOrNull()
                    sourceError = source.isBlank()
                    amountError = parsed == null || parsed <= 0
                    if (!sourceError && !amountError) {
                        onSave(RecurringTransaction(
                            id             = existing?.id ?: 0L,
                            name           = source.trim(),
                            source         = source.trim(),
                            expectedAmount = parsed!!,
                            type           = selectedType,
                            frequency      = selectedFreq,
                            isActive       = existing?.isActive ?: true,
                            createdAt      = existing?.createdAt ?: System.currentTimeMillis()
                        ))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = Color(0xFF0A1628))
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

private fun fmt(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(amount)