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

    /// <summary>POST /api/shared-images/upload — upload image file and share it</summary>
    [HttpPost("api/shared-images/upload")]
    [RequestSizeLimit(10 * 1024 * 1024)] // 10MB max
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

            // Save to wwwroot/images/
            var imagesDir = Path.Combine(Directory.GetCurrentDirectory(), "wwwroot", "images");
            Directory.CreateDirectory(imagesDir);

            var ext = Path.GetExtension(file.FileName).ToLower();
            if (string.IsNullOrEmpty(ext)) ext = ".jpg";
            var fileName = $"shared_{Guid.NewGuid():N}{ext}";
            var filePath = Path.Combine(imagesDir, fileName);

            using (var stream = new FileStream(filePath, FileMode.Create))
            {
                await file.CopyToAsync(stream);
            }

            // Build server URL
            var baseUrl = $"{Request.Scheme}://{Request.Host}";
            var imageUrl = $"{baseUrl}/images/{fileName}";

            // Save to shared images DB
            await _sharedImageService.ShareAsync(GetUserId(), keyword, imageUrl);

            return Ok(new { imageUrl });
        }
        catch (Exception ex) { return HandleError(ex); }
    }
}

public record ShareImageRequest(string Keyword, string ImageUrl);
