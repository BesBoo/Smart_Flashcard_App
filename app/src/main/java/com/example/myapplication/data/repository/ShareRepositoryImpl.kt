package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.api.ShareApi
import com.example.myapplication.data.remote.dto.CreateShareRequest
import com.example.myapplication.data.remote.dto.DeckPreviewResponse
import com.example.myapplication.data.remote.dto.JoinDeckRequest
import com.example.myapplication.data.remote.dto.JoinDeckResponse
import com.example.myapplication.data.remote.dto.ShareInfoResponse
import com.example.myapplication.data.remote.dto.UpdateSubscriberPermissionRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareRepositoryImpl @Inject constructor(
    private val shareApi: ShareApi
) {
    // ── Owner actions ──

    suspend fun createShare(deckId: String, permission: String = "read"): ShareInfoResponse =
        shareApi.createShare(deckId, CreateShareRequest(defaultPermission = permission))

    suspend fun getShareInfo(deckId: String): ShareInfoResponse =
        shareApi.getShareInfo(deckId)

    suspend fun stopSharing(deckId: String) =
        shareApi.stopSharing(deckId)

    suspend fun updateSubscriberPermission(deckId: String, userId: String, permission: String) =
        shareApi.updateSubscriberPermission(deckId, userId, UpdateSubscriberPermissionRequest(permission))

    suspend fun kickSubscriber(deckId: String, userId: String) =
        shareApi.kickSubscriber(deckId, userId)

    // ── User actions ──

    suspend fun previewDeck(code: String): DeckPreviewResponse =
        shareApi.previewDeck(code)

    suspend fun joinDeck(code: String): JoinDeckResponse =
        shareApi.joinDeck(JoinDeckRequest(code))

    suspend fun leaveDeck(deckId: String) =
        shareApi.leaveDeck(deckId)
}
