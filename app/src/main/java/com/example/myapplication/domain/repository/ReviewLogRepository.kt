package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.ReviewLog
import kotlinx.coroutines.flow.Flow

interface ReviewLogRepository {
    suspend fun getReviewLogsByCard(flashcardId: String): List<ReviewLog>
    suspend fun getReviewLogsByDateRange(userId: String, startDate: Long, endDate: Long): List<ReviewLog>
    suspend fun saveReviewLog(reviewLog: ReviewLog)
    suspend fun saveReviewLogs(reviewLogs: List<ReviewLog>)
    fun observeReviewCountToday(userId: String, todayStart: Long): Flow<Int>
    suspend fun getTotalReviewCount(userId: String): Int
    suspend fun getAverageQuality(flashcardId: String): Double?
    suspend fun deleteAllByUser(userId: String)
    suspend fun syncReviewLogs(userId: String)
}
