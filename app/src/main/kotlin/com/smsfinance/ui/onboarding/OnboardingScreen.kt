@file:Suppress("SpellCheckingInspection", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
package com.smsfinance.ui.onboarding
import com.smsfinance.R

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.smsfinance.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

// ── Colors ────────────────────────────────────────────────────────────────────
private val BgPrimary    = Color(0xFF1A2130)
private val BgCard       = Color(0xFF212C40)
private val BgSheet      = Color(0xFF1E2A3C)
private val BgRow        = Color(0xFF263044)
private val AccentTeal   = Color(0xFF3DDAD7)
private val AccentLight  = Color(0xFF5CE1E6)
private val TextWhite    = Color(0xFFFFFFFF)
private val TextMuted    = Color(0xFF8A96A8)
private val TextSoft     = Color(0xFFCDD5E0)
private val GreenOk      = Color(0xFF43C59E)

// ── Sender data ───────────────────────────────────────────────────────────────
data class SenderOption(val id: String, val displayName: String, val emoji: String, val category: String)

val ALL_SENDERS = listOf(
    SenderOption("NMB",      "NMB Bank",        "🏦", "Bank"),
    SenderOption("CRDB",     "CRDB Bank",        "🏦", "Bank"),
    SenderOption("NBC",      "NBC Bank",         "🏦", "Bank"),
    SenderOption("EQUITY",   "Equity Bank",      "🏦", "Bank"),
    SenderOption("STANBIC",  "Stanbic Bank",     "🏦", "Bank"),
    SenderOption("ABSA",     "ABSA Bank",        "🏦", "Bank"),
    SenderOption("EXIM",     "EXIM Bank",        "🏦", "Bank"),
    SenderOption("DTB",      "DTB Bank",         "🏦", "Bank"),
    SenderOption("MPESA",     "M-Pesa",                        "📱", "Mobile Money"),
    SenderOption("MIXX",      "Mixx by Yas (formerly Tigo Pesa)", "📱", "Mobile Money"),
    SenderOption("AIRTEL",    "Airtel Money",                  "📱", "Mobile Money"),
    SenderOption("HALOPESA",  "HaloPesa",                      "📱", "Mobile Money"),
    SenderOption("TPESA",     "T-Pesa",                        "📱", "Mobile Money"),
    SenderOption("AZAMPESA",  "AzamPesa",                      "📱", "Mobile Money"),
    SenderOption("SELCOMPESA","SelcomPesa",                     "📱", "Mobile Money"),
    SenderOption("EZYPESA",   "EzyPesa",                       "📱", "Mobile Money"),
    SenderOption("NALA",      "NALA",                          "📱", "Mobile Money"),
)

// ── Info page model ───────────────────────────────────────────────────────────
data class InfoPage(
    val emoji: String,
    @get:androidx.annotation.StringRes val titleRes: Int,
    @get:androidx.annotation.StringRes val subtitleRes: Int,
    @get:androidx.annotation.StringRes val descriptionRes: Int,
    val accent: Color,
    val features: List<Pair<ImageVector, Int>>
)

private val INFO_PAGES = listOf(
    InfoPage("📱", R.string.onb_page1_title, R.string.onb_page1_subtitle,
        R.string.onb_page1_body,
        AccentTeal, listOf(
            Icons.Default.AccountBalance to R.string.onb_feat_zero_entry,
            Icons.Default.PhoneAndroid   to R.string.onb_feat_instant,
            Icons.Default.FlashOn        to R.string.onb_feat_instant)),
    InfoPage("📊", R.string.onb_page2_title, R.string.onb_page2_subtitle,
        R.string.onb_page2_body,
        AccentLight, listOf(
            Icons.AutoMirrored.Filled.ArrowForward to R.string.onb_feat_live,
            Icons.Default.PieChart   to R.string.onb_feat_charts,
            Icons.Default.Psychology to R.string.onb_feat_ai)),
    InfoPage("🔒", R.string.onb_page3_title, R.string.onb_page3_subtitle,
        R.string.onb_page3_body,
        AccentTeal, listOf(
            Icons.Default.PhoneLocked to R.string.onb_feat_ondevice,
            Icons.Default.Lock        to R.string.onb_feat_encrypted,
            Icons.Default.Fingerprint to R.string.onb_feat_biometric)),
)

private const val TOTAL_PAGES = 4

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
    val isLast     = pagerState.currentPage == TOTAL_PAGES - 1

    var userName        by remember { mutableStateOf("") }
    var selectedSenders by remember { mutableStateOf(setOf<String>()) }
    var openingBalances by remember { mutableStateOf(mapOf<String, String>()) }

    var sheetCategory by remember { mutableStateOf("Bank") }
    var showSheet     by remember { mutableStateOf(false) }
    val sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val accent = if (pagerState.currentPage == 1) AccentLight else AccentTeal

    val canProceed = pagerState.currentPage != 2 || (
            userName.isNotBlank() &&
                    selectedSenders.isNotEmpty() &&
                    selectedSenders.all { id ->
                        (openingBalances[id]?.replace(",", "")?.toDoubleOrNull() ?: 0.0) > 0.0
                    }
            )

    Box(Modifier.fillMaxSize().background(BgPrimary)) {

        // Soft ambient glow top-centre
        Box(Modifier.fillMaxSize().drawBehind {
            drawCircle(
                Brush.radialGradient(
                    listOf(accent.copy(.08f), Color.Transparent),
                    Offset(size.width * .5f, 0f), size.width * .9f
                )
            )
        })

        Column(Modifier.fillMaxSize()) {

            // Pages
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = pagerState.currentPage != 2 || canProceed,
                pageSpacing = 16.dp,
                beyondViewportPageCount = 1
            ) { page ->
                val pageOffset = (pagerState.currentPage - page) +
                        pagerState.currentPageOffsetFraction
                val absOffset = kotlin.math.abs(pageOffset).coerceIn(0f, 1f)

                Box(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .graphicsLayer {
                            // Smooth fade
                            alpha = 1f - (absOffset * 0.65f)
                            // Gentle scale — incoming page grows in, outgoing shrinks
                            val scale = 0.90f + 0.10f * (1f - absOffset)
                            scaleX = scale
                            scaleY = scale
                            // Subtle depth: outgoing page moves slightly back
                            translationX = pageOffset * 24.dp.toPx()
                            // Rotation tilt for polish
                            rotationY = pageOffset * -4f
                            cameraDistance = 12f * density
                        }
                ) {
                    when (page) {
                        2    -> SetupPage(
                            userName        = userName,
                            onNameChange    = { userName = it },
                            selectedSenders = selectedSenders,
                            openingBalances = openingBalances,
                            onBalanceChange = { id, v -> openingBalances = openingBalances + (id to v) },
                            onOpenSheet     = { cat -> sheetCategory = cat; showSheet = true }
                        )
                        else -> InfoPageContent(INFO_PAGES[if (page < 2) page else 2])
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
                // Dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(TOTAL_PAGES) { i ->
                        val sel = pagerState.currentPage == i
                        val w by animateDpAsState(if (sel) 20.dp else 5.dp,
                            spring(Spring.DampingRatioMediumBouncy), label = "dot$i")
                        val c by animateColorAsState(
                            if (sel) accent else TextMuted.copy(.3f), tween(300), label = "dotc$i")
                        Box(Modifier.height(5.dp).width(w).clip(CircleShape).background(c))
                    }
                }

                // Buttons row
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    // Left side
                    if (pagerState.currentPage == 2) {
                        // Soft hint
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
                    } else {
                        TextButton(
                            onClick = { settingsViewModel.setOnboardingDone(); onFinished() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(stringResource(R.string.skip_label), color = TextMuted, fontSize = 14.sp)
                        }
                    }

                    // Next / Get Started
                    val btnScale by animateFloatAsState(
                        if (canProceed) 1f else .97f, spring(Spring.DampingRatioMediumBouncy), label = "btn")
                    Button(
                        onClick = {
                            scope.launch {
                                if (isLast) {
                                    settingsViewModel.saveUserSetup(userName, selectedSenders.toList(), openingBalances)
                                    settingsViewModel.setOnboardingDone()
                                    onFinished()
                                } else pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        enabled = canProceed,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            disabledContainerColor = accent.copy(.25f)
                        ),
                        modifier = Modifier.scale(btnScale).height(48.dp),
                        contentPadding = PaddingValues(horizontal = 28.dp)
                    ) {
                        Text(
                            if (isLast) stringResource(R.string.get_started) else stringResource(R.string.next_label),
                            color = Color(0xFF0D1B2A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
                            tint = Color(0xFF0D1B2A), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    // ── Sender bottom sheet ───────────────────────────────────────────────────
    if (showSheet) {
        SenderSheet(
            category        = sheetCategory,
            selectedSenders = selectedSenders,
            sheetState      = sheetState,
            onSelect        = { id ->
                // selecting removes it from the list — it "disappears" on tap
                selectedSenders = selectedSenders + id
            },
            onDismiss       = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false }
            }
        )
    }
}

// ── Sender sheet — tap to pick, disappears from list ─────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SenderSheet(
    category: String,
    selectedSenders: Set<String>,
    sheetState: SheetState,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    // Only show senders NOT yet selected — they disappear once tapped
    val available = ALL_SENDERS.filter { s ->
        s.category == category && s.id !in selectedSenders &&
                (query.isBlank() || s.displayName.contains(query, ignoreCase = true))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = BgSheet,
        tonalElevation   = 0.dp,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
                Alignment.Center) {
                Box(Modifier.width(32.dp).height(3.dp).clip(CircleShape)
                    .background(TextMuted.copy(.3f)))
            }
        }
    ) {
        Column(
            Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        if (category == "Bank") "Choose your bank" else "Choose mobile money",
                        fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextWhite
                    )
                    Text(
                        if (available.isEmpty() && selectedSenders.isNotEmpty())
                            stringResource(R.string.all_added_check)
                        else "Tap to add · ${available.size} available",
                        fontSize = 12.sp, color = if (available.isEmpty()) GreenOk else TextMuted
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }

            // Search
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_placeholder_ob), color = TextMuted, fontSize = 13.sp) },
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
                    focusedBorderColor      = AccentTeal.copy(.5f),
                    unfocusedBorderColor    = Color.Transparent,
                    focusedTextColor        = TextWhite, unfocusedTextColor = TextWhite,
                    cursorColor             = AccentTeal,
                    focusedContainerColor   = BgRow, unfocusedContainerColor = BgRow
                ),
                shape  = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // List — only unselected items, max 3 visible then scroll
            AnimatedContent(available.isEmpty(), label = "empty") { empty ->
                if (empty) {
                    Box(Modifier.fillMaxWidth().height(80.dp), Alignment.Center) {
                        Text(
                            if (query.isNotBlank()) "No match for \"$query\""
                            else "All ${category.lowercase()}s added!",
                            color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(available, key = { it.id }) { sender ->
                            SenderSheetRow(sender = sender, onTap = {
                                onSelect(sender.id)
                                // auto-close when last item picked
                                if (available.size == 1) onDismiss()
                            })
                        }
                    }
                }
            }
        }
    }
}

