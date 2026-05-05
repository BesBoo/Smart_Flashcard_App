using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Caching.Memory;
using SmartFlashcardAPI.Data;
using SmartFlashcardAPI.Models.DTOs;
using SmartFlashcardAPI.Models.Entities;
using SmartFlashcardAPI.Services;

namespace SmartFlashcardAPI.Controllers;

/// <summary>
/// AI endpoints — proxies requests to Gemini API via GeminiService.
/// Tracks AI usage per user and enforces daily limits.
/// </summary>
[Route("api/ai")]
[Authorize]
public class AiController : BaseController
{
    private readonly GeminiService _gemini;
    private readonly AppDbContext _db;
    private readonly IConfiguration _config;
    private readonly IMemoryCache _cache;

    public AiController(GeminiService gemini, AppDbContext db, IConfiguration config, IMemoryCache cache)
    {
        _gemini = gemini;
        _db = db;
        _config = config;
        _cache = cache;
    }

    // ── Usage Tracking Helpers ──────────────────────────────────

    private async Task<User> GetCurrentUserAsync()
    {
        var userId = GetUserId();
        var user = await _db.Users.FindAsync(userId)
            ?? throw new UnauthorizedAccessException("User not found");

        // Reset counter if it's a new day
        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        if (user.AiUsageResetDate < today)
        {
            user.AiUsageToday = 0;
            user.AiUsageResetDate = today;
        }

        return user;
    }

    private int GetDailyLimit(User user, string limitKey)
    {
        var tier = user.SubscriptionTier == "Premium" ? "Premium" : "Free";
        return _config.GetValue<int>($"RateLimits:{tier}:{limitKey}", 20);
    }

    private AiUsageInfo GetUsageInfo(User user, string limitKey)
    {
        return new AiUsageInfo(user.AiUsageToday, GetDailyLimit(user, limitKey));
    }

    private async Task<AiUsageInfo> IncrementUsageAsync(User user, string limitKey)
    {
        user.AiUsageToday++;
        user.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();
        return GetUsageInfo(user, limitKey);
    }

    private IActionResult QuotaExceeded(AiUsageInfo usage)
    {
        return StatusCode(429, new
        {
            error = new { code = "QUOTA_EXCEEDED", message = "Bạn đã hết lượt AI miễn phí hôm nay." },
            usage
        });
    }

    // ── AI Endpoints ────────────────────────────────────────────

