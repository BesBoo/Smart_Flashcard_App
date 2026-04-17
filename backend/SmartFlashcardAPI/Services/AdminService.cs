using Microsoft.EntityFrameworkCore;
using SmartFlashcardAPI.Data;
using SmartFlashcardAPI.Models.DTOs;
using SmartFlashcardAPI.Models.Entities;

namespace SmartFlashcardAPI.Services;

public class AdminService
{
    private readonly AppDbContext _db;

    public AdminService(AppDbContext db) => _db = db;

    // ── Dashboard Stats ─────────────────────────────────────
    public async Task<AdminStatsResponse> GetGlobalStatsAsync()
    {
        var totalUsers = await _db.Users.CountAsync();
        var activeUsers = await _db.Users.CountAsync(u => u.IsActive);
        var premiumUsers = await _db.Users.CountAsync(u => u.SubscriptionTier == "Premium");
        var totalDecks = await _db.Decks.CountAsync(d => !d.IsDeleted);
        var totalFlashcards = await _db.Flashcards.CountAsync(f => !f.IsDeleted);
        var totalReviews = await _db.ReviewLogs.CountAsync();

        return new AdminStatsResponse(
            totalUsers,
            activeUsers,
            totalDecks,
            totalFlashcards,
            totalReviews,
            premiumUsers
        );
    }

    // ── User Management ─────────────────────────────────────
    public async Task<AdminUserListResponse> GetUsersAsync(string? search, int page, int pageSize)
    {
        var query = _db.Users.AsQueryable();

        if (!string.IsNullOrWhiteSpace(search))
        {
            var s = search.ToLower();
            query = query.Where(u => u.Email.ToLower().Contains(s) || u.DisplayName.ToLower().Contains(s));
        }

        var totalCount = await query.CountAsync();

        var users = await query
            .OrderByDescending(u => u.CreatedAt)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .Select(u => new AdminUserDto(
                u.Id,
                u.Email,
                u.DisplayName,
                u.Role,
                u.SubscriptionTier,
                u.IsActive,
                u.Decks.Count(d => !d.IsDeleted),
                u.Decks.SelectMany(d => d.Flashcards).Count(f => !f.IsDeleted),
                u.CreatedAt
            ))
            .ToListAsync();

        return new AdminUserListResponse(users, totalCount);
    }

    public async Task BanUserAsync(Guid userId, bool ban)
    {
        var user = await _db.Users.FindAsync(userId)
            ?? throw new KeyNotFoundException($"User {userId} not found");
        user.IsActive = !ban;
        user.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();
    }

    public async Task ChangeRoleAsync(Guid userId, string newRole)
    {
        if (newRole != "admin" && newRole != "user")
            throw new ArgumentException("Role must be 'admin' or 'user'");

        var user = await _db.Users.FindAsync(userId)
            ?? throw new KeyNotFoundException($"User {userId} not found");
        user.Role = newRole;
        user.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();
    }

    // ── AI Usage Logs ───────────────────────────────────────
    public async Task<AdminAiLogListResponse> GetAiLogsAsync(int page, int pageSize, string? statusFilter = null, string? typeFilter = null)
    {
        var query = _db.AiUsageLogs
            .OrderByDescending(l => l.CreatedAt)
            .AsQueryable();

        if (!string.IsNullOrEmpty(statusFilter))
            query = query.Where(l => l.Status == statusFilter);
        if (!string.IsNullOrEmpty(typeFilter))
            query = query.Where(l => l.PromptType == typeFilter);

        var totalCount = await query.CountAsync();

        var logs = await query
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .Select(l => new AdminAiLogDto(
                l.Id,
                l.User != null ? l.User.Email : "System",
                l.PromptType,
                l.Model,
                l.Provider,
                l.TokensUsed,
                l.Status,
                l.DurationMs,
                l.CreatedAt
            ))
            .ToListAsync();

        return new AdminAiLogListResponse(logs, totalCount);
    }

    public async Task<AdminAiStatsResponse> GetAiStatsAsync()
    {
        var totalCalls = await _db.AiUsageLogs.CountAsync();
        var successCalls = await _db.AiUsageLogs.CountAsync(l => l.Status == "Success");
        var failedCalls = await _db.AiUsageLogs.CountAsync(l => l.Status == "Failed");
        var rateLimitedCalls = await _db.AiUsageLogs.CountAsync(l => l.Status == "RateLimited");
        var totalTokens = await _db.AiUsageLogs.SumAsync(l => l.TokensUsed);
        var avgDuration = totalCalls > 0
            ? (long)await _db.AiUsageLogs.Where(l => l.Status == "Success").AverageAsync(l => (double)l.DurationMs)
            : 0;

        var today = DateTime.UtcNow.Date;
        var todayCalls = await _db.AiUsageLogs.CountAsync(l => l.CreatedAt >= today);

        var callsByType = await _db.AiUsageLogs
            .GroupBy(l => l.PromptType)
            .Select(g => new { Key = g.Key, Count = g.Count() })
            .ToDictionaryAsync(x => x.Key, x => x.Count);

        var callsByProvider = await _db.AiUsageLogs
            .GroupBy(l => l.Provider)
            .Select(g => new { Key = g.Key, Count = g.Count() })
            .ToDictionaryAsync(x => x.Key, x => x.Count);

        return new AdminAiStatsResponse(
            totalCalls, successCalls, failedCalls, rateLimitedCalls,
            totalTokens, avgDuration, todayCalls,
            callsByType, callsByProvider
        );
    }

