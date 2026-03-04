package com.smsfinance.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.smsfinance.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

// ── Brand Colors ──────────────────────────────────────────────────────────────
private val BgPrimary     = Color(0xFF1F2633)
private val BgSecondary   = Color(0xFF2C3546)
private val AccentTeal    = Color(0xFF3DDAD7)
private val AccentLight   = Color(0xFF5CE1E6)
private val TextWhite     = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFAAB4C3)

// ── Page Data ─────────────────────────────────────────────────────────────────
data class OnboardingPage(
    val icon: ImageVector,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val accentColor: Color,
    val features: List<Pair<ImageVector, String>>
)

val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Default.Sms,
        emoji = "📱",
        title = "Auto SMS\nDetection",
        subtitle = "Zero manual entry",
        description = "Automatically reads financial messages from NMB, CRDB, M-Pesa, Airtel Money and 15+ providers. Every transaction captured instantly.",
        accentColor = AccentTeal,
        features = listOf(
            Icons.Default.AccountBalance to "NMB & CRDB Bank",
            Icons.Default.PhoneAndroid   to "M-Pesa & Airtel",
            Icons.Default.FlashOn        to "Instant capture"
        )
    ),
    OnboardingPage(
        icon = Icons.Default.Dashboard,
        emoji = "📊",
        title = "Smart\nDashboard",
        subtitle = "Your finances at a glance",
        description = "Live balance, income and expense charts with AI-powered spending predictions and beautiful monthly trend breakdowns.",
        accentColor = AccentLight,
        features = listOf(
            Icons.Default.ShowChart   to "Live balance",
            Icons.Default.PieChart   to "Spending charts",
            Icons.Default.Psychology to "AI predictions"
        )
    ),
    OnboardingPage(
        icon = Icons.Default.Widgets,
        emoji = "🏠",
        title = "Home Screen\nWidget",
        subtitle = "Always visible, always updated",
        description = "Add the Finance Widget to your home screen. Instant access to your balance without opening the app — choose from 8 themes.",
        accentColor = AccentTeal,
        features = listOf(
            Icons.Default.Bolt          to "Real-time balance",
            Icons.Default.Palette       to "8 color themes",
            Icons.Default.VisibilityOff to "Privacy mode"
        )
    ),
    OnboardingPage(
        icon = Icons.Default.Security,
        emoji = "🔒",
        title = "Private\n& Secure",
        subtitle = "Your data stays on your phone",
        description = "All processing happens locally on your device. Your SMS data is never sent to any server. Protect with PIN or biometrics.",
        accentColor = AccentLight,
        features = listOf(
            Icons.Default.PhoneLocked to "100% local",
            Icons.Default.Lock        to "Encrypted storage",
            Icons.Default.Fingerprint to "Biometric auth"
        )
    )
)

