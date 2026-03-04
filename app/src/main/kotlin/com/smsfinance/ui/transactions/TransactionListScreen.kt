package com.smsfinance.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionFilter
import com.smsfinance.domain.model.TransactionType
import com.smsfinance.ui.components.*
import com.smsfinance.ui.dashboard.TransactionRow
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.TransactionViewModel

@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<TransactionType?>(null) }
    val hasFilter = selectedType != null

    Scaffold(
        containerColor = BgPrimary,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                    Column {
                        Text("All Transactions", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge)
                        if (!uiState.isLoading) {
                            Text("${uiState.transactions.size} records",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                IconButton(onClick = { showFilterDialog = true }) {
                    BadgedBox(badge = {
                        if (hasFilter) Badge(containerColor = AccentTeal) {}
                    }) {
                        Icon(Icons.Default.FilterList, "Filter",
                            tint = if (hasFilter) AccentTeal
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (hasFilter) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    FilterChip(
                        selected = true,
                        onClick = { selectedType = null; viewModel.clearFilter() },
                        label = { Text(selectedType!!.label) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) }
                    )
                }
            }
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = AccentTeal, strokeWidth = 3.dp)
                }
                uiState.transactions.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    EmptyState("🔍", "No transactions found",
                        "Try clearing filters or adding transactions")
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.transactions, key = { it.id }) { tx ->
                        SwipeToDismissTransaction(tx, onDismiss = { viewModel.deleteTransaction(tx) }) {
                            TransactionRow(tx, privacyMode = false,
                                onClick = { onNavigateToDetail(tx.id) })
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Transactions", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Transaction Type", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedType == TransactionType.DEPOSIT,
                            onClick = {
                                selectedType = if (selectedType == TransactionType.DEPOSIT)
                                    null else TransactionType.DEPOSIT
                            },
                            label = { Text("💚  Deposits") }
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.WITHDRAWAL,
                            onClick = {
                                selectedType = if (selectedType == TransactionType.WITHDRAWAL)
                                    null else TransactionType.WITHDRAWAL
                            },
                            label = { Text("🔴  Withdrawals") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.applyFilter(TransactionFilter(type = selectedType))
                    showFilterDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedType = null; viewModel.clearFilter(); showFilterDialog = false
                }) { Text("Clear") }
            }
        )
    }
}

@Composable
fun SwipeToDismissTransaction(
    transaction: Transaction,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDismiss(); true } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(Modifier.fillMaxSize().padding(end = 20.dp), Alignment.CenterEnd) {
                GlassCard {
                    Icon(Icons.Default.Delete, null, Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        content = { content() }
    )
}
