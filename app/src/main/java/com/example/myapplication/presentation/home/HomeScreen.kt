package com.example.myapplication.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.domain.model.Deck
import com.example.myapplication.domain.model.LearningStats
import com.example.myapplication.ui.theme.QualityAgain
import com.example.myapplication.ui.theme.QualityGood
import com.example.myapplication.ui.theme.QualityHard
import com.example.myapplication.ui.theme.StreakFlame

// ════════════════════════════════════════════════════
//  Color Palette
// ════════════════════════════════════════════════════
private val PrimaryIndigo = Color(0xFF4F46E5)
private val PrimaryDark = Color(0xFF3730A3)
private val HeroGradient = listOf(PrimaryIndigo, Color(0xFF6366F1))
private val WarningBadgeBg = Color(0xFFFEF3C7)
private val WarningBadgeText = Color(0xFF92400E)
private val BorderColor = Color(0xFFE2E8F0)
private val TextPrimary = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF64748B)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onStartStudyClick: () -> Unit = {},
    onDeckClick: (String) -> Unit = {},
    onAvatarClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val cs = MaterialTheme.colorScheme

    // Refresh stats every time this screen enters composition
    val refreshKey = remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        refreshKey.value++
    }
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
        // ── 1. HEADER ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = getGreeting(),
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
                            .background(cs.primary)
                            .clickable { onAvatarClick() },
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

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── 2. HERO — Continue Learning ──
        item {
            HeroCard(
                cardsToReview = uiState.stats?.dueCards ?: 0,
                deckNames = uiState.recentDecks
                    .filter { it.dueCount > 0 }
                    .map { it.name },
                onClick = onStartStudyClick
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── 3. QUICK STATS ROW ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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

        // ── 4. MEMORY PROGRESS ──
        item {
            Text(
                text = "Tổng quan học tập",
                color = cs.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            uiState.stats?.let { stats ->
                MasteryProgress(stats)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── 5. RECENT DECKS ──
        if (uiState.recentDecks.isNotEmpty()) {
            item {
                Text(
                    text = "Deck gần đây",
                    color = cs.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            items(
                items = uiState.recentDecks,
                key = { it.id }
            ) { deck ->
                RecentDeckCard(
                    deck = deck,
                    onClick = { onDeckClick(deck.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

// ════════════════════════════════════════════════════
//  Hero Card
// ════════════════════════════════════════════════════

@Composable
private fun HeroCard(
    cardsToReview: Int,
    deckNames: List<String>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(colors = HeroGradient)
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Sẵn sàng học chưa?",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (cardsToReview > 0) "$cardsToReview thẻ cần ôn hôm nay"
                    else "Bạn đã ôn xong! 🎉",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                // Show deck names that have due cards
                if (deckNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = deckNames.joinToString(" • "),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = PrimaryIndigo
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Bắt đầu",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bắt đầu học",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════
//  Recent Deck Card
// ════════════════════════════════════════════════════

@Composable
private fun RecentDeckCard(
    deck: Deck,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Deck icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryIndigo.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Deck name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deck.name,
                    color = cs.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${deck.cardCount} thẻ",
                    color = cs.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            // Due badge
            if (deck.dueCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarningBadgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${deck.dueCount} cần ôn",
                        color = WarningBadgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════
//  Stat Card
// ════════════════════════════════════════════════════

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

// ════════════════════════════════════════════════════
//  Mastery Progress
// ════════════════════════════════════════════════════

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

// ════════════════════════════════════════════════════
//  Sync Indicator
// ════════════════════════════════════════════════════

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

// ════════════════════════════════════════════════════
//  Helpers
// ════════════════════════════════════════════════════

private fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Chào buổi sáng,"
        hour < 18 -> "Chào buổi chiều,"
        else -> "Chào buổi tối,"
    }
}
