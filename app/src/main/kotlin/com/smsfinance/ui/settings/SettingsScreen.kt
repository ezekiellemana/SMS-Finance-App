package com.smsfinance.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.R
import com.smsfinance.ui.components.*
import com.smsfinance.ui.onboarding.ALL_SENDERS
import com.smsfinance.ui.onboarding.SenderOption
import com.smsfinance.ui.theme.*
import com.smsfinance.util.LocaleHelper
import com.smsfinance.viewmodel.MultiUserViewModel
import com.smsfinance.viewmodel.ServiceBalance
import com.smsfinance.viewmodel.SettingsViewModel

// ── Local colour palette for service sheets ───────────────────────────────────
private val SheetBg  = Color(0xFF1C2840)
private val RowBg    = Color(0xFF232F45)
private val Muted    = Color(0xFF8A96A8)
private val Soft     = Color(0xFFCDD5E0)
private val Green    = Color(0xFF43C59E)

// ── Settings screen ───────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    multiUserVm: MultiUserViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToBudget: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    onNavigateToMultiUser: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToInvestments: () -> Unit = {},
    onNavigateToWidgetTheme: () -> Unit = {},
    onNavigateToCharts: () -> Unit = {}
) {
    val context          = LocalContext.current
    val privacyMode      by viewModel.privacyMode.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val pinEnabled       by viewModel.pinEnabled.collectAsStateWithLifecycle()
    val language         by viewModel.language.collectAsStateWithLifecycle()
    val serviceBalances  by viewModel.serviceBalances.collectAsStateWithLifecycle()
    val multiState       by multiUserVm.uiState.collectAsStateWithLifecycle()

    val profileAccent: Color = remember(multiState.activeProfile?.color) {
        runCatching { Color(multiState.activeProfile?.color?.toColorInt() ?: 0xFF3DDAD7.toInt()) }
            .getOrElse { AccentTeal }
    }

    val showPinSetup   = remember { mutableStateOf(false) }
    val pinInput       = remember { mutableStateOf("") }
    val editingService = remember { mutableStateOf<ServiceBalance?>(null) }
    val showAddService = remember { mutableStateOf(false) }

    AppScreenScaffold(
        title          = stringResource(R.string.settings),
        subtitle       = stringResource(R.string.settings_subtitle),
        onNavigateBack = onNavigateBack
    ) { padding ->
        ScreenEnterAnimation {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // ── 1. MONEY SERVICES ─────────────────────────────────────────
                SectionHeader(stringResource(R.string.section_money_services))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    serviceBalances.forEach { svc ->
                        ServiceTileRow(
                            svc           = svc,
                            profileAccent = profileAccent,
                            onEdit        = { editingService.value = svc }
                        )
                    }
                    // Add service button
                    Box(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(profileAccent.copy(0.08f))
                            .clickable(remember { MutableInteractionSource() }, null) {
                                showAddService.value = true
                            }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Add, null,
                                tint = profileAccent, modifier = Modifier.size(16.dp))
                            Text(stringResource(R.string.add_money_service),
                                fontSize = 13.sp, color = profileAccent,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // ── 2. PRIVACY & SECURITY ─────────────────────────────────────
                Spacer(Modifier.height(4.dp))
                SectionHeader(stringResource(R.string.privacy_security))
                AppSwitchRow(
                    stringResource(R.string.privacy_mode),
                    stringResource(R.string.privacy_mode_desc),
                    Icons.Default.VisibilityOff,
                    TextSecondary, TextSecondary.copy(alpha = 0.12f),
                    privacyMode
                ) { viewModel.setPrivacyMode(it) }
                AppSwitchRow(
                    stringResource(R.string.biometric_auth),
                    stringResource(R.string.biometric_auth_desc),
                    Icons.Default.Fingerprint,
                    AccentTeal, AccentTeal.copy(alpha = 0.12f),
                    biometricEnabled
                ) { viewModel.setBiometricEnabled(it) }
                AppSwitchRow(
                    stringResource(R.string.pin_lock),
                    if (pinEnabled) stringResource(R.string.pin_set)
                    else stringResource(R.string.pin_lock_desc),
                    Icons.Default.Lock,
                    ErrorRed, ErrorRed.copy(alpha = 0.12f),
                    pinEnabled
                ) { if (it) showPinSetup.value = true else viewModel.disablePin() }

                // ── 3. ANALYTICS & INSIGHTS ───────────────────────────────────
                Spacer(Modifier.height(4.dp))
                SectionHeader(stringResource(R.string.section_analytics))
                AppNavRow(stringResource(R.string.advanced_charts),
                    stringResource(R.string.advanced_charts_desc),
                    Icons.Default.BarChart,
                    BlueAccent, BlueAccent.copy(alpha = 0.12f), onNavigateToCharts)
                AppNavRow(stringResource(R.string.investments),
                    stringResource(R.string.investments_desc),
                    Icons.AutoMirrored.Filled.TrendingUp,
                    AccentTeal, AccentTeal.copy(alpha = 0.12f), onNavigateToInvestments)
                AppNavRow(stringResource(R.string.budget_planning),
                    stringResource(R.string.budget_planning_desc),
                    Icons.Default.AccountBalanceWallet,
                    AccentTeal, AccentTeal.copy(alpha = 0.12f), onNavigateToBudget)
                AppNavRow(stringResource(R.string.recurring_transactions),
                    stringResource(R.string.recurring_transactions_desc),
                    Icons.Default.Autorenew,
                    ErrorRed, ErrorRed.copy(alpha = 0.12f), onNavigateToRecurring)

                // ── 4. DATA & BACKUP ──────────────────────────────────────────
                Spacer(Modifier.height(4.dp))
                SectionHeader(stringResource(R.string.section_data_backup))
                AppNavRow(stringResource(R.string.export_data),
                    stringResource(R.string.export_data_desc),
                    Icons.Default.FileDownload,
                    OrangeWarn, OrangeWarn.copy(alpha = 0.12f), onNavigateToExport)
                AppNavRow(stringResource(R.string.cloud_backup),
                    stringResource(R.string.cloud_backup_desc),
                    Icons.Default.Cloud,
                    AccentTeal, AccentTeal.copy(alpha = 0.12f), onNavigateToBackup)

                // ── 5. ACCOUNT ────────────────────────────────────────────────
                Spacer(Modifier.height(4.dp))
                SectionHeader(stringResource(R.string.section_account))
                AppNavRow(stringResource(R.string.family_accounts),
                    stringResource(R.string.family_accounts_desc),
                    Icons.Default.Group,
                    AccentLight, AccentLight.copy(alpha = 0.12f), onNavigateToMultiUser)

                // ── 6. PERSONALISATION ────────────────────────────────────────
                Spacer(Modifier.height(4.dp))
                SectionHeader(stringResource(R.string.section_personalisation))
                AppNavRow(stringResource(R.string.widget_themes),
                    stringResource(R.string.widget_themes_desc),
                    Icons.Default.Palette,
                    AccentLight, AccentLight.copy(alpha = 0.12f), onNavigateToWidgetTheme)

                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.app_language),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.choose_language),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = language == LocaleHelper.LANG_ENGLISH,
                                onClick  = { viewModel.setLanguage(LocaleHelper.LANG_ENGLISH, context) },
                                label    = { Text("🇬🇧  ${stringResource(R.string.language_english)}") }
                            )
                            FilterChip(
                                selected = language == LocaleHelper.LANG_SWAHILI,
                                onClick  = { viewModel.setLanguage(LocaleHelper.LANG_SWAHILI, context) },
                                label    = { Text("🇹🇿  ${stringResource(R.string.language_swahili)}") }
                            )
                        }
                    }
                }

                // ── 7. ABOUT ──────────────────────────────────────────────────
                Spacer(Modifier.height(4.dp))
                SectionHeader(stringResource(R.string.about))
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("💚", fontSize = 32.sp)
                            Column {
                                Text(stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.app_version),
                                    fontSize = 12.sp, color = AccentTeal,
                                    fontWeight = FontWeight.Medium)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text("🔒  ${stringResource(R.string.privacy_note)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }

    // ── PIN setup dialogue ────────────────────────────────────────────────────
    if (showPinSetup.value) {
        AlertDialog(
            onDismissRequest = { showPinSetup.value = false },
            title = { Text(stringResource(R.string.set_pin), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = pinInput.value,
                    onValueChange = { if (it.length <= 6) pinInput.value = it },
                    label = { Text(stringResource(R.string.enter_pin)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (pinInput.value.length in 4..6) {
                        viewModel.setPin(pinInput.value)
                        pinInput.value = ""
                        showPinSetup.value = false
                    }
                }) { Text(stringResource(R.string.set_pin)) }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetup.value = false; pinInput.value = "" }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ── Edit service sheet ────────────────────────────────────────────────────
    editingService.value?.let { svc ->
        EditServiceSheet(
            svc           = svc,
            profileAccent = profileAccent,
            onSave        = { balance ->
                viewModel.updateServiceBalance(svc.id, balance)
                editingService.value = null
            },
            onDismiss     = { editingService.value = null },
            onDelete      = {
                viewModel.removeService(svc.id)
                editingService.value = null
            }
        )
    }

    // ── Add service sheet ─────────────────────────────────────────────────────
    if (showAddService.value) {
        val currentIds = serviceBalances.map { it.id }.toSet()
        AddServiceSheet(
            currentIds    = currentIds,
            profileAccent = profileAccent,
            onAdd         = { id, balance ->
                viewModel.addService(id, balance)
                showAddService.value = false
            },
            onDismiss     = { showAddService.value = false }
        )
    }
}

// ── Service tile row ──────────────────────────────────────────────────────────

@Composable
private fun ServiceTileRow(
    svc: ServiceBalance,
    profileAccent: Color,
    onEdit: () -> Unit
) {
    val pressedState = remember { mutableStateOf(false) }
    val tileScale by animateFloatAsState(
        if (pressedState.value) 0.97f else 1f,
        spring(Spring.DampingRatioMediumBouncy), label = "stScale"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .scale(tileScale)
            .clip(RoundedCornerShape(16.dp))
            .background(RowBg)
            .clickable(remember { MutableInteractionSource() }, null) {
                pressedState.value = true; onEdit()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                    .background(profileAccent.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) { Text(svc.emoji, fontSize = 20.sp) }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(svc.displayName, fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp, color = Soft)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(5.dp).background(
                        if (svc.openingBalance > 0) Green else Muted, CircleShape))
                    Text(
                        if (svc.openingBalance == 0.0) "No opening balance"
                        else "TZS ${"%,.0f".format(svc.openingBalance)}",
                        fontSize = 11.sp,
                        color = if (svc.openingBalance > 0) Green else Muted
                    )
                }
            }
        }
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(9.dp))
                .background(profileAccent.copy(0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Edit, null,
                tint = profileAccent, modifier = Modifier.size(15.dp))
        }
    }
}

