using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFlashcardAPI.Models.DTOs;
using SmartFlashcardAPI.Services;
using System.Security.Claims;

namespace SmartFlashcardAPI.Controllers;

/// <summary>Admin-only endpoints (JWT claim <c>role</c>=admin; see JwtBearer RoleClaimType).</summary>
[Route("api/admin")]
[Authorize(Roles = "admin")]
public class AdminController : BaseController
{
    private readonly AdminService _adminService;

    public AdminController(AdminService adminService) => _adminService = adminService;

    // ── Dashboard Stats ─────────────────────────────────────

    /// <summary>GET /api/admin/stats — aggregate counts for dashboard.</summary>
    [HttpGet("stats")]
    public async Task<IActionResult> GetStats()
    {
        try
        {
            var stats = await _adminService.GetGlobalStatsAsync();
            return Ok(stats);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    // ── User Management ─────────────────────────────────────

    /// <summary>GET /api/admin/users?search=&amp;page=1&amp;pageSize=20</summary>
    [HttpGet("users")]
    public async Task<IActionResult> GetUsers(
        [FromQuery] string? search = null,
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 20)
    {
        try
        {
            var result = await _adminService.GetUsersAsync(search, page, pageSize);
            return Ok(result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/admin/users/{id}/ban — ban or unban a user.</summary>
    [HttpPost("users/{id}/ban")]
    public async Task<IActionResult> BanUser(Guid id, [FromBody] AdminBanUserRequest request)
    {
        try
        {
            await _adminService.BanUserAsync(id, request.Ban);
            return Ok(new { message = request.Ban ? "User banned" : "User unbanned" });
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/admin/users/{id}/role — change user role.</summary>
    [HttpPost("users/{id}/role")]
    public async Task<IActionResult> ChangeRole(Guid id, [FromBody] AdminChangeRoleRequest request)
    {
        try
        {
            await _adminService.ChangeRoleAsync(id, request.NewRole);
            return Ok(new { message = $"Role changed to {request.NewRole}" });
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    // ── AI Usage Logs ───────────────────────────────────────

    /// <summary>GET /api/admin/ai-logs?page=1&amp;pageSize=20&amp;status=&amp;type=</summary>
    [HttpGet("ai-logs")]
    public async Task<IActionResult> GetAiLogs(
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 20,
        [FromQuery] string? status = null,
        [FromQuery] string? type = null)
    {
        try
        {
            var result = await _adminService.GetAiLogsAsync(page, pageSize, status, type);
            return Ok(result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>GET /api/admin/ai-stats — AI usage dashboard stats.</summary>
    [HttpGet("ai-stats")]
    public async Task<IActionResult> GetAiStats()
    {
        try
        {
            var stats = await _adminService.GetAiStatsAsync();
            return Ok(stats);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    // ── Content Reports ─────────────────────────────────────

    /// <summary>GET /api/admin/reports?page=1&amp;pageSize=20</summary>
    [HttpGet("reports")]
    public async Task<IActionResult> GetReports(
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 20)
    {
        try
        {
            var result = await _adminService.GetReportsAsync(page, pageSize);
            return Ok(result);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>GET /api/admin/report-stats — Report overview stats.</summary>
    [HttpGet("report-stats")]
    public async Task<IActionResult> GetReportStats()
    {
        try
        {
            var stats = await _adminService.GetReportStatsAsync();
            return Ok(stats);
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/admin/reports/{id}/action — approve or reject a report.</summary>
    [HttpPost("reports/{id}/action")]
    public async Task<IActionResult> HandleReport(Guid id, [FromBody] AdminReportActionRequest request)
    {
        try
        {
            await _adminService.HandleReportAsync(id, request.Action);
            return Ok(new { message = $"Report {request.Action}d" });
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>GET /api/admin/decks/{id}/preview — Admin preview of any deck's content.</summary>
    [HttpGet("decks/{id}/preview")]
    public async Task<IActionResult> PreviewDeck(Guid id)
    {
        try
        {
            var preview = await _adminService.GetDeckPreviewAsync(id);
            return Ok(preview);
        }
        catch (Exception ex) { return HandleError(ex); }
    }
}

/// <summary>User-facing report submission endpoint (any authenticated user).</summary>
[Route("api/reports")]
[Authorize]
public class ReportsController : BaseController
{
    private readonly AdminService _adminService;

    public ReportsController(AdminService adminService) => _adminService = adminService;

    /// <summary>POST /api/reports — Submit a content report.</summary>
    [HttpPost]
    public async Task<IActionResult> SubmitReport([FromBody] SubmitReportRequest request)
    {
        try
        {
            var userId = Guid.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
            await _adminService.SubmitReportAsync(userId, request);
            return Ok(new { message = "Report submitted successfully" });
        }
        catch (Exception ex) { return HandleError(ex); }
    }
}
