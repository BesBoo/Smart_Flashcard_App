using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFlashcardAPI.Models.DTOs;
using SmartFlashcardAPI.Services;

namespace SmartFlashcardAPI.Controllers;

[Route("api/reviews")]
[Authorize]
public class ReviewsController : BaseController
{
    private readonly ReviewService _reviewService;

    public ReviewsController(ReviewService reviewService) => _reviewService = reviewService;

    /// <summary>POST /api/reviews</summary>
    [HttpPost]
    public async Task<IActionResult> CreateReview([FromBody] CreateReviewRequest request)
    {
        try
        {
            var result = await _reviewService.CreateReviewAsync(GetUserId(), request);
            return StatusCode(201, result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>GET /api/reviews — fetch all review logs for sync</summary>
    [HttpGet]
    public async Task<IActionResult> GetReviews()
    {
        try
        {
            var result = await _reviewService.GetReviewsByUserAsync(GetUserId());
            return Ok(result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }
}