// ── Edit service sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServiceSheet(
    svc: ServiceBalance,
    profileAccent: Color,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var balanceText by remember {
        mutableStateOf(if (svc.openingBalance > 0) "%,.0f".format(svc.openingBalance) else "")
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val isValid = balanceText.replace(",", "").toDoubleOrNull()?.let { it >= 0 } == true

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = SheetBg,
        tonalElevation   = 0.dp,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), Alignment.Center) {
                Box(Modifier.width(32.dp).height(3.dp).clip(CircleShape).background(Muted.copy(.3f)))
            }
        }
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 24.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(profileAccent.copy(0.14f)),
                        contentAlignment = Alignment.Center) {
                        Text(svc.emoji, fontSize = 22.sp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(svc.displayName, fontWeight = FontWeight.Bold,
                            fontSize = 16.sp, color = Color.White)
                        Text(svc.category, fontSize = 11.sp, color = Muted)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, null, tint = Muted, modifier = Modifier.size(18.dp))
                }
            }

            Text("Opening Balance", fontSize = 12.sp, color = Muted,
                letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium)

            OutlinedTextField(
                value         = balanceText,
                onValueChange = { v ->
                    if (v.length <= 15 && v.all { it.isDigit() || it == ',' || it == '.' })
                        balanceText = v
                },
                placeholder   = {
                    Text("e.g. 150,000", color = Muted.copy(.45f),
                        fontSize = 15.sp, fontStyle = FontStyle.Italic)
                },
                leadingIcon   = {
                    Text("TZS", fontSize = 12.sp, color = profileAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp))
                },
                trailingIcon  = {
                    AnimatedVisibility(isValid && balanceText.isNotEmpty(),
                        enter = scaleIn(), exit = scaleOut()) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = Green, modifier = Modifier.size(20.dp))
                    }
                },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction    = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = profileAccent.copy(.6f),
                    unfocusedBorderColor    = Color.Transparent,
                    focusedTextColor        = Color.White,
                    unfocusedTextColor      = Color.White,
                    cursorColor             = profileAccent,
                    focusedContainerColor   = RowBg,
                    unfocusedContainerColor = RowBg
                ),
                shape    = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isValid)
                            Brush.horizontalGradient(listOf(profileAccent.copy(.7f), profileAccent))
                        else
                            Brush.horizontalGradient(listOf(RowBg, RowBg))
                    )
                    .clickable(
                        enabled           = isValid,
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null
                    ) {
                        val value = balanceText.replace(",", "").toDoubleOrNull() ?: 0.0
                        onSave(value)
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Save Balance", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = if (isValid) Color(0xFF05111E) else Muted)
            }

            if (!showDeleteConfirm) {
                TextButton(onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Delete, null, tint = ErrorRed.copy(.7f),
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Remove ${svc.displayName}", color = ErrorRed.copy(.7f), fontSize = 13.sp)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = false },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Muted.copy(.3f))
                    ) { Text("Cancel", color = Muted) }
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) { Text("Remove", color = Color.White) }
                }
            }
        }
    }
}

