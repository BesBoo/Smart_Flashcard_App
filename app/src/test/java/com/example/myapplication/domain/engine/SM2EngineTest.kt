package com.example.myapplication.domain.engine

import com.example.myapplication.domain.model.ReviewQuality
import com.example.myapplication.domain.model.SM2Data
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive unit tests for [SM2Engine].
 *
 * Tests follow the original SM-2 algorithm specification:
 * - Quality 0 ("Học lại"): reset to interval=1, failCount++
 * - Quality 2 ("Khó"): reset to interval=1, failCount++
 * - Quality 3 ("Tốt"): normal progression, failCount=0
 * - Quality 5 ("Dễ"): longer intervals, higher EF, failCount=0
 * - EF never drops below 1.3
 * - Interval progression: I(1)=1, I(2)=6, I(n)=I(n-1)*EF
 */
class SM2EngineTest {

    private val defaultSM2 = SM2Data()  // rep=0, I=1, EF=2.5, fail=0, total=0

    // ============================================================
    // Quality = 0 ("Học lại") — Complete failure, reset
    // ============================================================

    @Test
    fun `q0 - Học lại - resets repetition to 0 and interval to 1`() {
        val current = SM2Data(repetition = 3, intervalDays = 15, easeFactor = 2.5)
        val result = SM2Engine.calculate(current, ReviewQuality.AGAIN)

        assertEquals(0, result.sm2Data.repetition)
        assertEquals(1, result.sm2Data.intervalDays)
        assertTrue(result.wasReset)
    }

    @Test
    fun `q0 - increments failCount`() {
        val current = SM2Data(failCount = 2)
        val result = SM2Engine.calculate(current, ReviewQuality.AGAIN)

        assertEquals(3, result.sm2Data.failCount)
    }

    @Test
    fun `q0 - increments totalReviews`() {
        val current = SM2Data(totalReviews = 5)
        val result = SM2Engine.calculate(current, ReviewQuality.AGAIN)

        assertEquals(6, result.sm2Data.totalReviews)
    }

    @Test
    fun `q0 - lowers ease factor`() {
        val current = SM2Data(easeFactor = 2.5)
        val result = SM2Engine.calculate(current, ReviewQuality.AGAIN)

        // EF' = 2.5 + (0.1 - (5-0)*(0.08 + (5-0)*0.02))
        // EF' = 2.5 + (0.1 - 5*(0.08 + 0.10))
        // EF' = 2.5 + (0.1 - 0.9) = 2.5 - 0.8 = 1.7
        assertEquals(1.7, result.sm2Data.easeFactor, 0.01)
    }

    // ============================================================
    // Quality = 2 ("Khó") — Hard, also resets but less EF decrease
    // ============================================================

    @Test
    fun `q2 - Khó - resets repetition and interval`() {
        val current = SM2Data(repetition = 2, intervalDays = 6, easeFactor = 2.5)
        val result = SM2Engine.calculate(current, ReviewQuality.HARD)

        assertEquals(0, result.sm2Data.repetition)
        assertEquals(1, result.sm2Data.intervalDays)
        assertTrue(result.wasReset)
    }

    @Test
    fun `q2 - lowers ease factor moderately`() {
        val current = SM2Data(easeFactor = 2.5)
        val result = SM2Engine.calculate(current, ReviewQuality.HARD)

        // EF' = 2.5 + (0.1 - (5-2)*(0.08 + (5-2)*0.02))
        // EF' = 2.5 + (0.1 - 3*(0.08 + 0.06))
        // EF' = 2.5 + (0.1 - 0.42) = 2.5 - 0.32 = 2.18
        assertEquals(2.18, result.sm2Data.easeFactor, 0.01)
    }

    @Test
    fun `q2 - increments failCount`() {
        val current = SM2Data(failCount = 0)
        val result = SM2Engine.calculate(current, ReviewQuality.HARD)

        assertEquals(1, result.sm2Data.failCount)
    }

