using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using SmartFlashcardAPI.Data;
using SmartFlashcardAPI.Models.DTOs;
using SmartFlashcardAPI.Models.Entities;

namespace SmartFlashcardAPI.Services;

public class SyncService
{
    private readonly AppDbContext _db;

    public SyncService(AppDbContext db) => _db = db;

    public async Task<SyncPushResponse> PushAsync(Guid userId, SyncPushRequest request)
    {
        var accepted = new List<Guid>();
        var conflicts = new List<SyncConflict>();

        foreach (var change in request.Changes)
        {
            try
            {
                switch (change.EntityType)
                {
                    case "deck":
                        await ProcessDeckChange(userId, change, accepted, conflicts);
                        break;
                    case "flashcard":
                        await ProcessFlashcardChange(userId, change, accepted, conflicts);
                        break;
                    case "review_log":
                        await ProcessReviewLogChange(userId, change, accepted);
                        break;
                    default:
                        // Unknown entity type — skip
                        break;
                }
            }
            catch
            {
                // If individual change fails, report as conflict
                conflicts.Add(new SyncConflict(change.EntityType, change.EntityId,
                    "error", null, DateTime.UtcNow));
            }
        }

        await _db.SaveChangesAsync();
        return new SyncPushResponse(accepted, conflicts);
    }

    public async Task<SyncPullResponse> PullAsync(Guid userId, DateTime since)
    {
        var changes = new List<SyncChange>();

        // Pull changed decks
        var decks = await _db.Decks
            .Where(d => d.UserId == userId && d.UpdatedAt > since)
            .ToListAsync();

        foreach (var d in decks)
        {
            changes.Add(new SyncChange("deck", d.Id,
                d.IsDeleted ? "DELETE" : "UPDATE",
                d.UpdatedAt, d.IsDeleted ? null : d));
        }

        // Pull changed flashcards
        var cards = await _db.Flashcards
            .Where(f => f.UserId == userId && f.UpdatedAt > since)
            .ToListAsync();

        foreach (var f in cards)
        {
            changes.Add(new SyncChange("flashcard", f.Id,
                f.IsDeleted ? "DELETE" : "UPDATE",
                f.UpdatedAt, f.IsDeleted ? null : f));
        }

        // Pull review logs
        var reviews = await _db.ReviewLogs
            .Where(r => r.UserId == userId && r.ReviewedAt > since)
            .ToListAsync();

        foreach (var r in reviews)
        {
            changes.Add(new SyncChange("review_log", r.Id, "CREATE", r.ReviewedAt, r));
        }

        return new SyncPullResponse(changes, DateTime.UtcNow);
    }

    // ── Private change processors ───────────────────────────

    private async Task ProcessDeckChange(Guid userId, SyncChange change,
        List<Guid> accepted, List<SyncConflict> conflicts)
    {
        var existing = await _db.Decks.FirstOrDefaultAsync(d => d.Id == change.EntityId && d.UserId == userId);

        if (change.Action == "CREATE" && existing == null)
        {
            var data = DeserializeData<DeckSyncData>(change.Data);
            _db.Decks.Add(new Deck
            {
                Id = change.EntityId,
                UserId = userId,
                Name = data?.Name ?? "Untitled",
                Description = data?.Description,
                CoverImageUrl = data?.CoverImageUrl,
                Language = data?.Language ?? "vi",
                CreatedAt = change.UpdatedAt,
                UpdatedAt = change.UpdatedAt
            });
            accepted.Add(change.EntityId);
        }
        else if (existing != null && change.Action == "DELETE")
        {
            existing.IsDeleted = true;
            existing.UpdatedAt = DateTime.UtcNow;
            accepted.Add(change.EntityId);
        }
        else if (existing != null && change.Action == "UPDATE")
        {
            if (existing.UpdatedAt <= change.UpdatedAt)
            {
                var data = DeserializeData<DeckSyncData>(change.Data);
                if (data != null)
                {
                    existing.Name = data.Name ?? existing.Name;
                    existing.Description = data.Description;
                    existing.CoverImageUrl = data.CoverImageUrl;
                    if (data.Language != null) existing.Language = data.Language;
                }
                existing.UpdatedAt = change.UpdatedAt;
                accepted.Add(change.EntityId);
            }
            else
            {
                conflicts.Add(new SyncConflict("deck", change.EntityId,
                    "server_wins", existing, existing.UpdatedAt));
            }
        }
    }

