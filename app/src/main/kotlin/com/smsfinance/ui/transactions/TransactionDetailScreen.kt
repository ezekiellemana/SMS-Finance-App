package com.smsfinance.ui.transactions

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType
import com.smsfinance.ui.components.*
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── Transaction detail — shown as a bottom sheet modal ───────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    viewModel: TransactionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    val transaction  = uiState.transactions.find { it.id == transactionId }
    val sheetState   = rememberModalBottomSheetState()

    if (transaction == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = AccentTeal)
        }
        return
    }

    val isDeposit   = transaction.type == TransactionType.DEPOSIT
    val accentColor = if (isDeposit) AccentTeal else ErrorRed

    ModalBottomSheet(
        onDismissRequest  = onNavigateBack,
        sheetState        = sheetState,
        containerColor    = Color(0xFF141E2E),
        dragHandle        = {
            // Custom drag handle — colored pill
            Box(
                Modifier.padding(top = 14.dp, bottom = 6.dp)
                    .width(44.dp).height(4.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(.45f))
            )
        }
    ) {
        TransactionDetailContent(
            transaction = transaction,
            isDeposit   = isDeposit,
            accentColor = accentColor,
            onDelete    = { viewModel.deleteTransaction(transaction); onNavigateBack() }
        )
    }
}

// ── Sheet content ─────────────────────────────────────────────────────────────
@Composable
private fun TransactionDetailContent(
    transaction: Transaction,
    isDeposit: Boolean,
    accentColor: Color,
    onDelete: () -> Unit
) {
    // Entrance animation
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { triggered = true }
    val slideY by animateFloatAsState(if (triggered) 0f else 30f,
        tween(420, easing = EaseOutCubic), label = "sy")
    val alpha  by animateFloatAsState(if (triggered) 1f else 0f,
        tween(380), label = "a")

    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = slideY; this.alpha = alpha }
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ── Hero amount block ─────────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        if (isDeposit)
                            listOf(Color(0xFF163035), Color(0xFF1A3A30), Color(0xFF182B38))
                        else
                            listOf(Color(0xFF351620), Color(0xFF2E1A2A), Color(0xFF1E1B32))
                    )
                )
                .drawBehind {
                    // Soft glow behind amount
                    drawCircle(
                        Brush.radialGradient(
                            listOf(accentColor.copy(.18f), Color.Transparent),
                            Offset(size.width * .5f, size.height * .15f),
                            size.width * .7f
                        )
                    )
                }
                .padding(vertical = 28.dp, horizontal = 24.dp)
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Icon circle
                val pulse = rememberInfiniteTransition(label = "p")
                val glow  by pulse.animateFloat(.12f, .30f,
                    infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "g")
                Box(
                    Modifier.size(62.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(glow)),
                    Alignment.Center
                ) {
                    Icon(
                        if (isDeposit) Icons.AutoMirrored.Filled.CallReceived
                        else Icons.AutoMirrored.Filled.CallMade,
                        null, tint = accentColor, modifier = Modifier.size(28.dp)
                    )
                }

                // Direction label
                Text(
                    if (isDeposit) "Money Received" else "Money Sent",
                    fontSize = 12.sp, color = accentColor.copy(.75f),
                    fontWeight = FontWeight.SemiBold, letterSpacing = .8.sp
                )

                // Amount — animated counter feel
                val animAmt by animateFloatAsState(
                    transaction.amount.toFloat(), tween(900, easing = EaseOutCubic), label = "amt")
                Text(
                    "${if (isDeposit) "+" else "-"} TZS ${fmtAmt(animAmt.toDouble())}",
                    fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                    color = accentColor, textAlign = TextAlign.Center, letterSpacing = (-.5).sp
                )

                // Date pill
                Surface(
                    color  = accentColor.copy(.12f),
                    shape  = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                            .format(Date(transaction.date)),
                        fontSize = 11.sp, color = accentColor.copy(.85f),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // ── Details card ──────────────────────────────────────────────────────
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C2740))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            DetailItem(
                icon   = Icons.Default.AccountBalance,
                label  = "Source",
                value  = transaction.source,
                color  = accentColor
            )
            DetailDivider()
            DetailItem(
                icon   = if (isDeposit) Icons.AutoMirrored.Filled.CallReceived
                else Icons.AutoMirrored.Filled.CallMade,
                label  = "Type",
                value  = transaction.type.label,
                color  = accentColor
            )
            if (transaction.reference.isNotEmpty()) {
                DetailDivider()
                DetailItem(
                    icon   = Icons.Default.Tag,
                    label  = "Reference",
                    value  = transaction.reference,
                    color  = accentColor
                )
            }
            if (transaction.isManual) {
                DetailDivider()
                DetailItem(
                    icon   = Icons.Default.Edit,
                    label  = "Entry",
                    value  = "Manual Entry",
                    color  = accentColor
                )
            }
        }

        // ── Original SMS card ─────────────────────────────────────────────────
        if (transaction.description.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1C2740))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.size(32.dp).clip(RoundedCornerShape(9.dp))
                            .background(accentColor.copy(.12f)),
                        Alignment.Center
                    ) {
                        Icon(Icons.Default.Sms, null,
                            tint = accentColor, modifier = Modifier.size(15.dp))
                    }
                    Text("Original SMS", fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = TextWhite)
                    Spacer(Modifier.weight(1f))
                    // Small pill showing SMS label
                    Surface(color = accentColor.copy(.10f), shape = RoundedCornerShape(50.dp)) {
                        Text("RAW", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold,
                            color = accentColor.copy(.7f),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            letterSpacing = 1.sp)
                    }
                }

                // Divider
                Box(Modifier.fillMaxWidth().height(.5.dp)
                    .background(accentColor.copy(.10f)))

                // SMS body text — monospace-feel with subtle background
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141E2E))
                        .padding(12.dp)
                ) {
                    Text(
                        transaction.description,
                        fontSize   = 12.sp,
                        color      = Color(0xFFCDD5E0).copy(.85f),
                        lineHeight = 19.sp,
                        fontStyle  = FontStyle.Italic
                    )
                }
            }
        }

        // ── Delete button ─────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ErrorRed.copy(.08f))
                .clickable(
                    indication       = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick          = onDelete
                )
                .padding(vertical = 14.dp),
            Arrangement.Center, Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Delete, null,
                tint = ErrorRed, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Delete Transaction", color = ErrorRed,
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

