package com.smsfinance.ui.budget
import com.smsfinance.R

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.graphicsLayer
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
import com.smsfinance.ui.components.fmtAmt
import com.smsfinance.ui.components.GlassCard
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.BudgetViewModel
import java.util.Calendar

@Suppress("DEPRECATION")
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<Budget?>(null) }

    val cal       = Calendar.getInstance()
    val monthName = android.text.format.DateFormat.format("MMMM yyyy", cal.time).toString()

    // FAB pulse
    val fabPulse = rememberInfiniteTransition(label = "fab")
    val fabGlow  by fabPulse.animateFloat(.50f, .92f,
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
                    Text(stringResource(R.string.budget_planning), fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, color = TextWhite)
                    Text(monthName, fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(Modifier.width(48.dp))
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {

            if (!uiState.isLoading) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { BudgetOverviewCard(uiState.totalBudgeted, uiState.totalSpent) }

                    if (uiState.budgetProgress.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = 56.dp),
                                Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("💰", fontSize = 52.sp)
                                    Text(stringResource(R.string.no_budgets_yet),
                                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Text(stringResource(R.string.no_budgets_sub),
                                        fontSize = 13.sp, color = TextSecondary,
                                        textAlign = TextAlign.Center)
                                }
                            }
                        }
                    } else {
                        items(uiState.budgetProgress, key = { it.budget.id }) { progress ->
                            BudgetProgressCard(
                                progress = progress,
                                onEdit   = { editingBudget = progress.budget },
                                onDelete = { viewModel.deleteBudget(progress.budget) }
                            )
                        }
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
                            Brush.radialGradient(listOf(AccentTeal.copy(.55f), Color.Transparent)),
                            CircleShape
                        )
                )
                // FAB itself
                Box(
                    Modifier
                        .size(64.dp)
                        .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(AccentTeal, Color(0xFF00B8A0)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick  = { showAddDialog = true },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(Icons.Default.Add, "New Budget",
                            tint = Color(0xFF0A1628), modifier = Modifier.size(32.dp))
                    }
                }
                Text(
                    "New Budget",
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = AccentTeal.copy(.8f),
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = 54.dp)
                )
            }
        }
    }

    if (showAddDialog || editingBudget != null) {
        BudgetDialog(
            existing  = editingBudget,
            onSave    = { budget ->
                viewModel.saveBudget(budget)
                showAddDialog = false
                editingBudget = null
            },
            onDismiss = { showAddDialog = false; editingBudget = null }
        )
    }
}

// ── Budget overview card ──────────────────────────────────────────────────────
@Composable
fun BudgetOverviewCard(totalBudgeted: Double, totalSpent: Double) {
    val isOver       = totalSpent > totalBudgeted && totalBudgeted > 0
    val overallPct   = if (totalBudgeted > 0) (totalSpent / totalBudgeted).coerceIn(0.0, 1.0) else 0.0
    val barColor     = if (isOver) ErrorRed else AccentTeal
    val animProg     by animateFloatAsState(overallPct.toFloat(), tween(900), label = "ov")
    val remaining    = maxOf(0.0, totalBudgeted - totalSpent)

    GlassCard(Modifier.fillMaxWidth()) {
        Box(Modifier.padding(20.dp)) {
            // Background glow
            Box(Modifier.matchParentSize().background(
                Brush.radialGradient(
                    listOf(AccentTeal.copy(.08f), Color.Transparent),
                    Offset.Zero, 600f
                )
            ))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.monthly_overview), fontSize = 11.sp, color = TextSecondary,
                    fontWeight = FontWeight.Medium, letterSpacing = .6.sp)
                Text("TZS ${fmtAmt(totalSpent)}", fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold, color = TextWhite)
                Text("of TZS ${fmtAmt(totalBudgeted)} budgeted",
                    fontSize = 12.sp, color = TextSecondary)

                // Progress bar
                Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp))
                    .background(barColor.copy(.15f))) {
                    Box(
                        Modifier.fillMaxWidth(animProg).fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(listOf(barColor.copy(.7f), barColor)),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("${"%.0f".format(overallPct * 100)}% used",
                        fontSize = 11.sp, color = barColor, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.tzs_remaining, fmtAmt(remaining)),
                        fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}

// ── Budget progress card ──────────────────────────────────────────────────────
@Composable
fun BudgetProgressCard(
    progress: BudgetProgress, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val pct      = progress.percentUsed.coerceIn(0.0, 100.0)
    val animProg by animateFloatAsState((pct / 100.0).toFloat(), tween(700), label = "prog")
    val color    = when {
        progress.isOverBudget -> ErrorRed
        pct >= 80             -> OrangeWarn
        else                  -> AccentTeal
    }
    val bgColor = runCatching {
        Color(progress.budget.color.toColorInt())
    }.getOrDefault(AccentTeal)

    GlassCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                            .background(bgColor.copy(.15f)), Alignment.Center
                    ) { Text(progress.budget.icon, fontSize = 20.sp) }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(progress.budget.category, fontWeight = FontWeight.Bold,
                            fontSize = 15.sp, color = TextWhite)
                        Text(stringResource(R.string.budget_amount, fmtAmt(progress.budget.amount)),
                            fontSize = 11.sp, color = TextSecondary)
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

            // Spent vs remaining
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                Column {
                    Text("Spent", fontSize = 10.sp, color = TextSecondary)
                    Text("TZS ${fmtAmt(progress.spent)}", fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, color = color)
                }
                Text("${"%.0f".format(pct)}%", fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold, color = color)
            }

            // Progress bar
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                .background(color.copy(.12f))) {
                Box(
                    Modifier.fillMaxWidth(animProg).fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(color.copy(.7f), color)),
                            RoundedCornerShape(3.dp)
                        )
                )
            }

            if (progress.isOverBudget) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(ErrorRed.copy(.10f))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text("⚠️ Over by TZS ${fmtAmt(progress.spent - progress.budget.amount)}",
                        fontSize = 11.sp, color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text("TZS ${fmtAmt(progress.remaining)} remaining",
                    fontSize = 11.sp, color = TextSecondary)
            }
        } // Column
    } // GlassCard
}

