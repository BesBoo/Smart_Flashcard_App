package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.FlashcardDao
import com.example.myapplication.data.remote.api.FlashcardApi
import com.example.myapplication.data.remote.dto.CreateFlashcardRequest
import com.example.myapplication.data.remote.dto.UpdateFlashcardRequest
import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.model.toDomain
import com.example.myapplication.domain.model.toDomainFlashcards
import com.example.myapplication.domain.model.toEntity
import com.example.myapplication.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashcardRepositoryImpl @Inject constructor(
    private val flashcardDao: FlashcardDao,
    private val flashcardApi: FlashcardApi
) : FlashcardRepository {

    override fun observeFlashcardsByDeck(deckId: String): Flow<List<Flashcard>> =
        flashcardDao.observeFlashcardsByDeck(deckId).map { it.toDomainFlashcards() }

    override suspend fun getFlashcardsByDeck(deckId: String): List<Flashcard> =
        flashcardDao.getFlashcardsByDeck(deckId).toDomainFlashcards()

    override suspend fun getFlashcardById(cardId: String): Flashcard? =
        flashcardDao.getFlashcardById(cardId)?.toDomain()

    override suspend fun getDueCards(userId: String): List<Flashcard> =
        flashcardDao.getDueCards(userId).toDomainFlashcards()

    override suspend fun getDueCardsByDeck(userId: String, deckId: String): List<Flashcard> =
        flashcardDao.getDueCardsByDeck(userId, deckId).toDomainFlashcards()

    override suspend fun getNewCards(deckId: String, limit: Int): List<Flashcard> =
        flashcardDao.getNewCards(deckId, limit).toDomainFlashcards()

    override suspend fun getAllCardsByUser(userId: String): List<Flashcard> =
        flashcardDao.getAllCardsByUser(userId).toDomainFlashcards()

    override fun observeDueCardCount(userId: String): Flow<Int> =
        flashcardDao.observeDueCardCount(userId)

    override suspend fun createFlashcard(flashcard: Flashcard) {
        flashcardDao.insertFlashcard(flashcard.toEntity())
        try {
            flashcardApi.createCard(
                CreateFlashcardRequest(
                    id = flashcard.id,
                    deckId = flashcard.deckId,
                    frontText = flashcard.frontText,
                    backText = flashcard.backText,
                    exampleText = flashcard.exampleText,
                    imageUrl = flashcard.imageUrl,
                    audioUrl = flashcard.audioUrl,
                    repetition = flashcard.sm2.repetition,
                    intervalDays = flashcard.sm2.intervalDays,
                    easeFactor = flashcard.sm2.easeFactor,
                    failCount = flashcard.sm2.failCount,
                    totalReviews = flashcard.sm2.totalReviews
                )
            )
        } catch (_: Exception) {
            // Offline
        }
    }

    override suspend fun updateFlashcard(flashcard: Flashcard) {
        flashcardDao.updateFlashcard(flashcard.toEntity())
        try {
            // Convert Unix millis to ISO 8601 for server
            val isoDate = java.time.Instant.ofEpochMilli(flashcard.sm2.nextReviewDate)
                .toString() // e.g. "2024-04-15T12:00:00Z"

            flashcardApi.updateCard(
                id = flashcard.id,
                request = UpdateFlashcardRequest(
                    frontText = flashcard.frontText,
                    backText = flashcard.backText,
                    exampleText = flashcard.exampleText,
                    imageUrl = flashcard.imageUrl,
                    audioUrl = flashcard.audioUrl,
                    repetition = flashcard.sm2.repetition,
                    intervalDays = flashcard.sm2.intervalDays,
                    easeFactor = flashcard.sm2.easeFactor,
                    nextReviewDate = isoDate,
                    failCount = flashcard.sm2.failCount,
                    totalReviews = flashcard.sm2.totalReviews
                )
            )
        } catch (e: Exception) {
            android.util.Log.w("FlashcardRepo", "updateCard API failed: ${e.message}")
        }
    }

    override suspend fun updateSm2Fields(
        cardId: String, repetition: Int, intervalDays: Int,
        easeFactor: Double, nextReviewDate: Long, quality: Int
    ) {
        flashcardDao.updateSm2Fields(
            cardId = cardId,
            repetition = repetition,
            intervalDays = intervalDays,
            easeFactor = easeFactor,
            nextReviewDate = nextReviewDate,
            quality = quality
        )
        // Sync SM2 progress to server so it persists across logout/login
        try {
            val card = flashcardDao.getFlashcardById(cardId)
            if (card != null) {
                flashcardApi.updateCard(
                    id = cardId,
                    request = UpdateFlashcardRequest(
                        frontText = card.frontText,
                        backText = card.backText,
                        exampleText = card.exampleText,
                        imageUrl = card.imageUrl,
                        audioUrl = card.audioUrl,
                        repetition = repetition,
                        intervalDays = intervalDays,
                        easeFactor = easeFactor,
                        nextReviewDate = java.time.Instant.ofEpochMilli(nextReviewDate).toString(),
                        failCount = card.failCount,
                        totalReviews = card.totalReviews
                    )
                )
            }
        } catch (_: Exception) {
            // Offline — server will sync later
        }
    }

    override suspend fun deleteFlashcard(cardId: String) {
        flashcardDao.softDeleteFlashcard(cardId)
        try {
            flashcardApi.deleteCard(cardId)
        } catch (_: Exception) {
            // Offline
        }
    }

    override suspend fun syncFlashcards(userId: String) {
        // Sync is handled by SyncManager
    }
}
