package com.smsfinance.ui.transactions

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    viewModel: TransactionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val transaction = uiState.transactions.find { it.id == transactionId }

    Scaffold(
        containerColor = BgPrimary,
    ) { padding ->
        if (transaction == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = AccentTeal)
            }
            return@Scaffold
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = padding.calculateTopPadding()),
            Arrangement.SpaceBetween, Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
            Text("Transaction Details", fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(48.dp))
        }

        val isDeposit = transaction.type == TransactionType.DEPOSIT
        val accentColor = if (isDeposit) AccentTeal else ErrorRed
        val gradColors = if (isDeposit)
            listOf(Color(0xFF1A3040), Color(0xFF1E3A3A))
        else listOf(Color(0xFF3A1F1F), Color(0xFF2C2030))

        ScreenEnterAnimation {
            Column(
                Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Hero amount card ──────────────────────────────────────────
                GradientCard(Modifier.fillMaxWidth(), gradColors) {
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(64.dp).clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(0.15f)),
                            Alignment.Center
                        ) {
                            Icon(
                                if (isDeposit) Icons.AutoMirrored.Filled.CallReceived
                                else Icons.AutoMirrored.Filled.CallMade,
                                null, tint = Color.White, modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            if (isDeposit) "Money Received" else "Money Sent",
                            color = Color.White.copy(0.75f), fontSize = 13.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "TZS ${fmtAmt(transaction.amount)}",
                            color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Surface(color = Color.White.copy(0.18f), shape = RoundedCornerShape(50)) {
                            Text(
                                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                    .format(Date(transaction.date)),
                                color = Color.White, fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // ── Details card ──────────────────────────────────────────────
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Details", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DetailRow("Source", transaction.source, Icons.Default.AccountBalance)
                        DetailRow("Type", transaction.type.label,
                            if (isDeposit) Icons.AutoMirrored.Filled.CallReceived
                            else Icons.AutoMirrored.Filled.CallMade)
                        if (transaction.reference.isNotEmpty()) {
                            DetailRow("Reference", transaction.reference, Icons.Default.Tag)
                        }
                        if (transaction.isManual) {
                            DetailRow("Entry", "Manual Entry", Icons.Default.Edit)
                        }
                    }
                }

                // ── Original SMS card ─────────────────────────────────────────
                if (transaction.description.isNotEmpty()) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconBadge(Icons.Default.Sms, accentColor,
                                    accentColor.copy(0.1f), size = 32.dp, iconSize = 16.dp, cornerRadius = 8.dp)
                                Text("Original SMS", style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold)
                            }
                            Text(transaction.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp)
                        }
                    }
                }

                // ── Delete button ─────────────────────────────────────────────
                OutlinedButton(
                    onClick = { viewModel.deleteTransaction(transaction); onNavigateBack() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.error.copy(0.5f))
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Transaction", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector) {
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

@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    var amount      by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.DEPOSIT) }
    var source      by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BgPrimary,
    ) { padding ->
        ScreenEnterAnimation {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Type selector
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Transaction Type", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TransactionType.values().forEach { type ->
                                val isSelected = selectedType == type
                                val isDeposit = type == TransactionType.DEPOSIT
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedType = type },
                                    label = { Text(type.label, fontWeight = FontWeight.Medium) },
                                    leadingIcon = {
                                        Icon(
                                            if (isDeposit) Icons.AutoMirrored.Filled.CallReceived
                                            else Icons.AutoMirrored.Filled.CallMade,
                                            null, Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor =
                                            if (isDeposit) AccentTeal.copy(0.15f)
                                            else ErrorRed.copy(0.15f),
                                        selectedLabelColor =
                                            if (isDeposit) AccentTeal else ErrorRed
                                    )
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amount, onValueChange = { amount = it; amountError = false },
                    label = { Text("Amount (TZS)") },
                    isError = amountError,
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
                        val parsed = amount.replace(",","").toDoubleOrNull()
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
