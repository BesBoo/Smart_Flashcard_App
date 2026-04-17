using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SmartFlashcardAPI.Migrations
{
    /// <inheritdoc />
    public partial class InitialPostgres : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "Lemmas",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    LemmaText = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: false),
                    Language = table.Column<string>(type: "character varying(10)", maxLength: 10, nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Lemmas", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "SemanticClusters",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    ClusterLabel = table.Column<string>(type: "character varying(200)", maxLength: 200, nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_SemanticClusters", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "Users",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    Email = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: false),
                    PasswordHash = table.Column<string>(type: "character varying(512)", maxLength: 512, nullable: false),
                    DisplayName = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: false),
                    AvatarUrl = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: true),
                    SubscriptionTier = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: false, defaultValue: "Free"),
                    AiUsageToday = table.Column<int>(type: "integer", nullable: false),
                    AiUsageResetDate = table.Column<DateOnly>(type: "date", nullable: false),
                    PreferredLanguage = table.Column<string>(type: "character varying(10)", maxLength: 10, nullable: false, defaultValue: "vi"),
                    Role = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: false, defaultValue: "user"),
                    IsActive = table.Column<bool>(type: "boolean", nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Users", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "HomonymGroups",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    LemmaId = table.Column<Guid>(type: "uuid", nullable: false),
                    Label = table.Column<string>(type: "character varying(200)", maxLength: 200, nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_HomonymGroups", x => x.Id);
                    table.ForeignKey(
                        name: "FK_HomonymGroups_Lemmas_LemmaId",
                        column: x => x.LemmaId,
                        principalTable: "Lemmas",
                        principalColumn: "Id");
                });

            migrationBuilder.CreateTable(
                name: "WordVariants",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    LemmaId = table.Column<Guid>(type: "uuid", nullable: false),
                    VariantText = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: false),
                    VariantType = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_WordVariants", x => x.Id);
                    table.ForeignKey(
                        name: "FK_WordVariants_Lemmas_LemmaId",
                        column: x => x.LemmaId,
                        principalTable: "Lemmas",
                        principalColumn: "Id");
                });

            migrationBuilder.CreateTable(
                name: "AiChatHistory",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    UserId = table.Column<Guid>(type: "uuid", nullable: false),
                    SessionId = table.Column<Guid>(type: "uuid", nullable: false),
                    Role = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: false),
                    Content = table.Column<string>(type: "text", nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_AiChatHistory", x => x.Id);
                    table.ForeignKey(
                        name: "FK_AiChatHistory_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id");
                });

            migrationBuilder.CreateTable(
                name: "AiUsageLogs",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    UserId = table.Column<Guid>(type: "uuid", nullable: true),
                    PromptType = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: false),
                    Model = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: false),
                    Provider = table.Column<string>(type: "character varying(30)", maxLength: 30, nullable: false),
                    TokensUsed = table.Column<int>(type: "integer", nullable: false),
                    Status = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: false),
                    DurationMs = table.Column<long>(type: "bigint", nullable: false),
                    ErrorMessage = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: true),
                    CreatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_AiUsageLogs", x => x.Id);
                    table.ForeignKey(
                        name: "FK_AiUsageLogs_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.SetNull);
                });

            migrationBuilder.CreateTable(
                name: "ContentReports",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    ReportedByUserId = table.Column<Guid>(type: "uuid", nullable: false),
                    TargetType = table.Column<string>(type: "character varying(30)", maxLength: 30, nullable: false),
                    TargetId = table.Column<Guid>(type: "uuid", nullable: false),
                    Reason = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: false),
                    Status = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_ContentReports", x => x.Id);
                    table.ForeignKey(
                        name: "FK_ContentReports_Users_ReportedByUserId",
                        column: x => x.ReportedByUserId,
                        principalTable: "Users",
                        principalColumn: "Id");
                });

            migrationBuilder.CreateTable(
                name: "Decks",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    UserId = table.Column<Guid>(type: "uuid", nullable: false),
                    Name = table.Column<string>(type: "character varying(200)", maxLength: 200, nullable: false),
                    Description = table.Column<string>(type: "character varying(1000)", maxLength: 1000, nullable: true),
                    CoverImageUrl = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: true),
                    Language = table.Column<string>(type: "character varying(10)", maxLength: 10, nullable: false, defaultValue: "vi"),
                    IsShared = table.Column<bool>(type: "boolean", nullable: false),
                    GoogleSheetUrl = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: true),
                    IsDeleted = table.Column<bool>(type: "boolean", nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Decks", x => x.Id);
                    table.ForeignKey(
                        name: "FK_Decks_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id");
                });

            migrationBuilder.CreateTable(
                name: "PasswordResetTokens",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    UserId = table.Column<Guid>(type: "uuid", nullable: false),
                    Token = table.Column<string>(type: "character varying(6)", maxLength: 6, nullable: false),
                    ExpiresAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false),
                    IsUsed = table.Column<bool>(type: "boolean", nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_PasswordResetTokens", x => x.Id);
                    table.ForeignKey(
                        name: "FK_PasswordResetTokens_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "SharedImages",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    UserId = table.Column<Guid>(type: "uuid", nullable: false),
                    Keyword = table.Column<string>(type: "character varying(200)", maxLength: 200, nullable: false),
                    ImageUrl = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: false),
                    UsageCount = table.Column<int>(type: "integer", nullable: false),
                    IsDeleted = table.Column<bool>(type: "boolean", nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_SharedImages", x => x.Id);
                    table.ForeignKey(
                        name: "FK_SharedImages_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id");
                });

            migrationBuilder.CreateTable(
                name: "SyncMetadata",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    UserId = table.Column<Guid>(type: "uuid", nullable: false),
                    EntityType = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: false),
                    EntityId = table.Column<Guid>(type: "uuid", nullable: false),
                    Action = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false),
                    IsSynced = table.Column<bool>(type: "boolean", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_SyncMetadata", x => x.Id);
                    table.ForeignKey(
                        name: "FK_SyncMetadata_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id");
                });

            migrationBuilder.CreateTable(
                name: "WordEntries",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    LemmaId = table.Column<Guid>(type: "uuid", nullable: false),
                    HomonymGroupId = table.Column<Guid>(type: "uuid", nullable: true),
                    PartOfSpeech = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: false),
                    FrequencyRank = table.Column<int>(type: "integer", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_WordEntries", x => x.Id);
                    table.ForeignKey(
                        name: "FK_WordEntries_HomonymGroups_HomonymGroupId",
                        column: x => x.HomonymGroupId,
                        principalTable: "HomonymGroups",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_WordEntries_Lemmas_LemmaId",
                        column: x => x.LemmaId,
                        principalTable: "Lemmas",
                        principalColumn: "Id");
                });

            migrationBuilder.CreateTable(
                name: "DeckShares",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    DeckId = table.Column<Guid>(type: "uuid", nullable: false),
                    ShareCode = table.Column<string>(type: "character varying(6)", maxLength: 6, nullable: false),
                    IsPublic = table.Column<bool>(type: "boolean", nullable: false),
                    DefaultPermission = table.Column<string>(type: "character varying(10)", maxLength: 10, nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false),
                    ExpiresAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: true),
                    IsActive = table.Column<bool>(type: "boolean", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_DeckShares", x => x.Id);
                    table.ForeignKey(
                        name: "FK_DeckShares_Decks_DeckId",
                        column: x => x.DeckId,
                        principalTable: "Decks",
                        principalColumn: "Id");
                });

            migrationBuilder.CreateTable(
                name: "DeckSubscriptions",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    DeckId = table.Column<Guid>(type: "uuid", nullable: false),
                    SubscriberId = table.Column<Guid>(type: "uuid", nullable: false),
                    Permission = table.Column<string>(type: "character varying(10)", maxLength: 10, nullable: false),
                    JoinedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false),
                    IsActive = table.Column<bool>(type: "boolean", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_DeckSubscriptions", x => x.Id);
                    table.ForeignKey(
                        name: "FK_DeckSubscriptions_Decks_DeckId",
                        column: x => x.DeckId,
                        principalTable: "Decks",
                        principalColumn: "Id");
                    table.ForeignKey(
                        name: "FK_DeckSubscriptions_Users_SubscriberId",
                        column: x => x.SubscriberId,
                        principalTable: "Users",
                        principalColumn: "Id");
                });

            migrationBuilder.CreateTable(
                name: "WordSenses",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    WordEntryId = table.Column<Guid>(type: "uuid", nullable: false),
                    Definition = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: false),
                    DefinitionVi = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: true),
                    Example = table.Column<string>(type: "character varying(1000)", maxLength: 1000, nullable: true),
                    SenseOrder = table.Column<int>(type: "integer", nullable: false),
                    SemanticClusterId = table.Column<Guid>(type: "uuid", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_WordSenses", x => x.Id);
                    table.ForeignKey(
                        name: "FK_WordSenses_SemanticClusters_SemanticClusterId",
                        column: x => x.SemanticClusterId,
                        principalTable: "SemanticClusters",
                        principalColumn: "Id");
                    table.ForeignKey(
                        name: "FK_WordSenses_WordEntries_WordEntryId",
                        column: x => x.WordEntryId,
                        principalTable: "WordEntries",
                        principalColumn: "Id");
                });

            migrationBuilder.CreateTable(
                name: "Flashcards",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    UserId = table.Column<Guid>(type: "uuid", nullable: false),
                    DeckId = table.Column<Guid>(type: "uuid", nullable: false),
                    FrontText = table.Column<string>(type: "text", nullable: false),
                    BackText = table.Column<string>(type: "text", nullable: false),
                    ExampleText = table.Column<string>(type: "text", nullable: true),
                    ImageUrl = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: true),
                    AudioUrl = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: true),
                    Repetition = table.Column<int>(type: "integer", nullable: false),
                    IntervalDays = table.Column<int>(type: "integer", nullable: false, defaultValue: 1),
                    EaseFactor = table.Column<double>(type: "double precision", nullable: false, defaultValue: 2.5),
                    NextReviewDate = table.Column<DateTime>(type: "timestamp without time zone", nullable: false),
                    FailCount = table.Column<int>(type: "integer", nullable: false),
                    TotalReviews = table.Column<int>(type: "integer", nullable: false),
                    WordSenseId = table.Column<Guid>(type: "uuid", nullable: true),
                    IsDeleted = table.Column<bool>(type: "boolean", nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Flashcards", x => x.Id);
                    table.ForeignKey(
                        name: "FK_Flashcards_Decks_DeckId",
                        column: x => x.DeckId,
                        principalTable: "Decks",
                        principalColumn: "Id");
                    table.ForeignKey(
                        name: "FK_Flashcards_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id");
                    table.ForeignKey(
                        name: "FK_Flashcards_WordSenses_WordSenseId",
                        column: x => x.WordSenseId,
                        principalTable: "WordSenses",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.SetNull);
                });

            migrationBuilder.CreateTable(
                name: "ReviewLogs",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    UserId = table.Column<Guid>(type: "uuid", nullable: false),
                    FlashcardId = table.Column<Guid>(type: "uuid", nullable: false),
                    Quality = table.Column<int>(type: "integer", nullable: false),
                    ResponseTimeMs = table.Column<long>(type: "bigint", nullable: true),
                    ReviewedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_ReviewLogs", x => x.Id);
                    table.ForeignKey(
                        name: "FK_ReviewLogs_Flashcards_FlashcardId",
                        column: x => x.FlashcardId,
                        principalTable: "Flashcards",
                        principalColumn: "Id");
                    table.ForeignKey(
                        name: "FK_ReviewLogs_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id");
                });

            migrationBuilder.CreateIndex(
                name: "IX_AiChat_UserSession",
                table: "AiChatHistory",
                columns: new[] { "UserId", "SessionId", "CreatedAt" });

            migrationBuilder.CreateIndex(
                name: "IX_AiUsageLogs_CreatedAt",
                table: "AiUsageLogs",
                column: "CreatedAt");

            migrationBuilder.CreateIndex(
                name: "IX_AiUsageLogs_PromptType",
                table: "AiUsageLogs",
                column: "PromptType");

            migrationBuilder.CreateIndex(
                name: "IX_AiUsageLogs_UserDate",
                table: "AiUsageLogs",
                columns: new[] { "UserId", "CreatedAt" });

            migrationBuilder.CreateIndex(
                name: "IX_ContentReports_ReportedByUserId",
                table: "ContentReports",
                column: "ReportedByUserId");

            migrationBuilder.CreateIndex(
                name: "IX_ContentReports_Status",
                table: "ContentReports",
                column: "Status");

            migrationBuilder.CreateIndex(
                name: "IX_Decks_UserId",
                table: "Decks",
                column: "UserId",
                filter: "\"IsDeleted\" = false");

            migrationBuilder.CreateIndex(
                name: "IX_DeckShares_DeckId",
                table: "DeckShares",
                column: "DeckId");

            migrationBuilder.CreateIndex(
                name: "IX_DeckShares_ShareCode",
                table: "DeckShares",
                column: "ShareCode",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_DeckSubscriptions_DeckUser",
                table: "DeckSubscriptions",
                columns: new[] { "DeckId", "SubscriberId" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_DeckSubscriptions_Subscriber",
                table: "DeckSubscriptions",
                column: "SubscriberId");

            migrationBuilder.CreateIndex(
                name: "IX_Flashcards_DeckId",
                table: "Flashcards",
                column: "DeckId");

            migrationBuilder.CreateIndex(
                name: "IX_Flashcards_NextReview",
                table: "Flashcards",
                columns: new[] { "UserId", "NextReviewDate" },
                filter: "\"IsDeleted\" = false");

            migrationBuilder.CreateIndex(
                name: "IX_Flashcards_UpdatedAt",
                table: "Flashcards",
                columns: new[] { "UserId", "UpdatedAt" });

            migrationBuilder.CreateIndex(
                name: "IX_Flashcards_UserDeck",
                table: "Flashcards",
                columns: new[] { "UserId", "DeckId" },
                filter: "\"IsDeleted\" = false");

            migrationBuilder.CreateIndex(
                name: "IX_Flashcards_WordSenseId",
                table: "Flashcards",
                column: "WordSenseId");

            migrationBuilder.CreateIndex(
                name: "IX_HomonymGroups_LemmaId",
                table: "HomonymGroups",
                column: "LemmaId");

            migrationBuilder.CreateIndex(
                name: "IX_Lemmas_LemmaText_Language",
                table: "Lemmas",
                columns: new[] { "LemmaText", "Language" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_PasswordResetTokens_UserId",
                table: "PasswordResetTokens",
                column: "UserId");

            migrationBuilder.CreateIndex(
                name: "IX_ReviewLogs_FlashcardId",
                table: "ReviewLogs",
                column: "FlashcardId");

            migrationBuilder.CreateIndex(
                name: "IX_ReviewLogs_UserDate",
                table: "ReviewLogs",
                columns: new[] { "UserId", "ReviewedAt" });

            migrationBuilder.CreateIndex(
                name: "IX_SharedImages_Keyword",
                table: "SharedImages",
                column: "Keyword",
                filter: "\"IsDeleted\" = false");

            migrationBuilder.CreateIndex(
                name: "IX_SharedImages_UserId",
                table: "SharedImages",
                column: "UserId");

            migrationBuilder.CreateIndex(
                name: "IX_Sync_UserPending",
                table: "SyncMetadata",
                columns: new[] { "UserId", "IsSynced" },
                filter: "\"IsSynced\" = false");

            migrationBuilder.CreateIndex(
                name: "IX_Users_Email",
                table: "Users",
                column: "Email",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_WordEntries_HomonymGroupId",
                table: "WordEntries",
                column: "HomonymGroupId");

            migrationBuilder.CreateIndex(
                name: "IX_WordEntries_LemmaId",
                table: "WordEntries",
                column: "LemmaId");

            migrationBuilder.CreateIndex(
                name: "IX_WordEntries_PartOfSpeech",
                table: "WordEntries",
                column: "PartOfSpeech");

            migrationBuilder.CreateIndex(
                name: "IX_WordSenses_SemanticClusterId",
                table: "WordSenses",
                column: "SemanticClusterId");

            migrationBuilder.CreateIndex(
                name: "IX_WordSenses_WordEntryId",
                table: "WordSenses",
                column: "WordEntryId");

            migrationBuilder.CreateIndex(
                name: "IX_WordVariants_LemmaId",
                table: "WordVariants",
                column: "LemmaId");

            migrationBuilder.CreateIndex(
                name: "IX_WordVariants_VariantText",
                table: "WordVariants",
                column: "VariantText");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "AiChatHistory");

            migrationBuilder.DropTable(
                name: "AiUsageLogs");

            migrationBuilder.DropTable(
                name: "ContentReports");

            migrationBuilder.DropTable(
                name: "DeckShares");

            migrationBuilder.DropTable(
                name: "DeckSubscriptions");

            migrationBuilder.DropTable(
                name: "PasswordResetTokens");

            migrationBuilder.DropTable(
                name: "ReviewLogs");

            migrationBuilder.DropTable(
                name: "SharedImages");

            migrationBuilder.DropTable(
                name: "SyncMetadata");

            migrationBuilder.DropTable(
                name: "WordVariants");

            migrationBuilder.DropTable(
                name: "Flashcards");

            migrationBuilder.DropTable(
                name: "Decks");

            migrationBuilder.DropTable(
                name: "WordSenses");

            migrationBuilder.DropTable(
                name: "Users");

            migrationBuilder.DropTable(
                name: "SemanticClusters");

            migrationBuilder.DropTable(
                name: "WordEntries");

            migrationBuilder.DropTable(
                name: "HomonymGroups");

            migrationBuilder.DropTable(
                name: "Lemmas");
        }
    }
}
