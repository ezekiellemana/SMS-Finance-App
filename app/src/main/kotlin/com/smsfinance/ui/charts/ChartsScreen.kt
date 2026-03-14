// noinspection SpellCheckingInspection
package com.smsfinance.ui.charts
import com.smsfinance.R

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.ChartsViewModel
import com.smsfinance.viewmodel.MultiUserViewModel
import java.text.NumberFormat
import java.util.*
import kotlin.math.*
import com.smsfinance.ui.components.AppScreenScaffold
import kotlinx.coroutines.delay

// ── Palette ───────────────────────────────────────────────────────────────────
private val ChartBg       = Color(0xFF141E2E)
private val CardBg        = Color(0xFF1C2740)
private val CardStroke    = Color(0xFF2A3A58)
private val TextPrimary   = Color(0xFFEEF2FF)
private val TextDim       = Color(0xFF6B7A99)

val PIE_COLORS = listOf(
    Color(0xFF3DDAD7), Color(0xFF5B8EFF), Color(0xFFFF6B8A),
    Color(0xFFFFBB44), Color(0xFF69F0AE), Color(0xFFCE93D8)
)

@Composable
fun ChartsScreen(
    viewModel: ChartsViewModel = hiltViewModel(),
    multiUserVm: MultiUserViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val multiState    by multiUserVm.uiState.collectAsStateWithLifecycle()
    var selectedPeriod by remember { mutableIntStateOf(0) }

    val profileAccent: Color = remember(multiState.activeProfile?.color) {
        runCatching { Color(multiState.activeProfile?.color?.toColorInt() ?: 0xFF3DDAD7.toInt()) }
            .getOrElse { AccentTeal }
    }

    LaunchedEffect(selectedPeriod) { viewModel.loadData(selectedPeriod) }

    // Staggered card entrance
    var cardsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(120); cardsVisible = true }

    AppScreenScaffold(
        title = "Analytics",
        subtitle = "Your financial story",
        onNavigateBack = onNavigateBack
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .background(ChartBg)
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Period selector ────────────────────────────────────────────────
            item {
                AnimatedVisibility(cardsVisible, enter = fadeIn(tween(300)) + slideInVertically(tween(350)) { -20 }) {
                    PeriodSelector(selectedPeriod, profileAccent) { selectedPeriod = it }
                }
            }

            // ── Summary hero ───────────────────────────────────────────────────
            item {
                AnimatedVisibility(cardsVisible && !uiState.isLoading,
                    enter = fadeIn(tween(400, 80)) + slideInVertically(tween(450, 80)) { 30 }
                ) {
                    SummaryHeroCard(uiState.totalIncome, uiState.totalExpenses, profileAccent)
                }
            }

            if (!uiState.isLoading) {

                // ── Donut ──────────────────────────────────────────────────────
                item {
                    AnimatedVisibility(cardsVisible, enter = fadeIn(tween(400, 160)) + slideInVertically(tween(450, 160)) { 40 }) {
                        AnalyticsCard("Income vs Expenses", "🍩", profileAccent) {
                            DonutChart(uiState.totalIncome, uiState.totalExpenses, profileAccent)
                        }
                    }
                }

                // ── Trend line ─────────────────────────────────────────────────
                if (uiState.monthlyTrend.size >= 2) {
                    item {
                        AnimatedVisibility(cardsVisible, enter = fadeIn(tween(400, 240)) + slideInVertically(tween(450, 240)) { 40 }) {
                            AnalyticsCard("Spending Trend", "📈", profileAccent) {
                                TrendLineChart(
                                    incomeData  = uiState.monthlyTrend.map { it.income },
                                    expenseData = uiState.monthlyTrend.map { it.expense },
                                    labels      = uiState.monthlyTrend.map { it.label },
                                    profileAccent = profileAccent
                                )
                            }
                        }
                    }
                }

                // ── Spending by source ─────────────────────────────────────────
                if (uiState.expensesBySource.isNotEmpty()) {
                    item {
                        AnimatedVisibility(cardsVisible, enter = fadeIn(tween(400, 320)) + slideInVertically(tween(450, 320)) { 40 }) {
                            AnalyticsCard("Top Expense Sources", "📊", profileAccent) {
                                HorizontalBarChart(
                                    items    = uiState.expensesBySource.take(7),
                                    maxValue = uiState.expensesBySource.firstOrNull()?.second ?: 1.0,
                                    barColor = Color(0xFFFF6B8A)
                                )
                            }
                        }
                    }
                }

                // ── Spending by source pie ─────────────────────────────────────
                if (uiState.expensesBySource.isNotEmpty()) {
                    item {
                        AnimatedVisibility(cardsVisible, enter = fadeIn(tween(400, 380)) + slideInVertically(tween(450, 380)) { 40 }) {
                            AnalyticsCard("Spending Breakdown", "🥧", profileAccent) {
                                PieChart(uiState.expensesBySource.take(6), PIE_COLORS)
                            }
                        }
                    }
                }

                // ── Income sources ─────────────────────────────────────────────
                if (uiState.incomeBySource.isNotEmpty()) {
                    item {
                        AnimatedVisibility(cardsVisible, enter = fadeIn(tween(400, 440)) + slideInVertically(tween(450, 440)) { 40 }) {
                            AnalyticsCard("Income Sources", "💚", profileAccent) {
                                HorizontalBarChart(
                                    items    = uiState.incomeBySource.take(7),
                                    maxValue = uiState.incomeBySource.firstOrNull()?.second ?: 1.0,
                                    barColor = profileAccent
                                )
                            }
                        }
                    }
                }

                // ── Net savings ────────────────────────────────────────────────
                if (uiState.monthlyTrend.size >= 2) {
                    item {
                        AnimatedVisibility(cardsVisible, enter = fadeIn(tween(400, 500)) + slideInVertically(tween(450, 500)) { 40 }) {
                            AnalyticsCard("Monthly Net Savings", "💰", profileAccent) {
                                NetSavingsBarChart(uiState.monthlyTrend, profileAccent)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Period selector ───────────────────────────────────────────────────────────

@Composable
private fun PeriodSelector(selected: Int, accent: Color, onSelect: (Int) -> Unit) {
    val periods = listOf("Month", "Quarter", "Year")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardStroke, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        periods.forEachIndexed { i, label ->
            val isSelected = selected == i
            val bgAlpha by animateFloatAsState(if (isSelected) 1f else 0f, tween(220), label = "pa$i")
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(bgAlpha * 0.2f))
                    .then(if (isSelected) Modifier.border(1.dp, accent.copy(0.5f), RoundedCornerShape(12.dp)) else Modifier)
                    .clickable(remember { MutableInteractionSource() }, null) { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) accent else TextDim
                )
            }
        }
    }
}

// ── Summary hero card ─────────────────────────────────────────────────────────

@Composable
private fun SummaryHeroCard(income: Double, expenses: Double, accent: Color) {
    val savings = income - expenses
    val savingsPct = if (income > 0) (savings / income * 100).toInt() else 0

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(accent.copy(0.22f), Color(0xFF1C2740))))
            .border(1.dp, Brush.linearGradient(listOf(accent.copy(0.5f), accent.copy(0.08f))),
                RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        // Background glow
        Box(Modifier.matchParentSize().drawBehind {
            drawCircle(Brush.radialGradient(
                listOf(accent.copy(0.12f), Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.3f),
                radius = size.width * 0.6f
            ))
        })
        Column {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column {
                    Text("Net Savings", fontSize = 12.sp, color = accent.copy(0.7f),
                        letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(4.dp))
                    val animSavings by animateFloatAsState(savings.toFloat(), tween(1000, easing = EaseOutCubic), label = "sv")
                    Text(
                        "TZS ${fmt(animSavings.toDouble())}",
                        fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (savings >= 0) accent else Color(0xFFFF6B8A),
                        letterSpacing = (-0.5).sp
                    )
                }
                // Savings rate badge
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .background(if (savings >= 0) accent.copy(0.15f) else Color(0xFFFF6B8A).copy(0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("$savingsPct%", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                        color = if (savings >= 0) accent else Color(0xFFFF6B8A))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPill("Income", income, AccentTeal, Modifier.weight(1f))
                StatPill("Expenses", expenses, Color(0xFFFF6B8A), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Text(label, fontSize = 11.sp, color = color.copy(0.75f), letterSpacing = 0.3.sp)
        }
        Spacer(Modifier.height(3.dp))
        Text("TZS ${fmt(value)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
    }
}

// ── Analytics card wrapper ────────────────────────────────────────────────────

@Composable
private fun AnalyticsCard(
    title: String, icon: String, accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .border(1.dp, CardStroke, RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 18.sp)
            }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary,
                letterSpacing = (-0.2).sp)
        }
        Spacer(Modifier.height(18.dp))
        content()
    }
}

