package com.example.myapplication.presentation.home

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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.domain.model.LearningStats
import com.example.myapplication.ui.theme.GradientEnd
import com.example.myapplication.ui.theme.GradientStart
import com.example.myapplication.ui.theme.QualityAgain
import com.example.myapplication.ui.theme.QualityGood
import com.example.myapplication.ui.theme.QualityHard
import com.example.myapplication.ui.theme.StreakFlame
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onStartStudyClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val cs = MaterialTheme.colorScheme

    // Refresh stats every time this screen enters composition (tab switch, navigateUp, etc.)
    val refreshKey = remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        refreshKey.value++ // trigger on first composition
    }
    // Also refresh whenever we return to this composable (e.g., after study session)
    DisposableEffect(Unit) {
        viewModel.refresh()
        onDispose { }
    }

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = cs.primary)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Chào buổi sáng,",
                        color = cs.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Text(
                        text = uiState.userName.ifBlank { "Học viên" },
                        color = cs.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sync status indicator
                    SyncIndicator(
                        isSyncing = isSyncing,
                        lastResult = uiState.lastSyncResult,
                        onSyncClick = { viewModel.triggerSync() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(cs.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.userName.take(1).uppercase(),
                            color = cs.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            // Quick Start Card
            QuickStartCard(
                cardsToReview = uiState.stats?.dueCards ?: 0,
                onClick = onStartStudyClick
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // Stats & Streak row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    StatCard(
                        title = "Đã học",
                        value = "${uiState.stats?.totalReviews ?: 0}",
                        subtitle = "Thẻ"
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    StatCard(
                        title = "Chuỗi ngày",
                        value = "${uiState.stats?.currentStreak ?: 0}",
                        subtitle = "Ngày",
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = StreakFlame
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // Section Title
            Text(
                text = "Tổng quan học tập",
                color = cs.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Mastery breakdown
            uiState.stats?.let { stats ->
                MasteryProgress(stats)
            }
        }
    }
}

@Composable
private fun QuickStartCard(
    cardsToReview: Int,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    // Hero card swaps: Light mode → dark bg, Dark mode → light bg
    val gradientColors = if (isDark) {
        listOf(Color(0xFFE3F6F5), Color(0xFFBAE8E8)) // Mint/teal gradient (sáng)
    } else {
        listOf(GradientStart, GradientEnd)            // Dark navy gradient
    }
    val heroTextColor = if (isDark) Color.Black else Color.White
    val heroSubTextColor = if (isDark) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f)
    val heroBtnBg = if (isDark) Color(0xFF272343) else Color.White
    val heroBtnText = if (isDark) Color.White else Color(0xFF272343)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(colors = gradientColors)
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Sẵn sàng học chưa?",
                    color = heroSubTextColor,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bạn có $cardsToReview thẻ cần ôn tập",
                    color = heroTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = heroBtnBg,
                        contentColor = heroBtnText
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Bắt đầu",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ôn tập ngay",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = Color.Unspecified
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(text = title, color = cs.onSurfaceVariant, fontSize = 13.sp)
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, color = cs.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = subtitle, color = cs.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun MasteryProgress(stats: LearningStats) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tỷ lệ ghi nhớ", color = cs.onSurface, fontWeight = FontWeight.SemiBold)
                Text("${(stats.averageAccuracy * 100).toInt()}%", color = cs.primary, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                    .background(cs.outlineVariant.copy(alpha = 0.3f))
            ) {
                val good = stats.goodReviews.toFloat()
                val hard = stats.hardReviews.toFloat()
                val again = stats.againReviews.toFloat()
                val total = good + hard + again
                if (total > 0f) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (good > 0f) Box(modifier = Modifier.weight(good / total).fillMaxSize().background(QualityGood))
                        if (hard > 0f) Box(modifier = Modifier.weight(hard / total).fillMaxSize().background(QualityHard))
                        if (again > 0f) Box(modifier = Modifier.weight(again / total).fillMaxSize().background(QualityAgain))
                    }
                } else {
                    // No data — show empty bar
                    Box(modifier = Modifier.fillMaxSize().background(cs.outlineVariant.copy(alpha = 0.3f)))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem("Tốt", QualityGood)
                LegendItem("Khó", QualityHard)
                LegendItem("Học lại", QualityAgain)
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = cs.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
fun SyncIndicator(
    isSyncing: Boolean,
    lastResult: SyncResult?,
    onSyncClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val (icon, tint, description) = when {
        isSyncing || lastResult == SyncResult.IN_PROGRESS -> Triple(
            Icons.Default.Sync, cs.primary, "Đang đồng bộ..."
        )
        lastResult == SyncResult.FAILED -> Triple(
            Icons.Default.SyncProblem, cs.error, "Đồng bộ thất bại"
        )
        lastResult == SyncResult.SUCCESS -> Triple(
            Icons.Default.CloudDone, QualityGood, "Đã đồng bộ"
        )
        else -> Triple(
            Icons.Default.Sync, cs.onSurfaceVariant, "Đồng bộ"
        )
    }

    androidx.compose.material3.IconButton(
        onClick = onSyncClick,
        enabled = !isSyncing && lastResult != SyncResult.IN_PROGRESS
    ) {
        if (isSyncing || lastResult == SyncResult.IN_PROGRESS) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = cs.primary,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
