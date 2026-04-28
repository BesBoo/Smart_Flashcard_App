package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.DailyStats
import com.example.myapplication.domain.model.LearningStats
import com.example.myapplication.domain.repository.FlashcardRepository
import com.example.myapplication.domain.repository.ReviewLogRepository
import java.util.Calendar
import javax.inject.Inject

class GetLearningStatsUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val reviewLogRepository: ReviewLogRepository
) {
    suspend operator fun invoke(userId: String): LearningStats {
        val totalReviews = reviewLogRepository.getTotalReviewCount(userId)

        // Get ALL cards for the user to compute mastery breakdown
        val allCards = flashcardRepository.getAllCardsByUser(userId)

        // Get recent review logs for daily stats (last 7 days)
        val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val recentLogs = reviewLogRepository.getReviewLogsByDateRange(userId, sevenDaysAgo, System.currentTimeMillis())

        // Build daily stats from review logs
        val dailyStats = recentLogs
            .groupBy { log ->
                val cal = Calendar.getInstance().apply { timeInMillis = log.reviewedAt }
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            .map { (dayStart, logs) ->
                val correct = logs.count { it.quality.value >= 3 }
                DailyStats(
                    date = dayStart,
                    cardsReviewed = logs.size,
                    newCardsLearned = logs.count { it.quality.value >= 3 },
                    accuracy = if (logs.isNotEmpty()) correct.toFloat() / logs.size else 0f,
                    totalTimeMs = logs.mapNotNull { it.responseTimeMs }.sum()
                )
            }
            .sortedBy { it.date }

        val overallAccuracy = if (totalReviews > 0 && recentLogs.isNotEmpty()) {
            recentLogs.count { it.quality.value >= 3 }.toFloat() / recentLogs.size
        } else 0f

        // Calculate streak (consecutive days with reviews, counting back from today)
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val reviewDays = dailyStats.map { it.date }.toSet()
        var currentStreak = 0
        val checkDay = today.clone() as Calendar
        while (reviewDays.contains(checkDay.timeInMillis)) {
            currentStreak++
            checkDay.add(Calendar.DAY_OF_YEAR, -1)
        }

        val now = System.currentTimeMillis()
        val dueCards = allCards.count { it.isDue }

        return LearningStats(
            totalCards = allCards.size,
            masteredCards = allCards.count { it.sm2.repetition >= 3 && it.sm2.easeFactor >= 2.0 },
            learningCards = allCards.count { it.sm2.totalReviews > 0 && !(it.sm2.repetition >= 3 && it.sm2.easeFactor >= 2.0) },
            newCards = allCards.count { it.sm2.totalReviews == 0 },
            dueCards = dueCards,
            currentStreak = currentStreak,
            longestStreak = currentStreak, // simplified — would need full history for longest
            dailyStats = dailyStats,
            totalReviews = totalReviews,
            averageAccuracy = overallAccuracy,
            goodReviews = recentLogs.count { it.quality.value >= 3 },
            hardReviews = recentLogs.count { it.quality.value == 2 },
            againReviews = recentLogs.count { it.quality.value < 2 }
        )
    }
}