// ── Add service sheet ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceSheet(
    currentIds: Set<String>,
    profileAccent: Color,
    onAdd: (String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query       by remember { mutableStateOf("") }
    @Suppress("ASSIGNED_BUT_NEVER_READ_VARIABLE")
    var selectedId  by remember { mutableStateOf<String?>(null) }
    var balanceText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val available = remember(query, currentIds) {
        ALL_SENDERS.filter { s ->
            s.id !in currentIds &&
                    (query.isBlank() || s.displayName.contains(query, ignoreCase = true) ||
                            s.category.contains(query, ignoreCase = true))
        }
    }
    val selected      = ALL_SENDERS.find { it.id == selectedId }
    val isBalanceValid = balanceText.replace(",", "").toDoubleOrNull()?.let { it >= 0 } == true

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = SheetBg,
        tonalElevation   = 0.dp,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), Alignment.Center) {
                Box(Modifier.width(32.dp).height(3.dp).clip(CircleShape).background(Muted.copy(.3f)))
            }
        }
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 22.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text(
                        if (selected == null) "Add Money Service" else "Set Opening Balance",
                        fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                    Text(
                        selected?.displayName ?: "${available.size} services available",
                        fontSize = 12.sp,
                        color = if (selected == null) Muted else profileAccent)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, null, tint = Muted, modifier = Modifier.size(18.dp))
                }
            }

            AnimatedContent(selected != null, label = "addStep") { hasSelected ->
                if (!hasSelected) {
                    // Step 1 — pick service
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value         = query,
                            onValueChange = { query = it },
                            placeholder   = {
                                Text("Search banks or mobile money…",
                                    color = Muted, fontSize = 13.sp)
                            },
                            leadingIcon  = {
                                Icon(Icons.Default.Search, null,
                                    tint = Muted, modifier = Modifier.size(17.dp))
                            },
                            trailingIcon = {
                                AnimatedVisibility(query.isNotEmpty(),
                                    enter = fadeIn(), exit = fadeOut()) {
                                    IconButton(onClick = { query = "" },
                                        modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Close, null, tint = Muted,
                                            modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = profileAccent.copy(.5f),
                                unfocusedBorderColor    = Color.Transparent,
                                focusedTextColor        = Color.White,
                                unfocusedTextColor      = Color.White,
                                cursorColor             = profileAccent,
                                focusedContainerColor   = RowBg,
                                unfocusedContainerColor = RowBg
                            ),
                            shape    = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (available.isEmpty()) {
                            Box(Modifier.fillMaxWidth().height(80.dp), Alignment.Center) {
                                Text(
                                    if (query.isNotBlank()) "No match for \"$query\""
                                    else "All services already added",
                                    color = Muted, fontSize = 13.sp,
                                    textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val banks  = available.filter { it.category == "Bank" }
                                val mobile = available.filter { it.category == "Mobile Money" }
                                if (banks.isNotEmpty()) {
                                    item {
                                        Text("Banks", fontSize = 11.sp, color = profileAccent,
                                            fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                                    }
                                    items(banks, key = { it.id }) { sender ->
                                        AddServiceRow(sender, profileAccent) {
                                            @Suppress("UNUSED_VALUE")
                                            selectedId = sender.id
                                            query = ""
                                        }
                                    }
                                }
                                if (mobile.isNotEmpty()) {
                                    item {
                                        Spacer(Modifier.height(2.dp))
                                        Text("Mobile Money", fontSize = 11.sp,
                                            color = profileAccent, fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.5.sp)
                                    }
                                    items(mobile, key = { it.id }) { sender ->
                                        AddServiceRow(sender, profileAccent) {
                                            @Suppress("UNUSED_VALUE")
                                            selectedId = sender.id
                                            query = ""
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Step 2 — enter balance
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Green.copy(0.08f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                .background(Green.copy(.14f)),
                                contentAlignment = Alignment.Center) {
                                Text(selected?.emoji ?: "", fontSize = 18.sp)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(selected?.displayName ?: "", fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp, color = Soft)
                                Text(selected?.category ?: "", fontSize = 11.sp, color = Muted)
                            }
                            Icon(Icons.Default.CheckCircle, null,
                                tint = Green, modifier = Modifier.size(18.dp))
                            IconButton(onClick = {
                                @Suppress("UNUSED_VALUE")
                                selectedId = null
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, null,
                                    tint = Muted, modifier = Modifier.size(14.dp))
                            }
                        }

                        Text("Opening Balance (optional)", fontSize = 12.sp, color = Muted,
                            letterSpacing = 0.5.sp)

                        OutlinedTextField(
                            value         = balanceText,
                            onValueChange = { v ->
                                if (v.length <= 15 && v.all { it.isDigit() || it == ',' || it == '.' })
                                    balanceText = v
                            },
                            placeholder   = {
                                Text("e.g. 150,000", color = Muted.copy(.45f),
                                    fontSize = 15.sp, fontStyle = FontStyle.Italic)
                            },
                            leadingIcon   = {
                                Text("TZS", fontSize = 12.sp, color = profileAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 12.dp))
                            },
                            trailingIcon  = {
                                AnimatedVisibility(isBalanceValid && balanceText.isNotEmpty(),
                                    enter = scaleIn(), exit = scaleOut()) {
                                    Icon(Icons.Default.CheckCircle, null,
                                        tint = Green, modifier = Modifier.size(20.dp))
                                }
                            },
                            singleLine    = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction    = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = profileAccent.copy(.6f),
                                unfocusedBorderColor    = Color.Transparent,
                                focusedTextColor        = Color.White,
                                unfocusedTextColor      = Color.White,
                                cursorColor             = profileAccent,
                                focusedContainerColor   = RowBg,
                                unfocusedContainerColor = RowBg
                            ),
                            shape    = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Box(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(
                                    listOf(profileAccent.copy(.7f), profileAccent)))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null
                                ) {
                                    val balance = balanceText.replace(",", "")
                                        .toDoubleOrNull() ?: 0.0
                                    selected?.id?.let { id -> onAdd(id, balance) }
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Add ${selected?.displayName ?: ""}",
                                fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                color = Color(0xFF05111E))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddServiceRow(sender: SenderOption, accent: Color, onTap: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(RowBg)
            .clickable(remember { MutableInteractionSource() }, null) { onTap() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp))
            .background(accent.copy(.08f)), Alignment.Center) {
            Text(sender.emoji, fontSize = 17.sp)
        }
        Text(sender.displayName, color = Soft, fontSize = 13.sp,
            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Default.Add, null, tint = accent.copy(.7f), modifier = Modifier.size(16.dp))
    }
}