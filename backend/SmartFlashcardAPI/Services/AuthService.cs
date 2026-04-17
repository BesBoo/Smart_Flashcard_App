using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using SmartFlashcardAPI.Data;
using SmartFlashcardAPI.Models.DTOs;
using SmartFlashcardAPI.Models.Entities;
using SmartFlashcardAPI.Security;

namespace SmartFlashcardAPI.Services;

public class AuthService
{
    private readonly AppDbContext _db;
    private readonly IConfiguration _config;
    private readonly EmailService _emailService;
    private readonly ILogger<AuthService> _logger;

    public AuthService(AppDbContext db, IConfiguration config, EmailService emailService, ILogger<AuthService> logger)
    {
        _db = db;
        _config = config;
        _emailService = emailService;
        _logger = logger;
    }

    public async Task<AuthResponse> RegisterAsync(RegisterRequest request)
    {
        if (await _db.Users.AnyAsync(u => u.Email == request.Email))
            throw new InvalidOperationException("CONFLICT:Email is already in use");

        var user = new User
        {
            Email = request.Email,
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.Password),
            DisplayName = request.DisplayName
        };

        _db.Users.Add(user);
        await _db.SaveChangesAsync();
        return GenerateTokens(user);
    }

    public async Task<AuthResponse> LoginAsync(LoginRequest request)
    {
        var user = await _db.Users.FirstOrDefaultAsync(u => u.Email == request.Email && u.IsActive);
        if (user == null)
            throw new UnauthorizedAccessException("Invalid email or password");

        try
        {
            if (!BCrypt.Net.BCrypt.Verify(request.Password, user.PasswordHash))
                throw new UnauthorizedAccessException("Invalid email or password");
        }
        catch (BCrypt.Net.SaltParseException)
        {
            throw new UnauthorizedAccessException("Invalid email or password");
        }

        return GenerateTokens(user);
    }

    // ══════════════════════════════════════════════════════════
    //  UPDATE EMAIL
    // ══════════════════════════════════════════════════════════

    public async Task UpdateEmailAsync(Guid userId, string newEmail)
    {
        var user = await _db.Users.FirstOrDefaultAsync(u => u.Id == userId && u.IsActive)
            ?? throw new KeyNotFoundException("Người dùng không tồn tại.");

        // Check if new email is already taken
        var existing = await _db.Users.FirstOrDefaultAsync(u => u.Email == newEmail && u.Id != userId);
        if (existing != null)
            throw new InvalidOperationException("CONFLICT:Email này đã được sử dụng bởi tài khoản khác.");

        user.Email = newEmail;
        user.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();
        _logger.LogInformation("Email updated for user {UserId} to {Email}", userId, newEmail);
    }

    // ══════════════════════════════════════════════════════════
    //  FORGOT / RESET PASSWORD
    // ══════════════════════════════════════════════════════════

    public async Task ForgotPasswordAsync(string email)
    {
        var user = await _db.Users.FirstOrDefaultAsync(u => u.Email == email && u.IsActive);
        if (user == null)
        {
            _logger.LogWarning("ForgotPassword: email not found: {Email}", email);
            throw new InvalidOperationException("Email này chưa được đăng ký. Vui lòng kiểm tra lại.");
        }

        // Invalidate existing tokens
        var existingTokens = await _db.PasswordResetTokens
            .Where(t => t.UserId == user.Id && !t.IsUsed).ToListAsync();
        foreach (var t in existingTokens) t.IsUsed = true;

        // Generate 6-digit OTP
        var otp = Random.Shared.Next(100000, 999999).ToString();
        _db.PasswordResetTokens.Add(new PasswordResetToken
        {
            UserId = user.Id,
            Token = otp,
            ExpiresAt = DateTime.UtcNow.AddMinutes(10)
        });
        await _db.SaveChangesAsync();

        await _emailService.SendPasswordResetEmailAsync(email, otp);
        _logger.LogInformation("Password reset OTP sent to {Email}", email);
    }

    public async Task ResetPasswordAsync(string email, string token, string newPassword)
    {
        var user = await _db.Users.FirstOrDefaultAsync(u => u.Email == email && u.IsActive)
            ?? throw new InvalidOperationException("Email không tồn tại.");

        var resetToken = await _db.PasswordResetTokens
            .Where(t => t.UserId == user.Id && t.Token == token && !t.IsUsed && t.ExpiresAt > DateTime.UtcNow)
            .OrderByDescending(t => t.CreatedAt)
            .FirstOrDefaultAsync()
            ?? throw new InvalidOperationException("Mã OTP không hợp lệ hoặc đã hết hạn.");

        resetToken.IsUsed = true;
        user.PasswordHash = BCrypt.Net.BCrypt.HashPassword(newPassword);
        user.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();
        _logger.LogInformation("Password reset successful for {Email}", email);
    }

    // ══════════════════════════════════════════════════════════
    //  GOOGLE SIGN-IN
    // ══════════════════════════════════════════════════════════

    public async Task<AuthResponse> GoogleLoginAsync(string idToken)
    {
        var payload = await VerifyGoogleTokenAsync(idToken)
            ?? throw new UnauthorizedAccessException("Google token không hợp lệ.");

        var email = payload.Email!;
        var name = payload.Name ?? email.Split('@')[0];

        var user = await _db.Users.FirstOrDefaultAsync(u => u.Email == email);
        if (user == null)
        {
            user = new User
            {
                Email = email,
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(Guid.NewGuid().ToString()),
                DisplayName = name,
                AvatarUrl = payload.Picture
            };
            _db.Users.Add(user);
            await _db.SaveChangesAsync();
            _logger.LogInformation("New Google user registered: {Email}", email);
        }
        else if (!user.IsActive)
        {
            throw new UnauthorizedAccessException("Tài khoản đã bị khóa.");
        }

        return GenerateTokens(user);
    }

    private async Task<GoogleTokenPayload?> VerifyGoogleTokenAsync(string idToken)
    {
        try
        {
            using var http = new HttpClient();
            var response = await http.GetAsync($"https://oauth2.googleapis.com/tokeninfo?id_token={idToken}");
            if (!response.IsSuccessStatusCode) return null;

            var json = await response.Content.ReadAsStringAsync();
            var payload = JsonSerializer.Deserialize<GoogleTokenPayload>(json,
                new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

            var expectedClientId = _config["Google:WebClientId"];
            if (payload != null && !string.IsNullOrEmpty(expectedClientId)
                && payload.Aud != expectedClientId && payload.Azp != expectedClientId)
            {
                _logger.LogWarning("Google token audience mismatch: {Aud}", payload.Aud);
                return null;
            }

            return payload;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Google token verification error");
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  REFRESH TOKEN
    // ══════════════════════════════════════════════════════════

    public async Task<AuthResponse> RefreshAsync(string refreshToken)
    {
        var principal = ValidateToken(refreshToken)
            ?? throw new UnauthorizedAccessException("Invalid or expired refresh token");
        var userId = principal.TryGetUserId()
            ?? throw new UnauthorizedAccessException("Invalid or expired refresh token");
        var user = await _db.Users.FindAsync(userId);
        if (user == null || !user.IsActive)
            throw new UnauthorizedAccessException("User not found or inactive");
        return GenerateTokens(user);
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════

    private AuthResponse GenerateTokens(User user)
    {
        var jwtKey = _config["Jwt:Key"] ?? "DefaultSuperSecretKey1234567890ABCDEF";
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey));
        var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

        var roleNormalized = string.IsNullOrWhiteSpace(user.Role) ? "user" : user.Role.Trim().ToLowerInvariant();

        var claims = new[]
        {
            new Claim(ClaimTypes.NameIdentifier, user.Id.ToString()),
            new Claim(ClaimTypes.Email, user.Email),
            new Claim(ClaimTypes.Name, user.DisplayName),
            new Claim("tier", user.SubscriptionTier),
            new Claim("role", roleNormalized)
        };

        var accessToken = new JwtSecurityToken(
            _config["Jwt:Issuer"] ?? "SmartFlashcardAPI",
            _config["Jwt:Audience"] ?? "SmartFlashcardApp",
            claims, expires: DateTime.UtcNow.AddHours(1), signingCredentials: creds);

        var refreshToken = new JwtSecurityToken(
            _config["Jwt:Issuer"] ?? "SmartFlashcardAPI",
            _config["Jwt:Audience"] ?? "SmartFlashcardApp",
            claims, expires: DateTime.UtcNow.AddDays(30), signingCredentials: creds);

        var handler = new JwtSecurityTokenHandler { OutboundClaimTypeMap = new Dictionary<string, string>() };
        return new AuthResponse(user.Id, handler.WriteToken(accessToken), handler.WriteToken(refreshToken), 3600, user.Role);
    }

    private ClaimsPrincipal? ValidateToken(string token)
    {
        var jwtKey = _config["Jwt:Key"] ?? "DefaultSuperSecretKey1234567890ABCDEF";
        var handler = new JwtSecurityTokenHandler { MapInboundClaims = false };
        try
        {
            return handler.ValidateToken(token, new TokenValidationParameters
            {
                ValidateIssuerSigningKey = true,
                IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey)),
                ValidateIssuer = true, ValidIssuer = _config["Jwt:Issuer"] ?? "SmartFlashcardAPI",
                ValidateAudience = true, ValidAudience = _config["Jwt:Audience"] ?? "SmartFlashcardApp",
                ValidateLifetime = true, ClockSkew = TimeSpan.FromMinutes(2),
                RoleClaimType = JwtClaimNames.Role, NameClaimType = JwtClaimNames.Sub
            }, out _);
        }
        catch { return null; }
    }
}

public class GoogleTokenPayload
{
    public string? Email { get; set; }
    public string? Name { get; set; }
    public string? Picture { get; set; }
    public string? Aud { get; set; }
    public string? Azp { get; set; }
    public string? Sub { get; set; }
}
