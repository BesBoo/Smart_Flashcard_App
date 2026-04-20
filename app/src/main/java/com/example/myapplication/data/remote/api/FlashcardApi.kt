package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.dto.CreateDeckRequest
import com.example.myapplication.data.remote.dto.CreateFlashcardRequest
import com.example.myapplication.data.remote.dto.CreateReviewRequest
import com.example.myapplication.data.remote.dto.DeckResponse
import com.example.myapplication.data.remote.dto.FlashcardResponse
import com.example.myapplication.data.remote.dto.GoogleSheetSyncResponse
import com.example.myapplication.data.remote.dto.LinkGoogleSheetRequest
import com.example.myapplication.data.remote.dto.LinkGoogleSheetResponse
import com.example.myapplication.data.remote.dto.PagedResponse
import com.example.myapplication.data.remote.dto.ReviewResponse
import com.example.myapplication.data.remote.dto.UpdateDeckRequest
import com.example.myapplication.data.remote.dto.UpdateFlashcardRequest
import com.example.myapplication.data.remote.dto.ViolationNotice
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface FlashcardApi {

    // ── Decks ──

    @GET("api/decks")
    suspend fun getDecks(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): PagedResponse<DeckResponse>

    @POST("api/decks")
    suspend fun createDeck(@Body request: CreateDeckRequest): DeckResponse

    @PUT("api/decks/{id}")
    suspend fun updateDeck(
        @Path("id") id: String,
        @Body request: UpdateDeckRequest
    ): DeckResponse

    @DELETE("api/decks/{id}")
    suspend fun deleteDeck(@Path("id") id: String)

    @GET("api/decks/violations")
    suspend fun getViolations(): List<ViolationNotice>

    @POST("api/decks/violations/dismiss")
    suspend fun dismissViolations()

    // ── Flashcards ──

    @GET("api/decks/{deckId}/cards")
    suspend fun getCards(
        @Path("deckId") deckId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): PagedResponse<FlashcardResponse>

    @POST("api/cards")
    suspend fun createCard(@Body request: CreateFlashcardRequest): FlashcardResponse

    @PUT("api/cards/{id}")
    suspend fun updateCard(
        @Path("id") id: String,
        @Body request: UpdateFlashcardRequest
    ): FlashcardResponse

    @DELETE("api/cards/{id}")
    suspend fun deleteCard(@Path("id") id: String)

    // ── Reviews ──

    @POST("api/reviews")
    suspend fun createReview(@Body request: CreateReviewRequest): ReviewResponse

    @GET("api/reviews")
    suspend fun getReviews(): List<ReviewResponse>

    // ── Google Sheet Sync ──

    @PUT("api/decks/{id}/sheet")
    suspend fun linkGoogleSheet(
        @Path("id") deckId: String,
        @Body request: LinkGoogleSheetRequest
    ): LinkGoogleSheetResponse

    @DELETE("api/decks/{id}/sheet")
    suspend fun unlinkGoogleSheet(@Path("id") deckId: String)

    @POST("api/decks/{id}/sheet/sync")
    suspend fun syncGoogleSheet(@Path("id") deckId: String): GoogleSheetSyncResponse
}
