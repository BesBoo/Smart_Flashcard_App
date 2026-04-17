package com.example.myapplication.domain.model

import org.junit.Assert.*
import org.junit.Test

class StudyQueueTest {

    private fun makeCard(id: String, isNew: Boolean = false) = Flashcard(
        id = id, userId = "u1", deckId = "d1",
        frontText = "F", backText = "B",
        sm2 = if (isNew) SM2Data() else SM2Data(totalReviews = 3, repetition = 1, intervalDays = 1, easeFactor = 2.5, nextReviewDate = 0)
    )

    @Test
    fun `totalCount returns correct card count`() {
        val queue = StudyQueue(
            cards = listOf(makeCard("1"), makeCard("2"), makeCard("3")),
            newCardCount = 1, reviewCardCount = 2
        )
        assertEquals(3, queue.totalCount)
    }

    @Test
    fun `isEmpty returns true for empty queue`() {
        assertTrue(StudyQueue.empty().isEmpty)
    }

    @Test
    fun `isEmpty returns false for non-empty queue`() {
        val queue = StudyQueue(cards = listOf(makeCard("1")), newCardCount = 0, reviewCardCount = 1)
        assertFalse(queue.isEmpty)
    }

    @Test
    fun `remaining never returns negative`() {
        val queue = StudyQueue(cards = listOf(makeCard("1")), newCardCount = 0, reviewCardCount = 1)
        assertEquals(0, queue.remaining(100))
    }

    @Test
    fun `remaining gives correct value`() {
        val queue = StudyQueue(
            cards = (1..10).map { makeCard("$it") },
            newCardCount = 3, reviewCardCount = 7
        )
        assertEquals(7, queue.remaining(3))
    }

    @Test
    fun `constants have expected values`() {
        assertEquals(40, StudyQueue.MAX_NEW_CARDS_PER_DAY)
        assertEquals(150, StudyQueue.MAX_REVIEW_CARDS_PER_DAY)
    }
}

class SM2DataTest {

    @Test
    fun `default SM2Data has correct initial values`() {
        val data = SM2Data()
        assertEquals(0, data.repetition)
        assertEquals(1, data.intervalDays)
        assertEquals(2.5, data.easeFactor, 0.001)
        assertEquals(0, data.failCount)
        assertEquals(0, data.totalReviews)
    }

    @Test
    fun `isNew returns true for zero reviews`() {
        assertTrue(SM2Data().isNew)
    }

    @Test
    fun `isNew returns false for reviewed card`() {
        assertFalse(SM2Data(totalReviews = 1).isNew)
    }

    @Test
    fun `isDue returns true for past review date`() {
        val data = SM2Data(nextReviewDate = System.currentTimeMillis() - 86400000)
        assertTrue(data.isDue)
    }

    @Test
    fun `isDue returns false for future review date`() {
        val data = SM2Data(nextReviewDate = System.currentTimeMillis() + 86400000)
        assertFalse(data.isDue)
    }

    @Test
    fun `isStruggling returns true for failCount 3 or more`() {
        assertTrue(SM2Data(failCount = 3).isStruggling)
        assertTrue(SM2Data(failCount = 5).isStruggling)
    }

    @Test
    fun `isStruggling returns false for low fail count`() {
        assertFalse(SM2Data(failCount = 0).isStruggling)
        assertFalse(SM2Data(failCount = 2).isStruggling)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `easeFactor below 1_3 throws`() {
        SM2Data(easeFactor = 1.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `intervalDays below 1 throws`() {
        SM2Data(intervalDays = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative repetition throws`() {
        SM2Data(repetition = -1)
    }
}

class ReviewQualityTest {

    @Test
    fun `fromValue returns correct enum`() {
        assertEquals(ReviewQuality.AGAIN, ReviewQuality.fromValue(0))
        assertEquals(ReviewQuality.HARD, ReviewQuality.fromValue(2))
        assertEquals(ReviewQuality.GOOD, ReviewQuality.fromValue(3))
        assertEquals(ReviewQuality.EASY, ReviewQuality.fromValue(5))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fromValue throws for invalid value`() {
        ReviewQuality.fromValue(4)
    }

    @Test
    fun `fromValueOrNull returns null for invalid`() {
        assertNull(ReviewQuality.fromValueOrNull(1))
        assertNull(ReviewQuality.fromValueOrNull(4))
    }

    @Test
    fun `Vietnamese labels are correct`() {
        assertEquals("Học lại", ReviewQuality.AGAIN.labelVi)
        assertEquals("Khó", ReviewQuality.HARD.labelVi)
        assertEquals("Tốt", ReviewQuality.GOOD.labelVi)
        assertEquals("Dễ", ReviewQuality.EASY.labelVi)
    }

    @Test
    fun `getLabel returns Vietnamese by default`() {
        assertEquals("Tốt", ReviewQuality.getLabel(ReviewQuality.GOOD))
    }

    @Test
    fun `getLabel returns English`() {
        assertEquals("Good", ReviewQuality.getLabel(ReviewQuality.GOOD, "en"))
    }
}

class StudySessionSummaryTest {

    @Test
    fun `accuracy is correct`() {
        val summary = StudySessionSummary(
            totalCards = 10, correctCount = 7, incorrectCount = 3,
            averageResponseTimeMs = 5000, totalTimeMs = 50000,
            newCardsStudied = 3, reviewCardsStudied = 7
        )
        assertEquals(0.7f, summary.accuracy, 0.001f)
        assertEquals(70, summary.accuracyPercent)
    }

    @Test
    fun `accuracy is zero for no cards`() {
        val summary = StudySessionSummary(
            totalCards = 0, correctCount = 0, incorrectCount = 0,
            averageResponseTimeMs = 0, totalTimeMs = 0,
            newCardsStudied = 0, reviewCardsStudied = 0
        )
        assertEquals(0f, summary.accuracy, 0.001f)
    }
}
