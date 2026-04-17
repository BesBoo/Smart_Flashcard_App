using Microsoft.EntityFrameworkCore;
using SmartFlashcardAPI.Data;
using SmartFlashcardAPI.Models.DTOs;
using SmartFlashcardAPI.Models.Entities;

namespace SmartFlashcardAPI.Services;

public class ReviewService
{
    private readonly AppDbContext _db;

    public ReviewService(AppDbContext db) => _db = db;

    public async Task<ReviewResponse> CreateReviewAsync(Guid userId, CreateReviewRequest request)
    {
        // Validate quality: 0 (Học lại), 2 (Khó), 3 (Tốt), 5 (Dễ)
        if (!new[] { 0, 2, 3, 5 }.Contains(request.Quality))
            throw new ArgumentException("Quality must be 0, 2, 3, or 5");

        var review = new ReviewLog
        {
            Id = request.Id,
            UserId = userId,
            FlashcardId = request.FlashcardId,
            Quality = request.Quality,
            ResponseTimeMs = request.ResponseTimeMs,
            ReviewedAt = request.ReviewedAt
        };

        _db.ReviewLogs.Add(review);
        await _db.SaveChangesAsync();

        return new ReviewResponse(review.Id, review.FlashcardId,
            review.Quality, review.ResponseTimeMs, review.ReviewedAt);
    }

    /// <summary>
    /// Get all review logs for a user (for sync on login).
    /// </summary>
    public async Task<List<ReviewResponse>> GetReviewsByUserAsync(Guid userId)
    {
        var reviews = await _db.ReviewLogs
            .Where(r => r.UserId == userId)
            .OrderByDescending(r => r.ReviewedAt)
            .Select(r => new ReviewResponse(r.Id, r.FlashcardId, r.Quality, r.ResponseTimeMs, r.ReviewedAt))
            .ToListAsync();

        return reviews;
    }
}
