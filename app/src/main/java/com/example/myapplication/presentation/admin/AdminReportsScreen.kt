package com.example.myapplication.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.data.remote.dto.AdminReportDto
import com.example.myapplication.data.remote.dto.AdminReportStatsResponse
import com.example.myapplication.ui.theme.StreakFlame

// Confirm action for reports
private data class ReportConfirmAction(
    val title: String,
    val message: String,
    val isDestructive: Boolean,
    val onConfirm: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(
    modifier: Modifier = Modifier,
    onNavigateToDeck: (String) -> Unit = {},
    viewModel: AdminReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmAction by remember { mutableStateOf<ReportConfirmAction?>(null) }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    // Confirm Dialog
    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(action.title) },
            text = { Text(action.message) },
            confirmButton = {
                Button(
                    onClick = {
                        action.onConfirm()
                        confirmAction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (action.isDestructive) cs.error else cs.primary
                    )
                ) {
                    Text("Xác nhận", color = if (action.isDestructive) cs.onError else cs.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) {
                    Text("Hủy")
                }
            },
            containerColor = cs.surface
        )
    }

    // Filter reports based on selected tab
    val filteredReports = when (uiState.selectedFilter) {
        "Chờ xử lý" -> uiState.reports.filter { it.status == "Pending" }
        "Đã duyệt" -> uiState.reports.filter { it.status == "Approved" }
        "Từ chối" -> uiState.reports.filter { it.status == "Rejected" }
        else -> uiState.reports
    }

    Box(modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Báo cáo & Kiểm duyệt", color = cs.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Quản lý báo cáo vi phạm nội dung từ người dùng.",
                color = cs.onSurfaceVariant, fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))

            when {
                uiState.isLoading && uiState.stats == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = cs.primary)
                    }
                }
                uiState.error != null && uiState.stats == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.error!!, color = cs.onSurfaceVariant, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Chưa có báo cáo nào.", color = cs.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // ── Stats Header ──
                        uiState.stats?.let { stats ->
                            item { ReportStatsDashboard(stats) }
                        }

                        // ── Filter Chips ──
                        item { ReportFilterRow(uiState.selectedFilter) { viewModel.setFilter(it) } }

                        // ── Report List ──
                        if (filteredReports.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🎉", fontSize = 40.sp)
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            "Không có báo cáo cần xử lý",
                                            color = cs.onSurfaceVariant, fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredReports, key = { it.id }) { report ->
                                ReportRow(
                                    report = report,
                                    onViewDeck = { onNavigateToDeck(report.targetId) },
                                    onApprove = {
                                        confirmAction = ReportConfirmAction(
                                            title = "Duyệt báo cáo?",
                                            message = "Nội dung vi phạm sẽ bị xóa. Hành động này không thể hoàn tác.",
                                            isDestructive = true,
                                            onConfirm = { viewModel.handleReport(report.id, "approve") }
                                        )
                                    },
                                    onReject = {
                                        confirmAction = ReportConfirmAction(
                                            title = "Từ chối báo cáo?",
                                            message = "Xác nhận nội dung không vi phạm.",
                                            isDestructive = false,
                                            onConfirm = { viewModel.handleReport(report.id, "reject") }
                                        )
                                    }
                                )
                            }
                        }

                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
        } // PullToRefreshBox

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// ── Stats Dashboard ──

@Composable
private fun ReportStatsDashboard(stats: AdminReportStatsResponse) {
    val cs = MaterialTheme.colorScheme

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ReportKpiCard(
            value = "${stats.pendingCount}",
            label = "Chờ xử lý",
            icon = Icons.Default.HourglassEmpty,
            iconTint = StreakFlame,
            modifier = Modifier.weight(1f)
        )
        ReportKpiCard(
            value = "${stats.approvedCount}",
            label = "Đã duyệt",
            icon = Icons.Default.CheckCircle,
            iconTint = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        ReportKpiCard(
            value = "${stats.rejectedCount}",
            label = "Từ chối",
            icon = Icons.Default.Cancel,
            iconTint = cs.error,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReportKpiCard(
    value: String,
    label: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                value, color = cs.onSurface,
                fontSize = 22.sp, fontWeight = FontWeight.Bold
            )
            Text(label, color = cs.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

// ── Filter Chips ──

@Composable
private fun ReportFilterRow(selected: String, onSelect: (String) -> Unit) {
    val filters = listOf("Tất cả", "Chờ xử lý", "Đã duyệt", "Từ chối")
    val cs = MaterialTheme.colorScheme

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(filter, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = cs.primary,
                    selectedLabelColor = cs.onPrimary
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// ── Report Row ──

@Composable
private fun ReportRow(
    report: AdminReportDto,
    onViewDeck: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isPending = report.status == "Pending"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape)
                        .background(
                            if (isPending) StreakFlame.copy(alpha = 0.12f)
                            else cs.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPending) Icons.Default.Warning else Icons.Default.BugReport,
                        contentDescription = null,
                        tint = if (isPending) StreakFlame else cs.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(report.reason, color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 2)
                    Text("Người báo: ${report.reportedByEmail}", color = cs.onSurfaceVariant, fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                val statusColor = when (report.status) {
                    "Pending" -> StreakFlame
                    "Rejected" -> cs.onSurfaceVariant
                    else -> Color(0xFF4CAF50)
                }
                val statusText = when (report.status) {
                    "Pending" -> "Chờ duyệt"
                    "Approved" -> "Đã duyệt"
                    "Rejected" -> "Từ chối"
                    else -> report.status
                }
                Text(
                    statusText,
                    color = statusColor,
                    fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${report.targetType} • ${report.createdAt.take(10)}", color = cs.onSurfaceVariant, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // "Xem nội dung" button (always visible)
                    if (report.targetType == "Deck") {
                        Button(
                            onClick = onViewDeck,
                            colors = ButtonDefaults.buttonColors(containerColor = cs.primaryContainer),
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp), tint = cs.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("Xem", color = cs.primary, fontSize = 12.sp)
                        }
                    }
                    if (isPending) {
                        Button(
                            onClick = onReject,
                            colors = ButtonDefaults.buttonColors(containerColor = cs.surfaceVariant),
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Bỏ qua", color = cs.onSurface, fontSize = 12.sp)
                        }
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = cs.error),
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Duyệt & Xóa", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