    // ── Content Reports ─────────────────────────────────────
    public async Task<AdminReportListResponse> GetReportsAsync(int page, int pageSize)
    {
        var query = _db.ContentReports
            .OrderByDescending(r => r.CreatedAt);

        var totalCount = await query.CountAsync();

        var reports = await query
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .Select(r => new AdminReportDto(
                r.Id,
                r.TargetType,
                r.TargetId,
                r.Reason,
                r.ReportedByUser.Email,
                r.Status,
                r.CreatedAt
            ))
            .ToListAsync();

        return new AdminReportListResponse(reports, totalCount);
    }

    public async Task<AdminReportStatsResponse> GetReportStatsAsync()
    {
        var total = await _db.ContentReports.CountAsync();
        var pending = await _db.ContentReports.CountAsync(r => r.Status == "Pending");
        var approved = await _db.ContentReports.CountAsync(r => r.Status == "Approved");
        var rejected = await _db.ContentReports.CountAsync(r => r.Status == "Rejected");

        var byType = await _db.ContentReports
            .GroupBy(r => r.TargetType)
            .Select(g => new { Key = g.Key, Count = g.Count() })
            .ToDictionaryAsync(x => x.Key, x => x.Count);

        return new AdminReportStatsResponse(total, pending, approved, rejected, byType);
    }

    public async Task HandleReportAsync(Guid reportId, string action)
    {
        var report = await _db.ContentReports.FindAsync(reportId)
            ?? throw new KeyNotFoundException($"Report {reportId} not found");

        report.Status = action.ToLower() switch
        {
            "approve" => "Approved",
            "reject"  => "Rejected",
            _ => throw new ArgumentException("Action must be 'approve' or 'reject'")
        };
        report.UpdatedAt = DateTime.UtcNow;

        // If approved → soft-delete the reported content
        if (report.Status == "Approved")
        {
            switch (report.TargetType)
            {
                case "Deck":
                    var deck = await _db.Decks.FindAsync(report.TargetId);
                    if (deck != null)
                    {
                        deck.IsDeleted = true;
                        deck.UpdatedAt = DateTime.UtcNow;
                        // Also soft-delete all cards in this deck
                        var cards = await _db.Flashcards
                            .Where(f => f.DeckId == deck.Id && !f.IsDeleted)
                            .ToListAsync();
                        foreach (var card in cards)
                        {
                            card.IsDeleted = true;
                            card.UpdatedAt = DateTime.UtcNow;
                        }
                    }
                    break;

                case "Flashcard":
                    var flashcard = await _db.Flashcards.FindAsync(report.TargetId);
                    if (flashcard != null)
                    {
                        flashcard.IsDeleted = true;
                        flashcard.UpdatedAt = DateTime.UtcNow;
                    }
                    break;
            }
        }

        await _db.SaveChangesAsync();
    }

    /// <summary>Submit a new content report (any authenticated user).</summary>
    public async Task SubmitReportAsync(Guid userId, SubmitReportRequest request)
    {
        _db.ContentReports.Add(new ContentReport
        {
            ReportedByUserId = userId,
            TargetType = request.TargetType,
            TargetId = request.TargetId,
            Reason = request.Reason
        });
        await _db.SaveChangesAsync();
    }

    /// <summary>Admin preview: fetch deck info + all cards (bypasses ownership check).</summary>
    public async Task<AdminDeckPreviewResponse> GetDeckPreviewAsync(Guid deckId)
    {
        var deck = await _db.Decks
            .Include(d => d.User)
            .FirstOrDefaultAsync(d => d.Id == deckId && !d.IsDeleted)
            ?? throw new KeyNotFoundException($"Deck {deckId} not found");

        var cards = await _db.Flashcards
            .Where(f => f.DeckId == deckId && !f.IsDeleted)
            .OrderBy(f => f.CreatedAt)
            .Select(f => new AdminCardPreview(
                f.Id, f.FrontText, f.BackText, f.ExampleText, f.ImageUrl
            ))
            .ToListAsync();

        return new AdminDeckPreviewResponse(
            deck.Id,
            deck.Name,
            deck.Description,
            deck.User.Email,
            cards.Count,
            cards
        );
    }
}
