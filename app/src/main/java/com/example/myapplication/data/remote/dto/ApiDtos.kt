package com.example.myapplication.data.remote.dto

import kotlinx.serialization.Serializable

// ============================================================
// AUTH DTOs
// ============================================================

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class AuthResponse(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val role: String = "user"
)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val token: String,
    val newPassword: String
)

@Serializable
data class GoogleLoginRequest(val idToken: String)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class AdminStatsResponse(
    val totalUsers: Int,
    val activeUsers: Int,
    val totalDecks: Int,
    val totalFlashcards: Int,
    val totalReviews: Int,
    val premiumUsers: Int
)

// ── Admin User Management ───────────────────────────────────

@Serializable
data class AdminUserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val role: String,
    val subscriptionTier: String,
    val isActive: Boolean,
    val deckCount: Int,
    val flashcardCount: Int,
    val createdAt: String
)

@Serializable
data class AdminUserListResponse(
    val users: List<AdminUserDto>,
    val totalCount: Int
)

@Serializable
data class AdminBanUserRequest(val ban: Boolean)

@Serializable
data class AdminChangeRoleRequest(val newRole: String)

// ── Admin AI Usage Logs ─────────────────────────────────────

@Serializable
data class AdminAiLogDto(
    val id: String,
    val userEmail: String,
    val promptType: String,
    val model: String = "",
    val provider: String = "Gemini",
    val tokensUsed: Int,
    val status: String,
    val durationMs: Long = 0,
    val timestamp: String
)

@Serializable
data class AdminAiLogListResponse(
    val logs: List<AdminAiLogDto>,
    val totalCount: Int
)

@Serializable
data class AdminAiStatsResponse(
    val totalCalls: Int = 0,
    val successCalls: Int = 0,
    val failedCalls: Int = 0,
    val rateLimitedCalls: Int = 0,
    val totalTokensUsed: Int = 0,
    val avgDurationMs: Long = 0,
    val todayCalls: Int = 0,
    val callsByType: Map<String, Int> = emptyMap(),
    val callsByProvider: Map<String, Int> = emptyMap()
)

// ── Admin Content Reports ───────────────────────────────────

@Serializable
data class AdminReportDto(
    val id: String,
    val targetType: String,
    val targetId: String,
    val reason: String,
    val reportedByEmail: String,
    val status: String,
    val createdAt: String
)

@Serializable
data class AdminReportListResponse(
    val reports: List<AdminReportDto>,
    val totalCount: Int
)

@Serializable
data class AdminReportActionRequest(val action: String)

@Serializable
data class AdminReportStatsResponse(
    val totalReports: Int = 0,
    val pendingCount: Int = 0,
    val approvedCount: Int = 0,
    val rejectedCount: Int = 0,
    val reportsByType: Map<String, Int> = emptyMap()
)

@Serializable
data class SubmitReportRequest(
    val targetType: String,
    val targetId: String,
    val reason: String
)

// ── Admin Deck Preview ──────────────────────────────────────

@Serializable
data class AdminDeckPreviewResponse(
    val deckId: String,
    val deckName: String,
    val description: String? = null,
    val ownerEmail: String,
    val totalCards: Int,
    val cards: List<AdminCardPreview> = emptyList()
)

@Serializable
data class AdminCardPreview(
    val id: String,
    val frontText: String,
    val backText: String,
    val exampleText: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class ViolationNotice(
    val reportId: String,
    val deckName: String,
    val reason: String,
    val deletedAt: String
)

// ============================================================
// DECK DTOs
// ============================================================

@Serializable
data class CreateDeckRequest(
    val id: String,
    val name: String,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val language: String = "vi"
)

@Serializable
data class UpdateDeckRequest(
    val name: String,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val language: String? = null
)

@Serializable
data class DeckResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val language: String,
    val cardCount: Int,
    val dueCount: Int,
    val createdAt: String,
    val updatedAt: String,
    // ── Sharing fields ──
    val isOwner: Boolean = true,
    val permission: String? = null,       // null (owner) / "read" / "edit"
    val ownerName: String? = null,        // display name of deck owner
    val shareCode: String? = null,        // share code (only for owner)
    val isShared: Boolean = false,
    val googleSheetUrl: String? = null     // linked Google Sheet
)

