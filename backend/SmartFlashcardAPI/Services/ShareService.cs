using System.Security.Cryptography;
using Microsoft.EntityFrameworkCore;
using SmartFlashcardAPI.Data;
using SmartFlashcardAPI.Models.DTOs;
using SmartFlashcardAPI.Models.Entities;

namespace SmartFlashcardAPI.Services;

public class ShareService
{
    private readonly AppDbContext _db;

    public ShareService(AppDbContext db) => _db = db;

    // ── Characters for share code (no ambiguous: 0/O, 1/l/I) ──
    private const string CodeChars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"; // 30 chars → 30^6 ≈ 729M

    /// <summary>
    /// Create a share link for a deck. Returns the generated share code.
    /// Only the deck owner can call this.
    /// </summary>
    public async Task<ShareInfoResponse> CreateShareAsync(Guid userId, Guid deckId, CreateShareRequest request)
    {
        var deck = await _db.Decks.FirstOrDefaultAsync(d => d.Id == deckId && d.UserId == userId && !d.IsDeleted)
            ?? throw new KeyNotFoundException("Deck not found");

        // Check if there's already an active share
        var existing = await _db.DeckShares.FirstOrDefaultAsync(s => s.DeckId == deckId && s.IsActive);
        if (existing != null)
        {
            // Return existing share info
            return await GetShareInfoInternalAsync(existing);
        }

        // Validate permission
        if (request.DefaultPermission != "read" && request.DefaultPermission != "edit")
            throw new ArgumentException("DefaultPermission must be 'read' or 'edit'");

        // Generate unique share code
        var shareCode = await GenerateUniqueCodeAsync();

        var share = new DeckShare
        {
            DeckId = deckId,
            ShareCode = shareCode,
            IsPublic = true,
            DefaultPermission = request.DefaultPermission,
            ExpiresAt = request.ExpiresAt
        };

        _db.DeckShares.Add(share);

        // Mark deck as shared
        deck.IsShared = true;
        deck.UpdatedAt = DateTime.UtcNow;

        await _db.SaveChangesAsync();

        return await GetShareInfoInternalAsync(share);
    }

    /// <summary>
    /// Get share info for a deck (owner only).
    /// </summary>
    public async Task<ShareInfoResponse> GetShareInfoAsync(Guid userId, Guid deckId)
    {
        var deck = await _db.Decks.FirstOrDefaultAsync(d => d.Id == deckId && d.UserId == userId && !d.IsDeleted)
            ?? throw new KeyNotFoundException("Deck not found");

        var share = await _db.DeckShares.FirstOrDefaultAsync(s => s.DeckId == deckId && s.IsActive)
            ?? throw new KeyNotFoundException("This deck is not shared");

        return await GetShareInfoInternalAsync(share);
    }

    /// <summary>
    /// Deactivate sharing for a deck (owner only). All subscribers lose access.
    /// </summary>
    public async Task StopSharingAsync(Guid userId, Guid deckId)
    {
        var deck = await _db.Decks.FirstOrDefaultAsync(d => d.Id == deckId && d.UserId == userId && !d.IsDeleted)
            ?? throw new KeyNotFoundException("Deck not found");

        var share = await _db.DeckShares.FirstOrDefaultAsync(s => s.DeckId == deckId && s.IsActive);
        if (share != null)
        {
            share.IsActive = false;
        }

        // Deactivate all subscriptions
        var subs = await _db.DeckSubscriptions
            .Where(s => s.DeckId == deckId && s.IsActive)
            .ToListAsync();
        foreach (var sub in subs)
            sub.IsActive = false;

        deck.IsShared = false;
        deck.UpdatedAt = DateTime.UtcNow;

        await _db.SaveChangesAsync();
    }

    /// <summary>
    /// Preview a shared deck by code (before joining). Any authenticated user.
    /// </summary>
    public async Task<DeckPreviewResponse> PreviewByCodeAsync(string code)
    {
        var share = await _db.DeckShares
            .Include(s => s.Deck)
                .ThenInclude(d => d.User)
            .FirstOrDefaultAsync(s => s.ShareCode == code.ToUpper() && s.IsActive)
            ?? throw new KeyNotFoundException("Invalid or expired share code");

        // Check expiry
        if (share.ExpiresAt.HasValue && share.ExpiresAt.Value < DateTime.UtcNow)
            throw new KeyNotFoundException("Share code has expired");

        var cardCount = await _db.Flashcards.CountAsync(f => f.DeckId == share.DeckId && !f.IsDeleted);

        return new DeckPreviewResponse(
            share.DeckId,
            share.Deck.Name,
            share.Deck.Description,
            share.Deck.CoverImageUrl,
            share.Deck.User.DisplayName,
            cardCount,
            share.Deck.Language
        );
    }