// ── 1. Donut Chart ────────────────────────────────────────────────────────────

@Composable
fun DonutChart(income: Double, expenses: Double, accent: Color = AccentTeal) {
    val total = income + expenses
    if (total <= 0) { EmptyChartState(); return }

    val animProgress by animateFloatAsState(1f,
        tween(1100, easing = EaseOutCubic), label = "donut")
    val incomeAngle  = (income  / total * 360f * animProgress).toFloat()
    val expenseAngle = (expenses / total * 360f * animProgress).toFloat()
    val savings = income - expenses

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = size.minDimension * 0.16f
                val inset  = stroke * 0.55f
                val arc    = Size(size.width - stroke, size.height - stroke)
                val tl     = Offset(inset, inset)
                // Background ring
                drawArc(Color.White.copy(0.05f), -90f, 360f, false, tl, arc, style = Stroke(stroke))
                // Income arc
                drawArc(accent, -90f, incomeAngle, false, tl, arc,
                    style = Stroke(stroke, cap = StrokeCap.Round))
                // Expense arc
                drawArc(Color(0xFFFF6B8A), -90f + incomeAngle, expenseAngle, false, tl, arc,
                    style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (savings >= 0) "Saved" else "Over",
                    fontSize = 10.sp, color = TextDim, letterSpacing = 0.5.sp)
                Text("TZS ${fmt(abs(savings))}",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = if (savings >= 0) accent else Color(0xFFFF6B8A),
                    textAlign = TextAlign.Center)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DonutLegendRow(accent, "Income", income, income / total)
            DonutLegendRow(Color(0xFFFF6B8A), "Expenses", expenses, expenses / total)
        }
    }
}

