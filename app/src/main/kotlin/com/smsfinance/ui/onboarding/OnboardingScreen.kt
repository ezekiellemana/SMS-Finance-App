@file:Suppress("SpellCheckingInspection", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
package com.smsfinance.ui.onboarding
import com.smsfinance.R

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.smsfinance.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

// ── Colours ───────────────────────────────────────────────────────────────────
private val BgPrimary   = Color(0xFF111820)
private val BgCard      = Color(0xFF1A2438)
private val BgSheet     = Color(0xFF1C2840)
private val BgRow       = Color(0xFF232F45)
private val AccentTeal  = Color(0xFF3DDAD7)
private val AccentLight = Color(0xFF5CE1E6)
private val AccentGold  = Color(0xFFFFCA28)
private val AccentGreen = Color(0xFF43C59E)
private val AccentBlue  = Color(0xFF5B8DEF)
private val TextWhite   = Color(0xFFFFFFFF)
private val TextMuted   = Color(0xFF8A96A8)
private val TextSoft    = Color(0xFFCDD5E0)
private val GreenOk     = Color(0xFF43C59E)

// 0=Hero 1=SMS 2=Dashboard 3=Security 4=Language 5=Personalize
private const val TOTAL_PAGES = 6
private const val PAGE_SETUP  = 5
private const val PAGE_LANG   = 4

// ── Sender data ───────────────────────────────────────────────────────────────
data class SenderOption(val id: String, val displayName: String, val emoji: String, val category: String)

val ALL_SENDERS = listOf(
    SenderOption("NMB",        "NMB Bank",                  "🏦", "Bank"),
    SenderOption("CRDB",       "CRDB Bank",                 "🏦", "Bank"),
    SenderOption("NBC",        "NBC Bank",                  "🏦", "Bank"),
    SenderOption("EQUITY",     "Equity Bank",               "🏦", "Bank"),
    SenderOption("STANBIC",    "Stanbic Bank",              "🏦", "Bank"),
    SenderOption("ABSA",       "ABSA Bank",                 "🏦", "Bank"),
    SenderOption("EXIM",       "EXIM Bank",                 "🏦", "Bank"),
    SenderOption("DTB",        "DTB Bank",                  "🏦", "Bank"),
    SenderOption("MPESA",      "M-Pesa",                    "📱", "Mobile Money"),
    SenderOption("MIXX",       "Mixx by Yas (Tigo Pesa)",   "📱", "Mobile Money"),
    SenderOption("AIRTEL",     "Airtel Money",              "📱", "Mobile Money"),
    SenderOption("HALOPESA",   "HaloPesa",                  "📱", "Mobile Money"),
    SenderOption("TPESA",      "T-Pesa",                    "📱", "Mobile Money"),
    SenderOption("AZAMPESA",   "AzamPesa",                  "📱", "Mobile Money"),
    SenderOption("SELCOMPESA", "SelcomPesa",                "📱", "Mobile Money"),
    SenderOption("EZYPESA",    "EzyPesa",                   "📱", "Mobile Money"),
    SenderOption("NALA",       "NALA",                      "📱", "Mobile Money"),
)

// ── Feature page model ────────────────────────────────────────────────────────
data class FeaturePage(
    val emoji: String,
    val title: String,
    val tagline: String,
    val body: String,
    val accent: Color,
    val items: List<Triple<ImageVector, String, String>>
)

private val FEATURE_PAGES = listOf(
    FeaturePage(
        emoji   = "📲",
        title   = "Auto SMS\nDetection",
        tagline = "Zero manual entry",
        body    = "Smart Money silently watches your incoming SMS and instantly captures every transaction — deposits, withdrawals, transfers — the moment they arrive.",
        accent  = AccentTeal,
        items   = listOf(
            Triple(Icons.Default.FlashOn,        "Instant capture",  "Reads SMS in under 2 seconds"),
            Triple(Icons.Default.AccountBalance, "17+ services",     "NMB, CRDB, M-Pesa, Airtel & more"),
            Triple(Icons.Default.AutoAwesome,    "Smart patterns",   "Learns your transaction formats"),
        )
    ),
    FeaturePage(
        emoji   = "📊",
        title   = "Smart\nDashboard",
        tagline = "Your money at a glance",
        body    = "A live home-screen widget and full dashboard show your balance, income and expenses in real-time — beautifully charted and always up to date.",
        accent  = AccentBlue,
        items   = listOf(
            Triple(Icons.Default.Widgets,    "Home widget",    "Balance visible without opening app"),
            Triple(Icons.Default.PieChart,   "Spending charts","Visual breakdown by category"),
            Triple(Icons.Default.Psychology, "AI predictions", "Forecast where your money goes"),
        )
    ),
    FeaturePage(
        emoji   = "🔐",
        title   = "Private\n& Secure",
        tagline = "Stays on your phone",
        body    = "Every transaction is stored locally using AES encryption. Nothing is sent to any server. Your financial data belongs only to you.",
        accent  = AccentGreen,
        items   = listOf(
            Triple(Icons.Default.PhoneLocked, "On-device only",   "No cloud, no servers, ever"),
            Triple(Icons.Default.Lock,        "AES encryption",   "Data locked at rest"),
            Triple(Icons.Default.Fingerprint, "Biometric lock",   "PIN or fingerprint protection"),
        )
    ),
)

// ── Root ──────────────────────────────────────────────────────────────────────
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun OnboardingScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onFinished: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { TOTAL_PAGES })
    val scope      = rememberCoroutineScope()
    val context    = LocalContext.current

    var selectedLang    by remember { mutableStateOf("en") }
    var userName        by remember { mutableStateOf("") }
    var selectedSenders by remember { mutableStateOf(setOf<String>()) }
    var openingBalances by remember { mutableStateOf(mapOf<String, String>()) }
    var sheetCategory   by remember { mutableStateOf("Bank") }
    var showSheet       by remember { mutableStateOf(false) }
    val sheetState      = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val page = pagerState.currentPage
    val accent = when (page) {
        1    -> AccentTeal
        2    -> AccentBlue
        3    -> AccentGreen
        4    -> AccentGold
        else -> AccentTeal
    }
    val isLast     = page == TOTAL_PAGES - 1
    val canProceed = page != PAGE_SETUP || (
            userName.isNotBlank() && selectedSenders.isNotEmpty() &&
                    selectedSenders.all { id ->
                        (openingBalances[id]?.replace(",", "")?.toDoubleOrNull() ?: 0.0) > 0.0
                    }
            )

    Box(Modifier.fillMaxSize().background(BgPrimary)) {

        val glowColor by animateColorAsState(accent.copy(.07f), tween(600), label = "glow")
        Box(Modifier.fillMaxSize().drawBehind {
            drawCircle(
                Brush.radialGradient(listOf(glowColor, Color.Transparent),
                    Offset(size.width * .5f, 0f), size.width * 1.1f)
            )
        })

        Column(Modifier.fillMaxSize()) {

            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = page != PAGE_SETUP || canProceed,
                pageSpacing = 16.dp,
                beyondViewportPageCount = 1
            ) { p ->
                val pageOffset = (pagerState.currentPage - p) + pagerState.currentPageOffsetFraction
                val absOffset  = abs(pageOffset).coerceIn(0f, 1f)
                Box(
                    Modifier.fillMaxSize().statusBarsPadding()
                        .graphicsLayer {
                            alpha        = 1f - absOffset * 0.65f
                            val sc       = 0.90f + 0.10f * (1f - absOffset)
                            scaleX = sc; scaleY = sc
                            translationX = pageOffset * 24.dp.toPx()
                            rotationY    = pageOffset * -4f
                            cameraDistance = 12f * density
                        }
                ) {
                    when (p) {
                        0         -> HeroPage()
                        1, 2, 3   -> FeaturePageContent(FEATURE_PAGES[p - 1])
                        PAGE_LANG -> LanguagePage(
                            selected = selectedLang,
                            onSelect = { lang ->
                                selectedLang = lang
                                settingsViewModel.setLanguage(lang, context)
                            }
                        )
                        PAGE_SETUP -> SetupPage(
                            userName        = userName,
                            onNameChange    = { userName = it },
                            selectedSenders = selectedSenders,
                            openingBalances = openingBalances,
                            onBalanceChange = { id, v -> openingBalances = openingBalances + (id to v) },
                            onOpenSheet     = { cat -> sheetCategory = cat; showSheet = true }
                        )
                        else -> {}
                    }
                }
            }

            // ── Bottom nav ────────────────────────────────────────────────────
            Column(
                Modifier.fillMaxWidth().background(BgPrimary)
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp)
                    .padding(top = 12.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    repeat(TOTAL_PAGES) { i ->
                        val sel = page == i
                        val w by animateDpAsState(if (sel) 22.dp else 5.dp,
                            spring(Spring.DampingRatioMediumBouncy), label = "dot$i")
                        val c by animateColorAsState(
                            if (sel) accent else TextMuted.copy(.25f), tween(300), label = "dotc$i")
                        Box(Modifier.height(5.dp).width(w).clip(CircleShape).background(c))
                    }
                }

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    when {
                        page == 0 || page == PAGE_LANG -> Spacer(Modifier.width(1.dp))
                        page == PAGE_SETUP -> {
                            AnimatedVisibility(!canProceed,
                                enter = fadeIn(tween(300)), exit = fadeOut(tween(200))) {
                                val hint = when {
                                    userName.isBlank()        -> stringResource(R.string.enter_name_first)
                                    selectedSenders.isEmpty() -> stringResource(R.string.select_one_service)
                                    else                      -> stringResource(R.string.fill_all_balances)
                                }
                                Text(hint, color = TextMuted, fontSize = 12.sp)
                            }
                            if (canProceed) Spacer(Modifier.width(1.dp))
                        }
                        else -> TextButton(
                            onClick = { settingsViewModel.setOnboardingDone(); onFinished() },
                            contentPadding = PaddingValues(0.dp)
                        ) { Text(stringResource(R.string.skip_label), color = TextMuted, fontSize = 14.sp) }
                    }

                    val btnScale by animateFloatAsState(
                        if (canProceed) 1f else .97f, spring(Spring.DampingRatioMediumBouncy), label = "btn")
                    val btnColor by animateColorAsState(accent, tween(400), label = "btnc")

                    Button(
                        onClick = {
                            scope.launch {
                                if (isLast) {
                                    settingsViewModel.saveUserSetup(userName, selectedSenders.toList(), openingBalances)
                                    settingsViewModel.setOnboardingDone()
                                    onFinished()
                                } else pagerState.animateScrollToPage(page + 1)
                            }
                        },
                        enabled = canProceed,
                        shape   = RoundedCornerShape(14.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = btnColor, disabledContainerColor = btnColor.copy(.25f)),
                        modifier = Modifier.scale(btnScale).height(48.dp),
                        contentPadding = PaddingValues(horizontal = 28.dp)
                    ) {
                        Text(
                            if (isLast) stringResource(R.string.get_started) else stringResource(R.string.next_label),
                            color = Color(0xFF0D1B2A), fontWeight = FontWeight.Bold, fontSize = 15.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                            tint = Color(0xFF0D1B2A), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (showSheet) {
        SenderSheet(
            category        = sheetCategory,
            selectedSenders = selectedSenders,
            sheetState      = sheetState,
            onSelect        = { id -> selectedSenders = selectedSenders + id },
            onDismiss       = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false }
            }
        )
    }
}

