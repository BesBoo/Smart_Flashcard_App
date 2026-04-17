using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

/// <summary>
/// Tracks every AI API call for admin monitoring and analytics.
/// </summary>
[Table("AiUsageLogs")]
public class AiUsageLog
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    /// <summary>User who triggered the AI call (null for system calls).</summary>
    public Guid? UserId { get; set; }

    /// <summary>Type of AI operation: GenerateFlashcards, GenerateExample, ExtractVocab, TutorChat, GenerateImage, AdaptiveHint, Quiz, WordAnalysis.</summary>
    [Required, MaxLength(50)]
    public string PromptType { get; set; } = string.Empty;

    /// <summary>AI model used (e.g. gemini-2.5-flash, llama-3.3-70b).</summary>
    [MaxLength(100)]
    public string Model { get; set; } = string.Empty;

    /// <summary>Provider: Gemini, Groq, DeepSeek.</summary>
    [MaxLength(30)]
    public string Provider { get; set; } = "Gemini";

    /// <summary>Approximate token count (input + output).</summary>
    public int TokensUsed { get; set; }

    /// <summary>Success, Failed, RateLimited.</summary>
    [Required, MaxLength(20)]
    public string Status { get; set; } = "Success";

    /// <summary>Duration of the API call in milliseconds.</summary>
    public long DurationMs { get; set; }

    /// <summary>Error message if the call failed.</summary>
    [MaxLength(500)]
    public string? ErrorMessage { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    // Navigation
    [ForeignKey("UserId")]
    public User? User { get; set; }
}
