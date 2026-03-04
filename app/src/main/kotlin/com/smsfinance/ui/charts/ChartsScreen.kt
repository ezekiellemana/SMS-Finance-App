package com.smsfinance.ui.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.TransactionType
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.ChartsViewModel
import java.text.NumberFormat
import java.util.*
import kotlin.math.*

@Composable
fun ChartsScreen(
    viewModel: ChartsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedPeriod by remember { mutableStateOf(0) } // 0=Month, 1=Quarter, 2=Year

    LaunchedEffect(selectedPeriod) { viewModel.loadData(selectedPeriod) }

    Scaffold(
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                    Column {
                        Text("Analytics", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                            Text("Visual spending insights", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            }
            // Period selector
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("This Month", "3 Months", "This Year").forEachIndexed { i, label ->
                        FilterChip(
                            selected = selectedPeriod == i,
                            onClick = { selectedPeriod = i },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                        CircularProgressIndicator(color = AccentTeal)
                    }
                }
            } else {
                // ── 1. Income vs Expense Donut Chart ────────────────────────────
                item {
                    ChartCard(title = "Income vs Expenses", icon = "🍩") {
                        DonutChart(
                            income = uiState.totalIncome,
                            expenses = uiState.totalExpenses
                        )
                    }
                }

                // ── 2. Spending by Category Pie Chart ───────────────────────────
                item {
                    ChartCard(title = "Spending by Source", icon = "🥧") {
                        PieChart(
                            slices = uiState.expensesBySource.take(6),
                            colors = PIE_COLORS
                        )
                    }
                }

                // ── 3. Monthly Trend Line Chart ──────────────────────────────────
                if (uiState.monthlyTrend.size >= 2) {
                    item {
                        ChartCard(title = "Spending Trend", icon = "📈") {
                            TrendLineChart(
                                incomeData = uiState.monthlyTrend.map { it.income },
                                expenseData = uiState.monthlyTrend.map { it.expense },
                                labels = uiState.monthlyTrend.map { it.label }
                            )
                        }
                    }
                }

                // ── 4. Category Horizontal Bar Chart ────────────────────────────
                if (uiState.expensesBySource.isNotEmpty()) {
                    item {
                        ChartCard(title = "Top Expense Sources", icon = "📊") {
                            HorizontalBarChart(
                                items = uiState.expensesBySource.take(7),
                                maxValue = uiState.expensesBySource.firstOrNull()?.second ?: 1.0
                            )
                        }
                    }
                }

                // ── 5. Income Sources Horizontal Bar ────────────────────────────
                if (uiState.incomeBySource.isNotEmpty()) {
                    item {
                        ChartCard(title = "Income Sources", icon = "💚") {
                            HorizontalBarChart(
                                items = uiState.incomeBySource.take(7),
                                maxValue = uiState.incomeBySource.firstOrNull()?.second ?: 1.0,
                                barColor = AccentTeal
                            )
                        }
                    }
                }

                // ── 6. Net Savings Bar Chart ─────────────────────────────────────
                if (uiState.monthlyTrend.size >= 2) {
                    item {
                        ChartCard(title = "Monthly Net Savings", icon = "💰") {
                            NetSavingsBarChart(data = uiState.monthlyTrend)
                        }
                    }
                }
            }
        }
    }
}

// ── Chart card wrapper ────────────────────────────────────────────────────────

@Composable
fun ChartCard(title: String, icon: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = BgSecondary)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

// ── 1. Donut Chart ────────────────────────────────────────────────────────────

@Composable
fun DonutChart(income: Double, expenses: Double) {
    val total = income + expenses
    if (total <= 0) { EmptyChartState(); return }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "donut_anim"
    )
    val incomeAngle = ((income / total) * 360f * animProgress).toFloat()
    val expenseAngle = ((expenses / total) * 360f * animProgress).toFloat()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = size.minDimension * 0.18f
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset / 2, inset / 2)
                // Income arc
                drawArc(color = AccentTeal, startAngle = -90f,
                    sweepAngle = incomeAngle, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round))
                // Expense arc
                drawArc(color = ErrorRed, startAngle = -90f + incomeAngle,
                    sweepAngle = expenseAngle, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round))
            }
            // Center text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val savings = income - expenses
                Text(
                    if (savings >= 0) "Saved" else "Over",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    "TZS ${fmt(kotlin.math.abs(savings))}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (savings >= 0) AccentTeal else ErrorRed
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            LegendItem(AccentTeal, "Income", income)
            LegendItem(ErrorRed, "Expenses", expenses)
        }
    }
}

// ── 2. Pie Chart ──────────────────────────────────────────────────────────────

val PIE_COLORS = listOf(
    Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFF9C27B0),
    Color(0xFF00BCD4), Color(0xFFFF5722), Color(0xFF607D8B)
)

