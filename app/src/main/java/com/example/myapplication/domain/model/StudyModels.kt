package com.example.myapplication.domain.model

/**
 * Represents a single review event — used for statistics and AI analysis.
 */
data class ReviewLog(
    val id: String,
    val userId: String,
    val flashcardId: String,
    val quality: ReviewQuality,
    val responseTimeMs: Long? = null,
    val reviewedAt: Long = System.currentTimeMillis()
)

/**
 * Result of an SM-2 calculation.
 * Returned by SM2Engine.calculate() — contains the updated SM-2 state.
 */
data class SM2Result(
    val sm2Data: SM2Data,
    val wasReset: Boolean   // true if quality < 3 (card reset to beginning)
)

/**
 * Summary statistics for a study session.
 */
data class StudySessionSummary(
    val totalCards: Int,
    val correctCount: Int,          // quality >= 3
    val incorrectCount: Int,        // quality < 3
    val averageResponseTimeMs: Long,
    val totalTimeMs: Long,
    val newCardsStudied: Int,
    val reviewCardsStudied: Int
) {
    val accuracy: Float
        get() = if (totalCards > 0) correctCount.toFloat() / totalCards else 0f

    val accuracyPercent: Int
        get() = (accuracy * 100).toInt()
}

/**
 * Daily learning statistics for the stats screen.
 */
data class DailyStats(
    val date: Long,                 // start of day timestamp
    val cardsReviewed: Int,
    val newCardsLearned: Int,
    val accuracy: Float,            // 0.0 to 1.0
    val totalTimeMs: Long
)

/**
 * Overall learning statistics for the stats screen.
 */
data class LearningStats(
    val totalCards: Int,
    val masteredCards: Int,          // repetition >= 3 and easeFactor >= 2.5
    val learningCards: Int,          // repetition > 0 and not mastered
    val newCards: Int,               // totalReviews == 0
    val dueCards: Int = 0,           // cards due for review now
    val currentStreak: Int,         // consecutive days studied
    val longestStreak: Int,
    val dailyStats: List<DailyStats>,
    val totalReviews: Int,
    val averageAccuracy: Float,
    val goodReviews: Int = 0,       // quality >= 3 (Tốt + Dễ)
    val hardReviews: Int = 0,       // quality == 2 (Khó)
    val againReviews: Int = 0       // quality < 2 (Học lại)
)
