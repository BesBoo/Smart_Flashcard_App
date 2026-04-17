package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.engine.ConfidenceLevel
import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.model.ReviewLog
import com.example.myapplication.domain.model.ReviewQuality
import com.example.myapplication.domain.model.SM2Data
import com.example.myapplication.domain.repository.FlashcardRepository
import com.example.myapplication.domain.repository.ReviewLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ReviewCardUseCaseTest {

    private lateinit var useCase: ReviewCardUseCase
    private lateinit var fakeFlashcardRepo: FakeFlashcardRepo
    private lateinit var fakeReviewLogRepo: FakeReviewLogRepo

    private val testCard = Flashcard(
        id = "card1", userId = "u1", deckId = "d1",
        frontText = "Hello", backText = "Xin chào",
        sm2 = SM2Data(repetition = 1, intervalDays = 1, easeFactor = 2.5, nextReviewDate = 0)
    )

    @Before
    fun setup() {
        fakeFlashcardRepo = FakeFlashcardRepo()
        fakeReviewLogRepo = FakeReviewLogRepo()
        useCase = ReviewCardUseCase(fakeFlashcardRepo, fakeReviewLogRepo)
    }

    @Test
    fun `GOOD quality advances repetition`() = runBlocking {
        val result = useCase(testCard, ReviewQuality.GOOD)
        assertFalse(result.sm2Result.wasReset)
        assertEquals(2, result.sm2Result.sm2Data.repetition)
    }

    @Test
    fun `AGAIN quality resets repetition`() = runBlocking {
        val result = useCase(testCard, ReviewQuality.AGAIN)
        assertTrue(result.sm2Result.wasReset)
        assertEquals(0, result.sm2Result.sm2Data.repetition)
        assertEquals(1, result.sm2Result.sm2Data.failCount)
    }

    @Test
    fun `EASY quality increases ease factor`() = runBlocking {
        val result = useCase(testCard, ReviewQuality.EASY)
        assertTrue(result.sm2Result.sm2Data.easeFactor > 2.5)
    }

    @Test
    fun `HARD quality decreases ease factor but stays above 1_3`() = runBlocking {
        val result = useCase(testCard, ReviewQuality.HARD)
        assertTrue(result.sm2Result.sm2Data.easeFactor >= 1.3)
        assertTrue(result.sm2Result.sm2Data.easeFactor < 2.5)
    }

    @Test
    fun `review log is saved after processing`() = runBlocking {
        useCase(testCard, ReviewQuality.GOOD)
        assertEquals(1, fakeReviewLogRepo.savedLogs.size)
        assertEquals("card1", fakeReviewLogRepo.savedLogs[0].flashcardId)
        assertEquals(ReviewQuality.GOOD, fakeReviewLogRepo.savedLogs[0].quality)
    }

    @Test
    fun `SM2 fields are updated in repo`() = runBlocking {
        useCase(testCard, ReviewQuality.GOOD)
        assertTrue(fakeFlashcardRepo.sm2Updated)
        assertEquals("card1", fakeFlashcardRepo.lastUpdatedCardId)
    }

    @Test
    fun `slow response caps interval for correct answers`() = runBlocking {
        // Card with long interval
        val longIntervalCard = testCard.copy(
            sm2 = SM2Data(repetition = 5, intervalDays = 30, easeFactor = 2.5, nextReviewDate = 0)
        )
        val result = useCase(longIntervalCard, ReviewQuality.GOOD, responseTimeMs = 15000)
        // Slow response should cap at 14 days
        assertTrue(result.sm2Result.sm2Data.intervalDays <= 14)
    }

    @Test
    fun `triple failure triggers AI hint flag`() = runBlocking {
        val strugglingCard = testCard.copy(
            sm2 = SM2Data(repetition = 0, intervalDays = 1, easeFactor = 1.3, nextReviewDate = 0, failCount = 2)
        )
        val result = useCase(strugglingCard, ReviewQuality.AGAIN)
        assertTrue(result.shouldTriggerAiHint)
        assertEquals(3, result.sm2Result.sm2Data.failCount)
    }

    @Test
    fun `fast correct gives HIGH confidence`() = runBlocking {
        val result = useCase(testCard, ReviewQuality.GOOD, responseTimeMs = 1000)
        assertEquals(ConfidenceLevel.HIGH, result.confidenceLevel)
    }
}

// ── Fake Repositories ──

private class FakeFlashcardRepo : FlashcardRepository {
    var sm2Updated = false
    var lastUpdatedCardId: String? = null

    override fun observeFlashcardsByDeck(deckId: String): Flow<List<Flashcard>> = flowOf(emptyList())
    override suspend fun getFlashcardsByDeck(deckId: String) = emptyList<Flashcard>()
    override suspend fun getFlashcardById(cardId: String) = null
    override suspend fun getDueCards(userId: String) = emptyList<Flashcard>()
    override suspend fun getDueCardsByDeck(userId: String, deckId: String) = emptyList<Flashcard>()
    override suspend fun getAllCardsByUser(userId: String) = emptyList<Flashcard>()
    override suspend fun getNewCards(deckId: String, limit: Int) = emptyList<Flashcard>()
    override fun observeDueCardCount(userId: String): Flow<Int> = flowOf(0)
    override suspend fun createFlashcard(flashcard: Flashcard) {}
    override suspend fun updateFlashcard(flashcard: Flashcard) {}
    override suspend fun updateSm2Fields(cardId: String, repetition: Int, intervalDays: Int, easeFactor: Double, nextReviewDate: Long, quality: Int) {
        sm2Updated = true
        lastUpdatedCardId = cardId
    }
    override suspend fun deleteFlashcard(cardId: String) {}
    override suspend fun syncFlashcards(userId: String) {}
}

private class FakeReviewLogRepo : ReviewLogRepository {
    val savedLogs = mutableListOf<ReviewLog>()

    override suspend fun getReviewLogsByCard(flashcardId: String) = emptyList<ReviewLog>()
    override suspend fun getReviewLogsByDateRange(userId: String, startDate: Long, endDate: Long) = emptyList<ReviewLog>()
    override suspend fun saveReviewLog(reviewLog: ReviewLog) { savedLogs.add(reviewLog) }
    override suspend fun saveReviewLogs(reviewLogs: List<ReviewLog>) { savedLogs.addAll(reviewLogs) }
    override fun observeReviewCountToday(userId: String, todayStart: Long): Flow<Int> = flowOf(0)
    override suspend fun getTotalReviewCount(userId: String) = 0
    override suspend fun getAverageQuality(flashcardId: String) = null
    override suspend fun deleteAllByUser(userId: String) {}
    override suspend fun syncReviewLogs(userId: String) {}
}
