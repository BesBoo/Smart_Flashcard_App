package com.example.myapplication.domain.model

/**
 * Domain model for a daily study session queue.
 *
 * Built by [GetStudyQueueUseCase] applying the daily limits:
 *   - Max 40 new cards per day
 *   - Max 150 review cards per day
 *
 * Cards are ordered: review cards first (oldest due first), then new cards.
 */
data class StudyQueue(
    val cards: List<Flashcard>,
    val newCardCount: Int,
    val reviewCardCount: Int,
    val deckId: String? = null,     // null = all decks
    val deckName: String? = null
) {
    /** Total number of cards in the queue */
    val totalCount: Int get() = cards.size

    /** True if there are no cards to study */
    val isEmpty: Boolean get() = cards.isEmpty()

    /** How many cards remain after some have been studied */
    fun remaining(completedCount: Int): Int =
        (totalCount - completedCount).coerceAtLeast(0)

    companion object {
        /** Default daily limits per Phase 1 requirements */
        const val MAX_NEW_CARDS_PER_DAY = 40
        const val MAX_REVIEW_CARDS_PER_DAY = 150

        /** Create an empty queue */
        fun empty(deckId: String? = null) = StudyQueue(
            cards = emptyList(),
            newCardCount = 0,
            reviewCardCount = 0,
            deckId = deckId
        )
    }
}
