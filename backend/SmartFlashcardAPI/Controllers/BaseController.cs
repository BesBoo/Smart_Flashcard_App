using Microsoft.AspNetCore.Mvc;
using SmartFlashcardAPI.Security;

namespace SmartFlashcardAPI.Controllers;

/// <summary>
/// Base controller with helper to extract current user ID from JWT.
/// </summary>
[ApiController]
public abstract class BaseController : ControllerBase
{
    protected Guid GetUserId() =>
        User.TryGetUserId() ?? throw new UnauthorizedAccessException("User ID not found in token");

    protected IActionResult HandleError(Exception ex)
    {
        Console.WriteLine($"[ERROR] {ex}");
        return ex switch
        {
            KeyNotFoundException => NotFound(new { error = new { code = "NOT_FOUND", message = ex.Message } }),
            UnauthorizedAccessException => Unauthorized(new { error = new { code = "UNAUTHORIZED", message = ex.Message } }),
            InvalidOperationException e when e.Message.StartsWith("CONFLICT:") =>
                Conflict(new { error = new { code = "CONFLICT", message = e.Message[9..] } }),
            ArgumentException => BadRequest(new { error = new { code = "VALIDATION_ERROR", message = ex.Message } }),
            _ => StatusCode(500, new { error = new { code = "INTERNAL_ERROR", message = "An unexpected error occurred" } })
        };
    }
}
