package com.smsfinance.ui.transactions
import com.smsfinance.R

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.*
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import kotlinx.coroutines.delay
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
    @Suppress("UNUSED_PARAMETER") onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit = {},
    highlightTxId: Long = -1L   // ID of transaction to blink+shake after opening
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val multiState    by multiUserVm.uiState.collectAsStateWithLifecycle()
    @Suppress("ASSIGNED_BUT_NEVER_READ_VARIABLE")
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<TransactionType?>(null) }
    val hasFilter = selectedType != null

    // Track list scroll — when user has scrolled past the first item the FAB
    // moves to the top-right corner so it never covers content.
    val listState = rememberLazyListState()
    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    // ── Highlight: scroll to, then blink+shake the targeted transaction ───────
    var highlightedId by remember { mutableLongStateOf(highlightTxId) }
    LaunchedEffect(highlightTxId, uiState.transactions) {
        if (highlightTxId <= 0L || uiState.transactions.isEmpty()) return@LaunchedEffect
        val idx = uiState.transactions.indexOfFirst { it.id == highlightTxId }
        if (idx < 0) return@LaunchedEffect
        listState.animateScrollToItem(idx)
        delay(180)
        highlightedId = highlightTxId
        delay(1600) // blink+shake runs for ~1.6 s then resets
        highlightedId = -1L
    }

    val profileAccent: Color = remember(multiState.activeProfile?.color) {
        runCatching {
            val hex = multiState.activeProfile?.color ?: return@runCatching AccentTeal
            Color(hex.toColorInt())
        }.getOrElse { AccentTeal }
    }

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                // Title centred regardless of trailing actions
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.all_transactions), fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, color = TextWhite)
                    if (!uiState.isLoading) {
                        Text("${uiState.transactions.size} records",
                            fontSize = 11.sp, color = TextSecondary)
                    }
                }
                // Trailing actions pinned to end
                Row(Modifier.align(Alignment.CenterEnd)) {
                    IconButton(onClick = { showFilterDialog = true }) {
                        BadgedBox(badge = {
                            if (hasFilter) Badge(containerColor = AccentTeal) {}
                        }) {
                            Icon(Icons.Default.FilterList, "Filter",
                                tint = if (hasFilter) AccentTeal else TextSecondary)
                        }
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
                        // Smooth placeholder — content fades in when loaded
                        Box(Modifier.fillMaxSize())
                    }
                    uiState.transactions.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        EmptyState("🔍", stringResource(R.string.tx_list_empty_title),
                            stringResource(R.string.tx_list_empty_sub))
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
                                    HighlightableTransactionRow(
                                        tx            = tx,
                                        privacyMode   = false,
                                        profileAccent = profileAccent,
                                        highlighted   = highlightedId == tx.id,
                                        onClick       = { onNavigateToDetail(tx.id) }
                                    )
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
                    label     = stringResource(R.string.tx_list_add_btn),
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
                                selectedType = if (selectedType == TransactionType.DEPOSIT)
                                    null else TransactionType.DEPOSIT
                            },
                            label = { Text("💚  ${stringResource(R.string.deposits)}") }
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.WITHDRAWAL,
                            onClick = {
                                selectedType = if (selectedType == TransactionType.WITHDRAWAL)
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
                stringResource(R.string.tx_list_show_more),
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

// ── Wrapper that adds blink + shake animation when a tx is highlighted ────────

@Composable
private fun HighlightableTransactionRow(
    tx: Transaction,
    privacyMode: Boolean,
    profileAccent: Color,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "hlPulse")

    // Blink: background glow fades in and out
    val glowAlpha by inf.animateFloat(
        initialValue  = 0.08f, targetValue = 0.28f,
        animationSpec = infiniteRepeatable(tween(300, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    // Shake: rapid left-right translation
    val shakePhase by inf.animateFloat(
        initialValue  = 0f, targetValue  = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Restart),
        label = "shake"
    )
    val shakeX = if (highlighted) kotlin.math.sin(shakePhase) * 6f else 0f

    Box(
        Modifier.graphicsLayer { translationX = shakeX }
            .then(
                if (highlighted) Modifier.background(
                    profileAccent.copy(alpha = glowAlpha),
                    androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) else Modifier
            )
    ) {
        TransactionRow(
            transaction   = tx,
            privacyMode   = privacyMode,
            profileAccent = profileAccent,
            onClick       = onClick
        )
    }
}