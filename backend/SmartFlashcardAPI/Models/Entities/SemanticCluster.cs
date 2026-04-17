using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace SmartFlashcardAPI.Models.Entities;

[Table("SemanticClusters")]
public class SemanticCluster
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    [MaxLength(200)]
    public string? ClusterLabel { get; set; }
}
