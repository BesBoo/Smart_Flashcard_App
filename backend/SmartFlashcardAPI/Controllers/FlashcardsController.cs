using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFlashcardAPI.Models.DTOs;
using SmartFlashcardAPI.Services;

namespace SmartFlashcardAPI.Controllers;

[Authorize]
public class FlashcardsController : BaseController
{
    private readonly FlashcardService _cardService;

    public FlashcardsController(FlashcardService cardService) => _cardService = cardService;

    /// <summary>GET /api/decks/{deckId}/cards?cursor={id}&limit={n}</summary>
    [HttpGet("api/decks/{deckId}/cards")]
    public async Task<IActionResult> GetCards(Guid deckId, [FromQuery] Guid? cursor, [FromQuery] int limit = 50)
    {
        try
        {
            limit = Math.Clamp(limit, 1, 200);
            var result = await _cardService.GetCardsAsync(GetUserId(), deckId, cursor, limit);
            return Ok(result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/cards</summary>
    [HttpPost("api/cards")]
    public async Task<IActionResult> CreateCard([FromBody] CreateFlashcardRequest request)
    {
        try
        {
            var result = await _cardService.CreateCardAsync(GetUserId(), request);
            return StatusCode(201, result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>PUT /api/cards/{id}</summary>
    [HttpPut("api/cards/{id}")]
    public async Task<IActionResult> UpdateCard(Guid id, [FromBody] UpdateFlashcardRequest request)
    {
        try
        {
            var result = await _cardService.UpdateCardAsync(GetUserId(), id, request);
            return Ok(result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>DELETE /api/cards/{id}</summary>
    [HttpDelete("api/cards/{id}")]
    public async Task<IActionResult> DeleteCard(Guid id)
    {
        try
        {
            await _cardService.DeleteCardAsync(GetUserId(), id);
            return NoContent();
        }
        catch (Exception ex) { return HandleError(ex); }
    }
}
