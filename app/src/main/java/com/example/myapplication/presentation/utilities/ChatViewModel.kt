package com.example.myapplication.presentation.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.Deck
import com.example.myapplication.domain.model.VocabSuggestion
import com.example.myapplication.domain.repository.AiChatMessage
import com.example.myapplication.domain.repository.DeckRepository
import com.example.myapplication.domain.repository.UserRepository
import com.example.myapplication.domain.usecase.AiTutorUseCase
import com.example.myapplication.domain.usecase.CreateFlashcardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

// ── UI models ──

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val vocabSuggestions: List<VocabSuggestion> = emptyList(),
    val savedWords: List<String> = emptyList()  // words already saved as cards
)

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            text = "Xin chào! Tôi là AI Tutor 🤖. Bạn cần giúp gì trong việc học tập hôm nay?",
            isUser = false
        ),
        ChatMessage(
            text = "Tôi có thể:\n• Giải thích từ vựng\n• Cho ví dụ câu\n• Giải đáp thắc mắc\n• Gợi ý cách ghi nhớ\n\n💡 Khi tôi phát hiện từ vựng hay, bạn có thể lưu thành flashcard ngay!",
            isUser = false
        )
    ),
    val isLoading: Boolean = false,
    val error: String? = null,
    // Deck selection for saving cards
    val showDeckSelector: Boolean = false,
    val decks: List<Deck> = emptyList(),
    val pendingSaveVocab: VocabSuggestion? = null,
    val pendingSaveMessageId: String? = null,
    val savingCard: Boolean = false,
    val savedToast: String? = null
)

// ── JSON parsing for ---VOCAB--- ──

@Serializable
private data class VocabDto(
    val word: String = "",
    val pronunciation: String? = null,
    val partOfSpeech: String = "",
    val definitionEn: String = "",
    val definitionVi: String = "",
    val example: String = ""
)

private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiTutorUseCase: AiTutorUseCase,
    private val createFlashcardUseCase: CreateFlashcardUseCase,
    private val deckRepository: DeckRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val sessionId = UUID.randomUUID().toString()
    private val chatHistory = mutableListOf<AiChatMessage>()

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        // Add user message
        _uiState.update {
            it.copy(
                messages = it.messages + ChatMessage(text = message.trim(), isUser = true),
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
                    history = chatHistory.takeLast(10)
                )

                // Parse response for vocab suggestions
                val (chatText, vocabs) = parseVocabFromResponse(response)

                chatHistory.add(AiChatMessage("assistant", chatText))

                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatMessage(
                            text = chatText,
                            isUser = false,
                            vocabSuggestions = vocabs
                        ),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatMessage(
                            text = "Xin lỗi, đã xảy ra lỗi. Vui lòng thử lại. 😔",
                            isUser = false
                        ),
                        isLoading = false,
                        error = e.localizedMessage
                    )
                }
            }
        }
    }

    // ── Save card flow ──

    fun onSaveCardClick(vocab: VocabSuggestion, messageId: String) {
        viewModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId() ?: return@launch
                val decks = deckRepository.getDecksByUser(userId)
                _uiState.update {
                    it.copy(
                        showDeckSelector = true,
                        decks = decks,
                        pendingSaveVocab = vocab,
                        pendingSaveMessageId = messageId
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Không thể tải danh sách deck") }
            }
        }
    }

    fun onDeckSelected(deckId: String) {
        val vocab = _uiState.value.pendingSaveVocab ?: return
        val messageId = _uiState.value.pendingSaveMessageId ?: return

        _uiState.update { it.copy(savingCard = true, showDeckSelector = false) }

        viewModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId() ?: return@launch

                // Build card content
                val backText = buildString {
                    if (vocab.partOfSpeech.isNotBlank()) append("(${vocab.partOfSpeech}) ")
                    if (vocab.definitionVi.isNotBlank()) append(vocab.definitionVi)
                    else append(vocab.definitionEn)
                    if (vocab.pronunciation != null) append("\n${vocab.pronunciation}")
                }

                createFlashcardUseCase(
                    userId = userId,
                    deckId = deckId,
                    frontText = vocab.word,
                    backText = backText,
                    exampleText = vocab.example.ifBlank { null }
                )

                // Mark word as saved in the message
                val selectedDeck = _uiState.value.decks.find { it.id == deckId }
                _uiState.update { state ->
                    state.copy(
                        savingCard = false,
                        savedToast = "Đã lưu \"${vocab.word}\" vào ${selectedDeck?.name ?: "deck"}",
                        pendingSaveVocab = null,
                        pendingSaveMessageId = null,
                        messages = state.messages.map { msg ->
                            if (msg.id == messageId) {
                                msg.copy(savedWords = msg.savedWords + vocab.word)
                            } else msg
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        savingCard = false,
                        error = "Lưu thất bại: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun dismissDeckSelector() {
        _uiState.update {
            it.copy(showDeckSelector = false, pendingSaveVocab = null, pendingSaveMessageId = null)
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(savedToast = null) }
    }

    fun toggleVocabSelection(messageId: String, word: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(
                            vocabSuggestions = msg.vocabSuggestions.map { v ->
                                if (v.word == word) v.copy(isSelected = !v.isSelected) else v
                            }
                        )
                    } else msg
                }
            )
        }
    }

    // ── Parser ──

    private fun parseVocabFromResponse(response: String): Pair<String, List<VocabSuggestion>> {
        val marker = "---VOCAB---"
        if (!response.contains(marker)) {
            return response to emptyList()
        }

        val parts = response.split(marker, limit = 2)
        val chatText = parts[0].trim()
        val vocabJsonRaw = parts.getOrNull(1)?.trim() ?: return chatText to emptyList()

        return try {
            val dtos = lenientJson.decodeFromString<List<VocabDto>>(vocabJsonRaw)
            val vocabs = dtos.map { dto ->
                VocabSuggestion(
                    word = dto.word,
                    pronunciation = dto.pronunciation,
                    partOfSpeech = dto.partOfSpeech,
                    definitionEn = dto.definitionEn,
                    definitionVi = dto.definitionVi,
                    example = dto.example
                )
            }
            chatText to vocabs
        } catch (e: Exception) {
            // JSON parse failed — just return the full text without vocab
            chatText to emptyList()
        }
    }
}
