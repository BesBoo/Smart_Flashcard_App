using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

[Table("Decks")]
public class Deck
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    public Guid UserId { get; set; }

    [Required, MaxLength(200)]
    public string Name { get; set; } = string.Empty;

    [MaxLength(1000)]
    public string? Description { get; set; }

    [MaxLength(500)]
    public string? CoverImageUrl { get; set; }

    [Required, MaxLength(10)]
    public string Language { get; set; } = "vi";

    /// <summary>True when the deck has an active share link.</summary>
    public bool IsShared { get; set; } = false;

    /// <summary>Published Google Sheet CSV URL for auto-sync.</summary>
    [MaxLength(500)]
    public string? GoogleSheetUrl { get; set; }

    public bool IsDeleted { get; set; } = false;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    // Navigation
    [ForeignKey("UserId")]
    public User User { get; set; } = null!;
    public ICollection<Flashcard> Flashcards { get; set; } = new List<Flashcard>();
    public ICollection<DeckShare> DeckShares { get; set; } = new List<DeckShare>();
    public ICollection<DeckSubscription> Subscriptions { get; set; } = new List<DeckSubscription>();
}
