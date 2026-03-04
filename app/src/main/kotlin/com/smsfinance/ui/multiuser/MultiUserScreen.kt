package com.smsfinance.ui.multiuser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.domain.model.DEFAULT_AVATARS
import com.smsfinance.domain.model.PROFILE_COLORS
import com.smsfinance.domain.model.UserProfile
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.MultiUserViewModel

@Composable
fun MultiUserScreen(
    viewModel: MultiUserViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onProfileSwitched: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<UserProfile?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentTeal
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add profile", tint = BgPrimary)
            }
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentTeal)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Text("Family Accounts", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                            Text("Switch between family members", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            }
            // Active profile hero
            uiState.activeProfile?.let { active ->
                item {
                    ActiveProfileCard(profile = active)
                }
            }

            // Section header
            item {
                Text(
                    "All Profiles",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Profile list
            items(uiState.profiles, key = { it.id }) { profile ->
                ProfileCard(
                    profile = profile,
                    isActive = profile.id == uiState.activeProfile?.id,
                    onSwitch = {
                        viewModel.switchProfile(profile)
                        onProfileSwitched()
                    },
                    onEdit = { editingProfile = profile },
                    onDelete = {
                        if (uiState.profiles.size > 1) viewModel.deleteProfile(profile)
                    },
                    canDelete = uiState.profiles.size > 1
                )
            }

            // Info card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Each family member has their own transactions, " +
                            "budget plans, and spending alerts. All data stays on this device.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    // Add / Edit dialog
    if (showAddDialog || editingProfile != null) {
        ProfileDialog(
            existing = editingProfile,
            onSave = { profile ->
                if (editingProfile != null) viewModel.updateProfile(profile)
                else viewModel.addProfile(profile)
                showAddDialog = false
                editingProfile = null
            },
            onDismiss = { showAddDialog = false; editingProfile = null }
        )
    }
}

@Composable
fun ActiveProfileCard(profile: UserProfile) {
    val bg = runCatching { Color(android.graphics.Color.parseColor(profile.color)) }
        .getOrDefault(AccentTeal)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg.copy(alpha = 0.15f))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(bg.copy(alpha = 0.3f))
                        .border(3.dp, bg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(profile.avatarEmoji, fontSize = 28.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            profile.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = bg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Active",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        "Currently viewing this profile's data",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileCard(
    profile: UserProfile,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val bg = runCatching { Color(android.graphics.Color.parseColor(profile.color)) }
        .getOrDefault(AccentTeal)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isActive) Modifier.clickable { onSwitch() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, bg) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(profile.avatarEmoji, fontSize = 22.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, fontWeight = FontWeight.Bold)
                Text(
                    if (isActive) "✓ Active profile" else "Tap to switch",
                    fontSize = 12.sp,
                    color = if (isActive) bg else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Actions
            Row {
                if (!isActive) {
                    IconButton(onClick = onSwitch) {
                        Icon(
                            Icons.Default.SwitchAccount,
                            contentDescription = "Switch",
                            tint = bg
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit",
                        modifier = Modifier.size(20.dp))
                }
                if (canDelete && !isActive) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDialog(
    existing: UserProfile?,
    onSave: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var selectedEmoji by remember { mutableStateOf(existing?.avatarEmoji ?: "👤") }
    var selectedColor by remember { mutableStateOf(existing?.color ?: "#00C853") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Family Member" else "Edit Profile",
            fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Mama, Baba, John") },
                    isError = nameError,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Avatar picker
                Text("Choose Avatar", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DEFAULT_AVATARS) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedEmoji == emoji)
                                        AccentTeal.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = if (selectedEmoji == emoji) 2.dp else 0.dp,
                                    color = if (selectedEmoji == emoji) AccentTeal else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 22.sp, textAlign = TextAlign.Center)
                        }
                    }
                }

                // Color picker
                Text("Choose Color", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PROFILE_COLORS) { hex ->
                        val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                            .getOrDefault(AccentTeal)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (selectedColor == hex) Modifier.border(3.dp, Color.White, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                nameError = name.isBlank()
                if (!nameError) {
                    onSave(
                        UserProfile(
                            id = existing?.id ?: 0L,
                            name = name.trim(),
                            avatarEmoji = selectedEmoji,
                            color = selectedColor,
                            isActive = existing?.isActive ?: false,
                            pinHash = existing?.pinHash,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
