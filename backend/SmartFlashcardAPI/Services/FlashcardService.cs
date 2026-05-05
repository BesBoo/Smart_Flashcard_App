using Microsoft.EntityFrameworkCore;
using SmartFlashcardAPI.Data;
using SmartFlashcardAPI.Models.DTOs;
using SmartFlashcardAPI.Models.Entities;

namespace SmartFlashcardAPI.Services;

public class FlashcardService
{
    private readonly AppDbContext _db;
    private readonly ILogger<FlashcardService> _logger;

    public FlashcardService(AppDbContext db, ILogger<FlashcardService> logger)
    {
        _db = db;
        _logger = logger;
    }

    public async Task<PagedResponse<FlashcardResponse>> GetCardsAsync(Guid userId, Guid deckId, Guid? cursor, int limit = 50)
    {
        // Check access: owner OR subscriber
        var access = await CheckDeckAccessAsync(userId, deckId);
        if (!access.HasAccess)
            throw new KeyNotFoundException("Deck not found");

        var query = _db.Flashcards
            .Where(f => f.DeckId == deckId && !f.IsDeleted)
            .OrderBy(f => f.CreatedAt)
            .ThenBy(f => f.Id);

        if (cursor.HasValue)
        {
            // Look up the cursor card to get its CreatedAt for proper pagination
            var cursorCard = await _db.Flashcards.FindAsync(cursor.Value);
            if (cursorCard != null)
            {
                var cursorDate = cursorCard.CreatedAt;
                var cursorId = cursorCard.Id;
                query = (IOrderedQueryable<Flashcard>)query.Where(f =>
                    f.CreatedAt > cursorDate ||
                    (f.CreatedAt == cursorDate && f.Id.CompareTo(cursorId) > 0));
            }
        }

        var cards = await query.Take(limit + 1).ToListAsync();
        var hasMore = cards.Count > limit;
        var result = cards.Take(limit).Select(MapToResponse).ToList();

        return new PagedResponse<FlashcardResponse>(result,
            hasMore ? result.Last().Id : null, hasMore);
    }

    public async Task<FlashcardResponse> CreateCardAsync(Guid userId, CreateFlashcardRequest request)
    {
        // Validate SM-2 bounds
        ValidateSM2(request.EaseFactor, request.IntervalDays, request.Repetition);

        // Only owner or edit-subscriber can create cards
        var access = await CheckDeckAccessAsync(userId, request.DeckId);
        if (!access.HasAccess)
            throw new KeyNotFoundException("Deck not found");
        if (access.Permission == "read")
            throw new UnauthorizedAccessException("You don't have edit permission on this deck");

        // Use the deck owner's userId for the card (cards belong to the deck owner)
        var ownerId = access.IsOwner ? userId : access.OwnerId;

        var card = new Flashcard
        {
            Id = request.Id,
            UserId = ownerId,
            DeckId = request.DeckId,
            FrontText = request.FrontText,
            BackText = request.BackText,
            ExampleText = request.ExampleText,
            PronunciationIpa = request.PronunciationIpa,
            ImageUrl = request.ImageUrl,
            AudioUrl = request.AudioUrl,
            Repetition = request.Repetition,
            IntervalDays = request.IntervalDays,
            EaseFactor = request.EaseFactor,
            NextReviewDate = request.NextReviewDate ?? DateTime.UtcNow,
            FailCount = request.FailCount,
            TotalReviews = request.TotalReviews
        };

        _db.Flashcards.Add(card);
        await _db.SaveChangesAsync();

        // Auto-cache IPA for community reuse
        await TryCacheIpaAsync(request.FrontText, request.BackText, request.PronunciationIpa);

        return MapToResponse(card);
    }

