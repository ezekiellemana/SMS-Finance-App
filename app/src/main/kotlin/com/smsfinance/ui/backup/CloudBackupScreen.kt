package com.smsfinance.ui.backup

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.smsfinance.ui.theme.*
import com.smsfinance.viewmodel.CloudBackupViewModel

@Composable
fun CloudBackupScreen(
    viewModel: CloudBackupViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var signInIntent by remember { mutableStateOf<Intent?>(null) }

    // Build sign-in intent when needed
    LaunchedEffect(Unit) {
        signInIntent = viewModel.buildSignInIntent()
    }

    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            task.getResult(ApiException::class.java)
            viewModel.refreshSignInState()
        } catch (e: ApiException) { /* cancelled or failed */ }
    }

    Scaffold(
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                    Column {
                        Text("Cloud Backup", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                            Text("Google Drive — your data, your account", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Hero card ─────────────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                Box(Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF1565C0), Color(0xFF0D47A1))))
                    .padding(20.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Cloud, null, tint = Color.White, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Google Drive Backup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            if (uiState.isSignedIn) "Signed in as ${uiState.signedInEmail}"
                            else "Opt-in backup to your personal Google Drive",
                            color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── Privacy notice ────────────────────────────────────────────────
            Card(colors = CardDefaults.cardColors(containerColor = AccentTeal.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Shield, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("🔒 Privacy First — Your data goes directly to your own Google Drive " +
                        "app folder. It is never stored on our servers.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }

            if (!uiState.isSignedIn) {
                // ── Sign in button ────────────────────────────────────────────
                Button(
                    onClick = { signInIntent?.let { signInLauncher.launch(it) } },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                ) {
                    Icon(Icons.Default.AccountCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign in with Google", fontWeight = FontWeight.Bold)
                }
            } else {
                // ── Backup status card ────────────────────────────────────────
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Backup Status", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        HorizontalDivider()
                        BackupInfoRow(Icons.Default.Storage, "Transactions",
                            "${uiState.transactionCount} ready to backup")
                        BackupInfoRow(Icons.Default.Schedule, "Last Backup",
                            if (uiState.isLoadingInfo) "Loading…" else uiState.lastBackupDate ?: "Never")
                        uiState.lastBackupSize?.let {
                            BackupInfoRow(Icons.Default.FolderOpen, "Backup Size", it)
                        }
                    }
                }

                // ── Action buttons ────────────────────────────────────────────
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.backup() },
                        enabled = !uiState.isBackingUp && !uiState.isRestoring,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) {
                        if (uiState.isBackingUp) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudUpload, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Backup Now", fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(
                        onClick = { showRestoreConfirm = true },
                        enabled = !uiState.isBackingUp && !uiState.isRestoring,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (uiState.isRestoring) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudDownload, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Restore", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                TextButton(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("Sign Out from Google", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }

            // ── Messages ──────────────────────────────────────────────────────
            uiState.successMessage?.let { msg ->
                Card(colors = CardDefaults.cardColors(containerColor = AccentTeal.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentTeal)
                        Spacer(Modifier.width(8.dp))
                        Text(msg, color = AccentTeal, fontWeight = FontWeight.Medium)
                    }
                }
            }
            uiState.errorMessage?.let { err ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                    }
                }
            }

            // ── How it works ──────────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("How it works", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "1️⃣  Sign in with your Google account",
                        "2️⃣  Tap 'Backup Now' to save all transactions",
                        "3️⃣  Data is stored in your Drive's private app folder",
                        "4️⃣  On a new phone, sign in and tap 'Restore'",
                        "5️⃣  All your transactions are back instantly"
                    ).forEach { step ->
                        Text(step, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                    }
                }
            }
        }
    }

    // Restore confirmation dialog
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            icon = { Icon(Icons.Default.CloudDownload, null) },
            title = { Text("Restore from Backup?") },
            text = { Text("This will import transactions from your Google Drive backup. " +
                "Existing transactions will not be deleted — restored transactions will be merged.") },
            confirmButton = {
                Button(onClick = { viewModel.restore(); showRestoreConfirm = false }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun BackupInfoRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
