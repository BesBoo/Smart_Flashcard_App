using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

[Table("Lemmas")]
public class Lemma
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    [Required, MaxLength(100)]
    public string LemmaText { get; set; } = string.Empty;

    [MaxLength(10)]
    public string Language { get; set; } = "en";

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    // Navigation
    public ICollection<WordEntry> WordEntries { get; set; } = new List<WordEntry>();
    public ICollection<WordVariant> Variants { get; set; } = new List<WordVariant>();
    public ICollection<HomonymGroup> HomonymGroups { get; set; } = new List<HomonymGroup>();
}