@Composable
private fun DonutLegendRow(color: Color, label: String, value: Double, fraction: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).background(color, CircleShape))
            Text(label, fontSize = 12.sp, color = TextDim)
            Spacer(Modifier.weight(1f))
            Text("${(fraction * 100).toInt()}%", fontSize = 12.sp,
                fontWeight = FontWeight.Bold, color = color)
        }
        Text("TZS ${fmt(value)}", fontSize = 11.sp, color = TextPrimary.copy(0.8f))
        // Mini progress bar
        val animW by animateFloatAsState(fraction.toFloat().coerceIn(0f, 1f),
            tween(900, easing = EaseOutCubic), label = "donutBar")
        Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(color.copy(0.15f))) {
            Box(Modifier.fillMaxWidth(animW).fillMaxHeight().clip(CircleShape).background(color))
        }
    }
}

// ── 2. Pie Chart ──────────────────────────────────────────────────────────────

@Composable
fun PieChart(slices: List<Pair<String, Double>>, colors: List<Color>) {
    if (slices.isEmpty()) { EmptyChartState(); return }
    val total = slices.sumOf { it.second }
    if (total <= 0) { EmptyChartState(); return }

    val animProgress by animateFloatAsState(1f,
        tween(1200, easing = EaseOutCubic), label = "pie")

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Canvas(Modifier.size(140.dp)) {
            var startAngle = -90f
            slices.forEachIndexed { i, (_, value) ->
                val sweep = (value / total * 360f * animProgress).toFloat()
                drawArc(
                    color      = colors[i % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter  = true,
                    topLeft    = Offset(size.width * 0.06f, size.height * 0.06f),
                    size       = Size(size.width * 0.88f, size.height * 0.88f)
                )
                startAngle += sweep
            }
            // Centre hole
            drawCircle(ChartBg, size.minDimension * 0.28f)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            slices.forEachIndexed { i, (label, value) ->
                val pct = (value / total * 100).toInt()
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape)
                        .background(colors[i % colors.size]))
                    Text(label, fontSize = 11.sp, color = TextPrimary.copy(0.85f),
                        modifier = Modifier.weight(1f), maxLines = 1)
                    Text("$pct%", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = colors[i % colors.size])
                }
            }
        }
    }
}

// ── 3. Trend Line Chart ───────────────────────────────────────────────────────

