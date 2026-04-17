package com.example.myapplication.presentation.study

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myapplication.domain.model.ReviewQuality
import com.example.myapplication.ui.theme.GradientEnd
import com.example.myapplication.ui.theme.GradientStart
import com.example.myapplication.ui.theme.QualityAgain
import com.example.myapplication.ui.theme.QualityEasy
import com.example.myapplication.ui.theme.QualityGood
import com.example.myapplication.ui.theme.QualityHard
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionScreen(
    modifier: Modifier = Modifier,
    viewModel: StudySessionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current

    // Initialize TTS
    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.US
            }
        }
        engine
    }

    DisposableEffect(Unit) {
        onDispose { tts?.shutdown() }
    }

    Scaffold(
        modifier = modifier,
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ôn tập", color = cs.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Còn ${uiState.cardsRemaining} thẻ", color = cs.onSurfaceVariant, fontSize = 12.sp)
                    }
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = cs.primary,
                trackColor = cs.surfaceContainer
            )

            Spacer(Modifier.height(8.dp))

            // Progress text
            Text(
                text = "${uiState.currentIndex + 1} / ${uiState.cards.size}",
                color = cs.onSurfaceVariant, fontSize = 13.sp
            )

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = cs.primary)
                    }
                }
                uiState.cards.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎉", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("Không có thẻ cần ôn!", color = cs.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Quay lại sau nhé.", color = cs.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }
                uiState.isSessionComplete -> {
                    SessionCompleteView(
                        summary = uiState.summary,
                        difficultCount = uiState.difficultCards.size,
                        onRestart = { viewModel.restartSession() },
                        onFinish = onNavigateBack
                    )
                }
                else -> {
                    Spacer(Modifier.height(20.dp))

                    // Flip Card
                    FlipCard(
                        frontText = uiState.currentCard?.frontText ?: "",
                        backText = uiState.currentCard?.backText ?: "",
                        exampleText = uiState.currentCard?.exampleText,
                        imageUrl = uiState.currentCard?.imageUrl,
                        isFlipped = uiState.isFlipped,
                        onFlip = { viewModel.flipCard() },
                        onSpeakFront = {
                            val text = uiState.currentCard?.frontText ?: return@FlipCard
                            val locale = detectLocale(text)
                            tts?.language = locale
                            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "front")
                        },
                        onSpeakBack = {
                            val text = uiState.currentCard?.backText ?: return@FlipCard
                            val locale = detectLocale(text)
                            tts?.language = locale
                            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "back")
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.height(20.dp))

                    // SM-2 Answer Buttons (only show when flipped)
                    if (uiState.isFlipped) {
                        SM2Buttons(onAnswer = { viewModel.answerCard(it) })
                    } else {
                        Text(
                            "Chạm để lật thẻ",
                            color = cs.onSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    // ── AI-16: Struggling Card Hint Dialog ──
    if (uiState.showAiHintDialog) {
        AiHintDialog(
            card = uiState.aiHintCard,
            isLoading = uiState.aiHintLoading,
            hintResult = uiState.aiHintResult,
            onRequestHint = { viewModel.loadAiHint() },
            onDismiss = { viewModel.dismissAiHint() }
        )
    }
}

/** Detect language locale from text for TTS */
private fun detectLocale(text: String): Locale {
    val hasVietnamese = text.any { c ->
        c in '\u00C0'..'\u00FF' || c in '\u0100'..'\u024F' || c in '\u1E00'..'\u1EFF'
                || c == 'đ' || c == 'Đ'
    }
    val hasChinese = text.any { c -> c in '\u4E00'..'\u9FFF' }
    val hasJapanese = text.any { c -> c in '\u3040'..'\u30FF' || c in '\u31F0'..'\u31FF' }
    val hasKorean = text.any { c -> c in '\uAC00'..'\uD7AF' }

    return when {
        hasChinese -> Locale.CHINESE
        hasJapanese -> Locale.JAPANESE
        hasKorean -> Locale.KOREAN
        hasVietnamese -> Locale("vi", "VN")
        else -> Locale.US
    }
}

@Composable
fun FlipCard(
    frontText: String,
    backText: String,
    exampleText: String?,
    imageUrl: String?,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onSpeakFront: () -> Unit = {},
    onSpeakBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400),
        label = "flipRotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onFlip() }
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // ─── FRONT SIDE ───
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Front text
                    Text(
                        text = frontText,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    // Speaker icon for front pronunciation
                    IconButton(
                        onClick = { onSpeakFront() },
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            "Phát âm",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TouchApp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Chạm để xem đáp án", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            } else {
                // ─── BACK SIDE (mirrored to compensate for rotation) ───
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                        .verticalScroll(rememberScrollState())
                ) {
                    // Back text + speaker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = backText,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { onSpeakBack() },
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                "Phát âm",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Example
                    if (!exampleText.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "💡 $exampleText",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Italic
                        )
                    }

                    // Image
                    if (!imageUrl.isNullOrBlank()) {
                        Spacer(Modifier.height(16.dp))

                        val imageModel = if (imageUrl.startsWith("http")) {
                            imageUrl
                        } else {
                            File(imageUrl)
                        }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Ảnh minh họa",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 10f)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SM2Buttons(onAnswer: (ReviewQuality) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SM2Button("Học lại", QualityAgain, Modifier.weight(1f)) { onAnswer(ReviewQuality.AGAIN) }
        SM2Button("Khó", QualityHard, Modifier.weight(1f)) { onAnswer(ReviewQuality.HARD) }
        SM2Button("Tốt", QualityGood, Modifier.weight(1f)) { onAnswer(ReviewQuality.GOOD) }
        SM2Button("Dễ", QualityEasy, Modifier.weight(1f)) { onAnswer(ReviewQuality.EASY) }
    }
}

@Composable
private fun SM2Button(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.15f), contentColor = color),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun SessionCompleteView(
    summary: com.example.myapplication.domain.model.StudySessionSummary?,
    difficultCount: Int = 0,
    onRestart: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = QualityGood, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(20.dp))
        Text("Hoàn thành!", color = MaterialTheme.colorScheme.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Bạn đã ôn tập ${summary?.totalCards ?: 0} thẻ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
        
        Spacer(Modifier.height(32.dp))

        // Stats Cards
        if (summary != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    StatRow("Đúng", "${summary.correctCount}/${summary.totalCards}", QualityGood)
                    StatRow("Độ chính xác", "${summary.accuracyPercent}%", MaterialTheme.colorScheme.primary)
                    StatRow("Thời gian", "${summary.totalTimeMs / 1000}s", MaterialTheme.colorScheme.secondary)
                    if (difficultCount > 0) {
                        StatRow("Cần ôn lại", "$difficultCount thẻ", QualityAgain)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (difficultCount > 0) {
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QualityAgain.copy(alpha = 0.15f),
                        contentColor = QualityAgain
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Học lại ($difficultCount)", fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Hoàn tất", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/**
 * AI-16: Dialog shown when a card has been failed >= 3 consecutive times.
 * Offers AI-powered simplified explanation and additional examples.
 */
@Composable
private fun AiHintDialog(
    card: com.example.myapplication.domain.model.Flashcard?,
    isLoading: Boolean,
    hintResult: com.example.myapplication.domain.repository.AdaptiveHintResult?,
    onRequestHint: () -> Unit,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surface,
        icon = { Text("\uD83D\uDCA1", fontSize = 32.sp) },
        title = {
            Text(
                "Th\u1ebb n\u00e0y h\u01a1i kh\u00f3!",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (card != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(card.frontText, fontWeight = FontWeight.Bold, color = cs.onSurface, fontSize = 15.sp)
                            Text("\u2192 ${card.backText}", color = cs.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }

                when {
                    hintResult == null && !isLoading -> {
                        Text(
                            "B\u1ea1n \u0111\u00e3 sai th\u1ebb n\u00e0y nhi\u1ec1u l\u1ea7n. AI c\u00f3 th\u1ec3 gi\u1ea3i th\u00edch \u0111\u01a1n gi\u1ea3n h\u01a1n v\u00e0 th\u00eam v\u00ed d\u1ee5 gi\u00fap b\u1ea1n.",
                            color = cs.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    isLoading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = cs.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("AI \u0111ang ph\u00e2n t\u00edch...", color = cs.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    hintResult != null -> {
                        Text(
                            "\uD83D\uDCD6 Gi\u1ea3i th\u00edch \u0111\u01a1n gi\u1ea3n:",
                            fontWeight = FontWeight.Bold,
                            color = cs.primary,
                            fontSize = 14.sp
                        )
                        Text(hintResult.simplifiedExplanation, color = cs.onSurface, fontSize = 13.sp)

                        if (hintResult.additionalExamples.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "\uD83D\uDCDD V\u00ed d\u1ee5 th\u00eam:",
                                fontWeight = FontWeight.Bold,
                                color = cs.primary,
                                fontSize = 14.sp
                            )
                            hintResult.additionalExamples.forEachIndexed { i, example ->
                                Text(
                                    "${i + 1}. $example",
                                    color = cs.onSurface,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (hintResult == null && !isLoading) {
                Button(
                    onClick = onRequestHint,
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("AI H\u1ed7 tr\u1ee3", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                ) {
                    Text("\u0110\u00e3 hi\u1ec3u!", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (hintResult == null && !isLoading) {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("B\u1ecf qua", color = cs.onSurfaceVariant)
                }
            }
        }
    )
}

