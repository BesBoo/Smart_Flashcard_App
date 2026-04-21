using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFlashcardAPI.Services;

namespace SmartFlashcardAPI.Controllers;

[Authorize]
public class SharedImagesController : BaseController
{
    private readonly SharedImageService _sharedImageService;

    public SharedImagesController(SharedImageService sharedImageService)
        => _sharedImageService = sharedImageService;

    /// <summary>GET /api/shared-images?keyword=light&limit=10</summary>
    [HttpGet("api/shared-images")]
    public async Task<IActionResult> Search([FromQuery] string keyword, [FromQuery] int limit = 10)
    {
        try
        {
            limit = Math.Clamp(limit, 1, 20);
            var results = await _sharedImageService.SearchAsync(keyword, limit);
            return Ok(results);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/shared-images — share an image for a keyword</summary>
    [HttpPost("api/shared-images")]
    public async Task<IActionResult> Share([FromBody] ShareImageRequest request)
    {
        try
        {
            await _sharedImageService.ShareAsync(GetUserId(), request.Keyword, request.ImageUrl);
            return Ok(new { message = "Shared" });
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/shared-images/{id}/use — increment usage count</summary>
    [HttpPost("api/shared-images/{id}/use")]
    public async Task<IActionResult> Use(Guid id)
    {
        try
        {
            await _sharedImageService.IncrementUsageAsync(id);
            return Ok(new { message = "Usage incremented" });
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/shared-images/upload — upload image file and store in DB</summary>
    [HttpPost("api/shared-images/upload")]
    [RequestSizeLimit(5 * 1024 * 1024)] // 5MB max
    public async Task<IActionResult> Upload([FromForm] IFormFile file, [FromForm] string keyword)
    {
        try
        {
            if (file == null || file.Length == 0)
                return BadRequest(new { error = "No file provided" });

            if (string.IsNullOrWhiteSpace(keyword))
                return BadRequest(new { error = "Keyword is required" });

            // Validate file type
            var allowedTypes = new[] { "image/jpeg", "image/png", "image/webp", "image/gif" };
            if (!allowedTypes.Contains(file.ContentType.ToLower()))
                return BadRequest(new { error = "Only image files are allowed" });

            // Read file bytes into memory
            byte[] imageData;
            using (var ms = new MemoryStream())
            {
                await file.CopyToAsync(ms);
                imageData = ms.ToArray();
            }

            // Store in DB and get the blob-serve URL
            var baseUrl = $"{Request.Scheme}://{Request.Host}";
            var imageUrl = await _sharedImageService.UploadAsync(
                GetUserId(), keyword, imageData, file.ContentType, baseUrl);

            return Ok(new { imageUrl });
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>GET /api/shared-images/{id}/file — serve image bytes from DB (no auth required)</summary>
    [AllowAnonymous]
    [HttpGet("api/shared-images/{id}/file")]
    [ResponseCache(Duration = 86400)] // Cache 24h — images are immutable
    public async Task<IActionResult> GetFile(Guid id)
    {
        try
        {
            var (data, contentType) = await _sharedImageService.GetImageDataAsync(id);
            if (data == null)
                return NotFound();
            return File(data, contentType ?? "image/jpeg");
        }
        catch (Exception ex) { return HandleError(ex); }
    }
}

public record ShareImageRequest(string Keyword, string ImageUrl);