@Composable
fun TrendLineChart(
    incomeData: List<Double>, expenseData: List<Double>,
    labels: List<String>, profileAccent: Color = AccentTeal
) {
    if (incomeData.size < 2) { EmptyChartState(); return }

    val animProgress by animateFloatAsState(1f,
        tween(1400, easing = EaseOutCubic), label = "line")
    val maxVal = (incomeData + expenseData).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

    Column {
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            val pw = size.width; val ph = size.height
            val padL = 8f; val padB = 28f; val padT = 12f; val padR = 8f
            val cW = pw - padL - padR; val cH = ph - padB - padT
            val n = incomeData.size

            // Horizontal grid lines with labels
            repeat(4) { gi ->
                val y = padT + cH * (1f - gi / 3f)
                drawLine(Color.White.copy(0.06f), Offset(padL, y), Offset(pw - padR, y), 1f)
            }

            fun drawSeries(data: List<Double>, color: Color) {
                val pts = data.mapIndexed { i, v ->
                    Offset(padL + (i.toFloat() / (n - 1)) * cW,
                        padT + cH * (1f - (v / maxVal).toFloat()))
                }
                val count = (pts.size * animProgress).toInt().coerceAtLeast(2)
                val visible = pts.take(count)

                // Filled area gradient
                drawPath(Path().apply {
                    moveTo(visible.first().x, ph - padB)
                    visible.forEach { lineTo(it.x, it.y) }
                    lineTo(visible.last().x, ph - padB); close()
                }, Brush.verticalGradient(listOf(color.copy(0.28f), color.copy(0f)),
                    startY = padT, endY = ph - padB))

                // Smooth line
                val path = Path()
                visible.forEachIndexed { i, pt ->
                    if (i == 0) path.moveTo(pt.x, pt.y)
                    else {
                        val prev = visible[i - 1]
                        val cx = (prev.x + pt.x) / 2f
                        path.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                    }
                }
                drawPath(path, color, style = Stroke(2.5f, cap = StrokeCap.Round))

                // Dots at data points
                visible.forEach { pt ->
                    drawCircle(color.copy(0.3f), 8f, pt)
                    drawCircle(color, 4f, pt)
                    drawCircle(ChartBg, 2f, pt)
                }
            }

            drawSeries(expenseData, Color(0xFFFF6B8A))
            drawSeries(incomeData, profileAccent)
        }

        // X-axis labels
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            labels.take(7).forEach { label ->
                Text(label, fontSize = 10.sp, color = TextDim)
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ChartLegendDot(profileAccent, "Income")
            ChartLegendDot(Color(0xFFFF6B8A), "Expenses")
        }
    }
}

// ── 4. Horizontal Bar Chart ───────────────────────────────────────────────────

@Composable
fun HorizontalBarChart(
    items: List<Pair<String, Double>>,
    maxValue: Double,
    barColor: Color = Color(0xFFFF6B8A)
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { index, (label, value) ->
            val animWidth by animateFloatAsState(
                if (maxValue > 0) (value / maxValue).toFloat() else 0f,
                tween(900 + index * 80, easing = EaseOutCubic), label = "bar_$index"
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween,
                    Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Rank number
                        Box(Modifier.size(20.dp).clip(CircleShape)
                            .background(barColor.copy(0.14f)),
                            contentAlignment = Alignment.Center) {
                            Text("${index + 1}", fontSize = 9.sp,
                                fontWeight = FontWeight.Bold, color = barColor)
                        }
                        Text(label, fontSize = 12.sp, color = TextPrimary.copy(0.85f),
                            maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    }
                    Text("TZS ${fmt(value)}", fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, color = barColor)
                }
                // Bar track
                Box(Modifier.fillMaxWidth().height(8.dp)
                    .clip(CircleShape).background(barColor.copy(0.10f))) {
                    Box(Modifier.fillMaxWidth(animWidth).fillMaxHeight().clip(CircleShape)
                        .background(Brush.horizontalGradient(
                            listOf(barColor.copy(0.6f), barColor))))
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
fun NetSavingsBarChart(data: List<MonthData>, accent: Color = AccentTeal) {
    val maxAbs = data.maxOfOrNull { abs(it.net) }?.coerceAtLeast(1.0) ?: 1.0
    val animProgress by animateFloatAsState(1f, tween(1000, easing = EaseOutCubic), label = "netSavingsBar")

    Column {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val pw = size.width; val ph = size.height
            val count = data.size
            val barW = (pw / count) * 0.5f
            val midY = ph / 2f

            // Dashed zero line
            drawLine(Color.White.copy(0.12f), Offset(0f, midY), Offset(pw, midY), 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))

            data.forEachIndexed { i, item ->
                val cx  = (pw / count) * (i + 0.5f)
                val barH = ((abs(item.net) / maxAbs) * (midY - 16f) * animProgress).toFloat()
                val isPos = item.net >= 0
                val color = if (isPos) accent else Color(0xFFFF6B8A)
                val top   = if (isPos) midY - barH else midY
                // Bar glow
                drawRoundRect(color.copy(0.18f), Offset(cx - barW / 2 - 4f, top - 4f),
                    Size(barW + 8f, barH + 8f), CornerRadius(10f))
                // Bar
                drawRoundRect(color, Offset(cx - barW / 2, top),
                    Size(barW, barH), CornerRadius(8f))
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            data.forEach { item ->
                Text(item.label, fontSize = 10.sp, color = TextDim, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ChartLegendDot(accent, "Surplus")
            ChartLegendDot(Color(0xFFFF6B8A), "Deficit")
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, fontSize = 11.sp, color = TextDim)
    }
}



@Composable
fun EmptyChartState() {
    Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("📭", fontSize = 28.sp)
            Text(stringResource(R.string.not_enough_data_chart),
                fontSize = 13.sp, color = TextDim, textAlign = TextAlign.Center)
        }
    }
}

private fun fmt(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(amount)