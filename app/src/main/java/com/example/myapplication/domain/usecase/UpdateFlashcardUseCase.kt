package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.repository.FlashcardRepository
import javax.inject.Inject

class UpdateFlashcardUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository
) {
    suspend operator fun invoke(flashcard: Flashcard) {
        flashcardRepository.updateFlashcard(flashcard)
    }
}
