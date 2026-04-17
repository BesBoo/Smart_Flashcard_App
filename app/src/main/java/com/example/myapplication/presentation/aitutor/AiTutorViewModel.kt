package com.example.myapplication.presentation.aitutor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.repository.AiChatMessage
import com.example.myapplication.domain.usecase.AiTutorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiMessage(val text: String, val isUser: Boolean)

data class AiTutorUiState(
    val messages: List<ChatUiMessage> = listOf(
        ChatUiMessage("Xin chào! Tôi là AI Tutor 🤖. Bạn cần giúp gì trong việc học tập hôm nay?", false),
        ChatUiMessage("Tôi có thể:\n• Giải thích từ vựng\n• Cho ví dụ câu\n• Giải đáp thắc mắc\n• Gợi ý cách ghi nhớ\n\nHãy hỏi tôi bất cứ điều gì!", false)
    ),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AiTutorViewModel @Inject constructor(
    private val aiTutorUseCase: AiTutorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiTutorUiState())
    val uiState: StateFlow<AiTutorUiState> = _uiState.asStateFlow()

    private val sessionId = UUID.randomUUID().toString()
    private val chatHistory = mutableListOf<AiChatMessage>()

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        // Add user message immediately
        _uiState.update {
            it.copy(
                messages = it.messages + ChatUiMessage(message.trim(), true),
                isLoading = true,
                error = null
            )
        }

        chatHistory.add(AiChatMessage("user", message.trim()))

        viewModelScope.launch {
            try {
                val response = aiTutorUseCase(
                    sessionId = sessionId,
                    message = message.trim(),
                    history = chatHistory.takeLast(10) // Keep last 10 messages for context
                )

                chatHistory.add(AiChatMessage("assistant", response))

                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatUiMessage(response, false),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatUiMessage(
                            "Xin lỗi, đã xảy ra lỗi. Vui lòng thử lại. 😔",
                            false
                        ),
                        isLoading = false,
                        error = e.localizedMessage
                    )
                }
            }
        }
    }
}
