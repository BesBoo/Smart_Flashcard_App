package com.example.myapplication.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.ui.theme.StreakFlame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
    when {
        uiState.isLoading && uiState.stats == null -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = cs.primary)
            }
        }
        uiState.error != null && uiState.stats == null -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.error!!, color = cs.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Kiểm tra kết nối API", color = cs.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
        else -> {
            val stats = uiState.stats
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ─── Header ───
                item {
                    Text("Tổng quan", color = cs.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("Smart Flashcard Admin", color = cs.onSurfaceVariant, fontSize = 14.sp)
                }

                // ═══════════════════════════════════════
                // TRÊN: KPI – 4 số to, nhìn là hiểu ngay
                // ═══════════════════════════════════════
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard(
                            value = "${stats?.totalUsers ?: 0}",
                            label = "Người dùng",
                            icon = Icons.Default.People,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            value = "${stats?.totalDecks ?: 0}",
                            label = "Bộ thẻ",
                            icon = Icons.Default.LibraryBooks,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard(
                            value = "${stats?.totalFlashcards ?: 0}",
                            label = "Thẻ ghi nhớ",
                            icon = Icons.Default.FlashOn,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            value = "${stats?.totalReviews ?: 0}",
                            label = "Lượt ôn tập",
                            icon = Icons.Default.RateReview,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ═══════════════════════════════════════
                // GIỮA: Tỷ lệ & Trend
                // ═══════════════════════════════════════
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Chỉ số hoạt động", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Spacer(Modifier.height(16.dp))

                            // Active rate bar
                            val activeRate = uiState.activeRate
                            MetricBar(
                                label = "Tỷ lệ Active",
                                value = "${(activeRate * 100).toInt()}%",
                                fraction = activeRate,
                                color = cs.primary
                            )
                            Spacer(Modifier.height(14.dp))

                            // Decks per user
                            val u = stats?.totalUsers ?: 1
                            val decksPerUser = if (u > 0) (stats?.totalDecks ?: 0).toFloat() / u else 0f
                            MetricBar(
                                label = "Deck / User",
                                value = String.format("%.1f", decksPerUser),
                                fraction = (decksPerUser / 5f).coerceIn(0f, 1f),
                                color = cs.secondary
                            )
                            Spacer(Modifier.height(14.dp))

                            // Reviews per card
                            val c = (stats?.totalFlashcards ?: 1).coerceAtLeast(1)
                            val reviewsPerCard = (stats?.totalReviews ?: 0).toFloat() / c
                            MetricBar(
                                label = "Review / Card",
                                value = String.format("%.1f", reviewsPerCard),
                                fraction = (reviewsPerCard / 10f).coerceIn(0f, 1f),
                                color = cs.tertiary
                            )
                        }
                    }
                }

                // ═══════════════════════════════════════
                // DƯỚI: Anomaly + Chi tiết
                // ═══════════════════════════════════════

                // Anomaly alert — only show when pending > 0
                if (uiState.pendingReportsCount > 0) {
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = StreakFlame.copy(alpha = 0.1f))
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape)
                                        .background(StreakFlame.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Warning, null, tint = StreakFlame, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        "${uiState.pendingReportsCount} báo cáo chờ duyệt",
                                        color = StreakFlame,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "Cần xử lý ngay tại mục Báo cáo",
                                        color = cs.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Insight card
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Gợi ý theo dữ liệu", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Spacer(Modifier.height(12.dp))

                            val totalUsers = stats?.totalUsers ?: 0
                            val totalDecks = stats?.totalDecks ?: 0
                            val totalCards = stats?.totalFlashcards ?: 0
                            val totalReviews = stats?.totalReviews ?: 0

                            val hints = buildList {
                                if (totalUsers > 0 && totalDecks.toDouble() / totalUsers < 0.5) {
                                    add("📌 Trung bình mỗi user có ít bộ thẻ — nên hướng dẫn tạo deck đầu tiên.")
                                }
                                if (totalCards > 0 && totalReviews.toDouble() / totalCards < 2) {
                                    add("📌 Tỷ lệ ôn tập/thẻ thấp — nhắc người dùng ôn tập định kỳ.")
                                }
                                if (uiState.activeRate < 0.7f && totalUsers > 5) {
                                    add("⚠️ ${((1 - uiState.activeRate) * 100).toInt()}% users không active — kiểm tra lý do churn.")
                                }
                            }.ifEmpty {
                                listOf("✅ Dữ liệu đang phát triển tốt. Hệ thống hoạt động bình thường.")
                            }

                            hints.forEach { line ->
                                Text(line, color = cs.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp)
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }

                // Summary numbers
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Chi tiết", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Spacer(Modifier.height(12.dp))
                            DetailRow("Users active", "${stats?.activeUsers ?: 0} / ${stats?.totalUsers ?: 0}")
                            DetailRow("Premium users", "${stats?.premiumUsers ?: 0}")
                            DetailRow("Báo cáo chờ duyệt", "${uiState.pendingReportsCount}")
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
    } // PullToRefreshBox
}

// ─── KPI Card: Số to → Label nhỏ ───

@Composable
private fun KpiCard(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = cs.onSurfaceVariant, fontSize = 12.sp)
                Icon(icon, null, tint = cs.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatNumber(value),
                color = cs.onSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )
        }
    }
}

// ─── Metric Bar ───

@Composable
private fun MetricBar(
    label: String,
    value: String,
    fraction: Float,
    color: androidx.compose.ui.graphics.Color
) {
    val cs = MaterialTheme.colorScheme
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = cs.onSurfaceVariant, fontSize = 13.sp)
            Text(value, color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                .background(cs.outlineVariant.copy(alpha = 0.3f))
        ) {
            Box(
                Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp)
                    .clip(CircleShape).background(color)
            )
        }
    }
}

// ─── Detail Row ───

@Composable
private fun DetailRow(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = cs.onSurfaceVariant, fontSize = 14.sp)
        Text(value, color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// ─── Number formatter ───

private fun formatNumber(raw: String): String {
    val n = raw.toLongOrNull() ?: return raw
    return when {
        n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
        n >= 10_000 -> String.format("%.1fK", n / 1_000.0)
        n >= 1_000 -> String.format("%,d", n)
        else -> raw
    }
}
