package com.smsfinance.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.smsfinance.ui.ai.AiPredictionsScreen
import com.smsfinance.ui.alerts.SpendingAlertsScreen
import com.smsfinance.ui.auth.PinScreen
import com.smsfinance.ui.charts.ChartsScreen
import com.smsfinance.ui.onboarding.OnboardingScreen
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
import com.smsfinance.ui.transactions.AddTransactionScreen
import com.smsfinance.ui.transactions.TransactionDetailScreen
import com.smsfinance.ui.transactions.TransactionListScreen

data class NavRailItem(val route: String, val label: String, val icon: ImageVector)

// ── Shared transition spec (mirrors MainActivity) ─────────────────────────────
private const val ANIM_DURATION = 300
internal val enterTransition     = slideInHorizontally(tween(ANIM_DURATION))  { it / 3 } + fadeIn(tween(ANIM_DURATION))
internal val exitTransition      = fadeOut(tween(ANIM_DURATION / 2))
internal val popEnterTransition  = fadeIn(tween(ANIM_DURATION))
internal val popExitTransition   = slideOutHorizontally(tween(ANIM_DURATION)) { it / 3 } + fadeOut(tween(ANIM_DURATION))

val navRailItems = listOf(
    NavRailItem(Routes.DASHBOARD,      "Dashboard",    Icons.Default.Home),
    NavRailItem(Routes.TRANSACTIONS,   "Transactions", Icons.Default.Receipt),
    NavRailItem(Routes.ALERTS,         "Alerts",       Icons.Default.Notifications),
    NavRailItem(Routes.AI_PREDICTIONS, "AI Insights",  Icons.Default.Psychology),
    NavRailItem(Routes.SETTINGS,       "Settings",     Icons.Default.Settings)
)

@Composable
fun AdaptiveAppNavigation(
    windowSizeClass: WindowSizeClass,
    requireAuth: Boolean,
    onBiometricAuth: (() -> Unit) -> Unit,
    onboardingDone: Boolean = true,
) {
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val isMedium   = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium

    if (isExpanded || isMedium) {
        TabletLayout(requireAuth, onBiometricAuth, isExpanded, onboardingDone)
    } else {
        AppNavigation(requireAuth, onBiometricAuth, onboardingDone)
    }
}

