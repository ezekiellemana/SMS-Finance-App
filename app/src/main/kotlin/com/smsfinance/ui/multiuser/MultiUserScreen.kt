package com.smsfinance.ui.multiuser

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.smsfinance.domain.model.DEFAULT_AVATARS
import com.smsfinance.domain.model.PROFILE_COLORS
import com.smsfinance.domain.model.UserProfile
import com.smsfinance.ui.components.BigFab
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.MultiUserViewModel
import java.io.File

@Suppress("DEPRECATION")
@Composable
fun MultiUserScreen(
    viewModel: MultiUserViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onProfileSwitched: () -> Unit
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog  by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<UserProfile?>(null) }

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextWhite)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Family Accounts", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextWhite)
                    Text("Manage profiles & settings", fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(Modifier.width(48.dp))
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = AccentTeal, strokeWidth = 3.dp)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Active profile hero card
                    uiState.activeProfile?.let { active ->
                        item { ActiveProfileHeroCard(active) }
                    }

                    // Section header
                    item {
                        Text(
                            "All Members",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Profile cards
                    items(uiState.profiles, key = { it.id }) { profile ->
                        ProfileCard(
                            profile   = profile,
                            isActive  = profile.id == uiState.activeProfile?.id,
                            onSwitch  = { viewModel.switchProfile(profile); onProfileSwitched() },
                            onEdit    = { editingProfile = profile },
                            onDelete  = { viewModel.deleteProfile(profile) },
                            canDelete = uiState.profiles.size > 1
                        )
                    }
                }
            }

            // Add member FAB
            BigFab(
                onClick     = { showAddDialog = true },
                icon        = Icons.Default.PersonAdd,
                label       = "Add Member",
                accentColor = AccentTeal,
                modifier    = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }

    if (showAddDialog || editingProfile != null) {
        ProfileDialog(
            existing  = editingProfile,
            onSave    = { profile ->
                if (editingProfile != null) viewModel.updateProfile(profile)
                else viewModel.addProfile(profile)
                showAddDialog  = false
                editingProfile = null
            },
            onDismiss = { showAddDialog = false; editingProfile = null }
        )
    }
}

