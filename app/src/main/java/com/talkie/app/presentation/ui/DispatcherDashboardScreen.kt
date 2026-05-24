package com.talkie.app.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talkie.app.data.local.TransmissionLogEntity
import com.talkie.app.presentation.AdminViewModel
import com.talkie.app.presentation.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatcherDashboardScreen(
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel = viewModel()
) {
    val logs by adminViewModel.logs.collectAsState()
    val totalTransmissions by adminViewModel.totalTransmissions.collectAsState()
    val totalAirtime by adminViewModel.totalAirtime.collectAsState()
    val totalFlaggedIncidents by adminViewModel.totalFlaggedIncidents.collectAsState()
    val reportPath by adminViewModel.reportPath.collectAsState()

    var showReportDialog by remember { mutableStateOf(false) }

    val primaryBg = Color(0xFFF4F6FA)
    val navyBlue = Color(0xFF001A49)
    val accentEmerald = Color(0xFF0D9488)
    val accentCoral = Color(0xFFE11D48)

    LaunchedEffect(reportPath) {
        if (reportPath != null) {
            showReportDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryBg)
            .padding(16.dp)
    ) {
        // ── Header Section ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DISPATCH CONTROLLER",
                    fontFamily = Montserrat,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = navyBlue
                )
                Text(
                    text = "Supervisor traffic & channel matrix",
                    fontFamily = LeagueSpartan,
                    fontWeight = FontWeight.Light,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            IconButton(
                onClick = { authViewModel.logout() },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentCoral.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = accentCoral
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Aggregated Metrics Dashboard ─────────────────────────────────────
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("NETWORK TELEMETRY", fontFamily = Montserrat, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = navyBlue)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricBox(title = "Total Transmissions", value = "$totalTransmissions", color = navyBlue)
                    MetricBox(title = "Total Airtime", value = "${totalAirtime ?: 0}s", color = accentEmerald)
                    MetricBox(title = "Flagged Incidents", value = "$totalFlaggedIncidents", color = accentCoral)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { adminViewModel.generateReport() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Audit PDF", fontFamily = LeagueSpartan, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("TRANSMISSION LEDGER", fontFamily = Montserrat, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = navyBlue)
        Spacer(modifier = Modifier.height(12.dp))

        // ── Ledger Scroll ────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(logs) { log ->
                LedgerItem(log)
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = {
                showReportDialog = false
            },
            title = { Text("Report Generated", fontFamily = Montserrat, fontWeight = FontWeight.Bold) },
            text = { Text("PDF saved to:\n$reportPath", fontFamily = LeagueSpartan) },
            confirmButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("OK", color = navyBlue)
                }
            }
        )
    }
}

@Composable
fun MetricBox(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontFamily = Montserrat, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = title, fontFamily = LeagueSpartan, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
fun LedgerItem(log: TransmissionLogEntity) {
    val formatter = remember { SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault()) }
    val timeString = formatter.format(Date(log.timestamp))
    
    val containerColor = if (log.isIncidentFlagged) Color(0xFFFEF2F2) else Color.White
    val borderColor = if (log.isIncidentFlagged) Color(0xFFEF4444) else Color(0xFFE5E7EB)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = log.workerName, fontFamily = Montserrat, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF111827))
                Text(text = timeString, fontFamily = LeagueSpartan, fontSize = 12.sp, color = Color(0xFF6B7280))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Radio, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF6B7280))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = log.channelName, fontFamily = LeagueSpartan, fontSize = 14.sp, color = Color(0xFF374151))
                }
                Text(text = "${log.durationSeconds}s", fontFamily = Montserrat, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF0D9488))
            }

            if (log.isIncidentFlagged) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFEF4444), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text(text = "[ALERT: STUCK MIC / EXCESSIVE AIRTIME]", fontFamily = LeagueSpartan, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}
