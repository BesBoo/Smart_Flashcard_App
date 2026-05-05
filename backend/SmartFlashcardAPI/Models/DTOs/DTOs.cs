namespace SmartFlashcardAPI.Models.DTOs;

// ============================================================
// AUTH DTOs
// ============================================================

public record RegisterRequest(string Email, string Password, string DisplayName);
public record LoginRequest(string Email, string Password);
public record RefreshRequest(string RefreshToken);
public record ForgotPasswordRequest(string Email);
public record ResetPasswordRequest(string Email, string Token, string NewPassword);
public record GoogleLoginRequest(string IdToken);
public record UpdateEmailRequest(string NewEmail);

public record AuthResponse(Guid UserId, string AccessToken, string RefreshToken, int ExpiresIn, string Role);

public record AdminStatsResponse(
    int TotalUsers,
    int ActiveUsers,
    int TotalDecks,
    int TotalFlashcards,
    int TotalReviews,
    int PremiumUsers
);

// ── Admin User Management ───────────────────────────────────

public record AdminUserDto(
    Guid Id,
    string Email,
    string DisplayName,
    string Role,
    string SubscriptionTier,
    bool IsActive,
    int DeckCount,
    int FlashcardCount,
    DateTime CreatedAt
);

public record AdminUserListResponse(List<AdminUserDto> Users, int TotalCount);

public record AdminBanUserRequest(bool Ban);                  // true = ban, false = unban
public record AdminChangeRoleRequest(string NewRole);         // "admin" or "user"

// ── Admin AI Usage Logs ─────────────────────────────────────

public record AdminAiLogDto(
    Guid Id,
    string UserEmail,
    string PromptType,     // "TutorChat", "GenerateFlashcards", "GenerateExample", etc.
    string Model,          // "gemini-2.5-flash", "llama-3.3-70b", etc.
    string Provider,       // "Gemini", "Groq", "DeepSeek"
    int TokensUsed,
    string Status,         // "Success", "Failed", "RateLimited"
    long DurationMs,
    DateTime Timestamp
);

public record AdminAiLogListResponse(List<AdminAiLogDto> Logs, int TotalCount);

public record AdminAiStatsResponse(
    int TotalCalls,
    int SuccessCalls,
    int FailedCalls,
    int RateLimitedCalls,
    int TotalTokensUsed,
    long AvgDurationMs,
    int TodayCalls,
    Dictionary<string, int> CallsByType,         // { "TutorChat": 10, "GenerateExample": 5 }
    Dictionary<string, int> CallsByProvider       // { "Gemini": 12, "Groq": 3 }
);

// ── Admin Content Reports ───────────────────────────────────

public record AdminReportDto(
    Guid Id,
    string TargetType,     // "Deck", "Flashcard", "User"
    Guid TargetId,
    string Reason,
    string ReportedByEmail,
    string Status,         // "Pending", "Approved", "Rejected"
    DateTime CreatedAt
);

public record AdminReportListResponse(List<AdminReportDto> Reports, int TotalCount);
public record AdminReportActionRequest(string Action);        // "approve" or "reject"

public record AdminReportStatsResponse(
    int TotalReports,
    int PendingCount,
    int ApprovedCount,
    int RejectedCount,
    Dictionary<string, int> ReportsByType          // { "Deck": 3, "Flashcard": 2 }
);

/// <summary>Request body for user-submitted content reports.</summary>
public record SubmitReportRequest(
    string TargetType,     // "Deck", "Flashcard", "User"
    Guid TargetId,
    string Reason
);

/// <summary>Admin preview of a reported deck and its cards.</summary>
public record AdminDeckPreviewResponse(
    Guid DeckId,
    string DeckName,
    string? Description,
    string OwnerEmail,
    int TotalCards,
    List<AdminCardPreview> Cards
);

public record AdminCardPreview(
    Guid Id,
    string FrontText,
    string BackText,
    string? ExampleText,
    string? ImageUrl
);

/// <summary>Notification for deck owners whose decks were deleted.</summary>
public record ViolationNotice(
    Guid ReportId,
    string DeckName,
    string Reason,
    DateTime DeletedAt
);

// ============================================================
// DECK DTOs
// ============================================================

public record CreateDeckRequest(Guid Id, string Name, string? Description, string? CoverImageUrl, string Language = "vi");
public record UpdateDeckRequest(string Name, string? Description, string? CoverImageUrl, string? Language);

public record DeckResponse(
    Guid Id, string Name, string? Description, string? CoverImageUrl,
    string Language, int CardCount, int DueCount,
    DateTime CreatedAt, DateTime UpdatedAt,
    // ── Sharing fields ──
    bool IsOwner = true,
    string? Permission = null,       // null (owner) / "read" / "edit"
    string? OwnerName = null,        // display name of deck owner (for subscribers)
    string? ShareCode = null,        // share code (only visible to owner)
    bool IsShared = false,           // deck has active share
    string? GoogleSheetUrl = null    // linked Google Sheet URL
);

// ============================================================
// FLASHCARD DTOs
// ============================================================

public record CreateFlashcardRequest(
    Guid Id, Guid DeckId,
    string FrontText, string BackText, string? ExampleText,
    string? PronunciationIpa,
    string? ImageUrl, string? AudioUrl,
    int Repetition = 0, int IntervalDays = 1, double EaseFactor = 2.5,
    DateTime? NextReviewDate = null, int FailCount = 0, int TotalReviews = 0
);

