using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFlashcardAPI.Models.DTOs;
using SmartFlashcardAPI.Services;

namespace SmartFlashcardAPI.Controllers;

[Route("api/decks")]
[Authorize]
public class DecksController : BaseController
{
    private readonly DeckService _deckService;
    private readonly GoogleSheetSyncService _sheetSyncService;

    public DecksController(DeckService deckService, GoogleSheetSyncService sheetSyncService)
    {
        _deckService = deckService;
        _sheetSyncService = sheetSyncService;
    }

    /// <summary>GET /api/decks?cursor={id}&limit={n}</summary>
    [HttpGet]
    public async Task<IActionResult> GetDecks([FromQuery] Guid? cursor, [FromQuery] int limit = 50)
    {
        try
        {
            limit = Math.Clamp(limit, 1, 200);
            var result = await _deckService.GetDecksAsync(GetUserId(), cursor, limit);
            return Ok(result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/decks</summary>
    [HttpPost]
    public async Task<IActionResult> CreateDeck([FromBody] CreateDeckRequest request)
    {
        try
        {
            var result = await _deckService.CreateDeckAsync(GetUserId(), request);
            return StatusCode(201, result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>PUT /api/decks/{id}</summary>
    [HttpPut("{id}")]
    public async Task<IActionResult> UpdateDeck(Guid id, [FromBody] UpdateDeckRequest request)
    {
        try
        {
            var result = await _deckService.UpdateDeckAsync(GetUserId(), id, request);
            return Ok(result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>DELETE /api/decks/{id}</summary>
    [HttpDelete("{id}")]
    public async Task<IActionResult> DeleteDeck(Guid id)
    {
        try
        {
            await _deckService.DeleteDeckAsync(GetUserId(), id);
            return NoContent();
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    // ══════════════════════════════════════════════════════════
    //  GOOGLE SHEET SYNC
    // ══════════════════════════════════════════════════════════

    /// <summary>PUT /api/decks/{id}/sheet — Link a Google Sheet to this deck</summary>
    [HttpPut("{id}/sheet")]
    public async Task<IActionResult> LinkSheet(Guid id, [FromBody] LinkGoogleSheetRequest request)
    {
        try
        {
            var csvUrl = await _sheetSyncService.LinkSheetAsync(GetUserId(), id, request.SheetUrl);
            return Ok(new { sheetUrl = csvUrl, message = "Google Sheet linked successfully" });
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>DELETE /api/decks/{id}/sheet — Unlink Google Sheet from this deck</summary>
    [HttpDelete("{id}/sheet")]
    public async Task<IActionResult> UnlinkSheet(Guid id)
    {
        try
        {
            await _sheetSyncService.UnlinkSheetAsync(GetUserId(), id);
            return Ok(new { message = "Google Sheet unlinked" });
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/decks/{id}/sheet/sync — Sync cards from linked Google Sheet</summary>
    [HttpPost("{id}/sheet/sync")]
    public async Task<IActionResult> SyncSheet(Guid id)
    {
        try
        {
            var (added, skipped, total) = await _sheetSyncService.SyncAsync(GetUserId(), id);
            return Ok(new GoogleSheetSyncResult(added, skipped, total, null));
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>GET /api/decks/violations — Get violation notices for current user's deleted decks.</summary>
    [HttpGet("violations")]
    public async Task<IActionResult> GetViolations()
    {
        try
        {
            var notices = await _deckService.GetViolationNoticesAsync(GetUserId());
            return Ok(notices);
        }
        catch (Exception ex) { return HandleError(ex); }
    }
}
