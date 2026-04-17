package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.DeckDao
import com.example.myapplication.data.local.dao.FlashcardDao
import com.example.myapplication.data.local.entity.FlashcardEntity
import com.example.myapplication.data.remote.api.FlashcardApi
import com.example.myapplication.data.remote.dto.CreateDeckRequest
import com.example.myapplication.data.remote.dto.UpdateDeckRequest
import com.example.myapplication.domain.model.Deck
import com.example.myapplication.domain.model.toDomain
import com.example.myapplication.domain.model.toDomainDecks
import com.example.myapplication.domain.model.toEntity
import com.example.myapplication.domain.repository.DeckRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeckRepositoryImpl @Inject constructor(
    private val deckDao: DeckDao,
    private val flashcardDao: FlashcardDao,
    private val flashcardApi: FlashcardApi
) : DeckRepository {

    override fun observeDecksByUser(userId: String): Flow<List<Deck>> =
        // Combine decks with flashcard count so the list re-emits when cards change
        combine(
            deckDao.observeDecksByUser(userId),
            flashcardDao.observeTotalCardCount(userId)
        ) { entities, _ ->
            entities.map { entity ->
                val cardCount = deckDao.getCardCountByDeck(entity.id)
                val dueCount = deckDao.getDueCountByDeck(entity.id)
                entity.toDomain(cardCount = cardCount, dueCount = dueCount)
            }
        }

    override suspend fun getDecksByUser(userId: String): List<Deck> =
        deckDao.getDecksByUser(userId).toDomainDecks()

    override suspend fun getDeckById(deckId: String): Deck? =
        deckDao.getDeckById(deckId)?.toDomain()

    override fun observeDeckById(deckId: String): Flow<Deck?> =
        deckDao.observeDeckById(deckId).map { it?.toDomain() }

    override fun observeCardCount(deckId: String): Flow<Int> =
        deckDao.observeCardCount(deckId)

    override suspend fun createDeck(deck: Deck) {
        deckDao.insertDeck(deck.toEntity())
        try {
            flashcardApi.createDeck(
                CreateDeckRequest(
                    id = deck.id,
                    name = deck.name,
                    description = deck.description,
                    coverImageUrl = deck.coverImageUrl
                )
            )
        } catch (_: Exception) {
            // Offline: deck saved locally, will sync later
        }
    }

    override suspend fun updateDeck(deck: Deck) {
        deckDao.updateDeck(deck.toEntity())
        try {
            flashcardApi.updateDeck(
                id = deck.id,
                request = UpdateDeckRequest(
                    name = deck.name,
                    description = deck.description,
                    coverImageUrl = deck.coverImageUrl
                )
            )
        } catch (_: Exception) {
            // Offline
        }
    }

    override suspend fun deleteDeck(deckId: String) {
        deckDao.softDeleteDeck(deckId)
        try {
            flashcardApi.deleteDeck(deckId)
        } catch (_: Exception) {
            // Offline
        }
    }

    override suspend fun syncDecks(userId: String) {
        try {
            // Step 1: Sync deck metadata from server
            val remoteDecks = mutableListOf<Deck>()
            var cursor: String? = null
            do {
                val page = flashcardApi.getDecks(cursor = cursor, limit = 200)
                remoteDecks += page.data.map { dto ->
                    Deck(
                        id = dto.id,
                        userId = userId,
                        name = dto.name,
                        description = dto.description,
                        coverImageUrl = dto.coverImageUrl,
                        language = dto.language,
                        cardCount = dto.cardCount,
                        dueCount = dto.dueCount,
                        isOwner = dto.isOwner,
                        permission = dto.permission,
                        ownerName = dto.ownerName,
                        shareCode = dto.shareCode,
                        isShared = dto.isShared,
                        googleSheetUrl = dto.googleSheetUrl
                    )
                }
                cursor = if (page.hasMore) page.nextCursor else null
            } while (cursor != null)

            if (remoteDecks.isNotEmpty()) {
                // Safe upsert: IGNORE new + UPDATE existing to avoid CASCADE delete
                val entities = remoteDecks.map { it.toEntity() }
                deckDao.insertDecksIgnore(entities)
                for (e in entities) {
                    deckDao.updateDeckFields(
                        id = e.id, name = e.name, description = e.description,
                        coverImageUrl = e.coverImageUrl, isOwner = e.isOwner,
                        permission = e.permission, ownerName = e.ownerName,
                        shareCode = e.shareCode, isShared = e.isShared,
                        googleSheetUrl = e.googleSheetUrl, isDeleted = e.isDeleted,
                        updatedAt = e.updatedAt
                    )
                }
            }

            // Step 2: Sync flashcards for each deck from server
            for (deck in remoteDecks) {
                try {
                    val remoteCards = mutableListOf<FlashcardEntity>()
                    var cardCursor: String? = null
                    do {
                        val cardPage = flashcardApi.getCards(
                            deckId = deck.id,
                            cursor = cardCursor,
                            limit = 200
                        )
                        remoteCards += cardPage.data.map { dto ->
                            FlashcardEntity(
                                id = dto.id,
                                userId = userId,
                                deckId = dto.deckId,
                                frontText = dto.frontText,
                                backText = dto.backText,
                                exampleText = dto.exampleText,
                                imageUrl = dto.imageUrl,
                                audioUrl = dto.audioUrl,
                                repetition = dto.repetition,
                                intervalDays = dto.intervalDays,
                                easeFactor = dto.easeFactor,
                                nextReviewDate = try {
                                    dto.nextReviewDate.toLong()
                                } catch (_: Exception) {
                                    System.currentTimeMillis()
                                },
                                failCount = dto.failCount,
                                totalReviews = dto.totalReviews
                            )
                        }
                        cardCursor = if (cardPage.hasMore) cardPage.nextCursor else null
                    } while (cardCursor != null)

                    if (remoteCards.isNotEmpty()) {
                        // Preserve local SM2 state: if a card already exists locally
                        // with study progress, keep the local SM2 fields instead of
                        // overwriting with server values (which may be stale).
                        val mergedCards = remoteCards.map { remote ->
                            val local = flashcardDao.getFlashcardById(remote.id)
                            if (local != null && local.totalReviews > 0) {
                                // Keep local SM2 progress, update content from server
                                remote.copy(
                                    repetition = local.repetition,
                                    intervalDays = local.intervalDays,
                                    easeFactor = local.easeFactor,
                                    nextReviewDate = local.nextReviewDate,
                                    failCount = local.failCount,
                                    totalReviews = local.totalReviews
                                )
                            } else {
                                remote
                            }
                        }
                        // Safe upsert: IGNORE new + UPDATE existing to avoid CASCADE delete
                        flashcardDao.insertFlashcardsIgnore(mergedCards)
                        for (c in mergedCards) {
                            flashcardDao.updateCardFields(
                                id = c.id, frontText = c.frontText, backText = c.backText,
                                exampleText = c.exampleText, imageUrl = c.imageUrl, audioUrl = c.audioUrl,
                                repetition = c.repetition, intervalDays = c.intervalDays,
                                easeFactor = c.easeFactor, nextReviewDate = c.nextReviewDate,
                                failCount = c.failCount, totalReviews = c.totalReviews
                            )
                        }
                    }
                } catch (_: Exception) {
                    // Skip card sync for this deck if it fails
                }
            }
        } catch (_: Exception) {
            // Offline — keep existing Room data
        }
    }
    override suspend fun syncFlashcardsForDeck(deckId: String, userId: String) {
        try {
            val remoteCards = mutableListOf<FlashcardEntity>()
            var cardCursor: String? = null
            do {
                val cardPage = flashcardApi.getCards(
                    deckId = deckId,
                    cursor = cardCursor,
                    limit = 200
                )
                remoteCards += cardPage.data.map { dto ->
                    FlashcardEntity(
                        id = dto.id,
                        userId = userId,
                        deckId = dto.deckId,
                        frontText = dto.frontText,
                        backText = dto.backText,
                        exampleText = dto.exampleText,
                        imageUrl = dto.imageUrl,
                        audioUrl = dto.audioUrl,
                        repetition = dto.repetition,
                        intervalDays = dto.intervalDays,
                        easeFactor = dto.easeFactor,
                        nextReviewDate = try {
                            dto.nextReviewDate.toLong()
                        } catch (_: Exception) {
                            System.currentTimeMillis()
                        },
                        failCount = dto.failCount,
                        totalReviews = dto.totalReviews
                    )
                }
                cardCursor = if (cardPage.hasMore) cardPage.nextCursor else null
            } while (cardCursor != null)

            if (remoteCards.isNotEmpty()) {
                // Preserve local SM2 state for cards with study progress
                val mergedCards = remoteCards.map { remote ->
                    val local = flashcardDao.getFlashcardById(remote.id)
                    if (local != null && local.totalReviews > 0) {
                        remote.copy(
                            repetition = local.repetition,
                            intervalDays = local.intervalDays,
                            easeFactor = local.easeFactor,
                            nextReviewDate = local.nextReviewDate,
                            failCount = local.failCount,
                            totalReviews = local.totalReviews
                        )
                    } else {
                        remote
                    }
                }
                // Safe upsert: IGNORE new + UPDATE existing to avoid CASCADE delete
                flashcardDao.insertFlashcardsIgnore(mergedCards)
                for (c in mergedCards) {
                    flashcardDao.updateCardFields(
                        id = c.id, frontText = c.frontText, backText = c.backText,
                        exampleText = c.exampleText, imageUrl = c.imageUrl, audioUrl = c.audioUrl,
                        repetition = c.repetition, intervalDays = c.intervalDays,
                        easeFactor = c.easeFactor, nextReviewDate = c.nextReviewDate,
                        failCount = c.failCount, totalReviews = c.totalReviews
                    )
                }
            }

            // Remove local cards that no longer exist on server
            val localCards = flashcardDao.getFlashcardsByDeck(deckId)
            val remoteIds = remoteCards.map { it.id }.toSet()
            for (local in localCards) {
                if (local.id !in remoteIds) {
                    flashcardDao.softDeleteFlashcard(local.id)
                }
            }
        } catch (_: Exception) {
            // Offline — keep existing data
        }
    }
}
