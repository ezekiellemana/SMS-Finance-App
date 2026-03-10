@file:Suppress("SpellCheckingInspection", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
package com.smsfinance.ui.onboarding
import com.smsfinance.R

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

// 0=Hero 1=SMS Detection 2=Dashboard 3=Security 4=Personalize
private const val TOTAL_PAGES = 5
private const val PAGE_SETUP  = 4

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
    @field:androidx.annotation.StringRes val titleRes: Int,
    @field:androidx.annotation.StringRes val taglineRes: Int,
    @field:androidx.annotation.StringRes val bodyRes: Int,
    val accent: Color,
    val items: List<Triple<ImageVector, Int, Int>>  // icon, titleRes, subtitleRes
)

private val FEATURE_PAGES = listOf(
    FeaturePage(
        emoji      = "📲",
        titleRes   = R.string.onb_page1_title,
        taglineRes = R.string.onb_page1_subtitle,
        bodyRes    = R.string.onb_page1_body_long,
        accent     = AccentTeal,
        items      = listOf(
            Triple(Icons.Default.FlashOn,        R.string.sms_instant_capture, R.string.sms_reads_in),
            Triple(Icons.Default.AccountBalance, R.string.sms_17_services,     R.string.sms_services_list),
            Triple(Icons.Default.AutoAwesome,    R.string.sms_smart_patterns,  R.string.sms_learns),
        )
    ),
    FeaturePage(
        emoji      = "📊",
        titleRes   = R.string.onb_page2_title,
        taglineRes = R.string.onb_page2_subtitle,
        bodyRes    = R.string.onb_page2_body_long,
        accent     = AccentBlue,
        items      = listOf(
            Triple(Icons.Default.Widgets,    R.string.onb_feat_widget,  R.string.onb_feat_widget_sub),
            Triple(Icons.Default.PieChart,   R.string.onb_feat_charts,  R.string.onb_feat_charts_sub),
            Triple(Icons.Default.Psychology, R.string.onb_feat_ai,      R.string.onb_feat_ai_sub),
        )
    ),
    FeaturePage(
        emoji      = "🔐",
        titleRes   = R.string.onb_page3_title,
        taglineRes = R.string.onb_page3_subtitle,
        bodyRes    = R.string.onb_page3_body_long,
        accent     = AccentGreen,
        items      = listOf(
            Triple(Icons.Default.PhoneLocked, R.string.onb_feat_ondevice,  R.string.onb_feat_ondevice_sub),
            Triple(Icons.Default.Lock,        R.string.onb_feat_aes,       R.string.onb_feat_aes_sub),
            Triple(Icons.Default.Fingerprint, R.string.onb_feat_biometric, R.string.onb_feat_biometric_sub),
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
    onFinished: () -> Unit,
    onLangChange: (() -> Unit)? = null
) {
    val pagerState = rememberPagerState(pageCount = { TOTAL_PAGES })
    val scope      = rememberCoroutineScope()
    val context    = LocalContext.current

    val currentLang by settingsViewModel.language.collectAsStateWithLifecycle()
    // Read initial lang from SharedPrefs synchronously so the roller always
    // starts at the correct position — even right after a recreate() where
    // the DataStore flow might not have emitted yet.
    var selectedLang by remember {
        val prefs = context.getSharedPreferences("app_language", android.content.Context.MODE_PRIVATE)
        mutableStateOf(prefs.getString("language", "en") ?: "en")
    }
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
                        0         -> HeroPage(
                            selectedLang = selectedLang,
                            onSelectLang = { lang ->
                                selectedLang = lang
                                settingsViewModel.setLanguage(lang, context)
                                // No recreate() here — resources.updateConfiguration()
                                // in setContent handles the recompose instantly.
                            }
                        )
                        1, 2, 3   -> FeaturePageContent(FEATURE_PAGES[p - 1])
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
                    when (page) {
                        0 -> Spacer(Modifier.width(1.dp))
                        PAGE_SETUP -> {
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
                        else -> Spacer(Modifier.width(1.dp))
                    }

                    // Colour cross-fades smoothly across pages
                    val btnColor  by animateColorAsState(
                        if (canProceed) accent else accent.copy(.35f),
                        tween(600, easing = EaseInOutSine), label = "btnc")
                    val btnColor2 by animateColorAsState(
                        if (canProceed) accent.copy(.55f) else accent.copy(.15f),
                        tween(600, easing = EaseInOutSine), label = "btnc2")
                    // Press spring — identical feel to the pager swipe
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val btnScale by animateFloatAsState(
                        when { isPressed -> .93f; !canProceed -> .97f; else -> 1f },
                        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                        label = "btnsc")
                    val btnLabel = if (isLast) stringResource(R.string.get_started)
                    else        stringResource(R.string.next_label)

                    Box(
                        Modifier
                            .scale(btnScale)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                if (canProceed)
                                    Brush.horizontalGradient(listOf(btnColor2, btnColor))
                                else
                                    Brush.horizontalGradient(listOf(BgRow, BgRow))
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication        = null,
                                enabled           = canProceed
                            ) {
                                scope.launch {
                                    if (isLast) {
                                        settingsViewModel.saveUserSetup(
                                            userName, selectedSenders.toList(), openingBalances)
                                        settingsViewModel.setOnboardingDone()
                                        onFinished()
                                    } else {
                                        pagerState.animateScrollToPage(
                                            page + 1,
                                            animationSpec = tween(520, easing = EaseInOutSine)
                                        )
                                    }
                                }
                            }
                            .padding(start = 22.dp, end = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                btnLabel,
                                color      = Color(0xFF05111E),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 15.sp,
                                letterSpacing = .1.sp
                            )
                            // Circle arrow badge
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF05111E).copy(.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint     = Color(0xFF05111E),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
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
                @Suppress("UNUSED_EXPRESSION")
                scope.launch {
                    sheetState.hide()
                    showSheet = false
                }
                Unit
            }
        )
    }
}

