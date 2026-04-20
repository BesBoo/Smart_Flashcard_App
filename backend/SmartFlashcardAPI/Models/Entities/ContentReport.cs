using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

/// <summary>
/// User-submitted report about deck, flashcard, or user content.
/// </summary>
[Table("ContentReports")]
public class ContentReport
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    /// <summary>Who submitted the report.</summary>
    public Guid ReportedByUserId { get; set; }

    /// <summary>"Deck", "Flashcard", or "User".</summary>
    [Required, MaxLength(30)]
    public string TargetType { get; set; } = string.Empty;

    /// <summary>The ID of the offending entity.</summary>
    public Guid TargetId { get; set; }

    /// <summary>Free-text reason from the reporter.</summary>
    [Required, MaxLength(500)]
    public string Reason { get; set; } = string.Empty;

    /// <summary>"Pending", "Approved", "Rejected".</summary>
    [Required, MaxLength(20)]
    public string Status { get; set; } = "Pending";

    /// <summary>True after the deck owner has acknowledged the violation notice.</summary>
    public bool IsNotifiedToOwner { get; set; } = false;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    // Navigation
    [ForeignKey("ReportedByUserId")]
    public User ReportedByUser { get; set; } = null!;
}
