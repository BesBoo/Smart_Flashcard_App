package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.repository.AiRepository
import com.example.myapplication.domain.repository.QuizQuestion
import javax.inject.Inject

class GenerateQuizUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(
        flashcards: List<Flashcard>,
        questionCount: Int = 10,
        language: String = "vi"
    ): List<QuizQuestion> {
        return aiRepository.generateQuiz(flashcards, questionCount, language)
    }
}
