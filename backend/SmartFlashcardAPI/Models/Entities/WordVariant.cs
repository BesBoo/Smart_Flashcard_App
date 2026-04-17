using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

[Table("WordVariants")]
public class WordVariant
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    public Guid LemmaId { get; set; }

    [Required, MaxLength(100)]
    public string VariantText { get; set; } = string.Empty;

    [Required, MaxLength(50)]
    public string VariantType { get; set; } = string.Empty;

    // Navigation
    [ForeignKey("LemmaId")]
    public Lemma Lemma { get; set; } = null!;
}
