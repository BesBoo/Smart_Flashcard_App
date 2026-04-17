package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.ReviewLogDao
import com.example.myapplication.data.local.entity.ReviewLogEntity
import com.example.myapplication.data.remote.api.FlashcardApi
import com.example.myapplication.data.remote.dto.CreateReviewRequest
import com.example.myapplication.domain.model.ReviewLog
import com.example.myapplication.domain.model.toDomainReviewLogs
import com.example.myapplication.domain.model.toEntity
import com.example.myapplication.domain.repository.ReviewLogRepository
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewLogRepositoryImpl @Inject constructor(
    private val reviewLogDao: ReviewLogDao,
    private val flashcardApi: FlashcardApi
) : ReviewLogRepository {

    override suspend fun getReviewLogsByCard(flashcardId: String): List<ReviewLog> =
        reviewLogDao.getReviewLogsByCard(flashcardId).toDomainReviewLogs()

    override suspend fun getReviewLogsByDateRange(userId: String, startDate: Long, endDate: Long): List<ReviewLog> =
        reviewLogDao.getReviewLogsByDateRange(userId, startDate, endDate).toDomainReviewLogs()

    override suspend fun saveReviewLog(reviewLog: ReviewLog) {
        reviewLogDao.insertReviewLog(reviewLog.toEntity())
        try {
            // Convert timestamp to ISO 8601 string for API
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")
            val dateStr = isoFormat.format(Date(reviewLog.reviewedAt))

            flashcardApi.createReview(
                CreateReviewRequest(
                    id = reviewLog.id,
                    flashcardId = reviewLog.flashcardId,
                    quality = reviewLog.quality.value,
                    responseTimeMs = reviewLog.responseTimeMs,
                    reviewedAt = dateStr
                )
            )
        } catch (_: Exception) {
            // Offline
        }
    }

    override suspend fun saveReviewLogs(reviewLogs: List<ReviewLog>) {
        reviewLogDao.insertReviewLogs(reviewLogs.map { it.toEntity() })
    }

    override fun observeReviewCountToday(userId: String, todayStart: Long): Flow<Int> =
        reviewLogDao.observeReviewCountToday(userId, todayStart)

    override suspend fun getTotalReviewCount(userId: String): Int =
        reviewLogDao.getTotalReviewCount(userId)

    override suspend fun getAverageQuality(flashcardId: String): Double? =
        reviewLogDao.getAverageQuality(flashcardId)

    override suspend fun deleteAllByUser(userId: String) {
        reviewLogDao.deleteAllByUser(userId)
    }

    /**
     * Sync review logs from server to local DB.
     * Called on login to restore review history.
     */
    override suspend fun syncReviewLogs(userId: String) {
        try {
            val remoteReviews = flashcardApi.getReviews()
            if (remoteReviews.isNotEmpty()) {
                val entities = remoteReviews.map { dto ->
                    ReviewLogEntity(
                        id = dto.id,
                        userId = userId,
                        flashcardId = dto.flashcardId,
                        quality = dto.quality,
                        responseTimeMs = dto.responseTimeMs,
                        reviewedAt = try {
                            // Parse ISO 8601 date string to timestamp
                            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                            format.timeZone = TimeZone.getTimeZone("UTC")
                            format.parse(dto.reviewedAt.take(19))?.time ?: System.currentTimeMillis()
                        } catch (_: Exception) {
                            System.currentTimeMillis()
                        }
                    )
                }
                reviewLogDao.insertReviewLogs(entities)
            }
        } catch (_: Exception) {
            // Offline — skip sync
        }
    }
}
