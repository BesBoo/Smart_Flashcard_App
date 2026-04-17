package com.example.myapplication.domain.engine

import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.model.ReviewLog
import com.example.myapplication.domain.model.ReviewQuality
import com.example.myapplication.domain.model.SM2Result

/**
 * Adaptive Scheduler — wraps the pure SM-2 engine with intelligence.
 *
 * Responsibilities:
 * 1. Delegates core scheduling to [SM2Engine]
 * 2. Detects struggling cards (failCount >= threshold)
 * 3. Analyzes response time patterns to gauge confidence
 * 4. Determines when AI intervention should be triggered
 * 5. Can adjust intervals based on AI recommendations
 *
 * ## Design Principle
 * SM-2 is the SOURCE OF TRUTH for scheduling. The AdaptiveScheduler
 * NEVER overrides SM-2 calculations. It only AUGMENTS them by:
 * - Flagging cards for AI assistance
 * - Providing metadata for AI analysis
 * - Optionally capping intervals for uncertain recalls
 */
object AdaptiveScheduler {

    /** Number of consecutive failures before triggering AI intervention */
    private const val STRUGGLE_THRESHOLD = 3

    /** Response time (ms) above which a recall is considered slow/uncertain */
    private const val SLOW_RESPONSE_THRESHOLD_MS = 8000L

    /** Response time (ms) below which a recall is considered very fast/confident */
    private const val FAST_RESPONSE_THRESHOLD_MS = 2000L

    /** Maximum interval cap for cards answered slowly (days) */
    private const val SLOW_RESPONSE_MAX_INTERVAL = 14

    /**
     * Process a review through SM-2, then apply adaptive adjustments.
     *
     * @param flashcard       The card being reviewed
     * @param quality         The user's quality response
     * @param responseTimeMs  How long the user took to answer (ms), nullable
     * @param recentReviews   Recent review history for trend analysis
     * @return [AdaptiveResult] with the SM-2 result plus adaptive metadata
     */
    fun processReview(
        flashcard: Flashcard,
        quality: ReviewQuality,
        responseTimeMs: Long? = null,
        recentReviews: List<ReviewLog> = emptyList()
    ): AdaptiveResult {

        // Step 1: Run pure SM-2
        val sm2Result = SM2Engine.calculate(flashcard.sm2, quality)
        var adjustedResult = sm2Result

        // Step 2: Analyze response time — slow correct answers may need shorter intervals
        if (quality.value >= 3 && responseTimeMs != null && responseTimeMs > SLOW_RESPONSE_THRESHOLD_MS) {
            val cappedInterval = sm2Result.sm2Data.intervalDays
                .coerceAtMost(SLOW_RESPONSE_MAX_INTERVAL)

            if (cappedInterval < sm2Result.sm2Data.intervalDays) {
                adjustedResult = SM2Result(
                    sm2Data = sm2Result.sm2Data.copy(
                        intervalDays = cappedInterval,
                        nextReviewDate = System.currentTimeMillis() +
                            cappedInterval * 24L * 60 * 60 * 1000
                    ),
                    wasReset = sm2Result.wasReset
                )
            }
        }

        // Step 3: Determine if AI intervention is needed
        val newFailCount = adjustedResult.sm2Data.failCount
        val shouldTriggerAi = newFailCount >= STRUGGLE_THRESHOLD
        val confidenceLevel = analyzeConfidence(quality, responseTimeMs, recentReviews)
        val trend = analyzeReviewTrend(recentReviews)

        return AdaptiveResult(
            sm2Result = adjustedResult,
            shouldTriggerAiHint = shouldTriggerAi,
            confidenceLevel = confidenceLevel,
            trend = trend,
            responseAnalysis = responseTimeMs?.let { analyzeResponseTime(it) }
                ?: ResponseAnalysis.UNKNOWN
        )
    }

