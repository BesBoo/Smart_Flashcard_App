using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

[Table("AiChatHistory")]
public class AiChatMessage
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    public Guid UserId { get; set; }
    public Guid SessionId { get; set; }

    [Required, MaxLength(20)]
    public string Role { get; set; } = "user"; // "user" or "assistant"

    [Required]
    public string Content { get; set; } = string.Empty;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    // Navigation
    [ForeignKey("UserId")]
    public User User { get; set; } = null!;
}
