using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

[Table("WordEntries")]
public class WordEntry
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    public Guid LemmaId { get; set; }
    public Guid? HomonymGroupId { get; set; }

    [Required, MaxLength(20)]
    public string PartOfSpeech { get; set; } = string.Empty;

    public int? FrequencyRank { get; set; }

    // Navigation
    [ForeignKey("LemmaId")]
    public Lemma Lemma { get; set; } = null!;

    [ForeignKey("HomonymGroupId")]
    public HomonymGroup? HomonymGroup { get; set; }

    public ICollection<WordSense> Senses { get; set; } = new List<WordSense>();
}