// ============================================================
// FLASHCARD DTOs
// ============================================================

@Serializable
data class CreateFlashcardRequest(
    val id: String,
    val deckId: String,
    val frontText: String,
    val backText: String,
    val exampleText: String? = null,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val repetition: Int = 0,
    val intervalDays: Int = 1,
    val easeFactor: Double = 2.5,
    val nextReviewDate: String? = null,
    val failCount: Int = 0,
    val totalReviews: Int = 0
)

@Serializable
data class UpdateFlashcardRequest(
    val frontText: String,
    val backText: String,
    val exampleText: String? = null,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val repetition: Int,
    val intervalDays: Int,
    val easeFactor: Double,
    val nextReviewDate: String,
    val failCount: Int,
    val totalReviews: Int
)

@Serializable
data class FlashcardResponse(
    val id: String,
    val deckId: String,
    val frontText: String,
    val backText: String,
    val exampleText: String? = null,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val repetition: Int,
    val intervalDays: Int,
    val easeFactor: Double,
    val nextReviewDate: String,
    val failCount: Int,
    val totalReviews: Int,
    val createdAt: String,
    val updatedAt: String
)

// ============================================================
// REVIEW DTOs
// ============================================================

@Serializable
data class CreateReviewRequest(
    val id: String,
    val flashcardId: String,
    val quality: Int,
    val responseTimeMs: Long? = null,
    val reviewedAt: String
)

@Serializable
data class ReviewResponse(
    val id: String,
    val flashcardId: String,
    val quality: Int,
    val responseTimeMs: Long? = null,
    val reviewedAt: String
)

// ============================================================
// SYNC DTOs
// ============================================================

@Serializable
data class SyncChange(
    val entityType: String,
    val entityId: String,
    val action: String,
    val updatedAt: String,
    val data: String? = null  // JSON string of entity data
)

@Serializable
data class SyncPushRequest(
    val changes: List<SyncChange>
)

@Serializable
data class SyncConflict(
    val entityType: String,
    val entityId: String,
    val resolution: String,
    val serverVersion: String? = null,
    val serverUpdatedAt: String
)

@Serializable
data class SyncPushResponse(
    val accepted: List<String>,
    val conflicts: List<SyncConflict>
)

@Serializable
data class SyncPullResponse(
    val changes: List<SyncChange>,
    val syncTimestamp: String
)

// ============================================================
// AI DTOs
// ============================================================

@Serializable
data class AiGenerateTextRequest(
    val text: String,
    val language: String = "vi",
    val maxCards: Int = 10
)

@Serializable
data class DraftCard(
    val frontText: String,
    val backText: String,
    val exampleText: String? = null
)

@Serializable
data class AiUsageInfo(
    val used: Int,
    val limit: Int
)

@Serializable
data class AiGenerateResponse(
    val drafts: List<DraftCard>,
    val usage: AiUsageInfo
)

@Serializable
data class AiExampleRequest(
    val frontText: String,
    val backText: String,
    val language: String = "vi"
)

@Serializable
data class AiExampleResponse(
    val example: String,
    val usage: AiUsageInfo
)

@Serializable
data class AiImageRequest(
    val frontText: String,
    val backText: String = ""
)

@Serializable
data class AiImageResponse(
    val imageUrl: String? = null,
    val message: String? = null
)

@Serializable
data class AiUsageResponse(
    val remaining: Int = 20,
    val total: Int = 20,
    val used: Int = 0
)

@Serializable
data class QuizCardInput(
    val id: String,
    val frontText: String,
    val backText: String
)

@Serializable
data class AiQuizRequest(
    val cards: List<QuizCardInput>,
    val questionCount: Int = 10,
    val language: String = "vi"
)

@Serializable
data class QuizQuestion(
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val sourceCardId: String
)

