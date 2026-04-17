package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.engine.AdaptiveResult
import com.example.myapplication.domain.engine.AdaptiveScheduler
import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.model.ReviewLog
import com.example.myapplication.domain.model.ReviewQuality
import com.example.myapplication.domain.repository.FlashcardRepository
import com.example.myapplication.domain.repository.ReviewLogRepository
import java.util.UUID
import javax.inject.Inject

class ReviewCardUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val reviewLogRepository: ReviewLogRepository
) {
    /**
     * Process a card review through AdaptiveScheduler,
     * update SM-2 state in DB, and log the review.
     *
     * @return AdaptiveResult indicating if AI intervention is needed
     */
    suspend operator fun invoke(
        flashcard: Flashcard,
        quality: ReviewQuality,
        responseTimeMs: Long? = null
    ): AdaptiveResult {
        // 1. Get recent reviews for trend analysis
        val recentReviews = reviewLogRepository.getReviewLogsByCard(flashcard.id).take(5)

        // 2. Process via Adaptive Scheduler (SM-2 + adaptive logic)
        val adaptiveResult = AdaptiveScheduler.processReview(
            flashcard = flashcard,
            quality = quality,
            responseTimeMs = responseTimeMs,
            recentReviews = recentReviews
        )

        val newSm2Data = adaptiveResult.sm2Result.sm2Data

        // 3. Save updated SM-2 state to local DB
        flashcardRepository.updateSm2Fields(
            cardId = flashcard.id,
            repetition = newSm2Data.repetition,
            intervalDays = newSm2Data.intervalDays,
            easeFactor = newSm2Data.easeFactor,
            nextReviewDate = newSm2Data.nextReviewDate,
            quality = quality.value
        )

        // 4. Log the review
        val log = ReviewLog(
            id = UUID.randomUUID().toString(),
            userId = flashcard.userId,
            flashcardId = flashcard.id,
            quality = quality,
            responseTimeMs = responseTimeMs,
            reviewedAt = System.currentTimeMillis()
        )
        reviewLogRepository.saveReviewLog(log)

        return adaptiveResult
    }
}