// ── Sheet row — own composable (remember safety in LazyColumn) ─────────────────
@Composable
private fun SenderSheetRow(sender: SenderOption, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(BgRow)
            .clickable(remember { MutableInteractionSource() }, null) {
                onTap()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Emoji badge
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                .background(AccentTeal.copy(.08f)),
            Alignment.Center
        ) { Text(sender.emoji, fontSize = 18.sp) }

        Text(
            sender.displayName,
            color = TextSoft, fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Icon(Icons.Default.Add, null, tint = AccentTeal.copy(.7f), modifier = Modifier.size(18.dp))
    }
}

// ── Setup page ────────────────────────────────────────────────────────────────
@Composable
private fun SetupPage(
    userName: String,
    onNameChange: (String) -> Unit,
    selectedSenders: Set<String>,
    openingBalances: Map<String, String>,
    onBalanceChange: (String, String) -> Unit,
    onOpenSheet: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val bankCount   = selectedSenders.count { id -> ALL_SENDERS.any { it.id == id && it.category == "Bank" } }
    val mmCount     = selectedSenders.count { id -> ALL_SENDERS.any { it.id == id && it.category == "Mobile Money" } }
    val filled      = selectedSenders.count { id -> (openingBalances[id]?.replace(",","")?.toDoubleOrNull() ?: 0.0) > 0.0 }
    val allDone     = selectedSenders.isNotEmpty() && filled == selectedSenders.size

    // Step tracker — which step is visually "active"
    val activeStep = when {
        userName.isBlank()        -> 0
        selectedSenders.isEmpty() -> 1
        !allDone                  -> 2
        else                      -> 3
    }

    Box(Modifier.fillMaxSize()) {
        // Subtle radial glow behind content
        Box(Modifier.fillMaxSize().drawBehind {
            drawCircle(
                Brush.radialGradient(
                    listOf(AccentTeal.copy(.07f), Color.Transparent),
                    Offset(size.width * .5f, size.height * .18f),
                    size.width * .95f
                )
            )
        })

        Column(
            Modifier.fillMaxSize().verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ── Header ────────────────────────────────────────────────────────
            val waveT = rememberInfiniteTransition(label = "wt")
            val waveS by waveT.animateFloat(
                1f, 1.15f, infiniteRepeatable(tween(1000, easing = EaseInOut), RepeatMode.Reverse), label = "ws")

            Column(
                Modifier.fillMaxWidth().padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("👋", fontSize = 44.sp, modifier = Modifier.scale(waveS))
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.lets_set_you_up),
                    fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                    color = TextWhite, textAlign = TextAlign.Center,
                    letterSpacing = (-.3).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.just_3_steps),
                    fontSize = 13.sp, color = TextMuted,
                    fontStyle = FontStyle.Normal
                )
                // Step progress pills
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { i ->
                        val done   = i < activeStep
                        val active = i == activeStep
                        val w by animateDpAsState(
                            if (active) 28.dp else if (done) 20.dp else 16.dp,
                            spring(Spring.DampingRatioMediumBouncy), label = "pill$i"
                        )
                        val c by animateColorAsState(
                            when { done -> GreenOk; active -> AccentTeal; else -> BgRow },
                            tween(300), label = "pillc$i"
                        )
                        Box(Modifier.height(4.dp).width(w).clip(CircleShape).background(c))
                    }
                }
            }

            // ── Step 1 — Name ─────────────────────────────────────────────────
            StepCard(
                number  = "01",
                title   = stringResource(R.string.whats_your_name),
                caption = stringResource(R.string.name_caption),
                done    = userName.isNotBlank(),
                active  = activeStep == 0
            ) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value          = userName,
                    onValueChange  = onNameChange,
                    placeholder    = {
                        Text(
                            "e.g. John Masai",
                            color    = TextMuted.copy(.45f),
                            fontSize = 15.sp,
                            fontStyle = FontStyle.Italic
                        )
                    },
                    singleLine     = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    leadingIcon    = {
                        Icon(Icons.Default.Person, null,
                            tint = if (userName.isNotBlank()) AccentTeal else TextMuted.copy(.5f),
                            modifier = Modifier.size(20.dp))
                    },
                    trailingIcon   = {
                        AnimatedVisibility(userName.isNotBlank(), enter = scaleIn(), exit = scaleOut()) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = GreenOk, modifier = Modifier.size(20.dp))
                        }
                    },
                    colors         = stepFieldColors(userName.isNotBlank()),
                    shape          = RoundedCornerShape(14.dp),
                    modifier       = Modifier.fillMaxWidth()
                )
            }

            StepSpacer()

            // ── Step 2 — Services ─────────────────────────────────────────────
            StepCard(
                number  = "02",
                title   = stringResource(R.string.which_services),
                caption = stringResource(R.string.services_caption),
                done    = selectedSenders.isNotEmpty(),
                active  = activeStep == 1
            ) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ServiceTile(
                        emoji    = "🏦",
                        title    = "Banks",
                        subtitle = if (bankCount > 0) "$bankCount selected" else "NMB, CRDB, NBC…",
                        selected = bankCount > 0,
                        modifier = Modifier.weight(1f),
                        onClick  = { onOpenSheet("Bank") }
                    )
                    ServiceTile(
                        emoji    = "📱",
                        title    = "Mobile Money",
                        subtitle = if (mmCount > 0) "$mmCount selected" else "M-Pesa, Tigo…",
                        selected = mmCount > 0,
                        modifier = Modifier.weight(1f),
                        onClick  = { onOpenSheet("Mobile Money") }
                    )
                }
            }

            StepSpacer()

            // ── Step 3 — Balances ─────────────────────────────────────────────
            AnimatedVisibility(
                selectedSenders.isNotEmpty(),
                enter = fadeIn(tween(350)) + expandVertically(tween(400, easing = EaseOut)),
                exit  = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                StepCard(
                    number  = "03",
                    title   = stringResource(R.string.balance_per_service),
                    caption = stringResource(R.string.balance_caption),
                    done    = allDone,
                    active  = activeStep == 2
                ) {
                    Spacer(Modifier.height(4.dp))

                    // Progress bar
                    val prog by animateFloatAsState(
                        if (selectedSenders.isEmpty()) 0f else filled.toFloat() / selectedSenders.size,
                        tween(500), label = "prog"
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.weight(1f).height(3.dp)
                                .clip(CircleShape).background(BgRow)
                        ) {
                            Box(
                                Modifier.fillMaxWidth(prog).fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(AccentTeal, if (allDone) GreenOk else AccentLight)
                                        )
                                    )
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        AnimatedContent(allDone, label = "sc") { done ->
                            Text(
                                if (done) "✓ All set" else "$filled / ${selectedSenders.size}",
                                fontSize = 11.sp,
                                color    = if (done) GreenOk else TextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    selectedSenders.forEach { id ->
                        val sender = ALL_SENDERS.find { it.id == id } ?: return@forEach
                        val value  = openingBalances[id] ?: ""
                        val ok     = (value.replace(",", "").toDoubleOrNull() ?: 0.0) > 0.0
                        BalanceRow(sender = sender, value = value, ok = ok) { raw ->
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

// ── Step card ─────────────────────────────────────────────────────────────────
@Composable
private fun StepCard(
    number: String, title: String, caption: String,
    done: Boolean, active: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderAlpha by animateFloatAsState(
        if (active) .7f else if (done) .4f else .15f, tween(300), label = "ba")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard)
            .drawBehind {
                // left accent stripe
                val stripeW = 3.dp.toPx()
                val color   = (if (done) GreenOk else AccentTeal).copy(borderAlpha)
                drawRoundRect(
                    color        = color,
                    topLeft      = Offset(0f, 16.dp.toPx()),
                    size         = androidx.compose.ui.geometry.Size(stripeW, size.height - 32.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(stripeW / 2)
                )
            }
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Step number / done check
            Box(
                Modifier.size(30.dp).clip(CircleShape)
                    .background(
                        if (done) GreenOk.copy(.15f)
                        else if (active) AccentTeal.copy(.12f)
                        else BgRow
                    ),
                Alignment.Center
            ) {
                AnimatedContent(done, label = "ic$number") { isDone ->
                    if (isDone)
                        Icon(Icons.Default.Check, null, tint = GreenOk, modifier = Modifier.size(14.dp))
                    else
                        Text(number, fontSize = 9.sp, fontWeight = FontWeight.Bold,
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

// ── Step connector line ───────────────────────────────────────────────────────
@Composable
private fun StepSpacer() {
    Box(Modifier.fillMaxWidth().padding(start = 27.dp)) {
        Box(Modifier.width(2.dp).height(16.dp).background(BgRow.copy(.8f)))
    }
}

// ── Service tile ──────────────────────────────────────────────────────────────
@Composable
private fun ServiceTile(
    emoji: String, title: String, subtitle: String,
    selected: Boolean, modifier: Modifier, onClick: () -> Unit
) {
    val bg     by animateColorAsState(if (selected) AccentTeal.copy(.12f) else BgRow, tween(250), label = "stbg")
    val border by animateColorAsState(if (selected) AccentTeal.copy(.5f) else Color.Transparent, tween(250), label = "stbd")
    val scale  by animateFloatAsState(if (selected) 1.02f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "stsc")

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .drawBehind {
                if (selected) {
                    drawRoundRect(
                        color        = border,
                        size         = this.size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        style        = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
                    )
                }
            }
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Emoji in a soft circle
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .background(if (selected) AccentTeal.copy(.15f) else BgCard),
            Alignment.Center
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        Text(title,
            color = if (selected) AccentTeal else TextSoft,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle,
            color = if (selected) GreenOk else TextMuted.copy(.7f),
            fontSize = 10.sp, textAlign = TextAlign.Center,
            maxLines = 1)
    }
}

// ── Balance row ───────────────────────────────────────────────────────────────
@Composable
private fun BalanceRow(
    sender: SenderOption, value: String, ok: Boolean,
    onValueChange: (String) -> Unit
) {
    // Format raw digits into "100,000" style as the user types.
    // `value` stored in state is always the raw digit string ("100000").
    // The field shows the formatted version; trailing separator is stripped on confirm.
    fun formatAmount(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        return try {
            val number = digits.toLong()
            "%,d".format(number)
        } catch (_: NumberFormatException) { digits }
    }

    fun parseRaw(formatted: String): String =
        formatted.filter { it.isDigit() }

    val displayed = formatAmount(value)

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(if (ok) GreenOk.copy(.12f) else BgRow),
            Alignment.Center
        ) { Text(sender.emoji, fontSize = 16.sp) }

        OutlinedTextField(
            value          = displayed,
            onValueChange  = { typed ->
                // Strip everything but digits from what user typed, store raw
                val raw = parseRaw(typed)
                // Guard against absurdly large numbers
                if (raw.length <= 15) onValueChange(raw)
            },
            placeholder    = { Text(stringResource(R.string.balance_placeholder_ob), color = TextMuted.copy(.4f), fontSize = 13.sp) },
            label          = { Text(sender.displayName, fontSize = 10.sp) },
            singleLine     = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            // Show "/=" suffix inside the field when there is a value
            suffix         = {
                if (displayed.isNotEmpty()) {
                    Text("/=", color = if (ok) GreenOk else TextMuted.copy(.5f),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            trailingIcon   = {
                AnimatedVisibility(ok, enter = scaleIn(), exit = scaleOut()) {
                    Icon(Icons.Default.CheckCircle, null,
                        tint = GreenOk, modifier = Modifier.size(17.dp))
                }
            },
            colors  = stepFieldColors(ok),
            shape   = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
private fun stepFieldColors(filled: Boolean = false) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = if (filled) GreenOk else AccentTeal,
    unfocusedBorderColor    = if (filled) GreenOk.copy(.35f) else BgRow,
    focusedLabelColor       = if (filled) GreenOk else AccentTeal,
    unfocusedLabelColor     = TextMuted.copy(.6f),
    focusedTextColor        = TextWhite, unfocusedTextColor = TextWhite,
    cursorColor             = AccentTeal,
    focusedContainerColor   = BgCard, unfocusedContainerColor = BgCard
)


// ── Info page ─────────────────────────────────────────────────────────────────
@Composable
private fun InfoPageContent(page: InfoPage) {
    // Animate in on first composition — key on page so it re-triggers on page change
    var triggered by remember(page) { mutableStateOf(false) }
    LaunchedEffect(page) { triggered = true }
    val alpha     by animateFloatAsState(if (triggered) 1f else 0f, tween(500), label = "a")
    val slideY    by animateFloatAsState(if (triggered) 0f else 40f, tween(500, easing = EaseOut), label = "sy")
    val iconScale by animateFloatAsState(if (triggered) 1f else .6f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "is")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp).padding(top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Big icon circle
        Box(
            Modifier.size(130.dp).scale(iconScale).clip(CircleShape)
                .background(Brush.radialGradient(listOf(page.accent.copy(.18f), page.accent.copy(.04f)))),
            Alignment.Center
        ) {
            Text(page.emoji, fontSize = 52.sp)
        }

        Spacer(Modifier.height(32.dp))

        Text(stringResource(page.titleRes),
            fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
            color = TextWhite, textAlign = TextAlign.Center, lineHeight = 38.sp,
            modifier = Modifier.graphicsLayer { this.alpha = alpha; translationY = slideY })

        Spacer(Modifier.height(8.dp))

        // Pill badge
        Box(Modifier.clip(CircleShape).background(page.accent.copy(.12f))
            .padding(horizontal = 14.dp, vertical = 5.dp)) {
            Text(stringResource(page.subtitleRes), color = page.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(20.dp))

        Text(stringResource(page.descriptionRes), fontSize = 15.sp, color = TextMuted,
            textAlign = TextAlign.Center, lineHeight = 23.sp,
            modifier = Modifier.graphicsLayer { this.alpha = alpha; translationY = slideY })

        Spacer(Modifier.height(36.dp))

        // Feature list
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BgCard).padding(vertical = 4.dp)
        ) {
            page.features.forEachIndexed { i, (icon, labelRes) ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(page.accent.copy(.10f)), Alignment.Center) {
                        Icon(icon, null, tint = page.accent, modifier = Modifier.size(16.dp))
                    }
                    Text(stringResource(labelRes), color = TextSoft, fontSize = 14.sp,
                        fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Check, null, tint = page.accent.copy(.7f), modifier = Modifier.size(14.dp))
                }
                if (i < page.features.size - 1)
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp), .5.dp, TextMuted.copy(.10f))
            }
        }
    }
}