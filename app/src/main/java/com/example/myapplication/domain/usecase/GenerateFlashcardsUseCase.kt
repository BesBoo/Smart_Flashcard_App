package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.Deck
import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.repository.AiRepository
import com.example.myapplication.domain.repository.FlashcardRepository
import java.io.InputStream
import javax.inject.Inject

class GenerateFlashcardsUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val flashcardRepository: FlashcardRepository
) {
    /**
     * Generates flashcard drafts from text via AI, saves them to the given deck.
     */
    suspend operator fun invoke(
        text: String,
        deck: Deck,
        userId: String,
        language: String = "vi",
        maxCards: Int = 10
    ): List<Flashcard> {
        val drafts = aiRepository.generateFromText(text, language, maxCards)
        return saveAndReturn(drafts, deck, userId)
    }

    /**
     * Generates flashcard drafts from uploaded file (PDF/DOCX) via AI.
     */
    suspend operator fun invoke(
        inputStream: InputStream,
        fileName: String,
        fileType: String,
        deck: Deck,
        userId: String,
        language: String = "vi",
        maxCards: Int = 10
    ): List<Flashcard> {
        val drafts = when (fileType.lowercase()) {
            "pdf" -> aiRepository.generateFromPdf(inputStream, fileName, language, maxCards)
            "docx" -> aiRepository.generateFromDocx(inputStream, fileName, language, maxCards)
            else -> throw IllegalArgumentException("Unsupported file type: $fileType")
        }
        return saveAndReturn(drafts, deck, userId)
    }

    /**
     * Extracts vocabulary from uploaded file (PDF/DOCX) via AI vocabulary extraction.
     */
    suspend fun extractVocabulary(
        inputStream: InputStream,
        fileName: String,
        deck: Deck,
        userId: String,
        targetLanguage: String = "vi",
        maxWords: Int = 20
    ): List<Flashcard> {
        val drafts = aiRepository.extractVocabulary(inputStream, fileName, targetLanguage, maxWords)
        return saveAndReturn(drafts, deck, userId)
    }

    private suspend fun saveAndReturn(drafts: List<Flashcard>, deck: Deck, userId: String): List<Flashcard> {
        val newCards = drafts.map { draft ->
            draft.copy(userId = userId, deckId = deck.id)
        }
        newCards.forEach { flashcardRepository.createFlashcard(it) }
        return newCards
    }
}
