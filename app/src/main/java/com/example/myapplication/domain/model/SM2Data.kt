package com.example.myapplication.domain.model

/**
 * SM-2 spaced repetition state — a self-contained value object.
 *
 * This is the SOURCE OF TRUTH for scheduling. AI can READ these values
 * but CANNOT modify them. Only SM2Engine.calculate() writes to this.
 *
 * @property repetition     n — number of consecutive correct reviews (quality >= 3)
 * @property intervalDays   I(n) — inter-repetition interval in days
 * @property easeFactor     EF — difficulty factor, minimum 1.3
 * @property nextReviewDate Timestamp (ms) of the next scheduled review
 * @property failCount      Consecutive failures (quality < 3). Triggers AI adaptive at >= 3.
 * @property totalReviews   Lifetime total review count for statistics.
 */
data class SM2Data(
    val repetition: Int = 0,
    val intervalDays: Int = 1,
    val easeFactor: Double = 2.5,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val failCount: Int = 0,
    val totalReviews: Int = 0
) {
    /** True if this card is due for review (nextReviewDate <= now) */
    val isDue: Boolean
        get() = nextReviewDate <= System.currentTimeMillis()

    /** True if this card has never been reviewed */
    val isNew: Boolean
        get() = totalReviews == 0

    /** True if this card is struggling (triggers AI adaptive hint) */
    val isStruggling: Boolean
        get() = failCount >= 3

    init {
        require(easeFactor >= 1.3) { "EaseFactor must be >= 1.3, was $easeFactor" }
        require(intervalDays >= 1) { "IntervalDays must be >= 1, was $intervalDays" }
        require(repetition >= 0) { "Repetition must be >= 0, was $repetition" }
    }
}