// ── Page 0: Hero ──────────────────────────────────────────────────────────────
@Composable
private fun HeroPage() {
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { triggered = true }

    val alpha  by animateFloatAsState(if (triggered) 1f else 0f, tween(700), label = "ha")
    val slideY by animateFloatAsState(if (triggered) 0f else 60f, tween(700, easing = EaseOut), label = "hsy")
    val logoSc by animateFloatAsState(if (triggered) 1f else .4f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "hls")

    val pulse  = rememberInfiniteTransition(label = "hp")
    val glowR  by pulse.animateFloat(100f, 145f,
        infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse), label = "hgr")
    val glowA  by pulse.animateFloat(.06f, .18f,
        infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse), label = "hga")

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().drawBehind {
            drawCircle(
                Brush.radialGradient(
                    listOf(AccentTeal.copy(glowA), Color.Transparent),
                    Offset(size.width * .5f, size.height * .28f), glowR.dp.toPx()
                )
            )
            drawRect(Brush.verticalGradient(
                listOf(Color.Transparent, BgPrimary.copy(.85f)),
                startY = size.height * .55f, endY = size.height))
        })

        Column(
            Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(.4f))

            // Logo circle
            Box(
                Modifier.size(130.dp).scale(logoSc)
                    .drawBehind {
                        drawCircle(Brush.radialGradient(
                            listOf(AccentTeal.copy(.28f), Color.Transparent),
                            radius = size.minDimension * .9f))
                    }
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF1E3A5A), Color(0xFF0F1E30))))
                    .drawBehind {
                        drawCircle(color = AccentTeal.copy(.55f),
                            radius = size.minDimension / 2,
                            style  = Stroke(2.dp.toPx()))
                    },
                contentAlignment = Alignment.Center
            ) { Text("💰", fontSize = 52.sp) }

            Spacer(Modifier.height(30.dp))

            Text("Smart Money",
                fontSize = 38.sp, fontWeight = FontWeight.ExtraBold,
                color = TextWhite, textAlign = TextAlign.Center, letterSpacing = (-.5).sp,
                modifier = Modifier.graphicsLayer { this.alpha = alpha; translationY = slideY })
            Spacer(Modifier.height(6.dp))
            Text("Your finances, automated.",
                fontSize = 16.sp, color = AccentTeal, textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.graphicsLayer { this.alpha = alpha; translationY = slideY })

            Spacer(Modifier.height(32.dp))

            // Feature pills
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.graphicsLayer { this.alpha = alpha }) {
                listOf("📲 Auto SMS" to AccentTeal, "📊 Live charts" to AccentBlue, "🔐 Private" to AccentGreen)
                    .forEach { (label, col) ->
                        Box(
                            Modifier.clip(CircleShape)
                                .background(col.copy(.12f))
                                .drawBehind {
                                    drawCircle(color = col.copy(.28f),
                                        radius = size.minDimension / 2, style = Stroke(1.dp.toPx()))
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) { Text(label, color = col, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    }
            }

            Spacer(Modifier.height(32.dp))

            // Tagline card
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgCard)
                    .drawBehind {
                        drawRoundRect(color = AccentTeal.copy(.16f), size = this.size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                            style = Stroke(1.dp.toPx()))
                    }
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tanzania's smartest personal finance tracker",
                        color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        listOf("17+ banks", "Zero setup", "100% private").forEach { lbl ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(5.dp).clip(CircleShape).background(AccentTeal))
                                Text(lbl, color = TextSoft, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

// ── Pages 1-3: Feature pages ──────────────────────────────────────────────────
@Composable
private fun FeaturePageContent(page: FeaturePage) {
    var triggered by remember(page) { mutableStateOf(false) }
    LaunchedEffect(page) { triggered = true }

    val alpha     by animateFloatAsState(if (triggered) 1f else 0f, tween(500), label = "fa")
    val slideY    by animateFloatAsState(if (triggered) 0f else 40f, tween(500, easing = EaseOut), label = "fsy")
    val iconScale by animateFloatAsState(if (triggered) 1f else .5f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "fis")

    val pulse  = rememberInfiniteTransition(label = "fp")
    val floatY by pulse.animateFloat(-6f, 6f,
        infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse), label = "fpy")
    val ringA  by pulse.animateFloat(.15f, .40f,
        infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "fra")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp).padding(top = 20.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(140.dp).scale(iconScale)
                .graphicsLayer { translationY = floatY }
                .drawBehind {
                    drawCircle(Brush.radialGradient(
                        listOf(page.accent.copy(.22f), Color.Transparent),
                        radius = size.minDimension * .85f))
                }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(page.accent.copy(.18f), page.accent.copy(.04f))))
                .drawBehind {
                    drawCircle(color = page.accent.copy(ringA),
                        radius = size.minDimension / 2 - 2.dp.toPx(), style = Stroke(2.dp.toPx()))
                },
            contentAlignment = Alignment.Center
        ) { Text(page.emoji, fontSize = 56.sp) }

        Spacer(Modifier.height(28.dp))

        Text(page.title,
            fontSize = 34.sp, fontWeight = FontWeight.ExtraBold,
            color = TextWhite, textAlign = TextAlign.Center,
            lineHeight = 40.sp, letterSpacing = (-.4).sp,
            modifier = Modifier.graphicsLayer { this.alpha = alpha; translationY = slideY })

        Spacer(Modifier.height(10.dp))

        Box(
            Modifier.clip(RoundedCornerShape(50.dp)).background(page.accent.copy(.12f))
                .drawBehind {
                    drawRoundRect(color = page.accent.copy(.3f), size = this.size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(50.dp.toPx()),
                        style = Stroke(.8.dp.toPx()))
                }
                .padding(horizontal = 18.dp, vertical = 7.dp)
        ) { Text(page.tagline, color = page.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(18.dp))

        Text(page.body, fontSize = 15.sp, color = TextMuted,
            textAlign = TextAlign.Center, lineHeight = 24.sp,
            modifier = Modifier.graphicsLayer { this.alpha = alpha; translationY = slideY })

        Spacer(Modifier.height(28.dp))

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(BgCard)
                .drawBehind {
                    drawRoundRect(color = page.accent.copy(.12f), size = this.size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()),
                        style = Stroke(1.dp.toPx()))
                }
                .padding(vertical = 6.dp)
        ) {
            page.items.forEachIndexed { i, (icon, title, subtitle) ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                        .background(page.accent.copy(.12f)), Alignment.Center) {
                        Icon(icon, null, tint = page.accent, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(subtitle, color = TextMuted, fontSize = 12.sp)
                    }
                    Box(Modifier.size(22.dp).clip(CircleShape).background(page.accent.copy(.12f)),
                        Alignment.Center) {
                        Icon(Icons.Default.Check, null, tint = page.accent, modifier = Modifier.size(12.dp))
                    }
                }
                if (i < page.items.size - 1)
                    HorizontalDivider(Modifier.padding(horizontal = 18.dp), .5.dp, TextMuted.copy(.08f))
            }
        }
    }
}