// ── Main Screen ───────────────────────────────────────────────────────────────
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onFinished: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.size - 1
    val currentPage = onboardingPages[pagerState.currentPage]

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        val screenH = maxHeight
        // Controls height reserved at the bottom for dots + buttons
        val bottomBarH = if (screenH < 600.dp) 100.dp else 120.dp

        // Animated background glow
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                currentPage.accentColor.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.28f),
                            radius = size.width * 0.85f
                        )
                    )
                }
        )

        // Pager fills everything above the bottom bar
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(screenH - bottomBarH)
        ) { pageIndex ->
            PageContent(
                page = onboardingPages[pageIndex],
                screenHeight = screenH
            )
        }

        // ── Bottom Controls ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomBarH)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Dot indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(onboardingPages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 6.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "dot_w"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isSelected) currentPage.accentColor
                        else TextSecondary.copy(alpha = 0.4f),
                        label = "dot_c"
                    )
                    Box(
                        Modifier
                            .height(6.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Buttons row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLastPage) {
                    TextButton(
                        onClick = { settingsViewModel.setOnboardingDone(); onFinished() }
                    ) {
                        Text(
                            "Skip",
                            color = TextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (isLastPage) {
                            settingsViewModel.setOnboardingDone()
                            onFinished()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentPage.accentColor),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
                    modifier = Modifier.drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    currentPage.accentColor.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                radius = size.maxDimension
                            )
                        )
                    }
                ) {
                    Text(
                        text = if (isLastPage) "Get Started" else "Continue",
                        color = BgPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (!isLastPage) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            null,
                            tint = BgPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Individual Page ───────────────────────────────────────────────────────────
@Composable
private fun PageContent(page: OnboardingPage, screenHeight: Dp) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(page) { visible = true }

    // ── Adaptive sizing based on screen height ────────────────────────────────
    // Compact  < 600dp  (small phones, e.g. Pixel 4a)
    // Normal   600–700dp (Pixel 7, most mid-range)
    // Large    > 700dp  (Pixel 7 Pro, tall phones)
    val isCompact = screenHeight < 620.dp
    val isNormal  = screenHeight in 620.dp..720.dp

    val iconSize      = if (isCompact) 90.dp  else if (isNormal) 110.dp  else 130.dp
    val innerIconSize = if (isCompact) 62.dp  else if (isNormal) 76.dp   else 90.dp
    val emojiSize     = if (isCompact) 28.sp  else if (isNormal) 34.sp   else 40.sp
    val titleSize     = if (isCompact) 26.sp  else if (isNormal) 30.sp   else 34.sp
    val titleLineH    = if (isCompact) 32.sp  else if (isNormal) 36.sp   else 40.sp
    val descSize      = if (isCompact) 13.sp  else 15.sp
    val descLineH     = if (isCompact) 19.sp  else 23.sp
    val subtitleSize  = if (isCompact) 11.sp  else 12.sp
    val spacerIcon    = if (isCompact) 16.dp  else if (isNormal) 24.dp   else 36.dp
    val spacerTitle   = if (isCompact) 6.dp   else 10.dp
    val spacerSub     = if (isCompact) 14.dp  else 24.dp
    val spacerDesc    = if (isCompact) 14.dp  else 32.dp
    val topPad        = if (isCompact) 24.dp  else if (isNormal) 48.dp   else 72.dp
    val featureVertPad = if (isCompact) 4.dp  else 8.dp
    val featureIconSz  = if (isCompact) 30.dp else 36.dp

    val iconScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "icon_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, easing = EaseOut),
        label = "content_alpha"
    )

    // verticalScroll as safety net for very small/unusual screen sizes
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .padding(top = topPad, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        // ── Icon circle ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                page.accentColor.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
                }
                .clip(CircleShape)
                .background(BgSecondary),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(innerIconSize)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                page.accentColor.copy(alpha = 0.2f),
                                page.accentColor.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .drawBehind {
                        val sw = 1.5.dp.toPx()
                        drawCircle(
                            color = page.accentColor.copy(alpha = 0.6f),
                            radius = size.minDimension / 2 - sw / 2,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(sw)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(page.emoji, fontSize = emojiSize)
            }
        }

        Spacer(Modifier.height(spacerIcon))

        // ── Title ─────────────────────────────────────────────────────────────
        Text(
            text = page.title,
            fontSize = titleSize,
            fontWeight = FontWeight.ExtraBold,
            color = TextWhite,
            textAlign = TextAlign.Center,
            lineHeight = titleLineH,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha }
        )

        Spacer(Modifier.height(spacerTitle))

        // ── Subtitle pill ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(page.accentColor.copy(alpha = 0.15f))
                .drawBehind {
                    val sw = 1.dp.toPx()
                    drawRoundRect(
                        color = page.accentColor.copy(alpha = 0.4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(50.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(sw)
                    )
                }
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = page.subtitle,
                color = page.accentColor,
                fontSize = subtitleSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(Modifier.height(spacerSub))

        // ── Description ───────────────────────────────────────────────────────
        Text(
            text = page.description,
            fontSize = descSize,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = descLineH,
            modifier = Modifier.graphicsLayer { alpha = contentAlpha }
        )

        Spacer(Modifier.height(spacerDesc))

        // ── Feature rows ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(BgSecondary)
                .padding(vertical = featureVertPad),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            page.features.forEachIndexed { i, (icon, label) ->
                FeatureRow(
                    icon = icon,
                    label = label,
                    accent = page.accentColor,
                    iconBoxSize = featureIconSz,
                    isCompact = isCompact
                )
                if (i < page.features.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        thickness = 0.5.dp,
                        color = TextSecondary.copy(alpha = 0.15f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    label: String,
    accent: Color,
    iconBoxSize: Dp = 36.dp,
    isCompact: Boolean = false
) {
    val vertPad = if (isCompact) 10.dp else 14.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = vertPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(iconBoxSize)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        }
        Text(
            text = label,
            color = TextWhite,
            fontSize = if (isCompact) 13.sp else 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Default.Check,
            null,
            tint = accent,
            modifier = Modifier.size(15.dp)
        )
    }
}