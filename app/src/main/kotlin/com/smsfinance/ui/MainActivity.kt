package com.smsfinance.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.smsfinance.R
import com.smsfinance.ui.ai.AiPredictionsScreen
import com.smsfinance.ui.charts.ChartsScreen
import com.smsfinance.ui.onboarding.OnboardingScreen
import com.smsfinance.ui.alerts.SpendingAlertsScreen
import com.smsfinance.ui.auth.PinScreen
import com.smsfinance.ui.backup.CloudBackupScreen
import com.smsfinance.ui.budget.BudgetScreen
import com.smsfinance.ui.dashboard.DashboardScreen
import com.smsfinance.ui.export.ExportScreen
import com.smsfinance.ui.investments.InvestmentScreen
import com.smsfinance.ui.multiuser.MultiUserScreen
import com.smsfinance.ui.recurring.RecurringScreen
import com.smsfinance.ui.search.SearchScreen
import com.smsfinance.ui.settings.SettingsScreen
import com.smsfinance.ui.settings.WidgetThemeScreen
import com.smsfinance.ui.theme.SMSFinanceTheme
import com.smsfinance.ui.transactions.AddTransactionScreen
import com.smsfinance.ui.transactions.TransactionDetailScreen
import com.smsfinance.ui.transactions.TransactionListScreen
import com.smsfinance.viewmodel.SettingsViewModel
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint

object Routes {
    const val PIN = "pin"
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val TRANSACTION_DETAIL = "transaction_detail/{id}"
    const val ADD_TRANSACTION = "add_transaction"
    const val ALERTS = "alerts"
    const val AI_PREDICTIONS = "ai_predictions"
    const val BUDGET = "budget"
    const val EXPORT = "export"
    const val MULTI_USER = "multi_user"
    const val CLOUD_BACKUP = "cloud_backup"
    const val SETTINGS = "settings"
    // New routes
    const val RECURRING = "recurring"
    const val INVESTMENTS = "investments"
    const val SEARCH = "search"
    const val WIDGET_THEME = "widget_theme"
    const val ONBOARDING = "onboarding"
    const val CHARTS = "charts"
}

data class BottomNavItem(val route: String, val labelRes: Int, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD,      R.string.nav_dashboard,    Icons.Default.Home),
    BottomNavItem(Routes.TRANSACTIONS,   R.string.nav_transactions, Icons.Default.Receipt),
    BottomNavItem(Routes.ALERTS,         R.string.nav_alerts,       Icons.Default.Notifications),
    BottomNavItem(Routes.AI_PREDICTIONS, R.string.nav_predictions,  Icons.Default.Psychology),
)

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
        setContent {
            val vm: SettingsViewModel = hiltViewModel()
            val darkMode by vm.darkMode.collectAsStateWithLifecycle()
            val pinEnabled by vm.pinEnabled.collectAsStateWithLifecycle()
            val bioEnabled by vm.biometricEnabled.collectAsStateWithLifecycle()
            val onboardingDone by vm.onboardingDone.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)
            SMSFinanceTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Don't render navigation until DataStore has emitted the real
                    // onboardingDone value. Without this guard the app briefly shows
                    // onboarding on every launch because StateFlow starts at false.
                    val prefsReady by vm.prefsReady.collectAsStateWithLifecycle()
                    if (!prefsReady) return@Surface

                    AdaptiveAppNavigation(
                        windowSizeClass = windowSizeClass,
                        requireAuth = pinEnabled || bioEnabled,
                        onBiometricAuth = { cb -> triggerBiometricAuth(cb) },
                        onboardingDone = onboardingDone
                    )
                }
            }
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }

    private fun triggerBiometricAuth(onSuccess: () -> Unit) {
        val manager = BiometricManager.from(this)
        val canAuth = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) { onSuccess(); return }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock SMS Finance")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build())
    }
}