    public async Task<FlashcardResponse> UpdateCardAsync(Guid userId, Guid cardId, UpdateFlashcardRequest request)
    {
        // Validate SM-2 bounds
        ValidateSM2(request.EaseFactor, request.IntervalDays, request.Repetition);

        var card = await _db.Flashcards.FirstOrDefaultAsync(f => f.Id == cardId && !f.IsDeleted)
            ?? throw new KeyNotFoundException("Flashcard not found");

        // Check access: owner of the card OR edit-subscriber of the deck
        var access = await CheckDeckAccessAsync(userId, card.DeckId);
        if (!access.HasAccess)
            throw new KeyNotFoundException("Flashcard not found");
        if (access.Permission == "read")
            throw new UnauthorizedAccessException("You don't have edit permission on this deck");

        card.FrontText = request.FrontText;
        card.BackText = request.BackText;
        card.ExampleText = request.ExampleText;
        card.PronunciationIpa = request.PronunciationIpa;
        card.ImageUrl = request.ImageUrl;
        card.AudioUrl = request.AudioUrl;
        card.Repetition = request.Repetition;
        card.IntervalDays = request.IntervalDays;
        card.EaseFactor = request.EaseFactor;
        card.NextReviewDate = request.NextReviewDate;
        card.FailCount = request.FailCount;
        card.TotalReviews = request.TotalReviews;
        card.UpdatedAt = DateTime.UtcNow;

        await _db.SaveChangesAsync();

        // Auto-cache IPA for community reuse
        await TryCacheIpaAsync(request.FrontText, request.BackText, request.PronunciationIpa);
        return MapToResponse(card);
    }

    public async Task DeleteCardAsync(Guid userId, Guid cardId)
    {
        var card = await _db.Flashcards.FirstOrDefaultAsync(f => f.Id == cardId && !f.IsDeleted)
            ?? throw new KeyNotFoundException("Flashcard not found");

        // Check access: owner of the card OR edit-subscriber
        var access = await CheckDeckAccessAsync(userId, card.DeckId);
        if (!access.HasAccess)
            throw new KeyNotFoundException("Flashcard not found");
        if (access.Permission == "read")
            throw new UnauthorizedAccessException("You don't have edit permission on this deck");

        card.IsDeleted = true;
        card.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════

    /// <summary>
    /// Check if user has access to a deck (as owner or subscriber).
    /// Returns (HasAccess, IsOwner, Permission, OwnerId).
    /// </summary>
    private async Task<(bool HasAccess, bool IsOwner, string? Permission, Guid OwnerId)> CheckDeckAccessAsync(Guid userId, Guid deckId)
    {
        var deck = await _db.Decks.FirstOrDefaultAsync(d => d.Id == deckId && !d.IsDeleted);
        if (deck == null) return (false, false, null, Guid.Empty);

        // Owner
        if (deck.UserId == userId)
            return (true, true, null, userId);

        // Subscriber
        var sub = await _db.DeckSubscriptions
            .FirstOrDefaultAsync(s => s.DeckId == deckId && s.SubscriberId == userId && s.IsActive);
        if (sub != null)
            return (true, false, sub.Permission, deck.UserId);

        return (false, false, null, deck.UserId);
    }

    /// <summary>
    /// Server validates SM-2 bounds but NEVER recalculates.
    /// </summary>
    private static void ValidateSM2(double easeFactor, int intervalDays, int repetition)
    {
        if (easeFactor < 1.3)
            throw new ArgumentException("EaseFactor must be >= 1.3");
        if (intervalDays < 1)
            throw new ArgumentException("IntervalDays must be >= 1");
        if (repetition < 0)
            throw new ArgumentException("Repetition must be >= 0");
    }

    private static FlashcardResponse MapToResponse(Flashcard f) => new(
        f.Id, f.DeckId, f.FrontText, f.BackText, f.ExampleText,
        f.PronunciationIpa,
        f.ImageUrl, f.AudioUrl,
        f.Repetition, f.IntervalDays, f.EaseFactor,
        f.NextReviewDate, f.FailCount, f.TotalReviews,
        f.CreatedAt, f.UpdatedAt
    );

    /// <summary>
    /// Auto-cache IPA transcription for community reuse.
    /// Only caches if IPA is non-empty and no cache entry exists yet.
    /// </summary>
    private async Task TryCacheIpaAsync(string frontText, string backText, string? ipa)
    {
        if (string.IsNullOrWhiteSpace(ipa)) return;

        try
        {
            var key = NormalizeIpaKey(frontText, backText);
            var exists = await _db.IpaCaches.AnyAsync(c => c.LookupKey == key);
            if (!exists)
            {
                _db.IpaCaches.Add(new IpaCache
                {
                    LookupKey = key,
                    Ipa = ipa.Trim(),
                    FrontText = frontText.Trim(),
                    BackText = backText.Trim()
                });
                await _db.SaveChangesAsync();
            }
        }
        catch (Exception ex)
        {
            // Non-critical — don't fail the main operation
            _logger.LogWarning("IPA cache write failed: {Msg}", ex.Message);
        }
    }

    internal static string NormalizeIpaKey(string front, string back) =>
        (front ?? "").Trim().ToLowerInvariant() + "|" + (back ?? "").Trim().ToLowerInvariant();
}