    // ============================================================
    // Quality = 3 ("Tốt") — Normal progression
    // ============================================================

    @Test
    fun `q3 - Tốt - first successful review sets interval to 1`() {
        val result = SM2Engine.calculate(defaultSM2, ReviewQuality.GOOD)

        assertEquals(1, result.sm2Data.repetition)
        assertEquals(1, result.sm2Data.intervalDays)
        assertFalse(result.wasReset)
    }

    @Test
    fun `q3 - second successful review sets interval to 6`() {
        val afterFirst = SM2Data(repetition = 1, intervalDays = 1, easeFactor = 2.36)
        val result = SM2Engine.calculate(afterFirst, ReviewQuality.GOOD)

        assertEquals(2, result.sm2Data.repetition)
        assertEquals(6, result.sm2Data.intervalDays)
        assertFalse(result.wasReset)
    }

    @Test
    fun `q3 - third successful review uses EF multiplication`() {
        val afterSecond = SM2Data(repetition = 2, intervalDays = 6, easeFactor = 2.36)
        val result = SM2Engine.calculate(afterSecond, ReviewQuality.GOOD)

        assertEquals(3, result.sm2Data.repetition)
        // I(3) = I(2) * EF = 6 * 2.22 (recalculated EF) ≈ 13
        // EF' = 2.36 + (0.1 - (5-3)*(0.08 + (5-3)*0.02)) = 2.36 + (0.1 - 0.24) = 2.22
        val expectedEF = 2.36 + (0.1 - 2.0 * (0.08 + 2.0 * 0.02))
        assertEquals(expectedEF, result.sm2Data.easeFactor, 0.01)
    }

    @Test
    fun `q3 - resets failCount to 0`() {
        val current = SM2Data(failCount = 3)
        val result = SM2Engine.calculate(current, ReviewQuality.GOOD)

        assertEquals(0, result.sm2Data.failCount)
    }

    @Test
    fun `q3 - slightly decreases ease factor`() {
        val current = SM2Data(easeFactor = 2.5)
        val result = SM2Engine.calculate(current, ReviewQuality.GOOD)

        // EF' = 2.5 + (0.1 - (5-3)*(0.08 + (5-3)*0.02))
        // EF' = 2.5 + (0.1 - 2*(0.08 + 0.04))
        // EF' = 2.5 + (0.1 - 0.24) = 2.5 - 0.14 = 2.36
        assertEquals(2.36, result.sm2Data.easeFactor, 0.01)
    }

    // ============================================================
    // Quality = 5 ("Dễ") — Effortless, longer intervals
    // ============================================================

    @Test
    fun `q5 - Dễ - increases ease factor`() {
        val current = SM2Data(easeFactor = 2.5)
        val result = SM2Engine.calculate(current, ReviewQuality.EASY)

        // EF' = 2.5 + (0.1 - (5-5)*(0.08 + (5-5)*0.02))
        // EF' = 2.5 + (0.1 - 0) = 2.6
        assertEquals(2.6, result.sm2Data.easeFactor, 0.01)
    }

    @Test
    fun `q5 - results in longer intervals than q3`() {
        val current = SM2Data(repetition = 2, intervalDays = 6, easeFactor = 2.5)

        val resultGood = SM2Engine.calculate(current, ReviewQuality.GOOD)
        val resultEasy = SM2Engine.calculate(current, ReviewQuality.EASY)

        assertTrue(
            "Easy should give longer interval than Good",
            resultEasy.sm2Data.intervalDays >= resultGood.sm2Data.intervalDays
        )
    }

    @Test
    fun `q5 - resets failCount to 0`() {
        val current = SM2Data(failCount = 5)
        val result = SM2Engine.calculate(current, ReviewQuality.EASY)

        assertEquals(0, result.sm2Data.failCount)
        assertFalse(result.wasReset)
    }

    // ============================================================
    // EF bounds — never below 1.3
    // ============================================================

