using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SmartFlashcardAPI.Migrations
{
    /// <inheritdoc />
    public partial class AddImageDataToSharedImages : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "ContentType",
                table: "SharedImages",
                type: "character varying(50)",
                maxLength: 50,
                nullable: true);

            migrationBuilder.AddColumn<byte[]>(
                name: "ImageData",
                table: "SharedImages",
                type: "bytea",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "ContentType",
                table: "SharedImages");

            migrationBuilder.DropColumn(
                name: "ImageData",
                table: "SharedImages");
        }
    }
}
