using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

/// <summary>
/// Community IPA pronunciation cache.
/// Stores AI-generated or user-submitted IPA transcriptions keyed by normalized
/// "frontText|backText" to avoid redundant AI calls for the same word+meaning pair.
/// </summary>
[Table("IpaCaches")]
public class IpaCache
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    /// <summary>Normalized lookup key: "fronttext|backtext" (lowercase, trimmed)</summary>
    [Required, MaxLength(500)]
    public string LookupKey { get; set; } = string.Empty;

    /// <summary>IPA transcription string, e.g. "/ˈæp.əl/"</summary>
    [Required, MaxLength(200)]
    public string Ipa { get; set; } = string.Empty;

    /// <summary>Original front text (for display/debugging)</summary>
    [MaxLength(200)]
    public string FrontText { get; set; } = string.Empty;

    /// <summary>Original back text (for display/debugging)</summary>
    [MaxLength(500)]
    public string? BackText { get; set; }

    /// <summary>Number of times this cache entry was used by other users</summary>
    public int UsageCount { get; set; } = 0;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
