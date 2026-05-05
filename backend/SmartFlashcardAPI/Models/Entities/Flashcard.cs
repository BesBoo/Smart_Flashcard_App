using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

[Table("Flashcards")]
public class Flashcard
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    public Guid UserId { get; set; }
    public Guid DeckId { get; set; }

    [Required]
    public string FrontText { get; set; } = string.Empty;

    [Required]
    public string BackText { get; set; } = string.Empty;

    public string? ExampleText { get; set; }

    [MaxLength(200)]
    public string? PronunciationIpa { get; set; }

    [MaxLength(500)]
    public string? ImageUrl { get; set; }

    [MaxLength(500)]
    public string? AudioUrl { get; set; }

    // SM-2 State — CLIENT-CALCULATED, server stores only
    public int Repetition { get; set; } = 0;
    public int IntervalDays { get; set; } = 1;
    public double EaseFactor { get; set; } = 2.5;
    public DateTime NextReviewDate { get; set; } = DateTime.UtcNow;
    public int FailCount { get; set; } = 0;
    public int TotalReviews { get; set; } = 0;

    // Polysemy link (nullable = backward compatible)
    public Guid? WordSenseId { get; set; }

    public bool IsDeleted { get; set; } = false;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    // Navigation
    [ForeignKey("UserId")]
    public User User { get; set; } = null!;

    [ForeignKey("DeckId")]
    public Deck Deck { get; set; } = null!;

    [ForeignKey("WordSenseId")]
    public WordSense? WordSense { get; set; }

    public ICollection<ReviewLog> ReviewLogs { get; set; } = new List<ReviewLog>();
}
