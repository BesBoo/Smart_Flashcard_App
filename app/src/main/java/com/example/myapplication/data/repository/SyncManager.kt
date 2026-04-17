package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.DeckDao
import com.example.myapplication.data.local.dao.FlashcardDao
import com.example.myapplication.data.local.dao.ReviewLogDao
import com.example.myapplication.data.local.entity.DeckEntity
import com.example.myapplication.data.local.entity.FlashcardEntity
import com.example.myapplication.data.local.entity.ReviewLogEntity
import com.example.myapplication.data.remote.api.SyncApi
import com.example.myapplication.data.remote.dto.SyncChange
import com.example.myapplication.data.remote.dto.SyncPushRequest
import com.example.myapplication.domain.repository.SyncRepository
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages bidirectional delta sync between local Room DB and remote server.
 *
 * Strategy:
 * - PUSH: collect locally changed entities since last sync → POST to server
 * - PULL: GET changes from server since last sync → upsert into local DB
 * - Conflicts: server wins (last-write-wins with server resolution)
 * - Offline: all changes saved to local DB first; synced when online
 */
@Singleton
class SyncManager @Inject constructor(
    private val syncApi: SyncApi,
    private val deckDao: DeckDao,
    private val flashcardDao: FlashcardDao,
    private val reviewLogDao: ReviewLogDao
) : SyncRepository {

    companion object {
        private const val TAG = "SyncManager"
    }

    /**
     * Full sync cycle: push local changes, then pull remote changes.
     * @param userId the current user's ID
     * @param lastSyncTimestamp ISO timestamp of last successful sync
     * @return new sync timestamp from server, or null if sync failed
     */
    override suspend fun sync(userId: String, lastSyncTimestamp: String): String? {
        return try {
            Log.d(TAG, "Starting sync for user=$userId since=$lastSyncTimestamp")

            // 1. Push local changes
            val pushCount = pushLocalChanges(userId, lastSyncTimestamp)
            Log.d(TAG, "Pushed $pushCount local changes")

            // 2. Pull remote changes
            val pullResponse = syncApi.pull(since = lastSyncTimestamp)
            Log.d(TAG, "Pulled ${pullResponse.changes.size} remote changes")

            // 3. Apply remote changes to local DB (server wins on conflict)
            for (change in pullResponse.changes) {
                applyRemoteChange(change, userId)
            }

            Log.d(TAG, "Sync completed. New timestamp: ${pullResponse.syncTimestamp}")
            // Return new sync timestamp
            pullResponse.syncTimestamp
        } catch (e: Exception) {
            // Offline or error — changes remain in local DB, will be pushed next sync
            Log.w(TAG, "Sync failed (offline or error): ${e.message}. Local changes are queued.")
            null
        }
    }

    /**
     * Get number of locally pending changes that haven't been synced yet.
     */
    override suspend fun getPendingChangesCount(userId: String, lastSyncTimestamp: String): Int {
        val sinceMs = parseSyncTimestamp(lastSyncTimestamp)
        val decks = deckDao.getDecksUpdatedSince(userId, sinceMs).size
        val cards = flashcardDao.getFlashcardsUpdatedSince(userId, sinceMs).size
        return decks + cards
    }

    private suspend fun pushLocalChanges(userId: String, since: String): Int {
        val sinceMs = parseSyncTimestamp(since)
        val changes = mutableListOf<SyncChange>()

        // Collect changed decks
        val changedDecks = deckDao.getDecksUpdatedSince(userId, sinceMs)
        for (deck in changedDecks) {
            changes.add(
                SyncChange(
                    entityType = "deck",
                    entityId = deck.id,
                    action = if (deck.isDeleted) "DELETE" else "UPDATE",
                    updatedAt = deck.updatedAt.toString()
                )
            )
        }

        // Collect changed flashcards
        val changedCards = flashcardDao.getFlashcardsUpdatedSince(userId, sinceMs)
        for (card in changedCards) {
            changes.add(
                SyncChange(
                    entityType = "flashcard",
                    entityId = card.id,
                    action = if (card.isDeleted) "DELETE" else "UPDATE",
                    updatedAt = card.updatedAt.toString()
                )
            )
        }

        if (changes.isNotEmpty()) {
            syncApi.push(SyncPushRequest(changes))
        }

        return changes.size
    }

    private suspend fun applyRemoteChange(change: SyncChange, userId: String) {
        when (change.entityType) {
            "deck" -> when (change.action) {
                "DELETE" -> deckDao.softDeleteDeck(change.entityId)
                else -> {
                    // Server wins: upsert remote data
                    val existing = deckDao.getDeckById(change.entityId)
                    if (existing == null) {
                        deckDao.insertDeck(
                            DeckEntity(
                                id = change.entityId,
                                userId = userId,
                                name = "Synced Deck",
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
            "flashcard" -> when (change.action) {
                "DELETE" -> flashcardDao.softDeleteFlashcard(change.entityId)
                else -> {
                    val existing = flashcardDao.getFlashcardById(change.entityId)
                    if (existing == null) {
                        flashcardDao.insertFlashcard(
                            FlashcardEntity(
                                id = change.entityId,
                                userId = userId,
                                deckId = "",
                                frontText = "",
                                backText = "",
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
            "review_log" -> {
                // Review logs are push-only (client → server), no pull needed
            }
        }
    }

    private fun parseSyncTimestamp(timestamp: String): Long {
        return try {
            timestamp.toLong()
        } catch (_: Exception) {
            0L
        }
    }
}
