using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

[Table("HomonymGroups")]
public class HomonymGroup
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    public Guid LemmaId { get; set; }

    [MaxLength(200)]
    public string? Label { get; set; }

    // Navigation
    [ForeignKey("LemmaId")]
    public Lemma Lemma { get; set; } = null!;

    public ICollection<WordEntry> Entries { get; set; } = new List<WordEntry>();
}
