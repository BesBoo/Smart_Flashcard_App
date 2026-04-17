package com.example.myapplication.domain.engine

import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.model.ReviewLog
import com.example.myapplication.domain.model.ReviewQuality
import com.example.myapplication.domain.model.SM2Data
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [AdaptiveScheduler].
 */
class AdaptiveSchedulerTest {

    private fun makeFlashcard(
        failCount: Int = 0,
        repetition: Int = 0,
        easeFactor: Double = 2.5,
        intervalDays: Int = 1
    ) = Flashcard(
        id = "test-card",
        userId = "test-user",
        deckId = "test-deck",
        frontText = "Q",
        backText = "A",
        sm2 = SM2Data(
            repetition = repetition,
            intervalDays = intervalDays,
            easeFactor = easeFactor,
            failCount = failCount
        )
    )

    private fun makeReviewLog(quality: ReviewQuality, responded: Long = 3000L) = ReviewLog(
        id = "log-${System.nanoTime()}",
        userId = "test-user",
        flashcardId = "test-card",
        quality = quality,
        responseTimeMs = responded
    )

    // ── Struggle detection ──────────────────────────────────

    @Test
    fun `isStruggling returns true when failCount gte 3`() {
        assertTrue(AdaptiveScheduler.isStruggling(makeFlashcard(failCount = 3)))
        assertTrue(AdaptiveScheduler.isStruggling(makeFlashcard(failCount = 5)))
    }

    @Test
    fun `isStruggling returns false when failCount lt 3`() {
        assertFalse(AdaptiveScheduler.isStruggling(makeFlashcard(failCount = 0)))
        assertFalse(AdaptiveScheduler.isStruggling(makeFlashcard(failCount = 2)))
    }

    // ── AI trigger ──────────────────────────────────────────

    @Test
    fun `triggers AI hint when card reaches struggle threshold`() {
        // Card already has 2 failures, one more AGAIN puts it at 3
        val card = makeFlashcard(failCount = 2)
        val result = AdaptiveScheduler.processReview(card, ReviewQuality.AGAIN)

        assertTrue(result.shouldTriggerAiHint)
        assertEquals(3, result.sm2Result.sm2Data.failCount)
    }

    @Test
    fun `does not trigger AI hint for normal cards`() {
        val card = makeFlashcard(failCount = 0)
        val result = AdaptiveScheduler.processReview(card, ReviewQuality.GOOD)

        assertFalse(result.shouldTriggerAiHint)
    }

    // ── Response time analysis ──────────────────────────────

    @Test
    fun `caps interval for correct but slow responses`() {
        // Card with long interval
        val card = makeFlashcard(repetition = 4, intervalDays = 30, easeFactor = 2.5)
        val result = AdaptiveScheduler.processReview(
            card, ReviewQuality.GOOD,
            responseTimeMs = 10000  // 10s — slow
        )

        // Interval should be capped at 14 days max
        assertTrue(result.sm2Result.sm2Data.intervalDays <= 14)
    }

    @Test
    fun `does not cap interval for fast responses`() {
        val card = makeFlashcard(repetition = 4, intervalDays = 30, easeFactor = 2.5)
        val result = AdaptiveScheduler.processReview(
            card, ReviewQuality.GOOD,
            responseTimeMs = 1500  // 1.5s — fast
        )

        // No capping applied for fast responses
        assertTrue(result.sm2Result.sm2Data.intervalDays > 14)
    }

    // ── Confidence level ────────────────────────────────────

    @Test
    fun `fast correct answer gives HIGH confidence`() {
        val card = makeFlashcard()
        val result = AdaptiveScheduler.processReview(
            card, ReviewQuality.EASY, responseTimeMs = 1500
        )
        assertEquals(ConfidenceLevel.HIGH, result.confidenceLevel)
    }

    @Test
    fun `slow correct answer gives LOW confidence`() {
        val card = makeFlashcard()
        val result = AdaptiveScheduler.processReview(
            card, ReviewQuality.GOOD, responseTimeMs = 9000
        )
        assertEquals(ConfidenceLevel.LOW, result.confidenceLevel)
    }

    @Test
    fun `failed answer gives VERY_LOW confidence`() {
        val card = makeFlashcard()
        val result = AdaptiveScheduler.processReview(
            card, ReviewQuality.AGAIN, responseTimeMs = 5000
        )
        assertEquals(ConfidenceLevel.VERY_LOW, result.confidenceLevel)
    }

    // ── Response analysis ───────────────────────────────────

    @Test
    fun `classifies response time correctly`() {
        val card = makeFlashcard()

        val fast = AdaptiveScheduler.processReview(card, ReviewQuality.EASY, responseTimeMs = 1000)
        assertEquals(ResponseAnalysis.FAST, fast.responseAnalysis)

        val normal = AdaptiveScheduler.processReview(card, ReviewQuality.GOOD, responseTimeMs = 4000)
        assertEquals(ResponseAnalysis.NORMAL, normal.responseAnalysis)

        val slow = AdaptiveScheduler.processReview(card, ReviewQuality.GOOD, responseTimeMs = 9000)
        assertEquals(ResponseAnalysis.SLOW, slow.responseAnalysis)
    }

    @Test
    fun `null responseTime gives UNKNOWN analysis`() {
        val card = makeFlashcard()
        val result = AdaptiveScheduler.processReview(card, ReviewQuality.GOOD, responseTimeMs = null)
        assertEquals(ResponseAnalysis.UNKNOWN, result.responseAnalysis)
    }

    // ── Review trend ────────────────────────────────────────

    @Test
    fun `insufficient data returns INSUFFICIENT_DATA trend`() {
        val card = makeFlashcard()
        val result = AdaptiveScheduler.processReview(
            card, ReviewQuality.GOOD, recentReviews = listOf(makeReviewLog(ReviewQuality.GOOD))
        )
        assertEquals(ReviewTrend.INSUFFICIENT_DATA, result.trend)
    }

    @Test
    fun `all failures returns DECLINING trend`() {
        val card = makeFlashcard()
        val reviews = listOf(
            makeReviewLog(ReviewQuality.AGAIN),
            makeReviewLog(ReviewQuality.AGAIN),
            makeReviewLog(ReviewQuality.HARD)
        )
        val result = AdaptiveScheduler.processReview(card, ReviewQuality.AGAIN, recentReviews = reviews)
        assertEquals(ReviewTrend.DECLINING, result.trend)
    }

    @Test
    fun `all successes returns STABLE trend`() {
        val card = makeFlashcard()
        val reviews = listOf(
            makeReviewLog(ReviewQuality.GOOD),
            makeReviewLog(ReviewQuality.GOOD),
            makeReviewLog(ReviewQuality.EASY)
        )
        val result = AdaptiveScheduler.processReview(card, ReviewQuality.GOOD, recentReviews = reviews)
        assertEquals(ReviewTrend.STABLE, result.trend)
    }

    // ── SM-2 delegation ─────────────────────────────────────

    @Test
    fun `delegates to SM2Engine correctly`() {
        val card = makeFlashcard()
        val result = AdaptiveScheduler.processReview(card, ReviewQuality.GOOD)

        // Should match SM2Engine's output
        val directResult = SM2Engine.calculate(card.sm2, ReviewQuality.GOOD)
        assertEquals(directResult.sm2Data.repetition, result.sm2Result.sm2Data.repetition)
        assertEquals(directResult.wasReset, result.sm2Result.wasReset)
    }
}
