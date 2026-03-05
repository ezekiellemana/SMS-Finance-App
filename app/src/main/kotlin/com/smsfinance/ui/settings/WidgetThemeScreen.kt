package com.smsfinance.ui.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.WidgetTheme
import com.smsfinance.ui.components.AppScreenScaffold
import com.smsfinance.ui.components.GlassCard
import com.smsfinance.ui.components.SectionHeader
import com.smsfinance.ui.components.ScreenEnterAnimation
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.SettingsViewModel

@Composable
fun WidgetThemeScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val currentTheme by viewModel.widgetTheme.collectAsStateWithLifecycle()
    val selected = WidgetTheme.entries.firstOrNull { it.name == currentTheme } ?: WidgetTheme.SMART_DARK

    AppScreenScaffold(
        title = "Widget Themes",
        subtitle = "Customize your home screen widget",
        onNavigateBack = onNavigateBack
    ) { padding ->
        ScreenEnterAnimation {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionHeader("Preview")
                LiveWidgetPreview(selected)

                SectionHeader("Choose Theme")
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(480.dp)
                ) {
                    items(WidgetTheme.entries) { theme ->
                        ThemeCard(
                            theme = theme,
                            isSelected = selected == theme,
                            onClick = { viewModel.setWidgetTheme(theme.name) }
                        )
                    }
                }

                GlassCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null,
                            tint = AccentTeal, modifier = Modifier.size(20.dp))
                        Text(
                            "Theme changes apply instantly. After changing, long-press the widget and select Remove, then re-add it to see the new theme.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun LiveWidgetPreview(theme: WidgetTheme) {
    val bgStart = Color(theme.bgColorStart)
    val bgEnd   = Color(theme.bgColorEnd)
    val text    = Color(theme.textColor)
    val accent  = Color(theme.accentColor)
    val expense = Color(0xFFFF5252)

    val inf = rememberInfiniteTransition(label = "dot")
    val dotAlpha by inf.animateFloat(1f, 0.2f,
        infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse), label = "a")

    Box(
        Modifier.fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(bgStart, bgEnd)))
            .padding(18.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(7.dp).background(AccentTeal.copy(dotAlpha), CircleShape))
                        Text("SMART MONEY", color = text.copy(0.6f), fontSize = 9.sp,
                            letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(3.dp))
                    Text("TZS 1,250,000", color = text, fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("↑ TZS 850,000", color = accent, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                    Text("↓ TZS 350,000", color = expense, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(text.copy(0.2f)))
            Spacer(Modifier.height(10.dp))
            Text("RECENT ACTIVITY", color = text.copy(0.5f), fontSize = 9.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            listOf(
                Triple("↑", "HaloPesa", "+ TZS 20,000"),
                Triple("↓", "CRDB Agent", "- TZS 15,000"),
                Triple("↑", "M-Pesa", "+ TZS 100")
            ).forEach { (icon, source, amount) ->
                val isDeposit = icon == "↑"
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(text.copy(0.08f))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    Arrangement.SpaceBetween, Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(icon, color = if (isDeposit) accent else expense,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(source, color = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(amount, color = if (isDeposit) accent else expense,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.End) {
                Surface(color = text.copy(0.15f), shape = RoundedCornerShape(50)) {
                    Text("${theme.emoji} ${theme.displayName}", color = text, fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun ThemeCard(theme: WidgetTheme, isSelected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val bgStart = Color(theme.bgColorStart)
    val bgEnd   = Color(theme.bgColorEnd)
    val text    = Color(theme.textColor)
    val accent  = Color(theme.accentColor)

    val scale by animateFloatAsState(
        if (isSelected) 1.03f else 1f,
        spring(dampingRatio = 0.6f), label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth().height(100.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (isSelected) 8.dp else 2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(bgStart, bgEnd)))
            .then(if (isSelected) Modifier.border(2.dp, AccentTeal, RoundedCornerShape(16.dp)) else Modifier)
            .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }
            .padding(14.dp)
    ) {
        Column(Modifier.fillMaxSize(), Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Text(theme.emoji, fontSize = 22.sp)
                if (isSelected) {
                    Box(Modifier.size(22.dp).background(AccentTeal, CircleShape), Alignment.Center) {
                        Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = Color.White)
                    }
                }
            }
            Column {
                Text(theme.displayName, color = text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(width = 20.dp, height = 4.dp).background(accent, RoundedCornerShape(2.dp)))
                    Box(Modifier.size(width = 12.dp, height = 4.dp).background(Color(0xFFFF5252), RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}