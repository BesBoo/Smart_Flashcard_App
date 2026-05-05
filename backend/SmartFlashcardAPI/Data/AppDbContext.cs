using Microsoft.EntityFrameworkCore;
using SmartFlashcardAPI.Models.Entities;

namespace SmartFlashcardAPI.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    // Tell Npgsql to map DateTime as 'timestamp without time zone'
    // so we don't need to change all DateTime.Now → DateTime.UtcNow across the codebase.
    static AppDbContext()
    {
        AppContext.SetSwitch("Npgsql.EnableLegacyTimestampBehavior", true);
    }

    public DbSet<User> Users => Set<User>();
    public DbSet<Deck> Decks => Set<Deck>();
    public DbSet<Flashcard> Flashcards => Set<Flashcard>();
    public DbSet<ReviewLog> ReviewLogs => Set<ReviewLog>();
    public DbSet<AiChatMessage> AiChatMessages => Set<AiChatMessage>();
    public DbSet<SyncMetadata> SyncMetadata => Set<SyncMetadata>();
    public DbSet<ContentReport> ContentReports => Set<ContentReport>();
    public DbSet<DeckShare> DeckShares => Set<DeckShare>();
    public DbSet<DeckSubscription> DeckSubscriptions => Set<DeckSubscription>();
    public DbSet<AiUsageLog> AiUsageLogs => Set<AiUsageLog>();
    public DbSet<PasswordResetToken> PasswordResetTokens => Set<PasswordResetToken>();

    // Polysemy tables
    public DbSet<Lemma> Lemmas => Set<Lemma>();
    public DbSet<WordEntry> WordEntries => Set<WordEntry>();
    public DbSet<WordSense> WordSenses => Set<WordSense>();
    public DbSet<HomonymGroup> HomonymGroups => Set<HomonymGroup>();
    public DbSet<WordVariant> WordVariants => Set<WordVariant>();
    public DbSet<SemanticCluster> SemanticClusters => Set<SemanticCluster>();

    // Community image library
    public DbSet<SharedImage> SharedImages => Set<SharedImage>();

    // IPA pronunciation cache
    public DbSet<IpaCache> IpaCaches => Set<IpaCache>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        // ── Users ───────────────────────────────────────────
        modelBuilder.Entity<User>(entity =>
        {
            entity.HasIndex(e => e.Email).IsUnique();
            entity.Property(e => e.SubscriptionTier).HasDefaultValue("Free");
            entity.Property(e => e.PreferredLanguage).HasDefaultValue("vi");
            entity.Property(e => e.Role).HasMaxLength(20).HasDefaultValue("user");
        });

        // ── Decks ───────────────────────────────────────────
        modelBuilder.Entity<Deck>(entity =>
        {
            entity.HasIndex(e => e.UserId)
                  .HasFilter("\"IsDeleted\" = false")
                  .HasDatabaseName("IX_Decks_UserId");

            entity.Property(e => e.Language).HasDefaultValue("vi");

            entity.HasOne(d => d.User)
                  .WithMany(u => u.Decks)
                  .HasForeignKey(d => d.UserId)
                  .OnDelete(DeleteBehavior.NoAction);
        });

        // ── DeckShares ──────────────────────────────────────
        modelBuilder.Entity<DeckShare>(entity =>
        {
            entity.HasIndex(e => e.ShareCode).IsUnique();
            entity.HasIndex(e => e.DeckId);

            entity.HasOne(s => s.Deck)
                  .WithMany(d => d.DeckShares)
                  .HasForeignKey(s => s.DeckId)
                  .OnDelete(DeleteBehavior.NoAction);
        });

        // ── DeckSubscriptions ───────────────────────────────
        modelBuilder.Entity<DeckSubscription>(entity =>
        {
            entity.HasIndex(e => new { e.DeckId, e.SubscriberId })
                  .IsUnique()
                  .HasDatabaseName("IX_DeckSubscriptions_DeckUser");

            entity.HasIndex(e => e.SubscriberId)
                  .HasDatabaseName("IX_DeckSubscriptions_Subscriber");

            entity.HasOne(s => s.Deck)
                  .WithMany(d => d.Subscriptions)
                  .HasForeignKey(s => s.DeckId)
                  .OnDelete(DeleteBehavior.NoAction);

            entity.HasOne(s => s.Subscriber)
                  .WithMany()
                  .HasForeignKey(s => s.SubscriberId)
                  .OnDelete(DeleteBehavior.NoAction);
        });

        // ── Flashcards ──────────────────────────────────────
        modelBuilder.Entity<Flashcard>(entity =>
        {
            entity.HasIndex(e => new { e.UserId, e.DeckId })
                  .HasFilter("\"IsDeleted\" = false")
                  .HasDatabaseName("IX_Flashcards_UserDeck");

            entity.HasIndex(e => new { e.UserId, e.NextReviewDate })
                  .HasFilter("\"IsDeleted\" = false")
                  .HasDatabaseName("IX_Flashcards_NextReview");

            entity.HasIndex(e => new { e.UserId, e.UpdatedAt })
                  .HasDatabaseName("IX_Flashcards_UpdatedAt");

            entity.Property(e => e.EaseFactor).HasDefaultValue(2.5);
            entity.Property(e => e.IntervalDays).HasDefaultValue(1);

            entity.HasOne(f => f.User)
                  .WithMany()
                  .HasForeignKey(f => f.UserId)
                  .OnDelete(DeleteBehavior.NoAction);

            entity.HasOne(f => f.Deck)
                  .WithMany(d => d.Flashcards)
                  .HasForeignKey(f => f.DeckId)
                  .OnDelete(DeleteBehavior.NoAction);

            entity.HasOne(f => f.WordSense)
                  .WithMany()
                  .HasForeignKey(f => f.WordSenseId)
                  .OnDelete(DeleteBehavior.SetNull)
                  .IsRequired(false);
        });

        // ── Lemmas ───────────────────────────────────────────
        modelBuilder.Entity<Lemma>(entity =>
        {
            entity.HasIndex(e => new { e.LemmaText, e.Language }).IsUnique();
        });

        // ── WordEntries ──────────────────────────────────────
        modelBuilder.Entity<WordEntry>(entity =>
        {
            entity.HasIndex(e => e.LemmaId);
            entity.HasIndex(e => e.PartOfSpeech);

            entity.HasOne(e => e.Lemma)
                  .WithMany(l => l.WordEntries)
                  .HasForeignKey(e => e.LemmaId)
                  .OnDelete(DeleteBehavior.NoAction);

            entity.HasOne(e => e.HomonymGroup)
                  .WithMany(h => h.Entries)
                  .HasForeignKey(e => e.HomonymGroupId)
                  .OnDelete(DeleteBehavior.SetNull)
                  .IsRequired(false);
        });

        // ── WordSenses ───────────────────────────────────────
        modelBuilder.Entity<WordSense>(entity =>
        {
            entity.HasIndex(e => e.WordEntryId);

            entity.HasOne(s => s.WordEntry)
                  .WithMany(e => e.Senses)
                  .HasForeignKey(s => s.WordEntryId)
                  .OnDelete(DeleteBehavior.NoAction);
        });

        // ── HomonymGroups ────────────────────────────────────
        modelBuilder.Entity<HomonymGroup>(entity =>
        {
            entity.HasIndex(e => e.LemmaId);

            entity.HasOne(h => h.Lemma)
                  .WithMany(l => l.HomonymGroups)
                  .HasForeignKey(h => h.LemmaId)
                  .OnDelete(DeleteBehavior.NoAction);
        });

        // ── WordVariants ─────────────────────────────────────
        modelBuilder.Entity<WordVariant>(entity =>
        {
            entity.HasIndex(e => e.LemmaId);
            entity.HasIndex(e => e.VariantText);

            entity.HasOne(v => v.Lemma)
                  .WithMany(l => l.Variants)
                  .HasForeignKey(v => v.LemmaId)
                  .OnDelete(DeleteBehavior.NoAction);
        });

        // ── ReviewLogs ──────────────────────────────────────
        modelBuilder.Entity<ReviewLog>(entity =>
        {
            entity.HasIndex(e => e.FlashcardId)
                  .HasDatabaseName("IX_ReviewLogs_FlashcardId");

            entity.HasIndex(e => new { e.UserId, e.ReviewedAt })
                  .HasDatabaseName("IX_ReviewLogs_UserDate");

            entity.HasOne(r => r.User)
                  .WithMany(u => u.ReviewLogs)
                  .HasForeignKey(r => r.UserId)
                  .OnDelete(DeleteBehavior.NoAction);

            entity.HasOne(r => r.Flashcard)
                  .WithMany(f => f.ReviewLogs)
                  .HasForeignKey(r => r.FlashcardId)
                  .OnDelete(DeleteBehavior.NoAction);
        });

        // ── AiChatHistory ───────────────────────────────────
        modelBuilder.Entity<AiChatMessage>(entity =>
        {
            entity.HasIndex(e => new { e.UserId, e.SessionId, e.CreatedAt })
                  .HasDatabaseName("IX_AiChat_UserSession");

            entity.HasOne(a => a.User)
                  .WithMany(u => u.AiChatMessages)
                  .HasForeignKey(a => a.UserId)
                  .OnDelete(DeleteBehavior.NoAction);
        });

        // ── ContentReports ──────────────────────────────────
        modelBuilder.Entity<ContentReport>(entity =>
        {
            entity.HasIndex(e => e.Status)
                  .HasDatabaseName("IX_ContentReports_Status");

            entity.HasOne(r => r.ReportedByUser)
                  .WithMany()
                  .HasForeignKey(r => r.ReportedByUserId)
                  .OnDelete(DeleteBehavior.NoAction);
        });

        // ── SyncMetadata ────────────────────────────────────
        modelBuilder.Entity<SyncMetadata>(entity =>
        {
            entity.HasIndex(e => new { e.UserId, e.IsSynced })
                  .HasFilter("\"IsSynced\" = false")
                  .HasDatabaseName("IX_Sync_UserPending");

            entity.HasOne(s => s.User)
                  .WithMany()
                  .HasForeignKey(s => s.UserId)
                  .OnDelete(DeleteBehavior.NoAction);
        });
        // ── AiUsageLogs ─────────────────────────────────────
        modelBuilder.Entity<AiUsageLog>(entity =>
        {
            entity.HasIndex(e => e.CreatedAt)
                  .HasDatabaseName("IX_AiUsageLogs_CreatedAt");

            entity.HasIndex(e => new { e.UserId, e.CreatedAt })
                  .HasDatabaseName("IX_AiUsageLogs_UserDate");

            entity.HasIndex(e => e.PromptType)
                  .HasDatabaseName("IX_AiUsageLogs_PromptType");

            entity.HasOne(a => a.User)
                  .WithMany()
                  .HasForeignKey(a => a.UserId)
                  .OnDelete(DeleteBehavior.SetNull)
                  .IsRequired(false);
        });

        // ── SharedImages ────────────────────────────────────
        modelBuilder.Entity<SharedImage>(entity =>
        {
            entity.HasIndex(e => e.Keyword)
                  .HasFilter("\"IsDeleted\" = false")
                  .HasDatabaseName("IX_SharedImages_Keyword");

            entity.HasOne(s => s.User)
                  .WithMany()
                  .HasForeignKey(s => s.UserId)
                  .OnDelete(DeleteBehavior.NoAction);
        });

        // ── IpaCaches ───────────────────────────────────────
        modelBuilder.Entity<IpaCache>(entity =>
        {
            entity.HasIndex(e => e.LookupKey)
                  .IsUnique()
                  .HasDatabaseName("IX_IpaCaches_LookupKey");
        });
    }
}
