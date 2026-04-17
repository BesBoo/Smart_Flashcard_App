using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFlashcardAPI.Models.DTOs;
using SmartFlashcardAPI.Services;

namespace SmartFlashcardAPI.Controllers;

/// <summary>
/// Handles deck sharing: creating share codes, joining shared decks,
/// managing subscribers, and permission control.
/// </summary>
[Authorize]
public class ShareController : BaseController
{
    private readonly ShareService _shareService;

    public ShareController(ShareService shareService) => _shareService = shareService;

    // ══════════════════════════════════════════════════════════
    //  OWNER ENDPOINTS — manage sharing
    // ══════════════════════════════════════════════════════════

    /// <summary>POST /api/decks/{deckId}/share — Create share code for a deck</summary>
    [HttpPost("api/decks/{deckId}/share")]
    public async Task<IActionResult> CreateShare(Guid deckId, [FromBody] CreateShareRequest? request)
    {
        try
        {
            var result = await _shareService.CreateShareAsync(
                GetUserId(), deckId, request ?? new CreateShareRequest());
            return StatusCode(201, result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>GET /api/decks/{deckId}/share — Get share info (owner only)</summary>
    [HttpGet("api/decks/{deckId}/share")]
    public async Task<IActionResult> GetShareInfo(Guid deckId)
    {
        try
        {
            var result = await _shareService.GetShareInfoAsync(GetUserId(), deckId);
            return Ok(result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>DELETE /api/decks/{deckId}/share — Stop sharing a deck</summary>
    [HttpDelete("api/decks/{deckId}/share")]
    public async Task<IActionResult> StopSharing(Guid deckId)
    {
        try
        {
            await _shareService.StopSharingAsync(GetUserId(), deckId);
            return NoContent();
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>PUT /api/decks/{deckId}/share/subscribers/{userId} — Update subscriber permission</summary>
    [HttpPut("api/decks/{deckId}/share/subscribers/{userId}")]
    public async Task<IActionResult> UpdateSubscriberPermission(
        Guid deckId, Guid userId, [FromBody] UpdateSubscriberPermissionRequest request)
    {
        try
        {
            await _shareService.UpdateSubscriberPermissionAsync(GetUserId(), deckId, userId, request.Permission);
            return Ok(new { message = "Permission updated" });
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>DELETE /api/decks/{deckId}/share/subscribers/{userId} — Kick a subscriber</summary>
    [HttpDelete("api/decks/{deckId}/share/subscribers/{userId}")]
    public async Task<IActionResult> KickSubscriber(Guid deckId, Guid userId)
    {
        try
        {
            await _shareService.KickSubscriberAsync(GetUserId(), deckId, userId);
            return NoContent();
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    // ══════════════════════════════════════════════════════════
    //  USER ENDPOINTS — join / leave shared decks
    // ══════════════════════════════════════════════════════════

    /// <summary>GET /api/share/{code} — Preview a shared deck before joining</summary>
    [HttpGet("api/share/{code}")]
    public async Task<IActionResult> PreviewDeck(string code)
    {
        try
        {
            var result = await _shareService.PreviewByCodeAsync(code);
            return Ok(result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/share/join — Join a shared deck using a 6-char code</summary>
    [HttpPost("api/share/join")]
    public async Task<IActionResult> JoinDeck([FromBody] JoinDeckRequest request)
    {
        try
        {
            var result = await _shareService.JoinByCodeAsync(GetUserId(), request.Code);
            return Ok(result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>DELETE /api/share/subscriptions/{deckId} — Leave a shared deck</summary>
    [HttpDelete("api/share/subscriptions/{deckId}")]
    public async Task<IActionResult> LeaveDeck(Guid deckId)
    {
        try
        {
            await _shareService.LeaveSharedDeckAsync(GetUserId(), deckId);
            return NoContent();
        }
        catch (Exception ex) { return HandleError(ex); }
    }
}