@Composable
fun AppNavigation(
    requireAuth: Boolean,
    onBiometricAuth: (() -> Unit) -> Unit,
    onboardingDone: Boolean = true
) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val startRoute = when {
        !onboardingDone -> Routes.ONBOARDING
        requireAuth     -> Routes.PIN
        else            -> Routes.DASHBOARD
    }
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SmartMoneyBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onAddClick = { navController.navigate(Routes.ADD_TRANSACTION) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = startRoute,
            modifier = Modifier.padding(innerPadding).padding(bottom = 0.dp)) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onFinished = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                })
            }
            composable(Routes.PIN) {
                PinScreen(
                    onAuthenticated = { navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.PIN) { inclusive = true } } },
                    onBiometricRequest = { onBiometricAuth { navController.navigate(Routes.DASHBOARD) } }
                )
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onNavigateToTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToSearch = { navController.navigate(Routes.SEARCH) },
                    onNavigateToCharts = { navController.navigate(Routes.CHARTS) }
                )
            }
            composable(Routes.TRANSACTIONS) {
                TransactionListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { id -> navController.navigate("transaction_detail/$id") }
                )
            }
            composable(Routes.TRANSACTION_DETAIL) { back ->
                val id = back.arguments?.getString("id")?.toLongOrNull() ?: 0L
                TransactionDetailScreen(transactionId = id, onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.ADD_TRANSACTION) {
                AddTransactionScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.ALERTS) {
                SpendingAlertsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.AI_PREDICTIONS) {
                AiPredictionsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.BUDGET) {
                BudgetScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.EXPORT) {
                ExportScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.MULTI_USER) {
                MultiUserScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onProfileSwitched = { navController.popBackStack() }
                )
            }
            composable(Routes.CLOUD_BACKUP) {
                CloudBackupScreen(onNavigateBack = { navController.popBackStack() })
            }
            // ── New routes ────────────────────────────────────────────────────
            composable(Routes.RECURRING) {
                RecurringScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.INVESTMENTS) {
                InvestmentScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { id -> navController.navigate("transaction_detail/$id") }
                )
            }
            composable(Routes.WIDGET_THEME) {
                WidgetThemeScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.CHARTS) {
                ChartsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBudget = { navController.navigate(Routes.BUDGET) },
                    onNavigateToExport = { navController.navigate(Routes.EXPORT) },
                    onNavigateToMultiUser = { navController.navigate(Routes.MULTI_USER) },
                    onNavigateToBackup = { navController.navigate(Routes.CLOUD_BACKUP) },
                    onNavigateToRecurring = { navController.navigate(Routes.RECURRING) },
                    onNavigateToInvestments = { navController.navigate(Routes.INVESTMENTS) },
                    onNavigateToWidgetTheme = { navController.navigate(Routes.WIDGET_THEME) },
                    onNavigateToCharts = { navController.navigate(Routes.CHARTS) }
                )
            }
        }
    }
}

// ── Smart Money Bottom App Bar ────────────────────────────────────────────────
// Layout: [Home] [Transactions] [+ FAB] [Alerts] [AI]
// The FAB sits in the center cutout of the BottomAppBar.

private val BrandTeal   = Color(0xFF3DDAD7)
private val BrandDark   = Color(0xFF1F2633)
private val BrandSurface = Color(0xFF2C3546)

@Composable
fun SmartMoneyBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit
) {
    // Items split: 2 left, FAB center, 2 right
    val leftItems  = bottomNavItems.take(2)
    val rightItems = bottomNavItems.drop(2)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        // Bar background — rounded top corners
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(72.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(BrandSurface)
        )

        // Nav items row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left 2 items
            leftItems.forEach { item ->
                BottomBarItem(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Center FAB placeholder (takes space so items spread correctly)
            Spacer(modifier = Modifier.weight(1f))

            // Right 2 items
            rightItems.forEach { item ->
                BottomBarItem(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Center FAB — floats above the bar
        Box(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp)
                .clip(CircleShape)
                .background(BrandTeal)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add transaction",
                tint = BrandDark,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) BrandTeal else Color(0xFF6B7A99),
        animationSpec = tween(200),
        label = "iconColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) BrandTeal else Color(0xFF6B7A99),
        animationSpec = tween(200),
        label = "labelColor"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Active pill indicator
        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) BrandTeal else Color.Transparent)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.labelRes.let { androidx.compose.ui.res.stringResource(it) },
            color = labelColor,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}