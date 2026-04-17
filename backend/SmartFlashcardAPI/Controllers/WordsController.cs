using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFlashcardAPI.Services;

namespace SmartFlashcardAPI.Controllers;

/// <summary>
/// Word analysis endpoints — polysemy detection, homonym grouping, variant discovery.
/// </summary>
[Route("api/words")]
[Authorize]
public class WordsController : BaseController
{
    private readonly WordAnalysisService _wordService;

    public WordsController(WordAnalysisService wordService)
    {
        _wordService = wordService;
    }

    /// <summary>
    /// Analyze a word: detect all meanings, homonyms, and morphological variants.
    /// </summary>
    [HttpPost("analyze")]
    public async Task<IActionResult> AnalyzeWord([FromBody] WordAnalyzeRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Word))
            return BadRequest(new { error = "Word is required." });

        if (string.IsNullOrWhiteSpace(request.Definition))
            return BadRequest(new { error = "Definition is required." });

        var result = await _wordService.AnalyzeWordAsync(
            request.Word,
            request.Definition,
            request.Context
        );

        return Ok(result);
    }
}

public class WordAnalyzeRequest
{
    public string Word { get; set; } = "";
    public string Definition { get; set; } = "";
    public string? Context { get; set; }
}
