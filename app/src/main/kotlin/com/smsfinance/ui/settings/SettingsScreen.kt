package com.smsfinance.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.smsfinance.R
import com.smsfinance.ui.components.*
import com.smsfinance.ui.theme.*
import com.smsfinance.util.LocaleHelper
import com.smsfinance.viewmodel.DashboardViewModel
import com.smsfinance.viewmodel.ServiceBalance
import com.smsfinance.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
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

    var showPinSetup         by remember { mutableStateOf(false) }
    var pinInput             by remember { mutableStateOf("") }
    var editingService       by remember { mutableStateOf<ServiceBalance?>(null) }
    var showAddService       by remember { mutableStateOf(false) }
    var addServiceSelectedId by remember { mutableStateOf<String?>(null) }
    var addServiceBalance    by remember { mutableStateOf("") }

    AppScreenScaffold(
        title = stringResource(R.string.settings),
        subtitle = stringResource(R.string.settings_subtitle),
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
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        serviceBalances.forEach { svc ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                Arrangement.SpaceBetween,
                                Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(svc.emoji, fontSize = 20.sp)
                                    Column {
                                        Text(
                                            svc.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextWhite
                                        )
                                        Text(
                                            if (svc.openingBalance == 0.0) stringResource(R.string.no_opening_balance)
                                            else "TZS ${"%,.0f".format(svc.openingBalance)}",
                                            fontSize = 11.sp, color = AccentTeal
                                        )
                                    }
                                }
                                IconButton(onClick = { editingService = svc }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.edit),
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(.4f))
                        }
                        TextButton(
                            onClick = { showAddService = true },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.add_money_service), fontSize = 13.sp)
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
                    if (pinEnabled) stringResource(R.string.pin_set) else stringResource(R.string.pin_lock_desc),
                    Icons.Default.Lock,
                    ErrorRed, ErrorRed.copy(alpha = 0.12f),
                    pinEnabled
                ) { if (it) showPinSetup = true else viewModel.disablePin() }

                // ── 3. ANALYTICS & INSIGHTS ───────────────────────────────────
                Spacer(Modifier.height(4.dp))
                SectionHeader(stringResource(R.string.section_analytics))
                AppNavRow(
                    stringResource(R.string.advanced_charts),
                    stringResource(R.string.advanced_charts_desc),
                    Icons.Default.BarChart,
                    BlueAccent, BlueAccent.copy(alpha = 0.12f),
                    onNavigateToCharts
                )
                AppNavRow(
                    stringResource(R.string.investments),
                    stringResource(R.string.investments_desc),
                    Icons.AutoMirrored.Filled.TrendingUp,
                    AccentTeal, AccentTeal.copy(alpha = 0.12f),
                    onNavigateToInvestments
                )
                AppNavRow(
                    stringResource(R.string.budget_planning),
                    stringResource(R.string.budget_planning_desc),
                    Icons.Default.AccountBalanceWallet,
                    AccentTeal, AccentTeal.copy(alpha = 0.12f),
                    onNavigateToBudget
                )
                AppNavRow(
                    stringResource(R.string.recurring_transactions),
                    stringResource(R.string.recurring_transactions_desc),
                    Icons.Default.Autorenew,
                    ErrorRed, ErrorRed.copy(alpha = 0.12f),
                    onNavigateToRecurring
                )

                // ── 4. DATA & BACKUP ──────────────────────────────────────────
                Spacer(Modifier.height(4.dp))
                SectionHeader(stringResource(R.string.section_data_backup))
                AppNavRow(
                    stringResource(R.string.export_data),
                    stringResource(R.string.export_data_desc),
                    Icons.Default.FileDownload,
                    OrangeWarn, OrangeWarn.copy(alpha = 0.12f),
                    onNavigateToExport
                )
                AppNavRow(
                    stringResource(R.string.cloud_backup),
                    stringResource(R.string.cloud_backup_desc),
                    Icons.Default.Cloud,
                    AccentTeal, AccentTeal.copy(alpha = 0.12f),
                    onNavigateToBackup
                )

                // ── 5. ACCOUNT ────────────────────────────────────────────────
                Spacer(Modifier.height(4.dp))
                SectionHeader(stringResource(R.string.section_account))
                AppNavRow(
                    stringResource(R.string.family_accounts),
                    stringResource(R.string.family_accounts_desc),
                    Icons.Default.Group,
                    AccentLight, AccentLight.copy(alpha = 0.12f),
                    onNavigateToMultiUser
                )

                // ── 6. PERSONALISATION ────────────────────────────────────────
                Spacer(Modifier.height(4.dp))
                SectionHeader(stringResource(R.string.section_personalisation))
                AppNavRow(
                    stringResource(R.string.widget_themes),
                    stringResource(R.string.widget_themes_desc),
                    Icons.Default.Palette,
                    AccentLight, AccentLight.copy(alpha = 0.12f),
                    onNavigateToWidgetTheme
                )
                // Language
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.app_language),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.choose_language),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("💚", fontSize = 32.sp)
                            Column {
                                Text(
                                    stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.app_version),
                                    fontSize = 12.sp,
                                    color = AccentTeal,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            "🔒  ${stringResource(R.string.privacy_note)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }

    // ── PIN setup dialogue ────────────────────────────────────────────────────
    if (showPinSetup) {
        AlertDialog(
            onDismissRequest = { showPinSetup = false },
            title = { Text(stringResource(R.string.set_pin), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6) pinInput = it },
                    label = { Text(stringResource(R.string.enter_pin)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (pinInput.length in 4..6) {
                        viewModel.setPin(pinInput); pinInput = ""; showPinSetup = false
                    }
                }) { Text(stringResource(R.string.set_pin)) }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetup = false; pinInput = "" }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ── Edit service balance dialogue ─────────────────────────────────────────
    editingService?.let { svc ->
        var balanceInput by remember(svc.id) {
            mutableStateOf(if (svc.openingBalance == 0.0) "" else "%.0f".format(svc.openingBalance))
        }
        AlertDialog(
            onDismissRequest = { editingService = null },
            title = { Text("${svc.emoji}  ${svc.displayName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.edit_opening_balance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = balanceInput,
                        onValueChange = { balanceInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.opening_balance_tzs)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateServiceBalance(svc.id, balanceInput.toDoubleOrNull() ?: 0.0)
                    editingService = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { editingService = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // ── Add new service dialogue ──────────────────────────────────────────────
    if (showAddService) {
        val allServices = DashboardViewModel.KNOWN_SERVICES
        val currentIds  = serviceBalances.map { it.id }.toSet()
        val available   = allServices.filterKeys { it !in currentIds }

        AlertDialog(
            onDismissRequest = { showAddService = false; addServiceSelectedId = null; addServiceBalance = "" },
            title = { Text(stringResource(R.string.add_money_service), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (available.isEmpty()) {
                        Text(
                            stringResource(R.string.all_services_added),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            stringResource(R.string.choose_service),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        androidx.compose.foundation.lazy.LazyColumn(
                            Modifier.fillMaxWidth().heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(available.entries.toList()) { (id, meta) ->
                                val selected = addServiceSelectedId == id
                                Surface(
                                    onClick = { addServiceSelectedId = id },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) AccentTeal.copy(.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(meta.second, fontSize = 18.sp)
                                        Column {
                                            Text(meta.first,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold)
                                            Text(meta.third, fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                        if (addServiceSelectedId != null) {
                            OutlinedTextField(
                                value = addServiceBalance,
                                onValueChange = { addServiceBalance = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text(stringResource(R.string.opening_balance_tzs)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        addServiceSelectedId?.let { id ->
                            viewModel.addService(id, addServiceBalance.toDoubleOrNull() ?: 0.0)
                        }
                        showAddService = false; addServiceSelectedId = null; addServiceBalance = ""
                    },
                    enabled = addServiceSelectedId != null
                ) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddService = false; addServiceSelectedId = null; addServiceBalance = ""
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}