// ── Language options ─────────────────────────────────────────────────────────
private data class LangOption(val code: String, val flag: String, val name: String, val native: String)
private val LANGUAGES = listOf(
    LangOption("en", "🇬🇧", "English",  "English"),
    LangOption("sw", "🇹🇿", "Swahili",  "Kiswahili"),
)

// ── Page 0: Hero — full-screen with embedded language picker ─────────────────
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun HeroPage(selectedLang: String, onSelectLang: (String) -> Unit) {
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { triggered = true }

    val alpha  by animateFloatAsState(if (triggered) 1f else 0f, tween(700), label = "ha")
    val slideY by animateFloatAsState(if (triggered) 0f else 50f, tween(700, easing = EaseOut), label = "hsy")
    val logoSc by animateFloatAsState(if (triggered) 1f else .3f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "hls")

    val pulse = rememberInfiniteTransition(label = "hp")
    val glowR by pulse.animateFloat(130f, 200f,
        infiniteRepeatable(tween(2800, easing = EaseInOutSine), RepeatMode.Reverse), label = "hgr")
    val glowA by pulse.animateFloat(.07f, .20f,
        infiniteRepeatable(tween(2800, easing = EaseInOutSine), RepeatMode.Reverse), label = "hga")
    val ringA by pulse.animateFloat(.35f, .75f,
        infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "hra")
    val ringR by pulse.animateFloat(0f, 4f,
        infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "hrr")

    // Language roller state
    val initIdx      = LANGUAGES.indexOfFirst { it.code == selectedLang }.coerceAtLeast(0)
    val rowState     = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = initIdx)
    val snapBehavior = rememberSnapFlingBehavior(rowState)
    val langScope    = rememberCoroutineScope()
    val centreIndex  by remember { derivedStateOf { rowState.firstVisibleItemIndex } }
    // Fire instantly as centreIndex changes — no debounce needed because
    // recreate() in onLangChange reloads all strings immediately.
    LaunchedEffect(centreIndex) {
        val lang = LANGUAGES.getOrNull(centreIndex)
        if (lang != null && lang.code != selectedLang) onSelectLang(lang.code)
    }

    Box(Modifier.fillMaxSize()) {
        // Deep atmosphere — two layered glows
        Box(Modifier.fillMaxSize().drawBehind {
            // Primary pulsing glow top-centre
            drawCircle(
                Brush.radialGradient(
                    listOf(AccentTeal.copy(glowA), Color.Transparent),
                    Offset(size.width * .5f, size.height * .22f),
                    glowR.dp.toPx()
                )
            )
            // Secondary static blue glow bottom-right for depth
            drawCircle(
                Brush.radialGradient(
                    listOf(AccentBlue.copy(.05f), Color.Transparent),
                    Offset(size.width * .85f, size.height * .75f),
                    size.width * .6f
                )
            )
            // Bottom-to-top fade so content doesn't float
            drawRect(Brush.verticalGradient(
                listOf(Color.Transparent, BgPrimary.copy(.7f)),
                startY = size.height * .45f, endY = size.height))
        })

        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── TOP SECTION: Logo + title + subtitle ─────────────────────────
            Spacer(Modifier.weight(1.2f))

            Box(
                Modifier
                    .size(118.dp)
                    .scale(logoSc)
                    .drawBehind {
                        // Outer radial pulse
                        drawCircle(Brush.radialGradient(
                            listOf(AccentTeal.copy(glowA * .8f), Color.Transparent),
                            radius = size.minDimension * 1.1f))
                        // Animated ring
                        drawCircle(
                            color  = AccentTeal.copy(ringA),
                            radius = size.minDimension / 2 + ringR.dp.toPx(),
                            style  = Stroke(1.8.dp.toPx()))
                    }
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF1E3A5A), Color(0xFF0A1525))))
                    .drawBehind {
                        drawCircle(color = AccentTeal.copy(.5f),
                            radius = size.minDimension / 2 - .5.dp.toPx(),
                            style  = Stroke(1.5.dp.toPx()))
                    },
                contentAlignment = Alignment.Center
            ) { Text("💰", fontSize = 48.sp) }

            Spacer(Modifier.height(22.dp))

            Text("Smart Money",
                fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
                color = TextWhite, textAlign = TextAlign.Center, letterSpacing = (-.6).sp,
                modifier = Modifier.graphicsLayer { this.alpha = alpha; translationY = slideY })

            Spacer(Modifier.height(8.dp))

            Text(stringResource(R.string.hero_tagline),
                fontSize = 16.sp, color = AccentTeal, textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.graphicsLayer { this.alpha = alpha })

            Spacer(Modifier.height(20.dp))

            // Feature pills
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("📲", stringResource(R.string.hero_pill_sms),     AccentTeal),
                        Triple("📊", stringResource(R.string.hero_pill_charts),  AccentBlue),
                        Triple("🔐", stringResource(R.string.hero_pill_private), AccentGreen)
                    ).forEach { (emoji, label, col) ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(col.copy(.10f))
                                .drawBehind {
                                    drawRoundRect(color = col.copy(.25f), size = this.size,
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                                        style = Stroke(.8.dp.toPx()))
                                }
                                .padding(horizontal = 11.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(emoji, fontSize = 13.sp)
                                Text(label, color = col, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ── MIDDLE SECTION: Stats card ────────────────────────────────────
            Spacer(Modifier.weight(1f))

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgCard)
                    .drawBehind {
                        drawRoundRect(color = AccentTeal.copy(.14f), size = this.size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                            style = Stroke(1.dp.toPx()))
                    }
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .graphicsLayer { this.alpha = alpha }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Top row — tag
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(AccentTeal))
                        Text(stringResource(R.string.hero_card_tag),
                            color = TextSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    // Stats row
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        listOf(
                            Triple("📲", stringResource(R.string.hero_stat1_title), stringResource(R.string.hero_stat1_sub)),
                            Triple("📊", stringResource(R.string.hero_stat2_title), stringResource(R.string.hero_stat2_sub)),
                            Triple("🔐", stringResource(R.string.hero_stat3_title), stringResource(R.string.hero_stat3_sub)),
                        ).forEach { (emoji, statTitle, sub) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.weight(1f)) {
                                Text(emoji, fontSize = 18.sp)
                                Text(statTitle, color = TextWhite, fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Text(sub, color = TextMuted, fontSize = 9.sp,
                                    textAlign = TextAlign.Center, lineHeight = 12.sp)
                            }
                        }
                    }
                }
            }

            // ── BOTTOM SECTION: Language picker ──────────────────────────────
            Spacer(Modifier.weight(.8f))

            // Section label
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha }) {
                Box(Modifier.weight(1f).height(1.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, TextMuted.copy(.2f)))))
                Text(stringResource(R.string.hero_choose_lang), color = TextMuted, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = .5.sp)
                Box(Modifier.weight(1f).height(1.dp).background(
                    Brush.horizontalGradient(listOf(TextMuted.copy(.2f), Color.Transparent))))
            }

            Spacer(Modifier.height(10.dp))

            // Compact horizontal language roller — transparent background
            val itemW = 140.dp
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .graphicsLayer { this.alpha = alpha }
            ) {
                val screenW = maxWidth
                // Selection highlight in centre only
                Box(
                    Modifier
                        .width(itemW).height(64.dp)
                        .align(Alignment.Center)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentGold.copy(.10f))
                        .drawBehind {
                            drawRoundRect(color = AccentGold.copy(.38f), size = this.size,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                                style = Stroke(1.4.dp.toPx()))
                        }
                )

                LazyRow(
                    state         = rowState,
                    flingBehavior = snapBehavior,
                    modifier      = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = (screenW - itemW) / 2)
                ) {
                    items(LANGUAGES.size) { idx ->
                        val lang = LANGUAGES[idx]
                        val isSel = idx == centreIndex
                        val iAlpha by animateFloatAsState(if (isSel) 1f else .3f, tween(200), label = "lia$idx")
                        val iScale by animateFloatAsState(if (isSel) 1f else .80f,
                            spring(Spring.DampingRatioMediumBouncy), label = "lis$idx")
                        Box(
                            Modifier.width(itemW).fillMaxHeight()
                                .clickable(remember { MutableInteractionSource() }, null) {
                                    langScope.launch { rowState.animateScrollToItem(idx) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                Modifier.scale(iScale).graphicsLayer { this.alpha = iAlpha },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(lang.flag, fontSize = 22.sp)
                                Column {
                                    Text(lang.name, fontSize = 14.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) TextWhite else TextMuted)
                                    Text(lang.native, fontSize = 10.sp,
                                        color = if (isSel) AccentGold else TextMuted.copy(.5f))
                                }
                                if (isSel) {
                                    Icon(Icons.Default.Check, null,
                                        tint = AccentGold, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.hero_swipe_hint),
                fontSize = 10.sp, color = TextMuted.copy(.45f),
                modifier = Modifier.graphicsLayer { this.alpha = alpha })

            Spacer(Modifier.weight(.6f))
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

        Text(stringResource(page.titleRes),
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
        ) { Text(stringResource(page.taglineRes), color = page.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(18.dp))

        Text(stringResource(page.bodyRes), fontSize = 15.sp, color = TextMuted,
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
            page.items.forEachIndexed { i, (icon, titleRes, subtitleRes) ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                        .background(page.accent.copy(.12f)), Alignment.Center) {
                        Icon(icon, null, tint = page.accent, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(titleRes), color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(subtitleRes), color = TextMuted, fontSize = 12.sp)
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

// ── Page 4: Personalise / Setup ───────────────────────────────────────────────
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
                    ServiceTile("🏦", stringResource(R.string.onb_banks_label),
                        if (bankCount > 0) stringResource(R.string.onb_filled_of, bankCount, ALL_SENDERS.count{it.category=="Bank"}) else "NMB, CRDB, NBC…",
                        bankCount > 0, Modifier.weight(1f)) { onOpenSheet("Bank") }
                    ServiceTile("📱", stringResource(R.string.onb_mobile_money_label),
                        if (mmCount > 0) stringResource(R.string.onb_filled_of, mmCount, ALL_SENDERS.count{it.category=="Mobile Money"}) else "M-Pesa, Tigo…",
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
                            Text(if (done) stringResource(R.string.onb_all_set) else stringResource(R.string.onb_filled_of, filled, selectedSenders.size),
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
                    Text(if (category == "Bank") stringResource(R.string.onb_choose_bank) else stringResource(R.string.onb_choose_mobile),
                        fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextWhite)
                    Text(
                        if (available.isEmpty() && selectedSenders.isNotEmpty())
                            stringResource(R.string.all_added_check)
                        else stringResource(R.string.onb_tap_add_count, available.size),
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
                        Text(if (query.isNotBlank()) stringResource(R.string.onb_no_match, query)
                        else stringResource(R.string.onb_all_cat_added, category.lowercase()),
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