package com.example.myapplication.presentation.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.model.ReviewQuality
import com.example.myapplication.domain.model.StudySessionSummary
import com.example.myapplication.domain.repository.AiRepository
import com.example.myapplication.domain.repository.AdaptiveHintResult
import com.example.myapplication.domain.repository.FlashcardRepository
import com.example.myapplication.domain.repository.UserRepository
import com.example.myapplication.domain.usecase.ReviewCardUseCase
import com.example.myapplication.presentation.catpet.CatHungerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyUiState(
    val isLoading: Boolean = true,
    val cards: List<Flashcard> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val isSessionComplete: Boolean = false,
    val summary: StudySessionSummary? = null,
    val error: String? = null,
    // Tracking for summary
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val startTimeMs: Long = System.currentTimeMillis(),
    val cardStartTimeMs: Long = System.currentTimeMillis(),
    val difficultCards: List<Flashcard> = emptyList(),
    // AI-16: Struggling card hint
    val showAiHintDialog: Boolean = false,
    val aiHintLoading: Boolean = false,
    val aiHintResult: AdaptiveHintResult? = null,
    val aiHintCard: Flashcard? = null
) {
    val currentCard: Flashcard? get() = cards.getOrNull(currentIndex)
    val progress: Float get() = if (cards.isEmpty()) 0f else (currentIndex.toFloat() / cards.size)
    val cardsRemaining: Int get() = (cards.size - currentIndex).coerceAtLeast(0)
}

@HiltViewModel
class StudySessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val flashcardRepository: FlashcardRepository,
    private val deckRepository: com.example.myapplication.domain.repository.DeckRepository,
    private val reviewCardUseCase: ReviewCardUseCase,
    private val aiRepository: AiRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val deckId: String = savedStateHandle["deckId"] ?: ""

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private val prefs by lazy {
        context.getSharedPreferences("study_settings", android.content.Context.MODE_PRIVATE)
    }

    private val hungerManager by lazy { CatHungerManager(context) }

    init {
        loadCards()
    }

    private fun loadCards() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userId = userRepository.getCurrentUserId() ?: ""
                val maxReview = prefs.getInt("review_cards_per_day", 150)
                val maxNew = prefs.getInt("new_cards_per_day", 40)

                // Refresh cards from server for shared deck sync
                if (deckId.isNotBlank() && deckId != "all") {
                    try {
                        deckRepository.syncFlashcardsForDeck(deckId, userId)
                    } catch (_: Exception) { /* offline */ }
                }

                val dueCards = if (deckId.isNotBlank() && deckId != "all") {
                    flashcardRepository.getDueCardsByDeck(userId, deckId)
                } else {
                    flashcardRepository.getDueCards(userId)
                }.take(maxReview)

                // Also include new cards if there's room
                val newCards = if (deckId.isNotBlank() && deckId != "all") {
                    flashcardRepository.getNewCards(deckId, maxNew)
                } else if (dueCards.isEmpty()) {
                    flashcardRepository.getNewCards("", maxNew)
                } else {
                    emptyList()
                }

                // Combine: Review cards first, then New cards
                val allCards = dueCards + newCards

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cards = allCards.shuffled(),
                        currentIndex = 0,
                        isFlipped = false,
                        isSessionComplete = false,
                        summary = null,
                        correctCount = 0,
                        incorrectCount = 0,
                        difficultCards = emptyList(),
                        startTimeMs = System.currentTimeMillis(),
                        cardStartTimeMs = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun flipCard() {
        _uiState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    fun answerCard(quality: ReviewQuality) {
        val currentState = _uiState.value
        val card = currentState.currentCard ?: return

        val responseTime = System.currentTimeMillis() - currentState.cardStartTimeMs
        val isCorrect = quality.value >= 3
        val isDifficult = quality.value <= 2

        // ── OPTIMISTIC: Advance UI immediately ──
        val newCorrect = currentState.correctCount + if (isCorrect) 1 else 0
        val newIncorrect = currentState.incorrectCount + if (!isCorrect) 1 else 0
        val updatedDifficult = if (isDifficult) {
            currentState.difficultCards + card
        } else {
            currentState.difficultCards
        }
        val nextIndex = currentState.currentIndex + 1

        if (nextIndex >= currentState.cards.size) {
            val totalTime = System.currentTimeMillis() - currentState.startTimeMs
            val avgTime = if (currentState.cards.isNotEmpty()) totalTime / currentState.cards.size else 0L

            _uiState.update {
                it.copy(
                    isSessionComplete = true,
                    correctCount = newCorrect,
                    incorrectCount = newIncorrect,
                    difficultCards = updatedDifficult,
                    summary = StudySessionSummary(
                        totalCards = currentState.cards.size,
                        correctCount = newCorrect,
                        incorrectCount = newIncorrect,
                        averageResponseTimeMs = avgTime,
                        totalTimeMs = totalTime,
                        newCardsStudied = currentState.cards.count { c -> c.isNew },
                        reviewCardsStudied = currentState.cards.count { c -> !c.isNew }
                    )
                )
            }

            // Pet motivation: reward fish based on cards studied
            hungerManager.onSessionComplete(currentState.cards.size)
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = nextIndex,
                    isFlipped = false,
                    correctCount = newCorrect,
                    incorrectCount = newIncorrect,
                    difficultCards = updatedDifficult,
                    cardStartTimeMs = System.currentTimeMillis()
                )
            }
        }

        // ── BACKGROUND: Process SM-2 + sync to server (non-blocking) ──
        viewModelScope.launch {
            try {
                val adaptiveResult = reviewCardUseCase(card, quality, responseTime)

                if (adaptiveResult.shouldTriggerAiHint) {
                    _uiState.update {
                        it.copy(
                            showAiHintDialog = true,
                            aiHintCard = card,
                            aiHintResult = null,
                            aiHintLoading = false
                        )
                    }
                }
            } catch (_: Exception) {
                // Log but don't block the session
            }

            hungerManager.onCardReviewed()
        }
    }

    // ── AI-16: Load adaptive hint from AI ──
    fun loadAiHint() {
        val card = _uiState.value.aiHintCard ?: return
        _uiState.update { it.copy(aiHintLoading = true) }

        viewModelScope.launch {
            try {
                val hint = aiRepository.getAdaptiveHint(card)
                _uiState.update {
                    it.copy(aiHintLoading = false, aiHintResult = hint)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        aiHintLoading = false,
                        aiHintResult = AdaptiveHintResult(
                            simplifiedExplanation = "Không thể tải gợi ý AI: ${e.localizedMessage}",
                            additionalExamples = emptyList()
                        )
                    )
                }
            }
        }
    }

    fun dismissAiHint() {
        _uiState.update {
            it.copy(showAiHintDialog = false, aiHintResult = null, aiHintCard = null)
        }
    }

    fun restartSession() {
        val difficult = _uiState.value.difficultCards
        if (difficult.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    cards = difficult.shuffled(),
                    currentIndex = 0,
                    isFlipped = false,
                    isSessionComplete = false,
                    summary = null,
                    correctCount = 0,
                    incorrectCount = 0,
                    difficultCards = emptyList(),
                    startTimeMs = System.currentTimeMillis(),
                    cardStartTimeMs = System.currentTimeMillis()
                )
            }
        } else {
            loadCards()
        }
    }
}
