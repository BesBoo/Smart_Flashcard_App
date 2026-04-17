using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

[Table("Users")]
public class User
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    [Required, MaxLength(255)]
    public string Email { get; set; } = string.Empty;

    [Required, MaxLength(512)]
    public string PasswordHash { get; set; } = string.Empty;

    [Required, MaxLength(100)]
    public string DisplayName { get; set; } = string.Empty;

    [MaxLength(500)]
    public string? AvatarUrl { get; set; }

    [Required, MaxLength(20)]
    public string SubscriptionTier { get; set; } = "Free";

    public int AiUsageToday { get; set; } = 0;
    public DateOnly AiUsageResetDate { get; set; } = DateOnly.FromDateTime(DateTime.UtcNow);

    [MaxLength(10)]
    public string PreferredLanguage { get; set; } = "vi";

    /// <summary>Application role: "user" or "admin".</summary>
    [Required, MaxLength(20)]
    public string Role { get; set; } = "user";

    public bool IsActive { get; set; } = true;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    // Navigation
    public ICollection<Deck> Decks { get; set; } = new List<Deck>();
    public ICollection<ReviewLog> ReviewLogs { get; set; } = new List<ReviewLog>();
    public ICollection<AiChatMessage> AiChatMessages { get; set; } = new List<AiChatMessage>();
}