@Composable
fun PieChart(slices: List<Pair<String, Double>>, colors: List<Color>) {
    if (slices.isEmpty()) { EmptyChartState(); return }
    val total = slices.sumOf { it.second }
    if (total <= 0) { EmptyChartState(); return }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "pie_anim"
    )

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(160.dp)) {
            var startAngle = -90f
            slices.forEachIndexed { i, (_, value) ->
                val sweep = ((value / total) * 360f * animProgress).toFloat()
                val color = colors[i % colors.size]
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    size = Size(size.width * 0.9f, size.height * 0.9f),
                    topLeft = Offset(size.width * 0.05f, size.height * 0.05f)
                )
                // White divider line
                if (sweep > 5f) {
                    drawArc(color = Color.White, startAngle = startAngle,
                        sweepAngle = sweep, useCenter = true,
                        size = Size(size.width * 0.9f, size.height * 0.9f),
                        topLeft = Offset(size.width * 0.05f, size.height * 0.05f),
                        style = Stroke(2f))
                }
                startAngle += sweep
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            slices.forEachIndexed { i, (label, value) ->
                val pct = (value / total * 100).toInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(colors[i % colors.size], CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text("$pct%  TZS ${fmt(value)}", fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

// ── 3. Trend Line Chart ───────────────────────────────────────────────────────

@Composable
fun TrendLineChart(incomeData: List<Double>, expenseData: List<Double>, labels: List<String>) {
    if (incomeData.size < 2) { EmptyChartState(); return }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1400, easing = EaseOutCubic),
        label = "line_anim"
    )
    val maxVal = (incomeData + expenseData).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

    Column {
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val pw = size.width
            val ph = size.height
            val padL = 40f; val padB = 30f; val padT = 10f; val padR = 10f
            val chartW = pw - padL - padR
            val chartH = ph - padB - padT
            val n = incomeData.size

            // Grid lines
            repeat(4) { i ->
                val y = padT + chartH * (1f - i / 3f)
                drawLine(Color.Gray.copy(0.15f), Offset(padL, y), Offset(pw - padR, y), 1f)
            }

            // Draw line for both income and expense with animation clip
            fun drawLine(data: List<Double>, color: Color) {
                val points = data.mapIndexed { i, v ->
                    val x = padL + (i.toFloat() / (n - 1)) * chartW
                    val y = padT + chartH * (1f - (v / maxVal).toFloat())
                    Offset(x, y)
                }
                val animatedCount = (points.size * animProgress).toInt().coerceAtLeast(2)
                val animPoints = points.take(animatedCount)

                // Filled area
                val path = Path().apply {
                    moveTo(animPoints.first().x, ph - padB)
                    animPoints.forEach { lineTo(it.x, it.y) }
                    lineTo(animPoints.last().x, ph - padB)
                    close()
                }
                drawPath(path, Brush.verticalGradient(
                    listOf(color.copy(0.3f), color.copy(0.0f)),
                    startY = padT, endY = ph - padB
                ))

                // Line
                for (i in 1 until animPoints.size) {
                    drawLine(color, animPoints[i-1], animPoints[i], 3f,
                        cap = StrokeCap.Round)
                }
                // Dots
                animPoints.forEach { pt ->
                    drawCircle(color, 5f, pt)
                    drawCircle(Color.White, 2.5f, pt)
                }
            }

            drawLine(expenseData, ErrorRed)
            drawLine(incomeData, AccentTeal)

            // X labels
            labels.forEachIndexed { i, label ->
                if (labels.size <= 7 || i % (labels.size / 6).coerceAtLeast(1) == 0) {
                    val x = padL + (i.toFloat() / (n - 1)) * chartW
                    // Can't use drawContext.canvas.nativeCanvas easily — skip text for simplicity
                }
            }
        }

        // Legend
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LegendItem(AccentTeal, "Income", null)
            LegendItem(ErrorRed, "Expenses", null)
        }
        // Month labels below
        Row(Modifier.fillMaxWidth().padding(start = 40.dp, end = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            labels.take(7).forEach { label ->
                Text(label, fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

// ── 4. Horizontal Bar Chart ───────────────────────────────────────────────────

@Composable
fun HorizontalBarChart(
    items: List<Pair<String, Double>>,
    maxValue: Double,
    barColor: Color = ErrorRed
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { (label, value) ->
            val animWidth by animateFloatAsState(
                targetValue = if (maxValue > 0) (value / maxValue).toFloat() else 0f,
                animationSpec = tween(900, easing = EaseOutCubic),
                label = "bar_$label"
            )
            Column {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                        modifier = Modifier.weight(1f))
                    Text("TZS ${fmt(value)}", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Spacer(Modifier.height(3.dp))
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF3A4558))) {
                    Box(Modifier.fillMaxWidth(animWidth).fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(Brush.horizontalGradient(
                            listOf(barColor.copy(0.7f), barColor)
                        )))
                }
            }
        }
    }
}

// ── 5. Net Savings Bar Chart ──────────────────────────────────────────────────

data class MonthData(val label: String, val income: Double, val expense: Double) {
    val net: Double get() = income - expense
}

@Composable
fun NetSavingsBarChart(data: List<MonthData>) {
    val maxAbs = data.maxOfOrNull { abs(it.net) }?.coerceAtLeast(1.0) ?: 1.0

    Column {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val pw = size.width; val ph = size.height
            val barCount = data.size
            val barW = (pw / barCount) * 0.55f
            val gap = (pw / barCount) * 0.45f
            val midY = ph / 2f

            // Center zero line
            drawLine(Color.Gray.copy(0.3f), Offset(0f, midY), Offset(pw, midY), 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f)))

            data.forEachIndexed { i, item ->
                val cx = (pw / barCount) * (i + 0.5f)
                val barH = ((abs(item.net) / maxAbs) * (midY - 12f)).toFloat()
                val color = if (item.net >= 0) AccentTeal else ErrorRed
                val top = if (item.net >= 0) midY - barH else midY
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cx - barW / 2, top),
                    size = Size(barW, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            data.forEach { item ->
                Text(item.label, fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LegendItem(AccentTeal, "Surplus", null)
            LegendItem(ErrorRed, "Deficit", null)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
fun LegendItem(color: Color, label: String, value: Double?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(
            text = if (value != null) "$label: TZS ${fmt(value)}" else label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun EmptyChartState() {
    Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
        Text("Not enough data yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}

private fun fmt(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(amount)
