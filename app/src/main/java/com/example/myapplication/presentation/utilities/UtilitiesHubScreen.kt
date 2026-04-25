package com.example.myapplication.presentation.utilities

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.R

@Composable
fun UtilitiesHubScreen(
    modifier: Modifier = Modifier,
    viewModel: UtilitiesHubViewModel = hiltViewModel(),
    onOpenAiChat: () -> Unit = {},
    onOpenSmartReview: () -> Unit = {},
    onOpenFlashcardQuiz: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme

    DisposableEffect(Unit) {
        viewModel.refresh()
        onDispose { }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        // ── Header ──
        Text(
            text = "Tiện ích",
            color = cs.onSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Công cụ hỗ trợ học tập",
            color = cs.onSurfaceVariant,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(20.dp))

        // ── Quick Stats Card ──
        QuickStatsCard(
            dueCards = uiState.dueCards,
            totalCards = uiState.totalCards,
            currentStreak = uiState.currentStreak
        )

        Spacer(Modifier.height(24.dp))

        // ── Tools Section Header ──
        Text(
            text = "Công cụ",
            color = cs.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        // ── Tool Cards ──
        ToolCard(
            iconResId = R.drawable.ic_ai_chat,
            title = "AI Chat",
            subtitle = "Học từ mới qua trò chuyện",
            onClick = onOpenAiChat
        )

        Spacer(Modifier.height(12.dp))

        ToolCard(
            iconResId = R.drawable.ic_smart_review,
            title = "Smart Review",
            subtitle = "Ôn tập biến thể từ mọi deck",
            onClick = onOpenSmartReview
        )

        Spacer(Modifier.height(12.dp))

        ToolCard(
            iconResId = R.drawable.ic_flashcard_quiz,
            title = "Flashcard Quiz",
            subtitle = "Trắc nghiệm từ vựng",
            onClick = onOpenFlashcardQuiz
        )

        Spacer(Modifier.height(12.dp))

        ToolCard(
            iconResId = R.drawable.ic_more_tools,
            title = "Công cụ khác",
            subtitle = "Coming soon",
            enabled = false,
            onClick = { }
        )

        Spacer(Modifier.height(24.dp))

        // ── Floating Chat Toggle ──
        FloatingChatToggle(
            isEnabled = uiState.isBubbleEnabled,
            onToggle = { viewModel.toggleBubble(it) }
        )

        Spacer(Modifier.height(32.dp))
    }
}

// ════════════════════════════════════════════════════
//  Quick Stats Card
// ════════════════════════════════════════════════════

@Composable
private fun QuickStatsCard(
    dueCards: Int,
    totalCards: Int,
    currentStreak: Int
) {
    val cs = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Hôm nay",
                value = "$dueCards",
                unit = "thẻ ôn",
                color = if (dueCards > 0) Color(0xFFF59E0B) else cs.primary
            )

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(56.dp)
                    .background(cs.outlineVariant.copy(alpha = 0.3f))
            )

            StatItem(
                label = "Toàn bộ",
                value = formatNumber(totalCards),
                unit = "thẻ",
                color = cs.primary
            )

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(56.dp)
                    .background(cs.outlineVariant.copy(alpha = 0.3f))
            )

            StatItem(
                label = "Chuỗi ngày",
                value = "$currentStreak",
                unit = "ngày",
                color = Color(0xFFEF4444),
                showFireIcon = currentStreak > 0
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    unit: String,
    color: Color,
    showFireIcon: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showFireIcon) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(2.dp))
            }
            Text(
                text = value,
                color = color,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = unit,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

// ════════════════════════════════════════════════════
//  Tool Card
// ════════════════════════════════════════════════════

@Composable
private fun ToolCard(
    iconResId: Int,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val alpha = if (enabled) 1f else 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon image
            androidx.compose.foundation.Image(
                painter = painterResource(id = iconResId),
                contentDescription = title,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
                alpha = alpha
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = cs.onSurface.copy(alpha = alpha),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = cs.onSurfaceVariant.copy(alpha = alpha),
                    fontSize = 13.sp
                )
            }

            if (enabled) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════
//  Floating Chat Toggle
// ════════════════════════════════════════════════════

@Composable
private fun FloatingChatToggle(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                cs.primaryContainer.copy(alpha = 0.3f)
            else
                cs.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isEnabled) cs.primary.copy(alpha = 0.15f)
                        else cs.outlineVariant.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChatBubble,
                    contentDescription = null,
                    tint = if (isEnabled) cs.primary else cs.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bong bóng chat",
                    color = cs.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Hiển thị bong bóng chat trên mọi màn hình",
                    color = cs.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = cs.primary,
                    checkedTrackColor = cs.primaryContainer
                )
            )
        }
    }
}

// ── Helpers ──

private fun formatNumber(n: Int): String = when {
    n >= 10_000 -> String.format("%.1fK", n / 1_000.0)
    n >= 1_000 -> String.format("%,d", n)
    else -> "$n"
}
