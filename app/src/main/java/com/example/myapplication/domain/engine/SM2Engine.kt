package com.example.myapplication.domain.engine

import com.example.myapplication.domain.model.ReviewQuality
import com.example.myapplication.domain.model.SM2Data
import com.example.myapplication.domain.model.SM2Result
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Pure implementation of the SM-2 (SuperMemo 2) spaced repetition algorithm.
 *
 * Original paper: P.A. Woźniak, "Optimization of learning", 1990.
 *
 * ## Algorithm Summary
 *
 * Given a user response quality q ∈ {0, 2, 3, 5}:
 *
 * ### If q < 3 (failure — "Học lại" or "Khó"):
 * - Reset: n=0, I=1 (start over)
 * - EF is recalculated but never drops below 1.3
 * - failCount is incremented
 *
 * ### If q >= 3 (success — "Tốt" or "Dễ"):
 * - n = n + 1
 * - I(1) = 1, I(2) = 6, I(n) = I(n-1) * EF for n > 2
 * - EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
 * - EF = max(1.3, EF')
 * - failCount resets to 0
 *
 * This class is **stateless** — it takes SM2Data + quality and returns new SM2Data.
 * It does NOT interact with any database, repository, or AI service.
 */
object SM2Engine {

    /** Minimum allowed ease factor */
    private const val MIN_EASE_FACTOR = 1.3

    /** First interval (days) after first successful review */
    private const val FIRST_INTERVAL = 1

    /** Second interval (days) after second successful review */
    private const val SECOND_INTERVAL = 6

    /**
     * Calculate the next SM-2 state after a review.
     *
     * @param current  The current SM-2 state of the card
     * @param quality  The user's review quality response
     * @return [SM2Result] containing the updated SM-2 state
     */
    fun calculate(current: SM2Data, quality: ReviewQuality): SM2Result {
        val q = quality.value
        val totalReviews = current.totalReviews + 1

        // ── Calculate new ease factor (applies to ALL quality levels) ──
        val rawEF = current.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        val newEF = max(MIN_EASE_FACTOR, rawEF)

        return if (q < 3) {
            // ── FAILURE PATH (q=0 "Học lại" or q=2 "Khó") ──
            // Reset repetitions, set interval to 1 day, increment fail counter
            val newSM2 = SM2Data(
                repetition = 0,
                intervalDays = FIRST_INTERVAL,
                easeFactor = newEF,
                nextReviewDate = calculateNextReviewTimestamp(FIRST_INTERVAL),
                failCount = current.failCount + 1,
                totalReviews = totalReviews
            )
            SM2Result(sm2Data = newSM2, wasReset = true)
        } else {
            // ── SUCCESS PATH (q=3 "Tốt" or q=5 "Dễ") ──
            val newRepetition = current.repetition + 1
            val newInterval = calculateInterval(newRepetition, current.intervalDays, newEF)

            val newSM2 = SM2Data(
                repetition = newRepetition,
                intervalDays = newInterval,
                easeFactor = newEF,
                nextReviewDate = calculateNextReviewTimestamp(newInterval),
                failCount = 0,  // Reset fail counter on success
                totalReviews = totalReviews
            )
            SM2Result(sm2Data = newSM2, wasReset = false)
        }
    }

    /**
     * Calculate the next interval based on the repetition count.
     *
     * I(1) = 1
     * I(2) = 6
     * I(n) = I(n-1) * EF  for n > 2
     */
    private fun calculateInterval(repetition: Int, previousInterval: Int, easeFactor: Double): Int {
        return when (repetition) {
            1 -> FIRST_INTERVAL
            2 -> SECOND_INTERVAL
            else -> max(1, (previousInterval * easeFactor).toInt())
        }
    }

    /**
     * Convert interval in days to a future timestamp (milliseconds).
     */
    private fun calculateNextReviewTimestamp(intervalDays: Int): Long {
        return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(intervalDays.toLong())
    }
}
