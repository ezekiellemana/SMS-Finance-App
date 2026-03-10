package com.smsfinance.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.border
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.smsfinance.R
import com.smsfinance.ui.ai.AiPredictionsScreen
import com.smsfinance.ui.charts.ChartsScreen
import com.smsfinance.ui.components.LocalProfileColor
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
import com.smsfinance.util.LocaleHelper
import com.smsfinance.util.SmsHistoryImporter
import com.smsfinance.viewmodel.SettingsViewModel
import com.smsfinance.viewmodel.MultiUserViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.core.content.edit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

object Routes {
    const val PIN = "pin"
    const val DASHBOARD      = "dashboard"
    const val DASHBOARD_ROUTE = "dashboard?fromOnboarding={fromOnboarding}"
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
    BottomNavItem(Routes.MULTI_USER,     R.string.nav_profile,      Icons.Default.AccountCircle),
    BottomNavItem(Routes.ALERTS,         R.string.nav_alerts,       Icons.Default.Notifications),
    BottomNavItem(Routes.AI_PREDICTIONS, R.string.nav_predictions,  Icons.Default.Psychology),
)

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var smsHistoryImporter: SmsHistoryImporter

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("app_language", MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en"
        super.attachBaseContext(LocaleHelper.wrap(newBase, lang))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // Re-apply our saved locale after any system config change so that
        // the Compose UI always renders strings in the user's chosen language.
        val prefs = getSharedPreferences("app_language", MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en"
        LocaleHelper.wrap(baseContext, lang)
        super.onConfigurationChanged(newConfig)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        // If READ_SMS was just granted, import existing inbox in background
        if (grants[Manifest.permission.READ_SMS] == true) {
            lifecycleScope.launch { smsHistoryImporter.importIfNeeded() }
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissions()
        // If READ_SMS was already granted on a previous launch, run import now
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            == PackageManager.PERMISSION_GRANTED) {
            lifecycleScope.launch { smsHistoryImporter.importIfNeeded() }
        }
        setContent {
            @Suppress("DEPRECATION")
            val vm: SettingsViewModel = hiltViewModel()
            val darkMode        by vm.darkMode.collectAsStateWithLifecycle()
            val pinEnabled      by vm.pinEnabled.collectAsStateWithLifecycle()
            val bioEnabled      by vm.biometricEnabled.collectAsStateWithLifecycle()
            val onboardingDone  by vm.onboardingDone.collectAsStateWithLifecycle()
            val prefsReady      by vm.prefsReady.collectAsStateWithLifecycle()
            val currentLang     by vm.language.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)

            val multiUserVmRoot: MultiUserViewModel = hiltViewModel()
            val multiUserStateRoot by multiUserVmRoot.uiState.collectAsStateWithLifecycle()
            val activeProfileColor = runCatching {
                Color(
                    (multiUserStateRoot.activeProfile?.color ?: "#00C853").toColorInt()
                )
            }.getOrElse { Color(0xFF3DDAD7) }

            // Apply locale immediately to resources (so strings resolve correctly
            // even before any recompose). This alone is not enough for all strings —
            // we also recreate() the Activity when the language actually changes so
            // that the entire Compose tree is rebuilt with the new Configuration.
            val locale = java.util.Locale(currentLang)
            java.util.Locale.setDefault(locale)
            val cfg = resources.configuration
            cfg.setLocale(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(cfg, resources.displayMetrics)

            // Track whether language has changed since Activity was created.
            // We only call recreate() on a CHANGE — not on initial composition —
            // so there is no blink on first launch.
            val activity = this@MainActivity
            val initialLang = remember {
                activity.getSharedPreferences("app_language", MODE_PRIVATE)
                    .getString("language", "en").orEmpty().ifEmpty { "en" }
            }
            LaunchedEffect(currentLang) {
                if (currentLang != initialLang) {
                    // Persist the new language before recreate so attachBaseContext
                    // reads the updated value on the next Activity instance.
                    activity.getSharedPreferences("app_language", MODE_PRIVATE)
                        .edit { putString("language", currentLang) }
                    activity.recreate()
                }
            }

            CompositionLocalProvider(
                LocalProfileColor provides activeProfileColor
            ) {
                SMSFinanceTheme(darkTheme = darkMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (!prefsReady) return@Surface
                        AdaptiveAppNavigation(
                            windowSizeClass = windowSizeClass,
                            requireAuth     = pinEnabled || bioEnabled,
                            onBiometricAuth = { cb -> triggerBiometricAuth(cb) },
                            onboardingDone  = onboardingDone,
                            onLangChange    = {
                                // Language changed in settings — recreate so all
                                // Views and system UI also update.
                                activity.recreate()
                            }
                        )
                    }
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
            .setTitle("Unlock Smart Money")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build())
    }
}

// ── Shared navigation transition spec ────────────────────────────────────────
// Transitions are defined in AdaptiveLayout.kt and shared across all NavHosts.

@Composable
fun AppNavigation(
    requireAuth: Boolean,
    onBiometricAuth: (() -> Unit) -> Unit,
    onboardingDone: Boolean = true,
    onLangChange: () -> Unit = {}
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

    val multiUserVm: MultiUserViewModel = hiltViewModel()
    val multiUserState by multiUserVm.uiState.collectAsStateWithLifecycle()
    val activeProfile = multiUserState.activeProfile

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                SmartMoneyBottomBar(
                    currentRoute    = currentRoute,
                    profilePhotoUri = activeProfile?.photoUri,
                    profileEmoji    = activeProfile?.avatarEmoji ?: "👤",
                    profileColor    = activeProfile?.color ?: "#00C853",
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = startRoute,
            modifier = Modifier.padding(innerPadding).padding(bottom = 0.dp),
            enterTransition    = { enterTransition },
            exitTransition     = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition  = { popExitTransition }
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Routes.DASHBOARD + "?fromOnboarding=true") {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                    // No onLangChange here — the roller calls setLanguage() which
                    // updates DataStore → currentLang StateFlow → setContent
                    // re-applies resources.updateConfiguration() → all
                    // stringResource() calls recompose instantly. No recreate needed.
                )
            }
            composable(Routes.PIN) {
                PinScreen(
                    onAuthenticated = { navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.PIN) { inclusive = true } } },
                    onBiometricRequest = { onBiometricAuth { navController.navigate(Routes.DASHBOARD) } }
                )
            }
            composable(
                route = Routes.DASHBOARD_ROUTE,
                arguments = listOf(navArgument("fromOnboarding") {
                    type = NavType.BoolType; defaultValue = false
                })
            ) { back ->
                val fromOnboarding = back.arguments?.getBoolean("fromOnboarding") ?: false
                DashboardScreen(
                    onNavigateToTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                    onNavigateToSettings     = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToSearch       = { navController.navigate(Routes.SEARCH) },
                    onNavigateToCharts       = { navController.navigate(Routes.CHARTS) },
                    fromOnboarding           = fromOnboarding
                )
            }
            composable(Routes.TRANSACTIONS) {
                TransactionListScreen(
                    onNavigateBack    = { navController.popBackStack() },
                    onNavigateToDetail = { id -> navController.navigate("transaction_detail/$id") },
                    onNavigateToAdd   = { navController.navigate(Routes.ADD_TRANSACTION) }
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
// Layout: [Home] [Transactions] [Profile] [Alerts] [AI]

private val BrandTeal    = Color(0xFF3DDAD7)
@Suppress("unused")
private val BrandDark    = Color(0xFF1F2633)
@Suppress("unused")
private val BrandSurface = Color(0xFF2C3546)

@Composable
fun SmartMoneyBottomBar(
    currentRoute: String?,
    profilePhotoUri: String?,
    profileEmoji: String,
    profileColor: String,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Bar surface with top glow line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF0F1825))
                .drawBehind {
                    // Teal top border glow
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                BrandTeal.copy(alpha = 0.6f),
                                BrandTeal.copy(alpha = 0.8f),
                                BrandTeal.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(size.width, 1.5f)
                    )
                    // Subtle inner glow below the line
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(BrandTeal.copy(alpha = 0.08f), Color.Transparent),
                            startY = 0f, endY = size.height * 0.4f
                        )
                    )
                }
        )

        // Nav items row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                BottomBarItem(
                    item            = item,
                    selected        = currentRoute == item.route,
                    onClick         = { onNavigate(item.route) },
                    profilePhotoUri = if (item.route == Routes.MULTI_USER) profilePhotoUri else null,
                    profileEmoji    = if (item.route == Routes.MULTI_USER) profileEmoji else null,
                    profileColor    = if (item.route == Routes.MULTI_USER) profileColor else null,
                    modifier        = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    profilePhotoUri: String?,
    profileEmoji: String?,
    profileColor: String?,
    modifier: Modifier = Modifier
) {
    val isProfileTab = profileEmoji != null

    // Resolve profile colour for both the profile tab ring and regular tab rings/icons
    val accentColorResolved = runCatching {
        Color(profileColor!!.toColorInt())
    }.getOrElse { LocalProfileColor.current }

    val iconColor by animateColorAsState(
        targetValue = if (selected) accentColorResolved else Color(0xFF3A4556),
        animationSpec = tween(250), label = "iconColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )

    if (isProfileTab) {
        // ── Profile tab — noticeably larger, floating above the bar ──────────
        val accentColor = accentColorResolved

        val ringColor by animateColorAsState(
            targetValue = if (selected) accentColor else Color(0xFF3A4556),
            animationSpec = tween(250), label = "ring"
        )

        Box(
            modifier = modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                // Outer glow ring when selected
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .drawBehind {
                            if (selected) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(BrandTeal.copy(alpha = 0.28f), Color.Transparent),
                                        radius = size.minDimension * 1.0f
                                    )
                                )
                            }
                        }
                        .graphicsLayer { scaleX = scale; scaleY = scale },
                    contentAlignment = Alignment.Center
                ) {
                    // Ring border
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .border(2.dp, ringColor, CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profilePhotoUri.isNullOrBlank()) {
                            AsyncImage(
                                model              = profilePhotoUri,
                                contentDescription = null,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Text(text = profileEmoji.orEmpty().ifEmpty { "👤" }, fontSize = 26.sp)
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text       = stringResource(item.labelRes),
                    color      = if (selected) BrandTeal else Color(0xFF3A4556),
                    fontSize   = 11.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines   = 1,
                    textAlign  = TextAlign.Center
                )
            }
        }

    } else {
        // ── Regular tab ───────────────────────────────────────────────────────
        Column(
            modifier = modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val ringColor by animateColorAsState(
                targetValue = if (selected) accentColorResolved else Color.Transparent,
                animationSpec = tween(250), label = "tabRing"
            )
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .drawBehind {
                        if (selected) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(accentColorResolved.copy(alpha = 0.14f), Color.Transparent),
                                    radius = size.minDimension * 0.85f
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Ring border + tinted background when selected
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (selected) 1.8.dp else 0.dp,
                            color = ringColor,
                            shape = CircleShape
                        )
                        .background(
                            if (selected) accentColorResolved.copy(alpha = 0.10f)
                            else Color.Transparent,
                            CircleShape
                        )
                )
                Icon(
                    imageVector        = item.icon,
                    contentDescription = null,
                    tint               = iconColor,
                    modifier           = Modifier
                        .size(28.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text       = stringResource(item.labelRes),
                color      = if (selected) accentColorResolved else Color(0xFF3A4556),
                fontSize   = 11.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines   = 1,
                textAlign  = TextAlign.Center
            )
        }
    }
}