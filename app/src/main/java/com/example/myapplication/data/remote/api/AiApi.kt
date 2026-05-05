package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.dto.AiAdaptiveRequest
import com.example.myapplication.data.remote.dto.AiAdaptiveResponse
import com.example.myapplication.data.remote.dto.AiExampleRequest
import com.example.myapplication.data.remote.dto.AiExampleResponse
import com.example.myapplication.data.remote.dto.AiGenerateResponse
import com.example.myapplication.data.remote.dto.AiGenerateTextRequest
import com.example.myapplication.data.remote.dto.AiImageRequest
import com.example.myapplication.data.remote.dto.AiImageResponse
import com.example.myapplication.data.remote.dto.AiQuizRequest
import com.example.myapplication.data.remote.dto.AiQuizResponse
import com.example.myapplication.data.remote.dto.AiTutorRequest
import com.example.myapplication.data.remote.dto.AiTutorResponse
import com.example.myapplication.data.remote.dto.AiUsageResponse
import com.example.myapplication.data.remote.dto.WordAnalyzeRequest
import com.example.myapplication.data.remote.dto.WordAnalysisResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AiApi {

    @POST("api/ai/flashcards/text")
    suspend fun generateFromText(@Body request: AiGenerateTextRequest): AiGenerateResponse

    @Multipart
    @POST("api/ai/flashcards/pdf")
    suspend fun generateFromPdf(
        @Part file: MultipartBody.Part,
        @Part("language") language: RequestBody,
        @Part("maxCards") maxCards: RequestBody
    ): AiGenerateResponse

    @Multipart
    @POST("api/ai/flashcards/docx")
    suspend fun generateFromDocx(
        @Part file: MultipartBody.Part,
        @Part("language") language: RequestBody,
        @Part("maxCards") maxCards: RequestBody
    ): AiGenerateResponse

    @Multipart
    @POST("api/ai/flashcards/extract-vocab")
    suspend fun extractVocabulary(
        @Part file: MultipartBody.Part,
        @Part("targetLanguage") targetLanguage: RequestBody,
        @Part("maxWords") maxWords: RequestBody
    ): AiGenerateResponse

    @POST("api/ai/example")
    suspend fun generateExample(@Body request: AiExampleRequest): AiExampleResponse

    @POST("api/ai/quiz")
    suspend fun generateQuiz(@Body request: AiQuizRequest): AiQuizResponse

    @POST("api/ai/tutor")
    suspend fun tutorChat(@Body request: AiTutorRequest): AiTutorResponse

    @POST("api/ai/adaptive")
    suspend fun getAdaptiveHint(@Body request: AiAdaptiveRequest): AiAdaptiveResponse

    @POST("api/ai/image")
    suspend fun generateImage(@Body request: AiImageRequest): AiImageResponse

    @POST("api/ai/ipa")
    suspend fun generateIpa(@Body request: com.example.myapplication.data.remote.dto.AiIpaRequest): com.example.myapplication.data.remote.dto.AiIpaResponse

    @GET("api/ai/usage/remaining")
    suspend fun getRemainingUsage(): AiUsageResponse

    // ── Word Polysemy Analysis ──
    @POST("api/words/analyze")
    suspend fun analyzeWord(@Body request: WordAnalyzeRequest): WordAnalysisResponse

    // ── Smart Review (Variant Quiz) ──
    @POST("api/ai/smart-review")
    suspend fun smartReview(@Body request: com.example.myapplication.data.remote.dto.SmartReviewRequest): com.example.myapplication.data.remote.dto.SmartReviewResponse
}
