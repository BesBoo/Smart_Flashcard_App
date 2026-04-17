package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.repository.AiChatMessage
import com.example.myapplication.domain.repository.AiRepository
import javax.inject.Inject

class AiTutorUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        message: String,
        language: String = "vi",
        history: List<AiChatMessage>? = null
    ): String {
        return aiRepository.tutorChat(sessionId, message, language, history)
    }
}
