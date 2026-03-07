package com.smsfinance.ui.transactions
import com.smsfinance.R

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionFilter
import com.smsfinance.domain.model.TransactionType
import com.smsfinance.ui.components.*
import com.smsfinance.ui.dashboard.TransactionRow
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.MultiUserViewModel
import com.smsfinance.viewmodel.TransactionViewModel

@Suppress("DEPRECATION")
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel = hiltViewModel(),
    multiUserVm: MultiUserViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit = {}
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val multiState    by multiUserVm.uiState.collectAsStateWithLifecycle()
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<TransactionType?>(null) }
    val hasFilter = selectedType != null

    // Track list scroll — when user has scrolled past the first item the FAB
    // moves to the top-right corner so it never covers content.
    val listState = rememberLazyListState()
    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    val profileAccent: Color = remember(multiState.activeProfile?.color) {
        runCatching {
            val hex = multiState.activeProfile?.color ?: return@runCatching AccentTeal
            Color(hex.toColorInt())
        }.getOrElse { AccentTeal }
    }

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextWhite)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.all_transactions), fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, color = TextWhite)
                    if (!uiState.isLoading) {
                        Text("${uiState.transactions.size} records",
                            fontSize = 11.sp, color = TextSecondary)
                    }
                }
                IconButton(onClick = { showFilterDialog = true }) {
                    BadgedBox(badge = {
                        if (hasFilter) Badge(containerColor = AccentTeal) {}
                    }) {
                        Icon(Icons.Default.FilterList, "Filter",
                            tint = if (hasFilter) AccentTeal else TextSecondary)
                    }
                }
                // Add button moves here when list is scrolled (BigFab would cover content)
                if (isScrolled) {
                    IconButton(onClick = onNavigateToAdd) {
                        Icon(Icons.Default.Add, "Add transaction",
                            tint = AccentTeal,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AccentTeal.copy(alpha = 0.15f))
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    ) { scaffoldPadding ->
        Box(
            Modifier.fillMaxSize().padding(scaffoldPadding)
        ) {
            // ── Content ───────────────────────────────────────────────────────
            Column(Modifier.fillMaxSize()) {
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
                            "Tap + to add one, or wait for an SMS")
                    }
                    else -> Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                start  = 16.dp,
                                end    = 16.dp,
                                top    = 8.dp,
                                bottom = 120.dp   // space for BigFab
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.transactions, key = { it.id }) { tx ->
                                SwipeToDismissTransaction(tx, onDismiss = { viewModel.deleteTransaction(tx) }) {
                                    TransactionRow(tx, privacyMode = false,
                                        profileAccent = profileAccent,
                                        onClick = { onNavigateToDetail(tx.id) })
                                }
                            }
                            item { Spacer(Modifier.height(4.dp)) }
                        }

                        // "Show more" nudge — only when more pages exist
                        if (uiState.hasMore) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 130.dp)   // above BigFab
                            ) {
                                ShowMoreFab(onClick = { viewModel.loadMore() })
                            }
                        }
                    }
                }
            }

            // BigFab — only shown when at the top of the list.
            // When the user scrolls down it moves to the top-right corner of the screen.
            if (!isScrolled) {
                BigFab(
                    onClick   = onNavigateToAdd,
                    icon      = Icons.Default.Add,
                    label     = "Add Transaction",
                    accentColor = AccentTeal,
                    modifier  = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            }
        }
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text(stringResource(R.string.filter_transactions), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.transaction_type), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedType == TransactionType.DEPOSIT,
                            onClick = {
                                val current = selectedType
                                selectedType = if (current == TransactionType.DEPOSIT)
                                    null else TransactionType.DEPOSIT
                            },
                            label = { Text("💚  ${stringResource(R.string.deposits)}") }
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.WITHDRAWAL,
                            onClick = {
                                val current = selectedType
                                selectedType = if (current == TransactionType.WITHDRAWAL)
                                    null else TransactionType.WITHDRAWAL
                            },
                            label = { Text("🔴  ${stringResource(R.string.withdrawals)}") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.applyFilter(TransactionFilter(type = selectedType))
                    showFilterDialog = false
                }) { Text(stringResource(R.string.apply)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedType = null; viewModel.clearFilter(); showFilterDialog = false
                }) { Text(stringResource(R.string.clear)) }
            }
        )
    }
}

// ── Show more FAB ─────────────────────────────────────────────────────────────
@Suppress("NAME_SHADOWING")
@Composable
private fun ShowMoreFab(onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "fab")
    val glow  by pulse.animateFloat(
        .55f, .95f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fg"
    )
    val nudge by pulse.animateFloat(
        0f, -4f,
        infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
        label = "fn"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { translationY = nudge }
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(AccentTeal, AccentLight))
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 13.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                Icons.Default.ExpandMore, null,
                tint = Color(0xFF0D1B2A),
                modifier = Modifier.size(18.dp)
            )
            Text(
                "Show more",
                color      = Color(0xFF0D1B2A),
                fontWeight = FontWeight.Bold,
                fontSize   = 13.sp
            )
        }
        // Glow ring behind the button
        Box(
            Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(AccentTeal.copy(alpha = glow * 0.12f))
        )
    }
}


@Suppress("UNUSED_PARAMETER")
@Composable
fun SwipeToDismissTransaction(
    transaction: Transaction,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // Only EndToStart (swipe left) triggers dismiss — ignore StartToEnd (swipe right)
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,   // no right swipe
        enableDismissFromEndToStart = true,    // left swipe → delete
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