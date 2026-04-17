using Microsoft.EntityFrameworkCore;
using SmartFlashcardAPI.Data;
using SmartFlashcardAPI.Models.DTOs;
using SmartFlashcardAPI.Models.Entities;

namespace SmartFlashcardAPI.Services;

public class DeckService
{
    private readonly AppDbContext _db;

    public DeckService(AppDbContext db) => _db = db;

    public async Task<PagedResponse<DeckResponse>> GetDecksAsync(Guid userId, Guid? cursor, int limit = 50)
    {
        // ── 1. Own decks ──
        var ownQuery = _db.Decks
            .Where(d => d.UserId == userId && !d.IsDeleted)
            .OrderBy(d => d.CreatedAt)
            .ThenBy(d => d.Id);

        if (cursor.HasValue)
            ownQuery = (IOrderedQueryable<Deck>)ownQuery.Where(d => d.Id.CompareTo(cursor.Value) > 0);

        var ownDecks = await ownQuery.Take(limit + 1).ToListAsync();
        var hasMore = ownDecks.Count > limit;
        var ownResult = ownDecks.Take(limit).ToList();

        var deckResponses = new List<DeckResponse>();

        foreach (var d in ownResult)
        {
            var cardCount = await _db.Flashcards.CountAsync(f => f.DeckId == d.Id && !f.IsDeleted);
            var dueCount = await _db.Flashcards.CountAsync(f =>
                f.DeckId == d.Id && !f.IsDeleted && f.NextReviewDate <= DateTime.UtcNow);

            // Get share code if shared
            string? shareCode = null;
            if (d.IsShared)
            {
                var share = await _db.DeckShares.FirstOrDefaultAsync(s => s.DeckId == d.Id && s.IsActive);
                shareCode = share?.ShareCode;
            }

            deckResponses.Add(new DeckResponse(
                d.Id, d.Name, d.Description, d.CoverImageUrl,
                d.Language, cardCount, dueCount, d.CreatedAt, d.UpdatedAt,
                IsOwner: true,
                Permission: null,
                OwnerName: null,
                ShareCode: shareCode,
                IsShared: d.IsShared,
                GoogleSheetUrl: d.GoogleSheetUrl
            ));
        }

        // ── 2. Subscribed decks (appended after own decks) ──
        var subscribedDecks = await _db.DeckSubscriptions
            .Where(s => s.SubscriberId == userId && s.IsActive)
            .Include(s => s.Deck)
                .ThenInclude(d => d.User)
            .Where(s => !s.Deck.IsDeleted)
            .ToListAsync();

        foreach (var sub in subscribedDecks)
        {
            var d = sub.Deck;
            var cardCount = await _db.Flashcards.CountAsync(f => f.DeckId == d.Id && !f.IsDeleted);
            var dueCount = await _db.Flashcards.CountAsync(f =>
                f.DeckId == d.Id && !f.IsDeleted && f.NextReviewDate <= DateTime.UtcNow);

            deckResponses.Add(new DeckResponse(
                d.Id, d.Name, d.Description, d.CoverImageUrl,
                d.Language, cardCount, dueCount, d.CreatedAt, d.UpdatedAt,
                IsOwner: false,
                Permission: sub.Permission,
                OwnerName: d.User.DisplayName,
                ShareCode: null,
                IsShared: true
            ));
        }

        return new PagedResponse<DeckResponse>(deckResponses,
            hasMore ? ownResult.Last().Id : null, hasMore);
    }

    public async Task<DeckResponse> CreateDeckAsync(Guid userId, CreateDeckRequest request)
    {
        var deck = new Deck
        {
            Id = request.Id,
            UserId = userId,
            Name = request.Name,
            Description = request.Description,
            CoverImageUrl = request.CoverImageUrl,
            Language = request.Language
        };

        _db.Decks.Add(deck);
        await _db.SaveChangesAsync();

        return new DeckResponse(deck.Id, deck.Name, deck.Description,
            deck.CoverImageUrl, deck.Language, 0, 0, deck.CreatedAt, deck.UpdatedAt);
    }

    public async Task<DeckResponse> UpdateDeckAsync(Guid userId, Guid deckId, UpdateDeckRequest request)
    {
        var deck = await _db.Decks.FirstOrDefaultAsync(d => d.Id == deckId && d.UserId == userId && !d.IsDeleted)
            ?? throw new KeyNotFoundException("Deck not found");

        deck.Name = request.Name;
        deck.Description = request.Description;
        deck.CoverImageUrl = request.CoverImageUrl;
        if (request.Language != null) deck.Language = request.Language;
        deck.UpdatedAt = DateTime.UtcNow;

        await _db.SaveChangesAsync();

        var cardCount = await _db.Flashcards.CountAsync(f => f.DeckId == deck.Id && !f.IsDeleted);
        var dueCount = await _db.Flashcards.CountAsync(f =>
            f.DeckId == deck.Id && !f.IsDeleted && f.NextReviewDate <= DateTime.UtcNow);

        // Get share code if shared
        string? shareCode = null;
        if (deck.IsShared)
        {
            var share = await _db.DeckShares.FirstOrDefaultAsync(s => s.DeckId == deck.Id && s.IsActive);
            shareCode = share?.ShareCode;
        }

        return new DeckResponse(deck.Id, deck.Name, deck.Description,
            deck.CoverImageUrl, deck.Language, cardCount, dueCount, deck.CreatedAt, deck.UpdatedAt,
            IsOwner: true, ShareCode: shareCode, IsShared: deck.IsShared,
            GoogleSheetUrl: deck.GoogleSheetUrl);
    }

    public async Task DeleteDeckAsync(Guid userId, Guid deckId)
    {
        var deck = await _db.Decks.FirstOrDefaultAsync(d => d.Id == deckId && d.UserId == userId && !d.IsDeleted)
            ?? throw new KeyNotFoundException("Deck not found");

        deck.IsDeleted = true;
        deck.UpdatedAt = DateTime.UtcNow;

        // Soft-delete all cards in this deck
        var cards = await _db.Flashcards.Where(f => f.DeckId == deckId && !f.IsDeleted).ToListAsync();
        foreach (var card in cards)
        {
            card.IsDeleted = true;
            card.UpdatedAt = DateTime.UtcNow;
        }

        // Deactivate sharing
        var shares = await _db.DeckShares.Where(s => s.DeckId == deckId && s.IsActive).ToListAsync();
        foreach (var share in shares) share.IsActive = false;

        var subs = await _db.DeckSubscriptions.Where(s => s.DeckId == deckId && s.IsActive).ToListAsync();
        foreach (var sub in subs) sub.IsActive = false;

        await _db.SaveChangesAsync();
    }

    /// <summary>Get violation notices for decks owned by the user that were deleted due to reports.</summary>
    public async Task<List<ViolationNotice>> GetViolationNoticesAsync(Guid userId)
    {
        var notices = await (
            from r in _db.ContentReports
            join d in _db.Decks on r.TargetId equals d.Id
            where r.TargetType == "Deck"
               && r.Status == "Approved"
               && d.UserId == userId
               && d.IsDeleted
            orderby r.UpdatedAt descending
            select new { r.Id, d.Name, r.Reason, r.UpdatedAt }
        ).ToListAsync();

        return notices.Select(n => new ViolationNotice(n.Id, n.Name, n.Reason, n.UpdatedAt)).ToList();
    }
}
