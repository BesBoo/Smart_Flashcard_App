package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.ReviewLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewLog(reviewLog: ReviewLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewLogs(reviewLogs: List<ReviewLogEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReviewLogsIgnore(reviewLogs: List<ReviewLogEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReviewLogIgnore(reviewLog: ReviewLogEntity)

    // Review history for a specific card
    @Query("SELECT * FROM review_logs WHERE flashcardId = :flashcardId ORDER BY reviewedAt DESC")
    suspend fun getReviewLogsByCard(flashcardId: String): List<ReviewLogEntity>

    // Review history for a user within a date range (only non-deleted cards)
    @Query("""
        SELECT * FROM review_logs 
        WHERE userId = :userId AND reviewedAt BETWEEN :startDate AND :endDate 
          AND flashcardId IN (SELECT id FROM flashcards WHERE isDeleted = 0)
        ORDER BY reviewedAt DESC
    """)
    suspend fun getReviewLogsByDateRange(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<ReviewLogEntity>

    // Count reviews today (for stats)
    @Query("""
        SELECT COUNT(*) FROM review_logs 
        WHERE userId = :userId AND reviewedAt >= :todayStart
    """)
    fun observeReviewCountToday(userId: String, todayStart: Long): Flow<Int>

    // Total reviews for a user (only for non-deleted cards)
    @Query("""
        SELECT COUNT(*) FROM review_logs 
        WHERE userId = :userId 
          AND flashcardId IN (SELECT id FROM flashcards WHERE isDeleted = 0)
    """)
    suspend fun getTotalReviewCount(userId: String): Int

    // Average quality for a card (for AI analysis)
    @Query("SELECT AVG(CAST(quality AS REAL)) FROM review_logs WHERE flashcardId = :flashcardId")
    suspend fun getAverageQuality(flashcardId: String): Double?

    @Query("DELETE FROM review_logs WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: String)
}