// ── Budget dialog ─────────────────────────────────────────────────────────────
@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@Composable
fun BudgetDialog(existing: Budget?, onSave: (Budget) -> Unit, onDismiss: () -> Unit) {
    val cal = Calendar.getInstance()
    var category      by remember { mutableStateOf(existing?.category ?: "") }
    var icon          by remember { mutableStateOf(existing?.icon ?: "💰") }
    var color         by remember { mutableStateOf(existing?.color ?: "#00C853") }
    var amount        by remember { mutableStateOf(existing?.amount?.let { "%.0f".format(it) } ?: "") }
    var keywords      by remember { mutableStateOf(existing?.keywords ?: "") }
    var usePreset     by remember { mutableStateOf(existing == null) }
    var categoryError by remember { mutableStateOf(false) }
    var amountError   by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF1C2740),
        title = {
            Text(if (existing == null) "Add Budget Category" else "Edit Budget",
                fontWeight = FontWeight.Bold, color = TextWhite)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                if (usePreset) {
                    Text(stringResource(R.string.quick_presets), fontSize = 12.sp, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(BUDGET_PRESETS) { (cat, ic, kw) ->
                            FilterChip(
                                selected = category == cat,
                                onClick  = { category = cat; icon = ic; keywords = kw; usePreset = false },
                                label    = { Text("$ic $cat", fontSize = 11.sp) },
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
                    value = category, onValueChange = { category = it; categoryError = false },
                    label = { Text(stringResource(R.string.category_name)) }, isError = categoryError,
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text(icon, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp)) }
                )
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it; amountError = false },
                    label = { Text(stringResource(R.string.monthly_budget_tzs)) },
                    isError = amountError, prefix = { Text("TZS ") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                OutlinedTextField(
                    value = keywords, onValueChange = { keywords = it },
                    label = { Text(stringResource(R.string.match_keywords)) },
                    placeholder = { Text("e.g. food,hotel,fuel") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 2
                )
                Text(stringResource(R.string.icon_label), fontSize = 11.sp, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(BUDGET_PRESETS.map { it.second }) { ic ->
                        Text(ic, fontSize = 22.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (icon == ic) AccentTeal.copy(.15f) else Color.Transparent)
                                .clickable { icon = ic }
                                .padding(6.dp))
                    }
                }
                Text(stringResource(R.string.color_label), fontSize = 11.sp, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PROFILE_COLORS) { hex ->
                        val c = runCatching { Color(hex.toColorInt()) }.getOrDefault(AccentTeal)
                        Box(
                            Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                                .background(c).clickable { color = hex },
                            Alignment.Center
                        ) {
                            if (color == hex)
                                Icon(Icons.Default.Check, null, Modifier.size(16.dp), Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amount.replace(",", "").toDoubleOrNull()
                    categoryError = category.isBlank()
                    amountError   = parsed == null || parsed <= 0
                    if (!categoryError && !amountError) {
                        onSave(Budget(
                            id        = existing?.id ?: 0L,
                            userId    = existing?.userId ?: 1L,
                            category  = category.trim(),
                            icon      = icon,
                            color     = color,
                            amount    = parsed!!,
                            keywords  = keywords.trim(),
                            month     = cal.get(Calendar.MONTH) + 1,
                            year      = cal.get(Calendar.YEAR),
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        ))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = Color(0xFF0A1628))
            ) { Text(stringResource(R.string.save_budget), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = TextSecondary) }
        }
    )
}