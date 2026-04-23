package com.example.myapplication.presentation.utilities

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.domain.model.VocabSuggestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-scroll to bottom on new message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Show toast
    LaunchedEffect(uiState.savedToast) {
        uiState.savedToast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = cs.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(32.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Psychology, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("AI Chat", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = cs.onSurface)
                            Text("Học từ vựng qua trò chuyện", fontSize = 11.sp, color = cs.onSurfaceVariant)
                        }
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Messages
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        onSaveVocab = { vocab -> viewModel.onSaveCardClick(vocab, msg.id) },
                        onToggleVocab = { word -> viewModel.toggleVocabSelection(msg.id, word) }
                    )
                }

                // Loading indicator
                if (uiState.isLoading) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Input bar
            ChatInputBar(
                inputText = inputText,
                isLoading = uiState.isLoading,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank() && !uiState.isLoading) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                }
            )
        }

        // Deck selection bottom sheet
        if (uiState.showDeckSelector) {
            DeckSelectionSheet(
                decks = uiState.decks,
                onDeckSelected = { viewModel.onDeckSelected(it) },
                onDismiss = { viewModel.dismissDeckSelector() }
            )
        }
    }
}

// ════════════════════════════════════════════════════
//  Chat Bubble
// ════════════════════════════════════════════════════

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onSaveVocab: (VocabSuggestion) -> Unit,
    onToggleVocab: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!message.isUser) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF6366F1).copy(alpha = 0.15f), Color(0xFF8B5CF6).copy(alpha = 0.15f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
            }

            Card(
                modifier = Modifier.widthIn(max = 300.dp),
                shape = RoundedCornerShape(
                    topStart = if (message.isUser) 16.dp else 4.dp,
                    topEnd = if (message.isUser) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.isUser) cs.primary else cs.surfaceContainer
                )
            ) {
                val textColor = if (message.isUser) cs.onPrimary else cs.onSurface
                Text(
                    text = if (message.isUser) AnnotatedString(message.text) else parseMarkdown(message.text),
                    color = textColor,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 20.sp
                )
            }
        }

        // Vocab suggestions (inline below AI message)
        if (!message.isUser && message.vocabSuggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            VocabSuggestionCard(
                suggestions = message.vocabSuggestions,
                savedWords = message.savedWords,
                onSave = onSaveVocab,
                onToggle = onToggleVocab
            )
        }
    }
}

// ════════════════════════════════════════════════════
//  Vocab Suggestion Card
// ════════════════════════════════════════════════════

@Composable
private fun VocabSuggestionCard(
    suggestions: List<VocabSuggestion>,
    savedWords: List<String>,
    onSave: (VocabSuggestion) -> Unit,
    onToggle: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp), // Align with AI bubble (past avatar)
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1).copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.BookmarkAdd,
                    null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Từ vựng phát hiện",
                    color = Color(0xFF6366F1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(10.dp))

            suggestions.forEach { vocab ->
                val isSaved = vocab.word in savedWords

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSaved) Color(0xFF10B981).copy(alpha = 0.08f) else Color.Transparent)
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                vocab.word,
                                color = cs.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (vocab.pronunciation != null) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    vocab.pronunciation,
                                    color = cs.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                        Text(
                            buildString {
                                if (vocab.partOfSpeech.isNotBlank()) append("(${vocab.partOfSpeech}) ")
                                append(vocab.definitionVi.ifBlank { vocab.definitionEn })
                            },
                            color = cs.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        if (vocab.example.isNotBlank()) {
                            Text(
                                "\"${vocab.example}\"",
                                color = cs.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic,
                                maxLines = 2
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    if (isSaved) {
                        Icon(
                            Icons.Default.CheckCircle,
                            "Đã lưu",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        IconButton(
                            onClick = { onSave(vocab) },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.15f))
                        ) {
                            Icon(
                                Icons.Default.BookmarkAdd,
                                "Lưu thẻ",
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (vocab != suggestions.last()) {
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════
//  Deck Selection Bottom Sheet
// ════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckSelectionSheet(
    decks: List<com.example.myapplication.domain.model.Deck>,
    onDeckSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cs.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                "Chọn Deck",
                color = cs.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Thẻ sẽ được lưu vào deck đã chọn",
                color = cs.onSurfaceVariant,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(16.dp))

            if (decks.isEmpty()) {
                Text(
                    "Chưa có deck nào. Hãy tạo deck trước!",
                    color = cs.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                decks.forEach { deck ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onDeckSelected(deck.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Style,
                                null,
                                tint = cs.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    deck.name,
                                    color = cs.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    "${deck.cardCount} thẻ",
                                    color = cs.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ════════════════════════════════════════════════════
//  Typing Indicator
// ════════════════════════════════════════════════════

@Composable
private fun TypingIndicator() {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape)
                .background(Color(0xFF6366F1).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Card(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF6366F1), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Đang suy nghĩ...", color = cs.onSurfaceVariant, fontSize = 14.sp)
            }
        }
    }
}

// ════════════════════════════════════════════════════
//  Input Bar
// ════════════════════════════════════════════════════

@Composable
private fun ChatInputBar(
    inputText: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cs.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            placeholder = { Text("Nhập câu hỏi...", color = cs.onSurfaceVariant) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            singleLine = false,
            maxLines = 3,
            enabled = !isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = cs.onSurface,
                unfocusedTextColor = cs.onSurface,
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = cs.outlineVariant,
                focusedContainerColor = cs.surfaceContainer,
                unfocusedContainerColor = cs.surfaceContainer,
                cursorColor = Color(0xFF6366F1)
            )
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onSend,
            enabled = inputText.isNotBlank() && !isLoading,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (inputText.isNotBlank() && !isLoading)
                        Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))
                    else
                        Brush.linearGradient(listOf(cs.outlineVariant.copy(alpha = 0.3f), cs.outlineVariant.copy(alpha = 0.3f)))
                )
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, "Gửi", tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

// ════════════════════════════════════════════════════
//  Markdown Parser (reused from AiTutorScreen)
// ════════════════════════════════════════════════════

private fun parseMarkdown(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < text.length) {
        when {
            i + 1 < text.length && text[i] == '*' && text[i + 1] == '*' -> {
                val end = text.indexOf("**", i + 2)
                if (end > 0) {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    builder.append(text.substring(i + 2, end))
                    builder.pop()
                    i = end + 2
                } else { builder.append(text[i]); i++ }
            }
            text[i] == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end > 0) {
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    builder.append(text.substring(i + 1, end))
                    builder.pop()
                    i = end + 1
                } else { builder.append(text[i]); i++ }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > 0) {
                    builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                    builder.append(text.substring(i + 1, end))
                    builder.pop()
                    i = end + 1
                } else { builder.append(text[i]); i++ }
            }
            else -> { builder.append(text[i]); i++ }
        }
    }
    return builder.toAnnotatedString()
}