@Serializable
data class AiQuizResponse(
    val questions: List<QuizQuestion>
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class AiTutorRequest(
    val sessionId: String,
    val message: String,
    val language: String = "vi",
    val history: List<ChatMessage>? = null
)

@Serializable
data class AiTutorResponse(
    val response: String,
    val usage: AiUsageInfo
)

@Serializable
data class FlashcardInfo(
    val frontText: String,
    val backText: String,
    val exampleText: String? = null,
    val failCount: Int
)

@Serializable
data class RecentReview(
    val quality: Int,
    val responseTimeMs: Long? = null,
    val reviewedAt: String
)

@Serializable
data class AiAdaptiveRequest(
    val flashcard: FlashcardInfo,
    val recentReviews: List<RecentReview>,
    val language: String = "vi"
)

@Serializable
data class AdaptiveHint(
    val simplifiedExplanation: String,
    val additionalExamples: List<String>,
    val splitSuggestion: SplitSuggestion? = null
)

@Serializable
data class SplitSuggestion(
    val suggested: Boolean,
    val cards: List<DraftCard>? = null
)

@Serializable
data class AiAdaptiveResponse(
    val hint: AdaptiveHint,
    val usage: AiUsageInfo
)

// ============================================================
// SHARE DTOs
// ============================================================

@Serializable
data class CreateShareRequest(
    val defaultPermission: String = "read",
    val expiresAt: String? = null
)

@Serializable
data class ShareInfoResponse(
    val shareCode: String,
    val defaultPermission: String,
    val createdAt: String,
    val expiresAt: String? = null,
    val isActive: Boolean,
    val subscribers: List<SubscriberInfo>
)

@Serializable
data class SubscriberInfo(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val permission: String,
    val joinedAt: String
)

@Serializable
data class JoinDeckRequest(val code: String)

@Serializable
data class JoinDeckResponse(
    val deckId: String,
    val deckName: String,
    val description: String? = null,
    val permission: String,
    val ownerName: String,
    val cardCount: Int
)

@Serializable
data class DeckPreviewResponse(
    val deckId: String,
    val deckName: String,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val ownerName: String,
    val cardCount: Int,
    val language: String
)

@Serializable
data class UpdateSubscriberPermissionRequest(val permission: String)

// ============================================================
// PAGINATION & ERROR
// ============================================================

@Serializable
data class PagedResponse<T>(
    val data: List<T>,
    val nextCursor: String? = null,
    val hasMore: Boolean
)

@Serializable
data class ErrorDetail(
    val field: String,
    val message: String
)

@Serializable
data class ErrorBody(
    val code: String,
    val message: String,
    val details: List<ErrorDetail>? = null
)

@Serializable
data class ErrorResponse(
    val error: ErrorBody
)

// ============================================================
// WORD ANALYSIS DTOs (Polysemy)
// ============================================================

@Serializable
data class WordAnalyzeRequest(
    val word: String,
    val definition: String,
    val context: String? = null
)

@Serializable
data class WordAnalysisResponse(
    val lemma: String,
    val detectedPOS: String,
    val mainSense: SenseDto,
    val relatedVariants: List<SenseDto> = emptyList(),
    val otherMeanings: List<SenseDto> = emptyList(),
    val wordVariants: List<VariantDto> = emptyList()
)

@Serializable
data class SenseDto(
    val partOfSpeech: String? = null,
    val definitionEn: String? = null,
    val definitionVi: String? = null,
    val example: String? = null,
    val similarityScore: Int = 0,
    val homonymCluster: String? = null
)

@Serializable
data class VariantDto(
    val variant: String? = null,
    val type: String? = null
)

// ============================================================
// GOOGLE SHEET SYNC DTOs
// ============================================================

@Serializable
data class LinkGoogleSheetRequest(
    val sheetUrl: String
)

@Serializable
data class LinkGoogleSheetResponse(
    val sheetUrl: String? = null,
    val message: String? = null
)

@Serializable
data class GoogleSheetSyncResponse(
    val added: Int = 0,
    val skipped: Int = 0,
    val totalRows: Int = 0,
    val sheetUrl: String? = null
)
