package com.smsfinance.ui.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.smsfinance.domain.model.BUDGET_PRESETS
import com.smsfinance.domain.model.Budget
import com.smsfinance.domain.model.BudgetProgress
import com.smsfinance.domain.model.PROFILE_COLORS
import com.smsfinance.ui.theme.*
import com.smsfinance.ui.theme.ErrorRed
import com.smsfinance.viewmodel.BudgetViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<Budget?>(null) }

    val cal = Calendar.getInstance()
    val monthName = android.text.format.DateFormat.format("MMMM yyyy", cal.time).toString()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = AccentTeal) {
                Icon(Icons.Default.Add, contentDescription = "Add budget", tint = BgPrimary)
            }
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentTeal)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                        Text("Budget Planning", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                    }
                }
                Text(monthName, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp))
            }
            }
            // ── Overview card ─────────────────────────────────────────────────
            item { BudgetOverviewCard(uiState.totalBudgeted, uiState.totalSpent) }

            // ── Budget list ───────────────────────────────────────────────────
            if (uiState.budgetProgress.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccountBalanceWallet, null,
                                Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                            Spacer(Modifier.height(12.dp))
                            Text("No budgets set", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text("Tap + to create your first category budget",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(uiState.budgetProgress, key = { it.budget.id }) { progress ->
                    BudgetProgressCard(
                        progress = progress,
                        onEdit = { editingBudget = progress.budget },
                        onDelete = { viewModel.deleteBudget(progress.budget) }
                    )
                }
            }
        }
    }

    if (showAddDialog || editingBudget != null) {
        BudgetDialog(
            existing = editingBudget,
            onSave = { budget ->
                viewModel.saveBudget(budget)
                showAddDialog = false
                editingBudget = null
            },
            onDismiss = { showAddDialog = false; editingBudget = null }
        )
    }
}

@Composable
fun BudgetOverviewCard(totalBudgeted: Double, totalSpent: Double) {
    val overallPercent = if (totalBudgeted > 0) (totalSpent / totalBudgeted).coerceIn(0.0, 1.0) else 0.0
    val isOver = totalSpent > totalBudgeted
    val barColor = if (isOver) ErrorRed else AccentTeal
    val animProg by animateFloatAsState(overallPercent.toFloat(), label = "overall")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1565C0))))
                .padding(20.dp)
        ) {
            Column {
                Text("Monthly Budget Overview", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text("TZS ${fmt(totalSpent)}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("of TZS ${fmt(totalBudgeted)} budgeted", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { animProg },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (isOver) Color(0xFFFF5252) else Color(0xFF69F0AE),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("${"%.0f".format(overallPercent * 100)}% used",
                        color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text("TZS ${fmt(maxOf(0.0, totalBudgeted - totalSpent))} remaining",
                        color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun BudgetProgressCard(
    progress: BudgetProgress,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val pct = progress.percentUsed.coerceIn(0.0, 100.0)
    val animProg by animateFloatAsState((pct / 100.0).toFloat(), label = "prog")
    val color = when {
        progress.isOverBudget -> ErrorRed
        pct >= 80 -> Color(0xFFFF9800)
        else -> AccentTeal
    }
    val bgColor = runCatching { Color(android.graphics.Color.parseColor(progress.budget.color)) }
        .getOrDefault(AccentTeal)

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                            .background(bgColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) { Text(progress.budget.icon, fontSize = 20.sp) }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(progress.budget.category, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Budget: TZS ${fmt(progress.budget.amount)}", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("TZS ${fmt(progress.spent)} spent", fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
                Text("${"%.0f".format(pct)}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { animProg },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = color, trackColor = color.copy(alpha = 0.15f)
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                if (progress.isOverBudget) {
                    Text("⚠️ Over budget by TZS ${fmt(progress.spent - progress.budget.amount)}",
                        fontSize = 11.sp, color = ErrorRed, fontWeight = FontWeight.Medium)
                } else {
                    Text("TZS ${fmt(progress.remaining)} remaining",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun BudgetDialog(existing: Budget?, onSave: (Budget) -> Unit, onDismiss: () -> Unit) {
    val cal = Calendar.getInstance()
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var icon by remember { mutableStateOf(existing?.icon ?: "💰") }
    var color by remember { mutableStateOf(existing?.color ?: "#00C853") }
    var amount by remember { mutableStateOf(existing?.amount?.let { "%.0f".format(it) } ?: "") }
    var keywords by remember { mutableStateOf(existing?.keywords ?: "") }
    var usePreset by remember { mutableStateOf(existing == null) }
    var categoryError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Budget Category" else "Edit Budget",
            fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 480.dp)) {

                // Presets
                if (usePreset) {
                    Text("Quick Presets", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(BUDGET_PRESETS) { (cat, ic, kw) ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat; icon = ic; keywords = kw; usePreset = false },
                                label = { Text("$ic $cat", fontSize = 11.sp) }
                            )
                        }
                    }
                    HorizontalDivider()
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it; categoryError = false },
                    label = { Text("Category Name") },
                    isError = categoryError,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    leadingIcon = { Text(icon, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp)) }
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; amountError = false },
                    label = { Text("Monthly Budget (TZS)") },
                    isError = amountError,
                    prefix = { Text("TZS ") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Match Keywords (comma-separated)") },
                    placeholder = { Text("e.g. hotel,restaurant,food") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 2
                )

                // Icon picker row
                Text("Icon", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(BUDGET_PRESETS.map { it.second }) { ic ->
                        Text(
                            ic, fontSize = 22.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (icon == ic) AccentTeal.copy(0.15f) else Color.Transparent)
                                .clickable { icon = ic }
                                .padding(6.dp)
                        )
                    }
                }

                // Color picker row
                Text("Color", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PROFILE_COLORS) { hex ->
                        val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(AccentTeal)
                        Box(
                            Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(c)
                                .then(if (color == hex) Modifier.background(Color.Transparent) else Modifier)
                                .clickable { color = hex }
                        ) {
                            if (color == hex) {
                                Icon(Icons.Default.Check, null, Modifier.size(18.dp).align(Alignment.Center), Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = amount.replace(",", "").toDoubleOrNull()
                categoryError = category.isBlank()
                amountError = parsed == null || parsed <= 0
                if (!categoryError && !amountError) {
                    onSave(Budget(
                        id = existing?.id ?: 0L,
                        userId = existing?.userId ?: 1L,
                        category = category.trim(),
                        icon = icon,
                        color = color,
                        amount = parsed!!,
                        keywords = keywords.trim(),
                        month = cal.get(Calendar.MONTH) + 1,
                        year = cal.get(Calendar.YEAR),
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    ))
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun fmt(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(amount)
