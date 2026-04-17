package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.local.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>)

    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)

    // Get cards in a deck
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun observeFlashcardsByDeck(deckId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getFlashcardsByDeck(deckId: String): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE id = :cardId")
    suspend fun getFlashcardById(cardId: String): FlashcardEntity?

    // ── SM-2 Study Queries ──

    // Due cards: nextReviewDate <= now, sorted oldest first (exclude cards in deleted decks)
    @Query("""
        SELECT * FROM flashcards 
        WHERE userId = :userId AND isDeleted = 0 
          AND nextReviewDate <= :now
          AND deckId IN (SELECT id FROM decks WHERE isDeleted = 0)
        ORDER BY nextReviewDate ASC
    """)
    suspend fun getDueCards(userId: String, now: Long = System.currentTimeMillis()): List<FlashcardEntity>

    // Due cards for a specific deck
    @Query("""
        SELECT * FROM flashcards 
        WHERE userId = :userId AND deckId = :deckId AND isDeleted = 0 
          AND nextReviewDate <= :now
        ORDER BY nextReviewDate ASC
    """)
    suspend fun getDueCardsByDeck(userId: String, deckId: String, now: Long = System.currentTimeMillis()): List<FlashcardEntity>

    // New cards (never reviewed: repetition == 0)
    @Query("""
        SELECT * FROM flashcards 
        WHERE deckId = :deckId AND isDeleted = 0 AND repetition = 0
        ORDER BY createdAt ASC
        LIMIT :limit
    """)
    suspend fun getNewCards(deckId: String, limit: Int = 20): List<FlashcardEntity>

    // All cards for a user (for stats — exclude cards in deleted decks)
    @Query("""
        SELECT * FROM flashcards 
        WHERE userId = :userId AND isDeleted = 0
          AND deckId IN (SELECT id FROM decks WHERE isDeleted = 0)
    """)
    suspend fun getAllCardsByUser(userId: String): List<FlashcardEntity>

    // Count due cards for a user (exclude cards in deleted decks)
    @Query("""
        SELECT COUNT(*) FROM flashcards 
        WHERE userId = :userId AND isDeleted = 0 
          AND nextReviewDate <= :now
          AND deckId IN (SELECT id FROM decks WHERE isDeleted = 0)
    """)
    fun observeDueCardCount(userId: String, now: Long = System.currentTimeMillis()): Flow<Int>

    // Update SM-2 fields after review
    @Query("""
        UPDATE flashcards SET 
            repetition = :repetition, 
            intervalDays = :intervalDays, 
            easeFactor = :easeFactor, 
            nextReviewDate = :nextReviewDate,
            totalReviews = totalReviews + 1,
            failCount = CASE WHEN :quality = 0 THEN failCount + 1 ELSE failCount END,
            updatedAt = :updatedAt
        WHERE id = :cardId
    """)
    suspend fun updateSm2Fields(
        cardId: String,
        repetition: Int,
        intervalDays: Int,
        easeFactor: Double,
        nextReviewDate: Long,
        quality: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    // Soft delete
    @Query("UPDATE flashcards SET isDeleted = 1, updatedAt = :timestamp WHERE id = :cardId")
    suspend fun softDeleteFlashcard(cardId: String, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)

    // Sync support
    @Query("SELECT * FROM flashcards WHERE userId = :userId AND updatedAt > :since")
    suspend fun getFlashcardsUpdatedSince(userId: String, since: Long): List<FlashcardEntity>

    // Observe all flashcard count by user (triggers re-emission when cards change)
    @Query("SELECT COUNT(*) FROM flashcards WHERE userId = :userId AND isDeleted = 0")
    fun observeTotalCardCount(userId: String): Flow<Int>
}
