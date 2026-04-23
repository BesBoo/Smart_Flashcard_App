package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.api.AiApi
import com.example.myapplication.data.remote.dto.AiAdaptiveRequest
import com.example.myapplication.data.remote.dto.AiExampleRequest
import com.example.myapplication.data.remote.dto.AiGenerateTextRequest
import com.example.myapplication.data.remote.dto.AiQuizRequest
import com.example.myapplication.data.remote.dto.AiTutorRequest
import com.example.myapplication.data.remote.dto.ChatMessage
import com.example.myapplication.data.remote.dto.FlashcardInfo
import com.example.myapplication.data.remote.dto.QuizCardInput
import com.example.myapplication.data.remote.dto.RecentReview
import com.example.myapplication.domain.model.Flashcard
import com.example.myapplication.domain.model.SM2Data
import com.example.myapplication.domain.repository.AdaptiveHintResult
import com.example.myapplication.domain.repository.AiChatMessage
import com.example.myapplication.domain.repository.AiRepository
import com.example.myapplication.domain.repository.QuizQuestion
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val aiApi: AiApi
) : AiRepository {

    override suspend fun generateFromText(text: String, language: String, maxCards: Int): List<Flashcard> {
        val response = aiApi.generateFromText(AiGenerateTextRequest(text, language, maxCards))
        return response.drafts.map { draft ->
            Flashcard(
                id = UUID.randomUUID().toString(),
                userId = "",  // will be set by the use case
                deckId = "",  // will be set by the use case
                frontText = draft.frontText,
                backText = draft.backText,
                exampleText = draft.exampleText
            )
        }
    }

    override suspend fun generateFromPdf(inputStream: java.io.InputStream, fileName: String, language: String, maxCards: Int): List<Flashcard> {
        val bytes = inputStream.readBytes()
        val requestFile = bytes.toRequestBody("application/pdf".toMediaType())
        val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)
        val languagePart = language.toRequestBody("text/plain".toMediaType())
        val maxCardsPart = maxCards.toString().toRequestBody("text/plain".toMediaType())

        val response = aiApi.generateFromPdf(filePart, languagePart, maxCardsPart)
        return response.drafts.map { draft ->
            Flashcard(
                id = UUID.randomUUID().toString(),
                userId = "",
                deckId = "",
                frontText = draft.frontText,
                backText = draft.backText,
                exampleText = draft.exampleText
            )
        }
    }

    override suspend fun generateFromDocx(inputStream: java.io.InputStream, fileName: String, language: String, maxCards: Int): List<Flashcard> {
        val bytes = inputStream.readBytes()
        val requestFile = bytes.toRequestBody(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document".toMediaType()
        )
        val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)
        val languagePart = language.toRequestBody("text/plain".toMediaType())
        val maxCardsPart = maxCards.toString().toRequestBody("text/plain".toMediaType())

        val response = aiApi.generateFromDocx(filePart, languagePart, maxCardsPart)
        return response.drafts.map { draft ->
            Flashcard(
                id = UUID.randomUUID().toString(),
                userId = "",
                deckId = "",
                frontText = draft.frontText,
                backText = draft.backText,
                exampleText = draft.exampleText
            )
        }
    }

    override suspend fun extractVocabulary(inputStream: java.io.InputStream, fileName: String, targetLanguage: String, maxWords: Int): List<Flashcard> {
        val bytes = inputStream.readBytes()
        val mimeType = if (fileName.endsWith(".pdf", true)) "application/pdf"
            else "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        val requestFile = bytes.toRequestBody(mimeType.toMediaType())
        val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)
        val targetLangPart = targetLanguage.toRequestBody("text/plain".toMediaType())
        val maxWordsPart = maxWords.toString().toRequestBody("text/plain".toMediaType())

        val response = aiApi.extractVocabulary(filePart, targetLangPart, maxWordsPart)
        return response.drafts.map { draft ->
            Flashcard(
                id = UUID.randomUUID().toString(),
                userId = "",
                deckId = "",
                frontText = draft.frontText,
                backText = draft.backText,
                exampleText = draft.exampleText
            )
        }
    }

    override suspend fun generateExample(frontText: String, backText: String, language: String): String {
        val response = aiApi.generateExample(AiExampleRequest(frontText, backText, language))
        return response.example
    }

    override suspend fun generateQuiz(cards: List<Flashcard>, questionCount: Int, language: String): List<QuizQuestion> {
        val inputs = cards.map {
            QuizCardInput(id = it.id, frontText = it.frontText, backText = it.backText)
        }
        val response = aiApi.generateQuiz(AiQuizRequest(inputs, questionCount, language))
        return response.questions.map { q ->
            QuizQuestion(
                questionText = q.questionText,
                options = q.options,
                correctIndex = q.correctIndex,
                sourceCardId = q.sourceCardId
            )
        }
    }

    override suspend fun tutorChat(sessionId: String, message: String, language: String, history: List<AiChatMessage>?): String {
        val dtoHistory = history?.map { ChatMessage(role = it.role, content = it.content) }
        val response = aiApi.tutorChat(AiTutorRequest(sessionId, message, language, dtoHistory))
        return response.response
    }

    override suspend fun getAdaptiveHint(flashcard: Flashcard, language: String): AdaptiveHintResult {
        val info = FlashcardInfo(
            frontText = flashcard.frontText,
            backText = flashcard.backText,
            exampleText = flashcard.exampleText,
            failCount = flashcard.sm2.failCount
        )
        val response = aiApi.getAdaptiveHint(AiAdaptiveRequest(info, emptyList(), language))
        val hint = response.hint
        return AdaptiveHintResult(
            simplifiedExplanation = hint.simplifiedExplanation,
            additionalExamples = hint.additionalExamples,
            shouldSplit = hint.splitSuggestion?.suggested ?: false,
            suggestedCards = hint.splitSuggestion?.cards?.map { draft ->
                Flashcard(
                    id = UUID.randomUUID().toString(),
                    userId = flashcard.userId,
                    deckId = flashcard.deckId,
                    frontText = draft.frontText,
                    backText = draft.backText,
                    exampleText = draft.exampleText
                )
            }
        )
    }

    override suspend fun generateImageUrl(frontText: String, backText: String): String? {
        val response = aiApi.generateImage(
            com.example.myapplication.data.remote.dto.AiImageRequest(frontText, backText)
        )
        return response.imageUrl
    }

    override suspend fun getRemainingUsage(): Int {
        return try {
            val response = aiApi.getRemainingUsage()
            response.remaining
        } catch (_: Exception) {
            20 // Default fallback
        }
    }

    override suspend fun analyzeWord(
        word: String,
        definition: String,
        context: String?
    ): com.example.myapplication.domain.repository.WordAnalysisResult {
        val response = aiApi.analyzeWord(
            com.example.myapplication.data.remote.dto.WordAnalyzeRequest(word, definition, context)
        )
        return com.example.myapplication.domain.repository.WordAnalysisResult(
            lemma = response.lemma,
            detectedPOS = response.detectedPOS,
            mainSense = response.mainSense.toDomain(),
            relatedVariants = response.relatedVariants.map { it.toDomain() },
            otherMeanings = response.otherMeanings.map { it.toDomain(isSelected = false) },
            wordVariants = response.wordVariants.map {
                com.example.myapplication.domain.repository.WordVariantItem(
                    variant = it.variant ?: "",
                    type = it.type ?: ""
                )
            }
        )
    }

    override suspend fun generateSmartReview(
        words: List<com.example.myapplication.domain.repository.SmartReviewInput>,
        questionCount: Int,
        language: String
    ): List<com.example.myapplication.domain.model.VariantQuestion> {
        val dtoWords = words.map {
            com.example.myapplication.data.remote.dto.SmartReviewWordDto(
                word = it.word,
                partOfSpeech = it.partOfSpeech,
                definition = it.definition,
                sourceCardId = it.sourceCardId
            )
        }
        val response = aiApi.smartReview(
            com.example.myapplication.data.remote.dto.SmartReviewRequest(dtoWords, questionCount, language)
        )
        return response.questions.map { q ->
            com.example.myapplication.domain.model.VariantQuestion(
                sentence = q.sentence,
                baseWord = q.baseWord,
                options = q.options,
                correctIndex = q.correctIndex,
                sourceCardId = q.sourceCardId
            )
        }
    }

    private fun com.example.myapplication.data.remote.dto.SenseDto.toDomain(
        isSelected: Boolean = true
    ) = com.example.myapplication.domain.repository.WordSenseItem(
        partOfSpeech = partOfSpeech ?: "unknown",
        definitionEn = definitionEn ?: "",
        definitionVi = definitionVi,
        example = example,
        similarityScore = similarityScore,
        homonymCluster = homonymCluster,
        isSelected = isSelected
    )
}
