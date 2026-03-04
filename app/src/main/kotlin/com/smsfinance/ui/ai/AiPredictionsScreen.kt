package com.smsfinance.ui.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.R
import com.smsfinance.ui.theme.*
import com.smsfinance.util.AiPredictionEngine
import com.smsfinance.viewmodel.AiPredictionsViewModel
import java.text.NumberFormat
import java.util.Locale
import com.smsfinance.ui.components.AppScreenScaffold

/**
 * AI Predictions screen — shows spending forecast, trends, and savings tips.
 * All analysis runs on-device using statistical modelling.
 */
@Composable
fun AiPredictionsScreen(
    viewModel: AiPredictionsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppScreenScaffold(
        title = "AI Insights",
        subtitle = "On-device spending analysis",
        onNavigateBack = onNavigateBack,
        actions = {
            IconButton(onClick = { viewModel.runAnalysis() }) {
                Icon(Icons.Default.Refresh, null, tint = AccentTeal)
            }
        }
    ) { padding ->

        when {
            uiState.isLoading || uiState.isAnalyzing -> {
                AnalyzingIndicator(padding)
            }
            uiState.result?.hasEnoughData == false -> {
                NotEnoughDataScreen(padding)
            }
            uiState.result != null -> {
                PredictionContent(result = uiState.result!!, padding = padding)
            }
        }
    }
}

@Composable
private fun AnalyzingIndicator(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ), label = "alpha"
            )
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AccentTeal.copy(alpha = alpha)
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.generating_analysis), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(color = AccentTeal)
        }
    }
}

@Composable
private fun NotEnoughDataScreen(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Analytics,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.not_enough_data),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.not_enough_data_sub),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun PredictionContent(
    result: AiPredictionEngine.PredictionResult,
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Prediction hero card ──────────────────────────────────────────────
        item {
            PredictionHeroCard(result)
        }

        // ── Trend badge ───────────────────────────────────────────────────────
        item {
            TrendCard(result.trend)
        }

        // ── Insights grid ─────────────────────────────────────────────────────
        item {
            InsightsGrid(result)
        }

        // ── Monthly forecast bar chart ────────────────────────────────────────
        item {
            ForecastChart(result.monthlyForecast)
        }

        // ── Savings tip ───────────────────────────────────────────────────────
        item {
            SavingsTipCard(result.savingsTip)
        }

        item {
            Text(
                "* Predicted value based on ${result.dataMonths} months of data",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun PredictionHeroCard(result: AiPredictionEngine.PredictionResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1565C0), Color(0xFF0D47A1))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.predicted_expenses),
                        color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "TZS ${formatCurrency(result.predictedNextMonthExpense)}",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.next_month),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(stringResource(R.string.avg_monthly_income),
                            color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("TZS ${formatCurrency(result.avgMonthlyIncome)}",
                            color = Color(0xFF69F0AE), fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text(stringResource(R.string.savings_potential),
                            color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("TZS ${formatCurrency(result.savingsPotential)}",
                            color = Color(0xFFFFD54F), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun TrendCard(trend: AiPredictionEngine.SpendingTrend) {
    val (icon, color, text) = when (trend) {
        AiPredictionEngine.SpendingTrend.INCREASING ->
            Triple(Icons.AutoMirrored.Filled.TrendingUp, ErrorRed,
                stringResource(R.string.trend_increasing))
        AiPredictionEngine.SpendingTrend.DECREASING ->
            Triple(Icons.AutoMirrored.Filled.TrendingDown, AccentTeal,
                stringResource(R.string.trend_decreasing))
        AiPredictionEngine.SpendingTrend.STABLE ->
            Triple(Icons.AutoMirrored.Filled.TrendingFlat, Color(0xFF2962FF),
                stringResource(R.string.trend_stable))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, fontWeight = FontWeight.Medium, color = color)
        }
    }
}

@Composable
fun InsightsGrid(result: AiPredictionEngine.PredictionResult) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.spending_insights),
                fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InsightItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Store,
                    label = stringResource(R.string.top_expense_category),
                    value = result.topExpenseSource,
                    color = ErrorRed
                )
                InsightItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Today,
                    label = stringResource(R.string.avg_daily_spending),
                    value = "TZS ${formatCurrency(result.avgDailySpending)}",
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun InsightItem(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(2.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
        }
    }
}

@Composable
fun ForecastChart(forecast: List<AiPredictionEngine.MonthForecast>) {
    if (forecast.isEmpty()) return
    val maxValue = forecast.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.monthly_forecast),
                fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                forecast.forEach { month ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        val incomeH = ((month.income / maxValue) * 110).dp
                        val expenseH = ((month.expense / maxValue) * 110).dp

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Income bar
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(incomeH.coerceAtLeast(2.dp))
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(
                                        if (month.isPrediction) AccentTeal.copy(alpha = 0.5f)
                                        else AccentTeal
                                    )
                            )
                            // Expense bar
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(expenseH.coerceAtLeast(2.dp))
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(
                                        if (month.isPrediction) ErrorRed.copy(alpha = 0.5f)
                                        else ErrorRed
                                    )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            month.label,
                            fontSize = 9.sp,
                            color = if (month.isPrediction)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = if (month.isPrediction) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(AccentTeal, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text("Income", fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(ErrorRed, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text("Expense", fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(AccentTeal.copy(alpha = 0.4f), CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text("Predicted*", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun SavingsTipCard(tip: String) {
    if (tip.isBlank()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1B5E20).copy(alpha = 0.08f)
        )
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = AccentTeal,
                modifier = Modifier.size(24.dp).padding(top = 2.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(stringResource(R.string.prediction_tip),
                    fontWeight = FontWeight.Bold, color = AccentTeal, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(tip, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}
