package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.Deck
import com.example.myapplication.domain.repository.DeckRepository
import java.util.UUID
import javax.inject.Inject

class CreateDeckUseCase @Inject constructor(
    private val deckRepository: DeckRepository
) {
    suspend operator fun invoke(
        userId: String,
        name: String,
        description: String? = null,
        coverImageUrl: String? = null
    ): Deck {
        val newDeck = Deck(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = name,
            description = description,
            coverImageUrl = coverImageUrl
        )
        deckRepository.createDeck(newDeck)
        return newDeck
    }
}
