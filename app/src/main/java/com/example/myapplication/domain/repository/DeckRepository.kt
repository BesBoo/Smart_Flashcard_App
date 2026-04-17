package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.Deck
import kotlinx.coroutines.flow.Flow

interface DeckRepository {
    fun observeDecksByUser(userId: String): Flow<List<Deck>>
    suspend fun getDecksByUser(userId: String): List<Deck>
    suspend fun getDeckById(deckId: String): Deck?
    fun observeDeckById(deckId: String): Flow<Deck?>
    fun observeCardCount(deckId: String): Flow<Int>
    suspend fun createDeck(deck: Deck)
    suspend fun updateDeck(deck: Deck)
    suspend fun deleteDeck(deckId: String)
    suspend fun syncDecks(userId: String)
    suspend fun syncFlashcardsForDeck(deckId: String, userId: String)
}
