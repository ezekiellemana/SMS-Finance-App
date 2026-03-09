package com.smsfinance.ui.transactions
import com.smsfinance.R

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType
import com.smsfinance.ui.components.LocalProfileColor
import com.smsfinance.ui.components.*
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── Transaction detail — compact card dialog ──────────────────────────────────
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    viewModel: TransactionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val transaction   = uiState.transactions.find { it.id == transactionId }
    val profileColor: Color  = LocalProfileColor.current

    if (transaction == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = AccentTeal)
        }
        return
    }

    val isDeposit   = transaction.type == TransactionType.DEPOSIT
    val accentColor = if (isDeposit) AccentTeal else ErrorRed

    Dialog(
        onDismissRequest = onNavigateBack,
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
            // Coloured drag-handle pill
            Box(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.width(44.dp).height(4.dp)
                        .clip(CircleShape).background(accentColor.copy(.45f))
                )
            }

            TransactionDetailContent(
                transaction  = transaction,
                isDeposit    = isDeposit,
                accentColor  = accentColor,
                profileColor = profileColor,
                onDelete     = { viewModel.deleteTransaction(transaction); onNavigateBack() },
                onClose      = onNavigateBack
            )
        }
    }
}

// ── Card content ──────────────────────────────────────────────────────────────
@Composable
private fun TransactionDetailContent(
    transaction: Transaction,
    isDeposit: Boolean,
    accentColor: Color,
    profileColor: Color,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { triggered = true }
    val slideY by animateFloatAsState(if (triggered) 0f else 20f,
        tween(380, easing = EaseOutCubic), label = "sy")
    val alpha  by animateFloatAsState(if (triggered) 1f else 0f,
        tween(300), label = "a")

    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = slideY; this.alpha = alpha }
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, null,
                    tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }

        // ── Hero amount block ─────────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        if (isDeposit)
                            listOf(Color(0xFF163035), Color(0xFF1A3A30), Color(0xFF182B38))
                        else
                            listOf(Color(0xFF351620), Color(0xFF2E1A2A), Color(0xFF1E1B32))
                    )
                )
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(accentColor.copy(.18f), Color.Transparent),
                            Offset(size.width * .5f, size.height * .15f),
                            size.width * .7f
                        )
                    )
                }
                .padding(vertical = 22.dp, horizontal = 20.dp)
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val pulse = rememberInfiniteTransition(label = "p")
                val glow  by pulse.animateFloat(.12f, .28f,
                    infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
                Box(
                    Modifier.size(54.dp).clip(CircleShape).background(accentColor.copy(glow)),
                    Alignment.Center
                ) {
                    Icon(
                        if (isDeposit) Icons.AutoMirrored.Filled.CallReceived
                        else           Icons.AutoMirrored.Filled.CallMade,
                        null, tint = accentColor, modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    if (isDeposit) "Money Received" else "Money Sent",
                    fontSize = 11.sp, color = accentColor.copy(.75f),
                    fontWeight = FontWeight.SemiBold, letterSpacing = .8.sp
                )
                val animAmt by animateFloatAsState(
                    transaction.amount.toFloat(), tween(900, easing = EaseOutCubic), label = "amt")
                Text(
                    "${if (isDeposit) "+" else "-"} TZS ${fmtAmt(animAmt.toDouble())}",
                    fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                    color = accentColor, textAlign = TextAlign.Center, letterSpacing = (-.5).sp
                )
                Surface(color = accentColor.copy(.12f), shape = RoundedCornerShape(50.dp)) {
                    Text(
                        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                            .format(Date(transaction.date)),
                        fontSize = 11.sp, color = accentColor.copy(.85f),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // ── Details card — profile-colour border ───────────────────────────────
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(18.dp),
            colors    = CardDefaults.cardColors(containerColor = Color(0xFF1C2740)),
            border    = BorderStroke(1.dp, profileColor.copy(.30f)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                DetailItem(Icons.Default.AccountBalance, "Source",   transaction.source, accentColor)
                DetailDivider()
                DetailItem(
                    if (isDeposit) Icons.AutoMirrored.Filled.CallReceived
                    else           Icons.AutoMirrored.Filled.CallMade,
                    "Type", transaction.type.label, accentColor
                )
                if (transaction.reference.isNotEmpty()) {
                    DetailDivider()
                    DetailItem(Icons.Default.Tag, "Reference", transaction.reference, accentColor)
                }
                if (transaction.isManual) {
                    DetailDivider()
                    DetailItem(Icons.Default.Edit, "Entry", "Manual Entry", accentColor)
                }
            }
        }

        // ── Original SMS card — profile-colour border ──────────────────────────
        if (transaction.description.isNotEmpty()) {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(18.dp),
                colors    = CardDefaults.cardColors(containerColor = Color(0xFF1C2740)),
                border    = BorderStroke(1.dp, profileColor.copy(.30f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
                                .background(accentColor.copy(.12f)),
                            Alignment.Center
                        ) {
                            Icon(Icons.Default.Sms, null,
                                tint = accentColor, modifier = Modifier.size(14.dp))
                        }
                        Text(stringResource(R.string.original_sms), fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold, color = TextWhite)
                        Spacer(Modifier.weight(1f))
                        Surface(color = accentColor.copy(.10f), shape = RoundedCornerShape(50.dp)) {
                            Text(stringResource(R.string.raw_label), fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold, color = accentColor.copy(.7f),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                letterSpacing = 1.sp)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(.5.dp).background(accentColor.copy(.10f)))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF141E2E)).padding(10.dp)
                    ) {
                        Text(
                            transaction.description, fontSize = 12.sp,
                            color = Color(0xFFCDD5E0).copy(.85f),
                            lineHeight = 18.sp, fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }

        // ── Delete ────────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ErrorRed.copy(.08f))
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = onDelete
                )
                .padding(vertical = 13.dp),
            Arrangement.Center, Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Delete, null, tint = ErrorRed, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.delete_transaction), color = ErrorRed,
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

// ── Detail row helpers ────────────────────────────────────────────────────────
@Composable
private fun DetailItem(icon: ImageVector, label: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 11.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(26.dp).clip(RoundedCornerShape(7.dp))
                .background(color.copy(.10f)), Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
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

@Suppress("unused")
@Composable
fun DetailRow(label: String, value: String, icon: ImageVector) {
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

// ── Add Transaction — compact card dialog ────────────────────────────────────
@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val profileColor: Color = LocalProfileColor.current
    var amount       by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.DEPOSIT) }
    var source       by remember { mutableStateOf("") }
    var description  by remember { mutableStateOf("") }
    var amountError  by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onNavigateBack,
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
                                .background(profileColor.copy(.14f)),
                            Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null,
                                tint = profileColor, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(stringResource(R.string.add_transaction),
                                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(stringResource(R.string.manual_entry),
                                fontSize = 11.sp, color = profileColor.copy(.75f))
                        }
                    }
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, null,
                            tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }

                Box(
                    Modifier.fillMaxWidth().height(1.dp).background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, profileColor.copy(.4f), Color.Transparent)
                        )
                    )
                )

                // ── Type selector with profile border ─────────────────────────
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color(0xFF1C2740)),
                    border    = BorderStroke(1.dp, profileColor.copy(.28f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.transaction_type),
                            fontSize = 11.sp, color = TextSecondary,
                            fontWeight = FontWeight.SemiBold, letterSpacing = .5.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TransactionType.entries.forEach { type ->
                                val isSelected = selectedType == type
                                val dep        = type == TransactionType.DEPOSIT
                                val chipColor  = if (dep) profileColor else ErrorRed
                                FilterChip(
                                    selected = isSelected,
                                    onClick  = { selectedType = type },
                                    label    = { Text(type.label, fontWeight = FontWeight.Medium) },
                                    leadingIcon = {
                                        Icon(
                                            if (dep) Icons.AutoMirrored.Filled.CallReceived
                                            else     Icons.AutoMirrored.Filled.CallMade,
                                            null, Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor   = chipColor.copy(.18f),
                                        selectedLabelColor       = chipColor,
                                        selectedLeadingIconColor = chipColor
                                    )
                                )
                            }
                        }
                    }
                }

                // ── Input fields ──────────────────────────────────────────────
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; amountError = false },
                    label = { Text(stringResource(R.string.amount_tzs_label)) },
                    isError = amountError,
                    supportingText = if (amountError) {{ Text(stringResource(R.string.invalid_amount)) }} else null,
                    modifier = Modifier.fillMaxWidth(),
                    prefix  = { Text("TZS ") },
                    shape   = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = profileColor,
                        focusedLabelColor  = profileColor
                    )
                )
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label    = { Text(stringResource(R.string.source_hint_full)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = profileColor,
                        focusedLabelColor  = profileColor
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label    = { Text(stringResource(R.string.description_opt)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape    = RoundedCornerShape(14.dp),
                    colors   = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = profileColor,
                        focusedLabelColor  = profileColor
                    )
                )

                // ── Save ──────────────────────────────────────────────────────
                Button(
                    onClick = {
                        val parsed = amount.replace(",", "").toDoubleOrNull()
                        if (parsed == null || parsed <= 0) { amountError = true; return@Button }
                        viewModel.insertManualTransaction(
                            Transaction(
                                amount      = parsed,
                                type        = selectedType,
                                source      = source.ifEmpty { "Manual" },
                                date        = System.currentTimeMillis(),
                                description = description,
                                isManual    = true
                            )
                        )
                        onNavigateBack()
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
                    Text(stringResource(R.string.save_transaction),
                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}