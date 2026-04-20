using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SmartFlashcardAPI.Migrations
{
    /// <inheritdoc />
    public partial class AddIsNotifiedToOwner : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<bool>(
                name: "IsNotifiedToOwner",
                table: "ContentReports",
                type: "boolean",
                nullable: false,
                defaultValue: false);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "IsNotifiedToOwner",
                table: "ContentReports");
        }
    }
}
