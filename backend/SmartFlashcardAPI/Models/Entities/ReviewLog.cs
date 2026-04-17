using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

[Table("ReviewLogs")]
public class ReviewLog
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    public Guid UserId { get; set; }
    public Guid FlashcardId { get; set; }

    [Required]
    public int Quality { get; set; } // 0 (Học lại), 2 (Khó), 3 (Tốt), 5 (Dễ)

    public long? ResponseTimeMs { get; set; }
    public DateTime ReviewedAt { get; set; } = DateTime.UtcNow;

    // Navigation
    [ForeignKey("UserId")]
    public User User { get; set; } = null!;

    [ForeignKey("FlashcardId")]
    public Flashcard Flashcard { get; set; } = null!;
}
