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
import com.smsfinance.ui.components.LocalProfileColor
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
                val profileAccent = LocalProfileColor.current
                val privMode by viewModel.privacyMode.collectAsStateWithLifecycle()
                LiveWidgetPreview(selected, profileAccent, privMode)

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
fun LiveWidgetPreview(
    theme: WidgetTheme,
    profileColor: Color = Color(0xFF3DDAD7),
    privacyMode: Boolean = false
) {
    val bgStart = Color(theme.bgColorStart)
    val bgEnd   = Color(theme.bgColorEnd)
    val text    = Color(theme.textColor)
    val accent  = Color(theme.accentColor)
    val expClr  = Color(0xFFFF5C5C)
    val isLight    = theme == WidgetTheme.LIGHT_CLEAN
    val textColor  = if (isLight) Color(0xFF1A2233) else text
    val mutedColor = if (isLight) Color(0xFF1A2233).copy(0.55f) else text.copy(0.55f)
    val faintColor = if (isLight) Color(0xFF1A2233).copy(0.35f) else text.copy(0.35f)
    val dotColor   = if (isLight) accent else AccentTeal

    val inf = rememberInfiniteTransition(label = "dot")
    val dotAlpha by inf.animateFloat(1f, 0.3f,
        infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse), label = "d")

    // Outer container — matches widget_medium.xml geometry
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, profileColor, RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(
                listOf(bgStart, bgEnd),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end   = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {

            // ── Top row: live dot + app name + time ──────────────────────────
            Row(
                Modifier.fillMaxWidth().height(18.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(7.dp).background(
                        dotColor.copy(dotAlpha), CircleShape))
                    Text("SMART MONEY", color = text.copy(0.55f),
                        fontSize = 10.sp, letterSpacing = 1.2.sp)
                }
                Text("09:41", color = faintColor, fontSize = 11.sp)
            }

            Spacer(Modifier.height(5.dp))

            // ── Balance + income/expense pills ───────────────────────────────
            Row(
                Modifier.fillMaxWidth().height(52.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                // Balance column
                Column(Modifier.weight(1f)) {
                    Text("Est. Balance", color = mutedColor, fontSize = 11.sp)
                    Text(if (privacyMode) "TZS ••••••" else "TZS 1,250,000",
                        color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Income pill
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(.18f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("IN", color = accent.copy(.7f), fontSize = 9.sp,
                            letterSpacing = 0.8.sp)
                        Text(if (privacyMode) "↑ ••••" else "↑ TZS 850K", color = accent,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    // Expense pill
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(expClr.copy(.18f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("OUT", color = expClr.copy(.7f), fontSize = 9.sp,
                            letterSpacing = 0.8.sp)
                        Text(if (privacyMode) "↓ ••••" else "↓ TZS 350K", color = expClr,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── Divider ───────────────────────────────────────────────────────
            Box(Modifier.fillMaxWidth().height(1.dp)
                .background(text.copy(0.13f).takeIf { !isLight } ?: Color.Black.copy(0.10f)))

            Spacer(Modifier.height(5.dp))

            // ── "RECENT ACTIVITY" label ───────────────────────────────────────
            Text("RECENT ACTIVITY", color = accent.copy(.4f),
                fontSize = 11.sp, letterSpacing = 1.4.sp)

            Spacer(Modifier.height(5.dp))

            // ── Transaction rows ──────────────────────────────────────────────
            listOf(
                Triple(true,  "M-Pesa",     "+ TZS 50,000"),
                Triple(false, "CRDB Bank",  "- TZS 15,000"),
                Triple(true,  "NMB Bank",   "+ TZS 800,000")
            ).forEachIndexed { i, (isDeposit, source, amount) ->
                val rowAccent = if (isDeposit) accent else expClr
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .then(if (i < 2) Modifier.padding(bottom = 3.dp) else Modifier)
                        .clip(RoundedCornerShape(8.dp))
                        .background(text.copy(0.08f))
                        .padding(horizontal = 10.dp),
                    Arrangement.SpaceBetween, Alignment.CenterVertically
                ) {
                    // Arrow icon
                    Text(if (isDeposit) "↑" else "↓",
                        color = rowAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(14.dp))
                    Spacer(Modifier.width(7.dp))
                    // Source (fills space)
                    Text(if (privacyMode) "••••••" else source, color = textColor.copy(.93f), fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1,
                        modifier = Modifier.weight(1f))
                    // Date
                    Text(if (privacyMode) "" else "14 Mar", color = faintColor, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp))
                    // Amount
                    Text(if (privacyMode) "••••" else amount, color = rowAccent, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold)
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