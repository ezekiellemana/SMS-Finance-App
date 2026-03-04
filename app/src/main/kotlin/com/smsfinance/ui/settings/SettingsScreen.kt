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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.ui.components.*
import com.smsfinance.ui.theme.*
import com.smsfinance.util.LocaleHelper
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
    val context = LocalContext.current
    val privacyMode      by viewModel.privacyMode.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val pinEnabled       by viewModel.pinEnabled.collectAsStateWithLifecycle()
    val darkMode         by viewModel.darkMode.collectAsStateWithLifecycle()
    val language         by viewModel.language.collectAsStateWithLifecycle()
    var showPinSetup     by remember { mutableStateOf(false) }
    var pinInput         by remember { mutableStateOf("") }

    Scaffold(
        containerColor = BgPrimary,
    ) { padding ->
        ScreenEnterAnimation {
            Column(
                Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    Arrangement.SpaceBetween, Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                        Column {
                            Text("Settings", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            Text("Customize your experience", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
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
                SectionHeader("Appearance")
                AppSwitchRow("Dark Mode", "Switch to dark colour scheme",
                    Icons.Default.DarkMode,
                    AccentLight, AccentLight.copy(alpha = 0.12f),
                    darkMode) { viewModel.setDarkMode(it) }

                Spacer(Modifier.height(4.dp))
                SectionHeader("Language")
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("App Language", style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text("Choose your preferred language",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = language == LocaleHelper.LANG_ENGLISH,
                                onClick = { viewModel.setLanguage(LocaleHelper.LANG_ENGLISH, context) },
                                label = { Text("🇬🇧  English") })
                            FilterChip(selected = language == LocaleHelper.LANG_SWAHILI,
                                onClick = { viewModel.setLanguage(LocaleHelper.LANG_SWAHILI, context) },
                                label = { Text("🇹🇿  Kiswahili") })
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                SectionHeader("About")
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("💚", fontSize = 32.sp)
                            Column {
                                Text("Smart Money",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold)
                                Text("Version 4.0.0", fontSize = 12.sp,
                                    color = AccentTeal, fontWeight = FontWeight.Medium)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            "🔒  All data is processed locally on your device. Your SMS data is never transmitted externally.",
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
            title = { Text("Set PIN", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6) pinInput = it },
                    label = { Text("Enter 4–6 digit PIN") },
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
                }) { Text("Set PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetup = false; pinInput = "" }) { Text("Cancel") }
            }
        )
    }
}
