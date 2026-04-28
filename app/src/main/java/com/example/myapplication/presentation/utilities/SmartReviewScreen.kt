package com.example.myapplication.presentation.utilities

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.R

private val AccentGreen = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartReviewScreen(
    modifier: Modifier = Modifier,
    viewModel: SmartReviewViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme

    Scaffold(
        modifier = modifier,
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState.phase) {
                            SmartReviewPhase.SETUP -> "Ôn tập thông minh"
                            SmartReviewPhase.QUIZ -> "Câu ${uiState.currentIndex + 1}/${uiState.totalQuestions}"
                            SmartReviewPhase.RESULT -> "Kết quả"
                        },
                        color = cs.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = cs.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentGreen)
                        Spacer(Modifier.height(16.dp))
                        Text("Đang tạo câu hỏi biến thể...", color = cs.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
                uiState.error != null && uiState.phase != SmartReviewPhase.SETUP -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("❌ ${uiState.error}", color = cs.error, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.restartReview() }) { Text("Thử lại") }
                    }
                }
                uiState.phase == SmartReviewPhase.SETUP -> {
                    SetupContent(
                        uiState = uiState,
                        onToggleDeck = { viewModel.toggleDeckSelection(it) },
                        onToggleAll = { viewModel.toggleAllDecks() },
                        onQuestionCountChange = { viewModel.updateQuestionCount(it) },
                        onStart = { viewModel.startReview() }
                    )
                }
                uiState.phase == SmartReviewPhase.QUIZ -> {
                    QuizContent(
                        uiState = uiState,
                        onSelectAnswer = { viewModel.selectAnswer(it) },
                        onNext = { viewModel.nextQuestion() }
                    )
                }
                uiState.phase == SmartReviewPhase.RESULT -> {
                    ResultContent(
                        correct = uiState.correctCount,
                        total = uiState.totalQuestions,
                        onRestart = { viewModel.restartReview() },
                        onBack = onNavigateBack
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════
//  SETUP
// ════════════════════════════════════════════════════

@Composable
private fun SetupContent(
    uiState: SmartReviewUiState,
    onToggleDeck: (String) -> Unit,
    onToggleAll: () -> Unit,
    onQuestionCountChange: (Int) -> Unit,
    onStart: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Header
            Box(
                modifier = Modifier
                    .size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_smart_review),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(52.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Ôn tập thông minh", color = cs.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Ôn toàn bộ thẻ từ mọi deck với câu hỏi biến thể", color = cs.onSurfaceVariant, fontSize = 14.sp)

            Spacer(Modifier.height(20.dp))

            // Deck selection
            Text("Chọn deck", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))

            // Select all
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.allDecksSelected,
                        onCheckedChange = { onToggleAll() },
                        colors = CheckboxDefaults.colors(checkedColor = AccentGreen)
                    )
                    Text("Tất cả deck", color = cs.onSurface, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(8.dp))

            val deckListState = rememberLazyListState()
            LazyColumn(
                state = deckListState,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .simpleVerticalScrollbar(deckListState, AccentGreen)
            ) {
                items(uiState.decks) { deck ->
                    val isSelected = deck.id in uiState.selectedDeckIds
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) AccentGreen.copy(alpha = 0.08f) else cs.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggleDeck(deck.id) },
                                colors = CheckboxDefaults.colors(checkedColor = AccentGreen)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(deck.name, color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("${deck.cardCount} thẻ", color = cs.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Question count slider
            Text("Số câu hỏi: ${uiState.questionCount}", color = cs.onSurface, fontWeight = FontWeight.SemiBold)
            Slider(
                value = uiState.questionCount.toFloat(),
                onValueChange = { onQuestionCountChange(it.toInt()) },
                valueRange = 5f..20f,
                steps = 2,
                colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen)
            )

            if (uiState.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(uiState.error, color = cs.error, fontSize = 13.sp)
            }
        }

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
            enabled = uiState.selectedDeckIds.isNotEmpty()
        ) {
            Text("Bắt đầu ôn tập", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ════════════════════════════════════════════════════
//  QUIZ
// ════════════════════════════════════════════════════

@Composable
private fun QuizContent(
    uiState: SmartReviewUiState,
    onSelectAnswer: (Int) -> Unit,
    onNext: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val question = uiState.currentQuestion ?: return

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = AccentGreen,
                trackColor = cs.surfaceContainer,
            )

            Spacer(Modifier.height(24.dp))

            // Animated question transitions
            AnimatedContent(
                targetState = uiState.currentIndex,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it } + fadeOut()
                    )
                },
                label = "question"
            ) { _ ->
                val q = uiState.currentQuestion ?: return@AnimatedContent
                Column {
            // Cloze sentence
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Điền từ phù hợp:",
                        color = cs.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = q.sentence,
                        color = cs.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 28.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Options
            q.options.forEachIndexed { index, option ->
                val isSelected = uiState.selectedAnswer == index
                val isCorrect = index == q.correctIndex
                val isRevealed = uiState.isRevealed

                val borderColor by animateColorAsState(
                    targetValue = when {
                        isRevealed && isCorrect -> AccentGreen
                        isRevealed && isSelected && !isCorrect -> cs.error
                        isSelected -> AccentGreen
                        else -> cs.outlineVariant.copy(alpha = 0.3f)
                    },
                    label = "borderColor"
                )

                val bgColor by animateColorAsState(
                    targetValue = when {
                        isRevealed && isCorrect -> AccentGreen.copy(alpha = 0.1f)
                        isRevealed && isSelected && !isCorrect -> cs.error.copy(alpha = 0.1f)
                        else -> Color.Transparent
                    },
                    label = "bgColor"
                )

                OutlinedButton(
                    onClick = { onSelectAnswer(index) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, borderColor),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = bgColor),
                    enabled = !isRevealed
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape)
                                .background(if (isSelected || (isRevealed && isCorrect)) borderColor else cs.outlineVariant.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRevealed && isCorrect) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            } else if (isRevealed && isSelected && !isCorrect) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Text(
                                    "${'A' + index}",
                                    color = if (isSelected) Color.White else cs.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = option,
                            color = cs.onSurface,
                            fontSize = 16.sp,
                            fontWeight = if (isRevealed && isCorrect) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
                } // end Column inside AnimatedContent
            } // end AnimatedContent
        }

        // Next button
        if (uiState.isRevealed) {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text(
                    if (uiState.currentIndex + 1 >= uiState.totalQuestions) "Xem kết quả" else "Câu tiếp theo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════
//  RESULT
// ════════════════════════════════════════════════════

@Composable
private fun ResultContent(
    correct: Int,
    total: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val percentage = if (total > 0) (correct * 100) / total else 0

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.EmojiEvents,
            null,
            tint = if (percentage >= 70) AccentGreen else Color(0xFFF59E0B),
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = when {
                percentage >= 80 -> "Xuất sắc! 🎉"
                percentage >= 50 -> "Tốt lắm! 👍"
                else -> "Hãy ôn lại nhé! 💪"
            },
            color = cs.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "$correct/$total câu đúng ($percentage%)",
            color = cs.onSurfaceVariant,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        // Score bar
        Spacer(Modifier.height(20.dp))
        val animatedProgress by animateFloatAsState(
            targetValue = if (total > 0) correct.toFloat() / total else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "scoreProgress"
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = if (percentage >= 70) AccentGreen else Color(0xFFF59E0B),
            trackColor = cs.surfaceContainer,
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Ôn lại", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.3f))
        ) {
            Text("Quay lại", color = cs.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
    }
}
