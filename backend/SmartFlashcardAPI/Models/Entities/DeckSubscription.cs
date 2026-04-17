using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

/// <summary>
/// Represents a user's subscription (read-only or edit) to another user's shared deck.
/// The subscriber sees the owner's deck content directly — no data copy is made.
/// </summary>
[Table("DeckSubscriptions")]
public class DeckSubscription
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    public Guid DeckId { get; set; }

    /// <summary>The user who subscribed (NOT the deck owner)</summary>
    public Guid SubscriberId { get; set; }

    /// <summary>Permission level: "read" or "edit"</summary>
    [Required, MaxLength(10)]
    public string Permission { get; set; } = "read";

    public DateTime JoinedAt { get; set; } = DateTime.UtcNow;

    public bool IsActive { get; set; } = true;

    // Navigation
    [ForeignKey("DeckId")]
    public Deck Deck { get; set; } = null!;

    [ForeignKey("SubscriberId")]
    public User Subscriber { get; set; } = null!;
}
