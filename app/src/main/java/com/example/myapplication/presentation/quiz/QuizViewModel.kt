package com.example.myapplication.presentation.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.model.ReviewQuality
import com.example.myapplication.domain.repository.FlashcardRepository
import com.example.myapplication.domain.repository.QuizQuestion
import com.example.myapplication.domain.repository.TypingQuestion
import com.example.myapplication.domain.usecase.ReviewCardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

enum class QuizPhase {
    MULTIPLE_CHOICE,  // Part 1: select correct answer
    TYPING            // Part 2: type the answer
}

data class QuizUiState(
    val isLoading: Boolean = true,
    val phase: QuizPhase = QuizPhase.MULTIPLE_CHOICE,

    // ── Part 1: Multiple Choice ──
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: Int? = null,
    val isAnswerRevealed: Boolean = false,
    val correctCount: Int = 0,
    val isPhase1Finished: Boolean = false,

    // ── Part 2: Typing ──
    val typingQuestions: List<TypingQuestion> = emptyList(),
    val typingIndex: Int = 0,
    val typingInput: String = "",
    val isTypingRevealed: Boolean = false,
    val isTypingCorrect: Boolean = false,
    val typingCorrectCount: Int = 0,

    // ── Overall ──
    val isFinished: Boolean = false,
    val error: String? = null
) {
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(currentIndex)
    val currentTypingQuestion: TypingQuestion? get() = typingQuestions.getOrNull(typingIndex)
    val totalQuestions: Int get() = questions.size
    val totalTypingQuestions: Int get() = typingQuestions.size
    val progress: Float get() = when (phase) {
        QuizPhase.MULTIPLE_CHOICE ->
            if (totalQuestions > 0) (currentIndex + 1).toFloat() / totalQuestions else 0f
        QuizPhase.TYPING ->
            if (totalTypingQuestions > 0) (typingIndex + 1).toFloat() / totalTypingQuestions else 0f
    }
    val totalCorrect: Int get() = correctCount + typingCorrectCount
    val totalAll: Int get() = totalQuestions + totalTypingQuestions
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val flashcardRepository: FlashcardRepository,
    private val reviewCardUseCase: ReviewCardUseCase
) : ViewModel() {

    private val deckId: String = savedStateHandle["deckId"] ?: ""

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var loadedCards: List<Flashcard> = emptyList()

    init {
        loadQuiz()
    }

    private fun loadQuiz() {
        viewModelScope.launch {
            try {
                val cards = flashcardRepository.observeFlashcardsByDeck(deckId).first()
                if (cards.size < 4) {
                    _uiState.update { it.copy(isLoading = false, error = "Cần ít nhất 4 thẻ để tạo quiz") }
                    return@launch
                }
                loadedCards = cards

                val questionCount = minOf(10, cards.size)

                // Generate multiple-choice questions LOCALLY (no API call!)
                val questions = generateMultipleChoiceQuestions(cards, questionCount)

                // Generate typing questions from cards
                val typingQuestions = generateTypingQuestions(cards, questionCount)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        phase = QuizPhase.MULTIPLE_CHOICE,
                        questions = questions,
                        typingQuestions = typingQuestions,
                        error = if (questions.isEmpty()) "Không thể tạo câu hỏi" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.localizedMessage ?: "Đã xảy ra lỗi")
                }
            }
        }
    }

    /**
     * Generate multiple-choice questions LOCALLY from flashcards.
     * Each question shows the front text and 4 answer options (1 correct + 3 distractors).
     * Questions randomly choose direction: front→back or back→front.
     */
    private fun generateMultipleChoiceQuestions(cards: List<Flashcard>, count: Int): List<QuizQuestion> {
        val selectedCards = cards.shuffled().take(count)
        return selectedCards.mapNotNull { card ->
            val showFrontAsQuestion = (0..1).random() == 0
            val questionText: String
            val correctAnswer: String

            if (showFrontAsQuestion) {
                questionText = "\"${card.frontText}\" nghĩa là gì?"
                correctAnswer = card.backText
            } else {
                questionText = "\"${card.backText}\" trong tiếng Anh là gì?"
                correctAnswer = card.frontText
            }

            // Get 3 distractors from other cards
            val distractorPool = cards.filter { it.id != card.id }
            if (distractorPool.size < 3) return@mapNotNull null

            val distractors = distractorPool.shuffled().take(3).map {
                if (showFrontAsQuestion) it.backText else it.frontText
            }

            // Build options with correct answer at random position
            val options = (distractors + correctAnswer).shuffled()
            val correctIndex = options.indexOf(correctAnswer)

            QuizQuestion(
                questionText = questionText,
                options = options,
                correctIndex = correctIndex,
                sourceCardId = card.id
            )
        }
    }

    /**
     * Generate typing questions from flashcards:
     * Always show back text (Vietnamese meaning) as prompt,
     * user must type the front text (English).
     */
    private fun generateTypingQuestions(cards: List<Flashcard>, count: Int): List<TypingQuestion> {
        return cards.shuffled().take(count).map { card ->
            TypingQuestion(
                promptText = card.backText,
                correctAnswer = stripWordType(card.frontText),
                sourceCardId = card.id
            )
        }
    }

    // ═══════ Part 1: Multiple Choice ═══════

    fun selectAnswer(index: Int) {
        if (_uiState.value.isAnswerRevealed) return
        val isCorrect = index == _uiState.value.currentQuestion?.correctIndex
        _uiState.update {
            it.copy(
                selectedAnswer = index,
                isAnswerRevealed = true,
                correctCount = if (isCorrect) it.correctCount + 1 else it.correctCount
            )
        }
        // Update SM2 scheduling for the answered card
        val cardId = _uiState.value.currentQuestion?.sourceCardId ?: return
        val card = loadedCards.find { it.id == cardId } ?: return
        val quality = if (isCorrect) ReviewQuality.GOOD else ReviewQuality.AGAIN
        viewModelScope.launch {
            try { reviewCardUseCase(card, quality) } catch (_: Exception) {}
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state.currentIndex + 1 >= state.totalQuestions) {
            // Part 1 finished → move to Part 2
            _uiState.update {
                it.copy(
                    isPhase1Finished = true,
                    phase = QuizPhase.TYPING,
                    selectedAnswer = null,
                    isAnswerRevealed = false
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = it.currentIndex + 1,
                    selectedAnswer = null,
                    isAnswerRevealed = false
                )
            }
        }
    }

    // ═══════ Part 2: Typing ═══════

    fun updateTypingInput(text: String) {
        _uiState.update { it.copy(typingInput = text) }
    }

    fun submitTypingAnswer() {
        val state = _uiState.value
        val question = state.currentTypingQuestion ?: return
        if (state.isTypingRevealed) return

        val isCorrect = compareMultiLineAnswer(state.typingInput, question.correctAnswer)

        _uiState.update {
            it.copy(
                isTypingRevealed = true,
                isTypingCorrect = isCorrect,
                typingCorrectCount = if (isCorrect) it.typingCorrectCount + 1 else it.typingCorrectCount
            )
        }
        // Update SM2 scheduling for the answered card
        val card = loadedCards.find { it.id == question.sourceCardId } ?: return
        val quality = if (isCorrect) ReviewQuality.GOOD else ReviewQuality.AGAIN
        viewModelScope.launch {
            try { reviewCardUseCase(card, quality) } catch (_: Exception) {}
        }
    }

    /**
     * Compare multi-line answers:
     * 1. Split both into lines
     * 2. Normalize each line (trim, lowercase, strip diacritics)
     * 3. Remove empty lines
     * 4. Compare as SETS (order doesn't matter)
     *
     * Also handles: user types "line1  line2" on one line vs "line1\nline2" in answer
     */
    private fun compareMultiLineAnswer(userInput: String, correctAnswer: String): Boolean {
        val userLines = splitToNormalizedLines(userInput)
        val correctLines = splitToNormalizedLines(correctAnswer)

        // If both have same lines (regardless of order) → correct
        if (userLines.toSet() == correctLines.toSet()) return true

        // Fallback: if user typed everything on one line,
        // join correct lines with space and compare
        val userFlat = normalizeForComparison(userInput)
        val correctFlat = correctLines.sorted().joinToString(" ")
        return userFlat == correctFlat
    }

    private fun splitToNormalizedLines(text: String): List<String> {
        return text.split("\n", "\r\n", "\r")
            .map { normalizeForComparison(it) }
            .filter { it.isNotBlank() }
    }

    fun nextTypingQuestion() {
        val state = _uiState.value
        if (state.typingIndex + 1 >= state.totalTypingQuestions) {
            // All done
            _uiState.update { it.copy(isFinished = true) }
        } else {
            _uiState.update {
                it.copy(
                    typingIndex = it.typingIndex + 1,
                    typingInput = "",
                    isTypingRevealed = false,
                    isTypingCorrect = false
                )
            }
        }
    }

    // ═══════ Common ═══════

    fun restartQuiz() {
        _uiState.update { QuizUiState(isLoading = true) }
        loadQuiz()
    }

    /**
     * Normalize text for comparison:
     * - Strip word type prefix like (n), (adj), (v)
     * - Trim whitespace
     * - Lowercase
     * - Remove diacritical marks (for Vietnamese accent-insensitive comparison)
     * - Collapse multiple spaces
     */
    private fun normalizeForComparison(text: String): String {
        val stripped = stripWordType(text).trim().lowercase()
        val normalized = Normalizer.normalize(stripped, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("đ", "d").replace("Đ", "D")
            .replace(Regex("\\s+"), " ")
        return normalized
    }

    /**
     * Strip word type prefix like (n), (adj), (v), (adv), (prep), etc.
     * Example: "(n) thuật toán" → "thuật toán"
     */
    private fun stripWordType(text: String): String {
        return text.replace(Regex("^\\([a-zA-Z./]+\\)\\s*"), "").trim()
    }
}
