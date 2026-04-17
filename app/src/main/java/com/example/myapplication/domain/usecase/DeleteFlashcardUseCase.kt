package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.repository.FlashcardRepository
import javax.inject.Inject

class DeleteFlashcardUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository
) {
    suspend operator fun invoke(flashcardId: String) {
        flashcardRepository.deleteFlashcard(flashcardId)
    }
}
