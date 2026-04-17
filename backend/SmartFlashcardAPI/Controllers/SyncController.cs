using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFlashcardAPI.Models.DTOs;
using SmartFlashcardAPI.Services;

namespace SmartFlashcardAPI.Controllers;

[Route("api/sync")]
[Authorize]
public class SyncController : BaseController
{
    private readonly SyncService _syncService;

    public SyncController(SyncService syncService) => _syncService = syncService;

    /// <summary>POST /api/sync/push</summary>
    [HttpPost("push")]
    public async Task<IActionResult> Push([FromBody] SyncPushRequest request)
    {
        try
        {
            var result = await _syncService.PushAsync(GetUserId(), request);
            return Ok(result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>GET /api/sync/pull?since={timestamp}</summary>
    [HttpGet("pull")]
    public async Task<IActionResult> Pull([FromQuery] DateTime since)
    {
        try
        {
            var result = await _syncService.PullAsync(GetUserId(), since);
            return Ok(result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }
}
