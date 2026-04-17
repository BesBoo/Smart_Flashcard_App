using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

[Table("WordSenses")]
public class WordSense
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    public Guid WordEntryId { get; set; }

    [Required, MaxLength(500)]
    public string Definition { get; set; } = string.Empty;

    [MaxLength(500)]
    public string? DefinitionVi { get; set; }

    [MaxLength(1000)]
    public string? Example { get; set; }

    public int SenseOrder { get; set; } = 1;

    public Guid? SemanticClusterId { get; set; }

    // Navigation
    [ForeignKey("WordEntryId")]
    public WordEntry WordEntry { get; set; } = null!;

    [ForeignKey("SemanticClusterId")]
    public SemanticCluster? SemanticCluster { get; set; }
}
