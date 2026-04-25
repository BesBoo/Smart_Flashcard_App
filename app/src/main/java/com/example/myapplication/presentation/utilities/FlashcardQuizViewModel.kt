package com.example.myapplication.presentation.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.Deck
import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.repository.DeckRepository
import com.example.myapplication.domain.repository.FlashcardRepository
import com.example.myapplication.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Quiz question model (local, no AI) ──

data class FlashcardQuizQuestion(
    val frontText: String,
    val correctBack: String,
    val options: List<String>,   // 4 options including the correct one
    val correctIndex: Int
)

data class FlashcardQuizUiState(
    val phase: FlashcardQuizPhase = FlashcardQuizPhase.SETUP,
    val isLoading: Boolean = false,
    val questions: List<FlashcardQuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: Int? = null,
    val isRevealed: Boolean = false,
    val correctCount: Int = 0,
    val error: String? = null,
    // Setup
    val decks: List<Deck> = emptyList(),
    val selectedDeckIds: Set<String> = emptySet(),
    val allDecksSelected: Boolean = true,
    val questionCount: Int = 10
) {
    val currentQuestion: FlashcardQuizQuestion? get() = questions.getOrNull(currentIndex)
    val totalQuestions: Int get() = questions.size
    val progress: Float get() = if (questions.isEmpty()) 0f else (currentIndex.toFloat() / questions.size)
}

enum class FlashcardQuizPhase { SETUP, QUIZ, RESULT }

@HiltViewModel
class FlashcardQuizViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardQuizUiState())
    val uiState: StateFlow<FlashcardQuizUiState> = _uiState.asStateFlow()

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

    fun startQuiz() {
        val state = _uiState.value
        if (state.selectedDeckIds.isEmpty()) {
            _uiState.update { it.copy(error = "Vui lòng chọn ít nhất 1 deck") }
            return
        }

        _uiState.update { it.copy(phase = FlashcardQuizPhase.QUIZ, isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // Gather cards from selected decks
                val allCards = state.selectedDeckIds.flatMap { deckId ->
                    flashcardRepository.getFlashcardsByDeck(deckId)
                }.distinctBy { it.id }
                    .filter { it.frontText.isNotBlank() && it.backText.isNotBlank() }

                if (allCards.size < 4) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Cần ít nhất 4 thẻ để tạo trắc nghiệm",
                            phase = FlashcardQuizPhase.SETUP
                        )
                    }
                    return@launch
                }

                // Build questions locally (no AI needed)
                val questions = generateQuestions(allCards, state.questionCount)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        questions = questions,
                        currentIndex = 0,
                        selectedAnswer = null,
                        isRevealed = false,
                        correctCount = 0
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Lỗi: ${e.localizedMessage}",
                        phase = FlashcardQuizPhase.SETUP
                    )
                }
            }
        }
    }

    /**
     * Generate MCQ questions locally from flashcard data.
     * Question = frontText, correct answer = backText, distractors = other cards' backText
     */
    private fun generateQuestions(cards: List<Flashcard>, count: Int): List<FlashcardQuizQuestion> {
        val selected = cards.shuffled().take(count.coerceAtMost(cards.size))
        return selected.map { card ->
            // Pick 3 random distractors (different from correct answer)
            val distractors = cards
                .filter { it.id != card.id }
                .shuffled()
                .take(3)
                .map { it.backText }

            // Combine correct + distractors, shuffle
            val allOptions = (distractors + card.backText).shuffled()
            val correctIndex = allOptions.indexOf(card.backText)

            FlashcardQuizQuestion(
                frontText = card.frontText,
                correctBack = card.backText,
                options = allOptions,
                correctIndex = correctIndex
            )
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
            _uiState.update { it.copy(phase = FlashcardQuizPhase.RESULT) }
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

    fun restartQuiz() {
        _uiState.update {
            FlashcardQuizUiState(
                phase = FlashcardQuizPhase.SETUP,
                decks = it.decks,
                selectedDeckIds = it.selectedDeckIds,
                allDecksSelected = it.allDecksSelected
            )
        }
    }
}