    private async Task ProcessFlashcardChange(Guid userId, SyncChange change,
        List<Guid> accepted, List<SyncConflict> conflicts)
    {
        var existing = await _db.Flashcards.FirstOrDefaultAsync(f => f.Id == change.EntityId && f.UserId == userId);

        if (change.Action == "CREATE" && existing == null)
        {
            var data = DeserializeData<FlashcardSyncData>(change.Data);
            if (data != null)
            {
                _db.Flashcards.Add(new Flashcard
                {
                    Id = change.EntityId,
                    UserId = userId,
                    DeckId = data.DeckId,
                    FrontText = data.FrontText ?? "",
                    BackText = data.BackText ?? "",
                    ExampleText = data.ExampleText,
                    PronunciationIpa = data.PronunciationIpa,
                    ImageUrl = data.ImageUrl,
                    AudioUrl = data.AudioUrl,
                    Repetition = data.Repetition,
                    IntervalDays = Math.Max(1, data.IntervalDays),
                    EaseFactor = Math.Max(1.3, data.EaseFactor),
                    NextReviewDate = data.NextReviewDate,
                    FailCount = data.FailCount,
                    TotalReviews = data.TotalReviews,
                    CreatedAt = change.UpdatedAt,
                    UpdatedAt = change.UpdatedAt
                });
            }
            accepted.Add(change.EntityId);
        }
        else if (existing != null && change.Action == "DELETE")
        {
            existing.IsDeleted = true;
            existing.UpdatedAt = DateTime.UtcNow;
            accepted.Add(change.EntityId);
        }
        else if (existing != null && change.Action == "UPDATE")
        {
            if (existing.UpdatedAt <= change.UpdatedAt)
            {
                var data = DeserializeData<FlashcardSyncData>(change.Data);
                if (data != null)
                {
                    existing.FrontText = data.FrontText ?? existing.FrontText;
                    existing.BackText = data.BackText ?? existing.BackText;
                    existing.ExampleText = data.ExampleText;
                    existing.PronunciationIpa = data.PronunciationIpa;
                    existing.ImageUrl = data.ImageUrl;
                    existing.AudioUrl = data.AudioUrl;
                    existing.Repetition = data.Repetition;
                    existing.IntervalDays = Math.Max(1, data.IntervalDays);
                    existing.EaseFactor = Math.Max(1.3, data.EaseFactor);
                    existing.NextReviewDate = data.NextReviewDate;
                    existing.FailCount = data.FailCount;
                    existing.TotalReviews = data.TotalReviews;
                }
                existing.UpdatedAt = change.UpdatedAt;
                accepted.Add(change.EntityId);
            }
            else
            {
                conflicts.Add(new SyncConflict("flashcard", change.EntityId,
                    "server_wins", existing, existing.UpdatedAt));
            }
        }
    }

    private async Task ProcessReviewLogChange(Guid userId, SyncChange change, List<Guid> accepted)
    {
        // ReviewLogs are append-only — just insert if not exists
        var exists = await _db.ReviewLogs.AnyAsync(r => r.Id == change.EntityId);
        if (!exists)
        {
            var data = DeserializeData<ReviewSyncData>(change.Data);
            if (data != null)
            {
                _db.ReviewLogs.Add(new ReviewLog
                {
                    Id = change.EntityId,
                    UserId = userId,
                    FlashcardId = data.FlashcardId,
                    Quality = data.Quality,
                    ResponseTimeMs = data.ResponseTimeMs,
                    ReviewedAt = data.ReviewedAt
                });
            }
        }
        accepted.Add(change.EntityId);
    }

    // ── Helper ──────────────────────────────────────────────

    private static T? DeserializeData<T>(object? data) where T : class
    {
        if (data == null) return null;
        if (data is JsonElement element)
            return element.Deserialize<T>(new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
        return null;
    }

    // ── Sync data models ────────────────────────────────────

    private record DeckSyncData(string? Name, string? Description, string? CoverImageUrl, string? Language);

    private record FlashcardSyncData(
        Guid DeckId, string? FrontText, string? BackText, string? ExampleText,
        string? PronunciationIpa,
        string? ImageUrl, string? AudioUrl,
        int Repetition, int IntervalDays, double EaseFactor,
        DateTime NextReviewDate, int FailCount, int TotalReviews
    );

    private record ReviewSyncData(Guid FlashcardId, int Quality, long? ResponseTimeMs, DateTime ReviewedAt);
}
