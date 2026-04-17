package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.dto.CreateShareRequest
import com.example.myapplication.data.remote.dto.DeckPreviewResponse
import com.example.myapplication.data.remote.dto.JoinDeckRequest
import com.example.myapplication.data.remote.dto.JoinDeckResponse
import com.example.myapplication.data.remote.dto.ShareInfoResponse
import com.example.myapplication.data.remote.dto.UpdateSubscriberPermissionRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ShareApi {

    // ── Owner endpoints ──

    @POST("api/decks/{deckId}/share")
    suspend fun createShare(
        @Path("deckId") deckId: String,
        @Body request: CreateShareRequest = CreateShareRequest()
    ): ShareInfoResponse

    @GET("api/decks/{deckId}/share")
    suspend fun getShareInfo(@Path("deckId") deckId: String): ShareInfoResponse

    @DELETE("api/decks/{deckId}/share")
    suspend fun stopSharing(@Path("deckId") deckId: String)

    @PUT("api/decks/{deckId}/share/subscribers/{userId}")
    suspend fun updateSubscriberPermission(
        @Path("deckId") deckId: String,
        @Path("userId") userId: String,
        @Body request: UpdateSubscriberPermissionRequest
    )

    @DELETE("api/decks/{deckId}/share/subscribers/{userId}")
    suspend fun kickSubscriber(
        @Path("deckId") deckId: String,
        @Path("userId") userId: String
    )

    // ── User endpoints ──

    @GET("api/share/{code}")
    suspend fun previewDeck(@Path("code") code: String): DeckPreviewResponse

    @POST("api/share/join")
    suspend fun joinDeck(@Body request: JoinDeckRequest): JoinDeckResponse

    @DELETE("api/share/subscriptions/{deckId}")
    suspend fun leaveDeck(@Path("deckId") deckId: String)
}
