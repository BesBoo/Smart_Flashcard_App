using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

/// <summary>
/// Shared image library — images tagged by keyword (frontText) for community suggestions.
/// When a user saves a card with a server-hosted image, it's indexed here so other users
/// creating cards with the same keyword can reuse the image.
/// </summary>
[Table("SharedImages")]
public class SharedImage
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    public Guid UserId { get; set; }

    /// <summary>Normalized keyword (lowercase, trimmed) from frontText</summary>
    [Required, MaxLength(200)]
    public string Keyword { get; set; } = string.Empty;

    /// <summary>Server-hosted image URL (or self-hosted blob endpoint)</summary>
    [Required, MaxLength(500)]
    public string ImageUrl { get; set; } = string.Empty;

    /// <summary>Raw image bytes stored in DB (persistent across deploys)</summary>
    public byte[]? ImageData { get; set; }

    /// <summary>MIME type e.g. "image/jpeg"</summary>
    [MaxLength(50)]
    public string? ContentType { get; set; }

    /// <summary>Number of times other users used this image</summary>
    public int UsageCount { get; set; } = 0;

    public bool IsDeleted { get; set; } = false;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    // Navigation
    [ForeignKey("UserId")]
    public User User { get; set; } = null!;
}
