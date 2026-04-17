package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.local.entity.DeckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecks(decks: List<DeckEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDeckIgnore(deck: DeckEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDecksIgnore(decks: List<DeckEntity>)

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Query("SELECT * FROM decks WHERE userId = :userId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeDecksByUser(userId: String): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE userId = :userId AND isDeleted = 0 ORDER BY updatedAt DESC")
    suspend fun getDecksByUser(userId: String): List<DeckEntity>

    @Query("SELECT * FROM decks WHERE id = :deckId")
    suspend fun getDeckById(deckId: String): DeckEntity?

    @Query("SELECT * FROM decks WHERE id = :deckId")
    fun observeDeckById(deckId: String): Flow<DeckEntity?>

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId AND isDeleted = 0")
    fun observeCardCount(deckId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId AND isDeleted = 0")
    suspend fun getCardCountByDeck(deckId: String): Int

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId AND isDeleted = 0 AND nextReviewDate <= :now")
    suspend fun getDueCountByDeck(deckId: String, now: Long = System.currentTimeMillis()): Int

    // Soft delete
    @Query("UPDATE decks SET isDeleted = 1, updatedAt = :timestamp WHERE id = :deckId")
    suspend fun softDeleteDeck(deckId: String, timestamp: Long = System.currentTimeMillis())

    // Hard delete (for sync cleanup)
    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    @Query("DELETE FROM decks WHERE userId = :userId")
    suspend fun deleteAllDecksByUser(userId: String)

    // Safe upsert — avoids CASCADE delete triggered by REPLACE
    @Query("""
        UPDATE decks SET 
            name = :name, description = :description, coverImageUrl = :coverImageUrl,
            isOwner = :isOwner, permission = :permission, ownerName = :ownerName,
            shareCode = :shareCode, isShared = :isShared, googleSheetUrl = :googleSheetUrl,
            isDeleted = :isDeleted, updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateDeckFields(
        id: String, name: String, description: String?, coverImageUrl: String?,
        isOwner: Boolean, permission: String?, ownerName: String?,
        shareCode: String?, isShared: Boolean, googleSheetUrl: String?,
        isDeleted: Boolean, updatedAt: Long
    )

    // Sync support
    @Query("SELECT * FROM decks WHERE userId = :userId AND updatedAt > :since")
    suspend fun getDecksUpdatedSince(userId: String, since: Long): List<DeckEntity>
}