// ── Page 4: Language roller ───────────────────────────────────────────────────
private data class LangOption(val code: String, val flag: String, val name: String, val native: String)
private val LANGUAGES = listOf(
    LangOption("en", "🇬🇧", "English",  "English"),
    LangOption("sw", "🇹🇿", "Swahili",  "Kiswahili"),
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun LanguagePage(selected: String, onSelect: (String) -> Unit) {
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { triggered = true }
    val alpha  by animateFloatAsState(if (triggered) 1f else 0f, tween(500), label = "la")
    val slideY by animateFloatAsState(if (triggered) 0f else 40f, tween(500, easing = EaseOut), label = "lsy")
    val logoSc by animateFloatAsState(if (triggered) 1f else .5f,
        spring(Spring.DampingRatioMediumBouncy), label = "lls")

    val itemHeight   = 80.dp
    val initIdx      = LANGUAGES.indexOfFirst { it.code == selected }.coerceAtLeast(0)
    val listState    = rememberLazyListState(initialFirstVisibleItemIndex = initIdx)
    val snapBehavior = rememberSnapFlingBehavior(listState)
    val scope        = rememberCoroutineScope()

    val centreIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(centreIndex) {
        val lang = LANGUAGES.getOrNull(centreIndex)
        if (lang != null && lang.code != selected) onSelect(lang.code)
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp).padding(top = 24.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(100.dp).scale(logoSc)
                .clip(CircleShape).background(AccentGold.copy(.10f))
                .drawBehind {
                    drawCircle(color = AccentGold.copy(.30f),
                        radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                },
            contentAlignment = Alignment.Center
        ) { Text("🌍", fontSize = 42.sp) }

        Spacer(Modifier.height(20.dp))

        Text("Choose Language",
            fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            color = TextWhite, textAlign = TextAlign.Center, letterSpacing = (-.4).sp,
            modifier = Modifier.graphicsLayer { this.alpha = alpha; translationY = slideY })
        Spacer(Modifier.height(6.dp))
        Text("Scroll to select your preferred language",
            fontSize = 14.sp, color = TextMuted, textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { this.alpha = alpha })

        Spacer(Modifier.height(36.dp))

        // ── Drum roller ───────────────────────────────────────────────────────
        val rollerH = itemHeight * 3
        Box(Modifier.fillMaxWidth().height(rollerH)) {
            // Track background
            Box(Modifier.fillMaxWidth().height(rollerH).clip(RoundedCornerShape(20.dp))
                .background(BgCard)
                .drawBehind {
                    drawRoundRect(color = AccentGold.copy(.12f), size = this.size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                        style = Stroke(1.dp.toPx()))
                })

            // Centre selection highlight
            Box(Modifier.fillMaxWidth().height(itemHeight).align(Alignment.Center)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AccentGold.copy(.10f))
                .drawBehind {
                    drawRoundRect(color = AccentGold.copy(.42f), size = this.size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                        style = Stroke(1.5.dp.toPx()))
                })

            // Top fade
            Box(Modifier.fillMaxWidth().height(itemHeight).align(Alignment.TopCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Brush.verticalGradient(listOf(BgCard, Color.Transparent))))
            // Bottom fade
            Box(Modifier.fillMaxWidth().height(itemHeight).align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(Brush.verticalGradient(listOf(Color.Transparent, BgCard))))

            // Scrollable items
            LazyColumn(
                state         = listState,
                flingBehavior = snapBehavior,
                modifier      = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = itemHeight)
            ) {
                items(LANGUAGES.size) { idx ->
                    val lang      = LANGUAGES[idx]
                    val isCentred = idx == centreIndex
                    val iAlpha by animateFloatAsState(if (isCentred) 1f else .32f, tween(200), label = "ia$idx")
                    val iScale by animateFloatAsState(if (isCentred) 1f else .82f,
                        spring(Spring.DampingRatioMediumBouncy), label = "is$idx")

                    Box(
                        Modifier.fillMaxWidth().height(itemHeight)
                            .clickable(remember { MutableInteractionSource() }, null) {
                                scope.launch { listState.animateScrollToItem(idx) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            Modifier.scale(iScale).graphicsLayer { this.alpha = iAlpha },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(lang.flag, fontSize = 32.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(lang.name,
                                    fontSize   = 18.sp,
                                    fontWeight = if (isCentred) FontWeight.Bold else FontWeight.Normal,
                                    color      = if (isCentred) TextWhite else TextMuted)
                                Text(lang.native, fontSize = 13.sp,
                                    color = if (isCentred) AccentGold else TextMuted.copy(.5f))
                            }
                            if (isCentred) {
                                Spacer(Modifier.width(4.dp))
                                Box(Modifier.size(22.dp).clip(CircleShape).background(AccentGold.copy(.15f)),
                                    Alignment.Center) {
                                    Icon(Icons.Default.Check, null,
                                        tint = AccentGold, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Swipe up or down to change language",
            fontSize = 12.sp, color = TextMuted.copy(.6f), textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { this.alpha = alpha })
    }
}

// ── Page 5: Personalise / Setup ───────────────────────────────────────────────
@Composable
private fun SetupPage(
    userName: String,
    onNameChange: (String) -> Unit,
    selectedSenders: Set<String>,
    openingBalances: Map<String, String>,
    onBalanceChange: (String, String) -> Unit,
    onOpenSheet: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scrollState  = rememberScrollState()
    val bankCount    = selectedSenders.count { id -> ALL_SENDERS.any { it.id == id && it.category == "Bank" } }
    val mmCount      = selectedSenders.count { id -> ALL_SENDERS.any { it.id == id && it.category == "Mobile Money" } }
    val filled       = selectedSenders.count { id -> (openingBalances[id]?.replace(",","")?.toDoubleOrNull() ?: 0.0) > 0.0 }
    val allDone      = selectedSenders.isNotEmpty() && filled == selectedSenders.size
    val activeStep   = when {
        userName.isBlank()        -> 0
        selectedSenders.isEmpty() -> 1
        !allDone                  -> 2
        else                      -> 3
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().drawBehind {
            drawCircle(Brush.radialGradient(listOf(AccentTeal.copy(.07f), Color.Transparent),
                Offset(size.width * .5f, size.height * .18f), size.width * .95f))
        })

        Column(
            Modifier.fillMaxSize().verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 24.dp).padding(top = 28.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            val waveT = rememberInfiniteTransition(label = "wt")
            val waveS by waveT.animateFloat(1f, 1.15f,
                infiniteRepeatable(tween(1000, easing = EaseInOut), RepeatMode.Reverse), label = "ws")

            Column(Modifier.fillMaxWidth().padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👋", fontSize = 44.sp, modifier = Modifier.scale(waveS))
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.lets_set_you_up),
                    fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                    color = TextWhite, textAlign = TextAlign.Center, letterSpacing = (-.3).sp)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.just_3_steps), fontSize = 13.sp, color = TextMuted)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { i ->
                        val done   = i < activeStep
                        val active = i == activeStep
                        val w by animateDpAsState(
                            if (active) 28.dp else if (done) 20.dp else 16.dp,
                            spring(Spring.DampingRatioMediumBouncy), label = "pill$i")
                        val c by animateColorAsState(
                            when { done -> GreenOk; active -> AccentTeal; else -> BgRow },
                            tween(300), label = "pillc$i")
                        Box(Modifier.height(4.dp).width(w).clip(CircleShape).background(c))
                    }
                }
            }

            // Step 1 — Name
            StepCard("01", stringResource(R.string.whats_your_name),
                stringResource(R.string.name_caption),
                done = userName.isNotBlank(), active = activeStep == 0) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = userName, onValueChange = onNameChange,
                    placeholder = {
                        Text("e.g. John Masai", color = TextMuted.copy(.45f),
                            fontSize = 15.sp, fontStyle = FontStyle.Italic)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction      = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    leadingIcon = {
                        Icon(Icons.Default.Person, null,
                            tint = if (userName.isNotBlank()) AccentTeal else TextMuted.copy(.5f),
                            modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        AnimatedVisibility(userName.isNotBlank(), enter = scaleIn(), exit = scaleOut()) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = GreenOk, modifier = Modifier.size(20.dp))
                        }
                    },
                    colors = stepFieldColors(userName.isNotBlank()),
                    shape  = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            StepSpacer()

            // Step 2 — Services
            StepCard("02", stringResource(R.string.which_services),
                stringResource(R.string.services_caption),
                done = selectedSenders.isNotEmpty(), active = activeStep == 1) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ServiceTile("🏦", "Banks",
                        if (bankCount > 0) "$bankCount selected" else "NMB, CRDB, NBC…",
                        bankCount > 0, Modifier.weight(1f)) { onOpenSheet("Bank") }
                    ServiceTile("📱", "Mobile Money",
                        if (mmCount > 0) "$mmCount selected" else "M-Pesa, Tigo…",
                        mmCount > 0, Modifier.weight(1f)) { onOpenSheet("Mobile Money") }
                }
            }

            StepSpacer()

            // Step 3 — Balances
            AnimatedVisibility(selectedSenders.isNotEmpty(),
                enter = fadeIn(tween(350)) + expandVertically(tween(400, easing = EaseOut)),
                exit  = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
                StepCard("03", stringResource(R.string.balance_per_service),
                    stringResource(R.string.balance_caption),
                    done = allDone, active = activeStep == 2) {
                    Spacer(Modifier.height(4.dp))
                    val prog by animateFloatAsState(
                        if (selectedSenders.isEmpty()) 0f else filled.toFloat() / selectedSenders.size,
                        tween(500), label = "prog")
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(BgRow)) {
                            Box(Modifier.fillMaxWidth(prog).fillMaxHeight()
                                .background(Brush.horizontalGradient(
                                    listOf(AccentTeal, if (allDone) GreenOk else AccentLight))))
                        }
                        Spacer(Modifier.width(10.dp))
                        AnimatedContent(allDone, label = "sc") { done ->
                            Text(if (done) "All set" else "$filled / ${selectedSenders.size}",
                                fontSize = 11.sp, color = if (done) GreenOk else TextMuted,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }

                    selectedSenders.forEach { id ->
                        val sender = ALL_SENDERS.find { it.id == id } ?: return@forEach
                        val value  = openingBalances[id] ?: ""
                        val ok     = (value.replace(",", "").toDoubleOrNull() ?: 0.0) > 0.0
                        BalanceRow(sender, value, ok, onDone = { focusManager.clearFocus() }) { raw ->
                            onBalanceChange(id, raw)
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    if (!allDone) {
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Default.Info, null,
                                tint = AccentTeal.copy(.5f), modifier = Modifier.size(11.dp))
                            Text(stringResource(R.string.all_balances_required),
                                fontSize = 10.sp, color = TextMuted.copy(.55f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Sender bottom sheet ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SenderSheet(
    category: String, selectedSenders: Set<String>,
    sheetState: SheetState, onSelect: (String) -> Unit, onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val available = ALL_SENDERS.filter { s ->
        s.category == category && s.id !in selectedSenders &&
                (query.isBlank() || s.displayName.contains(query, ignoreCase = true))
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = BgSheet, tonalElevation = 0.dp,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), Alignment.Center) {
                Box(Modifier.width(32.dp).height(3.dp).clip(CircleShape).background(TextMuted.copy(.3f)))
            }
        }) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 22.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(if (category == "Bank") "Choose your bank" else "Choose mobile money",
                        fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextWhite)
                    Text(
                        if (available.isEmpty() && selectedSenders.isNotEmpty())
                            stringResource(R.string.all_added_check)
                        else "Tap to add · ${available.size} available",
                        fontSize = 12.sp, color = if (available.isEmpty()) GreenOk else TextMuted)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
            OutlinedTextField(value = query, onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_placeholder_ob),
                    color = TextMuted, fontSize = 13.sp) },
                leadingIcon  = { Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(17.dp)) },
                trailingIcon = {
                    AnimatedVisibility(query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                        IconButton(onClick = { query = "" }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentTeal.copy(.5f), unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                    cursorColor = AccentTeal, focusedContainerColor = BgRow, unfocusedContainerColor = BgRow),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())

            AnimatedContent(available.isEmpty(), label = "empty") { empty ->
                if (empty) {
                    Box(Modifier.fillMaxWidth().height(80.dp), Alignment.Center) {
                        Text(if (query.isNotBlank()) "No match for \"$query\""
                        else "All ${category.lowercase()}s added!",
                            color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(available, key = { it.id }) { sender ->
                            SenderSheetRow(sender) {
                                onSelect(sender.id)
                                if (available.size == 1) onDismiss()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SenderSheetRow(sender: SenderOption, onTap: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(BgRow)
        .clickable(remember { MutableInteractionSource() }, null) { onTap() }
        .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(AccentTeal.copy(.08f)),
            Alignment.Center) { Text(sender.emoji, fontSize = 18.sp) }
        Text(sender.displayName, color = TextSoft, fontSize = 14.sp,
            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Default.Add, null, tint = AccentTeal.copy(.7f), modifier = Modifier.size(18.dp))
    }
}

// ── Shared widgets ────────────────────────────────────────────────────────────
@Composable
private fun StepCard(
    number: String, title: String, caption: String, done: Boolean, active: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderAlpha by animateFloatAsState(
        if (active) .7f else if (done) .4f else .15f, tween(300), label = "ba")
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BgCard)
            .drawBehind {
                val stripeW = 3.dp.toPx()
                drawRoundRect(color = (if (done) GreenOk else AccentTeal).copy(borderAlpha),
                    topLeft = Offset(0f, 16.dp.toPx()),
                    size    = androidx.compose.ui.geometry.Size(stripeW, size.height - 32.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(stripeW / 2))
            }
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(30.dp).clip(CircleShape)
                .background(if (done) GreenOk.copy(.15f) else if (active) AccentTeal.copy(.12f) else BgRow),
                Alignment.Center) {
                AnimatedContent(done, label = "ic$number") { isDone ->
                    if (isDone) Icon(Icons.Default.Check, null, tint = GreenOk, modifier = Modifier.size(14.dp))
                    else Text(number, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        color = if (active) AccentTeal else TextMuted.copy(.6f))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = if (done || active) TextWhite else TextMuted)
                Text(caption, fontSize = 11.sp, color = TextMuted.copy(.7f))
            }
        }
        content()
    }
}

@Composable
private fun StepSpacer() {
    Box(Modifier.fillMaxWidth().padding(start = 27.dp)) {
        Box(Modifier.width(2.dp).height(16.dp).background(BgRow.copy(.8f)))
    }
}

@Composable
private fun ServiceTile(
    emoji: String, title: String, subtitle: String,
    selected: Boolean, modifier: Modifier, onClick: () -> Unit
) {
    val bg by animateColorAsState(if (selected) AccentTeal.copy(.12f) else BgRow, tween(250), label = "stbg")
    val sc by animateFloatAsState(if (selected) 1.02f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "stsc")
    Column(
        modifier = modifier.scale(sc).clip(RoundedCornerShape(16.dp)).background(bg)
            .drawBehind {
                if (selected) drawRoundRect(color = AccentTeal.copy(.45f), size = this.size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    style = Stroke(1.5.dp.toPx()))
            }
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape)
            .background(if (selected) AccentTeal.copy(.15f) else BgCard), Alignment.Center) {
            Text(emoji, fontSize = 20.sp)
        }
        Text(title, color = if (selected) AccentTeal else TextSoft,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = if (selected) GreenOk else TextMuted.copy(.7f),
            fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun BalanceRow(
    sender: SenderOption, value: String, ok: Boolean,
    onDone: () -> Unit, onValueChange: (String) -> Unit
) {
    fun fmt(raw: String): String {
        val d = raw.filter { it.isDigit() }
        return if (d.isEmpty()) "" else try { "%,d".format(d.toLong()) }
        catch (_: NumberFormatException) { d }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
            .background(if (ok) GreenOk.copy(.12f) else BgRow), Alignment.Center) {
            Text(sender.emoji, fontSize = 16.sp)
        }
        OutlinedTextField(
            value = fmt(value),
            onValueChange = { typed ->
                val raw = typed.filter { it.isDigit() }
                if (raw.length <= 15) onValueChange(raw)
            },
            placeholder  = { Text(stringResource(R.string.balance_placeholder_ob),
                color = TextMuted.copy(.4f), fontSize = 13.sp) },
            label        = { Text(sender.displayName, fontSize = 10.sp) },
            singleLine   = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            suffix       = {
                if (fmt(value).isNotEmpty())
                    Text("/=", color = if (ok) GreenOk else TextMuted.copy(.5f),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            },
            trailingIcon = {
                AnimatedVisibility(ok, enter = scaleIn(), exit = scaleOut()) {
                    Icon(Icons.Default.CheckCircle, null, tint = GreenOk, modifier = Modifier.size(17.dp))
                }
            },
            colors = stepFieldColors(ok), shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun stepFieldColors(filled: Boolean = false) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor    = if (filled) GreenOk else AccentTeal,
    unfocusedBorderColor  = if (filled) GreenOk.copy(.35f) else BgRow,
    focusedLabelColor     = if (filled) GreenOk else AccentTeal,
    unfocusedLabelColor   = TextMuted.copy(.6f),
    focusedTextColor      = TextWhite, unfocusedTextColor = TextWhite,
    cursorColor           = AccentTeal,
    focusedContainerColor = BgCard, unfocusedContainerColor = BgCard
)