package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.model.SM2Data
import com.example.myapplication.domain.model.StudyQueue
import com.example.myapplication.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GetStudyQueueUseCaseTest {

    private lateinit var useCase: GetStudyQueueUseCase

    private val dueCards = (1..10).map { i ->
        Flashcard(
            id = "due_$i", userId = "u1", deckId = "d1",
            frontText = "Front $i", backText = "Back $i",
            sm2 = SM2Data(
                repetition = 1, intervalDays = 1, easeFactor = 2.5,
                nextReviewDate = System.currentTimeMillis() - 86400000, // yesterday
                totalReviews = i
            )
        )
    }

    private val newCards = (1..50).map { i ->
        Flashcard(
            id = "new_$i", userId = "u1", deckId = "d1",
            frontText = "NewFront $i", backText = "NewBack $i",
            sm2 = SM2Data()
        )
    }

    @Before
    fun setup() {
        useCase = GetStudyQueueUseCase(FakeFlashcardRepository(dueCards, newCards))
    }

    @Test
    fun `invoke with deckId returns review cards first then new cards`() = runBlocking {
        val queue = useCase(userId = "u1", deckId = "d1")

        assertTrue(queue.reviewCardCount > 0)
        assertTrue(queue.newCardCount > 0)
        // Review cards come before new cards
        val firstNewIndex = queue.cards.indexOfFirst { it.sm2.totalReviews == 0 }
        val lastReviewIndex = queue.cards.indexOfLast { it.sm2.totalReviews > 0 }
        if (firstNewIndex >= 0 && lastReviewIndex >= 0) {
            assertTrue("Review cards should come before new cards", lastReviewIndex < firstNewIndex)
        }
    }

    @Test
    fun `invoke respects max new cards limit`() = runBlocking {
        val queue = useCase(userId = "u1", deckId = "d1", maxNewCards = 5)
        assertTrue(queue.newCardCount <= 5)
    }

    @Test
    fun `invoke respects max review cards limit`() = runBlocking {
        val queue = useCase(userId = "u1", deckId = "d1", maxReviewCards = 3)
        assertTrue(queue.reviewCardCount <= 3)
    }

    @Test
    fun `invoke with default limits uses StudyQueue constants`() = runBlocking {
        val queue = useCase(userId = "u1", deckId = "d1")
        assertTrue(queue.newCardCount <= StudyQueue.MAX_NEW_CARDS_PER_DAY)
        assertTrue(queue.reviewCardCount <= StudyQueue.MAX_REVIEW_CARDS_PER_DAY)
    }

    @Test
    fun `invoke without deckId returns empty new cards`() = runBlocking {
        val queue = useCase(userId = "u1", deckId = null)
        assertEquals(0, queue.newCardCount)
        assertTrue(queue.reviewCardCount > 0)
    }

    @Test
    fun `empty queue returns isEmpty true`() = runBlocking {
        val emptyUseCase = GetStudyQueueUseCase(FakeFlashcardRepository(emptyList(), emptyList()))
        val queue = emptyUseCase(userId = "u1", deckId = "d1")
        assertTrue(queue.isEmpty)
        assertEquals(0, queue.totalCount)
    }

    @Test
    fun `queue remaining calculation is correct`() = runBlocking {
        val queue = useCase(userId = "u1", deckId = "d1")
        val total = queue.totalCount
        assertEquals(total - 3, queue.remaining(3))
        assertEquals(0, queue.remaining(total + 10)) // never negative
    }
}

// ── Fake Repository ──────────────────────────────────
private class FakeFlashcardRepository(
    private val due: List<Flashcard>,
    private val new: List<Flashcard>
) : FlashcardRepository {
    override fun observeFlashcardsByDeck(deckId: String): Flow<List<Flashcard>> = flowOf(due + new)
    override suspend fun getFlashcardsByDeck(deckId: String) = due + new
    override suspend fun getFlashcardById(cardId: String) = null
    override suspend fun getDueCards(userId: String) = due
    override suspend fun getDueCardsByDeck(userId: String, deckId: String) = due
    override suspend fun getAllCardsByUser(userId: String) = due + new
    override suspend fun getNewCards(deckId: String, limit: Int) = new.take(limit)
    override fun observeDueCardCount(userId: String): Flow<Int> = flowOf(due.size)
    override suspend fun createFlashcard(flashcard: Flashcard) {}
    override suspend fun updateFlashcard(flashcard: Flashcard) {}
    override suspend fun updateSm2Fields(cardId: String, repetition: Int, intervalDays: Int, easeFactor: Double, nextReviewDate: Long, quality: Int) {}
    override suspend fun deleteFlashcard(cardId: String) {}
    override suspend fun syncFlashcards(userId: String) {}
}