// ── Active profile hero card ──────────────────────────────────────────────────
@Composable
fun ActiveProfileHeroCard(profile: UserProfile) {
    val accentColor = runCatching { Color(android.graphics.Color.parseColor(profile.color)) }
        .getOrElse { AccentTeal }

    val pulse = rememberInfiniteTransition(label = "heroGlow")
    val glow  by pulse.animateFloat(.12f, .28f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "hg")

    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1A2D40), Color(0xFF1C3035))))
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(accentColor.copy(glow), Color.Transparent),
                        Offset(size.width * .85f, size.height * .2f), size.width * .55f
                    )
                )
            }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            // Avatar
            ProfileAvatar(profile = profile, size = 70.dp, accentColor = accentColor, borderWidth = 3.dp)

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextWhite)
                    Surface(color = accentColor, shape = RoundedCornerShape(8.dp)) {
                        Text("Active", color = Color.White, fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                    }
                }
                Text("Currently active profile",
                    fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

// ── Profile card (list item) ──────────────────────────────────────────────────
@Composable
fun ProfileCard(
    profile: UserProfile,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val accentColor = runCatching { Color(android.graphics.Color.parseColor(profile.color)) }
        .getOrElse { AccentTeal }
    val bg by animateColorAsState(
        if (isActive) accentColor.copy(.10f) else Color(0xFF1C2740),
        tween(300), label = "bg"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(if (!isActive) Modifier.clickable { onSwitch() } else Modifier)
            .then(if (isActive) Modifier.border(1.5.dp, accentColor.copy(.4f), RoundedCornerShape(16.dp)) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(profile = profile, size = 52.dp, accentColor = accentColor, borderWidth = 2.dp)

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(profile.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextWhite)
            Text(
                if (isActive) "✓ Active" else "Tap to switch",
                fontSize = 11.sp,
                color = if (isActive) accentColor else TextSecondary
            )
        }

        if (!isActive) {
            IconButton(onClick = onSwitch) {
                Icon(Icons.Default.SwitchAccount, null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
        if (canDelete && !isActive) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = ErrorRed.copy(.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Shared profile avatar — shows photo if set, else emoji ───────────────────
@Composable
fun ProfileAvatar(
    profile: UserProfile,
    size: androidx.compose.ui.unit.Dp,
    accentColor: Color,
    borderWidth: androidx.compose.ui.unit.Dp
) {
    Box(
        Modifier.size(size).clip(CircleShape)
            .border(borderWidth, accentColor.copy(.5f), CircleShape)
            .background(accentColor.copy(.18f)),
        Alignment.Center
    ) {
        if (!profile.photoUri.isNullOrBlank()) {
            AsyncImage(
                model          = profile.photoUri,
                contentDescription = profile.name,
                contentScale   = ContentScale.Crop,
                modifier       = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Text(profile.avatarEmoji, fontSize = (size.value * 0.38f).sp)
        }
    }
}

// ── Profile dialog (add / edit) ───────────────────────────────────────────────
@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@Composable
fun ProfileDialog(
    existing: UserProfile?,
    onSave: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var name          by remember { mutableStateOf(existing?.name ?: "") }
    var selectedEmoji by remember { mutableStateOf(existing?.avatarEmoji ?: "👤") }
    var selectedColor by remember { mutableStateOf(existing?.color ?: "#00C853") }
    var photoUri      by remember { mutableStateOf<String?>(existing?.photoUri) }
    var nameError     by remember { mutableStateOf(false) }

    // Photo picker launcher — copies the chosen image into app-private files
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            // Copy into app-private storage so we keep access after the pick
            val profileId = existing?.id ?: System.currentTimeMillis()
            val dest = File(context.filesDir, "profile_photo_$profileId.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            photoUri = dest.absolutePath
        } catch (_: Exception) {
            photoUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF1C2740),
        shape            = RoundedCornerShape(20.dp),
        title = {
            Text(
                if (existing == null) "Add Family Member" else "Edit Profile",
                fontWeight = FontWeight.Bold, color = TextWhite
            )
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── Photo section ─────────────────────────────────────────────
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Current avatar / photo preview
                    val accentC = runCatching {
                        Color(android.graphics.Color.parseColor(selectedColor))
                    }.getOrElse { AccentTeal }

                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            Modifier.size(88.dp).clip(CircleShape)
                                .border(2.5.dp, accentC.copy(.5f), CircleShape)
                                .background(accentC.copy(.15f)),
                            Alignment.Center
                        ) {
                            if (!photoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model              = photoUri,
                                    contentDescription = null,
                                    contentScale       = ContentScale.Crop,
                                    modifier           = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(selectedEmoji, fontSize = 34.sp)
                            }
                        }
                        // Camera overlay badge
                        Box(
                            Modifier.size(28.dp).clip(CircleShape)
                                .background(accentC)
                                .clickable { photoPicker.launch("image/*") },
                            Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, null,
                                tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Pick from gallery
                        OutlinedButton(
                            onClick = { photoPicker.launch("image/*") },
                            shape   = RoundedCornerShape(10.dp),
                            colors  = ButtonDefaults.outlinedButtonColors(contentColor = AccentTeal),
                            border  = androidx.compose.foundation.BorderStroke(1.dp, AccentTeal.copy(.4f))
                        ) {
                            Icon(Icons.Default.Photo, null, Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Gallery", fontSize = 12.sp)
                        }
                        // Remove photo
                        if (!photoUri.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = { photoUri = null },
                                shape   = RoundedCornerShape(10.dp),
                                colors  = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                border  = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(.4f))
                            ) {
                                Icon(Icons.Default.Delete, null, Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Remove", fontSize = 12.sp)
                            }
                        }
                    }
                }

                HorizontalDivider(color = TextSecondary.copy(.15f))

                // ── Name ─────────────────────────────────────────────────────
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it; nameError = false },
                    label         = { Text("Full Name") },
                    placeholder   = { Text("e.g. Mama, Baba, John") },
                    isError       = nameError,
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AccentTeal,
                        focusedLabelColor    = AccentTeal,
                        unfocusedTextColor   = TextWhite,
                        focusedTextColor     = TextWhite,
                        unfocusedLabelColor  = TextSecondary,
                        unfocusedBorderColor = TextSecondary.copy(.4f)
                    )
                )

                // ── Emoji avatar picker ───────────────────────────────────────
                if (photoUri.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Avatar Emoji", style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(DEFAULT_AVATARS) { emoji ->
                                Box(
                                    Modifier.size(44.dp).clip(CircleShape)
                                        .background(
                                            if (selectedEmoji == emoji) AccentTeal.copy(.2f)
                                            else Color(0xFF243050)
                                        )
                                        .border(
                                            if (selectedEmoji == emoji) 2.dp else 0.dp,
                                            if (selectedEmoji == emoji) AccentTeal else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { selectedEmoji = emoji },
                                    Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 22.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }

                // ── Color picker ──────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Profile Color", style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(PROFILE_COLORS) { hex ->
                            val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                                .getOrElse { AccentTeal }
                            Box(
                                Modifier.size(34.dp).shadow(
                                    if (selectedColor == hex) 6.dp else 0.dp, CircleShape
                                ).clip(CircleShape).background(c)
                                    .then(
                                        if (selectedColor == hex)
                                            Modifier.border(3.dp, Color.White, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { selectedColor = hex }
                            ) {
                                if (selectedColor == hex) {
                                    Icon(Icons.Default.Check, null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.Center).size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    nameError = name.isBlank()
                    if (!nameError) {
                        onSave(
                            UserProfile(
                                id           = existing?.id ?: 0L,
                                name         = name.trim(),
                                avatarEmoji  = selectedEmoji,
                                color        = selectedColor,
                                isActive     = existing?.isActive ?: false,
                                pinHash      = existing?.pinHash,
                                createdAt    = existing?.createdAt ?: System.currentTimeMillis(),
                                photoUri     = photoUri
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
            ) { Text("Save", color = Color(0xFF0A1628), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}