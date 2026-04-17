package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.Flashcard
import kotlinx.coroutines.flow.Flow

interface FlashcardRepository {
    fun observeFlashcardsByDeck(deckId: String): Flow<List<Flashcard>>
    suspend fun getFlashcardsByDeck(deckId: String): List<Flashcard>
    suspend fun getFlashcardById(cardId: String): Flashcard?
    suspend fun getDueCards(userId: String): List<Flashcard>
    suspend fun getDueCardsByDeck(userId: String, deckId: String): List<Flashcard>
    suspend fun getAllCardsByUser(userId: String): List<Flashcard>
    suspend fun getNewCards(deckId: String, limit: Int = 20): List<Flashcard>
    fun observeDueCardCount(userId: String): Flow<Int>
    suspend fun createFlashcard(flashcard: Flashcard)
    suspend fun updateFlashcard(flashcard: Flashcard)
    suspend fun updateSm2Fields(
        cardId: String, repetition: Int, intervalDays: Int,
        easeFactor: Double, nextReviewDate: Long, quality: Int
    )
    suspend fun deleteFlashcard(cardId: String)
    suspend fun syncFlashcards(userId: String)
}
