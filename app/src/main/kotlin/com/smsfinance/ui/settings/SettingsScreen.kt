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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.R
import com.smsfinance.ui.components.*
import com.smsfinance.ui.theme.*
import com.smsfinance.util.LocaleHelper
import com.smsfinance.viewmodel.SettingsViewModel
import com.smsfinance.ui.components.AppScreenScaffold

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
    val context = LocalContext.current
    val privacyMode      by viewModel.privacyMode.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val pinEnabled       by viewModel.pinEnabled.collectAsStateWithLifecycle()
    val darkMode         by viewModel.darkMode.collectAsStateWithLifecycle()
    val language         by viewModel.language.collectAsStateWithLifecycle()
    var showPinSetup     by remember { mutableStateOf(false) }
    var pinInput         by remember { mutableStateOf("") }

    AppScreenScaffold(
        title = "Settings",
        subtitle = "Customize your experience",
        onNavigateBack = onNavigateBack
    ) { padding ->
        ScreenEnterAnimation {
            Column(
                Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionHeader("Features")
                AppNavRow("Budget Planning", "Set monthly category budgets",
                    Icons.Default.AccountBalanceWallet,
                    AccentTeal, AccentTeal.copy(alpha = 0.12f), onNavigateToBudget)
                AppNavRow("Family Accounts", "Switch between family member profiles",
                    Icons.Default.Group,
                    AccentLight, AccentLight.copy(alpha = 0.12f), onNavigateToMultiUser)
                AppNavRow("Export Data", "Save as Excel or PDF",
                    Icons.Default.FileDownload,
                    OrangeWarn, OrangeWarn.copy(alpha = 0.12f), onNavigateToExport)
                AppNavRow("Cloud Backup", "Back up to Google Drive",
                    Icons.Default.Cloud,
                    AccentTeal, AccentTeal.copy(alpha = 0.12f), onNavigateToBackup)
                AppNavRow("Recurring Transactions", "Auto-detect & manage regular payments",
                    Icons.Default.Autorenew,
                    ErrorRed, ErrorRed.copy(alpha = 0.12f), onNavigateToRecurring)
                AppNavRow("Investments", "Track savings goals & portfolio",
                    Icons.AutoMirrored.Filled.TrendingUp,
                    AccentTeal, AccentTeal.copy(alpha = 0.12f), onNavigateToInvestments)
                AppNavRow("Widget Themes", "Customize home screen widget colours",
                    Icons.Default.Palette,
                    AccentLight, AccentLight.copy(alpha = 0.12f), onNavigateToWidgetTheme)
                AppNavRow("Advanced Charts", "Pie, donut, trend & category analytics",
                    Icons.Default.BarChart,
                    BlueAccent, BlueAccent.copy(alpha = 0.12f), onNavigateToCharts)

                Spacer(Modifier.height(4.dp))
                SectionHeader("Privacy & Security")
                AppSwitchRow("Privacy Mode", "Hide all amounts on screen",
                    Icons.Default.VisibilityOff,
                    TextSecondary, TextSecondary.copy(alpha = 0.12f),
                    privacyMode) { viewModel.setPrivacyMode(it) }
                AppSwitchRow("Biometric Auth", "Use fingerprint or face to unlock",
                    Icons.Default.Fingerprint,
                    AccentTeal, AccentTeal.copy(alpha = 0.12f),
                    biometricEnabled) { viewModel.setBiometricEnabled(it) }
                AppSwitchRow("PIN Lock",
                    if (pinEnabled) "PIN is set — tap to change" else "Protect app with a PIN",
                    Icons.Default.Lock,
                    ErrorRed, ErrorRed.copy(alpha = 0.12f),
                    pinEnabled) { if (it) showPinSetup = true else viewModel.disablePin() }

                Spacer(Modifier.height(4.dp))
                SectionHeader(stringResource(R.string.appearance))
                AppSwitchRow(stringResource(R.string.dark_mode), stringResource(R.string.dark_mode_desc),
                    Icons.Default.DarkMode,
                    AccentLight, AccentLight.copy(alpha = 0.12f),
                    darkMode) { viewModel.setDarkMode(it) }

                Spacer(Modifier.height(4.dp))
                SectionHeader(stringResource(R.string.language))
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.app_language), style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.choose_language),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = language == LocaleHelper.LANG_ENGLISH,
                                onClick = { viewModel.setLanguage(LocaleHelper.LANG_ENGLISH, context) },
                                label = { Text("🇬🇧  ${stringResource(R.string.language_english)}") })
                            FilterChip(selected = language == LocaleHelper.LANG_SWAHILI,
                                onClick = { viewModel.setLanguage(LocaleHelper.LANG_SWAHILI, context) },
                                label = { Text("🇹🇿  ${stringResource(R.string.language_swahili)}") })
                        }
                    }
                }

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
                                Text(stringResource(R.string.app_version), fontSize = 12.sp,
                                    color = AccentTeal, fontWeight = FontWeight.Medium)
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

    if (showPinSetup) {
        AlertDialog(
            onDismissRequest = { showPinSetup = false },
            title = { Text(stringResource(R.string.set_pin), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6) pinInput = it },
                    label = { Text(stringResource(R.string.enter_pin)) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
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
                TextButton(onClick = { showPinSetup = false; pinInput = "" }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}