    /// <summary>POST /api/ai/flashcards/text — Generate flashcard drafts from text</summary>
    [HttpPost("flashcards/text")]
    public async Task<IActionResult> GenerateFromText([FromBody] AiGenerateTextRequest request)
    {
        try
        {
            var user = await GetCurrentUserAsync();
            var limit = GetDailyLimit(user, "TextGenPerDay");
            if (user.AiUsageToday >= limit)
                return QuotaExceeded(GetUsageInfo(user, "TextGenPerDay"));

            // Cache by text hash to avoid duplicate AI calls
            var cacheKey = $"ai:text:{request.Text.GetHashCode()}:{request.Language}:{request.MaxCards}";
            if (_cache.TryGetValue(cacheKey, out List<DraftCard>? cachedDrafts) && cachedDrafts != null)
            {
                var cachedUsage = await IncrementUsageAsync(user, "TextGenPerDay");
                return Ok(new AiGenerateResponse(cachedDrafts, cachedUsage));
            }

            var drafts = await _gemini.GenerateFlashcardsFromTextAsync(
                request.Text, request.Language, request.MaxCards, GetUserId());
            _cache.Set(cacheKey, drafts, TimeSpan.FromHours(6));
            var usage = await IncrementUsageAsync(user, "TextGenPerDay");
            return Ok(new AiGenerateResponse(drafts, usage));
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/ai/flashcards/extract-vocab — Upload PDF/DOCX, extract vocabulary words</summary>
    [HttpPost("flashcards/extract-vocab")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> ExtractVocabulary(
        IFormFile file,
        [FromForm] string targetLanguage = "vi",
        [FromForm] int maxWords = 20)
    {
        try
        {
            if (file == null || file.Length == 0)
                return BadRequest(new { error = new { code = "VALIDATION_ERROR", message = "No file uploaded" } });
            if (file.Length > 10 * 1024 * 1024)
                return BadRequest(new { error = new { code = "VALIDATION_ERROR", message = "File too large (max 10MB)" } });

            var user = await GetCurrentUserAsync();
            var limit = GetDailyLimit(user, "FileGenPerDay");
            if (user.AiUsageToday >= limit)
                return QuotaExceeded(GetUsageInfo(user, "FileGenPerDay"));

            // Extract text based on file type
            string extractedText;
            var ext = Path.GetExtension(file.FileName).ToLower();

            if (ext == ".pdf")
            {
                using var stream = file.OpenReadStream();
                using var pdfDoc = UglyToad.PdfPig.PdfDocument.Open(stream);
                var sb = new System.Text.StringBuilder();
                foreach (var page in pdfDoc.GetPages())
                    sb.AppendLine(page.Text);
                extractedText = sb.ToString().Trim();
            }
            else if (ext == ".docx")
            {
                using var stream = file.OpenReadStream();
                using var doc = DocumentFormat.OpenXml.Packaging.WordprocessingDocument.Open(stream, false);
                extractedText = doc.MainDocumentPart?.Document?.Body?.InnerText?.Trim() ?? "";
            }
            else
            {
                return BadRequest(new { error = new { code = "VALIDATION_ERROR", message = "File must be PDF or DOCX" } });
            }

            if (string.IsNullOrWhiteSpace(extractedText))
                return BadRequest(new { error = new { code = "EXTRACTION_ERROR", message = "Could not extract text from file" } });

            if (extractedText.Length > 8000)
                extractedText = extractedText[..8000];

            // Cache by text hash to avoid duplicate AI calls for same content
            var cacheKey = $"ai:vocab:{extractedText.GetHashCode()}:{targetLanguage}:{maxWords}";
            if (_cache.TryGetValue(cacheKey, out List<DraftCard>? cachedDrafts) && cachedDrafts != null)
            {
                var cachedUsage = await IncrementUsageAsync(user, "FileGenPerDay");
                return Ok(new AiGenerateResponse(cachedDrafts, cachedUsage));
            }

            var drafts = await _gemini.ExtractVocabularyFromTextAsync(extractedText, targetLanguage, maxWords, GetUserId());
            _cache.Set(cacheKey, drafts, TimeSpan.FromHours(6));
            var usage = await IncrementUsageAsync(user, "FileGenPerDay");
            return Ok(new AiGenerateResponse(drafts, usage));
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/ai/flashcards/pdf — Upload PDF, extract text, generate flashcards</summary>
    [HttpPost("flashcards/pdf")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> GenerateFromPdf(IFormFile file, [FromForm] string language = "en", [FromForm] int maxCards = 10)
    {
        try
        {
            if (file == null || file.Length == 0)
                return BadRequest(new { error = new { code = "VALIDATION_ERROR", message = "No file uploaded" } });
            if (!file.FileName.EndsWith(".pdf", StringComparison.OrdinalIgnoreCase))
                return BadRequest(new { error = new { code = "VALIDATION_ERROR", message = "File must be a PDF" } });
            if (file.Length > 10 * 1024 * 1024) // 10MB limit
                return BadRequest(new { error = new { code = "VALIDATION_ERROR", message = "File too large (max 10MB)" } });

            var user = await GetCurrentUserAsync();
            var limit = GetDailyLimit(user, "FileGenPerDay");
            if (user.AiUsageToday >= limit)
                return QuotaExceeded(GetUsageInfo(user, "FileGenPerDay"));

            // Extract text from PDF using PdfPig
            string extractedText;
            using (var stream = file.OpenReadStream())
            using (var pdfDoc = UglyToad.PdfPig.PdfDocument.Open(stream))
            {
                var sb = new System.Text.StringBuilder();
                foreach (var page in pdfDoc.GetPages())
                {
                    sb.AppendLine(page.Text);
                }
                extractedText = sb.ToString().Trim();
            }

            if (string.IsNullOrWhiteSpace(extractedText))
                return BadRequest(new { error = new { code = "EXTRACTION_ERROR", message = "Could not extract text from PDF" } });

            // Truncate to ~8000 chars to stay within token limits
            if (extractedText.Length > 8000)
                extractedText = extractedText[..8000];

            var drafts = await _gemini.GenerateFlashcardsFromTextAsync(extractedText, language, maxCards, GetUserId());
            var usage = await IncrementUsageAsync(user, "FileGenPerDay");
            return Ok(new AiGenerateResponse(drafts, usage));
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/ai/flashcards/docx — Upload DOCX, extract text, generate flashcards</summary>
    [HttpPost("flashcards/docx")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> GenerateFromDocx(IFormFile file, [FromForm] string language = "en", [FromForm] int maxCards = 10)
    {
        try
        {
            if (file == null || file.Length == 0)
                return BadRequest(new { error = new { code = "VALIDATION_ERROR", message = "No file uploaded" } });
            if (!file.FileName.EndsWith(".docx", StringComparison.OrdinalIgnoreCase))
                return BadRequest(new { error = new { code = "VALIDATION_ERROR", message = "File must be a DOCX" } });
            if (file.Length > 10 * 1024 * 1024)
                return BadRequest(new { error = new { code = "VALIDATION_ERROR", message = "File too large (max 10MB)" } });

            var user = await GetCurrentUserAsync();
            var limit = GetDailyLimit(user, "FileGenPerDay");
            if (user.AiUsageToday >= limit)
                return QuotaExceeded(GetUsageInfo(user, "FileGenPerDay"));

            // Extract text from DOCX using OpenXml
            string extractedText;
            using (var stream = file.OpenReadStream())
            using (var doc = DocumentFormat.OpenXml.Packaging.WordprocessingDocument.Open(stream, false))
            {
                var body = doc.MainDocumentPart?.Document?.Body;
                extractedText = body?.InnerText?.Trim() ?? "";
            }

            if (string.IsNullOrWhiteSpace(extractedText))
                return BadRequest(new { error = new { code = "EXTRACTION_ERROR", message = "Could not extract text from DOCX" } });

            if (extractedText.Length > 8000)
                extractedText = extractedText[..8000];

            var drafts = await _gemini.GenerateFlashcardsFromTextAsync(extractedText, language, maxCards, GetUserId());
            var usage = await IncrementUsageAsync(user, "FileGenPerDay");
            return Ok(new AiGenerateResponse(drafts, usage));
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/ai/example — Generate example sentence for a card</summary>
    [HttpPost("example")]
    public async Task<IActionResult> GenerateExample([FromBody] AiExampleRequest request)
    {
        try
        {
            var user = await GetCurrentUserAsync();
            var limit = GetDailyLimit(user, "ExamplePerDay");
            if (user.AiUsageToday >= limit)
                return QuotaExceeded(GetUsageInfo(user, "ExamplePerDay"));

            // Cache examples by word pair
            var cacheKey = $"ai:example:{request.FrontText?.GetHashCode()}:{request.BackText?.GetHashCode()}:{request.Language}";
            if (_cache.TryGetValue(cacheKey, out string? cachedExample) && cachedExample != null)
            {
                var cachedUsage = await IncrementUsageAsync(user, "ExamplePerDay");
                return Ok(new AiExampleResponse(cachedExample, cachedUsage));
            }

            var example = await _gemini.GenerateExampleAsync(
                request.FrontText, request.BackText, request.Language, GetUserId());
            _cache.Set(cacheKey, example, TimeSpan.FromHours(24));
            var usage = await IncrementUsageAsync(user, "ExamplePerDay");
            return Ok(new AiExampleResponse(example, usage));
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/ai/ipa — Generate or lookup IPA pronunciation (with community cache)</summary>
    [HttpPost("ipa")]
    public async Task<IActionResult> GenerateIpa([FromBody] AiIpaRequest request)
    {
        try
        {
            if (string.IsNullOrWhiteSpace(request.FrontText))
                return BadRequest(new { error = new { code = "VALIDATION_ERROR", message = "FrontText is required" } });

            // 1. Check IPA cache first
            var lookupKey = FlashcardService.NormalizeIpaKey(request.FrontText, request.BackText);
            var cached = await _db.IpaCaches.FirstOrDefaultAsync(c => c.LookupKey == lookupKey);
            if (cached != null)
            {
                // Cache hit — no AI quota consumed!
                cached.UsageCount++;
                await _db.SaveChangesAsync();
                return Ok(new AiIpaResponse(cached.Ipa, FromCache: true));
            }

            // 2. Cache miss — check quota and call AI
            var user = await GetCurrentUserAsync();
            var limit = GetDailyLimit(user, "ExamplePerDay");
            if (user.AiUsageToday >= limit)
                return QuotaExceeded(GetUsageInfo(user, "ExamplePerDay"));

            var ipa = await _gemini.GenerateIpaAsync(request.FrontText, request.BackText, GetUserId());

            // 3. Cache the result for future users
            try
            {
                _db.IpaCaches.Add(new IpaCache
                {
                    LookupKey = lookupKey,
                    Ipa = ipa,
                    FrontText = request.FrontText.Trim(),
                    BackText = request.BackText?.Trim()
                });
                await _db.SaveChangesAsync();
            }
            catch { /* unique constraint — another request cached it first, ignore */ }

            var usage = await IncrementUsageAsync(user, "ExamplePerDay");
            return Ok(new AiIpaResponse(ipa, FromCache: false));
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/ai/image — Generate image URL for a card using Gemini</summary>
    [HttpPost("image")]
    public async Task<IActionResult> GenerateImage([FromBody] AiImageRequest request)
    {
        try
        {
            var user = await GetCurrentUserAsync();
            var limit = GetDailyLimit(user, "FileGenPerDay");
            if (user.AiUsageToday >= limit)
                return QuotaExceeded(GetUsageInfo(user, "FileGenPerDay"));

            var baseUrl = $"{Request.Scheme}://{Request.Host}";
            var imageUrl = await _gemini.GenerateImageUrlAsync(request.FrontText, request.BackText, baseUrl, GetUserId());
            if (imageUrl == null)
                return Ok(new { imageUrl = (string?)null, message = "Không tìm thấy ảnh phù hợp" });

            var usage = await IncrementUsageAsync(user, "FileGenPerDay");
            return Ok(new { imageUrl, usage });
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/ai/quiz — Generate multiple-choice questions</summary>
    [HttpPost("quiz")]
    public async Task<IActionResult> GenerateQuiz([FromBody] AiQuizRequest request)
    {
        try
        {
            var user = await GetCurrentUserAsync();
            var limit = GetDailyLimit(user, "TextGenPerDay");
            if (user.AiUsageToday >= limit)
                return QuotaExceeded(GetUsageInfo(user, "TextGenPerDay"));

            var questions = await _gemini.GenerateQuizAsync(
                request.Cards, request.QuestionCount, request.Language, GetUserId());
            var usage = await IncrementUsageAsync(user, "TextGenPerDay");
            return Ok(new AiQuizResponse(questions));
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/ai/tutor — AI Tutor chat</summary>
    [HttpPost("tutor")]
    public async Task<IActionResult> TutorChat([FromBody] AiTutorRequest request)
    {
        try
        {
            var user = await GetCurrentUserAsync();
            var limit = GetDailyLimit(user, "TutorMsgPerDay");
            if (user.AiUsageToday >= limit)
                return QuotaExceeded(GetUsageInfo(user, "TutorMsgPerDay"));

            var response = await _gemini.TutorChatAsync(
                request.Message, request.Language, request.History, GetUserId());
            var usage = await IncrementUsageAsync(user, "TutorMsgPerDay");
            return Ok(new { response, usage });
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/ai/adaptive — Get adaptive learning hints</summary>
    [HttpPost("adaptive")]
    public async Task<IActionResult> GetAdaptiveHint([FromBody] AiAdaptiveRequest request)
    {
        try
        {
            var user = await GetCurrentUserAsync();
            var limit = GetDailyLimit(user, "ExamplePerDay");
            if (user.AiUsageToday >= limit)
                return QuotaExceeded(GetUsageInfo(user, "ExamplePerDay"));

            var hint = await _gemini.GetAdaptiveHintAsync(request.Flashcard, request.Language, GetUserId());
            var usage = await IncrementUsageAsync(user, "ExamplePerDay");
            return Ok(new AiAdaptiveResponse(hint, usage));
        }
        catch (Exception ex) { return HandleError(ex); }
    }

    /// <summary>POST /api/ai/smart-review — Generate variant-based cloze questions for cross-deck review</summary>
    [HttpPost("smart-review")]
    public async Task<IActionResult> SmartReview([FromBody] SmartReviewRequest request)
    {
        try
        {
            var user = await GetCurrentUserAsync();
            var limit = GetDailyLimit(user, "TextGenPerDay");
            if (user.AiUsageToday >= limit)
                return QuotaExceeded(GetUsageInfo(user, "TextGenPerDay"));

            var questions = await _gemini.GenerateSmartReviewAsync(
                request.Words, request.QuestionCount, request.Language, GetUserId());
            var usage = await IncrementUsageAsync(user, "TextGenPerDay");
            return Ok(new SmartReviewResponse(questions, usage));
        }
        catch (Exception ex) { return HandleError(ex); }
    }
}
