using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

/// <summary>
/// Represents a share link for a deck, identified by a unique 6-character alphanumeric code.
/// Created by the deck owner to allow others to subscribe to their deck.
/// </summary>
[Table("DeckShares")]
public class DeckShare
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    public Guid DeckId { get; set; }

    /// <summary>6-char alphanumeric code (A-Z, 2-9, no ambiguous chars)</summary>
    [Required, MaxLength(6)]
    public string ShareCode { get; set; } = string.Empty;

    /// <summary>If true, anyone with the code can join. If false, code is disabled.</summary>
    public bool IsPublic { get; set; } = true;

    /// <summary>Default permission for new subscribers: "read" or "edit"</summary>
    [Required, MaxLength(10)]
    public string DefaultPermission { get; set; } = "read";

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    /// <summary>Null = never expires</summary>
    public DateTime? ExpiresAt { get; set; }

    public bool IsActive { get; set; } = true;

    // Navigation
    [ForeignKey("DeckId")]
    public Deck Deck { get; set; } = null!;
}
