package com.example.myapplication.presentation.aitutor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AiTutorScreen(
    modifier: Modifier = Modifier,
    viewModel: AiTutorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(cs.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Psychology, null, tint = cs.onPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("AI Tutor", color = cs.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Trợ lý học tập thông minh", color = cs.onSurfaceVariant, fontSize = 12.sp)
            }
        }

        // Messages
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.messages) { msg ->
                ChatBubble(msg)
            }

            // Loading indicator
            if (uiState.isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(cs.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = cs.primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Card(
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = cs.primary, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Đang suy nghĩ...", color = cs.onSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cs.surfaceContainer)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Nhập câu hỏi...", color = cs.onSurfaceVariant) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                singleLine = false,
                maxLines = 3,
                enabled = !uiState.isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = cs.onSurface,
                    unfocusedTextColor = cs.onSurface,
                    focusedBorderColor = cs.primary,
                    unfocusedBorderColor = cs.outlineVariant,
                    focusedContainerColor = cs.surfaceContainer,
                    unfocusedContainerColor = cs.surfaceContainer,
                    cursorColor = cs.primary
                )
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !uiState.isLoading) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank() && !uiState.isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank() && !uiState.isLoading) cs.primary else cs.outlineVariant.copy(alpha = 0.3f))
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Gửi", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatUiMessage) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(cs.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = cs.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        Card(
            modifier = Modifier.widthIn(max = 280.dp),
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
}

/**
 * Parse basic markdown: **bold**, *italic*, `code`
 * Returns styled AnnotatedString.
 */
private fun parseMarkdown(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < text.length) {
        when {
            // **bold**
            i + 1 < text.length && text[i] == '*' && text[i + 1] == '*' -> {
                val end = text.indexOf("**", i + 2)
                if (end > 0) {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    builder.append(text.substring(i + 2, end))
                    builder.pop()
                    i = end + 2
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // *italic* (single *)
            text[i] == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end > 0) {
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    builder.append(text.substring(i + 1, end))
                    builder.pop()
                    i = end + 1
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // `code`
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > 0) {
                    builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                    builder.append(text.substring(i + 1, end))
                    builder.pop()
                    i = end + 1
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            else -> {
                builder.append(text[i])
                i++
            }
        }
    }
    return builder.toAnnotatedString()
}
