package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.repository.FlashcardRepository
import java.util.UUID
import javax.inject.Inject

class CreateFlashcardUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository
) {
    suspend operator fun invoke(
        userId: String,
        deckId: String,
        frontText: String,
        backText: String,
        exampleText: String? = null,
        imageUrl: String? = null,
        audioUrl: String? = null
    ): Flashcard {
        val newCard = Flashcard(
            id = UUID.randomUUID().toString(),
            userId = userId,
            deckId = deckId,
            frontText = frontText,
            backText = backText,
            exampleText = exampleText,
            imageUrl = imageUrl,
            audioUrl = audioUrl
        )
        flashcardRepository.createFlashcard(newCard)
        return newCard
    }
}
