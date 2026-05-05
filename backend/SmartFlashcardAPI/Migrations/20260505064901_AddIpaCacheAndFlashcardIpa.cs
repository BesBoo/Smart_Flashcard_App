using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SmartFlashcardAPI.Migrations
{
    /// <inheritdoc />
    public partial class AddIpaCacheAndFlashcardIpa : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "PronunciationIpa",
                table: "Flashcards",
                type: "character varying(200)",
                maxLength: 200,
                nullable: true);

            migrationBuilder.CreateTable(
                name: "IpaCaches",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    LookupKey = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: false),
                    Ipa = table.Column<string>(type: "character varying(200)", maxLength: 200, nullable: false),
                    FrontText = table.Column<string>(type: "character varying(200)", maxLength: 200, nullable: false),
                    BackText = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: true),
                    UsageCount = table.Column<int>(type: "integer", nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "timestamp without time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_IpaCaches", x => x.Id);
                });

            migrationBuilder.CreateIndex(
                name: "IX_IpaCaches_LookupKey",
                table: "IpaCaches",
                column: "LookupKey",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "IpaCaches");

            migrationBuilder.DropColumn(
                name: "PronunciationIpa",
                table: "Flashcards");
        }
    }
}
