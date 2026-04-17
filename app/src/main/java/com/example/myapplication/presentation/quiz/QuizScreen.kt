package com.example.myapplication.presentation.quiz

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.ui.theme.QualityGood

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel = hiltViewModel(),
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
                            QuizPhase.MULTIPLE_CHOICE -> "Quiz — Phần 1: Chọn đáp án"
                            QuizPhase.TYPING -> "Quiz — Phần 2: Viết đáp án"
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
                actions = {
                    if (!uiState.isLoading && !uiState.isFinished) {
                        val (current, total) = when (uiState.phase) {
                            QuizPhase.MULTIPLE_CHOICE ->
                                (uiState.currentIndex + 1) to uiState.totalQuestions
                            QuizPhase.TYPING ->
                                (uiState.typingIndex + 1) to uiState.totalTypingQuestions
                        }
                        Text(
                            text = "$current/$total",
                            color = cs.onSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 16.dp)
                        )
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
                        CircularProgressIndicator(color = cs.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Đang tạo câu hỏi...", color = cs.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
                uiState.error != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("❌ ${uiState.error}", color = cs.error, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onNavigateBack, colors = ButtonDefaults.buttonColors(containerColor = cs.primary)) {
                            Text("Quay lại")
                        }
                    }
                }
                uiState.isFinished -> {
                    ResultScreen(
                        phase1Correct = uiState.correctCount,
                        phase1Total = uiState.totalQuestions,
                        phase2Correct = uiState.typingCorrectCount,
                        phase2Total = uiState.totalTypingQuestions,
                        onRestart = { viewModel.restartQuiz() },
                        onBack = onNavigateBack
                    )
                }
                uiState.phase == QuizPhase.MULTIPLE_CHOICE -> {
                    MultipleChoiceContent(
                        uiState = uiState,
                        onSelectAnswer = { viewModel.selectAnswer(it) },
                        onNext = { viewModel.nextQuestion() }
                    )
                }
                uiState.phase == QuizPhase.TYPING -> {
                    TypingContent(
                        uiState = uiState,
                        onInputChange = { viewModel.updateTypingInput(it) },
                        onSubmit = { viewModel.submitTypingAnswer() },
                        onNext = { viewModel.nextTypingQuestion() }
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════
//  PART 1: Multiple Choice
// ════════════════════════════════════════════════════

@Composable
private fun MultipleChoiceContent(
    uiState: QuizUiState,
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
            // Progress
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = cs.primary,
                trackColor = cs.surfaceContainer,
            )

            Spacer(Modifier.height(24.dp))

            // Question
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
            ) {
                Text(
                    text = question.questionText,
                    color = cs.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(20.dp),
                    lineHeight = 26.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            // Options
            question.options.forEachIndexed { index, option ->
                val isSelected = uiState.selectedAnswer == index
                val isCorrect = index == question.correctIndex
                val isRevealed = uiState.isAnswerRevealed

                val borderColor by animateColorAsState(
                    targetValue = when {
                        isRevealed && isCorrect -> QualityGood
                        isRevealed && isSelected && !isCorrect -> cs.error
                        isSelected -> cs.primary
                        else -> cs.outlineVariant.copy(alpha = 0.3f)
                    },
                    label = "borderColor"
                )

                val bgColor by animateColorAsState(
                    targetValue = when {
                        isRevealed && isCorrect -> QualityGood.copy(alpha = 0.1f)
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
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Next button
        if (uiState.isAnswerRevealed) {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
            ) {
                Text(
                    if (uiState.currentIndex + 1 >= uiState.totalQuestions) "Sang Phần 2 →" else "Câu tiếp theo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════
//  PART 2: Typing
// ════════════════════════════════════════════════════

@Composable
private fun TypingContent(
    uiState: QuizUiState,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val question = uiState.currentTypingQuestion ?: return

    // Local text state for Vietnamese IME compatibility
    var localInput by remember(uiState.typingIndex) { mutableStateOf(uiState.typingInput) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Progress
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = cs.tertiary,
                trackColor = cs.surfaceContainer,
            )

            Spacer(Modifier.height(8.dp))

            // Phase indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            null,
                            tint = cs.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Phần 2: Viết đáp án",
                            color = cs.onTertiaryContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Prompt
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Nghĩa của từ này là gì?",
                        color = cs.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = question.promptText,
                        color = cs.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Input field (multi-line for answers with multiple lines)
            OutlinedTextField(
                value = localInput,
                onValueChange = {
                    localInput = it
                    onInputChange(it)
                },
                placeholder = { Text("Nhập đáp án... (Enter để xuống dòng)", color = cs.onSurfaceVariant.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = !uiState.isTypingRevealed,
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cs.primary,
                    unfocusedBorderColor = cs.outlineVariant.copy(alpha = 0.3f),
                    focusedTextColor = cs.onSurface,
                    unfocusedTextColor = cs.onSurface,
                    disabledTextColor = cs.onSurface,
                    disabledBorderColor = if (uiState.isTypingRevealed) {
                        if (uiState.isTypingCorrect) QualityGood else cs.error
                    } else cs.outlineVariant.copy(alpha = 0.3f),
                    cursorColor = cs.primary
                )
            )

            // Result feedback
            if (uiState.isTypingRevealed) {
                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isTypingCorrect)
                            QualityGood.copy(alpha = 0.1f)
                        else
                            cs.error.copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (uiState.isTypingCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                                null,
                                tint = if (uiState.isTypingCorrect) QualityGood else cs.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (uiState.isTypingCorrect) "Chính xác! 🎉" else "Chưa đúng",
                                color = if (uiState.isTypingCorrect) QualityGood else cs.error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        if (!uiState.isTypingCorrect) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Đáp án đúng:",
                                color = cs.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            Text(
                                question.correctAnswer,
                                color = QualityGood,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Submit / Next buttons
        Column {
            if (!uiState.isTypingRevealed) {
                Button(
                    onClick = {
                        onInputChange(localInput) // Ensure sync before submit
                        onSubmit()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    enabled = localInput.isNotBlank()
                ) {
                    Text("Kiểm tra", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                ) {
                    Text(
                        if (uiState.typingIndex + 1 >= uiState.totalTypingQuestions) "Xem kết quả" else "Câu tiếp theo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════
//  RESULT SCREEN
// ════════════════════════════════════════════════════

@Composable
private fun ResultScreen(
    phase1Correct: Int,
    phase1Total: Int,
    phase2Correct: Int,
    phase2Total: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val totalCorrect = phase1Correct + phase2Correct
    val totalAll = phase1Total + phase2Total
    val percentage = if (totalAll > 0) (totalCorrect * 100) / totalAll else 0

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.EmojiEvents,
            null,
            tint = if (percentage >= 70) QualityGood else cs.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (percentage >= 80) "Xuất sắc! 🎉" else if (percentage >= 50) "Tốt lắm! 👍" else "Hãy ôn lại nhé! 💪",
            color = cs.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Tổng: $totalCorrect/$totalAll câu đúng ($percentage%)",
            color = cs.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(16.dp))

        // Phase breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Phần 1 — Chọn đáp án", color = cs.onSurfaceVariant, fontSize = 14.sp)
                    Text(
                        "$phase1Correct/$phase1Total",
                        color = if (phase1Correct == phase1Total) QualityGood else cs.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Phần 2 — Viết đáp án", color = cs.onSurfaceVariant, fontSize = 14.sp)
                    Text(
                        "$phase2Correct/$phase2Total",
                        color = if (phase2Correct == phase2Total) QualityGood else cs.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Làm lại", fontWeight = FontWeight.Bold)
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
