package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.Flashcard

/**
 * Domain-level AI repository. Uses domain models only, not DTOs.
 */
interface AiRepository {
    /** Generate flashcard drafts from text */
    suspend fun generateFromText(text: String, language: String = "vi", maxCards: Int = 10): List<Flashcard>

    /** Generate flashcards from uploaded PDF file */
    suspend fun generateFromPdf(inputStream: java.io.InputStream, fileName: String, language: String = "vi", maxCards: Int = 10): List<Flashcard>

    /** Generate flashcards from uploaded DOCX file */
    suspend fun generateFromDocx(inputStream: java.io.InputStream, fileName: String, language: String = "vi", maxCards: Int = 10): List<Flashcard>

    /** Extract vocabulary from uploaded PDF/DOCX file */
    suspend fun extractVocabulary(inputStream: java.io.InputStream, fileName: String, targetLanguage: String = "vi", maxWords: Int = 20): List<Flashcard>

    /** Generate example sentence for a card */
    suspend fun generateExample(frontText: String, backText: String, language: String = "vi"): String

    /** Generate quiz questions from cards */
    suspend fun generateQuiz(cards: List<Flashcard>, questionCount: Int = 10, language: String = "vi"): List<QuizQuestion>

    /** AI tutor chat — returns response text */
    suspend fun tutorChat(sessionId: String, message: String, language: String = "vi", history: List<AiChatMessage>? = null): String

    /** Get adaptive hint for a struggling card */
    suspend fun getAdaptiveHint(flashcard: Flashcard, language: String = "vi"): AdaptiveHintResult

    /** Generate image URL for a flashcard */
    suspend fun generateImageUrl(frontText: String, backText: String): String?

    /** AI-15: Get remaining AI usage count for the current user today */
    suspend fun getRemainingUsage(): Int

    /** Polysemy: Analyze a word for all meanings, variants, and homonyms */
    suspend fun analyzeWord(word: String, definition: String, context: String? = null): WordAnalysisResult

    /** Smart Review: Generate variant-based cloze questions from vocabulary */
    suspend fun generateSmartReview(words: List<SmartReviewInput>, questionCount: Int = 10, language: String = "en"): List<com.example.myapplication.domain.model.VariantQuestion>

    /** Generate IPA pronunciation for a word (with community cache) */
    suspend fun generateIpa(frontText: String, backText: String): String
}

/** Input for Smart Review question generation */
data class SmartReviewInput(
    val word: String,
    val partOfSpeech: String = "",
    val definition: String = "",
    val sourceCardId: String? = null
)

/** Domain-level quiz question (no DTO dependency) */
data class QuizQuestion(
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val sourceCardId: String
)

/** Domain-level typing question: user sees prompt + types the answer */
data class TypingQuestion(
    val promptText: String,       // The word/text shown to user (e.g. Vietnamese)
    val correctAnswer: String,    // Expected answer (e.g. English)
    val sourceCardId: String
)

/** Domain-level chat message */
data class AiChatMessage(
    val role: String,       // "user" or "assistant"
    val content: String
)

/** Domain-level adaptive hint result */
data class AdaptiveHintResult(
    val simplifiedExplanation: String,
    val additionalExamples: List<String>,
    val shouldSplit: Boolean = false,
    val suggestedCards: List<Flashcard>? = null
)

/** Domain-level word analysis result (polysemy) */
data class WordAnalysisResult(
    val lemma: String,
    val detectedPOS: String,
    val mainSense: WordSenseItem,
    val relatedVariants: List<WordSenseItem> = emptyList(),
    val otherMeanings: List<WordSenseItem> = emptyList(),
    val wordVariants: List<WordVariantItem> = emptyList()
)

data class WordSenseItem(
    val partOfSpeech: String,
    val definitionEn: String,
    val definitionVi: String?,
    val example: String?,
    val similarityScore: Int,
    val homonymCluster: String? = null,
    val isSelected: Boolean = true
)

data class WordVariantItem(
    val variant: String,
    val type: String
)
