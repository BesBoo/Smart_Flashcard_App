package com.example.myapplication.presentation.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.Deck
import com.example.myapplication.domain.model.VariantQuestion
import com.example.myapplication.domain.repository.AiRepository
import com.example.myapplication.domain.repository.DeckRepository
import com.example.myapplication.domain.repository.FlashcardRepository
import com.example.myapplication.domain.repository.SmartReviewInput
import com.example.myapplication.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmartReviewUiState(
    val phase: SmartReviewPhase = SmartReviewPhase.SETUP,
    val isLoading: Boolean = false,
    val questions: List<VariantQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: Int? = null,
    val isRevealed: Boolean = false,
    val correctCount: Int = 0,
    val isFinished: Boolean = false,
    val error: String? = null,
    // Setup phase
    val decks: List<Deck> = emptyList(),
    val selectedDeckIds: Set<String> = emptySet(),
    val allDecksSelected: Boolean = true,
    val questionCount: Int = 10
) {
    val currentQuestion: VariantQuestion? get() = questions.getOrNull(currentIndex)
    val totalQuestions: Int get() = questions.size
    val progress: Float get() = if (questions.isEmpty()) 0f else (currentIndex.toFloat() / questions.size)
}

enum class SmartReviewPhase { SETUP, QUIZ, RESULT }

@HiltViewModel
class SmartReviewViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartReviewUiState())
    val uiState: StateFlow<SmartReviewUiState> = _uiState.asStateFlow()

    init {
        loadDecks()
    }

    private fun loadDecks() {
        viewModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId() ?: return@launch
                val decks = deckRepository.getDecksByUser(userId)
                _uiState.update {
                    it.copy(
                        decks = decks,
                        selectedDeckIds = decks.map { d -> d.id }.toSet()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun toggleDeckSelection(deckId: String) {
        _uiState.update { state ->
            val newSet = if (deckId in state.selectedDeckIds)
                state.selectedDeckIds - deckId
            else
                state.selectedDeckIds + deckId
            state.copy(
                selectedDeckIds = newSet,
                allDecksSelected = newSet.size == state.decks.size
            )
        }
    }

    fun toggleAllDecks() {
        _uiState.update { state ->
            if (state.allDecksSelected) {
                state.copy(selectedDeckIds = emptySet(), allDecksSelected = false)
            } else {
                state.copy(
                    selectedDeckIds = state.decks.map { it.id }.toSet(),
                    allDecksSelected = true
                )
            }
        }
    }

    fun updateQuestionCount(count: Int) {
        _uiState.update { it.copy(questionCount = count.coerceIn(5, 20)) }
    }

    fun startReview() {
        val state = _uiState.value
        if (state.selectedDeckIds.isEmpty()) {
            _uiState.update { it.copy(error = "Vui lòng chọn ít nhất 1 deck") }
            return
        }

        _uiState.update { it.copy(phase = SmartReviewPhase.QUIZ, isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId() ?: return@launch

                // Gather cards from selected decks
                val allCards = state.selectedDeckIds.flatMap { deckId ->
                    flashcardRepository.getFlashcardsByDeck(deckId)
                }.distinctBy { it.id }

                if (allCards.size < 3) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Cần ít nhất 3 thẻ để bắt đầu ôn tập")
                    }
                    return@launch
                }

                // Pick random cards for review
                val selectedCards = allCards.shuffled().take(state.questionCount.coerceAtMost(allCards.size))

                // Build input for AI
                val words = selectedCards.map { card ->
                    SmartReviewInput(
                        word = card.frontText,
                        partOfSpeech = "",
                        definition = card.backText,
                        sourceCardId = card.id
                    )
                }

                val questions = aiRepository.generateSmartReview(
                    words = words,
                    questionCount = state.questionCount.coerceAtMost(selectedCards.size),
                    language = "en"
                )

                if (questions.isEmpty()) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Không thể tạo câu hỏi. Vui lòng thử lại.")
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        questions = questions,
                        currentIndex = 0,
                        selectedAnswer = null,
                        isRevealed = false,
                        correctCount = 0,
                        isFinished = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Lỗi: ${e.localizedMessage}")
                }
            }
        }
    }

    fun selectAnswer(index: Int) {
        if (_uiState.value.isRevealed) return
        val question = _uiState.value.currentQuestion ?: return
        val isCorrect = index == question.correctIndex

        _uiState.update {
            it.copy(
                selectedAnswer = index,
                isRevealed = true,
                correctCount = it.correctCount + if (isCorrect) 1 else 0
            )
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1

        if (nextIndex >= state.questions.size) {
            _uiState.update { it.copy(phase = SmartReviewPhase.RESULT, isFinished = true) }
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = nextIndex,
                    selectedAnswer = null,
                    isRevealed = false
                )
            }
        }
    }

    fun restartReview() {
        _uiState.update {
            SmartReviewUiState(
                phase = SmartReviewPhase.SETUP,
                decks = it.decks,
                selectedDeckIds = it.selectedDeckIds,
                allDecksSelected = it.allDecksSelected
            )
        }
    }
}