public record UpdateFlashcardRequest(
    string FrontText, string BackText, string? ExampleText,
    string? PronunciationIpa,
    string? ImageUrl, string? AudioUrl,
    int Repetition, int IntervalDays, double EaseFactor,
    DateTime NextReviewDate, int FailCount, int TotalReviews
);

public record FlashcardResponse(
    Guid Id, Guid DeckId,
    string FrontText, string BackText, string? ExampleText,
    string? PronunciationIpa,
    string? ImageUrl, string? AudioUrl,
    int Repetition, int IntervalDays, double EaseFactor,
    DateTime NextReviewDate, int FailCount, int TotalReviews,
    DateTime CreatedAt, DateTime UpdatedAt
);

// ============================================================
// REVIEW DTOs
// ============================================================

public record CreateReviewRequest(Guid Id, Guid FlashcardId, int Quality, long? ResponseTimeMs, DateTime ReviewedAt);

public record ReviewResponse(Guid Id, Guid FlashcardId, int Quality, long? ResponseTimeMs, DateTime ReviewedAt);

// ============================================================
// SYNC DTOs
// ============================================================

public record SyncChange(string EntityType, Guid EntityId, string Action, DateTime UpdatedAt, object? Data);

public record SyncPushRequest(List<SyncChange> Changes);

public record SyncConflict(string EntityType, Guid EntityId, string Resolution, object? ServerVersion, DateTime ServerUpdatedAt);

public record SyncPushResponse(List<Guid> Accepted, List<SyncConflict> Conflicts);

public record SyncPullResponse(List<SyncChange> Changes, DateTime SyncTimestamp);

// ============================================================
// AI DTOs
// ============================================================

public record AiGenerateTextRequest(string Text, string Language = "vi", int MaxCards = 10);

public record DraftCard(string FrontText, string BackText, string? ExampleText);

public record AiGenerateResponse(List<DraftCard> Drafts, AiUsageInfo Usage);

public record AiExampleRequest(string FrontText, string BackText, string Language = "vi");
public record AiExampleResponse(string Example, AiUsageInfo Usage);

public record AiIpaRequest(string FrontText, string BackText);
public record AiIpaResponse(string Ipa, bool FromCache);

public record AiImageRequest(string FrontText, string BackText = "");

public record AiQuizRequest(List<QuizCardInput> Cards, int QuestionCount = 10, string Language = "vi");
public record QuizCardInput(Guid Id, string FrontText, string BackText);
public record QuizQuestion(string QuestionText, List<string> Options, int CorrectIndex, Guid SourceCardId);
public record AiQuizResponse(List<QuizQuestion> Questions);

public record AiTutorRequest(Guid SessionId, string Message, string Language = "vi", List<ChatMessage>? History = null);
public record ChatMessage(string Role, string Content);

public record AiAdaptiveRequest(FlashcardInfo Flashcard, List<RecentReview> RecentReviews, string Language = "vi");
public record FlashcardInfo(string FrontText, string BackText, string? ExampleText, int FailCount);
public record RecentReview(int Quality, long? ResponseTimeMs, DateTime ReviewedAt);
public record AdaptiveHint(string SimplifiedExplanation, List<string> AdditionalExamples, SplitSuggestion? SplitSuggestion);
public record SplitSuggestion(bool Suggested, List<DraftCard>? Cards);
public record AiAdaptiveResponse(AdaptiveHint Hint, AiUsageInfo Usage);

public record AiUsageInfo(int Used, int Limit);

// ── Smart Review (Variant Quiz) DTOs ────────────────────────
public record SmartReviewRequest(List<SmartReviewWord> Words, int QuestionCount = 10, string Language = "en");
public record SmartReviewWord(string Word, string PartOfSpeech = "", string Definition = "", Guid? SourceCardId = null);
public record SmartReviewQuestion(string BaseWord, string Sentence, List<string> Options, int CorrectIndex, Guid? SourceCardId);
public record SmartReviewResponse(List<SmartReviewQuestion> Questions, AiUsageInfo Usage);

// ============================================================
// SHARE DTOs
// ============================================================

public record CreateShareRequest(string DefaultPermission = "read", DateTime? ExpiresAt = null);

public record ShareInfoResponse(
    string ShareCode,
    string DefaultPermission,
    DateTime CreatedAt,
    DateTime? ExpiresAt,
    bool IsActive,
    List<SubscriberInfo> Subscribers
);

public record SubscriberInfo(Guid UserId, string DisplayName, string? AvatarUrl, string Permission, DateTime JoinedAt);

public record JoinDeckRequest(string Code);

public record JoinDeckResponse(Guid DeckId, string DeckName, string? Description, string Permission, string OwnerName, int CardCount);

public record DeckPreviewResponse(Guid DeckId, string DeckName, string? Description, string? CoverImageUrl, string OwnerName, int CardCount, string Language);

public record UpdateSubscriberPermissionRequest(string Permission);  // "read" or "edit"

// ============================================================
// GOOGLE SHEET SYNC DTOs
// ============================================================

public record LinkGoogleSheetRequest(string SheetUrl);

public record GoogleSheetSyncResult(int Added, int Skipped, int TotalRows, string? SheetUrl);

// ============================================================
// PAGINATION
// ============================================================

public record PagedResponse<T>(List<T> Data, Guid? NextCursor, bool HasMore);

// ============================================================
// ERROR
// ============================================================

public record ErrorDetail(string Field, string Message);
public record ErrorBody(string Code, string Message, List<ErrorDetail>? Details = null);
public record ErrorResponse(ErrorBody Error);
