package com.smsfinance.ui.export

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsfinance.ui.theme.GreenPrimary
import com.smsfinance.viewmodel.ExportViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExportScreen(
    viewModel: ExportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var selectedRange by remember { mutableStateOf("This Month") }

    // Set default date range to current month
    LaunchedEffect(Unit) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        startDate = cal.timeInMillis
        endDate = System.currentTimeMillis()
    }

    // Share launcher
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    LaunchedEffect(uiState.shareIntent) {
        uiState.shareIntent?.let {
            shareLauncher.launch(it)
            viewModel.clearShareIntent()
        }
    }

    Scaffold(
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        Text("Export Data", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text("Save as Excel or PDF", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Stats card ────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    Modifier.fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TableChart, null, tint = Color.White,
                            modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("${uiState.transactionCount} transactions",
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("available for export", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── Date range picker ─────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Date Range", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(10.dp))

                    // Quick range chips
                    val ranges = listOf("This Month", "Last Month", "Last 3 Months", "All Time")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ranges.take(2).forEach { range ->
                            FilterChip(
                                selected = selectedRange == range,
                                onClick = {
                                    selectedRange = range
                                    val r = getDateRange(range)
                                    startDate = r.first; endDate = r.second
                                },
                                label = { Text(range, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ranges.drop(2).forEach { range ->
                            FilterChip(
                                selected = selectedRange == range,
                                onClick = {
                                    selectedRange = range
                                    val r = getDateRange(range)
                                    startDate = r.first; endDate = r.second
                                },
                                label = { Text(range, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Custom date pickers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                startDate?.let { cal.timeInMillis = it }
                                DatePickerDialog(context, { _, y, m, d ->
                                    cal.set(y, m, d, 0, 0, 0)
                                    startDate = cal.timeInMillis
                                    selectedRange = "Custom"
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarToday, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(startDate?.let { dateFormat.format(Date(it)) } ?: "Start", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                endDate?.let { cal.timeInMillis = it }
                                DatePickerDialog(context, { _, y, m, d ->
                                    cal.set(y, m, d, 23, 59, 59)
                                    endDate = cal.timeInMillis
                                    selectedRange = "Custom"
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarMonth, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(endDate?.let { dateFormat.format(Date(it)) } ?: "End", fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Export buttons ────────────────────────────────────────────────
            Text("Choose Format", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            ExportButton(
                icon = Icons.Default.TableChart,
                title = "Export to Excel (.xlsx)",
                subtitle = "Open in Google Sheets, Microsoft Excel",
                color = Color(0xFF1B5E20),
                isLoading = uiState.isExporting,
                onClick = { viewModel.exportToExcel(startDate, endDate) }
            )

            ExportButton(
                icon = Icons.Default.PictureAsPdf,
                title = "Export to PDF",
                subtitle = "Print-ready report with summary and table",
                color = Color(0xFFB71C1C),
                isLoading = uiState.isExporting,
                onClick = { viewModel.exportToPdf(startDate, endDate) }
            )

            // Error
            uiState.error?.let {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                    }
                }
            }

            // Info
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(12.dp)) {
                    Icon(Icons.Default.Info, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Exported files will be shared via your phone's share menu. " +
                                "You can send via WhatsApp, Gmail, save to Drive, or download locally.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun ExportButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(72.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

private fun getDateRange(range: String): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    val end = cal.timeInMillis
    return when (range) {
        "This Month" -> {
            cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
            Pair(cal.timeInMillis, end)
        }
        "Last Month" -> {
            cal.add(Calendar.MONTH, -1)
            val lastEnd = cal.apply { set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            Pair(cal.timeInMillis, lastEnd)
        }
        "Last 3 Months" -> {
            cal.add(Calendar.MONTH, -3); cal.set(Calendar.DAY_OF_MONTH, 1)
            Pair(cal.timeInMillis, end)
        }
        else -> Pair(0L, end)
    }
}