    /**
     * Detect if a flashcard is struggling and should receive AI help.
     */
    fun isStruggling(flashcard: Flashcard): Boolean =
        flashcard.sm2.failCount >= STRUGGLE_THRESHOLD

    /**
     * Analyze user's confidence level based on response quality and timing.
     */
    private fun analyzeConfidence(
        quality: ReviewQuality,
        responseTimeMs: Long?,
        recentReviews: List<ReviewLog>
    ): ConfidenceLevel {
        // Fast + correct = high confidence
        if (quality.value >= 3 && responseTimeMs != null && responseTimeMs < FAST_RESPONSE_THRESHOLD_MS) {
            return ConfidenceLevel.HIGH
        }

        // Correct but slow = medium confidence
        if (quality.value >= 3 && responseTimeMs != null && responseTimeMs > SLOW_RESPONSE_THRESHOLD_MS) {
            return ConfidenceLevel.LOW
        }

        // Failed = very low confidence
        if (quality.value < 3) {
            return ConfidenceLevel.VERY_LOW
        }

        // Check recent trend
        if (recentReviews.size >= 3) {
            val recentSuccessRate = recentReviews.takeLast(3)
                .count { it.quality.value >= 3 } / 3.0f
            if (recentSuccessRate < 0.5f) return ConfidenceLevel.LOW
        }

        return ConfidenceLevel.MEDIUM
    }

    /**
     * Analyze the trend of recent reviews (improving / stable / declining).
     */
    private fun analyzeReviewTrend(recentReviews: List<ReviewLog>): ReviewTrend {
        if (recentReviews.size < 3) return ReviewTrend.INSUFFICIENT_DATA

        val lastThree = recentReviews.sortedBy { it.reviewedAt }.takeLast(3)
        val qualities = lastThree.map { it.quality.value }

        // Check if trending up
        if (qualities[0] < qualities[1] && qualities[1] <= qualities[2]) {
            return ReviewTrend.IMPROVING
        }

        // Check if trending down
        if (qualities[0] > qualities[1] && qualities[1] >= qualities[2]) {
            return ReviewTrend.DECLINING
        }

        // Check if all failures
        if (qualities.all { it < 3 }) return ReviewTrend.DECLINING

        // Check if all successes
        if (qualities.all { it >= 3 }) return ReviewTrend.STABLE

        return ReviewTrend.FLUCTUATING
    }

    /**
     * Classify response time.
     */
    private fun analyzeResponseTime(responseTimeMs: Long): ResponseAnalysis = when {
        responseTimeMs < FAST_RESPONSE_THRESHOLD_MS -> ResponseAnalysis.FAST
        responseTimeMs < SLOW_RESPONSE_THRESHOLD_MS -> ResponseAnalysis.NORMAL
        else -> ResponseAnalysis.SLOW
    }
}

// ── Result & metadata classes ─────────────────────────────

/**
 * Full adaptive review result, wrapping SM-2 output with AI metadata.
 */
data class AdaptiveResult(
    val sm2Result: SM2Result,
    val shouldTriggerAiHint: Boolean,
    val confidenceLevel: ConfidenceLevel,
    val trend: ReviewTrend,
    val responseAnalysis: ResponseAnalysis
)

/** User's confidence level based on response patterns */
enum class ConfidenceLevel {
    VERY_LOW,   // Failed, needs help
    LOW,        // Slow or inconsistent
    MEDIUM,     // Normal recall
    HIGH        // Fast, correct recall
}

/** Trend direction over recent reviews */
enum class ReviewTrend {
    IMPROVING,          // Getting better
    STABLE,             // Consistently correct
    DECLINING,          // Getting worse
    FLUCTUATING,        // Inconsistent
    INSUFFICIENT_DATA   // Not enough reviews to determine
}

/** Response time classification */
enum class ResponseAnalysis {
    FAST,       // < 2s — instant recall
    NORMAL,     // 2-8s — deliberate recall
    SLOW,       // > 8s — struggle/hesitation
    UNKNOWN     // No timing data
}