// ── Detail row helpers ────────────────────────────────────────────────────────
@Composable
private fun DetailItem(
    icon: ImageVector, label: String, value: String,
    color: Color
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                .background(color.copy(.10f)), Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
            }
            Text(label, fontSize = 12.sp, color = TextSecondary)
        }
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
    }
}

@Composable
private fun DetailDivider() {
    Box(Modifier.fillMaxWidth().height(.5.dp).background(Color.White.copy(.05f)))
}

// ── Keep DetailRow as public API (used elsewhere) ─────────────────────────────
@Suppress("unused")
@Composable
fun DetailRow(
    label: String, value: String, icon: ImageVector
) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

// ── Add transaction screen ────────────────────────────────────────────────────
@Suppress("DEPRECATION")
@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    var amount       by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.DEPOSIT) }
    var source       by remember { mutableStateOf("") }
    var description  by remember { mutableStateOf("") }
    var amountError  by remember { mutableStateOf(false) }

    Scaffold(containerColor = BgPrimary) { padding ->
        ScreenEnterAnimation {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Transaction Type", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TransactionType.entries.forEach { type ->
                                val isSelected = selectedType == type
                                val dep = type == TransactionType.DEPOSIT
                                FilterChip(
                                    selected = isSelected, onClick = { selectedType = type },
                                    label = { Text(type.label, fontWeight = FontWeight.Medium) },
                                    leadingIcon = {
                                        Icon(if (dep) Icons.AutoMirrored.Filled.CallReceived
                                        else Icons.AutoMirrored.Filled.CallMade,
                                            null, Modifier.size(16.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (dep) AccentTeal.copy(.15f) else ErrorRed.copy(.15f),
                                        selectedLabelColor = if (dep) AccentTeal else ErrorRed
                                    )
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amount, onValueChange = { amount = it; amountError = false },
                    label = { Text("Amount (TZS)") }, isError = amountError,
                    supportingText = if (amountError) {{ Text("Enter a valid amount") }} else null,
                    modifier = Modifier.fillMaxWidth(), prefix = { Text("TZS ") },
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = source, onValueChange = { source = it },
                    label = { Text("Source (e.g. NMB Bank)") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3,
                    shape = RoundedCornerShape(14.dp)
                )

                Button(
                    onClick = {
                        val parsed = amount.replace(",", "").toDoubleOrNull()
                        if (parsed == null || parsed <= 0) { amountError = true; return@Button }
                        viewModel.insertManualTransaction(
                            Transaction(amount = parsed, type = selectedType,
                                source = source.ifEmpty { "Manual" },
                                date = System.currentTimeMillis(),
                                description = description, isManual = true))
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = BgPrimary)
                ) {
                    Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Transaction", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}