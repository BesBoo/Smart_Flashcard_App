using SmartFlashcardAPI.Data;
using SmartFlashcardAPI.Models.Entities;

namespace SmartFlashcardAPI.Services;

/// <summary>
/// Lightweight service to log AI API calls into AiUsageLogs table.
/// Designed to be fire-and-forget so it doesn't slow down AI responses.
/// </summary>
public class AiUsageLogger
{
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly ILogger<AiUsageLogger> _logger;

    public AiUsageLogger(IServiceScopeFactory scopeFactory, ILogger<AiUsageLogger> logger)
    {
        _scopeFactory = scopeFactory;
        _logger = logger;
    }

    /// <summary>
    /// Log an AI API call. Runs in background so it doesn't block the caller.
    /// </summary>
    public void Log(
        Guid? userId,
        string promptType,
        string model,
        string provider,
        int tokensUsed,
        string status,
        long durationMs,
        string? errorMessage = null)
    {
        // Fire-and-forget background task
        _ = Task.Run(async () =>
        {
            try
            {
                using var scope = _scopeFactory.CreateScope();
                var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();

                db.AiUsageLogs.Add(new AiUsageLog
                {
                    UserId = userId,
                    PromptType = promptType,
                    Model = model,
                    Provider = provider,
                    TokensUsed = tokensUsed,
                    Status = status,
                    DurationMs = durationMs,
                    ErrorMessage = errorMessage
                });

                await db.SaveChangesAsync();
            }
            catch (Exception ex)
            {
                _logger.LogWarning("Failed to log AI usage: {Msg}", ex.Message);
            }
        });
    }
}
