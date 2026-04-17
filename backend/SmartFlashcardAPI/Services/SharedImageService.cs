using Microsoft.EntityFrameworkCore;
using SmartFlashcardAPI.Data;
using SmartFlashcardAPI.Models.Entities;

namespace SmartFlashcardAPI.Services;

/// <summary>
/// Community image library — allows users to share and discover images by keyword.
/// </summary>
public class SharedImageService
{
    private readonly AppDbContext _db;

    public SharedImageService(AppDbContext db) => _db = db;

    /// <summary>
    /// Search for shared images by keyword (case-insensitive).
    /// Returns top images ordered by usage count.
    /// </summary>
    public async Task<List<SharedImageResponse>> SearchAsync(string keyword, int limit = 10)
    {
        var normalized = keyword.Trim().ToLowerInvariant();
        if (string.IsNullOrEmpty(normalized)) return new();

        return await _db.SharedImages
            .Where(s => !s.IsDeleted && s.Keyword == normalized)
            .OrderByDescending(s => s.UsageCount)
            .ThenByDescending(s => s.CreatedAt)
            .Take(limit)
            .Select(s => new SharedImageResponse(s.Id, s.ImageUrl, s.UsageCount))
            .ToListAsync();
    }

    /// <summary>
    /// Share an image for a keyword. Deduplicates by (keyword + imageUrl).
    /// </summary>
    public async Task ShareAsync(Guid userId, string keyword, string imageUrl)
    {
        var normalized = keyword.Trim().ToLowerInvariant();
        if (string.IsNullOrEmpty(normalized) || string.IsNullOrEmpty(imageUrl)) return;

        // Only share server-hosted images (not local file paths)
        if (!imageUrl.StartsWith("http")) return;

        // Check for duplicate
        var exists = await _db.SharedImages.AnyAsync(s =>
            !s.IsDeleted && s.Keyword == normalized && s.ImageUrl == imageUrl);
        if (exists) return;

        _db.SharedImages.Add(new SharedImage
        {
            UserId = userId,
            Keyword = normalized,
            ImageUrl = imageUrl
        });
        await _db.SaveChangesAsync();
    }

    /// <summary>
    /// Increment usage count when a user picks a shared image.
    /// </summary>
    public async Task IncrementUsageAsync(Guid imageId)
    {
        var image = await _db.SharedImages.FindAsync(imageId);
        if (image != null)
        {
            image.UsageCount++;
            await _db.SaveChangesAsync();
        }
    }
}

public record SharedImageResponse(Guid Id, string ImageUrl, int UsageCount);
