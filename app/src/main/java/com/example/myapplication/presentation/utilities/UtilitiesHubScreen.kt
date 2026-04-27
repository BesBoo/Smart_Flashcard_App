package com.example.myapplication.presentation.utilities

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.R

// ════════════════════════════════════════════════════
//  Color Palette
// ════════════════════════════════════════════════════
private val PrimaryIndigo = Color(0xFF4F46E5)
private val PrimaryLight = Color(0xFFEEF2FF)
private val BorderColor = Color(0xFFE2E8F0)
private val SurfaceWhite = Color(0xFFFFFFFF)
private val ComingSoonBg = Color(0xFFF1F5F9)
private val ComingSoonText = Color(0xFF94A3B8)
private val WarningAmber = Color(0xFFF59E0B)
private val DangerRed = Color(0xFFEF4444)
private val ToggleOn = Color(0xFF4F46E5)
private val ToggleTrack = Color(0xFFCBD5F5)

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
            .padding(horizontal = 16.dp)
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

        // ═══════════════════════════════════════════
        // 1️⃣ TODAY SUMMARY
        // ═══════════════════════════════════════════
        QuickStatsCard(
            dueCards = uiState.dueCards,
            totalCards = uiState.totalCards,
            currentStreak = uiState.currentStreak
        )

        Spacer(Modifier.height(24.dp))

        // ═══════════════════════════════════════════
        // 2️⃣ PRIMARY TOOLS
        // ═══════════════════════════════════════════
        SectionHeader("Công cụ chính")

        Spacer(Modifier.height(12.dp))

        // Smart Review — FEATURED
        FeaturedToolCard(
            iconResId = R.drawable.ic_smart_review,
            title = "Smart Review",
            subtitle = "Ôn tập toàn bộ deck",
            onClick = onOpenSmartReview
        )

        Spacer(Modifier.height(12.dp))

        // AI Chat
        ToolCard(
            iconResId = R.drawable.ic_ai_chat,
            title = "Memo Chat",
            subtitle = "Chat cùng Memo để học từ mới",
            onClick = onOpenAiChat
        )

        Spacer(Modifier.height(12.dp))

        // Flashcard Quiz
        ToolCard(
            iconResId = R.drawable.ic_flashcard_quiz,
            title = "Flashcard Quiz",
            subtitle = "Trắc nghiệm từ vựng",
            onClick = onOpenFlashcardQuiz
        )

        Spacer(Modifier.height(24.dp))

        // ═══════════════════════════════════════════
        // 3️⃣ SECONDARY TOOLS (Coming Soon)
        // ═══════════════════════════════════════════
        SectionHeader("Công cụ sắp có")

        Spacer(Modifier.height(12.dp))

        ComingSoonCard(
            iconResId = R.drawable.ic_more_tools,
            title = "Công cụ khác",
            subtitle = "Coming soon"
        )

        Spacer(Modifier.height(24.dp))

        // ═══════════════════════════════════════════
        // 4️⃣ SETTINGS
        // ═══════════════════════════════════════════
        SectionHeader("Cài đặt nhanh")

        Spacer(Modifier.height(12.dp))

        FloatingChatToggle(
            isEnabled = uiState.isBubbleEnabled,
            onToggle = { viewModel.toggleBubble(it) }
        )

        Spacer(Modifier.height(32.dp))
    }
}

// ════════════════════════════════════════════════════
//  Section Header
// ════════════════════════════════════════════════════

@Composable
private fun SectionHeader(title: String) {
    val cs = MaterialTheme.colorScheme
    Text(
        text = title,
        color = cs.onSurface,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )
}

// ════════════════════════════════════════════════════
//  Quick Stats Card — white bg + border + dividers
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
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, BorderColor)
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
                color = if (dueCards > 0) WarningAmber else cs.primary
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(56.dp)
                    .background(BorderColor)
            )

            StatItem(
                label = "Toàn bộ",
                value = formatNumber(totalCards),
                unit = "thẻ",
                color = cs.primary
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(56.dp)
                    .background(BorderColor)
            )

            StatItem(
                label = "Chuỗi ngày",
                value = "$currentStreak",
                unit = "ngày",
                color = DangerRed,
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
//  Featured Tool Card (Smart Review)
// ════════════════════════════════════════════════════

@Composable
private fun FeaturedToolCard(
    iconResId: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            androidx.compose.foundation.Image(
                painter = painterResource(id = iconResId),
                contentDescription = title,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.Black,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.Black.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryIndigo,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Bắt đầu",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════
//  Normal Tool Card — white bg + border, compact 72dp
// ════════════════════════════════════════════════════

@Composable
private fun ToolCard(
    iconResId: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            androidx.compose.foundation.Image(
                painter = painterResource(id = iconResId),
                contentDescription = title,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = cs.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = cs.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════
//  Coming Soon Card — muted bg
// ════════════════════════════════════════════════════

@Composable
private fun ComingSoonCard(
    iconResId: Int,
    title: String,
    subtitle: String
) {
    val cs = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ComingSoonBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            androidx.compose.foundation.Image(
                painter = painterResource(id = iconResId),
                contentDescription = title,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                alpha = 0.5f
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = ComingSoonText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = ComingSoonText.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════
//  Floating Chat Toggle — own section
// ════════════════════════════════════════════════════

@Composable
private fun FloatingChatToggle(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.3f))
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
                        if (isEnabled) PrimaryIndigo.copy(alpha = 0.1f)
                        else cs.outlineVariant.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChatBubble,
                    contentDescription = null,
                    tint = if (isEnabled) PrimaryIndigo else cs.onSurfaceVariant,
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
                    text = "Hiển thị chat nổi trên mọi tab",
                    color = cs.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ToggleOn,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = ToggleTrack
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