    @Test
    fun `EF never drops below 1_3 after repeated failures`() {
        var sm2 = SM2Data(easeFactor = 1.5)

        // Repeatedly fail — EF should bottom out at 1.3
        repeat(10) {
            val result = SM2Engine.calculate(sm2, ReviewQuality.AGAIN)
            assertTrue(
                "EF should be >= 1.3, was ${result.sm2Data.easeFactor}",
                result.sm2Data.easeFactor >= 1.3
            )
            sm2 = result.sm2Data
        }

        assertEquals(1.3, sm2.easeFactor, 0.01)
    }

    @Test
    fun `EF stays at minimum 1_3 when already at floor`() {
        val current = SM2Data(easeFactor = 1.3)
        val result = SM2Engine.calculate(current, ReviewQuality.AGAIN)

        assertEquals(1.3, result.sm2Data.easeFactor, 0.01)
    }

    // ============================================================
    // Interval progression: 1 → 6 → calculated
    // ============================================================

    @Test
    fun `interval progression follows 1-6-calculated pattern`() {
        var sm2 = defaultSM2  // rep=0, I=1

        // Review 1: I(1) = 1
        val r1 = SM2Engine.calculate(sm2, ReviewQuality.GOOD)
        assertEquals(1, r1.sm2Data.intervalDays)
        assertEquals(1, r1.sm2Data.repetition)
        sm2 = r1.sm2Data

        // Review 2: I(2) = 6
        val r2 = SM2Engine.calculate(sm2, ReviewQuality.GOOD)
        assertEquals(6, r2.sm2Data.intervalDays)
        assertEquals(2, r2.sm2Data.repetition)
        sm2 = r2.sm2Data

        // Review 3: I(3) = I(2) * EF = 6 * EF
        val r3 = SM2Engine.calculate(sm2, ReviewQuality.GOOD)
        assertEquals(3, r3.sm2Data.repetition)
        assertTrue(r3.sm2Data.intervalDays > 6)   // Should be > 6
        sm2 = r3.sm2Data

        // Review 4: I(4) = I(3) * EF — continues growing
        val r4 = SM2Engine.calculate(sm2, ReviewQuality.GOOD)
        assertTrue(r4.sm2Data.intervalDays > r3.sm2Data.intervalDays)
    }

    // ============================================================
    // Next review date
    // ============================================================

    @Test
    fun `nextReviewDate is set in the future`() {
        val now = System.currentTimeMillis()
        val result = SM2Engine.calculate(defaultSM2, ReviewQuality.GOOD)

        assertTrue(result.sm2Data.nextReviewDate > now)
    }

    // ============================================================
    // Edge cases
    // ============================================================

    @Test
    fun `brand new card with first AGAIN stays at interval 1`() {
        val result = SM2Engine.calculate(defaultSM2, ReviewQuality.AGAIN)

        assertEquals(0, result.sm2Data.repetition)
        assertEquals(1, result.sm2Data.intervalDays)
        assertEquals(1, result.sm2Data.failCount)
    }

    @Test
    fun `recovery after failure follows correct progression`() {
        // Start mastered
        val mastered = SM2Data(repetition = 4, intervalDays = 15, easeFactor = 2.5, failCount = 0)

        // Fail once
        val failed = SM2Engine.calculate(mastered, ReviewQuality.AGAIN)
        assertEquals(0, failed.sm2Data.repetition)
        assertEquals(1, failed.sm2Data.intervalDays)
        assertEquals(1, failed.sm2Data.failCount)

        // Recover: first success after failure
        val recovered = SM2Engine.calculate(failed.sm2Data, ReviewQuality.GOOD)
        assertEquals(1, recovered.sm2Data.repetition)
        assertEquals(1, recovered.sm2Data.intervalDays)
        assertEquals(0, recovered.sm2Data.failCount)

        // Second success: interval jumps to 6
        val second = SM2Engine.calculate(recovered.sm2Data, ReviewQuality.GOOD)
        assertEquals(2, second.sm2Data.repetition)
        assertEquals(6, second.sm2Data.intervalDays)
    }
}
