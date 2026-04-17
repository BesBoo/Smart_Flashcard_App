package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.repository.DeckRepository
import javax.inject.Inject

class DeleteDeckUseCase @Inject constructor(
    private val deckRepository: DeckRepository
) {
    suspend operator fun invoke(deckId: String) {
        deckRepository.deleteDeck(deckId)
    }
}
