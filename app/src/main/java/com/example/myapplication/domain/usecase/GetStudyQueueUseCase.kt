package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.StudyQueue
import com.example.myapplication.domain.repository.FlashcardRepository
import javax.inject.Inject

class GetStudyQueueUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository
) {
    suspend operator fun invoke(
        userId: String,
        deckId: String? = null,
        maxNewCards: Int = StudyQueue.MAX_NEW_CARDS_PER_DAY,
        maxReviewCards: Int = StudyQueue.MAX_REVIEW_CARDS_PER_DAY
    ): StudyQueue {
        // 1. Get Due Review Cards (repo returns domain Flashcard directly)
        val reviewCards = if (deckId != null) {
            flashcardRepository.getDueCardsByDeck(userId, deckId)
        } else {
            flashcardRepository.getDueCards(userId)
        }.take(maxReviewCards)

        // 2. Get New Cards
        val newCards = if (deckId != null) {
            flashcardRepository.getNewCards(deckId, maxNewCards)
        } else {
            emptyList()
        }

        // 3. Combine: Review cards first, then New cards
        val allCards = reviewCards + newCards

        return StudyQueue(
            cards = allCards,
            newCardCount = newCards.size,
            reviewCardCount = reviewCards.size,
            deckId = deckId
        )
    }
}
