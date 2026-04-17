using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

[Table("SyncMetadata")]
public class SyncMetadata
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    public Guid UserId { get; set; }

    [Required, MaxLength(50)]
    public string EntityType { get; set; } = string.Empty; // "deck", "flashcard", "review_log", "ai_chat"

    public Guid EntityId { get; set; }

    [Required, MaxLength(20)]
    public string Action { get; set; } = string.Empty; // "CREATE", "UPDATE", "DELETE"

    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    public bool IsSynced { get; set; } = false;

    // Navigation
    [ForeignKey("UserId")]
    public User User { get; set; } = null!;
}