@Composable
fun TabletLayout(
    requireAuth: Boolean,
    onBiometricAuth: (() -> Unit) -> Unit,
    isTwoPane: Boolean,
    onboardingDone: Boolean = true
) {
    val primaryNav   = rememberNavController()
    val detailNav    = rememberNavController()
    val backStack    by primaryNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val startDest    = when {
        !onboardingDone -> Routes.ONBOARDING
        requireAuth     -> Routes.PIN
        else            -> Routes.DASHBOARD
    }

    Row(Modifier.fillMaxSize()) {

        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Spacer(Modifier.height(16.dp))
            navRailItems.forEach { item ->
                NavigationRailItem(
                    selected = currentRoute == item.route,
                    onClick = {
                        primaryNav.navigate(item.route) {
                            popUpTo(Routes.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                    label = { Text(text = item.label) }
                )
            }
        }

        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (isTwoPane) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(0.4f).fillMaxHeight()) {
                    NavHost(navController = primaryNav, startDestination = startDest,
                        enterTransition    = { enterTransition },
                        exitTransition     = { exitTransition },
                        popEnterTransition = { popEnterTransition },
                        popExitTransition  = { popExitTransition }
                    ) {
                        buildSharedGraph(primaryNav, detailNav, onBiometricAuth)
                    }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(Modifier.weight(0.6f).fillMaxHeight()) {
                    NavHost(navController = detailNav, startDestination = "detail_empty",
                        enterTransition    = { enterTransition },
                        exitTransition     = { exitTransition },
                        popEnterTransition = { popEnterTransition },
                        popExitTransition  = { popExitTransition }
                    ) {
                        composable("detail_empty") { DetailEmptyState() }
                        buildDetailGraph(detailNav)
                    }
                }
            }
        } else {
            NavHost(
                navController = primaryNav,
                startDestination = startDest,
                modifier = Modifier.fillMaxSize(),
                enterTransition    = { enterTransition },
                exitTransition     = { exitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition  = { popExitTransition }
            ) {
                buildSharedGraph(primaryNav, primaryNav, onBiometricAuth)
            }
        }
    }
}

@Composable
private fun DetailEmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Select an item to view details",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

private fun NavGraphBuilder.buildSharedGraph(
    nav: NavController,
    detailNav: NavController,
    onBiometricAuth: (() -> Unit) -> Unit
) {
    composable(Routes.ONBOARDING) {
        OnboardingScreen(onFinished = {
            nav.navigate(Routes.DASHBOARD) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
        })
    }
    composable(Routes.PIN) {
        PinScreen(
            onAuthenticated = { nav.navigate(Routes.DASHBOARD) { popUpTo(Routes.PIN) { inclusive = true } } },
            onBiometricRequest = {
                onBiometricAuth { nav.navigate(Routes.DASHBOARD) { popUpTo(Routes.PIN) { inclusive = true } } }
            }
        )
    }
    composable(Routes.DASHBOARD) {
        DashboardScreen(
            onNavigateToTransactions = { nav.navigate(Routes.TRANSACTIONS) },
            onNavigateToSettings = { nav.navigate(Routes.SETTINGS) },
            onNavigateToSearch = { nav.navigate(Routes.SEARCH) },
            onNavigateToCharts = { safeNavigate(detailNav, nav, Routes.CHARTS) }
        )
    }
    composable(Routes.TRANSACTIONS) {
        TransactionListScreen(
            onNavigateBack = { nav.popBackStack() },
            onNavigateToDetail = { id -> safeNavigate(detailNav, nav, "transaction_detail/$id") }
        )
    }
    composable(Routes.TRANSACTION_DETAIL) { back ->
        val id = back.arguments?.getString("id")?.toLongOrNull() ?: 0L
        TransactionDetailScreen(transactionId = id, onNavigateBack = { nav.popBackStack() })
    }
    composable(Routes.ADD_TRANSACTION)  { AddTransactionScreen(onNavigateBack = { nav.popBackStack() }) }
    composable(Routes.ALERTS)           { SpendingAlertsScreen(onNavigateBack = { nav.popBackStack() }) }
    composable(Routes.AI_PREDICTIONS)   { AiPredictionsScreen(onNavigateBack = { nav.popBackStack() }) }
    composable(Routes.SEARCH) {
        SearchScreen(
            onNavigateBack = { nav.popBackStack() },
            onNavigateToDetail = { id -> safeNavigate(detailNav, nav, "transaction_detail/$id") }
        )
    }
    composable(Routes.SETTINGS) {
        SettingsScreen(
            onNavigateBack = { nav.popBackStack() },
            onNavigateToBudget      = { safeNavigate(detailNav, nav, Routes.BUDGET) },
            onNavigateToExport      = { safeNavigate(detailNav, nav, Routes.EXPORT) },
            onNavigateToMultiUser   = { safeNavigate(detailNav, nav, Routes.MULTI_USER) },
            onNavigateToBackup      = { safeNavigate(detailNav, nav, Routes.CLOUD_BACKUP) },
            onNavigateToRecurring   = { safeNavigate(detailNav, nav, Routes.RECURRING) },
            onNavigateToInvestments = { safeNavigate(detailNav, nav, Routes.INVESTMENTS) },
            onNavigateToWidgetTheme = { safeNavigate(detailNav, nav, Routes.WIDGET_THEME) },
            onNavigateToCharts = { safeNavigate(detailNav, nav, Routes.CHARTS) }
        )
    }
    composable(Routes.BUDGET)       { BudgetScreen(onNavigateBack = { nav.popBackStack() }) }
    composable(Routes.RECURRING)    { RecurringScreen(onNavigateBack = { nav.popBackStack() }) }
    composable(Routes.INVESTMENTS)  { InvestmentScreen(onNavigateBack = { nav.popBackStack() }) }
    composable(Routes.EXPORT)       { ExportScreen(onNavigateBack = { nav.popBackStack() }) }
    composable(Routes.CLOUD_BACKUP) { CloudBackupScreen(onNavigateBack = { nav.popBackStack() }) }
    composable(Routes.MULTI_USER)   {
        MultiUserScreen(onNavigateBack = { nav.popBackStack() }, onProfileSwitched = { nav.popBackStack() })
    }
    composable(Routes.WIDGET_THEME) { WidgetThemeScreen(onNavigateBack = { nav.popBackStack() }) }
    composable(Routes.CHARTS) { ChartsScreen(onNavigateBack = { nav.popBackStack() }) }
}

private fun NavGraphBuilder.buildDetailGraph(detailNav: NavController) {
    composable(Routes.TRANSACTION_DETAIL) { back ->
        val id = back.arguments?.getString("id")?.toLongOrNull() ?: 0L
        TransactionDetailScreen(transactionId = id, onNavigateBack = { detailNav.popBackStack() })
    }
    composable(Routes.BUDGET)       { BudgetScreen(onNavigateBack = { detailNav.popBackStack() }) }
    composable(Routes.RECURRING)    { RecurringScreen(onNavigateBack = { detailNav.popBackStack() }) }
    composable(Routes.INVESTMENTS)  { InvestmentScreen(onNavigateBack = { detailNav.popBackStack() }) }
    composable(Routes.EXPORT)       { ExportScreen(onNavigateBack = { detailNav.popBackStack() }) }
    composable(Routes.CLOUD_BACKUP) { CloudBackupScreen(onNavigateBack = { detailNav.popBackStack() }) }
    composable(Routes.MULTI_USER)   {
        MultiUserScreen(onNavigateBack = { detailNav.popBackStack() }, onProfileSwitched = { detailNav.popBackStack() })
    }
    composable(Routes.WIDGET_THEME) { WidgetThemeScreen(onNavigateBack = { detailNav.popBackStack() }) }
    composable(Routes.CHARTS) { ChartsScreen(onNavigateBack = { detailNav.popBackStack() }) }
}

private fun safeNavigate(detailNav: NavController, primaryNav: NavController, route: String) {
    try { detailNav.navigate(route) } catch (_: Exception) { primaryNav.navigate(route) }
}