    /// <summary>
    /// Join a shared deck using a 6-char code. Creates a subscription.
    /// </summary>
    public async Task<JoinDeckResponse> JoinByCodeAsync(Guid userId, string code)
    {
        var share = await _db.DeckShares
            .Include(s => s.Deck)
                .ThenInclude(d => d.User)
            .FirstOrDefaultAsync(s => s.ShareCode == code.ToUpper() && s.IsActive)
            ?? throw new KeyNotFoundException("Invalid or expired share code");

        // Check expiry
        if (share.ExpiresAt.HasValue && share.ExpiresAt.Value < DateTime.UtcNow)
            throw new KeyNotFoundException("Share code has expired");

        // Can't subscribe to your own deck
        if (share.Deck.UserId == userId)
            throw new InvalidOperationException("CONFLICT:You cannot subscribe to your own deck");

        // Check if already subscribed
        var existingSub = await _db.DeckSubscriptions
            .FirstOrDefaultAsync(s => s.DeckId == share.DeckId && s.SubscriberId == userId);

        if (existingSub != null)
        {
            if (existingSub.IsActive)
                throw new InvalidOperationException("CONFLICT:You are already subscribed to this deck");

            // Re-activate
            existingSub.IsActive = true;
            existingSub.Permission = share.DefaultPermission;
            existingSub.JoinedAt = DateTime.UtcNow;
        }
        else
        {
            var subscription = new DeckSubscription
            {
                DeckId = share.DeckId,
                SubscriberId = userId,
                Permission = share.DefaultPermission
            };
            _db.DeckSubscriptions.Add(subscription);
        }

        await _db.SaveChangesAsync();

        var cardCount = await _db.Flashcards.CountAsync(f => f.DeckId == share.DeckId && !f.IsDeleted);

        return new JoinDeckResponse(
            share.DeckId,
            share.Deck.Name,
            share.Deck.Description,
            share.DefaultPermission,
            share.Deck.User.DisplayName,
            cardCount
        );
    }

    /// <summary>
    /// Subscriber leaves a shared deck voluntarily.
    /// </summary>
    public async Task LeaveSharedDeckAsync(Guid userId, Guid deckId)
    {
        var sub = await _db.DeckSubscriptions
            .FirstOrDefaultAsync(s => s.DeckId == deckId && s.SubscriberId == userId && s.IsActive);

        if (sub != null)
        {
            sub.IsActive = false;
            await _db.SaveChangesAsync();
        }
        // If sub is null or already deactivated (e.g. owner deleted deck), just return OK
    }

    /// <summary>
    /// Owner updates a subscriber's permission.
    /// </summary>
    public async Task UpdateSubscriberPermissionAsync(Guid ownerId, Guid deckId, Guid subscriberId, string permission)
    {
        if (permission != "read" && permission != "edit")
            throw new ArgumentException("Permission must be 'read' or 'edit'");

        // Verify ownership
        var deck = await _db.Decks.FirstOrDefaultAsync(d => d.Id == deckId && d.UserId == ownerId && !d.IsDeleted)
            ?? throw new KeyNotFoundException("Deck not found");

        var sub = await _db.DeckSubscriptions
            .FirstOrDefaultAsync(s => s.DeckId == deckId && s.SubscriberId == subscriberId && s.IsActive)
            ?? throw new KeyNotFoundException("Subscriber not found");

        sub.Permission = permission;
        await _db.SaveChangesAsync();
    }

    /// <summary>
    /// Owner removes (kicks) a subscriber from the deck.
    /// </summary>
    public async Task KickSubscriberAsync(Guid ownerId, Guid deckId, Guid subscriberId)
    {
        // Verify ownership
        var deck = await _db.Decks.FirstOrDefaultAsync(d => d.Id == deckId && d.UserId == ownerId && !d.IsDeleted)
            ?? throw new KeyNotFoundException("Deck not found");

        var sub = await _db.DeckSubscriptions
            .FirstOrDefaultAsync(s => s.DeckId == deckId && s.SubscriberId == subscriberId && s.IsActive)
            ?? throw new KeyNotFoundException("Subscriber not found");

        sub.IsActive = false;
        await _db.SaveChangesAsync();
    }

    /// <summary>
    /// Check if a user has access to a deck (as owner OR subscriber).
    /// Returns (hasAccess, permission) where permission is null for owner, "read"/"edit" for subscriber.
    /// </summary>
    public async Task<(bool HasAccess, string? Permission, Guid OwnerId)> CheckAccessAsync(Guid userId, Guid deckId)
    {
        // Check if owner
        var deck = await _db.Decks.FirstOrDefaultAsync(d => d.Id == deckId && !d.IsDeleted);
        if (deck == null) return (false, null, Guid.Empty);

        if (deck.UserId == userId)
            return (true, null, deck.UserId); // Owner → full access

        // Check subscription
        var sub = await _db.DeckSubscriptions
            .FirstOrDefaultAsync(s => s.DeckId == deckId && s.SubscriberId == userId && s.IsActive);

        if (sub != null)
            return (true, sub.Permission, deck.UserId);

        return (false, null, deck.UserId);
    }

    // ══════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════

    private async Task<string> GenerateUniqueCodeAsync()
    {
        const int maxAttempts = 100;
        for (int attempt = 0; attempt < maxAttempts; attempt++)
        {
            var code = GenerateCode();
            var exists = await _db.DeckShares.AnyAsync(s => s.ShareCode == code);
            if (!exists) return code;
        }
        throw new InvalidOperationException("Failed to generate unique share code after multiple attempts");
    }

    private static string GenerateCode()
    {
        var bytes = new byte[6];
        using var rng = RandomNumberGenerator.Create();
        rng.GetBytes(bytes);
        var code = new char[6];
        for (int i = 0; i < 6; i++)
            code[i] = CodeChars[bytes[i] % CodeChars.Length];
        return new string(code);
    }

    private async Task<ShareInfoResponse> GetShareInfoInternalAsync(DeckShare share)
    {
        var subscribers = await _db.DeckSubscriptions
            .Where(s => s.DeckId == share.DeckId && s.IsActive)
            .Include(s => s.Subscriber)
            .Select(s => new SubscriberInfo(
                s.SubscriberId,
                s.Subscriber.DisplayName,
                s.Subscriber.AvatarUrl,
                s.Permission,
                s.JoinedAt
            ))
            .ToListAsync();

        return new ShareInfoResponse(
            share.ShareCode,
            share.DefaultPermission,
            share.CreatedAt,
            share.ExpiresAt,
            share.IsActive,
            subscribers
        );
    }
}
