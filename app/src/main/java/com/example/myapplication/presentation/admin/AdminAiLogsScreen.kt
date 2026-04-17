package com.example.myapplication.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.data.remote.dto.AdminAiLogDto
import com.example.myapplication.data.remote.dto.AdminAiStatsResponse
import com.example.myapplication.ui.theme.StreakFlame

@Composable
fun AdminAiLogsScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminAiLogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme

    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("Quản lý AI", color = cs.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Giám sát lưu lượng API, model, provider và trạng thái.",
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
                        Text("Chưa có dữ liệu AI.", color = cs.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // ── Stats Dashboard ──
                    uiState.stats?.let { stats ->
                        item { AiStatsDashboard(stats) }
                        item { ProviderBreakdown(stats) }
                    }

                    // ── Filter Chips ──
                    item { FilterRow(uiState.selectedFilter) { viewModel.setFilter(it) } }

                    // ── Log List ──
                    if (uiState.logs.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                Text("Chưa có hoạt động AI nào.", color = cs.onSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                    } else {
                        items(uiState.logs, key = { it.id }) { log ->
                            AiLogRow(log = log)
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ── AI Stats Dashboard (4 KPI cards) ──

@Composable
private fun AiStatsDashboard(stats: AdminAiStatsResponse) {
    val cs = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AiKpiCard(
                value = "${stats.totalCalls}",
                label = "Tổng API Calls",
                icon = Icons.Default.Api,
                iconTint = cs.primary,
                modifier = Modifier.weight(1f)
            )
            AiKpiCard(
                value = "${stats.todayCalls}",
                label = "Hôm nay",
                icon = Icons.Default.Timeline,
                iconTint = cs.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val successRate = if (stats.totalCalls > 0) (stats.successCalls * 100 / stats.totalCalls) else 0
            AiKpiCard(
                value = "$successRate%",
                label = "Thành công",
                icon = Icons.Default.CheckCircle,
                iconTint = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            AiKpiCard(
                value = "${stats.avgDurationMs}ms",
                label = "TB thời gian",
                icon = Icons.Default.Speed,
                iconTint = StreakFlame,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AiKpiCard(
                value = formatTokens(stats.totalTokensUsed),
                label = "Tổng Tokens",
                icon = Icons.Default.Token,
                iconTint = cs.secondary,
                modifier = Modifier.weight(1f)
            )
            AiKpiCard(
                value = "${stats.rateLimitedCalls}",
                label = "Rate Limited",
                icon = Icons.Default.Error,
                iconTint = cs.error,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AiKpiCard(
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
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = cs.onSurfaceVariant, fontSize = 11.sp)
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                value, color = cs.onSurface,
                fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp
            )
        }
    }
}

// ── Provider Breakdown Card ──

@Composable
private fun ProviderBreakdown(stats: AdminAiStatsResponse) {
    val cs = MaterialTheme.colorScheme
    if (stats.callsByProvider.isEmpty() && stats.callsByType.isEmpty()) return

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Phân bổ theo Provider", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))

            if (stats.callsByProvider.isNotEmpty()) {
                val total = stats.callsByProvider.values.sum().coerceAtLeast(1)

                // Stacked bar
                Row(
                    Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))
                        .background(cs.outlineVariant.copy(alpha = 0.3f))
                ) {
                    stats.callsByProvider.entries.forEachIndexed { index, (_, count) ->
                        val fraction = count.toFloat() / total
                        val color = providerColor(index)
                        Box(
                            Modifier.weight(fraction.coerceAtLeast(0.01f)).height(12.dp)
                                .background(color)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Labels
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    stats.callsByProvider.entries.forEachIndexed { index, (name, count) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(providerColor(index)))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "$name ($count)",
                                color = cs.onSurfaceVariant, fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (stats.callsByType.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("Phân bổ theo loại", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))

                stats.callsByType.entries.sortedByDescending { it.value }.forEach { (type, count) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatPromptType(type), color = cs.onSurfaceVariant, fontSize = 13.sp)
                        Text("$count", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ── Filter Chips Row ──

@Composable
private fun FilterRow(selected: String, onSelect: (String) -> Unit) {
    val filters = listOf("Tất cả", "Thành công", "Thất bại", "Rate Limited")
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

// ── Log Row ──

@Composable
private fun AiLogRow(log: AdminAiLogDto) {
    val cs = MaterialTheme.colorScheme
    val statusColor = when (log.status) {
        "Success" -> Color(0xFF4CAF50)
        "RateLimited" -> StreakFlame
        else -> cs.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Memory, null, tint = statusColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(log.userEmail, color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                    Text(
                        "${log.model} • ${log.provider}",
                        color = cs.onSurfaceVariant, fontSize = 11.sp, maxLines = 1
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    log.status,
                    color = statusColor,
                    fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    formatPromptType(log.promptType),
                    color = cs.onSurface, fontSize = 13.sp
                )
                Text("${log.tokensUsed} tokens", color = cs.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(log.timestamp.take(16).replace("T", " "), color = cs.onSurfaceVariant, fontSize = 11.sp)
                if (log.durationMs > 0) {
                    Text("${log.durationMs}ms", color = cs.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        }
    }
}

// ── Helpers ──

private fun formatPromptType(type: String): String = when (type) {
    "GenerateFlashcards" -> "Tạo Flashcard"
    "GenerateExample" -> "Tạo ví dụ"
    "ExtractVocab" -> "Trích xuất từ vựng"
    "TutorChat" -> "AI Tutor Chat"
    "GenerateImage" -> "Tạo hình ảnh"
    "AdaptiveHint" -> "Gợi ý thích ứng"
    "Quiz" -> "Tạo quiz"
    "WordAnalysis" -> "Phân tích từ"
    else -> type
}

private fun formatTokens(tokens: Int): String = when {
    tokens >= 1_000_000 -> String.format("%.1fM", tokens / 1_000_000.0)
    tokens >= 1_000 -> String.format("%.1fK", tokens / 1_000.0)
    else -> "$tokens"
}

private fun providerColor(index: Int): Color = when (index) {
    0 -> Color(0xFF4285F4) // Google Blue
    1 -> Color(0xFFF97316) // Groq Orange
    2 -> Color(0xFF06B6D4) // DeepSeek Cyan
    else -> Color(0xFF9CA3AF)
}
