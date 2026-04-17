using System.Text;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using SmartFlashcardAPI.Data;
using SmartFlashcardAPI.Security;
using SmartFlashcardAPI.Services;

var builder = WebApplication.CreateBuilder(args);

// ── Database (EF Core + PostgreSQL / Supabase) ──────────────
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseNpgsql(builder.Configuration.GetConnectionString("DefaultConnection")));

// ── Authentication (JWT Bearer) ─────────────────────────────
var jwtKey = builder.Configuration["Jwt:Key"] ?? "DefaultSuperSecretKey1234567890ABCDEF";
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        // Keep JWT payload keys as claim types (sub, role, …). Default in .NET 8+.
        options.MapInboundClaims = false;
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey)),
            ValidateIssuer = true,
            ValidIssuer = builder.Configuration["Jwt:Issuer"] ?? "SmartFlashcardAPI",
            ValidateAudience = true,
            ValidAudience = builder.Configuration["Jwt:Audience"] ?? "SmartFlashcardApp",
            ValidateLifetime = true,
            // Small tolerance for device/server clock drift (Zero caused frequent 401 right at expiry).
            ClockSkew = TimeSpan.FromMinutes(2),
            // Default RoleClaimType is ClaimTypes.Role (long URI) — JWT uses short "role" → IsInRole / Roles auth would 403.
            RoleClaimType = JwtClaimNames.Role,
            NameClaimType = JwtClaimNames.Sub
        };
    });
builder.Services.AddAuthorization();

// ── Services (DI Registration) ──────────────────────────────
builder.Services.AddScoped<AuthService>();
builder.Services.AddScoped<EmailService>();
builder.Services.AddScoped<DeckService>();
builder.Services.AddScoped<FlashcardService>();
builder.Services.AddScoped<ReviewService>();
builder.Services.AddScoped<SyncService>();
builder.Services.AddScoped<AdminService>();
builder.Services.AddScoped<ShareService>();
builder.Services.AddScoped<GoogleSheetSyncService>();
builder.Services.AddSingleton<AiUsageLogger>();
builder.Services.AddHttpClient<GeminiService>(client =>
{
    client.Timeout = TimeSpan.FromMinutes(3);
});
builder.Services.AddScoped<WordAnalysisService>();
builder.Services.AddScoped<SharedImageService>();
builder.Services.AddMemoryCache();

// ── Controllers ─────────────────────────────────────────────
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.PropertyNamingPolicy = System.Text.Json.JsonNamingPolicy.CamelCase;
        options.JsonSerializerOptions.DefaultIgnoreCondition = System.Text.Json.Serialization.JsonIgnoreCondition.WhenWritingNull;
    });

// ── CORS (allow Android emulator & device) ──────────────────
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyMethod()
              .AllowAnyHeader();
    });
});

var app = builder.Build();

// ── Middleware Pipeline ─────────────────────────────────────
// NFR-S01: Enforce HTTPS in production (TLS 1.2+)
if (!app.Environment.IsDevelopment())
{
    app.UseHsts();
    app.UseHttpsRedirection();
}
app.UseStaticFiles(); // Serve wwwroot/ (generated images)
app.UseCors();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

// ── Startup Info ────────────────────────────────────────────
app.Logger.LogInformation("SmartFlashcard API starting...");
app.Logger.LogInformation("Endpoints: /api/auth, /api/decks, /api/cards, /api/reviews, /api/sync, /api/ai, /api/admin");

app.Run();
